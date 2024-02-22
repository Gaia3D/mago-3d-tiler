package com.gaia3d.engine.dataStructure;

import com.gaia3d.basic.structure.GaiaScene;

import java.util.ArrayList;
import java.util.List;

public class GaiaScenesContainer {
    private List<GaiaScene> gaiaScenes;

    public GaiaScenesContainer() {
        gaiaScenes = new ArrayList<>();
    }

    public void addGaiaScene(GaiaScene gaiaScene) {
        gaiaScenes.add(gaiaScene);
    }
}
