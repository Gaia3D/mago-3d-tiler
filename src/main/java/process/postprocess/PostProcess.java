package process.postprocess;

import process.tileprocess.tile.ContentInfo;

public interface PostProcess {
    ContentInfo run(ContentInfo tileInfo);
}
