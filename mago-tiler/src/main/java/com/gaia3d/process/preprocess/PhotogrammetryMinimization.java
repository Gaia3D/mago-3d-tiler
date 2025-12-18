package com.gaia3d.process.preprocess;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.modifier.topology.GaiaSceneCleaner;
import com.gaia3d.basic.geometry.modifier.topology.GaiaWelder;
import com.gaia3d.basic.geometry.modifier.topology.GaiaWeldOptions;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;


@Slf4j
public class PhotogrammetryMinimization implements PreProcess {
    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();

        if (scene != null) {
            scene.deleteNormals();
            // 1rst, must weld vertices
            double error = 1e-4;
            log.info("[Pre][Photogrammetry] Welding vertices in GaiaScene : {}", tileInfo.getTempPath());

            GaiaWeldOptions weldOptions = GaiaWeldOptions.builder()
                    .error(error)
                    .checkTexCoord(true)
                    .checkNormal(false)
                    .checkColor(false)
                    .checkBatchId(false)
                    .build();
            GaiaWelder weld = new GaiaWelder(weldOptions);
            weld.apply(scene);

            GaiaSceneCleaner cleaner = new GaiaSceneCleaner();
            cleaner.apply(scene);
            //scene.deleteDegeneratedFaces();

            log.info("[Pre][Photogrammetry] Minimize GaiaScene LOD 0 , Path : {}", tileInfo.getTempPath());

            GaiaSet tempSetLod0 = GaiaSet.fromGaiaScene(scene);
            Path tempPathLod0 = tempSetLod0.writeFile(tileInfo.getTempPath(), tileInfo.getSerial());
            tileInfo.setTempPath(tempPathLod0);
            tempSetLod0.clear();
            tempSetLod0 = null;

            scene.clear();
            scene = null;
        }
        return tileInfo;
    }
}