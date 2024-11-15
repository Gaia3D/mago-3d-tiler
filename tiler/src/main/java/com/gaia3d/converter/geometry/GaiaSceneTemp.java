package com.gaia3d.converter.geometry;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.structure.SceneStructure;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Getter
@Setter
public class GaiaSceneTemp extends SceneStructure implements Serializable {
    private String originalPath;
    private GaiaBoundingBox gaiaBoundingBox;
    private GaiaAttribute attribute;

    public static GaiaSceneTemp from(GaiaScene gaiaScene) {
        GaiaSceneTemp scene = new GaiaSceneTemp();
        scene.setOriginalPath(gaiaScene.getOriginalPath().toString());
        scene.setGaiaBoundingBox(gaiaScene.getGaiaBoundingBox());
        scene.setAttribute(gaiaScene.getAttribute());
        scene.setNodes(gaiaScene.getNodes());
        scene.setMaterials(gaiaScene.getMaterials());
        return scene;
    }

    public static GaiaScene to(GaiaSceneTemp gaiaSceneTemp) {
        GaiaScene scene = new GaiaScene();
        scene.setOriginalPath(Path.of(gaiaSceneTemp.getOriginalPath()));
        scene.setGaiaBoundingBox(gaiaSceneTemp.getGaiaBoundingBox());
        scene.setAttribute(gaiaSceneTemp.getAttribute());
        scene.setNodes(gaiaSceneTemp.getNodes());
        scene.setMaterials(gaiaSceneTemp.getMaterials());
        return scene;
    }
}
