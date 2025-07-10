package com.gaia3d.process.preprocess;

import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.preprocess.sub.UpAxisTransformer;
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
        GaiaScene scene = tileInfo.getScene();
        double rotateX = globalOptions.getRotateX();

        if (recentScene == scene) {
            return tileInfo;
        }
        recentScene = scene;

        if (rotateX == 0.0) {
            return tileInfo;
        } else {
            UpAxisTransformer.rotateDegreeX(scene, rotateX);
            return tileInfo;
        }
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

    private Matrix4d rotateX(Matrix4d transformMatrix, double degree) {
        Matrix4d xRotMatrix = new Matrix4d();
        xRotMatrix.rotateX(Math.toRadians(degree));
        xRotMatrix.mul(transformMatrix, transformMatrix);
        return transformMatrix;
    }
}
