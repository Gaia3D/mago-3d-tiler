package com.gaia3d.process.preprocess;

import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.geometry.modifier.transform.GaiaBaker;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class GaiaTransformBaker implements PreProcess {

    private final GaiaBaker backer = new GaiaBaker();

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();
        backer.apply(scene);
        tileInfo.updateSceneInfo();
        return tileInfo;
    }
}
