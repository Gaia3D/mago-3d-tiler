package renderable;

import lombok.Getter;
import lombok.Setter;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

@Setter
@Getter
public class TextureBuffer {
    private int[] vbos;
    private int vboCount = 0;
    private int textureVbo;

    public TextureBuffer() {
        this.initVbo();
    }
    public void initVbo() {
        vbos = new int[1];
        GL20.glGenTextures(vbos);
        vboCount = 0;
    }

    //bind texture
    public static void setTextureBind(int vbo) {
        GL20.glBindTexture(GL20.GL_TEXTURE_2D, vbo);
    }
    //unbind texture
    public static void setTextureUnbind() {
        GL20.glBindTexture(GL20.GL_TEXTURE_2D, 0);
    }


    public int makeTexture(BufferedImage image, int format) {
        byte size = -1;
        if (format == GL20.GL_RGB) {
            size = 3;
        } else if (format == GL20.GL_RGBA) {
            size = 4;
        } else {
            return -1;
        }

        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * size);
        for (int y = 0; y < image.getHeight(); y++) {
            for( int x = 0; x < image.getWidth(); x++) {
                int pixel = pixels[y * image.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF));     // Red component
                buffer.put((byte) ((pixel >> 8) & 0xFF));      // Green component
                buffer.put((byte) (pixel & 0xFF));               // Blue component
                if (size == 4) {
                    buffer.put((byte) ((pixel >> 24) & 0xFF));    // Alpha component. Only for RGBA
                }
            }
        }
        buffer.flip();

        int texture = GL20.glGenTextures();
        GL20.glBindTexture(GL20.GL_TEXTURE_2D, texture);
        GL20.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, texture);
        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);
        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_NEAREST);
        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_NEAREST);
        GL20.glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGBA8, image.getWidth(), image.getHeight(), 0, format, GL20.GL_UNSIGNED_BYTE, buffer);
        return texture;
    }


    public int createTexture(ByteBuffer buffer, int width, int height, int internalFormat, int format) {
        int vbo = vbos[this.vboCount];
        GL20.glBindTexture(GL20.GL_TEXTURE_2D, vbo);
        //GL20.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1);
        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);
        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR);
        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR); //GL20.GL_LINEAR or GL20.GL_NEAREST
        GL20.glTexImage2D(GL20.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, GL20.GL_UNSIGNED_BYTE, buffer);
        vboCount++;
        return vbo;
    }


    public int createTexture(BufferedImage image) {
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 3); //4 for RGBA, 3 for RGB
        for(int y = 0; y < image.getHeight(); y++){
            for(int x = 0; x < image.getWidth(); x++){
                int pixel = pixels[y * image.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF));     // Red component
                buffer.put((byte) ((pixel >> 8) & 0xFF));      // Green component
                buffer.put((byte) (pixel & 0xFF));               // Blue component
                //buffer.put((byte) ((pixel >> 24) & 0xFF));    // Alpha component. Only for RGBA
            }
        }
        buffer.flip();
        int format = GL20.GL_RGBA;
        //int vbo = vbos[this.vboCount];
        int vbo = GL20.glGenTextures();
        GL20.glBindTexture(GL20.GL_TEXTURE_2D, vbo);
        //GL20.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1);
        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);
        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_NEAREST);
        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_NEAREST);
        GL20.glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGBA8, image.getWidth(), image.getHeight(), 0, format, GL20.GL_UNSIGNED_SHORT, buffer);
        vboCount++;
        return vbo;
    }

    public short[] convertShortArrayToArrayList(ArrayList<Short> shortList) {
        short[] shortArray = new short[shortList.size()];
        int i = 0;
        for (Short s : shortList) {
            shortArray[i++] = (s != null ? s : 0); // Or whatever default you want.
            // it has issue about unsigned short
        }
        return shortArray;
    }
    public float[] convertFloatArrayToArrayList(ArrayList<Float> floatList) {
        float[] floatArray = new float[floatList.size()];
        int i = 0;
        for (Float f : floatList) {
            floatArray[i++] = (f != null ? f : Float.NaN); // Or whatever default you want.
        }
        return floatArray;
    }
}
