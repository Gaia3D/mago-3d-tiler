package com.gaia3d.process.postprocess;

import com.gaia3d.basic.exchangable.GaiaBuffer;
import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.util.GlobeUtils;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import org.joml.Vector4d;
import org.locationtech.proj4j.CoordinateReferenceSystem;

@Slf4j
@AllArgsConstructor
public class GaiaRelocator implements PostProcess {
    @Override
    public ContentInfo run(ContentInfo contentInfo) {

        GlobalOptions options = GlobalOptions.getInstance();
        CoordinateReferenceSystem sourceCrs = options.getSourceCrs();
        if (sourceCrs != null && sourceCrs.getName().equals("EPSG:4978")) {
            return relocateCartesian(contentInfo);
        } else {
            return relocateCartographicStrict(contentInfo);
        }
    }

    private ContentInfo relocateCartographicStrict(ContentInfo contentInfo) {
        GaiaBoundingBox allBoundingBox = contentInfo.getBoundingBox();
        Vector3d centerCartographic = allBoundingBox.getCenter();
        Vector3d centerCartesian = GlobeUtils.geographicToCartesianWgs84(centerCartographic);
        Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerCartesian);
        Matrix4d transformMatrixInv = new Matrix4d(transformMatrix).invert();
        for (TileInfo tileInfo : contentInfo.getTileInfos()) {
            TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
            Vector3d kmlCenter = tileTransformInfo.getPosition();
            kmlCenter = GlobeUtils.geographicToCartesianWgs84(kmlCenter);

            Matrix4d kmlTMat = GlobeUtils.transformMatrixAtCartesianPointWgs84(kmlCenter);

            GaiaSet set = tileInfo.getSet();
            if (set == null) {
                log.error("GaiaSet is null");
                continue;
            }

            for (GaiaBufferDataSet bufferData : set.getBufferDataList()) {
                GaiaBuffer positionBuffer = bufferData.getBuffers().get(AttributeType.POSITION);

                if (positionBuffer == null) {
                    log.error("[ERROR] Position buffer is null");
                    break;
                }

                float[] positions = positionBuffer.getFloats();
                for (int i = 0; i < positions.length; i += 3) {
                    Vector4d pos = new Vector4d(positions[i], positions[i + 1], positions[i + 2], 1.0);
                    Vector4d posWC = kmlTMat.transform(pos, new Vector4d());
                    Vector4d posLC = transformMatrixInv.transform(posWC, new Vector4d());
                    positions[i] = (float) posLC.x;
                    positions[i + 1] = (float) posLC.y;
                    positions[i + 2] = (float) posLC.z;
                }
            }
        }
        return contentInfo;
    }

    private ContentInfo relocateCartographic(ContentInfo contentInfo) {
        GaiaBoundingBox allBoundingBox = contentInfo.getBoundingBox();
        Vector3d centerCartographic = allBoundingBox.getCenter();
        Vector3d centerCartesian = GlobeUtils.geographicToCartesianWgs84(centerCartographic);
        Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerCartesian);
        Matrix4d transformMatrixInv = new Matrix4d(transformMatrix).invert();
        for (TileInfo tileInfo : contentInfo.getTileInfos()) {
            TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
            Vector3d kmlCenter = tileTransformInfo.getPosition();
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

    private ContentInfo relocateCartesian(ContentInfo contentInfo) {
        GaiaBoundingBox allBoundingBox = contentInfo.getBoundingBox();
        Vector3d centerCartesian = allBoundingBox.getCenter();

        Matrix4d transformMatrix = new Matrix4d().identity();
        transformMatrix.translate(centerCartesian);
        Matrix4d transformMatrixInv = new Matrix4d(transformMatrix).invert();

        for (TileInfo tileInfo : contentInfo.getTileInfos()) {
            TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
            Vector3d cartesianPosition = tileTransformInfo.getPosition();
            Matrix4d resultTransformMatrix = transformMatrixInv.translate(cartesianPosition, new Matrix4d());
            Vector3d translatedPosition = new Vector3d();
            resultTransformMatrix.getTranslation(translatedPosition);

            GaiaSet set = tileInfo.getSet();
            if (set == null) {
                log.error("GaiaSet is null");
                continue;
            }
            set.translate(translatedPosition);
        }
        return contentInfo;
    }
}
