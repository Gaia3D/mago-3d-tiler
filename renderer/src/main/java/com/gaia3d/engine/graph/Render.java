package com.gaia3d.engine.graph;

import org.lwjgl.opengl.GL;
import com.gaia3d.engine.Window;
import com.gaia3d.engine.scene.Scene;

import static org.lwjgl.opengl.GL11.*;
public class Render {

    private SceneRender sceneRender;
    public Render() {
        GL.createCapabilities();
        sceneRender = new SceneRender();
    }

    public void cleanup() {
        sceneRender.cleanup();
    }

    public void render(Window window, Scene scene) {
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        //glClearColor(0.0f, 1.0f, 0.0f, 0.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glViewport(0, 0, window.getWidth(), window.getHeight());
        sceneRender.render(scene);
    }
}
