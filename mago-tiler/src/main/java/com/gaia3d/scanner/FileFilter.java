package com.gaia3d.scanner;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;

public class FileFilter {

    public List<File> getFiles(File file, boolean isRecursive) {
        if (file.isDirectory()) {
            return findFiles(file, isRecursive);
        } else {
            return List.of(file);
        }
    }

    public List<File> findFiles(File directory, boolean recursive) {
        return FileUtils.listFiles(directory, null, recursive).stream().toList();
    }
}
