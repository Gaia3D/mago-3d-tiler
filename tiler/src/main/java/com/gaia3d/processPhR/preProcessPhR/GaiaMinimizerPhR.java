package com.gaia3d.processPhR.preProcessPhR;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.halfEdgeStructure.HalfEdgeScene;
import com.gaia3d.basic.halfEdgeStructure.HalfEdgeUtils;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.process.preprocess.PreProcess;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
@AllArgsConstructor
public class GaiaMinimizerPhR implements PreProcess {
    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();
        if (scene != null) {
            log.info("Welding vertices in GaiaScene");

            // 1rst, must weld vertices.***
            boolean checkTexCoord = true;
            boolean checkNormal = false;
            boolean checkColor = false;
            boolean checkBatchId = false;
            double error = 1e-8;
            scene.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);

            log.info("Making HalfEdgeScene from GaiaScene");
            //HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);

            GaiaSet tempSet = GaiaSet.fromGaiaScene(scene);
            Path tempPath = tempSet.writeFile(tileInfo.getTempPath(), tileInfo.getSerial(), tempSet.getAttribute());
            tileInfo.setTempPath(tempPath);
            if (tempSet != null) {
                tempSet.clear();
            }
            tempSet = null;
            if (scene != null) {
                scene.clear();
            }
            scene = null;
        }
        return tileInfo;
    }
}
