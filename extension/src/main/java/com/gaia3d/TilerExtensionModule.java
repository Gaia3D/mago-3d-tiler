package com.gaia3d;

import com.gaia3d.basic.exchangable.SceneInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.entities.GaiaAAPlane;
import com.gaia3d.basic.geometry.octree.HalfEdgeOctree;
import com.gaia3d.basic.halfedge.DecimateParameters;
import com.gaia3d.basic.halfedge.HalfEdgeScene;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.renderer.MainRenderer;
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
        log.info("+ Extension has been applied.");
        log.info("----------------------------------------");
        return null;
    }

    @Override
    public void getColorAndDepthRender(List<SceneInfo> sceneInfos, int bufferedImageType, List<BufferedImage> resultImages, GaiaBoundingBox nodeBBox, Matrix4d nodeTMatrix, int maxScreenSize, int maxDepthScreenSize) {
        if (renderer == null) renderer = new MainRenderer();

        renderer.getColorAndDepthRender(sceneInfos, bufferedImageType, resultImages, nodeBBox, nodeTMatrix, maxScreenSize, maxDepthScreenSize);
        deleteObjects();
    }

    @Override
    public void decimateAndCutByObliqueCamera(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters,
                                              HalfEdgeOctree octree, List<GaiaAAPlane> cuttingPlanes, double screenPixelsForMeter, boolean makeSkirt) {
        if (renderer == null) renderer = new MainRenderer();
        renderer.decimateAndCutByObliqueCamera(scenes, resultHalfEdgeScenes, decimateParameters, octree, cuttingPlanes, screenPixelsForMeter, makeSkirt);
        deleteObjects();
    }

    @Override
    public void decimateNetSurfaceAndCutByObliqueCamera(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters,
                                                        HalfEdgeOctree octree, List<GaiaAAPlane> cuttingPlanes, double depthTexPixelsForMeter, double screenPixelsForMeter, boolean makeSkirt) {
        if (renderer == null) renderer = new MainRenderer();
        renderer.decimateNetSurfaceAndCutByObliqueCamera(scenes, resultHalfEdgeScenes, decimateParameters, octree, cuttingPlanes, depthTexPixelsForMeter, screenPixelsForMeter, makeSkirt);
        deleteObjects();
    }

    @Override
    public void makeNetSurfacesWithBoxTexturesObliqueCamera(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters, double depthTexPixelsForMeter, double screenPixelsForMeter) {
        if (renderer == null) renderer = new MainRenderer();
        renderer.makeNetSurfacesWithBoxTexturesObliqueCamera(scenes, resultHalfEdgeScenes, decimateParameters, depthTexPixelsForMeter, screenPixelsForMeter);
        deleteObjects();
    }

    @Override
    public void deleteObjects() {
        if (renderer != null) {
            renderer.deleteObjects();
            renderer = null;
        }
    }
}
