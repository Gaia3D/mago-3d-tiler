package com.gaia3d;

import com.gaia3d.basic.exchangable.SceneInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.entities.GaiaAAPlane;
import com.gaia3d.basic.geometry.octree.HalfEdgeOctreeFaces;
import com.gaia3d.basic.geometry.voxel.VoxelGrid3D;
import com.gaia3d.basic.geometry.voxel.VoxelizeParameters;
import com.gaia3d.basic.halfedge.DecimateParameters;
import com.gaia3d.basic.halfedge.HalfEdgeScene;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.remesher.ReMeshParameters;
import com.gaia3d.renderer.MainRenderer;
import com.gaia3d.renderer.MainRendererBillBoard;
import com.gaia3d.renderer.MainVoxelizer;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

@Slf4j
public class TilerExtensionModule implements ExtensionModuleFrame {
    MainRenderer renderer;
    MainVoxelizer voxelizer;
    MainRendererBillBoard rendererBillboard;

    @Override
    public String getName() {
        return "Extension Project";
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public void executePhotogrammetry(GaiaScene gaiaScene, Map<String, Object> options) {
        log.info("+ Extension has been applied.");
        log.info("----------------------------------------");
    }

    @Override
    public void getColorAndDepthRender(List<SceneInfo> sceneInfos, int bufferedImageType, List<BufferedImage> resultImages, GaiaBoundingBox nodeBBox, Matrix4d nodeTMatrix, int maxScreenSize, int maxDepthScreenSize) {
        if (renderer == null) renderer = new MainRenderer();
        renderer.getColorAndDepthRender(sceneInfos, bufferedImageType, resultImages, nodeBBox, nodeTMatrix, maxScreenSize, maxDepthScreenSize);
        deleteObjects();
    }

    @Override
    public void decimateAndCutByObliqueCamera(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters, HalfEdgeOctreeFaces octree, List<GaiaAAPlane> cuttingPlanes, double screenPixelsForMeter, boolean makeHorizontalSkirt) {
        if (renderer == null) renderer = new MainRenderer();
        renderer.decimateAndCutByObliqueCamera(scenes, resultHalfEdgeScenes, decimateParameters, octree, cuttingPlanes, screenPixelsForMeter, makeHorizontalSkirt);
        deleteObjects();
    }

    @Override
    public void decimateNetSurfaceAndCutByObliqueCamera(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters, HalfEdgeOctreeFaces octree, List<GaiaAAPlane> cuttingPlanes, double depthTexPixelsForMeter, double screenPixelsForMeter, boolean makeHorizontalSkirt) {
        if (renderer == null) renderer = new MainRenderer();
        renderer.decimateNetSurfaceAndCutByObliqueCamera(scenes, resultHalfEdgeScenes, decimateParameters, octree, cuttingPlanes, depthTexPixelsForMeter, screenPixelsForMeter, makeHorizontalSkirt);
        deleteObjects();
    }

    @Override
    public void reMeshAndCutByObliqueCamera(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, ReMeshParameters reMeshParams, HalfEdgeOctreeFaces octree,
                                            List<GaiaAAPlane> cuttingPlanes, double depthTexPixelsForMeter, double screenPixelsForMeter, boolean makeHorizontalSkirt) {
        if (voxelizer == null) voxelizer = new MainVoxelizer();
        voxelizer.reMeshAndCutByObliqueCamera(scenes, resultHalfEdgeScenes, reMeshParams, octree, cuttingPlanes, depthTexPixelsForMeter, screenPixelsForMeter,
                makeHorizontalSkirt);
        deleteObjects();
    }

    @Override
    public void voxelize(List<GaiaScene> scenes, List<VoxelGrid3D> resultVoxelGrids, List<GaiaScene> resultGaiaScenes, VoxelizeParameters voxelizeParameters) {
        if (voxelizer == null) voxelizer = new MainVoxelizer();
        voxelizer.voxelize(scenes, resultVoxelGrids, resultGaiaScenes, voxelizeParameters);
        deleteObjects();
    }

    @Override
    public void makeBillBoard(List<GaiaScene> scenes, List<GaiaScene> resultScenes) {
        if (rendererBillboard == null) rendererBillboard = new MainRendererBillBoard();
        rendererBillboard.makeBillBoard(scenes, resultScenes);
        deleteObjects();
    }

    @Override
    public void deleteObjects() {
        if (renderer != null) {
            renderer.deleteObjects();
            renderer = null;
        }

        if (voxelizer != null) {
            voxelizer.deleteObjects();
            voxelizer = null;
        }
    }
}
