package com.gaia3d.process.preprocess;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.process.preprocess.sub.TransformBaker;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

@Slf4j
@AllArgsConstructor
public class GaiaTransformBaker implements PreProcess {

    private final TransformBaker transformBaker = new TransformBaker();

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();
        transformBaker.bake(scene);
        tileInfo.updateSceneInfo();
        //GaiaBoundingBox updatedBoundingBox = scene.updateBoundingBox();
        //tileInfo.setTransformMatrix(new Matrix4d().identity());
        //tileInfo.setBoundingBox(updatedBoundingBox);
        return tileInfo;
    }
}
