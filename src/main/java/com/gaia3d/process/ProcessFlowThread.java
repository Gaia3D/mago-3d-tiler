package com.gaia3d.process;

import com.gaia3d.converter.FileLoader;
import com.gaia3d.converter.TriangleFileLoader;
import com.gaia3d.process.postprocess.PostProcess;
import com.gaia3d.process.postprocess.thread.ProcessThreadPool;
import com.gaia3d.process.preprocess.PreProcess;
import com.gaia3d.process.tileprocess.Process;
import com.gaia3d.process.tileprocess.TileProcess;
import com.gaia3d.process.tileprocess.Tiler;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class ProcessFlowThread implements Process {
    private List<PreProcess> preProcesses;
    private TileProcess tileProcess;
    private List<PostProcess> postProcesses;

    public void process(FileLoader fileLoader) throws IOException {
        List<TileInfo> tileInfos = new ArrayList<>();
        log.info("[process] Start loading tile infos.");

        /* PreProcess */
        preprocess(fileLoader, tileInfos);

        /* TileProcess */
        Tileset tileset = tileprocess(tileInfos);

        /* PostProcess */
        postprocess(tileset);

        // Delete Temp Directory
        if (!tileInfos.isEmpty()) {
            tileInfos.get(0).deleteTemp();
        }
    }

    private void preprocess(FileLoader fileLoader, List<TileInfo> tileInfos) {
        List<File> fileList = fileLoader.loadFiles();
        log.info("Total {} files.", fileList.size());

        ProcessThreadPool pool = new ProcessThreadPool();
        try {
            pool.preProcessStart(tileInfos, fileList, fileLoader, preProcesses);
        } catch (InterruptedException e) {
            log.error("Error while pre processing.", e);
            throw new RuntimeException(e);
        }
        pool = null;
    }

    private Tileset tileprocess(List<TileInfo> tileInfos) {
        Tiler tiler = (Tiler) tileProcess;
        Tileset tileset = tiler.run(tileInfos);
        tiler.writeTileset(tileset);
        return tileset;
    }

    private void postprocess(Tileset tileset) {
        ProcessThreadPool pool = new ProcessThreadPool();
        List<ContentInfo> contentInfos = tileset.findAllContentInfo();
        try {
            pool.postProcessStart(contentInfos, postProcesses);
        } catch (InterruptedException e) {
            log.error("Error while post processing.", e);
            throw new RuntimeException(e);
        }
        pool = null;
    }
}
