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
public class PointCloudProcessFlow implements Process {
    private List<PreProcess> preProcesses;
    private TileProcess tileProcess;
    private List<PostProcess> postProcesses;

    public void process(FileLoader fileLoader) throws IOException {
        List<TileInfo> tileInfos = new ArrayList<>();
        log.info("[process] Start loading tile infos.");
        preprocess(fileLoader, tileInfos);
        Tileset tileset = tileprocess(tileInfos);
        postprocess(tileset);
        if (!tileInfos.isEmpty()) {
            tileInfos.get(0).deleteTemp();
        }
    }

    private void preprocess(FileLoader fileLoader, List<TileInfo> tileInfos) {
        List<File> fileList = fileLoader.loadFiles();
        log.info("[pre-process] Total file counts : {}", fileList.size());
        int count = 0;
        int size = fileList.size();
        for (File file : fileList) {
            count++;
            log.info("[load] : {}/{} : {}", count, size, file);
            List<TileInfo> tileInfoResult = fileLoader.loadTileInfo(file);
            int serial = 0;
            for (TileInfo tileInfo : tileInfoResult) {
                if (tileInfo != null) {
                    log.info("[{}/{}][{}/{}] load tile...", count, size, serial, tileInfoResult.size());
                    for (PreProcess preProcessors : preProcesses) {
                        preProcessors.run(tileInfo);
                    }
                    serial++;
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
        List<ContentInfo> contentInfos = tileset.findAllContentInfo();
        int count = 0;
        int size = contentInfos.size();
        for (ContentInfo contentInfo : contentInfos) {
            count++;
            log.info("[post-process][{}/{}] content-info : {}", count, size, contentInfo.getName());
            for (PostProcess postProcessor : postProcesses) {
                postProcessor.run(contentInfo);
            }
        }
    }
}
