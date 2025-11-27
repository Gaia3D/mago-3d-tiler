package com.gaia3d.process.tileprocess;

import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;

import java.io.FileNotFoundException;
import java.util.List;

public interface TilingProcess {
    Tileset run(List<TileInfo> tileInfo) throws FileNotFoundException;

    void writeTileset(Tileset tileset);
}
