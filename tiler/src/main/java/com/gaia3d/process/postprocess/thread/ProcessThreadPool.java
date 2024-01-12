package com.gaia3d.process.postprocess.thread;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.FileLoader;
import com.gaia3d.process.postprocess.PostProcess;
import com.gaia3d.process.preprocess.PreProcess;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class ProcessThreadPool {

    public void preProcessStart(List<TileInfo> tileInfos, List<File> fileList, FileLoader fileLoader, List<PreProcess> preProcessors) throws InterruptedException {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        byte multiThreadCount = globalOptions.getMultiThreadCount();
        log.info("[ThreadPool][{}][Start Pre-process]", multiThreadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(multiThreadCount);
        List<Runnable> tasks = new ArrayList<>();

        log.info("[pre-process] Total input file count : {}", fileList.size());
        AtomicInteger count = new AtomicInteger();
        int size = fileList.size();
        for (File file : fileList) {
            Runnable callableTask = () -> {
                count.getAndIncrement();
                log.info("[File][load] : {}/{} : {}", count, size, file);
                List<TileInfo> tileInfoResult = fileLoader.loadTileInfo(file);
                int serial = 0;
                for (TileInfo tileInfo : tileInfoResult) {
                    if (tileInfo != null) {
                        log.info("[{}/{}][{}/{}] load TileInfo...", count, size, serial, tileInfoResult.size());
                        for (PreProcess preProcessor : preProcessors) {
                            preProcessor.run(tileInfo);
                        }
                        tileInfo.minimize(serial++);
                        tileInfos.add(tileInfo);
                    }
                }
            };
            tasks.add(callableTask);
        }

        for (Runnable task : tasks) {
            executorService.submit(task);
        }
        executorService.shutdown();

        do {
            if (executorService.isTerminated()) {
                executorService.shutdownNow();
            }
        } while (!executorService.awaitTermination(3, TimeUnit.SECONDS));
        log.info("[ThreadPool][End Pre-process]");
    }

    public void postProcessStart(List<ContentInfo> contentInfos, List<PostProcess> postProcesses) throws InterruptedException {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        byte multiThreadCount = globalOptions.getMultiThreadCount();
        ExecutorService executorService = Executors.newFixedThreadPool(multiThreadCount);
        log.info("[ThreadPool][{}][Start Post-process]", multiThreadCount);

        AtomicInteger count = new AtomicInteger();
        int size = contentInfos.size();
        List<Runnable> tasks = new ArrayList<>();
        for (ContentInfo contentInfo : contentInfos) {
            Runnable callableTask = () -> {
                count.getAndIncrement();
                log.info("[B3DM][{}/{}] post-process : {}", count, size, contentInfo.getName());
                List<TileInfo> childTileInfos = contentInfo.getTileInfos();
                List<TileInfo> copiedTileInfos = childTileInfos.stream()
                        .map((childTileInfo) -> TileInfo.builder()
                                .kmlInfo(childTileInfo.getKmlInfo())
                                .scenePath(childTileInfo.getScenePath())
                                .tempPath(childTileInfo.getTempPath())
                                .transformMatrix(childTileInfo.getTransformMatrix())
                                .boundingBox(childTileInfo.getBoundingBox())
                                .build())
                        .collect(Collectors.toList());
                contentInfo.setTileInfos(copiedTileInfos);

                for (TileInfo tileInfo : copiedTileInfos) {
                    tileInfo.maximize();
                }
                for (PostProcess postProcessor : postProcesses) {
                    postProcessor.run(contentInfo);
                }
                contentInfo.deleteTexture();
                copiedTileInfos.clear();
            };
            tasks.add(callableTask);
        }

        for (Runnable task : tasks) {
            executorService.submit(task);
        }
        executorService.shutdown();

        do {
            if (executorService.isTerminated()) {
                executorService.shutdownNow();
            }
        } while (!executorService.awaitTermination(3, TimeUnit.SECONDS));
        log.info("[ThreadPool][End Post-process]");
    }
}
