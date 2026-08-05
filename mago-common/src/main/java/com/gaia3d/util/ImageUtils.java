package com.gaia3d.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.*;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
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
import java.util.Locale;
import java.util.concurrent.Semaphore;

/**
 * Utility class for image operations.
 */
@SuppressWarnings("ALL")
@Slf4j
@UtilityClass
public class ImageUtils {

    private final int MAX_IMAGE_SIZE = 16384;
    private final int MIN_IMAGE_SIZE = 32;
    private static final Semaphore IMAGE_SAVE_PERMITS =
            new Semaphore(2);

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
            if (flip) {byteBuffer.flip();}
            return byteBuffer;
        } catch (IOException e) {
            log.error("[ERROR] :", e);
        }
        return null;
    }

    public static File getChildFile(File parent, String path) {
        File file = resolveCaseInsensitive(parent, new File(path));
        return file != null && file.isFile() ? file : null;
    }

    public static String getChildPath(File parent, String path) {
        File file = getChildFile(parent, path);
        if (file == null) {
            return null;
        }
        return getRelativePath(parent, file);
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
        result = resolveCaseInsensitive(parent, file);
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

    private static File resolveCaseInsensitive(File parent, File file) {
        File input = file.isAbsolute() ? file : new File(parent, file.getPath());
        if (input.exists()) {
            return input;
        }

        File correctedFile = correctFile(input);
        if (correctedFile != null) {
            return correctedFile;
        }

        File root = file.isAbsolute() ? input.toPath().getRoot().toFile() : parent;
        File resolved = root;
        Path relativePath = file.isAbsolute() ? input.toPath().getRoot().relativize(input.toPath()) : file.toPath();
        for (Path segmentPath : relativePath) {
            String segment = segmentPath.toString();
            File exact = new File(resolved, segment);
            if (exact.exists()) {
                resolved = exact;
                continue;
            }

            File[] children = resolved.listFiles();
            if (children == null) {
                return null;
            }

            File matched = null;
            for (File child : children) {
                if (child.getName().equalsIgnoreCase(segment)) {
                    matched = child;
                    break;
                }
            }
            if (matched == null) {
                return null;
            }
            resolved = matched;
        }
        return resolved.exists() ? resolved : null;
    }

    private static String getRelativePath(File parent, File child) {
        try {
            Path parentPath = parent.toPath().toAbsolutePath().normalize();
            Path childPath = child.toPath().toAbsolutePath().normalize();
            return parentPath.relativize(childPath).toString();
        } catch (IllegalArgumentException e) {
            return child.getName();
        }
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

        while (it < iterations) {
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

    public static BufferedImage fillColor(BufferedImage image, Color newColor) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage newImage = new BufferedImage(width, height, image.getType());
        //boolean hasAlpha = image.getColorModel().hasAlpha();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                newImage.setRGB(i, j, newColor.getRGB());
            }
        }
        newImage.flush();
        return newImage;
    }

    public static BufferedImage changeBackgroundColor(
            BufferedImage image,
            Color oldColor,
            Color newColor
    ) {
        if (image == null || oldColor == null || newColor == null) {
            return image;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        int oldRGB = oldColor.getRGB() & 0x00FFFFFF;
        int newRGB = newColor.getRGB() & 0x00FFFFFF;

        boolean hasAlpha = image.getColorModel().hasAlpha();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int rgb = argb & 0x00FFFFFF;

                if (rgb == oldRGB) {
                    if (hasAlpha) {
                        int alpha = argb & 0xFF000000;
                        image.setRGB(x, y, alpha | newRGB);
                    } else {
                        image.setRGB(x, y, 0xFF000000 | newRGB);
                    }
                }
            }
        }

        return image;
    }

//    public static void saveBufferedImage(BufferedImage image, String format, String path) {
//        try {
//            File file = new File(path);
//            ImageIO.write(image, format, new File(path));
//        } catch (IOException e) {
//            log.error("[ERROR] :", e);
//        }
//    }

    /**
     * @param savePath Ruta de destino.
     * @param fastPng true para priorizar velocidad sobre tamaño del PNG.
     * @param jpegQuality Calidad JPEG entre 0.0 y 1.0.
     * @return true cuando la imagen se escribió correctamente.
     */
    public static boolean saveBufferedImage(
            BufferedImage bufferedImage,
            String savePath,
            boolean fastPng,
            float jpegQuality
    ) {
        if (bufferedImage == null) {
            log.warn("GaiaTexture.saveImage(): bufferedImage is null.");
            return false;
        }

        if (savePath == null || savePath.isBlank()) {
            log.warn("GaiaTexture.saveImage(): savePath is null or empty.");
            return false;
        }

        String imageFormat = getImageFormat(savePath);

        if (imageFormat == null) {
            log.error(
                    "GaiaTexture.saveImage(): destination has no valid extension: {}",
                    savePath
            );
            return false;
        }

        Path outputPath = Path.of(savePath);
        Path parent = outputPath.getParent();

        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            log.error(
                    "GaiaTexture.saveImage(): could not create directory: {}",
                    parent,
                    e
            );
            return false;
        }

        Iterator<ImageWriter> writers =
                ImageIO.getImageWritersByFormatName(imageFormat);

        if (!writers.hasNext()) {
            log.error(
                    "GaiaTexture.saveImage(): no ImageWriter found for format: {}",
                    imageFormat
            );
            return false;
        }

        ImageWriter writer = writers.next();

        try (
                ImageOutputStream output =
                        new FileImageOutputStream(outputPath.toFile())
        ) {
            writer.setOutput(output);

            ImageWriteParam writeParam =
                    writer.getDefaultWriteParam();

            configureCompression(
                    writeParam,
                    imageFormat,
                    fastPng,
                    jpegQuality
            );

            IIOImage outputImage = new IIOImage(
                    bufferedImage,
                    null,
                    null
            );

            writer.write(
                    null,
                    outputImage,
                    writeParam
            );

            output.flush();
            return true;

        } catch (IOException | RuntimeException e) {
            log.error(
                    "GaiaTexture.saveImage(): failed to write image: {}",
                    savePath,
                    e
            );
            return false;

        } finally {
            writer.dispose();
        }
    }

    public static boolean saveBufferedImageControlled(
            BufferedImage image,
            String savePath,
            boolean fastPng,
            float jpegQuality
    ) {
        boolean acquired = false;

        try {
            IMAGE_SAVE_PERMITS.acquire();
            acquired = true;

            return saveBufferedImage(
                    image,
                    savePath,
                    fastPng,
                    jpegQuality
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Interrupted while waiting to save image: "
                            + savePath,
                    e
            );

        } finally {
            if (acquired) {
                IMAGE_SAVE_PERMITS.release();
            }
        }
    }

    private static void configureCompression(
            ImageWriteParam writeParam,
            String imageFormat,
            boolean fastPng,
            float jpegQuality
    ) {
        if (!writeParam.canWriteCompressed()) {
            return;
        }

        writeParam.setCompressionMode(
                ImageWriteParam.MODE_EXPLICIT
        );

        /*
         * Algunos writers exigen seleccionar un tipo de compresión
         * antes de configurar su calidad.
         */
        String[] compressionTypes =
                writeParam.getCompressionTypes();

        if (compressionTypes != null
                && compressionTypes.length > 0
                && writeParam.getCompressionType() == null) {

            writeParam.setCompressionType(
                    compressionTypes[0]
            );
        }

        switch (imageFormat) {
            case "png" -> {
                /*
                 * PNG continúa siendo lossless.
                 *
                 * Un valor alto prioriza velocidad y menor trabajo
                 * de compresión, normalmente a cambio de archivos
                 * de mayor tamaño.
                 */
                if (fastPng) {
                    writeParam.setCompressionQuality(0.9f);
                }
            }

            case "jpg", "jpeg" -> {
                float clampedQuality = Math.max(
                        0.0f,
                        Math.min(1.0f, jpegQuality)
                );

                writeParam.setCompressionQuality(
                        clampedQuality
                );

                if (writeParam.canWriteProgressive()) {
                    writeParam.setProgressiveMode(
                            ImageWriteParam.MODE_DISABLED
                    );
                }
            }

            default -> {
                /*
                 * Mantener la configuración explícita predeterminada
                 * del writer para otros formatos.
                 */
            }
        }
    }

    private static String getImageFormat(String savePath) {
        int dotIndex = savePath.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == savePath.length() - 1) {
            return null;
        }

        String extension = savePath
                .substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);

        return switch (extension) {
            case "jpg", "jpeg" -> "jpeg";
            case "png" -> "png";
            case "bmp" -> "bmp";
            case "gif" -> "gif";
            default -> extension;
        };
    }

    public static boolean isImageFullyTransparent(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = img.getRGB(x, y);  // ARGB packed in int
                int alpha = (argb >> 24) & 0xff;
                int red = (argb >> 16) & 0xff;
                int green = (argb >> 8) & 0xff;
                int blue = (argb) & 0xff;

                // Si no es completamente transparente (0,0,0,0), retorna false
//                if (!(alpha == 0 && red == 0 && green == 0 && blue == 0)) {
//                    return false;
//                }

                if (!(alpha == 0)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static BufferedImage correctGammaSaturation(BufferedImage src, double gamma, float saturationFactor) {
        int w = src.getWidth();
        int h = src.getHeight();

        boolean hasAlpha = src.getColorModel().hasAlpha();

        BufferedImage result = new BufferedImage(
                w,
                h,
                hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB
        );

        // -------- LUT gamma --------
        double invGamma = 1.0 / gamma;
        int[] gammaLUT = new int[256];
        for (int i = 0; i < 256; i++) {
            gammaLUT[i] = (int) (Math.pow(i / 255.0, invGamma) * 255.0 + 0.5);
        }

        float[] hsb = new float[3];

        // -------- Loop principal --------
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                int argb = src.getRGB(x, y);

                int a = (argb >>> 24);

                // 1️⃣ Gamma primero
                int r = gammaLUT[(argb >> 16) & 0xff];
                int g = gammaLUT[(argb >> 8) & 0xff];
                int b = gammaLUT[argb & 0xff];

                // 2️⃣ Contraste suave tipo HDR natural
                r = clamp((int) ((r - 128) * 1.05 + 128));
                g = clamp((int) ((g - 128) * 1.05 + 128));
                b = clamp((int) ((b - 128) * 1.05 + 128));

                // 3️⃣ Pasar a HSB
                Color.RGBtoHSB(r, g, b, hsb);

                float hue = hsb[0];
                float sat = hsb[1];
                float bri = hsb[2];

                // 4️⃣ Saturación global suave
                sat *= saturationFactor;

                // 5️⃣ Mejora específica de verdes (árboles)
                if (hue > 0.25f && hue < 0.45f && sat > 0.15f) {

                    // más vida al verde
                    sat *= 1.10f;
                    if (sat > 1f) {sat = 1f;}

                    // verde más claro natural
                    bri += (1f - bri) * 0.08f;

                    // pequeño shift para evitar verde oliva
                    hue -= 0.01f;
                    if (hue < 0f) {hue += 1f;}
                }

                if (sat > 1f) {sat = 1f;}
                if (bri > 1f) {bri = 1f;}

                int rgb = Color.HSBtoRGB(hue, sat, bri);

                int newArgb = (a << 24) | (rgb & 0x00ffffff);
                result.setRGB(x, y, newArgb);
            }
        }
        return result;
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    public static BufferedImage correctGammaSaturation_original(BufferedImage src, double gamma, float saturationFactor) {
        int w = src.getWidth();
        int h = src.getHeight();

        boolean hasAlpha = src.getColorModel().hasAlpha();

        BufferedImage result = new BufferedImage(
                w,
                h,
                hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB
        );

        // Precompute gamma correction lookup table
        double invGamma = 1.0 / gamma;
        int[] gammaLUT = new int[256];
        for (int i = 0; i < 256; i++) {
            //gammaLUT[i] = (int) Math.min(255, Math.max(0, Math.pow(i / 255.0, gamma) * 255.0 + 0.5));
            gammaLUT[i] = (int) (Math.pow(i / 255.0, invGamma) * 255.0 + 0.5);
        }

        float[] hsb = new float[3];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                int argb = src.getRGB(x, y);

                int a = (argb >>> 24);
                int r = gammaLUT[(argb >> 16) & 0xff];
                int g = gammaLUT[(argb >> 8) & 0xff];
                int b = gammaLUT[argb & 0xff];

                // --- RGB → HSB ---
                Color.RGBtoHSB(r, g, b, hsb);

                // subir saturación
                hsb[1] *= saturationFactor;
                if (hsb[1] > 1f) {hsb[1] = 1f;}

                int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);

                int newArgb = (a << 24) | (rgb & 0x00ffffff);
                result.setRGB(x, y, newArgb);
            }
        }
        return result;
    }

    public static BufferedImage expandWithBorderFast(BufferedImage src, int n, boolean bordeCopiado) {
        int w = src.getWidth();
        int h = src.getHeight();
        int newW = w + 2 * n;
        int newH = h + 2 * n;

        BufferedImage expanded = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        // Copiar centro
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = src.getRGB(x, y);
                expanded.setRGB(x + n, y + n, argb);
            }
        }

        if (bordeCopiado) {
            // Copiar bordes horizontales (arriba y abajo)
            for (int x = 0; x < w; x++) {
                int topPixel = src.getRGB(x, 0);
                int bottomPixel = src.getRGB(x, h - 1);
                for (int k = 0; k < n; k++) {
                    expanded.setRGB(x + n, k, topPixel);                  // arriba
                    expanded.setRGB(x + n, newH - n + k, bottomPixel);    // abajo
                }
            }

            // Copiar bordes verticales (izquierda y derecha)
            for (int y = 0; y < h; y++) {
                int leftPixel = src.getRGB(0, y);
                int rightPixel = src.getRGB(w - 1, y);
                for (int k = 0; k < n; k++) {
                    expanded.setRGB(k, y + n, leftPixel);                 // izquierda
                    expanded.setRGB(newW - n + k, y + n, rightPixel);     // derecha
                }
            }

            // Copiar esquinas
            int topLeft = src.getRGB(0, 0);
            int topRight = src.getRGB(w - 1, 0);
            int bottomLeft = src.getRGB(0, h - 1);
            int bottomRight = src.getRGB(w - 1, h - 1);

            for (int y = 0; y < n; y++) {
                for (int x = 0; x < n; x++) {
                    expanded.setRGB(x, y, topLeft);                             // arriba-izq
                    expanded.setRGB(newW - n + x, y, topRight);                 // arriba-der
                    expanded.setRGB(x, newH - n + y, bottomLeft);               // abajo-izq
                    expanded.setRGB(newW - n + x, newH - n + y, bottomRight);   // abajo-der
                }
            }
        }
        // Si bordeCopiado = false → padding queda transparente (ARGB=0)

        return expanded;
    }
}
