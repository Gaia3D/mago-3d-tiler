package com.gaia3d.process.preprocess;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.pointcloud.GaiaPointCloudOld;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;

@Slf4j
@AllArgsConstructor
/**
 * Save only the essential information of the object as a file.
 */
public class GaiaMinimization implements PreProcess {

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();
        minimizeGaiaScene(tileInfo, scene);
        return tileInfo;
    }

    private void minimizeGaiaScene(TileInfo tileInfo, GaiaScene scene) {
        if (scene != null) {
            GaiaSet tempSet = GaiaSet.fromGaiaScene(scene);
            Path tempPath = tempSet.writeFile(tileInfo.getTempPath(), tileInfo.getSerial(), tempSet.getAttribute());
            tileInfo.setTempPath(tempPath);
            tempSet.clear();
            tempSet = null;
            scene.clear();
            scene = null;
        }
    }
}
