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
@Deprecated
public class GaiaRotatorOld implements PreProcess {

    private GaiaScene recentScene = null;

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        boolean isSwapUpAxis = globalOptions.isSwapUpAxis();
        boolean isFlipUpAxis = globalOptions.isFlipUpAxis();

        GaiaScene gaiaScene = tileInfo.getScene();
        if (recentScene == gaiaScene) {
            return tileInfo;
        }
        recentScene = gaiaScene;

        // 90 degree rotation
        double rotateX = isSwapUpAxis ? 90 : 0;
        // Reverse the rotation direction
        rotateX = isFlipUpAxis ? rotateX + 180 : rotateX;

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
}
