package com.gaia3d;

import com.gaia3d.basic.exchangable.SceneInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.halfedge.HalfEdgeScene;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.renderer.MainRenderer;
import com.gaia3d.basic.halfedge.DecimateParameters;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

@Slf4j
public class TilerExtensionModule implements ExtensionModuleFrame {
    MainRenderer renderer;

    @Override
    public String getName() {
        return "Extension Project";
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public GaiaScene executePhotorealistic(GaiaScene gaiaScene, Map<String, Object> options) {
        // TODO: Implement this method
        log.info("----------------------------------------");
        log.info("Extension has been applied.");
        log.info("----------------------------------------");


        return null;
    }
    @Override
    public void getColorAndDepthRender(List<SceneInfo> sceneInfos, int bufferedImageType, List<BufferedImage> resultImages, GaiaBoundingBox nodeBBox,
                                       Matrix4d nodeTMatrix, int maxScreenSize, int maxDepthScreenSize)
    {
        if(renderer == null)
            renderer = new MainRenderer();

        renderer.getColorAndDepthRender(sceneInfos, bufferedImageType, resultImages, nodeBBox, nodeTMatrix, maxScreenSize, maxDepthScreenSize);
        deleteObjects();
    }

    @Override
    public void getRenderScene(List<GaiaScene> scenes, int bufferedImageType, int maxScreenSize, List<BufferedImage> resultImages)
    {
        if(renderer == null)
            renderer = new MainRenderer();
        renderer.render(scenes, bufferedImageType, resultImages, maxScreenSize);
        deleteObjects();
    }

    @Override
    public void renderDecimate(List<GaiaScene> scenes, List<GaiaScene> resultScenes)
    {
        //***************************************************************
        // test function to check if the decimate function is working.***
        //***************************************************************
        if(renderer == null)
            renderer = new MainRenderer();
        renderer.renderDecimate(scenes, resultScenes);
        deleteObjects();
    }

    @Override
    public void decimate(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters)
    {
        if(renderer == null)
            renderer = new MainRenderer();
        renderer.decimate(scenes, resultHalfEdgeScenes, decimateParameters);
        deleteObjects();
    }

    @Override
    public void makeNetSurfacesByPyramidDeformationRender(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters, double pixelsForMeter)
    {
        if(renderer == null)
            renderer = new MainRenderer();
        renderer.makeNetSurfacesByPyramidDeformationRender(scenes, resultHalfEdgeScenes, decimateParameters, pixelsForMeter);
        deleteObjects();
    }

    @Override
    public void makeNetSurfaces(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters, double pixelsForMeter)
    {
        if(renderer == null)
            renderer = new MainRenderer();
        renderer.makeNetSurfaces(scenes, resultHalfEdgeScenes, decimateParameters, pixelsForMeter);
        deleteObjects();
    }

    @Override
    public void deleteObjects()
    {
        if(renderer != null) {
            renderer.deleteObjects();
            renderer = null;
        }
    }
}
