package com.gaia3d.process.tileprocess;

import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;

import java.util.List;

public interface TileProcess {
    Tileset run(List<TileInfo> tileInfo);

    public void writeTileset(Tileset tileset);
}
