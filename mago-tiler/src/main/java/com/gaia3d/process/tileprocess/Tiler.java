package com.gaia3d.process.tileprocess;

import com.gaia3d.process.tileprocess.tile.tileset.Tileset;

public interface Tiler extends TilingProcess {
    void writeTileset(Tileset tileset);
}
