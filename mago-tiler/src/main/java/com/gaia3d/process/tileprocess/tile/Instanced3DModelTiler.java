package com.gaia3d.process.tileprocess.tile;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.command.mago.TilingMode;
import com.gaia3d.process.tileprocess.Tiler;
import com.gaia3d.process.tileprocess.TilesetBuildResult;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;

import java.io.FileNotFoundException;
import java.util.List;

public class Instanced3DModelTiler implements Tiler {
    private final Tiler delegate;

    public Instanced3DModelTiler() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        if (globalOptions.getTilingMode() == TilingMode.IMPLICIT) {
            this.delegate = new Instanced3DModelImplicitTiler();
        } else {
            this.delegate = new Instanced3DModelExplicitTiler();
        }
    }

    @Override
    public Tileset run(List<TileInfo> tileInfos) throws FileNotFoundException {
        return delegate.run(tileInfos);
    }

    @Override
    public TilesetBuildResult runWithResult(List<TileInfo> tileInfos) throws FileNotFoundException {
        return delegate.runWithResult(tileInfos);
    }

    @Override
    public void writeTileset(Tileset tileset) {
        delegate.writeTileset(tileset);
    }
}
