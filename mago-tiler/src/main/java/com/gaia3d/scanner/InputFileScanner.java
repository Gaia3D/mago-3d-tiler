package com.gaia3d.scanner;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

@Slf4j
public class InputFileScanner {

    public void scan(File path) {
        // Implementation for scanning input files
        boolean isRecursive = true;
        FileFilter fileFilter = new FileFilter();

        List<File> inputFiles = fileFilter.getFiles(path, isRecursive);

        /* Filter tileset.json files */
        List<File> tileset = inputFiles.stream()
                .filter(file -> file.getName().equalsIgnoreCase("tileset.json"))
                .toList();

        /* Filter GeoTiff files */
        List<File> tifFiles = inputFiles.stream()
                .filter(file -> file.getName().toLowerCase().endsWith(".tif") || file.getName().toLowerCase().endsWith(".tiff"))
                .toList();

        /* Filter PointCloud(.laz, .las) files */
        List<File> pointCloudFiles = inputFiles.stream().filter(file -> file.getName().toLowerCase().endsWith(".laz") || file.getName().toLowerCase().endsWith(".las"))
                .toList();

    }
}
