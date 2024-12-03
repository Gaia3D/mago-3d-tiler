package com.gaia3d;

import com.gaia3d.basic.exchangable.SceneInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.halfedge.DecimateParameters;
import com.gaia3d.basic.halfedge.HalfEdgeScene;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.GaiaTexture;
import org.joml.Matrix4d;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

public interface ExtensionModuleFrame {
    String getName();
    boolean isSupported();
    GaiaScene executePhotorealistic(GaiaScene gaiaScene, Map<String, Object> options);
    void getColorAndDepthRender(List<SceneInfo> sceneInfos, int bufferedImageType, List<BufferedImage> resultImages, GaiaBoundingBox nodeBBox, Matrix4d nodeTMatrix, int maxScreenSize, int maxDepthScreenSize);
    void getRenderScene(List<GaiaScene> scene, int bufferedImageType, int maxScreenSize, List<BufferedImage> resultImages);
    void renderDecimate(List<GaiaScene> scenes, List<GaiaScene> resultScenes);
    void decimate(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters);

    void makeNetSurfaces(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters, int maxDepthScreenSize);

    void deleteObjects();
}
