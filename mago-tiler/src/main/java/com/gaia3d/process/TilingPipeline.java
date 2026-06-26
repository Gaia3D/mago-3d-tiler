package com.gaia3d.process;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.assimp.validation.GaiaSceneValidationReportCollector;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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
    private static final DateTimeFormatter TEMP_BACKUP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
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
            /* Write validation summary if requested */
            if (globalOptions.isValidationReport()) {
                writeValidationSummary();
            }
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
        tileInfos = Collections.synchronizedList(new ArrayList<>());

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
                    boolean manyTiles = infoLength > 100000;
                    int percentageStep = infoLength / 100;
                    nodeCount.addAndGet(infoLength);
                    for (int index = 0; index < infoLength; index++) {
                        TileInfo tileInfo = loadedTileInfos.get(index);
                        if (tileInfo != null) {
                            if (manyTiles) {
                                if (index % percentageStep == 0 || index == infoLength - 1) {
                                    int percent = (index / percentageStep);
                                    log.info("[Pre][{}/{}][{}/{}] Processing tile info. {}%", finalCount + 1, fileCount, index + 1, infoLength, percent);
                                }
                            } else {
                                log.info("[Pre][{}/{}][{}/{}] Processing tile info.", finalCount + 1, fileCount, index + 1, infoLength);
                            }
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
        backupExistingTempDirectory(tempFile);
        if (!tempFile.exists() && tempFile.mkdirs()) {
            log.info("[Pre] Created temp directory in {}", tempFile.getAbsolutePath());
        }
        fileList = fileLoader.loadTemp(tempFile, fileList);
    }

    private void backupExistingTempDirectory(File tempFile) {
        if (!tempFile.exists() || !tempFile.isDirectory()) {
            return;
        }

        File[] children = tempFile.listFiles();
        if (children == null || children.length == 0) {
            return;
        }

        String backupName = tempFile.getName() + "_backup_" + LocalDateTime.now().format(TEMP_BACKUP_FORMAT);
        Path backupPath = tempFile.toPath().resolveSibling(backupName);
        int duplicateIndex = 1;
        while (Files.exists(backupPath)) {
            backupPath = tempFile.toPath().resolveSibling(backupName + "_" + duplicateIndex++);
        }

        try {
            Files.move(tempFile.toPath(), backupPath, StandardCopyOption.ATOMIC_MOVE);
            log.warn("[Pre] Existing temp directory was moved to {}", backupPath);
        } catch (IOException atomicMoveException) {
            try {
                Files.move(tempFile.toPath(), backupPath);
                log.warn("[Pre] Existing temp directory was moved to {}", backupPath);
            } catch (IOException moveException) {
                throw new RuntimeException("Failed to move existing temp directory: " + tempFile.getAbsolutePath(), moveException);
            }
        }
    }

    private void writeValidationSummary() {
        GaiaSceneValidationReportCollector collector = GaiaSceneValidationReportCollector.getInstance();
        try {
            collector.writeSummary(new File(globalOptions.getOutputPath()));
        } catch (IOException e) {
            log.error("[ERROR][Validation] Failed to write validation summary: {}", e.getMessage());
        } finally {
            collector.reset();
        }
    }

    private void deleteTemp() {
        if (globalOptions.isLeaveTemp()) {
            return;
        }

        File userTempFile = new File(globalOptions.getTempPath());
        if (userTempFile.exists() && userTempFile.isDirectory()) {
            try {
                FileUtils.deleteDirectory(userTempFile);
            } catch (Exception e) {
                log.error("[WARN] Failed to delete temp directory in {}", userTempFile.getAbsolutePath(), e);
            }
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
