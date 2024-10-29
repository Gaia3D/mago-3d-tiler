package com.gaia3d.process.preprocess;

import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

@Slf4j
@NoArgsConstructor
public class GaiaRotator implements PreProcess {
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

        /* set the transform matrix */
        rootNode.setTransformMatrix(transform);
        tileInfo.setTransformMatrix(transform);
        gaiaScene.getBoundingBox();

        //log.info("after transform");
        //log.info(transform.toString());
        return tileInfo;
    }

    private Matrix4d rotateX(Matrix4d transformMatrix , double degree) {
        Matrix4d xRotMatrix = new Matrix4d();
        xRotMatrix.rotateX(Math.toRadians(degree));
        xRotMatrix.mul(transformMatrix, transformMatrix);
        return transformMatrix;
    }
}
