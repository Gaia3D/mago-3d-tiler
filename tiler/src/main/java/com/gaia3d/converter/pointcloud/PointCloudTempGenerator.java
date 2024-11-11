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
        for (int i = 0; i < tempGrid.length; i++) {
            for (int j = 0; j < tempGrid[i].length; j++) {
                String tempFileName = String.format("temp-%d-%d.bin", i, j);
                File tempFile = new File(tempPath, tempFileName);
                tempGrid[i][j] = new GaiaPointCloudTemp(tempFile, combinedHeader.getBlockSize());
                tempGrid[i][j].openOutputStream();
                tempGrid[i][j].writeHeader();
                tempFiles.add(tempFile);
            }
        }
        return tempFiles;
    }

    private GaiaPointCloudHeader readAllHeaders(List<File> fileList) {
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
        fileList.forEach((originalFile) -> {
            converter.loadToTemp(combinedHeader, originalFile);
            log.info("Generated temp file for {}", originalFile.getName());
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
