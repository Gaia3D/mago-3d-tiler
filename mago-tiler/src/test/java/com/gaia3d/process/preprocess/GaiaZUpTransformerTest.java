package com.gaia3d.process.preprocess;

import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import org.joml.Matrix4d;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("default")
class GaiaZUpTransformerTest {

    @Test
    void preservesGltfNodeTransformBecauseTheFormatDefinesYUp() {
        GaiaScene scene = sceneWithXRotation(90.0, "model.glb");
        Matrix4d originalTransform = new Matrix4d(scene.getNodes().getFirst().getTransformMatrix());
        TileInfo tileInfo = TileInfo.builder().scene(scene).build();

        new GaiaZUpTransformer().run(tileInfo);

        assertEquals(originalTransform, scene.getNodes().getFirst().getTransformMatrix());
    }

    @Test
    void stillAutoDetectsFormatsWithoutAnExplicitUpAxis() {
        GaiaScene scene = sceneWithXRotation(90.0, "model.obj");
        Matrix4d expectedTransform = new Matrix4d(scene.getNodes().getFirst().getTransformMatrix())
                .rotateX(Math.toRadians(90.0));
        TileInfo tileInfo = TileInfo.builder().scene(scene).build();

        new GaiaZUpTransformer().run(tileInfo);

        assertTrue(expectedTransform.equals(scene.getNodes().getFirst().getTransformMatrix(), 1e-12));
    }

    private GaiaScene sceneWithXRotation(double degrees, String fileName) {
        GaiaScene scene = new GaiaScene();
        GaiaNode rootNode = new GaiaNode();
        rootNode.setTransformMatrix(new Matrix4d().rotateX(Math.toRadians(degrees)));
        scene.getNodes().add(rootNode);
        scene.setOriginalPath(Path.of(fileName));
        return scene;
    }
}
