package com.gaia3d.process.preprocess;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

@Slf4j
@AllArgsConstructor
public class TileInfoGenerator implements PreProcess {
    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();
        GaiaNode rootNode = scene.getNodes()
                .get(0);
        tileInfo.setName(rootNode.getName());
        tileInfo.setTransformMatrix(rootNode.getTransformMatrix());

        GaiaBoundingBox boundingBox;
        if (scene.getGaiaBoundingBox() != null) {
            boundingBox = scene.getGaiaBoundingBox();
        } else {
            boundingBox = scene.updateBoundingBox();
        }

        TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
        if (tileTransformInfo == null) {
            tileTransformInfo = TileTransformInfo.builder()
                    .name("default")
                    .altitudeMode("absolute")
                    .position(new Vector3d(0.0d, 0.0d, 0.0d))
                    .heading(0.0d)
                    .tilt(0.0d)
                    .roll(0.0d)
                    .scaleX(1.0d)
                    .scaleY(1.0d)
                    .scaleZ(1.0d)
                    .build();
            tileInfo.setTileTransformInfo(tileTransformInfo);
        }

        tileInfo.setBoundingBox(boundingBox);
        tileInfo.setScenePath(tileInfo.getScene()
                .getOriginalPath());
        tileInfo.setTempPath(tileInfo.getOutputPath()
                .resolve("temp"));
        tileInfo.setTriangleCount(tileInfo.getScene()
                .calcTriangleCount());
        return tileInfo;
    }
}
