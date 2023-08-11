package com.gaia3d.process;

import com.gaia3d.converter.FileLoader;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.gaia3d.process.postprocess.PostProcess;
import com.gaia3d.process.postprocess.thread.TestPool;
import com.gaia3d.process.preprocess.PreProcess;
import com.gaia3d.process.tileprocess.TileProcess;
import com.gaia3d.process.tileprocess.Tiler;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class ProcessFlow {
    private List<PreProcess> preProcesses;
    private TileProcess tileProcess;
    private List<PostProcess> postProcesses;

    public void process(FileLoader fileLoader) throws IOException {
        List<TileInfo> tileInfos = new ArrayList<>();
        log.info("Start loading tile infos.");
        List<File> fileList = fileLoader.loadFiles();
        log.info("Total {} files.", fileList.size());

        TestPool testPool = new TestPool();

        /* PreProcess */
        try {
            testPool.preProcessStart(tileInfos, fileList, fileLoader, preProcesses);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        /*int count = 0;
        int size = fileList.size();
        for (File file : fileList) {
            count++;
            log.info("[{}/{}] load tile info: {}", count, size, file);
            TileInfo tileInfo = fileLoader.loadTileInfo(file);
            if (tileInfo != null) {
                for (PreProcess preProcessors : preProcesses) {
                    preProcessors.run(tileInfo);
                }
                tileInfo.minimize();
                tileInfos.add(tileInfo);
            }
            if (count % 1000 == 0) {
                System.gc();
            }
        }*/
        System.gc();

        /* TileProcess */
        Tiler tiler = (Tiler) tileProcess;
        Tileset tileset = tiler.run(tileInfos);
        tiler.writeTileset(tileset);
        //System.gc();

        /* PostProcess */
        List<ContentInfo> contentInfos = tileset.findAllContentInfo();
        try {
            testPool.postProcessStart(contentInfos, postProcesses);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        /*for (ContentInfo contentInfo : contentInfos) {
            List<TileInfo> childTileInfos = contentInfo.getTileInfos();
            for (TileInfo tileInfo : childTileInfos) {
                tileInfo.maximize();
            }
            for (PostProcess postProcessor : postProcesses) {
                postProcessor.run(contentInfo);
            }
            contentInfo.deleteTexture();
            contentInfo.clear();
        }*/

        // Delete Temp Directory
        if (tileInfos.size() > 0) {
            tileInfos.get(0).deleteTemp();
        }
    }
}
