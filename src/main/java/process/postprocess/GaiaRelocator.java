package process.postprocess;

import basic.exchangable.GaiaSet;
import converter.kml.KmlInfo;
import basic.geometry.GaiaBoundingBox;
import basic.structure.GaiaScene;
import lombok.AllArgsConstructor;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import process.tileprocess.tile.ContentInfo;
import process.tileprocess.tile.TileInfo;
import util.GlobeUtils;

@AllArgsConstructor
public class GaiaRelocator implements PostProcess {

    @Override
    public ContentInfo run(ContentInfo contentInfo) {
        GaiaBoundingBox allBoundingBox = contentInfo.getBoundingBox();
        Vector3d center = allBoundingBox.getCenter();
        center = GlobeUtils.geographicToCartesianWgs84(center);
        Matrix4d transformMatrix = GlobeUtils.normalAtCartesianPointWgs84(center);
        Matrix4d transformMatrixInv = new Matrix4d(transformMatrix).invert();
        for (TileInfo tileInfo : contentInfo.getTileInfos()) {
            KmlInfo kmlInfo = tileInfo.getKmlInfo();
            Vector3d kmlCenter = kmlInfo.getPosition();
            kmlCenter = GlobeUtils.geographicToCartesianWgs84(kmlCenter);

            Matrix4d resultTransfromMatrix = transformMatrixInv.translate(kmlCenter, new Matrix4d());

            double x = resultTransfromMatrix.get(3, 0);
            double y = resultTransfromMatrix.get(3, 1);
            double z = resultTransfromMatrix.get(3, 2);

            Vector3d translation = new Vector3d(x, y, z);

            GaiaSet set = tileInfo.getSet();
            set.translate(translation);
            //GaiaScene scene = tileInfo.getScene();
            //scene.translate(translation);
        }
        return contentInfo;
    }
}
