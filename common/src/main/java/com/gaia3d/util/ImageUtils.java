package com.gaia3d.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
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
 */
@SuppressWarnings("ALL")
@Slf4j
public class ImageUtils {

    private final int MAX_IMAGE_SIZE = 16384;
    private final int MIN_IMAGE_SIZE = 32;

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

    public static int getNearestPowerOfTwoHigher(int value) {
        int power = 1;
        while (power < value) {
            power *= 2;
        }
        setMinMaxSize(power);
        return power;
    }

    public static int getNearestPowerOfTwoLower(int value) {
        int power = 1;
        int powerDown = 1;
        while (power < value) {
            powerDown = power;
            power *= 2;
        }
        setMinMaxSize(powerDown);
        return powerDown;
    }

    public static int setMinMaxSize(int size) {
        return Math.min(Math.max(size, 32), 16384);
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
            ByteBuffer byteBuffer = ByteBuffer.allocate(size);

            int bufferSize = 8192;
            bufferSize = Math.min(size, bufferSize);
            byte[] buffer = new byte[bufferSize];
            while (buffer.length > 0 && is.read(buffer) != -1) {
                byteBuffer.put(buffer);
                if (is.available() < bufferSize) {
                    buffer = new byte[is.available()];
                }
            }
            if (flip) byteBuffer.flip();
            return byteBuffer;
        } catch (IOException e) {
            log.error("[ERROR] :", e);
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

        throw new FileNotFoundException("File not found : " + file.getAbsolutePath());
    }

    public static int[] readImageSize(String imagePath) {
        File imageFile = new File(imagePath);

        int[] result = new int[2];
        result[0] = -1;
        result[1] = -1;

        if (!imageFile.exists()) {
            System.err.println("File not found : " + imageFile.getAbsolutePath());
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

                log.info("Width: " + width);
                log.info("Height: " + height);

                reader.dispose();

                return result;
            } else {
                System.err.println("No ImageReader found for the given format.");
            }
        } catch (IOException e) {
            log.error("[ERROR] :", e);
        }

        return result;
    }

    public static float unpackDepth32(float[] packedDepth) {
        if (packedDepth.length != 4) {
            throw new IllegalArgumentException("packedDepth debe tener exactamente 4 elementos.");
        }

        // Ajuste del valor final (equivalente a packedDepth - 1.0 / 512.0)
        for (int i = 0; i < 4; i++) {
            packedDepth[i] -= 1.0f / 512.0f;
        }

        // Producto punto para recuperar la profundidad original
        return packedDepth[0] + packedDepth[1] / 256.0f + packedDepth[2] / (256.0f * 256.0f) + packedDepth[3] / 16777216.0f;
    }

    public static float[][] bufferedImageToFloatMatrix(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        float[][] floatMatrix = new float[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Color color = new Color(image.getRGB(i, j), true);
                float r = color.getRed() / 255.0f;
                float g = color.getGreen() / 255.0f;
                float b = color.getBlue() / 255.0f;
                float a = color.getAlpha() / 255.0f;

                float depth = unpackDepth32(new float[]{r, g, b, a});
                floatMatrix[i][j] = depth;
            }
        }

        return floatMatrix;
    }

    public static BufferedImage invertImageY(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage newImage = new BufferedImage(width, height, image.getType());
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                newImage.setRGB(i, height - j - 1, image.getRGB(i, j));
            }
        }
        return newImage;
    }

    public static BufferedImage clampBackGroundColor(BufferedImage image, Color backGroundColor, int borderSize, int iterations) {
        //log.debug("Clamp Background Color");
        int width = image.getWidth();
        int height = image.getHeight();
        int noBackGroundColor = 0;
        int it = 0;
        boolean changed = false;

        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        BufferedImage oldImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

        // fill the new image with the background color
        Graphics2D graphics = newImage.createGraphics();
        graphics.setColor(backGroundColor);
        // copy the image to the new image
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();

        graphics = oldImage.createGraphics();
        graphics.setColor(backGroundColor);
        // copy the image to the new image
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();

        while(it < iterations) {
            changed = false;
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    Color pixel = new Color(oldImage.getRGB(i, j), false);
                    // now check if pixel is background color
                    if (pixel.equals(backGroundColor)) {
                        // take a pixelMatrix of 5x5 around the pixel
                        for (int x = i - borderSize; x <= i + borderSize; x++) {
                            for (int y = j - borderSize; y <= j + borderSize; y++) {
                                if (x >= 0 && x < width && y >= 0 && y < height) {
                                    noBackGroundColor = oldImage.getRGB(x, y);
                                    if (!new Color(noBackGroundColor, false).equals(backGroundColor)) {
                                        newImage.setRGB(i, j, noBackGroundColor);
                                        changed = true;
                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        newImage.setRGB(i, j, newImage.getRGB(i, j));
                    }
                }
            }

            graphics = oldImage.createGraphics();
            graphics.setColor(backGroundColor);
            // copy the image to the new image
            graphics.drawImage(newImage, 0, 0, null);
            graphics.dispose();

            if (!changed) {
                break;
            }
            it++;
        }

        return newImage;
    }

    public static BufferedImage changeBackgroundColor(BufferedImage image, Color oldColor, Color newColor) {
        log.debug("Change Background Color");
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage newImage = new BufferedImage(width, height, image.getType());
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Color pixel = new Color(image.getRGB(i, j), true);
                if (pixel.getRGB() == oldColor.getRGB()) {
                    newImage.setRGB(i, j, newColor.getRGB());
                } else {
                    newImage.setRGB(i, j, image.getRGB(i, j));
                }
            }
        }
        newImage.flush();
        return newImage;
    }

    public void saveBufferedImage(BufferedImage image, String format, String path) {
        try {
            File file = new File(path);
            ImageIO.write(image, format, new File(path));
        } catch (IOException e) {
            log.error("[ERROR] :", e);
        }
    }
}
