package geometry.structure;

import geometry.types.TextureType;
import io.LittleEndianDataInputStream;
import io.LittleEndianDataOutputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import util.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.Arrays;

import static org.lwjgl.system.MemoryStack.stackPush;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaTexture {
    Path parentPath;
    private String name;
    private String path;
    private TextureType type;

    private int width;
    private int height;
    private int format;

    private int byteLength;
    private BufferedImage bufferedImage;
    private ByteBuffer byteBuffer;

    private int textureId = -1;

    public void loadImage() {
        Path diffusePath = new File(path).toPath();
        String imagePath = parentPath + File.separator + diffusePath;
        BufferedImage bufferedImage = ImageUtils.readImage(imagePath);
        this.bufferedImage = bufferedImage;
        assert bufferedImage != null;
        this.width = bufferedImage.getWidth();
        this.height = bufferedImage.getHeight();
    }

    public void loadTextureBuffer() {
        Path diffusePath = new File(path).toPath();
        String imagePath = parentPath + File.separator + diffusePath;

        ByteBuffer buf = null;
        try (MemoryStack stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            buf = STBImage.stbi_load(imagePath, width, height, channels, 4);
            if (buf == null) {
                throw new Exception("Image file [" + imagePath  + "] not loaded: " + STBImage.stbi_failure_reason());
            }
            this.format = GL20.GL_RGBA;
            this.width = width.get();
            this.height = height.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        setByteBuffer(buf);
    }

    public static boolean areEqualTextures(GaiaTexture textureA, GaiaTexture textureB) throws IOException {
        if (textureA == null || textureB == null) {
            return false;
        }

        if(textureA == textureB) {
            return true;
        }

        // load image if not loaded
        if (textureA.getBufferedImage() == null) {
            textureA.loadImage();

        }

        if (textureB.getBufferedImage() == null) {
            textureB.loadImage();
        }

        if (textureA.getWidth() != textureB.getWidth()) {
            return false;
        }
        if (textureA.getHeight() != textureB.getHeight()) {
            return false;
        }
        if (textureA.getFormat() != textureB.getFormat()) {
            return false;
        }
        // now, compare the pixels
        int width = textureA.getWidth();
        int height = textureA.getHeight();

        byte[] rgbaByteArray = ((DataBufferByte) textureA.bufferedImage.getRaster().getDataBuffer()).getData();
        byte[] rgbaByteArray2 = ((DataBufferByte) textureB.bufferedImage.getRaster().getDataBuffer()).getData();

        boolean areEqual = Arrays.equals(rgbaByteArray, rgbaByteArray2);
        return areEqual;
    }
    public void loadBuffer() {
        BufferedImage image = this.bufferedImage;
        byte size = 4;
        if (this.format == GL20.GL_RGB) {
            size = 3;
        }
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

        try (MemoryStack stack = stackPush()) {
            ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * size);
            for (int y = 0; y < image.getHeight(); y++) {
                for( int x = 0; x < image.getWidth(); x++) {
                    int pixel = pixels[y * image.getWidth() + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF));
                    buffer.put((byte) ((pixel >> 8) & 0xFF));
                    buffer.put((byte) (pixel & 0xFF));
                    if (size == 4) {
                        buffer.put((byte) ((pixel >> 24) & 0xFF));    // Alpha component. Only for RGBA
                    }
                }
            }
            buffer.flip();
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            ByteBuffer result = STBImage.stbi_load_from_memory(buffer, width, height, channels, 4);
            setByteBuffer(result);
        }
    }

    public void write(LittleEndianDataOutputStream stream) throws IOException {
        stream.writeText(path);
    }

    public void read(LittleEndianDataInputStream stream) throws IOException {
        this.setPath(stream.readText());
    }
}
