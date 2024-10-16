package com.gaia3d.process.preprocess;

import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class GaiaTileInfoInitiator implements PreProcess {

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();
        GaiaNode rootNode = scene.getNodes().get(0);

        tileInfo.setName(rootNode.getName());
        tileInfo.setTransformMatrix(rootNode.getTransformMatrix());
        tileInfo.setBoundingBox(scene.getBoundingBox());
        tileInfo.setScenePath(tileInfo.getScene().getOriginalPath());
        tileInfo.setTempPath(tileInfo.getOutputPath().resolve("temp"));
        tileInfo.setTriangleCount(tileInfo.getScene().calcTriangleCount());
        return tileInfo;
    }
}
