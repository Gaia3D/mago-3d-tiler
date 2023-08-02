package geometry.batch;

import command.Configurator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class GaiaBatcherTest {

    private static final String INPUT_PATH = "../../sample/";
    private static final String OUTPUT_PATH = "../../output/";

    @Test
    void loadColladaWithKml() throws URISyntaxException, IOException {
        Configurator.initLogger();
        /*File kml = new File(getAbsolutePath(INPUT_PATH) + "a_bd001.kml");
        KmlInfo kmlInfo = KmlReader.read(kml);
        Vector3d lonlat = new Vector3d(kmlInfo.getLongitude(), kmlInfo.getLatitude(), 0);

        File kml2 = new File(getAbsolutePath(INPUT_PATH) + "a_bd002.kml");
        KmlInfo kmlInfo2 = KmlReader.read(kml2);
        Vector3d lonlat2 = new Vector3d(kmlInfo2.getLongitude(), kmlInfo2.getLatitude(), 0);

        File kml3 = new File(getAbsolutePath(INPUT_PATH) + "a_bd003.kml");
        KmlInfo kmlInfo3 = KmlReader.read(kml3);
        Vector3d lonlat3 = new Vector3d(kmlInfo3.getLongitude(), kmlInfo3.getLatitude(), 0);

        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        boundingBox.addPoint(lonlat);
        boundingBox.addPoint(lonlat2);
        boundingBox.addPoint(lonlat3);

        Vector3d center = boundingBox.getCenter();

        log.info("Center: " + center);*/

        /*double[] lonlat1C = GlobeUtils.geographicToCartesianWgs84(lonlat.x, lonlat.y, lonlat.z);
        double[] lonlat2C = GlobeUtils.geographicToCartesianWgs84(lonlat2.x, lonlat2.y, lonlat2.z);
        double[] lonlat3C = GlobeUtils.geographicToCartesianWgs84(lonlat3.x, lonlat3.y, lonlat3.z);
        double[] centerC = GlobeUtils.geographicToCartesianWgs84(center.x, center.y, center.z);

        Vector3d translation1 = new Vector3d(centerC[0] - lonlat1C[0], centerC[1] - lonlat1C[1], centerC[2] - lonlat1C[2]);
        Vector3d translation2 = new Vector3d(centerC[0] - lonlat2C[0], centerC[1] - lonlat2C[1], centerC[2] - lonlat2C[2]);
        Vector3d translation3 = new Vector3d(centerC[0] - lonlat3C[0], centerC[1] - lonlat3C[1], centerC[2] - lonlat3C[2]);

        Converter converter = new AssimpConverter(null);
        GaiaScene scene = converter.load(getAbsolutePath(INPUT_PATH) + kmlInfo.getHref());
        GaiaScene scene2 = converter.load(getAbsolutePath(INPUT_PATH) + kmlInfo2.getHref());
        GaiaScene scene3 = converter.load(getAbsolutePath(INPUT_PATH) + kmlInfo3.getHref());

        GaiaNode node = scene.getNodes().get(0);
        GaiaNode node2 = scene2.getNodes().get(0);
        GaiaNode node3 = scene3.getNodes().get(0);

        Matrix4d nodeTransform = node.getTransformMatrix();
        Matrix4d node2Transform = node2.getTransformMatrix();
        Matrix4d node3Transform = node3.getTransformMatrix();

        nodeTransform.setTranslation(translation1);
        node2Transform.setTranslation(translation2);
        node3Transform.setTranslation(translation3);

        nodeTransform.rotateX(Math.toRadians(90), nodeTransform);
        node2Transform.rotateX(Math.toRadians(90), node2Transform);
        node3Transform.rotateX(Math.toRadians(90), node3Transform);

        node.recalculateTransform();
        node2.recalculateTransform();
        node3.recalculateTransform();


        BatchInfo batchInfo = new BatchInfo();
        GaiaUniverse universe = new GaiaUniverse("test", new File(getAbsolutePath(INPUT_PATH)), new File(getAbsolutePath(OUTPUT_PATH)));
        universe.getScenes().add(scene);
        universe.getScenes().add(scene2);
        universe.getScenes().add(scene3);

        batchInfo.setLod(LevelOfDetail.LOD0);
        batchInfo.setUniverse(universe);
        batchInfo.setNodeCode("TEST");
        batchInfo.setBoundingBox(scene.getBoundingBox());
        Batcher batcher = new GaiaBatcher(batchInfo, null);
        GaiaSet gaiaSet = batcher.batch();
        GaiaScene batchedScene = new GaiaScene(gaiaSet);

        GltfWriter gltfWriter = new GltfWriter();
        gltfWriter.writeGltf(batchedScene, getAbsolutePath(OUTPUT_PATH) + "batch.gltf");*/
    }

    private String getAbsolutePath(String classPath) throws URISyntaxException {
        File file = new File(getClass().getResource(classPath).toURI());
        assert(file != null);
        return file.getAbsolutePath() + File.separator;
    }
}