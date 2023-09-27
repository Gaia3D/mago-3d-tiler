package com.gaia3d.converter;

import com.gaia3d.process.tileprocess.tile.TileInfo;

import java.io.File;
import java.util.List;

public interface FileLoader {
    public List<TileInfo> loadTileInfo(File file);
    public List<File> loadFiles();
}
