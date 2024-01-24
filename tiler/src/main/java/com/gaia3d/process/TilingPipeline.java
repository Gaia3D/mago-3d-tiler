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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
        tileInfos = new ArrayList<>();

        /* Pre-process */
        try {
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

        /* loading all file list */
        log.info("[Pre] Loading all files.");
        List<File> fileList = fileLoader.loadFiles();
        log.info("[Pre] Finished loading all files");

        ExecutorService executorService = Executors.newFixedThreadPool(globalOptions.getMultiThreadCount());
        List<Runnable> tasks = new ArrayList<>();
        int fileCount = fileList.size();
        for (int count = 0; count < fileCount; count++) {
            File file = fileList.get(count);
            int finalCount = count;
            Runnable callableTask = () -> {
                List<TileInfo> loadedTileInfos = fileLoader.loadTileInfo(file);
                log.warn("[Pre][{}/{}] Loading file : {}", finalCount + 1, fileCount, file.getName());
                if (loadedTileInfos == null) {
                    log.warn("[Pre][{}/{}] Failed to load file : {}.", finalCount + 1, fileCount, file.getName());
                    return;
                }
                int infoLength = loadedTileInfos.size();
                for (int index = 0; index < infoLength; index++) {
                    TileInfo tileInfo = loadedTileInfos.get(index);
                    if (tileInfo != null) {
                        log.info("[Pre][{}/{}][{}/{}] Loading tiles from file.", finalCount + 1, fileCount, index + 1, infoLength);
                        for (PreProcess preProcessors : preProcesses) {
                            preProcessors.run(tileInfo);
                        }
                        // TODO : Upcoming improvements to minimize
                        tileInfo.minimize(index + 1);
                        tileInfos.add(tileInfo);
                    }
                }
            };
            tasks.add(callableTask);
        }
        excuteThread(executorService, tasks);
        log.info("[Pre] End the pre-processing.");
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
                List<TileInfo> childTileInfos = contentInfo.getTileInfos();
                for (TileInfo tileInfo : childTileInfos) {
                    // TODO : Upcoming improvements to maximize
                    tileInfo.maximize();
                }
                for (PostProcess postProcessor : postProcesses) {
                    postProcessor.run(contentInfo);
                }
                contentInfo.deleteTexture();
                contentInfo.clear();
            };
            tasks.add(callableTask);
        }
        excuteThread(executorService, tasks);
        log.info("[Post] End the post-processing.");
    }

    private void deleteTemp() throws IOException {
        if (!tileInfos.isEmpty()) {
            tileInfos.get(0).deleteTemp();
        } else {
            log.warn("No tile info to delete temp files.");
        }
    }

    private void excuteThread(ExecutorService executorService, List<Runnable> tasks) throws InterruptedException {
        for (Runnable task : tasks) {
            try {
                Future<?> future = executorService.submit(task);
                Object result = future.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        executorService.shutdown();
        do {
            if (executorService.isTerminated()) {
                executorService.shutdownNow();
            }
        } while (!executorService.awaitTermination(3, TimeUnit.SECONDS));
    }
}
