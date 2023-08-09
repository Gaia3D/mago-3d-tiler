package process.postprocess.thread;

import converter.FileLoader;
import lombok.extern.slf4j.Slf4j;
import process.postprocess.PostProcess;
import process.preprocess.PreProcess;
import process.tileprocess.tile.ContentInfo;
import process.tileprocess.tile.TileInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class TestPool  {
    private static final int THREAD_COUNT = 8;

    public void preProcessStart(List<TileInfo> tileInfos, List<File> fileList, FileLoader fileLoader, List<PreProcess> preProcessors) throws InterruptedException {
        log.info("start pre process");
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Runnable> tasks = new ArrayList<>();

        AtomicInteger count = new AtomicInteger();
        int size = fileList.size();
        for (File file : fileList) {
            Runnable callableTask = () -> {
                count.getAndIncrement();
                log.info("[{}/{}] load tile info: {}", count, size, file);
                TileInfo tileInfo = fileLoader.loadTileInfo(file);
                if (tileInfo != null) {
                    for (PreProcess preProcessor : preProcessors) {
                        preProcessor.run(tileInfo);
                    }
                    tileInfo.minimize();
                    tileInfos.add(tileInfo);
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
        } while (!executorService.awaitTermination(2, TimeUnit.SECONDS));
        log.info("end pre process");
    }
    
    public void postProcessStart(List<ContentInfo> contentInfos, List<PostProcess> postProcesses) throws InterruptedException {
        log.info("start post process");
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

        List<Runnable> tasks = new ArrayList<>();
        for (ContentInfo contentInfo : contentInfos) {
            Runnable callableTask = () -> {
                List<TileInfo> childTileInfos = contentInfo.getTileInfos();
                List<TileInfo> copiedTileInfos = childTileInfos.stream().map((childTileInfo) -> {
                    return TileInfo.builder()
                            .kmlInfo(childTileInfo.getKmlInfo())
                            .scenePath(childTileInfo.getScenePath())
                            .tempPath(childTileInfo.getTempPath())
                            .transformMatrix(childTileInfo.getTransformMatrix())
                            .boundingBox(childTileInfo.getBoundingBox())
                            .build();
                }).collect(Collectors.toList());
                contentInfo.setTileInfos(copiedTileInfos);

                for (TileInfo tileInfo : copiedTileInfos) {
                    tileInfo.maximize();
                }
                for (PostProcess postProcessor : postProcesses) {
                    postProcessor.run(contentInfo);
                }

                contentInfo.deleteTexture();
                //contentInfo.setTileInfos(childTileInfos);
                //contentInfo.deleteTexture();
                //contentInfo.clear();

                copiedTileInfos.clear();
                copiedTileInfos = null;
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
        } while (!executorService.awaitTermination(2, TimeUnit.SECONDS));
        log.info("end post process");
    }
}
