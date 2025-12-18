package com.gaia3d.process.preprocess;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.basic.types.LevelOfDetail;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
/**
 * Save only the essential information of the object as a file.
 */
public class GaiaMinimization implements PreProcess {
    private final List<LevelOfDetail> lodList = new ArrayList<>();

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();
        minimizeGaiaScene(tileInfo, scene);
        return tileInfo;
    }

    private void minimizeGaiaScene(TileInfo tileInfo, GaiaScene scene) {
        if (scene != null) {
            GaiaSet tempSet = GaiaSet.fromGaiaScene(scene);

            if (lodList.isEmpty()) {
                GlobalOptions globalOptions = GlobalOptions.getInstance();
                int minimumLod = globalOptions.getMinLod();
                int maximumLod = globalOptions.getMaxLod();
                for (int index = minimumLod; index <= maximumLod; index++) {
                    LevelOfDetail lod = LevelOfDetail.getByLevel(index);
                    lodList.add(lod);
                }
            }

            Path tempPath = tempSet.writeFileWithLod(tileInfo.getTempPath(), tileInfo.getSerial(), lodList);
            tileInfo.setTempPath(tempPath);
            tempSet.clear();
            tempSet = null;
            scene.clear();
            scene = null;
        }
    }
}
