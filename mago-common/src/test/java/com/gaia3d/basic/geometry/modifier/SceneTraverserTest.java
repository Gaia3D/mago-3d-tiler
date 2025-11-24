package com.gaia3d.basic.geometry.modifier;

import com.gaia3d.basic.model.GaiaScene;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SceneTraverserTest {

    @Test
    void traverse() {
        SceneTraverser sceneTraverser = new SceneTraverser();
        GaiaScene gaiaScene = new GaiaScene();

        SceneElementVisitor visitor = new SceneElementVisitor() {};

        assertNotNull(sceneTraverser);
        assertNotNull(gaiaScene);
        sceneTraverser.traverse(gaiaScene, visitor);
    }
}