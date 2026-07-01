package com.gaia3d.process.tileprocess;

import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;

public interface TilingProcess {
    Tileset run(List<TileInfo> tileInfo) throws FileNotFoundException;

    default TilesetBuildResult runWithResult(List<TileInfo> tileInfo) throws FileNotFoundException {
        Tileset tileset = run(tileInfo);
        if (tileset == null) {
            return new TilesetBuildResult(null, Collections.emptyList());
        }
        return new TilesetBuildResult(tileset, tileset.findAllContentInfo());
    }

    void writeTileset(Tileset tileset);
}
