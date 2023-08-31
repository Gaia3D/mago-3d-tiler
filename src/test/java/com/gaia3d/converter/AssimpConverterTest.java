package com.gaia3d.converter;

import com.gaia3d.command.Configurator;
import com.gaia3d.converter.kml.FastKmlReader;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.process.postprocess.batch.Batcher;
import com.gaia3d.process.postprocess.batch.GaiaBatcher;
import com.gaia3d.basic.structure.GaiaScene;
import org.junit.jupiter.api.Test;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.LevelOfDetail;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AssimpConverterTest {

    private static final String INPUT_PATH = "../sample/";
    private static final String OUTPUT_PATH = "../output/";

    @Test
    void load() throws URISyntaxException {
        Converter converter = new AssimpConverter(null);
        List<GaiaScene> scenes = converter.load(getAbsolutePath(INPUT_PATH) + "a_bd001.3ds");
        assertNotNull(scenes);
    }

    @Test
    void loadCollada() throws URISyntaxException {
        Converter converter = new AssimpConverter(null);
        List<GaiaScene> scenes = converter.load(getAbsolutePath(INPUT_PATH) + "a_bd001.dae");
        assertNotNull(scenes);
    }

    @Test
    void loadColladaWithKml() throws URISyntaxException, IOException, ParserConfigurationException {
        Configurator.initConsoleLogger();
        File kml = new File(getAbsolutePath(INPUT_PATH) + "a_bd001.kml");
        //KmlReader kmlReader = new KmlReader();
        FastKmlReader kmlReader = new FastKmlReader();

        KmlInfo kmlInfo = kmlReader.read(kml);

        Converter converter = new AssimpConverter(null);
        List<GaiaScene> scenes = converter.load(getAbsolutePath(INPUT_PATH) + kmlInfo.getHref());
        assertNotNull(scenes);


        ContentInfo batchInfo = new ContentInfo();
        //GaiaUniverse universe = new GaiaUniverse("test", new File(getAbsolutePath(INPUT_PATH)), new File(getAbsolutePath(OUTPUT_PATH)));
        //universe.getScenes().add(scene);

        batchInfo.setLod(LevelOfDetail.LOD0);
        batchInfo.setNodeCode("TEST");
        batchInfo.setBoundingBox(scenes.get(0).getBoundingBox());
        Batcher batcher = new GaiaBatcher(null);
        //GaiaSet gaiaSet = batcher.batch(batchInfo);
        //GaiaScene batchedScene = new GaiaScene(gaiaSet);

        //GltfWriter gltfWriter = new GltfWriter();
        //gltfWriter.writeGltf(batchedScene, getAbsolutePath(OUTPUT_PATH) + "a_bd001.gltf");
    }

    private String getAbsolutePath(String classPath) throws URISyntaxException {
        File file = new File(getClass().getResource(classPath).toURI());
        assert(file != null);
        return file.getAbsolutePath() + File.separator;
    }
}