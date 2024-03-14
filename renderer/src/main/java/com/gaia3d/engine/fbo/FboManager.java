package com.gaia3d.engine.fbo;
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

    public Fbo getFbo(String name) {
        // 1rst check if exist
        return mapNameFbos.get(name);
    }
}
