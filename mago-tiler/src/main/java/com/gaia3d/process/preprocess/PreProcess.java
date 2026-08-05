package com.gaia3d.process.preprocess;

import com.gaia3d.process.tileprocess.tile.TileInfo;

public interface PreProcess {
    TileInfo run(TileInfo tileInfo);
}
