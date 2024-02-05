package com.gaia3d.process.preprocess;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.structure.GaiaNode;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
@AllArgsConstructor
public class GaiaMinimizer implements PreProcess {

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GlobalOptions options = GlobalOptions.getInstance();
        GaiaScene scene = tileInfo.getScene();
        if (scene != null) {
            GaiaSet tempSet = new GaiaSet(scene);
            tileInfo.setTempPath(tempSet.writeFile(tileInfo.getTempPath(), tileInfo.getSerial()));
            tempSet.clear();
            tempSet = null;
            scene.clear();
            scene = null;
        }
        return tileInfo;
    }
}
