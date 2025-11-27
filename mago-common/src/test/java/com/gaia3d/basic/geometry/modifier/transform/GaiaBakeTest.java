package com.gaia3d.basic.geometry.modifier.transform;

import com.gaia3d.basic.geometry.modifier.DefaultSceneFactory;
import com.gaia3d.basic.geometry.modifier.SceneElementVisitor;
import com.gaia3d.basic.geometry.modifier.SceneTraverser;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import org.joml.Matrix4d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GaiaBakeTest {

    @Test
    void bake() {

        DefaultSceneFactory sceneFactory = new DefaultSceneFactory();
        GaiaScene scene = sceneFactory.createScene();
        GaiaNode rootNode = scene.getRootNode();
        Matrix4d matrix4d = rootNode.getTransformMatrix();
        matrix4d.rotateX(Math.toRadians(45.0));

        GaiaNode sampleNode = sceneFactory.createGridNode(256, 256);
        rootNode.addChild(sampleNode);

        SceneElementVisitor baker = new Baker();
        SceneTraverser traverser = new SceneTraverser();
        traverser.traverse(scene, baker);
        //GaiaBaker baker = new GaiaBaker();
        //baker.bake(scene);

        Matrix4d identity = new Matrix4d().identity();
        assertEquals(identity, rootNode.getTransformMatrix());

    }

}