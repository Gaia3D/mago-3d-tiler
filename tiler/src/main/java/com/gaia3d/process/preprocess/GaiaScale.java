package com.gaia3d.process.preprocess;

import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.converter.kml.KmlInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import com.gaia3d.process.tileprocess.tile.TileInfo;

@Slf4j
@AllArgsConstructor
public class GaiaScale implements PreProcess {

    @Override
    public TileInfo run(TileInfo tileInfo) {
        KmlInfo kmlInfo = tileInfo.getKmlInfo();
        if (kmlInfo == null) {
            return tileInfo;
        }

        GaiaScene gaiaScene = tileInfo.getScene();
        GaiaNode rootNode = gaiaScene.getNodes().get(0);
        Matrix4d transform = rootNode.getTransformMatrix();

        double scaleX = kmlInfo.getScaleX() <= 0 ? 1.0d : kmlInfo.getScaleX();
        double scaleY = kmlInfo.getScaleY() <= 0 ? 1.0d : kmlInfo.getScaleY();
        double scaleZ = kmlInfo.getScaleZ() <= 0 ? 1.0d : kmlInfo.getScaleZ();
        transform.scale(scaleX, scaleY, scaleZ);
        rootNode.setTransformMatrix(transform);
        gaiaScene.getBoundingBox();
        return tileInfo;
    }
}
