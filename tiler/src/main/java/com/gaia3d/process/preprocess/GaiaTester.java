package com.gaia3d.process.preprocess;

import com.gaia3d.basic.structure.GaiaNode;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import java.io.File;

@Slf4j
@AllArgsConstructor
public class GaiaTester implements PreProcess {

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();
        scene.getBoundingBox();
        GaiaNode rootNode = scene.getNodes().get(0);

        tileInfo.setTransformMatrix(rootNode.getTransformMatrix());
        tileInfo.setName(rootNode.getName());
        tileInfo.setBoundingBox(scene.getGaiaBoundingBox());
        tileInfo.setScenePath(tileInfo.getScene().getOriginalPath());

        tileInfo.setTempPath(tileInfo.getOutputPath().resolve("temp"));
        File tempFile = tileInfo.getTempPath().toFile();
        if (!tempFile.exists() && tempFile.mkdir()) {
            log.info("[Pre] Created temp directory in {}", tileInfo.getTempPath());
        }
        return tileInfo;
    }
}
