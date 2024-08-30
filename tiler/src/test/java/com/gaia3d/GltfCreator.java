package com.gaia3d;

import com.gaia3d.basic.structure.GaiaNode;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.command.Configurator;
import com.gaia3d.converter.EasySceneCreator;
import com.gaia3d.converter.jgltf.GltfWriter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
public class GltfCreator {

    @Test
    public void run() {
        Configurator.initConsoleLogger();

        File file = new File("D:/workspaces/scene.gltf");
        EasySceneCreator easySceneCreator = new EasySceneCreator();
        GaiaScene gaiaScene = easySceneCreator.createScene(file);
        GaiaNode rootNode = gaiaScene.getNodes().get(0);

        int gridSize = 512;
        GaiaNode gridNode = easySceneCreator.createGridNode(gridSize, gridSize);
        rootNode.getChildren().add(gridNode);

        GltfWriter gltfWriter = new GltfWriter();
        gltfWriter.writeGltf(gaiaScene, file);
    }
}
