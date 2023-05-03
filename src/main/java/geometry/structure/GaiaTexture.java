package geometry.structure;

import geometry.types.TextureType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import util.BinaryUtils;
import util.FileUtils;

import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;

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

    public BufferedImage readImage() {
        Path diffusePath = new File(path).toPath();
        String imagePath = parentPath + File.separator + diffusePath;
        BufferedImage bufferedImage = FileUtils.readImage(imagePath);
        this.bufferedImage = bufferedImage;
        this.width = bufferedImage.getWidth();
        this.height = bufferedImage.getHeight();
        return bufferedImage;
    }

    public ByteBuffer loadTextureBuffer() {
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
            //buf.flip();

            /*int channel = channels.get();
            if (channel == 3) {
                this.format = GL20.GL_RGB;
            } else {
                this.format = GL20.GL_RGBA;
            }*/

            this.format = GL20.GL_RGBA;
            this.width = width.get();
            this.height = height.get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //STBImage.stbi_image_free(buf);
        setByteBuffer(buf);
        return buf;
    }

    public ByteBuffer loadBuffer() {
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
            //setByteBuffer(buffer);

            //ByteBuffer texturePointer = stack.malloc(1);
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            //width.put(image.getWidth());
            //height.put(image.getHeight());
            //channels.put(size);

            ByteBuffer result = STBImage.stbi_load_from_memory(buffer, width, height, channels, 4);
            setByteBuffer(result);
            return result;
        }
    }

    public void write(DataOutputStream stream) throws IOException {
        BinaryUtils.writeText(stream, path);
    }
}
