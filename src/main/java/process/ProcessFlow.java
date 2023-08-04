package process;

import converter.FileLoader;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import process.postprocess.PostProcess;
import process.preprocess.PreProcess;
import process.tileprocess.TileProcess;
import process.tileprocess.Tiler;
import process.tileprocess.tile.ContentInfo;
import process.tileprocess.tile.TileInfo;
import process.tileprocess.tile.tileset.Tileset;

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
        /* PreProcess */
        int count = 0;
        int size = fileList.size();
        for (File file : fileList) {
            count++;
            log.info("[{}/{}] load tile info: {}", count, size, file);
            TileInfo tileInfo = fileLoader.loadTileInfo(file);
            if (tileInfo != null) {
                for (PreProcess preProcessors : preProcesses) {
                    preProcessors.run(tileInfo);
                }
                //tileInfo.minimize();
                tileInfos.add(tileInfo);
            }
        }
        System.gc();

        /* TileProcess */
        Tiler tiler = (Tiler) tileProcess;
        Tileset tileset = tiler.run(tileInfos);
        tiler.writeTileset(tileset);
        System.gc();

        /* PostProcess */
        List<ContentInfo> contentInfos = tileset.findAllContentInfo();
        for (ContentInfo contentInfo : contentInfos) {
            List<TileInfo> childTileInfos = contentInfo.getTileInfos();
            for (TileInfo tileInfo : childTileInfos) {
                tileInfo.maximize(fileLoader);
            }

            for (PostProcess postProcessor : postProcesses) {
                postProcessor.run(contentInfo);
            }
            contentInfo.deleteTexture();
            contentInfo.clear();
            //System.gc();
        }
    }
}
