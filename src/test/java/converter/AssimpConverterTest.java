package converter;

import command.Configurator;
import converter.kml.KmlInfo;
import converter.kml.KmlReader;
import converter.assimp.AssimpConverter;
import converter.assimp.Converter;
import process.postprocess.batch.Batcher;
import process.postprocess.batch.GaiaBatcher;
import basic.exchangable.GaiaUniverse;
import basic.structure.GaiaScene;
import org.junit.jupiter.api.Test;
import process.tileprocess.tile.ContentInfo;
import process.tileprocess.tile.LevelOfDetail;

import javax.xml.parsers.ParserConfigurationException;
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
    void loadColladaWithKml() throws URISyntaxException, IOException, ParserConfigurationException {
        Configurator.initLogger();
        File kml = new File(getAbsolutePath(INPUT_PATH) + "a_bd001.kml");
        KmlReader kmlReader = new KmlReader();
        KmlInfo kmlInfo = kmlReader.read(kml);

        Converter converter = new AssimpConverter(null);
        GaiaScene scene = converter.load(getAbsolutePath(INPUT_PATH) + kmlInfo.getHref());
        assertNotNull(scene);


        ContentInfo batchInfo = new ContentInfo();
        //GaiaUniverse universe = new GaiaUniverse("test", new File(getAbsolutePath(INPUT_PATH)), new File(getAbsolutePath(OUTPUT_PATH)));
        //universe.getScenes().add(scene);

        batchInfo.setLod(LevelOfDetail.LOD0);
        batchInfo.setNodeCode("TEST");
        batchInfo.setBoundingBox(scene.getBoundingBox());
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