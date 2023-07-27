package converter;

import command.Configurator;
import command.KmlInfo;
import command.KmlReader;
import geometry.batch.Batcher;
import geometry.batch.GaiaBatcher;
import geometry.exchangable.GaiaSet;
import geometry.exchangable.GaiaUniverse;
import geometry.structure.GaiaScene;
import gltf.GltfWriter;
import org.junit.jupiter.api.Test;
import tiler.BatchInfo;
import tiler.LevelOfDetail;
import util.ImageUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AssimpConverterTest {

    private static final String INPUT_PATH = "../sample/";
    private static final String OUTPUT_PATH = "../output/";

    @Test
    void load() throws URISyntaxException {
        Converter converter = new AssimpConverter(null);
        GaiaScene scene = converter.load(getAbsolutePath(INPUT_PATH) + "a_bd001.3ds");
        assertNotNull(scene);
    }

    @Test
    void loadCollada() throws URISyntaxException {
        Converter converter = new AssimpConverter(null);
        GaiaScene scene = converter.load(getAbsolutePath(INPUT_PATH) + "a_bd001.dae");
        assertNotNull(scene);
    }

    @Test
    void loadColladaWithKml() throws URISyntaxException, IOException {
        Configurator.initLogger();
        File kml = new File(getAbsolutePath(INPUT_PATH) + "a_bd001.kml");
        KmlInfo kmlInfo = KmlReader.read(kml);

        Converter converter = new AssimpConverter(null);
        GaiaScene scene = converter.load(getAbsolutePath(INPUT_PATH) + kmlInfo.getHref());
        assertNotNull(scene);


        BatchInfo batchInfo = new BatchInfo();
        GaiaUniverse universe = new GaiaUniverse("test", new File(getAbsolutePath(INPUT_PATH)), new File(getAbsolutePath(OUTPUT_PATH)));
        universe.getScenes().add(scene);

        batchInfo.setLod(LevelOfDetail.LOD0);
        batchInfo.setUniverse(universe);
        batchInfo.setNodeCode("TEST");
        batchInfo.setBoundingBox(scene.getBoundingBox());
        Batcher batcher = new GaiaBatcher(batchInfo, null);
        GaiaSet gaiaSet = batcher.batch();
        GaiaScene batchedScene = new GaiaScene(gaiaSet);

        GltfWriter gltfWriter = new GltfWriter();
        gltfWriter.writeGltf(batchedScene, getAbsolutePath(OUTPUT_PATH) + "a_bd001.gltf");
    }

    private String getAbsolutePath(String classPath) throws URISyntaxException {
        File file = new File(getClass().getResource(classPath).toURI());
        assert(file != null);
        return file.getAbsolutePath() + File.separator;
    }
}