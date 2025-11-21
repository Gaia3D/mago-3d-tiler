package com.gaia3d.process.preprocess;

import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class GaiaSceneValidator implements PreProcess {

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene gaiaScene = tileInfo.getScene();
        if (!validateGaiaScene(gaiaScene)) {
            throw new IllegalArgumentException("GaiaScene validation failed. Scene is null or empty.");
        }
        return tileInfo;
    }

    private boolean validateGaiaScene(GaiaScene gaiaScene) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        // Check if the GaiaScene is null or has no nodes
        if (gaiaScene == null || gaiaScene.getNodes() == null || gaiaScene.getNodes().isEmpty()) {
            String message = "GaiaScene is empty or null, skipping Z-Up transformation.";
            log.warn(message);
            return false;
        }
        return true;
    }
}
