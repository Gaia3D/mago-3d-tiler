package com.gaia3d;

import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.renderer.MainRenderer;
import com.gaia3d.renderer.engine.InternDataConverter;
import com.gaia3d.renderer.renderable.RenderableGaiaScene;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
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

    public List<GaiaTexture> getRenderScene(List<GaiaScene> scenes)
    {



        List<GaiaTexture> textures = new ArrayList<>();
        GaiaTexture tex = null;

        textures.add(tex);

        MainRenderer renderer = new MainRenderer();
        renderer.render(scenes);

        // Renderer renderer = new Renderer();
        // use the renderer to render the scene
        return textures;
    }
}
