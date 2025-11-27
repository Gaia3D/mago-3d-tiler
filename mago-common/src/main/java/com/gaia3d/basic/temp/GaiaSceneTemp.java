package com.gaia3d.basic.temp;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.structure.SceneStructure;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.io.Serializable;
import java.nio.file.Path;

@Slf4j
@Getter
@Setter
public class GaiaSceneTemp extends SceneStructure implements Serializable {
    private String originalPath;
    private GaiaBoundingBox gaiaBoundingBox;
    private GaiaAttribute attribute;
    private Vector3d translation;

    public static GaiaSceneTemp from(GaiaScene gaiaScene) {
        GaiaSceneTemp scene = new GaiaSceneTemp();
        scene.setOriginalPath(gaiaScene.getOriginalPath().toString());
        scene.setGaiaBoundingBox(gaiaScene.getGaiaBoundingBox());
        scene.setAttribute(gaiaScene.getAttribute());
        scene.setNodes(gaiaScene.getNodes());
        scene.setMaterials(gaiaScene.getMaterials());
        scene.setTranslation(gaiaScene.getTranslation() != null ? new Vector3d(gaiaScene.getTranslation()) : new Vector3d(0.0, 0.0, 0.0));
        return scene;
    }

    public static GaiaScene to(GaiaSceneTemp gaiaSceneTemp) {
        GaiaScene scene = new GaiaScene();
        scene.setOriginalPath(Path.of(gaiaSceneTemp.getOriginalPath()));
        scene.setGaiaBoundingBox(gaiaSceneTemp.getGaiaBoundingBox());
        scene.setAttribute(gaiaSceneTemp.getAttribute());
        scene.setNodes(gaiaSceneTemp.getNodes());
        scene.setMaterials(gaiaSceneTemp.getMaterials());
        scene.setTranslation(gaiaSceneTemp.getTranslation() != null ? new Vector3d(gaiaSceneTemp.getTranslation()) : new Vector3d(0.0, 0.0, 0.0));
        return scene;
    }
}
