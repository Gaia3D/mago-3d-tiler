package com.gaia3d.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.lwjgl.BufferUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for image operations.
 * @author znkim
 * @since 1.0.0
 */
@Slf4j
public class ImageUtils {
    public static int getNearestPowerOfTwo(int value) {
        int power = 1;
        int powerDown = 1;
        while (power < value) {
            powerDown = power;
            power *= 2;
        }
        if (power - value < value - powerDown) {
            return power;
        } else {
            return powerDown;
        }
    }

    public static String getFormatNameByMimeType(String mimeType) {
        String formatName = null;
        switch (mimeType) {
            case "image/png":
                formatName = "png";
                break;
            case "image/jpeg":
                formatName = "jpeg";
                break;
            case "image/gif":
                formatName = "gif";
                break;
            case "image/bmp":
                formatName = "bmp";
                break;
            case "image/tiff":
                formatName = "tiff";
                break;
            case "image/x-icon":
                formatName = "ico";
                break;
            case "image/svg+xml":
                formatName = "svg";
                break;
            case "image/webp":
                formatName = "webp";
                break;
        }
        return formatName;
    }

    public static String getMimeTypeByExtension(String extension) {
        String mimeType;
        extension = extension.toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                mimeType = "image/jpeg";
                break;
            case "gif":
                mimeType = "image/gif";
                break;
            case "bmp":
                mimeType = "image/bmp";
                break;
            case "tiff":
            case "tif":
                mimeType = "image/tiff";
                break;
            case "ico":
                mimeType = "image/x-icon";
                break;
            case "svg":
                mimeType = "image/svg+xml";
                break;
            case "webp":
                mimeType = "image/webp";
                break;
            default:
                mimeType = "image/png";
                break;
        }
        return mimeType;
    }

    public static ByteBuffer readFile(File file, boolean flip) {
        Path path = file.toPath();
        try (var is = new BufferedInputStream(Files.newInputStream(path))) {
            int size = (int) Files.size(path);
            ByteBuffer byteBuffer = BufferUtils.createByteBuffer(size);

            int bufferSize = 8192;
            bufferSize = Math.min(size, bufferSize);
            byte[] buffer = new byte[bufferSize];
            while (buffer.length > 0 && is.read(buffer) != -1) {
                byteBuffer.put(buffer);
                if (is.available() < bufferSize) {
                    buffer = new byte[is.available()];
                }
            }
            if (flip)
                byteBuffer.flip();
            return byteBuffer;
        } catch (IOException e) {
            log.error("FileUtils.readBytes: " + e.getMessage());
        }
        return null;
    }

    public static File getChildFile(File parent, String path) {
        File file = new File(parent, path);
        String name = FilenameUtils.getBaseName(path);
        String ext = FilenameUtils.getExtension(path);
        if (file.exists() && file.isFile()) {
            return file;
        }
        file = new File(parent, name.toLowerCase() + "." + ext.toLowerCase());
        if (file.exists() && file.isFile()) {
            return file;
        }
        file = new File(parent, name.toUpperCase() + "." + ext.toUpperCase());
        if (file.exists() && file.isFile()) {
            return file;
        }
        file = new File(parent, name.toLowerCase() + "." + ext.toUpperCase());
        if (file.exists() && file.isFile()) {
            return file;
        }
        file = new File(parent, name.toUpperCase() + "." + ext.toLowerCase());
        if (file.exists() && file.isFile()) {
            return file;
        }
        return null;
    }

    public static File correctFile(File file) {
        File parentPath = file.getParentFile();
        String fileName = file.getName();
        String name = FilenameUtils.getBaseName(fileName);
        String ext = FilenameUtils.getExtension(fileName);
        if (file.exists() && file.isFile()) {
            return file;
        }
        file = new File(parentPath, name.toLowerCase() + "." + ext.toLowerCase());
        if (file.exists() && file.isFile()) {
            return file;
        }
        file = new File(parentPath, name.toUpperCase() + "." + ext.toUpperCase());
        if (file.exists() && file.isFile()) {
            return file;
        }
        file = new File(parentPath, name.toLowerCase() + "." + ext.toUpperCase());
        if (file.exists() && file.isFile()) {
            return file;
        }
        file = new File(parentPath, name.toUpperCase() + "." + ext.toLowerCase());
        if (file.exists() && file.isFile()) {
            return file;
        }
        return null;
    }

    public static File correctPath(File parent, File file) throws FileNotFoundException {
        // Check if the file exists
        if (file.exists() && file.isFile()) {
            return file;
        }

        // Original Path
        File input = file;
        File result = correctFile(file);
        if (result != null && result.exists() && result.isFile()) {
            log.debug("Original Path: " + file.getPath());
            log.debug("Corrected Path: " + result.getPath());
            return result;
        }

        input = new File(parent, file.getPath());
        result = correctFile(input);
        if (result != null && result.exists() && result.isFile()) {
            log.debug("Original Path: " + file.getPath());
            log.debug("Corrected Path: " + result.getPath());
            return result;
        }

        input = new File(parent, file.getName());
        result = correctFile(input);
        if (result != null && result.exists() && result.isFile()) {
            log.debug("Original Path: " + file.getPath());
            log.debug("Corrected Path: " + result.getPath());
            return result;
        }

        throw new FileNotFoundException("File not found : " + file.getPath());
    }
}
