package process.preprocess;

import basic.geometry.GaiaBoundingBox;
import basic.structure.GaiaNode;
import basic.structure.GaiaScene;
import converter.kml.KmlInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import process.tileprocess.tile.TileInfo;
import util.GlobeUtils;

@Slf4j
@AllArgsConstructor
public class GaiaTranslator implements PreProcess {

    private final CoordinateReferenceSystem source;

    @Override
    public TileInfo run(TileInfo tileInfo) {
        if (source == null && tileInfo != null) {
            return tileInfo;
        }
        GaiaScene gaiaScene = tileInfo.getScene();
        GaiaBoundingBox boundingBox = gaiaScene.getBoundingBox();
        //GaiaBoundingBox boundingBox = tileInfo.getBoundingBox();

        Vector3d center = boundingBox.getCenter();
        ProjCoordinate centerSource = new ProjCoordinate(center.x, center.y, boundingBox.getMinZ());
        ProjCoordinate centerWgs84 = GlobeUtils.transform(source, centerSource);

        Vector3d position = new Vector3d(centerWgs84.x, centerWgs84.y, 0.0d);

        Vector3d traslation = new Vector3d(center.x, center.y, 0.0d);
        traslation.negate();

        GaiaNode rootNode = gaiaScene.getNodes().get(0);
        Matrix4d transfrom = rootNode.getTransformMatrix();
        Matrix4d resultTransfromMatrix = transfrom.translate(traslation, new Matrix4d());

        //tileInfo.setTransformMatrix(resultTransfromMatrix);
        rootNode.setTransformMatrix(resultTransfromMatrix);
        gaiaScene.getBoundingBox();

        KmlInfo kmlInfo = KmlInfo.builder().position(position).build();
        tileInfo.setKmlInfo(kmlInfo);
        return tileInfo;
    }
}
