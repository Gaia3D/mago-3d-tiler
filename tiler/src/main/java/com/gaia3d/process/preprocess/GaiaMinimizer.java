package com.gaia3d.process.preprocess;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;

@Slf4j
@AllArgsConstructor
public class GaiaMinimizer implements PreProcess {

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();
        GaiaPointCloud pointCloud = tileInfo.getPointCloud();

        minimizeGaiaScene(tileInfo, scene);
        minimizeGaiaPointCloud(tileInfo, pointCloud);

        if (scene != null) {
            GaiaSet tempSet = GaiaSet.fromGaiaScene(scene);
            Path tempPath = tempSet.writeFile(tileInfo.getTempPath(), tileInfo.getSerial(), tempSet.getAttribute());
            tileInfo.setTempPath(tempPath);
            tempSet.clear();
            tempSet = null;
            scene.clear();
            scene = null;
        }
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

    private void minimizeGaiaPointCloud(TileInfo tileInfo, GaiaPointCloud pointCloud) {
        if (pointCloud != null) {
            //Path tempPath = tempSet.writeFile(tileInfo.getTempPath(), tileInfo.getSerial(), tempSet.getAttribute());
            tileInfo.setTempPath(tileInfo.getOutputPath().resolve("temp"));

            GaiaAttribute attribute = pointCloud.getGaiaAttribute();
            String id = attribute.getIdentifier().toString();

            File tempFile = new File(tileInfo.getTempPath().toString(), id);
            pointCloud.minimize(tempFile);
        }
    }
}
