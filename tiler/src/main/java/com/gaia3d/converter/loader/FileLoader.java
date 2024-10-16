package com.gaia3d.converter.loader;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import org.geotools.coverage.grid.GridCoverage2D;

import java.io.File;
import java.util.List;

public interface FileLoader {
    List<TileInfo> loadTileInfo(File file);

    List<File> loadFiles();

    List<GridCoverage2D> loadGridCoverages(List<GridCoverage2D> coverages);

    default String[] getExtensions(FormatType formatType) {
        String[] extensions = new String[4];
        extensions[0] = formatType.getExtension().toLowerCase();
        extensions[1] = formatType.getExtension().toUpperCase();
        extensions[2] = formatType.getSubExtension().toLowerCase();
        extensions[3] = formatType.getSubExtension().toUpperCase();
        return extensions;
    }
}
