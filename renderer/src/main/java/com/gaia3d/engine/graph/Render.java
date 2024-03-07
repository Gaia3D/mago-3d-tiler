package com.gaia3d.engine.graph;

import com.gaia3d.engine.dataStructure.GaiaScenesContainer;
import org.lwjgl.opengl.GL;
import com.gaia3d.engine.Window;
import com.gaia3d.engine.scene.Scene;
import org.lwjgl.opengl.GL20;


public class Render {

    private SceneRender sceneRender;
//    public Render() {
//        GL.createCapabilities();
//        sceneRender = new SceneRender();
//    }
//
//    public void cleanup() {
//        sceneRender.cleanup();
//    }
//
//    public void render(Window window, Scene scene) {
//        GL.createCapabilities();
//        GL20.glEnable(GL20.GL_DEPTH_TEST);
//        GL20.glClearColor(1.0f, 0.5f, 1.0f, 0.0f);
//        GL20.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
//
//        GL20.glViewport(0, 0, window.getWidth(), window.getHeight());
//        sceneRender.render(scene);
//    }
//
//    public void renderGaiaRenderables(Window window, GaiaScenesContainer gaiaScenesContainer) {
//        GL.createCapabilities();
//        GL20.glViewport(0, 0, window.getWidth(), window.getHeight());
//        GL20.glDisable(GL20.GL_DEPTH_TEST);
//        GL20.glClearColor(0.5f, 0.5f, 0.5f, 0.0f);
//        GL20.glClearDepth(1.0f);
//        GL20.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
//
//        sceneRender.renderGaiaScenes(gaiaScenesContainer);
//    }
}
