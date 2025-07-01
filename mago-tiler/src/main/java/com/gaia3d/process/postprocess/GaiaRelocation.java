package com.gaia3d.process.postprocess;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.util.GlobeUtils;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;

@Slf4j
@AllArgsConstructor
public class GaiaRelocation implements PostProcess {
    @Override
    public ContentInfo run(ContentInfo contentInfo) {
        GaiaBoundingBox allBoundingBox = contentInfo.getBoundingBox();
        Vector3d centerCartographic = allBoundingBox.getCenter();
        Vector3d centerCartesian = GlobeUtils.geographicToCartesianWgs84(centerCartographic);
        Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerCartesian);
        Matrix4d transformMatrixInv = new Matrix4d(transformMatrix).invert();
        for (TileInfo tileInfo : contentInfo.getTileInfos()) {
            KmlInfo kmlInfo = tileInfo.getKmlInfo();
            Vector3d kmlCenter = kmlInfo.getPosition();
            kmlCenter = GlobeUtils.geographicToCartesianWgs84(kmlCenter);

            Matrix4d resultTransformMatrix = transformMatrixInv.translate(kmlCenter, new Matrix4d());

            double x = resultTransformMatrix.get(3, 0);
            double y = resultTransformMatrix.get(3, 1);
            double z = resultTransformMatrix.get(3, 2);

            Vector3d translation = new Vector3d(x, y, z);

            GaiaSet set = tileInfo.getSet();
            if (set == null) {
                log.error("GaiaSet is null");
                continue;
            }
            set.translate(translation);
        }
        return contentInfo;
    }
}
