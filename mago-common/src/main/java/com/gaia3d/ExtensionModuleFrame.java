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
import org.joml.Matrix4d;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

public interface ExtensionModuleFrame {
    String getName();

    boolean isSupported();

    void executePhotogrammetry(GaiaScene gaiaScene, Map<String, Object> options);

    void getColorAndDepthRender(List<SceneInfo> sceneInfos, int bufferedImageType, List<BufferedImage> resultImages, GaiaBoundingBox nodeBBox, Matrix4d nodeTMatrix, int maxScreenSize, int maxDepthScreenSize);

    void decimateAndCutByObliqueCamera(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters, HalfEdgeOctreeFaces octree, List<GaiaAAPlane> cuttingPlanes, double screenPixelsForMeter, boolean makeHorizontalSkirt);

    void decimateNetSurfaceAndCutByObliqueCamera(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters, HalfEdgeOctreeFaces octree, List<GaiaAAPlane> cuttingPlanes, double depthTexPixelsForMeter, double screenPixelsForMeter, boolean makeHorizontalSkirt);

    void reMeshAndCutByObliqueCamera(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, ReMeshParameters reMeshParams, HalfEdgeOctreeFaces octree,
                                     List<GaiaAAPlane> cuttingPlanes, double depthTexPixelsForMeter, double screenPixelsForMeter, boolean makeHorizontalSkirt);

    void integralReMeshByObliqueCameraV2(List<SceneInfo> sceneInfos, List<HalfEdgeScene> resultHalfEdgeScenes, ReMeshParameters reMeshParams, GaiaBoundingBox nodeBBox,
                                         Matrix4d nodeTMatrix, int maxScreenSize, String outputPathString, String nodeName, int lod);

    void voxelize(List<GaiaScene> scenes, List<VoxelGrid3D> resultVoxelGrids, List<GaiaScene> resultGaiaScenes, VoxelizeParameters voxelizeParameters);

    void makeBillBoard(List<GaiaScene> scenes, List<GaiaScene> resultScenes, int verticalPlanesCount, int horizontalPlanesCount);


    void deleteObjects();
}
