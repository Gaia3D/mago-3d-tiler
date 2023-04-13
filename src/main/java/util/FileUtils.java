package util;

import geometry.structure.*;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.util.Base64;

public class FileUtils {
    public static String writeImage(BufferedImage bufferedImage, String mimeType) {

        String imageString = null;
        if (mimeType == null) {
            mimeType = "image/png";
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, "jpeg", baos);
            byte[] bytes = baos.toByteArray();
            imageString = "data:" + mimeType +";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageString;
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
        //byteBuffer = MemoryUtil.memCalloc(bytes.length);
        byteBuffer = BufferUtils.createByteBuffer(bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        return byteBuffer;
    }
}
