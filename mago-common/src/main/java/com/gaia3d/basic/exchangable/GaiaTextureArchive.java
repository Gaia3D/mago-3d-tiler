package com.gaia3d.basic.exchangable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class GaiaTextureArchive {
    private final Map<String, BufferedImage> textures = new HashMap<>();

    public void addTexture(String path, BufferedImage texture) {
        if (textures.containsKey(path)) {
            log.warn("Texture with name {} already exists in the archive.", path);
        } else {
            textures.put(path, texture);
        }
    }

    public BufferedImage getTexture(String path) {
        return textures.get(path);
    }
}
