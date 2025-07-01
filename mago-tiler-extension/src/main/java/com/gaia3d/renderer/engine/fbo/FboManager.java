package com.gaia3d.renderer.engine.fbo;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class FboManager {
    private Map<String, Fbo> mapNameFbos;

    public FboManager() {
        mapNameFbos = new HashMap<>();
    }

    public Fbo createFbo(String name, int fboWidth, int fboHeight) {
        Fbo fbo = new Fbo(name, fboWidth, fboHeight);
        mapNameFbos.put(name, fbo);
        return fbo;
    }

    public Fbo getOrCreateFbo(String name, int fboWidth, int fboHeight) {
        Fbo fbo = getFbo(name);
        if (fbo == null) {
            fbo = createFbo(name, fboWidth, fboHeight);
        }

        if (fbo.getFboWidth() != fboWidth || fbo.getFboHeight() != fboHeight) {
            deleteFbo(name);
            fbo = createFbo(name, fboWidth, fboHeight);
        }
        //fbo.resize(fboWidth, fboHeight);

        return fbo;
    }

    public Fbo getFbo(String name) {
        // 1rst check if exist
        if (mapNameFbos.containsKey(name)) {
            return mapNameFbos.get(name);
        }
        return null;
    }

    public void deleteFbo(String name) {
        Fbo fbo = mapNameFbos.get(name);
        if (fbo != null) {
            fbo.cleanup();
            mapNameFbos.remove(name);
        }
    }

    public void deleteAllFbos() {
        for (Fbo fbo : mapNameFbos.values()) {
            fbo.cleanup();
        }
        mapNameFbos.clear();
    }
}
