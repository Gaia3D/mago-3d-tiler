package com.gaia3d.converter;

import com.gaia3d.process.tileprocess.tile.TileInfo;
import org.geotools.coverage.grid.GridCoverage2D;

import java.io.File;
import java.util.List;

public interface FileLoader {
    public List<TileInfo> loadTileInfo(File file);
    public List<File> loadFiles();
    public List<GridCoverage2D> loadGridCoverages(List<GridCoverage2D> coverages);
}
