package process;

import lombok.AllArgsConstructor;
import process.postprocess.PostProcess;
import process.preprocess.PreProcess;
import process.tileprocess.TileProcess;
import process.tileprocess.tile.ContentInfo;
import process.tileprocess.tile.TileInfo;
import process.tileprocess.Tiler;
import process.tileprocess.tile.tileset.Tileset;

import java.io.IOException;
import java.util.List;

@AllArgsConstructor
public class ProcessFlow {
    private List<PreProcess> preProcesses;
    private TileProcess tileProcess;
    private List<PostProcess> postProcesses;

    public void process(List<TileInfo> tileInfos) throws IOException {
        /* PreProcess */
        for (PreProcess preProcessors : preProcesses) {
            for (TileInfo tileInfo : tileInfos) {
                preProcessors.run(tileInfo);
            }
        }

        /* TileProcess */
        Tiler tiler = (Tiler) tileProcess;
        Tileset tileset = tiler.run(tileInfos);
        tiler.writeTileset(tileset);

        /* PostProcess */
        List<ContentInfo> contentInfos = tileset.findAllContentInfo();
        for (ContentInfo contentInfo : contentInfos) {
            for (PostProcess postProcessor : postProcesses) {
                postProcessor.run(contentInfo);
            }
            contentInfo.deleteTexture();
        }
    }
}
