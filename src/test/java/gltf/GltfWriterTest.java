package gltf;

import assimp.AssimpConverter;
import command.Configurator;
import geometry.structure.GaiaScene;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class GltfWriterTest {

    private static final String INPUT_PATH = "../sample/";
    private static final String OUTPUT_PATH = "../output/";

    @Test
    void writeGltf() throws URISyntaxException {
        Configurator.initLogger();
        AssimpConverter converter = new AssimpConverter(null);
        GaiaScene scene = converter.load(getAbsolutePath(INPUT_PATH) + "a_bd001.3ds", "3ds");
        GltfWriter.writeGltf(scene, getAbsolutePath(OUTPUT_PATH) + "a_bd001.gltf");
    }

    @Test
    void writeGlb() throws URISyntaxException {
        Configurator.initLogger();
        AssimpConverter converter = new AssimpConverter(null);
        GaiaScene scene = converter.load(getAbsolutePath(INPUT_PATH) + "a_bd001.3ds", "3ds");
        GltfWriter.writeGlb(scene, getAbsolutePath(OUTPUT_PATH) + "a_bd001.glb");
    }

    private String getAbsolutePath(String classPath) throws URISyntaxException {
        File file = new File(getClass().getResource(classPath).toURI());
        assert(file != null);
        return file.getAbsolutePath() + File.separator;
    }
}