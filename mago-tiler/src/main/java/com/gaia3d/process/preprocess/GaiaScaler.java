package com.gaia3d.process.preprocess;

import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.converter.kml.TileTransformInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import com.gaia3d.process.tileprocess.tile.TileInfo;

import java.util.List;

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

        List<GaiaNode> nodes = gaiaScene.getNodes();
        for (GaiaNode node : nodes) {
            Matrix4d transform = node.getTransformMatrix();
            Matrix4d scaleMatrix = new Matrix4d().identity();
            scaleMatrix.scale(tileTransformInfo.getScaleX(), tileTransformInfo.getScaleY(), tileTransformInfo.getScaleZ());
            transform.mul(scaleMatrix);
        }

        tileInfo.updateSceneInfo();
        return tileInfo;
    }
}
