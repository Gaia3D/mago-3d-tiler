package com.gaia3d.converter.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaVertex;
import com.gaia3d.basic.pointcloud.GaiaPointCloudHeader;
import com.gaia3d.basic.pointcloud.GaiaPointCloudTemp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class PointCloudTempGenerator {
    private final float GRID_SIZE = 100.0f; // in meters
    private final LasConverter converter;
    private GaiaPointCloudHeader combinedHeader;

    public List<File> generate(File tempPath, List<File> fileList) {
        List<File> tempFiles;
        combinedHeader = readAllHeaders(fileList);
        try {
            tempFiles = createTempGrid(tempPath);
            generateTempFiles(fileList);
            closeAllStreams();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return tempFiles;
    }

    private List<File> createTempGrid(File tempPath) throws FileNotFoundException {
        List<File> tempFiles = new ArrayList<>();
        GaiaPointCloudTemp[][] tempGrid = combinedHeader.getTempGrid();
        Vector3d volume = combinedHeader.getSrsBoundingBox().getVolume();
        Vector3d offset = combinedHeader.getSrsBoundingBox().getMinPosition();
        for (int i = 0; i < tempGrid.length; i++) {
            for (int j = 0; j < tempGrid[i].length; j++) {
                String tempFileName = String.format("temp-%d-%d.bin", i, j);
                File tempFile = new File(tempPath, tempFileName);
                tempGrid[i][j] = new GaiaPointCloudTemp(tempFile);

                // Set quantized volume scale and offset
                tempGrid[i][j].getQuantizedVolumeScale()[0] = volume.x;
                tempGrid[i][j].getQuantizedVolumeScale()[1] = volume.y;
                tempGrid[i][j].getQuantizedVolumeScale()[2] = volume.z;
                tempGrid[i][j].getQuantizedVolumeOffset()[0] = offset.x;
                tempGrid[i][j].getQuantizedVolumeOffset()[1] = offset.y;
                tempGrid[i][j].getQuantizedVolumeOffset()[2] = offset.z;

                tempGrid[i][j].openOutputStream();
                tempGrid[i][j].writeHeader();
                tempFiles.add(tempFile);
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
        int gridXCount = (int) Math.ceil(volume.x / GRID_SIZE);
        int gridYCount = (int) Math.ceil(volume.y / GRID_SIZE);

        GaiaPointCloudTemp[][] tempGrid = new GaiaPointCloudTemp[gridXCount][gridYCount];
        combinedHeader.setTempGrid(tempGrid);
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

    private void closeAllStreams() {
        GaiaPointCloudTemp[][] tempGrid = combinedHeader.getTempGrid();
        for (GaiaPointCloudTemp[] gaiaPointCloudTemps : tempGrid) {
            for (GaiaPointCloudTemp gaiaPointCloudTemp : gaiaPointCloudTemps) {
                gaiaPointCloudTemp.closeSteam();
            }
        }
    }
}
