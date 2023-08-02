package geometry.structure;

import geometry.types.TextureType;
import io.LittleEndianDataInputStream;
import io.LittleEndianDataOutputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiler.LevelOfDetail;
import util.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;

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
        //BufferedImage bufferedImage = simpleImage();
        this.bufferedImage = bufferedImage;
        this.width = bufferedImage.getWidth();
        this.height = bufferedImage.getHeight();
    }

    private BufferedImage simpleImage() {
        BufferedImage bufferedImage = new BufferedImage(16, 16, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 16, 16);
        //graphics.dispose();
        return bufferedImage;
    }

    public void loadImage(float scaleFactor) {
        loadImage();
        int resizeWidth = (int) (this.bufferedImage.getWidth() * scaleFactor);
        int resizeHeight = (int) (this.bufferedImage.getHeight() * scaleFactor);
        resizeWidth = ImageUtils.getNearestPowerOfTwo(resizeWidth);
        resizeHeight = ImageUtils.getNearestPowerOfTwo(resizeHeight);
        this.width = resizeWidth;
        this.height = resizeHeight;
        this.bufferedImage = ImageUtils.resizeImageGraphic2D(this.bufferedImage, resizeWidth, resizeHeight);
    }

    // getBufferedImage
    public BufferedImage getBufferedImage() {
        if (this.bufferedImage == null) {
            loadImage();
        }
        return this.bufferedImage;
    }

    // getBufferedImage
    public BufferedImage getBufferedImage(float scaleFactor) {
        if (this.bufferedImage == null) {
            loadImage(scaleFactor);
        }
        return this.bufferedImage;
    }

    public void deleteObjects()
    {
        //if (textureId != -1) {
        //    GL20.glDeleteTextures(textureId);
        //}
        if (byteBuffer != null) {
            byteBuffer.clear();
        }
        bufferedImage = null;
    }

    public boolean isEqualTexture(GaiaTexture compareTexture) {
        BufferedImage bufferedImage = this.getBufferedImage();
        BufferedImage comparebufferedImage = compareTexture.getBufferedImage();

        int width = this.getWidth();
        int height = this.getHeight();

        if (width != compareTexture.getWidth()) {
            return false;
        }
        if (height != compareTexture.getHeight()) {
            return false;
        }
        if (this.getFormat() != compareTexture.getFormat()) {
            return false;
        }

        byte[] rgbaByteArray = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
        byte[] rgbaByteArray2 = ((DataBufferByte) comparebufferedImage.getRaster().getDataBuffer()).getData();

        return Arrays.equals(rgbaByteArray, rgbaByteArray2);
    }

    public boolean isEqualTexture(GaiaTexture compareTexture, LevelOfDetail levelOfDetail) {
        float scaleFactor = levelOfDetail.getTextureScale();
        getBufferedImage(scaleFactor);
        compareTexture.getBufferedImage(scaleFactor);
        return isEqualTexture(compareTexture);
    }

    /*public static boolean areEqualTextures(GaiaTexture textureA, GaiaTexture textureB) throws IOException {
        if (textureA == null || textureB == null) {
            return false;
        }

        if(textureA == textureB) {
            return true;
        }

        BufferedImage bufferedImageA = textureA.getBufferedImage();
        BufferedImage bufferedImageB = textureB.getBufferedImage();

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

        byte[] rgbaByteArray = ((DataBufferByte) bufferedImageA.getRaster().getDataBuffer()).getData();
        byte[] rgbaByteArray2 = ((DataBufferByte) bufferedImageB.getRaster().getDataBuffer()).getData();

        boolean areEqual = Arrays.equals(rgbaByteArray, rgbaByteArray2);

        return areEqual;
    } */

    public void write(LittleEndianDataOutputStream stream) throws IOException {
        stream.writeText(path);
    }

    public void read(LittleEndianDataInputStream stream) throws IOException {
        this.setPath(stream.readText());
    }
}
