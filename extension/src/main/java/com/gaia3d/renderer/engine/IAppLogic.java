package com.gaia3d.renderer.engine;

import com.gaia3d.renderer.engine.dataStructure.GaiaScenesContainer;

import java.awt.*;

public interface IAppLogic {
    void cleanup();

    void init(Window window, GaiaScenesContainer gaiaScenesContainer);

    //void input(Window window, Scene scene, long diffTimeMillis);
    void input(Window window, GaiaScenesContainer gaiaScenesContainer, long diffTimeMillis);

    void update(Window window, GaiaScenesContainer gaiaScenesContainer, long diffTimeMillis);
}
