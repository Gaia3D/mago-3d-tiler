package com.gaia3d.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

@Slf4j
public class GaiaFileUtils {
    public static void deleteAll(String path) {
        FileUtils.deleteQuietly(new File(path));
    }
    public static void deleteAll(File file) {
        FileUtils.deleteQuietly(file);
    }
    public static void copyAll(File srcDir, File destDir) throws IOException {
        if (srcDir.exists()) {
            if (srcDir.isDirectory()) {
                FileUtils.copyDirectory(srcDir, destDir);
            } else {
                FileUtils.copyFile(srcDir, destDir);
            }
        }
    }
    public static void moveAll(File srcDir, File destDir) throws IOException {
        if (srcDir.exists()) {
            if (srcDir.isDirectory()) {
                FileUtils.moveDirectory(srcDir, destDir);
            } else {
                FileUtils.moveFile(srcDir, destDir);
            }
        }
    }
    public static void makeDirectory(String path) {
        File file = new File(path);
        if (!file.exists() && file.mkdirs()) {
            log.info("Created new directory in {}", path);
        }
    }
}
