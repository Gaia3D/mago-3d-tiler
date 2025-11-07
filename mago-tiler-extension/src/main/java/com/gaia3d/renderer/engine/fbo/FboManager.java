package com.gaia3d.renderer.engine.fbo;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class FboManager {
    private Map<String, Fbo> mapNameFbos;
    private Map<String, FboMRT> mapNameFboMRTs;

    public FboManager() {
        mapNameFbos = new HashMap<>();
    }

    public Fbo createFbo(String name, int fboWidth, int fboHeight) {
        Fbo fbo = new Fbo(name, fboWidth, fboHeight);
        mapNameFbos.put(name, fbo);
        return fbo;
    }

    public FboMRT createFboMRT(String name, int fboWidth, int fboHeight, int numColorAttachments) {
        if (mapNameFboMRTs == null) {
            mapNameFboMRTs = new HashMap<>();
        }
        FboMRT fboMRT = new FboMRT(name, fboWidth, fboHeight, numColorAttachments);
        mapNameFboMRTs.put(name, fboMRT);
        return fboMRT;
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

    public FboMRT getOrCreateFboMRT(String name, int fboWidth, int fboHeight, int numColorAttachments) {
        if (mapNameFboMRTs == null) {
            mapNameFboMRTs = new HashMap<>();
        }
        FboMRT fboMRT = mapNameFboMRTs.get(name);
        if (fboMRT == null) {
            fboMRT = createFboMRT(name, fboWidth, fboHeight, numColorAttachments);
        }

        if (fboMRT.getFboWidth() != fboWidth || fboMRT.getFboHeight() != fboHeight || fboMRT.getNumColorAttachments() != numColorAttachments) {
            deleteFboMRT(name);
            fboMRT = createFboMRT(name, fboWidth, fboHeight, numColorAttachments);
        }
        //fbo.resize(fboWidth, fboHeight);

        return fboMRT;
    }

    public void deleteFboMRT(String name) {
        if (mapNameFboMRTs == null) {
            return;
        }
        FboMRT fboMRT = mapNameFboMRTs.get(name);
        if (fboMRT != null) {
            fboMRT.cleanup();
            mapNameFboMRTs.remove(name);
        }
    }

    public FboMRT getFboMRT(String name) {
        if (mapNameFboMRTs == null) {
            return null;
        }
        // 1rst check if exist
        if (mapNameFboMRTs.containsKey(name)) {
            return mapNameFboMRTs.get(name);
        }
        return null;
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

        if (mapNameFboMRTs != null) {
            for (FboMRT fboMRT : mapNameFboMRTs.values()) {
                fboMRT.cleanup();
            }
            mapNameFboMRTs.clear();
        }
    }
}
