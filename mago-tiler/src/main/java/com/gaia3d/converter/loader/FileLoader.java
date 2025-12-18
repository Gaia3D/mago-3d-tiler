package com.gaia3d.converter.loader;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;

import java.io.File;
import java.util.List;

public interface FileLoader {
    List<File> loadTemp(File tempPath, List<File> files);

    List<TileInfo> loadTileInfo(File file);

    List<File> loadFiles();

    List<GridCoverage2D> loadGridCoverages(File geoTiffPath, List<GridCoverage2D> coverages);

    default List<File> loadFileDefault() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        File inputFile = new File(globalOptions.getInputPath());

        if (inputFile.isFile()) {
            return List.of(inputFile);
        } else if (inputFile.isDirectory()) {
            boolean recursive = globalOptions.isRecursive();
            FormatType formatType = globalOptions.getInputFormat();
            String[] extensions = getExtensions(formatType);
            return (List<File>) FileUtils.listFiles(inputFile, extensions, recursive);
        } else {
            throw new IllegalArgumentException("Input path is neither a file nor a directory: " + globalOptions.getInputPath());
        }
    }

    default String[] getExtensions(FormatType formatType) {
        String[] extensions = new String[4];
        extensions[0] = formatType.getExtension().toLowerCase();
        extensions[1] = formatType.getExtension().toUpperCase();
        extensions[2] = formatType.getSubExtension().toLowerCase();
        extensions[3] = formatType.getSubExtension().toUpperCase();
        return extensions;
    }
}
