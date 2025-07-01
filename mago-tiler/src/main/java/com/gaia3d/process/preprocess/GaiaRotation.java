package com.gaia3d.process.preprocess;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

@Slf4j
@NoArgsConstructor
public class GaiaRotation implements PreProcess {
    private GaiaScene recentScene = null;

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();

        double rotateX = globalOptions.getRotateX();
        boolean isSwapUpAxis = globalOptions.isSwapUpAxis();
        boolean isFlipUpAxis = globalOptions.isFlipUpAxis();

        if (isSwapUpAxis) {
            rotateX -= 90;
        }
        if (isFlipUpAxis) {
            rotateX += 180;
        }
        rotateX += 90;

        GaiaScene gaiaScene = tileInfo.getScene();
        // Skip if the scene is already processed
        if (recentScene == gaiaScene) {
            return tileInfo;
        }
        recentScene = gaiaScene;

        //log.info("rotateX: {}, isSwapUpAxis: {}, isFlipUpAxis: {}", rotateX, isSwapUpAxis, isFlipUpAxis);
        // 90 degree rotation
        //double rotateX = isSwapUpAxis ? 90 : 0;
        // Reverse the rotation direction
        //rotateX = isFlipUpAxis ? rotateX + 180 : rotateX;

        GaiaNode rootNode = gaiaScene.getNodes().get(0);
        Matrix4d transform = rootNode.getTransformMatrix();

        //log.info("before transform");
        //log.info(transform.toString());

        rotateX(transform, rotateX);

        /*KmlInfo kml = tileInfo.getKmlInfo();
        if (kml != null) {
            double heading = -kml.getHeading();
            double tilt = -kml.getTilt();
            double roll = -kml.getRoll();
            rotateXYZ(transform, heading, tilt, roll);
        }*/

        /* set the transform matrix */
        rootNode.setTransformMatrix(transform);
        tileInfo.setTransformMatrix(transform);
        GaiaBoundingBox boundingBox = gaiaScene.getBoundingBox();
        tileInfo.setBoundingBox(boundingBox.clone());

        //log.info("after transform");
        //log.info(transform.toString());
        return tileInfo;
    }

    private Matrix4d rotateXYZ(Matrix4d transformMatrix, double x, double y, double z) {
        Matrix4d xRotMatrix = new Matrix4d();
        xRotMatrix.rotateX(Math.toRadians(x));
        xRotMatrix.mul(transformMatrix, transformMatrix);

        Matrix4d yRotMatrix = new Matrix4d();
        yRotMatrix.rotateY(Math.toRadians(y));
        yRotMatrix.mul(transformMatrix, transformMatrix);

        Matrix4d zRotMatrix = new Matrix4d();
        zRotMatrix.rotateZ(Math.toRadians(z));
        zRotMatrix.mul(transformMatrix, transformMatrix);
        return transformMatrix;
    }

    private Matrix4d rotateY(Matrix4d transformMatrix, double degree) {
        Matrix4d zRotMatrix = new Matrix4d();
        zRotMatrix.rotateZ(Math.toRadians(degree));
        zRotMatrix.mul(transformMatrix, transformMatrix);
        return transformMatrix;
    }

    private Matrix4d rotateZ(Matrix4d transformMatrix, double degree) {
        Matrix4d zRotMatrix = new Matrix4d();
        zRotMatrix.rotateZ(Math.toRadians(degree));
        zRotMatrix.mul(transformMatrix, transformMatrix);
        return transformMatrix;
    }

    private Matrix4d rotateX(Matrix4d transformMatrix , double degree) {
        Matrix4d xRotMatrix = new Matrix4d();
        xRotMatrix.rotateX(Math.toRadians(degree));
        xRotMatrix.mul(transformMatrix, transformMatrix);
        return transformMatrix;
    }
}
