package com.gaia3d.process.preprocess;

import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.basic.geometry.modifier.transform.UpAxisTransformer;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

        if (rotateX != 0.0) {
            UpAxisTransformer.rotateDegreeX(scene, rotateX);
        }

        tileInfo.updateSceneInfo();
        return tileInfo;
    }
}
