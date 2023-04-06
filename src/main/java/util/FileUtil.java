package util;

import geometry.*;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Files;

public class FileUtil {

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

    // readImage
    public static ByteBuffer convertByteBufferToBufferdImage(BufferedImage bufferedImage) {
        int srcPixelFormat = -1;
        if (bufferedImage.getColorModel().hasAlpha()) {
            srcPixelFormat = GL20.GL_RGBA;
        } else {
            srcPixelFormat = GL20.GL_RGB;
        }

        byte[] bytes = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
        //ByteBuffer byteBuffer = BufferUtils.createByteBuffer(bytes.length);
        //ByteBuffer byteBuffer = BufferUtils.createByteBuffer(bufferedImage.getWidth() * bufferedImage.getHeight() * 4);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bufferedImage.getWidth() * bufferedImage.getHeight() * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.put(bytes);
        byteBuffer.flip();
        return byteBuffer;
    }

    // convertIntBufferToByteBuffer
    public static IntBuffer convertByteBufferToIntBuffer(ByteBuffer byteBuffer) {
        int length = byteBuffer.capacity() / 4;
        IntBuffer intBuffer = IntBuffer.allocate(length);
        for (int i = 0; i < length; i++) {
            intBuffer.put(byteBuffer.getInt(i * 4));
        }
        /*IntBuffer intBuffer = BufferUtils.createIntBuffer(byteBuffer.capacity() / 4);
        for (int i = 0; i < byteBuffer.capacity() / 4; i++) {
            intBuffer.put(byteBuffer.getInt(i * 4));
        }*/
        return intBuffer;
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

    public static GaiaScene sampleScene() {
        GaiaScene scene = new GaiaScene();
        GaiaNode rootNode = new GaiaNode();
        GaiaNode childNode = new GaiaNode();
        GaiaMesh mesh = new GaiaMesh();
        GaiaPrimitive primitive = new GaiaPrimitive();

        GaiaVertex vertex1 = new GaiaVertex();
        vertex1.setPosition(new Vector3d(0.0, 0.0, 0.0));
        vertex1.setColor(new Vector4d(0.5, 0.5, 0.5, 1.0));

        GaiaVertex vertex2 = new GaiaVertex();
        vertex2.setPosition(new Vector3d(256.0, 0.0, 0.0));
        vertex2.setColor(new Vector4d(0.5, 0.5, 0.5, 1.0));

        GaiaVertex vertex3 = new GaiaVertex();
        vertex3.setPosition(new Vector3d(256.0, 256.0, 0.0));
        vertex3.setColor(new Vector4d(0.5, 0.5, 0.5, 1.0));

        GaiaVertex vertex4 = new GaiaVertex();
        vertex4.setPosition(new Vector3d(0.0, 256.0, 0.0));
        vertex4.setColor(new Vector4d(0.5, 0.5, 0.5, 1.0));

        primitive.getVertices().add(vertex1);
        primitive.getVertices().add(vertex2);
        primitive.getVertices().add(vertex3);
        primitive.getVertices().add(vertex4);

        primitive.getIndices().add(0);
        primitive.getIndices().add(1);
        primitive.getIndices().add(2);
        primitive.getIndices().add(0);
        primitive.getIndices().add(2);
        primitive.getIndices().add(3);

        mesh.getPrimitives().add(primitive);
        childNode.getMeshes().add(mesh);
        rootNode.getChildren().add(childNode);
        scene.getNodes().add(rootNode);
        return scene;
    }
}
