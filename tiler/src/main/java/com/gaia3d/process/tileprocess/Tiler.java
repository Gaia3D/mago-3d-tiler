package com.gaia3d.process.tileprocess;

import com.gaia3d.process.tileprocess.tile.tileset.Tileset;

public interface Tiler extends TilingProcess {
    public void writeTileset(Tileset tileset);
}
