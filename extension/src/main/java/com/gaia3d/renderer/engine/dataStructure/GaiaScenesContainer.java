package com.gaia3d.renderer.engine.dataStructure;

import com.gaia3d.renderer.engine.scene.Camera;
import com.gaia3d.renderer.engine.scene.Projection;
import com.gaia3d.renderer.renderable.RenderableGaiaScene;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class GaiaScenesContainer {
    private Projection projection;
    private Camera camera;
    private List<RenderableGaiaScene> renderableGaiaScenes;

    public GaiaScenesContainer(int screenWidth, int screenHeight) {
        renderableGaiaScenes = new ArrayList<>();
        projection = new Projection(screenWidth, screenHeight);
        //textureCache = new TextureCache();
        //camera = new Camera();
    }

    public void addRenderableGaiaScene(RenderableGaiaScene renderableGaiaScene) {
        renderableGaiaScenes.add(renderableGaiaScene);
    }
}
