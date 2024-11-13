package com.gaia3d.converter.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.pointcloud.GaiaPointCloudHeader;
import com.gaia3d.basic.pointcloud.GaiaPointCloudTemp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class PointCloudTempGenerator {
    private final float HORIZONTAL_GRID_SIZE = 500.0f; // in meters
    private final float VERTICAL_GRID_SIZE = 50.0f; // in meters
    private final LasConverter converter;
    private GaiaPointCloudHeader combinedHeader;

    public List<File> generate(File tempPath, List<File> fileList) {
        List<File> tempFiles;
        combinedHeader = readAllHeaders(fileList);
        try {
            tempFiles = createTempGrid(tempPath);
            generateTempFiles(fileList);
            closeAllStreams();
            tempFiles = removeEmptyFiles(tempFiles);
            tempFiles = shuffleTempFiles(tempFiles);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return tempFiles;
    }

    private List<File> createTempGrid(File tempPath) throws FileNotFoundException {
        List<File> tempFiles = new ArrayList<>();
        GaiaPointCloudTemp[][][] tempGrid = combinedHeader.getTempGrid();
        Vector3d volume = combinedHeader.getSrsBoundingBox().getVolume();
        Vector3d offset = combinedHeader.getSrsBoundingBox().getMinPosition();
        for (int i = 0; i < tempGrid.length; i++) {
            for (int j = 0; j < tempGrid[i].length; j++) {
                for (int k = 0; k < tempGrid[i][j].length; k++) {
                    String tempFileName = String.format("grid-%d-%d-%d.bin", i, j, k);
                    File tempFile = new File(tempPath, tempFileName);

                    GaiaPointCloudTemp temp = new GaiaPointCloudTemp(tempFile);
                    tempGrid[i][j][k] = temp;

                    // Set quantized volume scale and offset
                    temp.getQuantizedVolumeScale()[0] = volume.x;
                    temp.getQuantizedVolumeScale()[1] = volume.y;
                    temp.getQuantizedVolumeScale()[2] = volume.z;
                    temp.getQuantizedVolumeOffset()[0] = offset.x;
                    temp.getQuantizedVolumeOffset()[1] = offset.y;
                    temp.getQuantizedVolumeOffset()[2] = offset.z;

                    temp.writeHeader();
                    tempFiles.add(tempFile);
                }
            }
        }
        return tempFiles;
    }

    private GaiaPointCloudHeader readAllHeaders(List<File> fileList) {
        log.info("[Pre] Reading headers of all files");
        List<GaiaPointCloudHeader> headers = new ArrayList<>();
        for (File file : fileList) {
            headers.add(converter.readHeader(file));
        }
        GaiaPointCloudHeader combinedHeader = GaiaPointCloudHeader.combineHeaders(headers);
        GaiaBoundingBox srsBoundingBox = combinedHeader.getSrsBoundingBox();
        Vector3d volume = srsBoundingBox.getVolume();
        int gridXCount = (int) Math.ceil(volume.x / HORIZONTAL_GRID_SIZE);
        int gridYCount = (int) Math.ceil(volume.y / HORIZONTAL_GRID_SIZE);
        int gridZCount = (int) Math.ceil(volume.z / VERTICAL_GRID_SIZE);

        GaiaPointCloudTemp[][][] tempGrid = new GaiaPointCloudTemp[gridXCount][gridYCount][gridZCount];
        combinedHeader.setTempGrid(tempGrid);

        log.debug("[Pre] Combined header: {}", combinedHeader);
        log.debug("[Pre] Grid size: {}x{}", gridXCount, gridYCount);
        log.debug("[Pre] Volume: {}", volume);
        log.debug("[Pre] Get Points Size: {}", combinedHeader.getSize());
        combinedHeader.getSize();

        return combinedHeader;
    }

    private void generateTempFiles(List<File> fileList) {
        int fileLength = fileList.size();
        AtomicInteger fileCount = new AtomicInteger(0);
        fileList.forEach((originalFile) -> {
            converter.loadToTemp(combinedHeader, originalFile);
            log.info("[Pre][{}/{}] Generated temp file for {}", fileCount.incrementAndGet(), fileLength, originalFile.getName());
        });
    }

    private List<File> shuffleTempFiles(List<File> tempFiles) {
        List<File> shuffledTempFiles = new ArrayList<>();
        int fileLength = tempFiles.size();
        AtomicInteger tempCount = new AtomicInteger(0);
        tempFiles.forEach((tempFile) -> {
            int count = tempCount.incrementAndGet();
            log.info("[Pre][{}/{}] Shuffling temp file: {}", count, fileLength, tempFile.getName());
            GaiaPointCloudTemp temp = new GaiaPointCloudTemp(tempFile);
            temp.shuffleTemp();
            shuffledTempFiles.add(temp.getTempFile());
        });
        return shuffledTempFiles;
    }

    private void closeAllStreams() {
        GaiaPointCloudTemp[][][] tempGridAll = combinedHeader.getTempGrid();
        for (GaiaPointCloudTemp[][] tempGridX : tempGridAll) {
            for (GaiaPointCloudTemp[] tempGridY : tempGridX) {
                for (GaiaPointCloudTemp tempGridZ : tempGridY) {
                    OutputStream outputStream = tempGridZ.getOutputStream();
                    if (outputStream != null) {
                        try {
                            outputStream.flush();
                            outputStream.close();
                        } catch (Exception e) {
                            log.error("Failed to close input stream", e);
                        }
                    }
                }
            }
        }
    }

    private List<File> removeEmptyFiles(List<File> tempFiles) {
        List<File> newTempFiles = new ArrayList<>();
        tempFiles.forEach((tempFile) -> {
            if (tempFile.length() <= 52) {
                if (!tempFile.delete()) {
                    log.error("Failed to delete empty temp file: {}", tempFile);
                }
            } else {
                newTempFiles.add(tempFile);
            }
        });
        return newTempFiles;
    }
}
