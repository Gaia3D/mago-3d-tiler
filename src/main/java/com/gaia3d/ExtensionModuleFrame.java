package com.gaia3d;

import com.gaia3d.basic.exchangable.SceneInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.entities.GaiaAAPlane;
import com.gaia3d.basic.geometry.octree.HalfEdgeOctree;
import com.gaia3d.basic.geometry.voxel.VoxelGrid3D;
import com.gaia3d.basic.geometry.voxel.VoxelizeParameters;
import com.gaia3d.basic.halfedge.DecimateParameters;
import com.gaia3d.basic.halfedge.HalfEdgeScene;
import com.gaia3d.basic.model.GaiaScene;
import org.joml.Matrix4d;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

public interface ExtensionModuleFrame {
    String getName();

    boolean isSupported();

    GaiaScene executePhotorealistic(GaiaScene gaiaScene, Map<String, Object> options);

    void getColorAndDepthRender(List<SceneInfo> sceneInfos, int bufferedImageType, List<BufferedImage> resultImages, GaiaBoundingBox nodeBBox, Matrix4d nodeTMatrix, int maxScreenSize, int maxDepthScreenSize);

    void makeNetSurfacesWithBoxTexturesObliqueCamera(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters, double depthTexPixelsForMeter, double screenPixelsForMeter);

    void decimateAndCutByObliqueCamera(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters,
                                       HalfEdgeOctree octree, List<GaiaAAPlane> cuttingPlanes, double screenPixelsForMeter, boolean makeHorizontalSkirt);

    void decimateNetSurfaceAndCutByObliqueCamera(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters,
                                                 HalfEdgeOctree octree, List<GaiaAAPlane> cuttingPlanes, double depthTexPixelsForMeter, double screenPixelsForMeter, boolean makeHorizontalSkirt);

    void voxelize(List<GaiaScene> scenes, List<VoxelGrid3D> resultVoxelGrids, List<GaiaScene> resultGaiaScenes, VoxelizeParameters voxelizeParameters);

    void deleteObjects();
}
