package com.gaia3d.process;

import com.gaia3d.converter.FileLoader;
import com.gaia3d.process.postprocess.PostProcess;
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
public class ProcessFlow implements Process {
    private List<PreProcess> preProcesses;
    private TileProcess tileProcess;
    private List<PostProcess> postProcesses;

    public void process(FileLoader fileLoader) throws IOException {
        List<TileInfo> tileInfos = new ArrayList<>();
        log.info("Start loading tile infos.");

        /* PreProcess */
        preprocess(fileLoader, tileInfos);
        //System.gc();

        /* TileProcess */
        Tileset tileset = tileprocess(tileInfos);
        //System.gc();

        /* PostProcess */
        postprocess(tileset);
        //System.gc();

        /* Delete Temp Directory */
        if (tileInfos.size() > 0) {
            tileInfos.get(0).deleteTemp();
        }
    }

    private void preprocess(FileLoader fileLoader, List<TileInfo> tileInfos) {
        List<File> fileList = fileLoader.loadFiles();
        log.info("Total file counts : {}", fileList.size());

        int count = 0;
        int size = fileList.size();
        for (File file : fileList) {
            count++;
            log.info("[File Loading] : {}/{} : {}", count, size, file);
            TileInfo tileInfo = fileLoader.loadTileInfo(file);
            if (tileInfo != null) {
                for (PreProcess preProcessors : preProcesses) {
                    preProcessors.run(tileInfo);
                }
                tileInfo.minimize();
                tileInfos.add(tileInfo);
            }
            if (count % 10000 == 0) {
                //System.gc();
            }
        }
    }

    private Tileset tileprocess(List<TileInfo> tileInfos) {
        Tiler tiler = (Tiler) tileProcess;
        Tileset tileset = tiler.run(tileInfos);
        tiler.writeTileset(tileset);
        return tileset;
    }

    private void postprocess(Tileset tileset) {
        List<ContentInfo> contentInfos = tileset.findAllContentInfo();
        for (ContentInfo contentInfo : contentInfos) {
            List<TileInfo> childTileInfos = contentInfo.getTileInfos();
            for (TileInfo tileInfo : childTileInfos) {
                tileInfo.maximize();
            }
            for (PostProcess postProcessor : postProcesses) {
                postProcessor.run(contentInfo);
            }
            contentInfo.deleteTexture();
            contentInfo.clear();
        }
    }
}
