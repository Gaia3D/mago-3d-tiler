package com.gaia3d.process;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.loader.FileLoader;
import com.gaia3d.process.postprocess.PostProcess;
import com.gaia3d.process.preprocess.PreProcess;
import com.gaia3d.process.tileprocess.Pipeline;
import com.gaia3d.process.tileprocess.Tiler;
import com.gaia3d.process.tileprocess.TilingProcess;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
    private List<File> fileList;
    private List<TileInfo> tileInfos;
    private Tileset tileset;
    private List<ContentInfo> contentInfos;

    @Override
    public void process(FileLoader fileLoader) throws IOException {
        /* Pre-process */
        try {
            /* Load all files */
            readAllFiles(fileLoader);
            /* Pre-process */
            createTemp(fileLoader);
            executePreProcesses(fileLoader);
            /* Main-process */
            executeTilingProcess();
            /* Post-process */
            executePostProcesses();
            /* Delete temp files */
            deleteTemp();
        } catch (InterruptedException e) {
            log.error("[ERROR][Pipeline] : ", e);
            throw new RuntimeException(e);
        }
    }

    private void readAllFiles(FileLoader fileLoader) {
        log.info("[Load] Start loading all files.");
        fileList = fileLoader.loadFiles();
        log.info("[Load] Finished loading {} files.", fileList.size());
    }

    private void executePreProcesses(FileLoader fileLoader) throws InterruptedException {
        log.info("[Pre] Start the pre-processing.");
        tileInfos = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(globalOptions.getMultiThreadCount());
        List<Runnable> tasks = new ArrayList<>();
        int fileCount = fileList.size();
        AtomicLong nodeCount = new AtomicLong(0);
        for (int count = 0; count < fileCount; count++) {
            File file = fileList.get(count);
            int finalCount = count;
            Runnable callableTask = () -> {
                try {
                    List<TileInfo> loadedTileInfos = fileLoader.loadTileInfo(file);
                    log.info("[Pre][{}/{}] Loading file : {}", finalCount + 1, fileCount, file.getName());
                    if (loadedTileInfos == null) {
                        log.warn("[WARN][Pre][{}/{}] Failed to load file : {}.", finalCount + 1, fileCount, file.getName());
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
                } catch (RuntimeException e) {
                    log.error("[ERROR][PreProcess] : ", e);
                }
            };
            tasks.add(callableTask);
        }
        executeThread(executorService, tasks);

        log.info("[Pre] Total Node Count {}", nodeCount);
        log.info("[Pre] End the pre-processing.");
    }

    private void executeTilingProcess() throws FileNotFoundException {
        log.info("[Tile] Start the tiling process.");
        Tiler tiler = (Tiler) tilingProcess;
        log.info("[Tile] Writing tileset file.");
        tileset = tiler.run(tileInfos);
        tiler.writeTileset(tileset);
        log.info("[Tile] End the tiling process.");
    }

    private void executePostProcesses() throws InterruptedException {
        log.info("[Post] Start the post-processing.");

        ExecutorService executorService = Executors.newFixedThreadPool(globalOptions.getMultiThreadCount());
        List<Runnable> tasks = new ArrayList<>();
        contentInfos = tileset.findAllContentInfo();
        AtomicInteger count = new AtomicInteger(1);
        int contentCount = contentInfos.size();
        globalOptions.setTileCount(contentCount);

        // Sort contentInfos by node code length to ensure parent nodes are processed before child nodes
        contentInfos.sort((c1, c2) -> c1.getNodeCode().length() - c2.getNodeCode().length());
        for (ContentInfo contentInfo : contentInfos) {
            Runnable callableTask = () -> {
                try {
                    log.info("[Post][{}/{}] post-process in progress : {}", count.getAndIncrement(), contentCount, contentInfo.getName());
                    List<TileInfo> tileInfos = contentInfo.getTileInfos();
                    List<TileInfo> tileInfosClone = tileInfos.stream()
                            .map((childTileInfo) -> TileInfo.builder()
                                    .scene(childTileInfo.getScene())
                                    .tileTransformInfo(childTileInfo.getTileTransformInfo())
                                    .scenePath(childTileInfo.getScenePath())
                                    .tempPath(childTileInfo.getTempPath())
                                    .transformMatrix(childTileInfo.getTransformMatrix())
                                    .boundingBox(childTileInfo.getBoundingBox())
                                    .pointCloud(childTileInfo.getPointCloud())
                                    /*.pointCloudOld(childTileInfo.getPointCloudOld())*/
                                    .build())
                            .collect(Collectors.toList());
                    contentInfo.setTileInfos(tileInfosClone);
                    for (PostProcess postProcessor : postProcesses) {
                        postProcessor.run(contentInfo);
                    }
                    contentInfo.deleteTexture();
                    tileInfosClone.clear();
                } catch (RuntimeException e) {
                    log.error("[ERROR][PostProcess] : ", e);
                }
            };
            tasks.add(callableTask);
        }
        executeThread(executorService, tasks);
        log.info("[Post] End the post-processing.");
    }

    private void createTemp(FileLoader fileLoader) {
        /* create temp directory */
        File tempFile = new File(globalOptions.getTempPath());
        if (!tempFile.exists() && tempFile.mkdirs()) {
            log.info("[Pre] Created temp directory in {}", tempFile.getAbsolutePath());
        }
        fileList = fileLoader.loadTemp(tempFile, fileList);
    }

    private void deleteTemp() throws IOException {
        if (globalOptions.isLeaveTemp()) {
            return;
        }

        /* delete temp directory */
        /*File tempFile = new File(globalOptions.getOutputPath(), "temp");
        if (tempFile.exists() && tempFile.isDirectory()) {
            FileUtils.deleteDirectory(tempFile);
        }*/

        File userTempFile = new File(globalOptions.getTempPath());
        if (userTempFile.exists() && userTempFile.isDirectory()) {
            FileUtils.deleteDirectory(userTempFile);
        }
    }

    private void executeThread(ExecutorService executorService, List<Runnable> tasks) throws InterruptedException {
        try {
            for (Runnable task : tasks) {
                Future<?> future = executorService.submit(task);
                if (globalOptions.isDebug()) {
                    future.get();
                }
            }
        } catch (Exception e) {
            log.error("[ERROR] Failed to execute thread.", e);
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
