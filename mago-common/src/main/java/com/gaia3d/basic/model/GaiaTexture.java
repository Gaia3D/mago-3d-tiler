package com.gaia3d.basic.model;

import com.gaia3d.basic.model.structure.TextureStructure;
import com.gaia3d.basic.types.LevelOfDetail;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.util.ImageResizer;
import com.gaia3d.util.ImageUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

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
    private transient LevelOfDetail bufferedImageLod = LevelOfDetail.NONE;
    private ByteBuffer byteBuffer;

    private int textureId = -1;

    public void loadImage() {
        if (path == null || parentPath == null || path.isEmpty() || parentPath.isEmpty()) {
            return;
        }

        Path diffusePath = new File(path).toPath();
        String imagePath = parentPath + File.separator + diffusePath;
        if (this.bufferedImage == null) {
            BufferedImage bufferedImage = readImage(imagePath);
            if (bufferedImage != null) {
                this.bufferedImage = bufferedImage;
                this.bufferedImageLod = LevelOfDetail.NONE;
                this.width = bufferedImage.getWidth();
                this.height = bufferedImage.getHeight();
                this.format = bufferedImage.getType();
            }
        }
    }

    public boolean hasBufferedImage() {
        return this.bufferedImage != null;
    }

    public void deleteBufferedImage() {
        if (this.bufferedImage != null) {
            this.bufferedImage.flush();
            this.bufferedImage = null;
        }
    }

    public BufferedImage readImageScaled(
            Path imagePath,
            int dimensionLimit
    ) throws IOException {

        if (imagePath == null) {
            throw new IllegalArgumentException(
                    "imagePath must not be null"
            );
        }

        if (dimensionLimit <= 0) {
            throw new IllegalArgumentException(
                    "dimensionLimit must be greater than zero"
            );
        }

        if (!Files.isRegularFile(imagePath)) {
            return null;
        }

        try (ImageInputStream inputStream =
                     ImageIO.createImageInputStream(
                             imagePath.toFile()
                     )) {

            if (inputStream == null) {
                throw new IOException(
                        "Could not create ImageInputStream for: "
                                + imagePath
                );
            }

            Iterator<ImageReader> readers =
                    ImageIO.getImageReaders(inputStream);

            if (!readers.hasNext()) {
                throw new IOException(
                        "No compatible ImageReader found for: "
                                + imagePath
                );
            }

            ImageReader reader =
                    readers.next();

            try {
                reader.setInput(
                        inputStream,
                        true,
                        true
                );

                int originalWidth =
                        reader.getWidth(0);

                int originalHeight =
                        reader.getHeight(0);

                boolean exceedsLimit =
                        originalWidth > dimensionLimit
                                || originalHeight > dimensionLimit;

                int subsampling =
                        exceedsLimit ? 2 : 1;

                ImageReadParam readParam =
                        reader.getDefaultReadParam();

                if (subsampling > 1) {
                    readParam.setSourceSubsampling(
                            subsampling,
                            subsampling,
                            0,
                            0
                    );
                }

                BufferedImage result =
                        reader.read(
                                0,
                                readParam
                        );

                if (result == null) {
                    throw new IOException(
                            "Could not decode image: "
                                    + imagePath
                    );
                }

                if (result != null) {
                    this.bufferedImage = result;
                    this.bufferedImageLod = LevelOfDetail.NONE;
                    this.width = result.getWidth();
                    this.height = result.getHeight();
                    this.format = result.getType();
                }

                return result;

            } finally {
                reader.dispose();
            }
        }
    }

    public void saveImage(String savePath) {
        saveImage(savePath, true, 0.90f);
    }

    /**
     * @param savePath Ruta de destino.
     * @param fastPng true para priorizar velocidad sobre tamaño del PNG.
     * @param jpegQuality Calidad JPEG entre 0.0 y 1.0.
     * @return true cuando la imagen se escribió correctamente.
     */
    public boolean saveImage(
            String savePath,
            boolean fastPng,
            float jpegQuality
    ) {
        if (bufferedImage == null) {
            log.warn("GaiaTexture.saveImage(): bufferedImage is null.");
            return false;
        }

        if (savePath == null || savePath.isBlank()) {
            log.warn("GaiaTexture.saveImage(): savePath is null or empty.");
            return false;
        }

        //ImageUtils.saveBufferedImage(bufferedImage, savePath, fastPng, jpegQuality);
        ImageUtils.saveBufferedImageControlled(bufferedImage, savePath, fastPng, jpegQuality);

        return true;
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

    public void loadImage(LevelOfDetail lod) {
        if (path == null || parentPath == null || path.isEmpty() || parentPath.isEmpty()) {
            return;
        }

        int level = lod.getLevel();
        Path diffusePath = new File(path).toPath();
        String imagePath = parentPath + File.separator + diffusePath;
        File imageFile = new File(imagePath);
        String fileName = imageFile.getName();
        String lodFileName = level > 0 ? level + "_" + fileName : fileName;
        File lodImageFile = new File(imageFile.getParent(), lodFileName);
        boolean lodFileExists = lodImageFile.exists();

        if (lodFileExists) {
            if (this.bufferedImage == null) {
                BufferedImage bufferedImage = readImage(lodImageFile.getAbsolutePath());
                if (bufferedImage != null) {
                    this.bufferedImage = bufferedImage;
                    this.bufferedImageLod = lod;
                    this.width = bufferedImage.getWidth();
                    this.height = bufferedImage.getHeight();
                    this.format = bufferedImage.getType();
                }
            }
        } else {
            log.debug("[WARN] LOD file not found : {}, loading original image and resizing.", lodImageFile.getAbsolutePath());
            float scaleFactor = lod.getTextureScale();
            loadImage();
            if (this.bufferedImage != null) {
                int resizeWidth = (int) (this.bufferedImage.getWidth() * scaleFactor);
                int resizeHeight = (int) (this.bufferedImage.getHeight() * scaleFactor);
                resizeWidth = ImageUtils.getNearestPowerOfTwo(resizeWidth);
                resizeHeight = ImageUtils.getNearestPowerOfTwo(resizeHeight);
                this.width = resizeWidth;
                this.height = resizeHeight;
                this.bufferedImage = ImageResizer.resizeImageGraphic2D(this.bufferedImage, resizeWidth, resizeHeight);
                this.bufferedImageLod = lod;
            }
        }
    }

    public void resizeImage(int width, int height) {
        if (this.bufferedImage == null) {
            loadImage();
        }
        if (this.bufferedImage == null) {
            return;
        }
        this.bufferedImage = ImageResizer.resizeImageGraphic2D(this.bufferedImage, width, height);
        this.bufferedImageLod = LevelOfDetail.NONE;
        this.width = width;
        this.height = height;
    }

    public BufferedImage getPureBufferedImage() {
        return this.bufferedImage;
    }

    public BufferedImage getBufferedImage() {
        if (this.bufferedImage == null) {
            if (this.parentPath != null && this.path != null) {
                loadImage();
            }
        }
        return this.bufferedImage;
    }

    // getBufferedImage
    public BufferedImage getBufferedImage(LevelOfDetail lod) {
        if (this.bufferedImage == null || this.bufferedImageLod != lod) {
            if (this.bufferedImage != null) {
                this.bufferedImage.flush();
                this.bufferedImage = null;
            }
            loadImage(lod);
        }
        return this.bufferedImage;
    }

    public void deleteObjects() {
        if (byteBuffer != null) {
            byteBuffer.clear();
        }
        if (bufferedImage != null) {
            bufferedImage.flush();
            bufferedImage = null;
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

        byte[] rgbaByteArray;
        byte[] rgbaByteArray2;
        try {
            rgbaByteArray = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
            rgbaByteArray2 = ((DataBufferByte) comparebufferedImage.getRaster().getDataBuffer()).getData();
        } catch (Exception e) {
            log.error("[ERROR] Unable to get byte array from buffered image:", e);
            return false;
        }

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

    public boolean compareFileBytes(GaiaTexture a, GaiaTexture b) {
        if (a.getByteLength() != b.getByteLength()) {
            return false;
        }
        ByteBuffer byteBufferA = a.getByteBuffer();
        ByteBuffer byteBufferB = b.getByteBuffer();

        if (byteBufferA == null || byteBufferB == null) {
            return false;
        }
        for (int i = 0; i < a.getByteLength(); i++) {
            if (byteBufferA.get(i) != byteBufferB.get(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean isEqualTexture(GaiaTexture compareTexture, LevelOfDetail lod) {
        // file size comparison first
        /*File fileA = new File(this.getFullPath());
        File fileB = new File(compareTexture.getFullPath());
        if (fileA.length() != fileB.length()) {
            return false;
        }*/

        getBufferedImage(lod);
        compareTexture.getBufferedImage(lod);
        return isEqualTexture(compareTexture);
    }

    public void clear() {
        if (this.bufferedImage != null) {
            this.bufferedImage.flush();
            this.bufferedImage = null;
            this.bufferedImageLod = LevelOfDetail.NONE;
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
