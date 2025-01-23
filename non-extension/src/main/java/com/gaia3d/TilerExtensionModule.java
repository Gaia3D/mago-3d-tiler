package com.gaia3d;

import com.gaia3d.basic.exchangable.SceneInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.halfedge.DecimateParameters;
import com.gaia3d.basic.halfedge.HalfEdgeScene;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.GaiaTexture;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

@Slf4j
public class TilerExtensionModule implements ExtensionModuleFrame {

    @Override
    public String getName() {
        return "Non-Extension Project";
    }

    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public GaiaScene executePhotorealistic(GaiaScene gaiaScene, Map<String, Object> options) {
        log.debug("----------------------------------------");
        log.debug("Cannot execute photorealistic extension module.");
        log.debug("This module is not implemented.");
        log.debug("----------------------------------------");
        return null;
    }

    @Override
    public void getColorAndDepthRender(List<SceneInfo> sceneInfos, int bufferedImageType, List<BufferedImage> resultImages, GaiaBoundingBox nodeBBox, Matrix4d nodeTMatrix, int maxScreenSize, int maxDepthScreenSize) {

    }

    @Override
    public void getRenderScene(List<GaiaScene> scene, int bufferedImageType, int maxScreenSize, List<BufferedImage> resultImages) {
    }

    @Override
    public void renderDecimate(List<GaiaScene> scenes, List<GaiaScene> resultScenes) {
    }

    @Override
    public void decimate(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters) {
    }

    @Override
    public void decimateByObliqueCamera(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters){
    }

    @Override
    public void renderPyramidDeformation(List<GaiaScene> scenes, List<GaiaScene> resultScenes){
    }

    @Override
    public void makeNetSurfacesByPyramidDeformationRender(List<SceneInfo> sceneInfos, int bufferedImageType, List<HalfEdgeScene> resultHalfEdgeScenes, List<BufferedImage> resultImages,
                                                          GaiaBoundingBox nodeBBox, Matrix4d nodeTMatrix, int maxScreenSize, int maxDepthScreenSize) {

    }

    @Override
    public void makeNetSurfacesWithBoxTextures(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters, double pixelsForMeter){

    }

    @Override
    public void deleteObjects() {
    }
}
