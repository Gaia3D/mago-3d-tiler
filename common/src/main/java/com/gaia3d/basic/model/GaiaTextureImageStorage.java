package com.gaia3d.basic.model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedHashMap;

@Setter
@Getter
@Slf4j
public class GaiaTextureImageStorage {
    private static final int MAX_IMAGE_COUNT = 10;
    private static LinkedHashMap<String, BufferedImage> textures = new LinkedHashMap<>();

    public static void putBufferedImage(String textureName, BufferedImage image) {
        if (textures.size() >= MAX_IMAGE_COUNT) {
            String firstKey = textures.keySet().iterator().next();
            textures.remove(firstKey);
            log.info("[GaiaTextureImageStorage] Texture image storage is full. Remove the first texture image: {}", firstKey);
        }
        textures.put(textureName, image);
        log.info("[GaiaTextureImageStorage] Texture image is stored: {}", textureName);
    }

    public static BufferedImage findBufferedImage(String textureName) {
        return textures.getOrDefault(textureName, null);
    }

    public static int getAllTextureCount() {
        return textures.size();
    }

    public static int getAllTextureSize() {
        int sizeResult = 0;
        for (BufferedImage image : textures.values()) {
            int imageSize = image.getWidth() * image.getHeight() * 4;
            sizeResult += imageSize;
        }
        return sizeResult;
    }
}
