package com.gaia3d.process.preprocess;

import com.gaia3d.basic.structure.GaiaNode;
import com.gaia3d.basic.structure.GaiaScene;
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
        GaiaScene gaiaScene = tileInfo.getScene();
        GaiaNode rootNode = gaiaScene.getNodes().get(0);
        Matrix4d transform = rootNode.getTransformMatrix();

        if (recentScene == gaiaScene) {
            return tileInfo;
        }
        recentScene = gaiaScene;

        Matrix4d xRotMatrix = new Matrix4d();
        xRotMatrix.rotateX(Math.toRadians(90));
        xRotMatrix.mul(transform, transform);

        rootNode.setTransformMatrix(transform);
        tileInfo.setTransformMatrix(transform);
        gaiaScene.getBoundingBox();
        return tileInfo;
    }
}
