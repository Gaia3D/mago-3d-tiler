package com.gaia3d.converter.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.pointcloud.GaiaPointCloudHeader;
import com.gaia3d.basic.pointcloud.GaiaPointCloudTemp;
import com.gaia3d.command.mago.GlobalConstants;
import com.gaia3d.command.mago.GlobalOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.proj.LongLatProjection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class PointCloudTempGenerator {
    /*private final float HORIZONTAL_GRID_SIZE = 500.0f; // in meters
    private final float VERTICAL_GRID_SIZE = 50.0f; // in meters*/
    private final LasConverter converter;
    private GaiaPointCloudHeader combinedHeader;

    public List<File> generate(File tempPath, List<File> fileList) {
        List<File> tempFiles;
        combinedHeader = readAllHeaders(fileList);
        GaiaBoundingBox boundingBox = combinedHeader.getSrsBoundingBox();
        Vector3d volume = boundingBox.getVolume();
        GaiaPointCloudTemp[][] tempGrid = combinedHeader.getTempGrid();
        int length = tempGrid.length;
        int width = tempGrid[0].length;
        log.info("[Pre] Total Volume: {}, {}, {}", volume.x, volume.y, volume.z);
        log.info("[Pre] Generating temp files");
        try {
            tempFiles = createTempGrid(tempPath);
            generateTempFiles(fileList);
            //generateTempFilesOnThread(fileList);
            closeAllStreams();
            tempFiles = removeEmptyFiles(tempFiles);
            //tempFiles = shuffleTempFiles(tempFiles);
            tempFiles = shuffleTempFilesOnThread(tempFiles);
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
                String tempFileName = String.format("%d/%d.bin", i, j);
                File tempFile = new File(tempPath, tempFileName);
                if (!tempFile.getParentFile().exists()) {
                    tempFile.getParentFile().mkdirs();
                }
                GaiaPointCloudTemp temp = new GaiaPointCloudTemp(tempFile);
                tempGrid[i][j] = temp;

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
        return tempFiles;
    }

    private GaiaPointCloudHeader readAllHeaders(List<File> fileList) {
        log.info("[Pre] Reading headers of all files");
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        CoordinateReferenceSystem crs = globalOptions.getCrs();

        float horizontalGridSize;
        float verticalGridSize;
        if (crs.getProjection() instanceof LongLatProjection) {
            horizontalGridSize = GlobalConstants.POINTSCLOUD_HORIZONTAL_ARC;
            verticalGridSize = GlobalConstants.POINTSCLOUD_VERTICAL_ARC;
        } else {
            horizontalGridSize = GlobalConstants.POINTSCLOUD_HORIZONTAL_GRID;
            verticalGridSize = GlobalConstants.POINTSCLOUD_VERTICAL_GRID;
        }

        List<GaiaPointCloudHeader> headers = new ArrayList<>();
        for (File file : fileList) {
            headers.add(converter.readHeader(file));
        }
        GaiaPointCloudHeader combinedHeader = GaiaPointCloudHeader.combineHeaders(headers);
        GaiaBoundingBox srsBoundingBox = combinedHeader.getSrsBoundingBox();
        Vector3d volume = srsBoundingBox.getVolume();
        int gridXCount = (int) Math.ceil(volume.x / horizontalGridSize);
        int gridYCount = (int) Math.ceil(volume.y / verticalGridSize);

        int limit = 67108864; // (8192 * 8192)
        int gridVolume = gridXCount * gridYCount;
        if ((combinedHeader.getSize() / gridVolume) > limit) {
            log.warn("[WARN] The point density is {} points per grid.", combinedHeader.getSize() / gridVolume);
            horizontalGridSize = horizontalGridSize / 2;
            verticalGridSize = verticalGridSize / 2;
            gridXCount = (int) Math.ceil(volume.x / horizontalGridSize);
            gridYCount = (int) Math.ceil(volume.y / verticalGridSize);
        }

        GaiaPointCloudTemp[][] tempGrid = new GaiaPointCloudTemp[gridXCount][gridYCount];
        combinedHeader.setTempGrid(tempGrid);

        log.debug("[Pre] Combined header: {}", combinedHeader);
        log.debug("[Pre] Grid size: {}x{}", gridXCount, gridYCount);
        log.debug("[Pre] Volume: {}", volume);
        log.debug("[Pre] Get Points Size: {}", combinedHeader.getSize());
        log.debug("[Pre] Grid Width: {}", horizontalGridSize);
        log.debug("[Pre] Grid Height: {}", verticalGridSize);
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

    private void generateTempFilesOnThread(List<File> fileList) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        ExecutorService executorService = Executors.newFixedThreadPool(globalOptions.getMultiThreadCount());
        List<Runnable> tasks = new ArrayList<>();
        fileList.forEach((originalFile) -> {
            Runnable callableTask = () -> {
                log.info("[Pre] Generated temp file for {}", originalFile.getName());
                converter.loadToTemp(combinedHeader, originalFile);
            };
            tasks.add(callableTask);
        });
        try {
            executeThread(executorService, tasks);
        } catch (InterruptedException e) {
            log.error("[ERROR] :Failed to generate temp files on thread.", e);
            throw new RuntimeException(e);
        }
    }

    /*private List<File> shuffleTempFiles(List<File> tempFiles) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        List<File> shuffledTempFiles = new ArrayList<>();
        int fileLength = tempFiles.size();
        AtomicInteger tempCount = new AtomicInteger(0);

        double width = globalOptions.POINTSCLOUD_HORIZONTAL_GRID;
        double height = globalOptions.POINTSCLOUD_HORIZONTAL_GRID;
        double depth = globalOptions.POINTSCLOUD_VERTICAL_GRID;
        depth = 1.0;
        int volume = (int) Math.ceil(width * height * depth);

        int limitSize;
        if (globalOptions.isSourcePrecision()) {
            limitSize = -1;
        } else {
            limitSize = volume;
        }
        limitSize = globalOptions.getPointLimit() * 5;
        limitSize = -1;
        log.info("[Pre] Shuffling temp files with limit size: {}", limitSize);

        int finalLimitSize = limitSize;
        tempFiles.forEach((tempFile) -> {
            int count = tempCount.incrementAndGet();
            log.info("[Pre][{}/{}] Shuffling temp file: {}", count, fileLength, tempFile.getName());
            GaiaPointCloudTemp temp = new GaiaPointCloudTemp(tempFile);
            temp.shuffleTemp(finalLimitSize);
            shuffledTempFiles.add(temp.getTempFile());
        });
        log.info("[Pre] Shuffled temp files");
        return shuffledTempFiles;
    }*/

    private List<File> shuffleTempFilesOnThread(List<File> tempFiles) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        List<File> shuffledTempFiles = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(globalOptions.getMultiThreadCount());
        List<Runnable> tasks = new ArrayList<>();
        AtomicInteger tempCount = new AtomicInteger(0);
        int fileLength = tempFiles.size();

        /*int depth = 1 + 4 + 16 + 64 + 256;
        int limitSize;
        if (globalOptions.isSourcePrecision()) {
            limitSize = -1;
        } else {
            limitSize = globalOptions.getMaximumPointPerTile() * depth;
        }

        log.info("[Pre] Shuffling temp files with limit size: {}", limitSize);*/

        //int finalLimitSize = limitSize;
        tempFiles.forEach((tempFile) -> {
            Runnable callableTask = () -> {
                int count = tempCount.incrementAndGet();
                log.info("[Pre][{}/{}] Shuffling temp file: {}", count, fileLength, tempFile.getAbsoluteFile());
                GaiaPointCloudTemp temp = new GaiaPointCloudTemp(tempFile);
                temp.shuffleTempMoreFast(count, fileLength);
                shuffledTempFiles.add(temp.getTempFile());
            };
            tasks.add(callableTask);
        });
        try {
            executeThread(executorService, tasks);
        } catch (InterruptedException e) {
            log.error("[ERROR] :Failed to shuffle temp files on thread.", e);
            throw new RuntimeException(e);
        }
        return shuffledTempFiles;
    }

    private void closeAllStreams() {
        GaiaPointCloudTemp[][] tempGridAll = combinedHeader.getTempGrid();
        for (GaiaPointCloudTemp[] tempGridX : tempGridAll) {
            for (GaiaPointCloudTemp tempGridY : tempGridX) {
                OutputStream outputStream = tempGridY.getOutputStream();
                if (outputStream != null) {
                    try {
                        outputStream.flush();
                        outputStream.close();
                    } catch (Exception e) {
                        log.error("[ERROR] :Failed to close input stream", e);
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
                    log.error("[ERROR] Failed to delete empty temp file: {}", tempFile);
                }
            } else {
                newTempFiles.add(tempFile);
            }
        });
        return newTempFiles;
    }

    private void executeThread(ExecutorService executorService, List<Runnable> tasks) throws InterruptedException {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        try {
            for (Runnable task : tasks) {
                Future<?> future = executorService.submit(task);
                if (globalOptions.isDebug()) {
                    future.get();
                }
            }
        } catch (Exception e) {
            log.error("[ERROR] :Failed to execute thread.", e);
            throw new RuntimeException(e);
        }
        executorService.shutdown();
        do {
            if (executorService.isTerminated()) {
                executorService.shutdownNow();
            }
        } while (!executorService.awaitTermination(2, TimeUnit.SECONDS));
    }
}
