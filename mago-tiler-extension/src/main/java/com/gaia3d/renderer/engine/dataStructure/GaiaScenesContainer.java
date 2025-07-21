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
        int projType = 0; // 0: perspective, 1: orthographic
        projection = new Projection(projType, screenWidth, screenHeight);
    }

    public void addRenderableGaiaScene(RenderableGaiaScene renderableGaiaScene) {
        renderableGaiaScenes.add(renderableGaiaScene);
    }

    public void deleteObjects() {
        int renderablesCount = renderableGaiaScenes.size();
        for (int i = 0; i < renderablesCount; i++) {
            renderableGaiaScenes.get(i).deleteGLBuffers();
        }
        renderableGaiaScenes.clear();
    }

    public List<RenderableGaiaScene> getRenderableGaiaScenes() {
        if (renderableGaiaScenes == null) {
            renderableGaiaScenes = new ArrayList<>();
        }
        return renderableGaiaScenes;
    }
}
