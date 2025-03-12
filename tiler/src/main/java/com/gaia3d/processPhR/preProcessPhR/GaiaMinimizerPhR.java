package com.gaia3d.processPhR.preProcessPhR;

import com.gaia3d.TilerExtensionModule;
import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.process.preprocess.PreProcess;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


@Slf4j
public class GaiaMinimizerPhR implements PreProcess {
    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();

        if (scene != null) {
            scene.deleteNormals();
            // 1rst, must weld vertices
            boolean checkTexCoord = true;
            boolean checkNormal = false;
            boolean checkColor = false;
            boolean checkBatchId = false;
            double error = 1e-4;
            log.info("[Pre][PhotoRealistic] Welding vertices in GaiaScene : {}", tileInfo.getTempPath());
            scene.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);
            scene.deleteDegeneratedFaces();

            // test render*************************************************************
            /*TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();
            List<GaiaScene> gaiaSceneList = new ArrayList<>();
            gaiaSceneList.add(scene);
            List<GaiaScene> resultScenes = new ArrayList<>();
            tilerExtensionModule.renderPyramidDeformation(gaiaSceneList, resultScenes);*/
            // End test.-----------------------------------------------------------------

            log.info("[Pre][PhotoRealistic] Minimize GaiaScene LOD 0 , Path : {}", tileInfo.getTempPath());

            GaiaSet tempSetLod0 = GaiaSet.fromGaiaScene(scene);
            Path tempPathLod0 = tempSetLod0.writeFile(tileInfo.getTempPath(), tileInfo.getSerial(), tempSetLod0.getAttribute());
            tileInfo.setTempPath(tempPathLod0);
            tempSetLod0.clear();
            tempSetLod0 = null;

            scene.clear();
            scene = null;
        }
        return tileInfo;
    }
}
