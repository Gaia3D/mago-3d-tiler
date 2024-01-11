package com.gaia3d.process;

import com.gaia3d.command.mago.GlobalOptions;
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

        /* PreProcess */
        preprocess(fileLoader, tileInfos);
        /* TileProcess */
        Tileset tileset = tileprocess(tileInfos);
        /* PostProcess */
        postprocess(tileset);

        /* Delete temp files */
        if (!tileInfos.isEmpty()) {
            tileInfos.get(0).deleteTemp();
        }
    }

    private void preprocess(FileLoader fileLoader, List<TileInfo> tileInfos) {
        List<File> fileList = fileLoader.loadFiles();
        log.info("[pre-process] Total input file count : {}", fileList.size());
        int count = 0;
        int size = fileList.size();
        for (File file : fileList) {
            count++;
            log.info("[File][load] : {}/{} : {}", count, size, file);
            List<TileInfo> tileInfoResult = fileLoader.loadTileInfo(file);
            int serial = 1;
            for (TileInfo tileInfo : tileInfoResult) {
                if (tileInfo != null) {
                    log.info("[{}/{}][{}/{}] load TileInfo...", count, size, serial, tileInfoResult.size());
                    for (PreProcess preProcessors : preProcesses) {
                        preProcessors.run(tileInfo);
                    }
                    tileInfo.minimize(serial++);
                    tileInfos.add(tileInfo);
                }
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
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        List<ContentInfo> contentInfos = tileset.findAllContentInfo();
        int count = 0;
        int size = contentInfos.size();
        globalOptions.setTileCount(size);
        for (ContentInfo contentInfo : contentInfos) {
            count++;
            log.info("[B3DM][{}/{}] post-process : {}", count, size, contentInfo.getName());
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
