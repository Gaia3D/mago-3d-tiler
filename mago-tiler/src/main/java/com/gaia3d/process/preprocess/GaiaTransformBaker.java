package com.gaia3d.process.preprocess;

import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.geometry.modifier.TransformBaker;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class GaiaTransformBaker implements PreProcess {

    private final TransformBaker transformBaker = new TransformBaker();

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();
        transformBaker.bake(scene);
        tileInfo.updateSceneInfo();
        return tileInfo;
    }
}
