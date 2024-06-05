package com.gaia3d.process.preprocess;

import com.gaia3d.basic.structure.GaiaNode;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.KmlInfo;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import com.gaia3d.process.tileprocess.tile.TileInfo;

@Slf4j
@NoArgsConstructor
public class GaiaRotator implements PreProcess {

    private GaiaScene recentScene = null;

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        boolean isSwapUpAxis = globalOptions.isSwapUpAxis();
        boolean isReverseUpAxis = globalOptions.isReverseUpAxis();

        GaiaScene gaiaScene = tileInfo.getScene();
        if (recentScene == gaiaScene) {
            return tileInfo;
        }
        recentScene = gaiaScene;

        // 90 degree rotation
        double rotateX = isSwapUpAxis ? 90 : 0;
        // Reverse the rotation direction
        rotateX = isReverseUpAxis ? rotateX + 180 : rotateX;

        GaiaNode rootNode = gaiaScene.getNodes().get(0);
        Matrix4d transform = rootNode.getTransformMatrix();

        Matrix4d xRotMatrix = new Matrix4d();
        xRotMatrix.rotateX(Math.toRadians(rotateX));
        xRotMatrix.mul(transform, transform);

        /* set the transform matrix */
        rootNode.setTransformMatrix(transform);
        tileInfo.setTransformMatrix(transform);
        gaiaScene.getBoundingBox();
        return tileInfo;
    }

    /*private void rotateX(Matrix4d matrix, double angle) {
        Matrix4d rotationMatrix = new Matrix4d();
        rotationMatrix.identity();
        rotationMatrix.rotateX(Math.toRadians(angle));
        matrix.mul(rotationMatrix, matrix);
    }*/
}
