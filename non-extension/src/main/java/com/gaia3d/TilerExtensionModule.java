package com.gaia3d;

import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.GaiaTexture;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

@Slf4j
public class TilerExtensionModule implements ExtensionModuleFrame {

    @Override
    public String getName() {
        return "Non-Extension Project";
    }

    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public GaiaScene executePhotorealistic(GaiaScene gaiaScene, Map<String, Object> options) {
        log.debug("----------------------------------------");
        log.debug("Cannot execute photorealistic extension module.");
        log.debug("This module is not implemented.");
        log.debug("----------------------------------------");
        return null;
    }

    @Override
    public List<GaiaTexture> getRenderScene(List<GaiaScene> scene, int bufferedImageType, List<BufferedImage> resultImages) {
        return null;
    }
}
