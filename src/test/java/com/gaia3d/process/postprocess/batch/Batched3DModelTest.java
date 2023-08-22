package com.gaia3d.process.postprocess.batch;

import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.command.Configurator;
import com.gaia3d.converter.assimp.AssimpConverter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class Batched3DModelTest {
    @Test
    void extract() {
        Configurator.initConsoleLogger();
        Batched3DModel batched3DModel = new Batched3DModel(null);
        File b3dm = new File("D:\\workspaces\\plasma-tester\\data\\ion-seoul\\7\\66\\89.b3dm");
        File output = new File("D:\\extracted.glb");
        batched3DModel.extract(b3dm, output);

        //AssimpConverter assimpConverter = new AssimpConverter(null);
        //GaiaScene gaiaScene = assimpConverter.load(output);
    }
}