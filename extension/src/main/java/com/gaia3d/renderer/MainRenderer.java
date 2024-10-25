package com.gaia3d.renderer;

import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.renderer.engine.Engine;
import com.gaia3d.renderer.engine.IAppLogic;
import com.gaia3d.renderer.engine.InternDataConverter;
import com.gaia3d.renderer.engine.Window;
import com.gaia3d.renderer.engine.dataStructure.GaiaScenesContainer;
import com.gaia3d.renderer.renderable.RenderableGaiaScene;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MainRenderer implements IAppLogic {
    private Engine engine = new Engine("MagoVisual3D", new Window.WindowOptions(), this);

    public void render() {
        // render the scene
        log.info("Rendering the scene...");
        try{
            engine.run();
        } catch (Exception e) {
            log.error("Error initializing the engine: " + e.getMessage());
        }

    }

    public void render(List<GaiaScene> gaiaScenes) {
        // render the scene
        log.info("Rendering the scene...");

        // Must init gl.***
        try{
            engine.init();
        } catch (Exception e) {
            log.error("Error initializing the engine: " + e.getMessage());
        }

        int screenWidth = 800;
        int screenHeight = 600;

        InternDataConverter internDataConverter = new InternDataConverter();
        List<RenderableGaiaScene> renderableGaiaScenes = new ArrayList<>();
        int gaiaSceneCount = gaiaScenes.size();
        for(int i = 0; i < gaiaSceneCount; i++)
        {
            RenderableGaiaScene renderableScene = internDataConverter.getRenderableGaiaScene(gaiaScenes.get(i));
            renderableGaiaScenes.add(renderableScene);
        }

        GaiaScenesContainer gaiaScenesContainer = new GaiaScenesContainer(screenWidth, screenHeight);
        gaiaScenesContainer.setRenderableGaiaScenes(renderableGaiaScenes);
        engine.setGaiaScenesContainer(gaiaScenesContainer);

        try{
            engine.run();
        } catch (Exception e) {
            log.error("Error initializing the engine: " + e.getMessage());
        }

    }

    @Override
    public void cleanup() {
    }

    @Override
    public void init(Window window, GaiaScenesContainer gaiaScenesContainer) {

    }

    @Override
    public void input(Window window, GaiaScenesContainer gaiaScenesContainer, long diffTimeMillis) {

    }

    @Override
    public void update(Window window, GaiaScenesContainer gaiaScenesContainer, long diffTimeMillis) {

    }
}
