package com.gaia3d.process;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.FileLoader;
import com.gaia3d.process.postprocess.PostProcess;
import com.gaia3d.process.preprocess.PreProcess;
import com.gaia3d.process.tileprocess.Pipeline;
import com.gaia3d.process.tileprocess.TilingProcess;
import com.gaia3d.process.tileprocess.Tiler;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class TilingPipeline implements Pipeline {
    private final List<PreProcess> preProcesses;
    private final TilingProcess tilingProcess;
    private final List<PostProcess> postProcesses;

    /* global options */
    private final GlobalOptions globalOptions = GlobalOptions.getInstance();

    /* Tiling process info */
    private List<TileInfo> tileInfos;
    private Tileset tileset;
    private List<ContentInfo> contentInfos;

    @Override
    public void process(FileLoader fileLoader) throws IOException {
        /* Pre-process */
        try {
            createTemp();
            startPreProcesses(fileLoader);
            /* Main-process */
            startTilingProcess();
            /* Post-process */
            startPostProcesses();
            /* Delete temp files */
            deleteTemp();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void startPreProcesses(FileLoader fileLoader) throws InterruptedException {
        log.info("[Pre] Start the pre-processing.");
        tileInfos = new ArrayList<>();

        /* loading all file list */
        log.info("[Pre] Loading all files.");
        List<File> fileList = fileLoader.loadFiles();
        log.info("[Pre] Finished loading all files");

        ExecutorService executorService = Executors.newFixedThreadPool(globalOptions.getMultiThreadCount());
        List<Runnable> tasks = new ArrayList<>();
        int fileCount = fileList.size();
        AtomicLong nodeCount = new AtomicLong(0);
        for (int count = 0; count < fileCount; count++) {
            File file = fileList.get(count);
            int finalCount = count;
            Runnable callableTask = () -> {
                List<TileInfo> loadedTileInfos = fileLoader.loadTileInfo(file);
                log.info("[Pre][{}/{}] Loading file : {}", finalCount + 1, fileCount, file.getName());
                if (loadedTileInfos == null) {
                    log.warn("[Pre][{}/{}] Failed to load file : {}.", finalCount + 1, fileCount, file.getName());
                    return;
                }
                int infoLength = loadedTileInfos.size();
                nodeCount.addAndGet(infoLength);
                for (int index = 0; index < infoLength; index++) {
                    TileInfo tileInfo = loadedTileInfos.get(index);
                    if (tileInfo != null) {
                        log.info("[Pre][{}/{}][{}/{}] Loading tiles from file.", finalCount + 1, fileCount, index + 1, infoLength);
                        tileInfo.setSerial(index + 1);
                        for (PreProcess preProcessors : preProcesses) {
                            preProcessors.run(tileInfo);
                        }
                        tileInfos.add(tileInfo);
                    }
                }
            };
            tasks.add(callableTask);
        }
        executeThread(executorService, tasks);

        long nodeCountValue = nodeCount.get();
        // Auto set node limit
        calcNodeLimit(nodeCountValue);

        log.info("[Pre] Total Node Count {}, Auto Node limit : {}", nodeCount, globalOptions.getNodeLimit());
        log.info("[Pre] End the pre-processing.");
    }

    private void calcNodeLimit(long nodeCountValue) {
        if (globalOptions.getNodeLimit() < 0) {
            /*if (nodeCountValue > 262144) {
                globalOptions.setNodeLimit(16384);
            } else */

            if (nodeCountValue > 131072) {
                globalOptions.setNodeLimit(8192);
            } else if (nodeCountValue > 65536) {
                globalOptions.setNodeLimit(4096);
            } else if (nodeCountValue > 32768) {
                globalOptions.setNodeLimit(2048);
            } else {
                globalOptions.setNodeLimit(1024);
            }

           /* if (nodeCountValue > 262144) {
                globalOptions.setNodeLimit(16384);
            } else if (nodeCountValue > 131072) {
                globalOptions.setNodeLimit(8192);
            } else if (nodeCountValue > 65536) {
                globalOptions.setNodeLimit(4096);
            } else if (nodeCountValue > 32768) {
                globalOptions.setNodeLimit(2048);
            } else if (nodeCountValue > 16384) {
                globalOptions.setNodeLimit(1024);
            } else {
                globalOptions.setNodeLimit(512);
            }*/
        }
    }

    private void startTilingProcess() {
        log.info("[Tiling] Start the tiling process.");
        Tiler tiler = (Tiler) tilingProcess;
        log.info("[Tiling] Writing tileset file.");
        tileset = tiler.run(tileInfos);
        tiler.writeTileset(tileset);
        log.info("[Tiling] End the tiling process.");
    }

    private void startPostProcesses() throws InterruptedException {
        log.info("[Post] Start the post-processing.");

        ExecutorService executorService = Executors.newFixedThreadPool(globalOptions.getMultiThreadCount());
        List<Runnable> tasks = new ArrayList<>();
        contentInfos = tileset.findAllContentInfo();
        AtomicInteger count = new AtomicInteger(1);
        int contentCount = contentInfos.size();
        globalOptions.setTileCount(contentCount);
        for (ContentInfo contentInfo : contentInfos) {
            Runnable callableTask = () -> {
                log.info("[Post][{}/{}] post-process in progress. : {}", count.getAndIncrement(), contentCount, contentInfo.getName());
                List<TileInfo> tileInfos = contentInfo.getTileInfos();
                List<TileInfo> tileInfosClone = tileInfos.stream()
                        .map((childTileInfo) -> TileInfo.builder()
                            .scene(childTileInfo.getScene())
                            .kmlInfo(childTileInfo.getKmlInfo())
                            .scenePath(childTileInfo.getScenePath())
                            .tempPath(childTileInfo.getTempPath())
                            .transformMatrix(childTileInfo.getTransformMatrix())
                            .boundingBox(childTileInfo.getBoundingBox())
                            .pointCloud(childTileInfo.getPointCloud())
                            .build())
                        .collect(Collectors.toList());
                contentInfo.setTileInfos(tileInfosClone);
                for (PostProcess postProcessor : postProcesses) {
                    postProcessor.run(contentInfo);
                }
                contentInfo.deleteTexture();
                tileInfosClone.clear();
            };
            tasks.add(callableTask);
        }
        executeThread(executorService, tasks);
        log.info("[Post] End the post-processing.");
    }

    private void createTemp() throws IOException {
        /* create temp directory */
        File tempFile = new File(globalOptions.getOutputPath(), "temp");
        if (!tempFile.exists() && tempFile.mkdirs()) {
            log.info("[Pre] Created temp directory in {}", tempFile.getAbsolutePath());
        }
    }

    private void deleteTemp() throws IOException {
        /* delete temp directory */
        File tempFile = new File(globalOptions.getOutputPath(), "temp");
        if (tempFile.exists() && tempFile.isDirectory()) {
            FileUtils.deleteDirectory(tempFile);
        }
    }

    private void executeThread(ExecutorService executorService, List<Runnable> tasks) throws InterruptedException {
        try {
            for (Runnable task : tasks) {
                executorService.submit(task);
                //Future<?> future = executorService.submit(task);
                // TODO MultiThead BUG
                //future.get();
                //future.isDone();
            }
        } catch (Exception e) {
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
