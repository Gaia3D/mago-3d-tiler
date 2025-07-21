package com.gaia3d.process.postprocess;

import com.gaia3d.process.tileprocess.tile.ContentInfo;

public interface PostProcess {
    ContentInfo run(ContentInfo contentInfo);
}
