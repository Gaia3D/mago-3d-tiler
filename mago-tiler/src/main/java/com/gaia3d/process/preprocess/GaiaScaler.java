package com.gaia3d.process.preprocess;

import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.converter.kml.TileTransformInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import com.gaia3d.process.tileprocess.tile.TileInfo;

@Slf4j
@AllArgsConstructor
public class GaiaScaler implements PreProcess {

    @Override
    public TileInfo run(TileInfo tileInfo) {
        TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
        if (tileTransformInfo == null) {
            return tileInfo;
        }

        GaiaScene gaiaScene = tileInfo.getScene();
        GaiaNode rootNode = gaiaScene.getNodes().get(0);
        Matrix4d transform = rootNode.getTransformMatrix();

        double scaleX = tileTransformInfo.getScaleX() <= 0 ? 1.0d : tileTransformInfo.getScaleX();
        double scaleY = tileTransformInfo.getScaleY() <= 0 ? 1.0d : tileTransformInfo.getScaleY();
        double scaleZ = tileTransformInfo.getScaleZ() <= 0 ? 1.0d : tileTransformInfo.getScaleZ();
        transform.scale(scaleX, scaleY, scaleZ);
        rootNode.setTransformMatrix(transform);

        tileInfo.updateSceneInfo();
        return tileInfo;
    }
}
