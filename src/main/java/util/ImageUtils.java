package util;

import lombok.extern.slf4j.Slf4j;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Slf4j
public class ImageUtils {
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

    public static BufferedImage resizeImageGraphic2D(BufferedImage originalImage, int width, int height) {
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

    public static BufferedImage readImage(String filePath) {
        BufferedImage image = null;
        try (FileInputStream stream = new FileInputStream(new File(filePath))){
            image = ImageIO.read(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
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
}
