package com.gaia3d.process.postprocess;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.util.GlobeUtils;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.AllArgsConstructor;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;

@AllArgsConstructor
public class GaiaRelocator implements PostProcess {
    @Override
    public ContentInfo run(ContentInfo contentInfo) {
        GaiaBoundingBox allBoundingBox = contentInfo.getBoundingBox();
        Vector3d center = allBoundingBox.getCenter();
        center = GlobeUtils.geographicToCartesianWgs84(center);
        Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(center);
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
        }
        return contentInfo;
    }
}
