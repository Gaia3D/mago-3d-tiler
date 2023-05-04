package util;

import geometry.structure.*;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class FileUtils {
    // getNearestPowerOfTwo
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

    private static BufferedImage resizeImageGraphic2D(BufferedImage originalImage, int width, int height) {
        BufferedImage outputImage = new BufferedImage(width, height, originalImage.getType());
        Graphics2D graphics2D = outputImage.createGraphics();
        graphics2D.setComposite(AlphaComposite.Src);
        graphics2D.drawImage(originalImage, 0, 0, width, height, null);
        graphics2D.dispose();
        return outputImage;
    }

    public static String writeImage(BufferedImage bufferedImage, String mimeType) {
        String formatName = getFormatNameByMimeType(mimeType);
        String imageString = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            if (width != getNearestPowerOfTwo(width) || height != getNearestPowerOfTwo(height)) {
                bufferedImage = resizeImageGraphic2D(bufferedImage, getNearestPowerOfTwo(width), getNearestPowerOfTwo(height));
            }
            ImageIO.write(bufferedImage, formatName, baos);
            byte[] bytes = baos.toByteArray();
            imageString = "data:" + mimeType +";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageString;
    }

    //getFormatNameByMimeType
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

    //getMimeTypeByExtension
    public static String getMimeTypeByExtension(String extension) {
        String mimeType = null;
        extension = extension.toLowerCase();
        switch (extension) {
            case "png":
                mimeType = "image/png";
                break;
            case "jpg":
                mimeType = "image/jpeg";
                break;
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
                mimeType = "image/tiff";
                break;
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

    public static BufferedImage readImage(String filePath) {
        BufferedImage image;
        try {
            image = ImageIO.read(new File(filePath));
            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String changeExtension(String fileName, String extension) {
        String name = getFileNameWithoutExtension(fileName);
        return name + "." + extension;
    }

    public static String getFileNameWithoutExtension(String fileName) {
        String name = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            name = fileName.substring(0, i);
        }
        return name;
    }

    public static String getExtension(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i+1);
        }
        return extension;
    }

    public static byte[] readBytes(File file) {
        byte[] bytes = null;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public static ByteBuffer readFile(File file) {
        ByteBuffer byteBuffer = null;
        byte[] bytes = readBytes(file);
        byteBuffer = BufferUtils.createByteBuffer(bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        return byteBuffer;
    }

    // copyFile
    public static void copyFile(Path source, Path dest) {
        try {
            if (!dest.toFile().exists()) {
                Files.copy(source, dest);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
