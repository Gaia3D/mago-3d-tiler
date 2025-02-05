package com.gaia3d.basic.model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.LinkedHashMap;

@Setter
@Getter
@Slf4j
@Deprecated
public class ImageCacheQueue {
    private final int MAX_IMAGE_COUNT = 20;
    private final LinkedHashMap<String, BufferedImage> textures = new LinkedHashMap<>();
    private static ImageCacheQueue instance = null;
    public static ImageCacheQueue getInstance() {
        if (instance == null) {
            instance = new ImageCacheQueue();
        }
        return instance;
    }

    public void putBufferedImage(String textureName, BufferedImage image) {
        if (textures.size() >= MAX_IMAGE_COUNT) {
            String firstKey = textures.keySet().iterator().next();
            textures.remove(firstKey);
            log.info("[GaiaTextureImageStorage] Texture image storage is full. Remove the first texture image: {}", firstKey);
        }
        textures.put(textureName, image);
        log.info("[GaiaTextureImageStorage] Texture image is stored: {}", textureName);
    }

    public boolean hasBufferedImage(String textureName) {
        return textures.containsKey(textureName);
    }

    public BufferedImage getBufferedImage(String textureName) {
        BufferedImage image = textures.get(textureName);
        if (image != null) {
            // Return a deep copy of the image to prevent the original image from being modified.
            return deepCopy(image);
            //return image;
        } else {
            return null;
        }
    }

    public int getAllTextureCount() {
        return textures.size();
    }

    public int getAllTextureSize() {
        int sizeResult = 0;
        for (BufferedImage image : textures.values()) {
            int imageSize = image.getWidth() * image.getHeight() * 4;
            sizeResult += imageSize;
        }
        return sizeResult;
    }

    public BufferedImage deepCopy(BufferedImage bufferedImage) {
        log.info("[GaiaTextureImageStorage] Deep copy of the texture image: {}", bufferedImage);
        ColorModel colorModel = bufferedImage.getColorModel();
        boolean isAlphaPremultiplied = colorModel.isAlphaPremultiplied();
        WritableRaster raster = bufferedImage.copyData(null);
        return new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
    }
}
