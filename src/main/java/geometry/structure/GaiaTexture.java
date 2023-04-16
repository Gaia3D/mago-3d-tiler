package geometry.structure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import util.FileUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Path;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaTexture {
    Path parentPath;
    private String name;
    private String path;
    private GaiaMaterialType type;

    private int width;
    private int height;
    private int format;

    private int byteLength;
    private BufferedImage bufferedImage;
    private ByteBuffer byteBuffer;

    public BufferedImage readImage() {
        Path diffusePath = new File(path).toPath();
        String imagePath = parentPath + File.separator + diffusePath;
        BufferedImage bufferedImage = FileUtils.readImage(imagePath);
        this.bufferedImage = bufferedImage;
        this.width = bufferedImage.getWidth();
        this.height = bufferedImage.getHeight();
        return bufferedImage;
    }

    public ByteBuffer loadBuffer() {
        BufferedImage image = this.bufferedImage;
        byte size = 4;
        if (this.format == GL20.GL_RGB) {
            size = 3;
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
        setByteBuffer(buffer);
        return buffer;
    }
}
