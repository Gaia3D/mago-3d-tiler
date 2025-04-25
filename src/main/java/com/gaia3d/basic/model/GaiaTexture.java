package com.gaia3d.basic.model;

import com.gaia3d.basic.model.structure.TextureStructure;
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
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;

/**
 * A class that represents a texture of a Gaia object.
 * It contains the texture name, path, type, width, height, format, byteLength, and byteBuffer.
 * The byteBuffer is used to create a texture.
 * The byteBuffer is created by reading the texture file.
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
    private TextureType type; // DIFFUSE, NORMAL, SPECULAR, ETC

    private int width;
    private int height;
    private int format;

    private int byteLength;
    private transient BufferedImage bufferedImage;
    private ByteBuffer byteBuffer;

    private int textureId = -1;

    public void loadImage() {
        if (path == null || parentPath == null) {
            return;
        }

        // check for empty strings
        if (path.isEmpty() || parentPath.isEmpty()) {
            return;
        }
        Path diffusePath = new File(path).toPath();
        String imagePath = parentPath + File.separator + diffusePath;
        if (this.bufferedImage == null) {
            BufferedImage bufferedImage = readImage(imagePath);
            if (bufferedImage != null) {
                this.bufferedImage = bufferedImage;
                this.width = bufferedImage.getWidth();
                this.height = bufferedImage.getHeight();
                this.format = bufferedImage.getType();
            }
        }
    }

    public void saveImage(String savePath) {
        try {
            String imageExtension = savePath.substring(savePath.lastIndexOf(".") + 1);
            File file = new File(savePath);
            ImageIO.setUseCache(false);
            ImageIO.write(bufferedImage, imageExtension, file);
        } catch (IOException e) {
            log.error("[ERROR] :", e);
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
        File imageFile = new File(filePath);

        String fileName = imageFile.getName();
        if (!imageFile.exists()) {
            fileName = fileName.replace(".jpg", ".png");
            fileName = fileName.replace(".jpeg", ".png");
            fileName = fileName.replace(".JPG", ".png");
            fileName = fileName.replace(".JPEG", ".png");
            imageFile = new File(imageFile.getParent(), fileName);
            if (!imageFile.exists()) {
                log.error("[ERROR] Image file not found : {}", imageFile.getAbsolutePath());
                return null;
            }
        }

        BufferedImage image = null;
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(imageFile))) {
            image = ImageIO.read(stream);
        } catch (IOException e) {
            log.error("[ERROR] :", e);
        }
        return image;
    }

    public void createImage(int width, int height, int imageType) {
        this.width = width;
        this.height = height;
        this.bufferedImage = new BufferedImage(width, height, imageType);
    }

    public void fillImage(Color color) {
        Graphics2D graphics = this.bufferedImage.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, this.width, this.height);
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
        if (this.bufferedImage != null) {
            int resizeWidth = (int) (this.bufferedImage.getWidth() * scaleFactor);
            int resizeHeight = (int) (this.bufferedImage.getHeight() * scaleFactor);
            resizeWidth = ImageUtils.getNearestPowerOfTwo(resizeWidth);
            resizeHeight = ImageUtils.getNearestPowerOfTwo(resizeHeight);
            this.width = resizeWidth;
            this.height = resizeHeight;
            ImageResizer imageResizer = new ImageResizer();
            this.bufferedImage = imageResizer.resizeImageGraphic2D(this.bufferedImage, resizeWidth, resizeHeight);
        }
    }

    public void resizeImage(int width, int height) {
        if (this.bufferedImage == null) {
            loadImage();
        }
        if (this.bufferedImage == null) {
            return;
        }
        ImageResizer imageResizer = new ImageResizer();
        this.bufferedImage = imageResizer.resizeImageGraphic2D(this.bufferedImage, width, height);
    }

    public BufferedImage getBufferedImage() {
        if (this.bufferedImage == null) {
            if (this.parentPath != null && this.path != null) {
                loadImage();
            }
        }
        return this.bufferedImage;
    }

    public BufferedImage getBufferedImageWithCache() {
        if (this.bufferedImage == null) {
            String keyPath = this.path;
            ImageCacheQueue imageCacheQueue = ImageCacheQueue.getInstance();
            boolean hasKey = imageCacheQueue.hasBufferedImage(keyPath);
            if (keyPath.contains("_atlas_")) {
                loadImage();
                return this.bufferedImage;
            }

            if (hasKey) {
                BufferedImage tempImage = imageCacheQueue.getBufferedImage(keyPath);
                log.info("ImageCacheQueue hit: {}", keyPath);
                this.bufferedImage = tempImage;
            } else {
                log.info("ImageCacheQueue put: {}", keyPath);
                loadImage();
                imageCacheQueue.putBufferedImage(keyPath, this.bufferedImage);
            }
            log.info("bufferedImage : {} -> ({} x {})", this.path, this.bufferedImage.getHeight(), this.bufferedImage.getWidth());
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

        // compare the byte array by difference.
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
