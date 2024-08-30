package com.gaia3d.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

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
        return switch (mimeType) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpeg";
            case "image/gif" -> "gif";
            case "image/bmp" -> "bmp";
            case "image/tiff" -> "tiff";
            case "image/x-icon" -> "ico";
            case "image/svg+xml" -> "svg";
            case "image/webp" -> "webp";
            default -> null;
        };
    }

    public static String getMimeTypeByExtension(String extension) {
        String mimeType;
        extension = extension.toLowerCase();
        mimeType = switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "tiff", "tif" -> "image/tiff";
            case "ico" -> "image/x-icon";
            case "svg" -> "image/svg+xml";
            case "webp" -> "image/webp";
            default -> "image/png";
        };
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
            log.error("FileUtils.readBytes: {}", e.getMessage());
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
            log.debug("Original Path: {}", file.getPath());
            log.debug("Corrected Path: {}", result.getPath());
            return result;
        }

        input = new File(parent, file.getPath());
        result = correctFile(input);
        if (result != null && result.exists() && result.isFile()) {
            log.debug("Original Path: {}", file.getPath());
            log.debug("Corrected Path: {}", result.getPath());
            return result;
        }

        input = new File(parent, file.getName());
        result = correctFile(input);
        if (result != null && result.exists() && result.isFile()) {
            log.debug("Original Path: {}", file.getPath());
            log.debug("Corrected Path: {}", result.getPath());
            return result;
        }

        throw new FileNotFoundException("File not found : " + file.getPath());
    }

    public static int[] readImageSize(String imagePath) {
        //**********************************************************************************************
        // This function reads the size of an image file, without loading the entire image into memory.
        //----------------------------------------------------------------------------------------------
        File imageFile = new File(imagePath);

        int[] result = new int[2];
        result[0] = -1;
        result[1] = -1;

        if (!imageFile.exists()) {
            System.err.println( "File not found : " + imageFile.getAbsolutePath());
            return result;
        }

        if (!imageFile.canRead()) {
            System.err.println("File is not readable : " + imageFile.getAbsolutePath());
            return result;
        }

        try (ImageInputStream input = ImageIO.createImageInputStream(imageFile)) {
            if (input == null) {
                System.err.println("Failed to create ImageInputStream.");
                return result;
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(input);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                result[0] = width;
                result[1] = height;

                System.out.println("Width: " + width);
                System.out.println("Height: " + height);

                reader.dispose();

                return result;
            } else {
                System.err.println("No ImageReader found for the given format.");
            }
        } catch (IOException e) {
            log.error("Error reading image size: {}", e.getMessage());
        }

        return result;
    }
}
