package com.gaia3d.basic.structure;

import com.gaia3d.basic.structure.interfaces.TextureStructure;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.util.ImageResizer;
import com.gaia3d.util.ImageUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.file.Path;

/**
 * A class that represents a texture of a Gaia object.
 * It contains the texture name, path, type, width, height, format, byteLength, and byteBuffer.
 * The byteBuffer is used to create a texture.
 * The byteBuffer is created by reading the texture file.
 *
 * @author znkim
 * @see <a href="https://en.wikipedia.org/wiki/Texture_mapping">Texture mapping</a>
 * @since 1.0.0
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaTexture extends TextureStructure implements Serializable {
    private String parentPath;
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
        if (this.bufferedImage == null) {
            BufferedImage bufferedImage = readImage(imagePath);
            //BufferedImage bufferedImage = testImage();
            this.bufferedImage = bufferedImage;
            this.width = bufferedImage.getWidth();
            this.height = bufferedImage.getHeight();
            this.format = bufferedImage.getType();
        }
    }

    public void flipImageY() {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height / 2; j++) {
                int tmp = bufferedImage.getRGB(i, j);
                bufferedImage.setRGB(i, j, bufferedImage.getRGB(i, height - j - 1));
                bufferedImage.setRGB(i, height - j - 1, tmp);
            }
        }
    }

    public String getFullPath() {
        Path diffusePath = new File(path).toPath();
        return parentPath + File.separator + diffusePath;
    }

    private BufferedImage readImage(String filePath) {
        BufferedImage image = null;
        try (FileInputStream stream = new FileInputStream(filePath)) {
            image = ImageIO.read(stream);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return image;
    }

    private BufferedImage testImage() {
        BufferedImage bufferedImage = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 1, 1);
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
        ImageResizer imageResizer = new ImageResizer();
        this.bufferedImage = imageResizer.resizeImageGraphic2D(this.bufferedImage, resizeWidth, resizeHeight);
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

    public void deleteObjects() {
        if (byteBuffer != null) {
            byteBuffer.clear();
        }
        if (bufferedImage != null) {
            bufferedImage.flush();
        }
    }

    /**
     * It's a slow comparison of two textures, but it's accurate.
     */
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

        // compare the byte array by difference.***
        int length = rgbaByteArray.length;
        int length2 = rgbaByteArray2.length;

        if (length != length2) {
            return false;
        }

        float differenceAccum = 0;
        float difference = 0;
        float tolerance = 5.0f;
        for (int i = 0; i < length; i++) {
            difference = Math.abs(rgbaByteArray[i] - rgbaByteArray2[i]);
            differenceAccum += difference;
            if ((differenceAccum / (float) (i + 1)) > tolerance) {
                return false;
            }
        }

        float differenceRatio = differenceAccum / (float) length;

        return differenceRatio < tolerance;
    }

    public boolean isEqualTexture(GaiaTexture compareTexture, float scaleFactor) {
        getBufferedImage(scaleFactor);
        compareTexture.getBufferedImage(scaleFactor);
        return isEqualTexture(compareTexture);
    }

    public void clear() {
        if (this.bufferedImage != null) {
            this.bufferedImage.flush();
            this.bufferedImage = null;
        }
        if (this.byteBuffer != null) {
            this.byteBuffer.clear();
            this.byteBuffer = null;
        }
    }

    public GaiaTexture clone() {
        GaiaTexture clonedTexture = new GaiaTexture();
        clonedTexture.setName(this.name);
        clonedTexture.setPath(this.path);
        clonedTexture.setType(this.type);
        clonedTexture.setWidth(this.width);
        clonedTexture.setHeight(this.height);
        clonedTexture.setFormat(this.format);
        clonedTexture.setByteBuffer(this.byteBuffer);
        clonedTexture.setTextureId(this.textureId);
        clonedTexture.setParentPath(this.parentPath);
        return clonedTexture;
    }
}
