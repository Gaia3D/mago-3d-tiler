package geometry.batch;

import command.KmlInfo;
import de.javagl.jgltf.impl.v1.Scene;
import geometry.basic.GaiaBoundingBox;
import geometry.structure.GaiaNode;
import geometry.structure.GaiaScene;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import tiler.ContentInfo;
import tiler.TileInfo;
import util.GlobeUtils;

@Slf4j
public class GaiaTransfomer {

    public static void translate(CoordinateReferenceSystem source, TileInfo tileInfo) {
        if (source == null && tileInfo != null) {
            return;
        }

        GaiaScene gaiaScene = tileInfo.getScene();
        GaiaBoundingBox boundingBox = gaiaScene.getBoundingBox();


        Vector3d center = boundingBox.getCenter();
        ProjCoordinate centerSource = new ProjCoordinate(center.x, center.y, boundingBox.getMinZ());
        ProjCoordinate centerWgs84 = GlobeUtils.transform(source, centerSource);

        Vector3d position = new Vector3d(centerWgs84.x, centerWgs84.y, 0.0d);

        Vector3d traslation = new Vector3d(center.x, center.y, 0.0d);
        traslation.negate();

        log.info("center: {}", center);
        log.info("traslation: {}", traslation);

        GaiaNode rootNode = gaiaScene.getNodes().get(0);
        Matrix4d transfrom = rootNode.getTransformMatrix();
        Matrix4d resultTransfromMatrix = transfrom.translate(traslation, new Matrix4d());
        //rotateX90(resultTransfromMatrix);

        rootNode.setTransformMatrix(resultTransfromMatrix);
        //rootNode.recalculateTransform();

        gaiaScene.getBoundingBox();

        KmlInfo kmlInfo = KmlInfo.builder()
                .position(position)
                .build();
        tileInfo.setKmlInfo(kmlInfo);
    }

    /*public static void translate(CoordinateReferenceSystem source, ContentInfo contentInfo) {
        for (TileInfo tileInfo : contentInfo.getTileInfos()) {
            KmlInfo kmlInfo = tileInfo.getKmlInfo();
            GaiaScene gaiaScene = tileInfo.getScene();
            if (kmlInfo == null) {
                GaiaBoundingBox boundingBox = gaiaScene.getBoundingBox();
                Vector3d center = boundingBox.getCenter();
                ProjCoordinate centerSource = new ProjCoordinate(center.x, center.y, center.z);
                ProjCoordinate centerWgs84 = GlobeUtils.transform(source, centerSource);

                Vector3d position = new Vector3d(centerWgs84.x, centerWgs84.y, center.z);

                Vector3d traslation = new Vector3d(center.x, center.y, 0.0d);
                traslation.negate();

                log.info("center: {}", center);
                log.info("traslation: {}", traslation);

                GaiaNode rootNode = gaiaScene.getNodes().get(0);
                Matrix4d transfrom = rootNode.getTransformMatrix();
                Matrix4d resultTransfromMatrix = transfrom.translate(traslation, new Matrix4d());
                //rotateX90(resultTransfromMatrix);

                rootNode.setTransformMatrix(resultTransfromMatrix);
                //rootNode.recalculateTransform();

                gaiaScene.getBoundingBox();

                kmlInfo = KmlInfo.builder()
                        .position(position)
                        .build();
                tileInfo.setKmlInfo(kmlInfo);
            }
        }
    }*/

    public static ContentInfo relocation(ContentInfo contentInfo) {
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

            GaiaScene scene = tileInfo.getScene();
            scene.translate(translation);
        }
        return contentInfo;
    }

    private static void rotateX90(Matrix4d matrix) {
        Matrix4d rotationMatrix = new Matrix4d();
        rotationMatrix.identity();
        rotationMatrix.rotateX(Math.toRadians(-90));
        matrix.mul(rotationMatrix, matrix);
    }
}
