package com.gaia3d.converter.jgltf;

import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.Configurator;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.assimp.AssimpConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
class GltfWriterTest {

    @Disabled
    @Test
    void testQuantization() {
        Configurator.initConsoleLogger();
        Configurator.setLevel(Level.DEBUG);

        String inputPath = "D:/data/mago-3d-tiler/release-sample/sample-tree";
        String outputPath = "E:/data/mago-server/output/QUANTIZATION";
        File outputDir = new File(outputPath);
        if (outputDir.exists()) {
            outputDir.mkdirs();
        } else {
            outputDir.mkdirs();
        }

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setInputPath(inputPath);
        globalOptions.setOutputPath(outputPath);

        File file = FileUtils.listFiles(new File(inputPath), new String[]{"glb"}, true).stream().findFirst().orElseThrow();
        AssimpConverter converter = new AssimpConverter();
        GaiaScene scene = converter.load(file).stream().findFirst().orElseThrow();

        GltfWriter gltfWriter = new GltfWriter();
        gltfWriter.writeGltf(scene, outputPath + "/output.gltf");
    }
}