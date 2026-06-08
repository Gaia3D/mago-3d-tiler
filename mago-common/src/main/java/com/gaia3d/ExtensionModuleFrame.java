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

    void integralReMeshByObliqueCameraV2(List<SceneInfo> sceneInfos, List<HalfEdgeScene> resultHalfEdgeScenes, ReMeshParameters reMeshParams, GaiaBoundingBox nodeBBox,
                                         Matrix4d nodeTMatrix, int maxScreenSize, String outputPathString, String nodeName, int lod);

    void integralDecimateByObliqueCamera(List<SceneInfo> sceneInfos, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters, ReMeshParameters reMeshParams,
                                         GaiaBoundingBox nodeBBox, Matrix4d nodeTMatrix, int maxScreenSize, String outputPathString, String nodeName, int lod);

    void integralLeafScene(List<SceneInfo> sceneInfos,
                           List<GaiaScene> resultGaiaScenes,
                                  GaiaBoundingBox nodeBBox,
                                  Matrix4d nodeTMatrix,
                                  int maxScreenSize,
                                  String outputPathString,
                                  String nodeName,
                                  int lod);

    void makeBillBoard(List<GaiaScene> scenes, List<GaiaScene> resultScenes, int verticalPlanesCount, int horizontalPlanesCount);


    void deleteObjects();
}
