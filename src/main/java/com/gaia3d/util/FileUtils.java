package com.gaia3d.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FileUtils {

    public static boolean isFileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    public static void createAllFoldersIfNoExist(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.isDirectory()) {
            return;
        } else {
            if (!file.mkdirs()) {
                throw new RuntimeException("Failed to create folder: " + filePath);
            }
        }
    }

    public static String removeFileNameFromPath(String filePath) {
        File file = new File(filePath);
        return file.getParent();
    }

    public static void getFileNames(String folderPath, String extension, List<String> fileNames) {
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null) {
            log.warn("No files in the folder: " + folderPath);
            return;
        }
        for (File file : listOfFiles) {
            if (file.isFile()) {
                String fileName = file.getName();
                if (fileName.endsWith(extension)) {
                    fileNames.add(fileName);
                }
            }
        }
    }

    public static void getFolderNames(String folderPath, List<String> folderNames) {
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null) {
            log.warn("No files in the folder: " + folderPath);
            return;
        }
        for (File file : listOfFiles) {
            if (file.isDirectory()) {
                String folderName = file.getName();
                folderNames.add(folderName);
            }
        }
    }

    public static void getFilePathsByExtension(String folderPath, String extension, List<String> fileNames, boolean isRecursive) {
        List<String> currfileNames = new ArrayList<>();
        FileUtils.getFileNames(folderPath, extension, currfileNames);
        for (String fileName : currfileNames) {
            fileNames.add(folderPath + File.separator + fileName);
        }

        if (isRecursive) {
            List<String> folderNames = new ArrayList<>();
            FileUtils.getFolderNames(folderPath, folderNames);
            for (String folderName : folderNames) {
                String subFolderPath = folderPath + File.separator + folderName;
                FileUtils.getFilePathsByExtension(subFolderPath, extension, fileNames, isRecursive);
            }
        }
    }
}
