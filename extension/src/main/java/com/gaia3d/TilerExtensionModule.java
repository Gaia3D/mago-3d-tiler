package com.gaia3d;

import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.renderer.Renderer;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class TilerExtensionModule implements ExtensionModuleFrame {

    @Override
    public String getName() {
        return "Extension Project";
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public GaiaScene executePhotorealistic(GaiaScene gaiaScene, Map<String, Object> options) {
        // TODO: Implement this method
        log.info("----------------------------------------");
        log.info("Extension has been applied.");
        log.info("----------------------------------------");


        return null;
    }

    public GaiaTexture getRenderScene(GaiaScene scene)
    {
        GaiaTexture tex = null;

        Renderer renderer = new Renderer();

        // Renderer renderer = new Renderer();
        // use the renderer to render the scene
        return tex;
    }
}
