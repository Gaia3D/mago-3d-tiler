package com.gaia3d.render;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.exchangable.SceneInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.modifier.halfedge.HalfEdgeDecimator;
import com.gaia3d.basic.geometry.modifier.topology.*;
import com.gaia3d.basic.geometry.modifier.transform.GaiaBaker;
import com.gaia3d.basic.geometry.octree.OctreeBBoxInfo;
import com.gaia3d.basic.halfedge.*;
import com.gaia3d.basic.magogl.*;
import com.gaia3d.basic.magogl.backend.MagoRenderingBackend;
import com.gaia3d.basic.magogl.backend.MagoRenderingSession;
import com.gaia3d.basic.magogl.backend.SoftwareRenderingBackend;
import com.gaia3d.basic.magogl.maker.MagoRenderableMaker;
import com.gaia3d.basic.magogl.renderable.MagoRenderableScene;
import com.gaia3d.basic.magogl.shader.program.MagoShaderProgram;
import com.gaia3d.basic.magogl.shader.resources.MagoDefaultVertexShader;
import com.gaia3d.basic.magogl.shader.resources.MagoFaceCodeFragmentShader;
import com.gaia3d.basic.magogl.shader.resources.MagoTexturedFragmentShader;
import com.gaia3d.basic.magogl.texture.MagoTextureFilter;
import com.gaia3d.basic.magogl.texture.MagoTextureWrap;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.remesher.*;
import com.gaia3d.basic.remesher.information.GaiaStatistics;
import com.gaia3d.basic.texture.atlas.TextureAtlasManager;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.util.GaiaTextureUtils;
import com.gaia3d.util.ImageResizer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.Math;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import static com.gaia3d.basic.magogl.MagoRenderEngine.toArgb;

@Slf4j
@Getter
@Setter

public class MagoReTextureByObliqueCamera {

    public static final int BACKGROUND_FACE_CODE =
            0xFFFFFFFF;

    private final MagoRenderingBackend renderingBackend;
    private CameraDirectionType[] renderDirections = {
            CameraDirectionType.ZNEG,
            CameraDirectionType.XPOS_ZNEG,
            CameraDirectionType.XNEG_ZNEG,
            CameraDirectionType.YPOS_ZNEG,
            CameraDirectionType.YNEG_ZNEG,
            CameraDirectionType.XPOS_YPOS_ZNEG,
            CameraDirectionType.XNEG_YPOS_ZNEG,
            CameraDirectionType.XPOS_YNEG_ZNEG,
            CameraDirectionType.XNEG_YNEG_ZNEG
    };
    private FaceColorCodeManager faceColorCodeManager = new FaceColorCodeManager();


    public MagoReTextureByObliqueCamera() {
        this(new SoftwareRenderingBackend());
    }

    public MagoReTextureByObliqueCamera(MagoRenderingBackend renderingBackend) {
        this.renderingBackend = Objects.requireNonNull(
                renderingBackend,
                "renderingBackend must not be null"
        );

    }

    public static void faceCodeToColor(
            int faceCode,
            Vector4f result
    ) {
        float inverse255 = 1.0f / 255.0f;

        result.set(
                ((faceCode >>> 16) & 0xFF) * inverse255,
                ((faceCode >>> 8) & 0xFF) * inverse255,
                (faceCode & 0xFF) * inverse255,
                ((faceCode >>> 24) & 0xFF) * inverse255
        );

    }

    public void integralReMeshByObliqueCameraV2(List<SceneInfo> sceneInfos,
                                                List<HalfEdgeScene> resultHalfEdgeScenes,
                                                ReMeshParameters reMeshParams,
                                                GaiaBoundingBox nodeBBox,
                                                Matrix4d nodeTMatrix,
                                                int maxScreenSize,
                                                String outputPathString,
                                                String nodeName,
                                                int lod) {
        // Note: There are only one scene in the scene list
        Map<CameraDirectionType, GaiaBoundingBox> mapCameraDirectionTypeBBox = new HashMap<>();
        Map<CameraDirectionType, Matrix4d> mapCameraDirectionTypeModelViewMatrix = new HashMap<>();
        Map<CameraDirectionType, Projection> mapCameraDirectionTypeProjection = new HashMap<>();

        double screenPixelsForMeter = 20;
        double nodeBBoxMaxSize = nodeBBox.getMaxSize();

        if (!Double.isFinite(nodeBBoxMaxSize)
                || nodeBBoxMaxSize <= 0.0) {

            throw new IllegalStateException(
                    "Invalid node bounding-box size: "
                            + nodeBBoxMaxSize
            );
        }

        int targetMaxSize = 512;
        screenPixelsForMeter = targetMaxSize / nodeBBoxMaxSize;
        MagoFboSet fboSet = create9MagoFbos(nodeBBox,
                renderDirections,
                mapCameraDirectionTypeBBox,
                mapCameraDirectionTypeModelViewMatrix,
                mapCameraDirectionTypeProjection,
                screenPixelsForMeter);

        Vector4f backgroundColor =
                new Vector4f(
                        1.0f,
                        0.0f,
                        1.0f,
                        1.0f
                );

        int backgroundArgb =
                toArgb(backgroundColor);

        fboSet.clearAll(
                backgroundArgb,
                1.0f
        ); // only one time

        // create faceCodeFboSet by copy from fboSet.
        MagoFboSet faceCodeFboSet =
                fboSet.createCompatible(
                        BACKGROUND_FACE_CODE,
                        1.0f
                );

        Matrix4d nodeMatrixInv = new Matrix4d(nodeTMatrix);
        nodeMatrixInv.invert();

        Map<Vector3i, List<GaiaVertex>> vertexClusters = new HashMap<>();
        GaiaScene gaiaSceneMaster = null;
        double weldError = 1e-5; // 1e-6 is a good value for remeshing

        // render the scenes
        int scenesCount = sceneInfos.size();
        int counter = 0;
        int faceIdAvailable = 0;

        Map<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> mapClassificationCamDirTypeFacesList = new HashMap<>();

        Vector3i nodeMinCellIndex = new Vector3i(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        Vector3i nodeMaxCellIndex = new Vector3i(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);

        GaiaWeldOptions weldOptions = GaiaWeldOptions.builder()
                .error(weldError)
                .checkTexCoord(false)
                .checkNormal(false)
                .checkColor(false)
                .checkBatchId(false)
                .build();

        GaiaTriangulator triangulator = new GaiaTriangulator();
        GaiaSceneCleaner cleaner = new GaiaSceneCleaner();
        GaiaWelder weld = new GaiaWelder(weldOptions);

        MagoRenderableMaker magoRenderableMaker = new MagoRenderableMaker();

        // remesh each mesh
        MagoRenderingSession renderingSession = renderingBackend.openSession();
        try {
            for (int i = 0; i < scenesCount; i++) {
                // load and render, one by one
                SceneInfo sceneInfo = sceneInfos.get(i);
                String scenePath = sceneInfo.getScenePath();
                Matrix4d sceneTMat = sceneInfo.getTransformMatrix();

                // must find the local position of the scene rel to node
                Vector3d scenePosWC = new Vector3d(sceneTMat.m30(), sceneTMat.m31(), sceneTMat.m32());
                Vector3d scenePosLC = nodeMatrixInv.transformPosition(scenePosWC, new Vector3d());

                // calculate the local sceneTMat
                Matrix4d sceneTMatLC = new Matrix4d();
                sceneTMatLC.identity();
                sceneTMatLC.m30(scenePosLC.x);
                sceneTMatLC.m31(scenePosLC.y);
                sceneTMatLC.m32(scenePosLC.z);

                // load the set file
                GaiaSet gaiaSet = null;
                GaiaScene gaiaScene = null;
                GaiaScene gaiaSceneToRender = null;
                MagoRenderableScene magoRenderableScene = null;
                Path path = Paths.get(scenePath);
                try {
                    gaiaSet = GaiaSet.readFile(path);
                    gaiaScene = new GaiaScene(gaiaSet);
                    gaiaSceneToRender = new GaiaScene(gaiaSet);
                    triangulator.apply(gaiaSceneToRender);

                    GaiaNode gaiaNode = gaiaSceneToRender.getNodes().get(0);
                    gaiaNode.setTransformMatrix(new Matrix4d(sceneTMatLC));
                    gaiaNode.setPreMultipliedTransformMatrix(new Matrix4d(sceneTMatLC));
                    magoRenderableScene = magoRenderableMaker.makeScene(gaiaSceneToRender);
                } catch (Exception e) {
                    log.error("[ERROR] reading the file: ", e);
                }

                if (gaiaScene == null) {
                    // throw error
                    throw new RuntimeException("[ERROR] integralReMeshByObliqueCamera : GaiaScene is null");
                }

                // reMesh the scene.****************************************************************************************
                // The "scenePositionRelToCellGrid" is the relative position of the scene respect the center of RootNode (Depth = 0). All scenes must be synchronized to the RootNode.
                Vector3d scenePositionRelToCellGrid = sceneInfo.getScenePosLC(); // relative position of the scene respect the center of RootNode (Depth = 0).
                Vector3d scenePosRelToCellGridNegative = new Vector3d(-scenePositionRelToCellGrid.x, -scenePositionRelToCellGrid.y, -scenePositionRelToCellGrid.z);
                triangulator.apply(gaiaScene);
                GaiaBaker baker = new GaiaBaker();
                baker.apply(gaiaScene);
                gaiaScene.joinAllSurfaces();

                GaiaStatistics stats = GaiaStatistics.calculateStatistics(gaiaScene);

                // Pre-ReMesh.******************************************************************************************
                GeometryOnlyReMesherByOctree preReMesher = new GeometryOnlyReMesherByOctree();
                GaiaBoundingBox effectiveNodeBBox = nodeBBox.clone();

                double desiredLeafSize = 1.5;
                OctreeBBoxInfo octreeBoxInfo = preReMesher.calculateBoundingBoxForLeafDistInfo(nodeBBox, desiredLeafSize);
                int octreeMaxDepth = octreeBoxInfo.maxDepth;
                double rootOctreeSize = octreeBoxInfo.rootCubeSize;
                double nodeSize = nodeBBox.getMaxSize();
                double scaleFactor = rootOctreeSize / nodeSize;
                effectiveNodeBBox.expand(nodeSize * scaleFactor * 0.5);
                preReMesher.setReMeshAnyWay(true);
                preReMesher.setLimitDepth(octreeMaxDepth);
                preReMesher.setMinFacesCount(1);
                preReMesher.setLimitBoxSize(desiredLeafSize);
                preReMesher.reMeshScene(gaiaScene, stats, effectiveNodeBBox);
                weld.apply(gaiaScene);
                cleaner.apply(gaiaScene);
                // End pre-ReMesh.--------------------------------------------------------------------------------------

                weld.apply(gaiaScene);
                cleaner.apply(gaiaScene);
                List<GaiaMaterial> materials = gaiaScene.getMaterials();

                // Here decimate the scene.*******************************************************************************************************
                DecimateParameters decimateParameters = new DecimateParameters();
                //decimateParameters.setBasicValues(14.0, 0.01, 0.9, 40.0, 1000000, 5, 1.0);
                decimateParameters.setBasicValues(12.0, 0.001, 0.9, 40.0, 1000000, 5, 0.1);
                HalfEdgeScene halfEdgeSceneToDecimate = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(gaiaScene);
                HalfEdgeDecimator decimator = new HalfEdgeDecimator(decimateParameters);
                decimator.apply(halfEdgeSceneToDecimate);

                // now, try to reMesh vegetation.
                gaiaScene = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeSceneToDecimate);

                // delete materials.
                for (GaiaMaterial material : materials) {
                    material.clear();
                }
                gaiaScene.getMaterials().clear();

                Vector3i sceneMinCellIndex = new Vector3i();
                Vector3i sceneMaxCellIndex = new Vector3i();
                translateScene(gaiaScene, scenePositionRelToCellGrid); // translate the scene to the cell grid position
                // new.*******************************************************

                ReMesherVertexClusterV2.reMeshScene(
                        gaiaScene,
                        reMeshParams,
                        sceneMinCellIndex,
                        sceneMaxCellIndex
                );
                // end new.-------------------------------------------------------------------------------
                translateScene(gaiaScene, scenePosRelToCellGridNegative); // translate the scene back to the original position

                // update the node cell index bbox
                if (sceneMinCellIndex.x < nodeMinCellIndex.x) {
                    nodeMinCellIndex.x = sceneMinCellIndex.x;
                }
                if (sceneMinCellIndex.y < nodeMinCellIndex.y) {
                    nodeMinCellIndex.y = sceneMinCellIndex.y;
                }
                if (sceneMinCellIndex.z < nodeMinCellIndex.z) {
                    nodeMinCellIndex.z = sceneMinCellIndex.z;
                }
                if (sceneMaxCellIndex.x > nodeMaxCellIndex.x) {
                    nodeMaxCellIndex.x = sceneMaxCellIndex.x;
                }
                if (sceneMaxCellIndex.y > nodeMaxCellIndex.y) {
                    nodeMaxCellIndex.y = sceneMaxCellIndex.y;
                }
                if (sceneMaxCellIndex.z > nodeMaxCellIndex.z) {
                    nodeMaxCellIndex.z = sceneMaxCellIndex.z;
                }
                // end of reMeshing the scene.******************************************************************************

                // now must translate to the relative position in the node
                GaiaNode gaiaNode = gaiaScene.getNodes().get(0);
                gaiaNode.setTransformMatrix(new Matrix4d(sceneTMatLC));
                gaiaNode.setPreMultipliedTransformMatrix(new Matrix4d(sceneTMatLC));
                baker.apply(gaiaScene);
                gaiaScene.joinAllSurfaces();
                weld.apply(gaiaScene);
                cleaner.apply(gaiaScene);

                // Test 2nd decimating.*****************************************************************************************
                DecimateParameters decimateParameters2ndTest = new DecimateParameters();
                decimateParameters.setBasicValues(12.0, 0.001, 0.0, 40.0, 1000000, 2, 0.1);
                HalfEdgeScene halfEdgeSceneToDecimate2ndTest = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(gaiaScene);
                HalfEdgeDecimator decimator2ndTest = new HalfEdgeDecimator(decimateParameters2ndTest);
                decimator2ndTest.apply(halfEdgeSceneToDecimate2ndTest);
                gaiaScene.clear();
                gaiaScene = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeSceneToDecimate2ndTest);
                halfEdgeSceneToDecimate2ndTest.deleteObjects();
                // End test 2nd decimating.-------------------------------------------------------------------------------------

                try {
                    // render the scene
                    log.debug("Rendering the scene : " + i + " / " + scenesCount + ". LOD : " + lod);

                    // for each gaiaScene, set the available faceIds, to use for colorCoded rendering
                    GaiaExtractor extractor = new GaiaExtractor();
                    List<GaiaFace> gaiaFaces = extractor.extractAllFaces(gaiaScene);
                    for (GaiaFace gaiaFace : gaiaFaces) {
                        gaiaFace.setId(faceIdAvailable);
                        faceIdAvailable++;
                    }

                    MagoRenderableScene decimatedRenderableScene = magoRenderableMaker.makeScene(gaiaScene);
                    makeIntegralBoxTexturesByObliqueCamera9Directions(magoRenderableScene,
                            decimatedRenderableScene,
                            fboSet,
                            faceCodeFboSet,
                            mapCameraDirectionTypeModelViewMatrix,
                            mapCameraDirectionTypeProjection,
                            renderingSession);
                    // end of making oblique camera textures

                    if (magoRenderableScene != null) {
                        magoRenderableScene.deleteObjects();
                    }

                    if (decimatedRenderableScene != null) {
                        decimatedRenderableScene.deleteObjects();
                    }

                } catch (Exception e) {
                    log.error("[ERROR] initializing the engine: ", e);
                }

                if (gaiaSceneMaster == null) {
                    gaiaSceneMaster = gaiaScene;
                } else {
                    GaiaExtractor extractor = new GaiaExtractor();
                    List<GaiaPrimitive> primitives = extractor.extractAllPrimitives(gaiaScene);
                    GaiaNode rootNodeMaster = gaiaSceneMaster.getNodes().get(0);
                    GaiaNode nodeMaster = rootNodeMaster.getChildren().get(0);
                    GaiaMesh meshMaster = nodeMaster.getMeshes().get(0);
                    meshMaster.getPrimitives().addAll(primitives);
                    gaiaScene = null;
                }

                if (gaiaSet != null) {
                    gaiaSet.clear();
                }

                counter++;
                if (counter > 20) {
                    counter = 0;
                }
            }

            finishRenderingSession(renderingSession, fboSet, faceCodeFboSet);
        } finally {
            renderingSession.close();
        }

        // Test save 9 camera rendered.*************************
//        String outputPathStringTest = "D:\\temp";
//        String nodeNameTest = "albedo";
//        save9MagoFboAsPng(
//                fboSet,
//                outputPathStringTest,
//                nodeNameTest
//        );
//
//        nodeNameTest = "colorCode";
//        save9MagoFboAsPng(
//                faceCodeFboSet,
//                outputPathStringTest,
//                nodeNameTest
//        );
        // End test.------------------------------------------------------

        // Join all surfaces and weld vertices of the gaiaSceneMaster.
        gaiaSceneMaster.joinAllSurfaces();
        weld.apply(gaiaSceneMaster);
        cleaner.apply(gaiaSceneMaster);

//        // GaiaSkirtMaker.**********************************************************************************************
//        double nodeBoxSizeX = nodeBBox.getSizeX();
//        double nodeBoxSizeY = nodeBBox.getSizeY();
//        double nodeBoxSizeZ = nodeBBox.getSizeZ();
//        GaiaBoundingBox nodeBBoxCentered = new GaiaBoundingBox(-nodeBoxSizeX / 2.0, -nodeBoxSizeY / 2.0, -nodeBoxSizeZ / 2.0,
//                nodeBoxSizeX / 2.0, nodeBoxSizeY / 2.0, nodeBoxSizeZ / 2.0
//        );
//        GaiaSkirtMaker skirtMaker = new GaiaSkirtMaker();
//        double limitBoxSize = nodeBBox.getMaxSize() / 16.0;
//        double tolerance = nodeBoxSizeX * 0.08;
//        double skirtDepth = nodeBoxSizeX * 0.08;
//        double maxSegmentLength = nodeBoxSizeX * 0.5;
//
//        skirtMaker.addSkirtsToScene(
//                gaiaSceneMaster,
//                nodeBBoxCentered,
//                tolerance,
//                skirtDepth,
//                maxSegmentLength
//        );
//        // end making skirt.--------------------------------------------------------------------------------------------
        // Make frontier expansion.************************************************************************************
        GaiaFrontierExpander frontierExpander = new GaiaFrontierExpander();
        double maxNodeBBoxSize = nodeBBox.getMaxSize();
        frontierExpander.expandFrontiersToScene(gaiaSceneMaster, nodeBBox, 0.2, maxNodeBBoxSize * 0.005);
        // End making frontier expansion.------------------------------------------------------------------------------

        gaiaSceneMaster.joinAllSurfaces();
        weld.apply(gaiaSceneMaster);
        cleaner.apply(gaiaSceneMaster);
        // end----------------------------------------------------------------------------------------------------------

        HalfEdgeScene halfEdgeSceneMaster = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(gaiaSceneMaster);

        List<GaiaTexture> resultAtlasTextures = new ArrayList<>();
        // Here scissor the atlas textures.
        atlasTextureForIntegralReMesh9Directions(fboSet,
                faceCodeFboSet,
                backgroundColor,
                halfEdgeSceneMaster,
                mapCameraDirectionTypeBBox,
                mapCameraDirectionTypeModelViewMatrix,
                resultAtlasTextures,
                mapClassificationCamDirTypeFacesList,
                outputPathString, nodeName, lod);
        // end of atlas texture*************************************************************************************

        // check if atlasTexture is made.
        if (!resultAtlasTextures.isEmpty()) {
            resultHalfEdgeScenes.add(halfEdgeSceneMaster);
        }
    }

    public void integralDecimateByObliqueCamera(List<SceneInfo> sceneInfos,
                                                List<HalfEdgeScene> resultHalfEdgeScenes,
                                                DecimateParameters decimateParameters,
                                                ReMeshParameters reMeshParams,
                                                GaiaBoundingBox nodeBBox,
                                                Matrix4d nodeTMatrix,
                                                int maxScreenSize,
                                                String outputPathString,
                                                String nodeName,
                                                int lod) {
        // Note: There are only one scene in the scene list
        Map<CameraDirectionType, GaiaBoundingBox> mapCameraDirectionTypeBBox = new HashMap<>();
        Map<CameraDirectionType, Matrix4d> mapCameraDirectionTypeModelViewMatrix = new HashMap<>();
        Map<CameraDirectionType, Projection> mapCameraDirectionTypeProjection = new HashMap<>();

        double screenPixelsForMeter = 20;
        double nodeBBoxMaxSize = nodeBBox.getMaxSize();

        if (!Double.isFinite(nodeBBoxMaxSize)
                || nodeBBoxMaxSize <= 0.0) {

            throw new IllegalStateException(
                    "Invalid node bounding-box size: "
                            + nodeBBoxMaxSize
            );
        }

        int targetMaxSize = 512;
        screenPixelsForMeter = targetMaxSize / nodeBBoxMaxSize;
        MagoFboSet fboSet = create9MagoFbos(nodeBBox,
                renderDirections,
                mapCameraDirectionTypeBBox,
                mapCameraDirectionTypeModelViewMatrix,
                mapCameraDirectionTypeProjection,
                screenPixelsForMeter);

        Vector4f backgroundColor =
                new Vector4f(
                        1.0f,
                        0.0f,
                        1.0f,
                        1.0f
                );

        int backgroundArgb =
                toArgb(backgroundColor);

        fboSet.clearAll(
                backgroundArgb,
                1.0f
        ); // only one time

        // create faceCodeFboSet by copy from fboSet.
        MagoFboSet faceCodeFboSet =
                fboSet.createCompatible(
                        BACKGROUND_FACE_CODE,
                        1.0f
                );

        Matrix4d nodeMatrixInv = new Matrix4d(nodeTMatrix);
        nodeMatrixInv.invert();

        GaiaScene gaiaSceneMaster = null;
        double weldError = 1e-6; // 1e-6 is a good value for remeshing

        // render the scenes
        int scenesCount = sceneInfos.size();
        //List<RenderableGaiaScene> renderableGaiaScenes = new ArrayList<>();
        int counter = 0;
        int faceIdAvailable = 0;

        Map<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> mapClassificationCamDirTypeFacesList = new HashMap<>();

        GaiaSceneCleaner cleaner = new GaiaSceneCleaner();
        GaiaWeldOptions weldOptions = GaiaWeldOptions.builder()
                .error(weldError)
                .checkTexCoord(false)
                .checkNormal(false)
                .checkColor(false)
                .checkBatchId(false)
                .build();

        MagoRenderableMaker magoRenderableMaker = new MagoRenderableMaker();
        GaiaTriangulator triangulator = new GaiaTriangulator();

        MagoRenderingSession renderingSession = renderingBackend.openSession();
        try {
            for (int i = 0; i < scenesCount; i++) {
                // load and render, one by one
                SceneInfo sceneInfo = sceneInfos.get(i);
                String scenePath = sceneInfo.getScenePath();
                Matrix4d sceneTMat = sceneInfo.getTransformMatrix();

                // must find the local position of the scene rel to node
                Vector3d scenePosWC = new Vector3d(sceneTMat.m30(), sceneTMat.m31(), sceneTMat.m32());
                Vector3d scenePosLC = nodeMatrixInv.transformPosition(scenePosWC, new Vector3d());

                // calculate the local sceneTMat
                Matrix4d sceneTMatLC = new Matrix4d();
                sceneTMatLC.identity();
                sceneTMatLC.m30(scenePosLC.x);
                sceneTMatLC.m31(scenePosLC.y);
                sceneTMatLC.m32(scenePosLC.z);

                // load the set file
                GaiaSet gaiaSet = null;
                GaiaScene gaiaScene = null;
                GaiaScene gaiaSceneToRender = null;
                MagoRenderableScene magoRenderableScene = null;
                Path path = Paths.get(scenePath);
                try {
                    gaiaSet = GaiaSet.readFile(path);
                    gaiaScene = new GaiaScene(gaiaSet);
                    gaiaSceneToRender = new GaiaScene(gaiaSet);
                    triangulator.apply(gaiaSceneToRender);

                    GaiaNode gaiaNode = gaiaSceneToRender.getNodes().getFirst();
                    gaiaNode.setTransformMatrix(new Matrix4d(sceneTMatLC));
                    gaiaNode.setPreMultipliedTransformMatrix(new Matrix4d(sceneTMatLC));
                    magoRenderableScene = magoRenderableMaker.makeScene(gaiaSceneToRender);
                } catch (Exception e) {
                    log.error("[ERROR] reading the file: ", e);
                }

                if (gaiaScene == null) {
                    // throw error
                    throw new RuntimeException("[ERROR] integralReMeshByObliqueCamera : GaiaScene is null");
                }

                if (gaiaSceneToRender != null) {
                    gaiaSceneToRender.clear();
                    gaiaSceneToRender = null;
                }

                //gaiaScenesContainer.setRenderableGaiaScenes(renderableGaiaScenes);

                // decimate the scene.****************************************************************************************
                triangulator.apply(gaiaScene);

                GaiaNode gaiaNode = gaiaScene.getNodes().getFirst();
                gaiaNode.setTransformMatrix(new Matrix4d(sceneTMatLC));
                gaiaNode.setPreMultipliedTransformMatrix(new Matrix4d(sceneTMatLC));

                GaiaBaker baker = new GaiaBaker();
                baker.apply(gaiaScene);
                gaiaScene.joinAllSurfaces();

                List<GaiaMaterial> materials = gaiaScene.getMaterials();

                // delete materials.
                for (GaiaMaterial material : materials) {
                    material.clear();
                }
                gaiaScene.getMaterials().clear();

                GaiaStatistics stats = GaiaStatistics.calculateStatistics(gaiaScene);

                // Pre-ReMesh.******************************************************************************************
                GeometryOnlyReMesherByOctree preReMesher = new GeometryOnlyReMesherByOctree();
                GaiaBoundingBox effectiveNodeBBox = nodeBBox.clone();

                if (lod == 1) {
                    double desiredLeafSize = 0.8;
                    OctreeBBoxInfo octreeBoxInfo = preReMesher.calculateBoundingBoxForLeafDistInfo(nodeBBox, desiredLeafSize);
                    int octreeMaxDepth = octreeBoxInfo.maxDepth;
                    double rootOctreeSize = octreeBoxInfo.rootCubeSize;
                    double nodeSize = nodeBBox.getMaxSize();
                    double scaleFactor = rootOctreeSize / nodeSize;
                    effectiveNodeBBox.expand(nodeSize * scaleFactor * 0.5);

                    preReMesher.setReMeshAnyWay(false);
                    preReMesher.setLimitDepth(octreeMaxDepth);
                    preReMesher.setMinFacesCount(1);
                    preReMesher.setLimitBoxSize(desiredLeafSize);
                } else {
                    double desiredLeafSize = 1.2;
                    OctreeBBoxInfo octreeBoxInfo = preReMesher.calculateBoundingBoxForLeafDistInfo(nodeBBox, desiredLeafSize);
                    int octreeMaxDepth = octreeBoxInfo.maxDepth;
                    double rootOctreeSize = octreeBoxInfo.rootCubeSize;
                    double nodeSize = nodeBBox.getMaxSize();
                    double scaleFactor = rootOctreeSize / nodeSize;
                    effectiveNodeBBox.expand(nodeSize * scaleFactor * 0.5);

                    preReMesher.setReMeshAnyWay(true);
                    preReMesher.setLimitDepth(octreeMaxDepth);
                    preReMesher.setMinFacesCount(1);
                    preReMesher.setLimitBoxSize(desiredLeafSize);
                }

                preReMesher.reMeshScene(gaiaScene, stats, effectiveNodeBBox);
                GaiaWelder weld = new GaiaWelder(weldOptions);
                weld.apply(gaiaScene);
                cleaner.apply(gaiaScene);
                // End pre-ReMesh.--------------------------------------------------------------------------------------

                double averageEdgeSize = stats.getAverageEdgeSize();
                double smallHedgeSize = averageEdgeSize * 1.2;
                smallHedgeSize = Math.min(smallHedgeSize, 1.0);
                double minHedgeSize = averageEdgeSize;
                minHedgeSize = Math.min(minHedgeSize, 0.5);
                decimateParameters.setSmallHedgeSize(smallHedgeSize);
                decimateParameters.setHedgeMinLength(minHedgeSize);
                HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(gaiaScene);
                HalfEdgeDecimator decimator = new HalfEdgeDecimator(decimateParameters);
                decimator.apply(halfEdgeScene);

                // now, try to reMesh vegetation.
                log.debug("trianglesCount = " + stats.trianglesCount
                        + ", areaTotal = " + stats.areaTotal
                        + ", density = " + stats.trianglesDensity
                        + ", normalVariance = " + stats.normalVariance
                        + ", verticalRange = " + stats.verticalRange
                        + ", areaFoldRatio = " + stats.areaFoldRatio
                        + ", averageEdgeSize = " + stats.averageEdgeSize);
                GeometryOnlyReMesherByOctree reMesherByOctree = new GeometryOnlyReMesherByOctree();
                double nodeBoxSize = nodeBBox.getMaxSize();
                double minBoxSize = nodeBoxSize / 17.0;
                if (lod == 1) {
                    reMesherByOctree.setLimitDepth(12);
                    reMesherByOctree.setMinFacesCount(5);
                    reMesherByOctree.setLimitBoxSize(minBoxSize);
                } else {
                    reMesherByOctree.setLimitDepth(12);
                    reMesherByOctree.setMinFacesCount(5);
                    reMesherByOctree.setLimitBoxSize(minBoxSize);
                }

                Vector3d scenePositionRelToCellGrid = sceneInfo.getScenePosLC(); // relative position of the scene respect the center of RootNode (Depth = 0).
                gaiaScene = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);
                reMesherByOctree.reMeshScene(gaiaScene, stats, nodeBBox);

                //******************************************************************************************************
                halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(gaiaScene);

                try {
                    // render the scene
                    log.debug("Rendering the scene : " + i + " / " + scenesCount + ". LOD : " + lod);

                    // for each gaiaScene, set the available faceIds, to use for colorCoded rendering
                    List<HalfEdgeSurface> halfEdgeSurfaces = halfEdgeScene.extractSurfaces(null);
                    for (HalfEdgeSurface halfEdgeSurface : halfEdgeSurfaces) {
                        List<HalfEdgeFace> halfEdgeFaces = halfEdgeSurface.getFaces();
                        for (HalfEdgeFace halfEdgeFace : halfEdgeFaces) {
                            halfEdgeFace.setId(faceIdAvailable);
                            faceIdAvailable++;
                        }
                    }

                    gaiaScene = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene); // decimated gaiaScene.
                    MagoRenderableScene decimatedRenderableScene = magoRenderableMaker.makeScene(gaiaScene);

                    int bufferedImageType = BufferedImage.TYPE_INT_ARGB;
                    int texturePixelsForMeter = 20; // decimateParameters.getTexturePixelsForMeter();
                    makeIntegralBoxTexturesByObliqueCamera9Directions(magoRenderableScene,
                            decimatedRenderableScene,
                            fboSet,
                            faceCodeFboSet,
                            mapCameraDirectionTypeModelViewMatrix,
                            mapCameraDirectionTypeProjection,
                            renderingSession);

                    if (magoRenderableScene != null) {
                        magoRenderableScene.deleteObjects();
                    }

                    if (decimatedRenderableScene != null) {
                        decimatedRenderableScene.deleteObjects();
                    }

                } catch (Exception e) {
                    log.error("[ERROR] initializing the engine: ", e);
                }

                if (gaiaSceneMaster == null) {
                    gaiaSceneMaster = gaiaScene;
                } else {
                    GaiaExtractor extractor = new GaiaExtractor();
                    List<GaiaPrimitive> primitives = extractor.extractAllPrimitives(gaiaScene);
                    GaiaNode rootNodeMaster = gaiaSceneMaster.getNodes().get(0);
                    GaiaNode nodeMaster = rootNodeMaster.getChildren().get(0);
                    GaiaMesh meshMaster = nodeMaster.getMeshes().get(0);
                    meshMaster.getPrimitives().addAll(primitives);
                    gaiaScene = null;
                }

                if (gaiaSet != null) {
                    gaiaSet.clear();
                }

                counter++;
                if (counter > 20) {
                    counter = 0;
                }
            }

            finishRenderingSession(renderingSession, fboSet, faceCodeFboSet);
        } finally {
            renderingSession.close();
        }

        // Test save 9 camera rendered.*************************
//        String outputPathStringTest = "D:\\temp";
//        String nodeNameTest = "albedo";
//        save9MagoFboAsPng(
//                fboSet,
//                outputPathStringTest,
//                nodeNameTest
//        );
//
//        nodeNameTest = "colorCode";
//        save9MagoFboAsPng(
//                faceCodeFboSet,
//                outputPathStringTest,
//                nodeNameTest
//        );
        // End test.------------------------------------------------------

//        // Finish LOD2 -> LOD3 transition anchors.***************************************
//        if (lod == 2 && reMeshParams != null) {
//            TileBoundaryAnchors lodTransitionTileAnchors =
//                    reMeshParams.getTileBoundaryAnchors();
//
//            GlobalBoundaryAnchors globalBoundaryAnchors =
//                    reMeshParams.getGlobalBoundaryAnchors();
//
//            if (lodTransitionTileAnchors != null && globalBoundaryAnchors != null) {
//                ReMesherVertexClusterV2.finishTileBoundaryAnchors(lodTransitionTileAnchors);
//
//                globalBoundaryAnchors.addMissingFromTileAnchors(lodTransitionTileAnchors);
//
//                log.debug("LOD2 -> LOD3 transition tile anchors cells = {}",
//                        lodTransitionTileAnchors.frontierAveragePositions.size());
//
//                log.debug("LOD2 -> LOD3 globalBoundaryAnchors locked cells = {}",
//                        globalBoundaryAnchors.lockedAveragePositions.size());
//
//                // Importante:
//                // TileBoundaryAnchors es temporal. Los anchors útiles ya quedaron bloqueados en global.
//                lodTransitionTileAnchors.clear();
//            }
//        }
//        // End LOD2 -> LOD3 transition anchors.------------------------------------------

        // Join all surfaces and weld vertices of the gaiaSceneMaster.
        gaiaSceneMaster.joinAllSurfaces();
        GaiaWelder weld = new GaiaWelder(weldOptions);
        weld.apply(gaiaSceneMaster);
        cleaner.apply(gaiaSceneMaster);

        // Make frontier expansion.************************************************************************************
        GaiaFrontierExpander frontierExpander = new GaiaFrontierExpander();
        double maxNodeBBoxSize = nodeBBox.getMaxSize();
        frontierExpander.expandFrontiersToScene(gaiaSceneMaster, nodeBBox, 0.2, maxNodeBBoxSize * 0.003);
        // End making frontier expansion.------------------------------------------------------------------------------

        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaFace> gaiaFacesMaster = extractor.extractAllFaces(gaiaSceneMaster);
        if (gaiaFacesMaster.isEmpty()) {
            log.info("[ERROR] gaiaFacesMaster is empty");
            return;
        }

        HalfEdgeScene halfEdgeSceneMaster = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(gaiaSceneMaster);

        List<GaiaTexture> resultAtlasTextures = new ArrayList<>();
        // Here scissor the atlas textures.
        atlasTextureForIntegralReMesh9Directions(fboSet,
                faceCodeFboSet,
                backgroundColor,
                halfEdgeSceneMaster,
                mapCameraDirectionTypeBBox,
                mapCameraDirectionTypeModelViewMatrix,
                resultAtlasTextures,
                mapClassificationCamDirTypeFacesList,
                outputPathString, nodeName, lod);

        // check if atlasTexture is made.
        if (!resultAtlasTextures.isEmpty()) {
            resultHalfEdgeScenes.add(halfEdgeSceneMaster);
        }
    }

    public MagoFbo renderTopView(List<SceneInfo> sceneInfos,
                                 GaiaBoundingBox nodeBBox,
                                 Matrix4d nodeTMatrix,
                                 int maxScreenSize,
                                 int maxDepthScreenSize) {
        // render the scene
        log.info("Rendering the scene...getColorAndDepthRender");

        int screenWidth = 1024;
        int screenHeight = 1024;

        // calculate the projectionMatrix for the camera
        Vector3d bboxCenter = nodeBBox.getCenter();
        float xLength = (float) nodeBBox.getSizeX();
        float yLength = (float) nodeBBox.getSizeY();
        float zLength = (float) nodeBBox.getSizeZ();

        Projection projection = new Projection(0, screenWidth, screenHeight);
        float safeXLength =
                Math.max(xLength, 0.001f);

        float safeYLength =
                Math.max(yLength, 0.001f);

        float safeZLength =
                Math.max(zLength, 0.001f);

        float zPadding =
                Math.max(
                        safeZLength * 0.01f,
                        0.1f
                );

        projection.setProjectionOrthographic(
                -safeXLength * 0.5f,
                safeXLength * 0.5f,
                -safeYLength * 0.5f,
                safeYLength * 0.5f,
                -safeZLength * 0.5f - zPadding,
                safeZLength * 0.5f + zPadding
        );

        MagoFbo colorFbo = new MagoFbo("topView", 1024, 1024);
        Vector4f backgroundColor =
                new Vector4f(
                        1.0f,
                        0.0f,
                        1.0f,
                        1.0f
                );

        int backgroundArgb =
                toArgb(backgroundColor);

        colorFbo.clear(
                backgroundArgb,
                1.0f
        );

        // now set camera position
        Camera camera = new Camera();
        camera.setPosition(bboxCenter);
        camera.setDirection(new Vector3d(0, 0, -1));
        camera.setUp(new Vector3d(0, 1, 0));

        Matrix4d modelViewMatrix =
                new Matrix4d(camera.getModelViewMatrix());

        // 1. albedo-render.
        if (modelViewMatrix == null) {
            throw new IllegalStateException(
                    "model-view matrix is null"
            );
        }

        if (projection == null) {
            throw new IllegalStateException(
                    "projection is null"
            );
        }

        MagoShaderProgram shaderProgram =
                new MagoShaderProgram(
                        "texturedShader",
                        new MagoDefaultVertexShader(),
                        new MagoTexturedFragmentShader()
                );

        // Render-context.
        MagoRenderContext renderContext =
                new MagoRenderContext();

        renderContext.setShaderProgram(
                shaderProgram
        );

        renderContext.setDepthTestEnabled(true);
        renderContext.setCullFaceEnabled(true);
        renderContext.setBlendEnabled(false);
        renderContext.getUniforms().textureFilter = MagoTextureFilter.BILINEAR;
        renderContext.getUniforms().wrapS = MagoTextureWrap.CLAMP_TO_EDGE;
        renderContext.getUniforms().wrapT = MagoTextureWrap.CLAMP_TO_EDGE;
        renderContext.getUniforms().invertTextureV = false;

        renderContext.setFbo(colorFbo);
        renderContext.setViewMatrix(modelViewMatrix);

        renderContext.setProjectionMatrix(
                projection.getProjMatrix()
        );

        renderContext.setPolygonMode(
                MagoPolygonMode.FILL
        );

        Matrix4d nodeMatrixInv = new Matrix4d(nodeTMatrix);
        nodeMatrixInv.invert();

        MagoRenderableMaker magoRenderableMaker = new MagoRenderableMaker();
        GaiaTriangulator triangulator = new GaiaTriangulator();

        // render the scenes
        int scenesCount = sceneInfos.size();
        int counter = 0;
        MagoRenderingSession renderingSession = renderingBackend.openSession();
        try {
            for (int i = 0; i < scenesCount; i++) {
                // load and render, one by one
                SceneInfo sceneInfo = sceneInfos.get(i);
                String scenePath = sceneInfo.getScenePath();
                Matrix4d sceneTMat = sceneInfo.getTransformMatrix();

                // must find the local position of the scene rel to node
                Vector3d scenePosWC = new Vector3d(sceneTMat.m30(), sceneTMat.m31(), sceneTMat.m32());
                Vector3d scenePosLC = nodeMatrixInv.transformPosition(scenePosWC, new Vector3d());

                // calculate the local sceneTMat
                Matrix4d sceneTMatLC = new Matrix4d();
                sceneTMatLC.identity();
                sceneTMatLC.m30(scenePosLC.x);
                sceneTMatLC.m31(scenePosLC.y);
                sceneTMatLC.m32(scenePosLC.z);

                // load the set file
                GaiaSet gaiaSet = null;
                GaiaScene gaiaScene = null;
                MagoRenderableScene renderableScene = null;
                Path path = Paths.get(scenePath);
                try {
                    gaiaSet = GaiaSet.readFile(path);
                    gaiaScene = new GaiaScene(gaiaSet);
                    triangulator.apply(gaiaScene);
                    GaiaNode gaiaNode = gaiaScene.getNodes().get(0);
                    gaiaNode.setTransformMatrix(sceneTMatLC);
                    gaiaNode.setPreMultipliedTransformMatrix(sceneTMatLC);
                    renderableScene = magoRenderableMaker.makeScene(gaiaScene);
                } catch (Exception e) {
                    log.error("[ERROR] reading the file: ", e);
                }

                try {
                    // render the scene
                    Objects.requireNonNull(
                            renderableScene,
                            "renderableScene must not be null"
                    );

                    renderingSession.renderIntoFbo(
                            renderableScene,
                            renderContext
                    );

                } catch (Exception e) {
                    log.error("[ERROR] initializing the engine: ", e);
                }

                if (gaiaSet != null) {
                    gaiaSet.clear();
                }

                if (gaiaScene != null) {
                    gaiaScene.clear();
                }

                counter++;
                if (counter > 20) {
                    counter = 0;
                }
            }

            renderingSession.readback();
        } finally {
            renderingSession.close();
        }

//        String outputPathString = "D:\\temp";
//        String nodeName = "test";
//        saveMagoFboAsPng(
//                colorFbo,
//                outputPathString,
//                nodeName + "_topView"
//        );

        int hola = 0;

        return colorFbo;

        // take the final rendered colorBuffer of the fbo
//        colorFbo.bind();
//        BufferedImage image = colorFbo.getBufferedImage(bufferedImageType);
//        resultImages.add(image);
//        colorFbo.unbind();
//
//        // take the final rendered depthBuffer of the fbo
//        int depthBufferedImageType = BufferedImage.TYPE_INT_ARGB;
//        depthFbo.bind();
//        BufferedImage depthImage = depthFbo.getBufferedImage(depthBufferedImageType);
//        resultImages.add(depthImage);
//        depthFbo.unbind();

        // delete renderableGaiaScenes
//        engine.deleteObjects();
//        for (RenderableGaiaScene renderableScene : renderableGaiaScenes) {
//            renderableScene.deleteGLBuffers();
//        }
    }

    private void translateScene(GaiaScene gaiaScene, Vector3d translation) {
        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaPrimitive> primitives = extractor.extractAllPrimitives(gaiaScene);
        for (GaiaPrimitive primitive : primitives) {
            List<GaiaVertex> vertices = primitive.getVertices();
            for (GaiaVertex vertex : vertices) {
                Vector3d position = vertex.getPosition();
                position.add(translation);
                vertex.setPosition(position);
            }
        }
    }

    private void atlasTextureForIntegralReMesh9Directions(MagoFboSet fboSet,
                                                          MagoFboSet faceCodeFboSet,
                                                          Vector4f backgroundColor,
                                                          HalfEdgeScene halfEdgeSceneMaster,
                                                          Map<CameraDirectionType, GaiaBoundingBox> mapCameraDirectionTypeBBox,
                                                          Map<CameraDirectionType, Matrix4d> mapCameraDirectionTypeModelViewMatrix,
                                                          List<GaiaTexture> resultAtlasTextures,
                                                          Map<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> mapClassificationCamDirTypeFacesList,
                                                          String outputPathString,
                                                          String nodeName, int lod) {
        List<HalfEdgeSurface> surfaces = halfEdgeSceneMaster.extractSurfaces(null);

        TextureAtlasManager texAtlasManager = new TextureAtlasManager();
        Map<Integer, List<HalfEdgeFace>> facesClassificationMap = new HashMap<>();
        int surfacesCount = surfaces.size();
        for (int i = 0; i < surfacesCount; i++) {
            HalfEdgeSurface surface = surfaces.get(i);
            int facesCount = surface.getFaces().size();
            for (int j = 0; j < facesCount; j++) {
                HalfEdgeFace face = surface.getFaces().get(j);
                int classificationId = face.getClassifyId();
                List<HalfEdgeFace> facesList = facesClassificationMap.computeIfAbsent(classificationId, k -> new ArrayList<>());
                facesList.add(face);
            }
        }

        int classificationId = -1; // in integralReMesh, there is only one classificationId = -1

        int bufferedImageType = BufferedImage.TYPE_INT_ARGB;
        List<com.gaia3d.basic.texture.atlas.TexturesAtlasData> texturesAtlasDataList = new ArrayList<>();
        //Vector4f backgroundColor = integralReMeshParameters.getBackgroundColor();
        Color backGroundColor = new Color(
                (int) (backgroundColor.x * 255),
                (int) (backgroundColor.y * 255),
                (int) (backgroundColor.z * 255)
        );

        for (CameraDirectionType cameraDirectionType : renderDirections) {
            MagoFbo fbo = fboSet.get(cameraDirectionType);
            BufferedImage image = fbo.getBufferedImage();
            if (image != null) {
                texAtlasManager.dilateBackgroundColor(image, backGroundColor);
                com.gaia3d.basic.texture.atlas.TexturesAtlasData texturesAtlasData = new com.gaia3d.basic.texture.atlas.TexturesAtlasData();
                texturesAtlasData.setClassifyId(classificationId);
                texturesAtlasData.setCameraDirectionType(cameraDirectionType);
                texturesAtlasData.setTextureImage(image);
                texturesAtlasDataList.add(texturesAtlasData);
            }
        }

        // There are no visible faces, so 1rst set the CAMERA_DIRECTION_ZNEG to all the halfEdgeFaces as default
        List<HalfEdgeFace> facesList = facesClassificationMap.get(classificationId);
        if (facesList == null) {
            log.error("atlasTextureForIntegralReMesh: facesList is null for classificationId: " + classificationId);
            return;
        }
        for (HalfEdgeFace halfEdgeFace : facesList) {
            halfEdgeFace.setCameraDirectionType(CameraDirectionType.ZNEG);
        }

        // check visibility data manager****************************************************************************
        FaceVisibilityManager visibilityManager =
                new FaceVisibilityManager();

        GaiaScene gaiaSceneMaster = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeSceneMaster);

        for (CameraDirectionType direction
                : renderDirections) {

            MagoFbo faceCodeFbo =
                    faceCodeFboSet.get(direction);

            visibilityManager.updateFaceVisibilityData(
                    gaiaSceneMaster,
                    direction,
                    faceCodeFbo,
                    mapCameraDirectionTypeModelViewMatrix,
                    mapCameraDirectionTypeBBox
            );
        }

        // now assign face to each cameraDirectionType
        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaPrimitive> gaiaPrimitives = extractor.extractAllPrimitives(gaiaSceneMaster);

        // Solve CameraDirectionType for each face.***
        Map<GaiaFace, CameraDirectionType> mapFaceToBestCameraDirectionType = visibilityManager.solveCameraDirectionTypeToFaces(gaiaPrimitives);
        Map<Integer, CameraDirectionType> mapFaceIdToBestCameraDirectionType = new HashMap<>();
        for (GaiaFace gaiaFace : mapFaceToBestCameraDirectionType.keySet()) {
            int faceId = gaiaFace.getId();
            CameraDirectionType cameraDirectionType = mapFaceToBestCameraDirectionType.get(gaiaFace);
            mapFaceIdToBestCameraDirectionType.put(faceId, cameraDirectionType);
        }

        // set cameraDirectionType to halfEdges of halfEdgeSceneMaster.
        for (HalfEdgeFace halfEdgeFace : facesList) {
            int faceId = halfEdgeFace.getId();
            CameraDirectionType cameraDirectionType = mapFaceIdToBestCameraDirectionType.get(faceId);
            halfEdgeFace.setCameraDirectionType(cameraDirectionType);
        }

        //**************************************************************************************************************
        halfEdgeSceneMaster.splitFacesByBestObliqueCameraDirectionToProject();

        // now, for each classifyId - CameraDirectionType, calculate the texCoords
        Map<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> mapFaceGroupByClassifyIdAndObliqueCamDirType = new HashMap<>();
        List<HalfEdgeSurface> halfEdgeSurfaces = halfEdgeSceneMaster.extractSurfaces(null);
        for (HalfEdgeSurface halfEdgeSurface : halfEdgeSurfaces) {
            halfEdgeSurface.getMapClassifyIdToCameraDirectionTypeToFaces(mapFaceGroupByClassifyIdAndObliqueCamDirType);

            // test create texCoords (if no exist) for all vertices
            List<HalfEdgeVertex> vertexOfSurface = new ArrayList<>();
            HalfEdgeUtils.getVerticesOfFaces(halfEdgeSurface.getFaces(), vertexOfSurface);
            for (HalfEdgeVertex vertex : vertexOfSurface) {
                if (vertex.getTexcoords() == null) {
                    vertex.setTexcoords(new Vector2d(0.0, 0.0));
                }
            }
        }

        CameraDirectionType cameraDirectionType;

        List<HalfEdgeVertex> verticesOfFaces = new ArrayList<>();
        Map<HalfEdgeVertex, HalfEdgeVertex> visitedVerticesMap = new HashMap<>();
        double texCoordError = 1e-4; // clamp error.
        for (Map.Entry<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> entry : mapFaceGroupByClassifyIdAndObliqueCamDirType.entrySet()) {
            int classifyId = entry.getKey();
            Map<CameraDirectionType, List<HalfEdgeFace>> mapCameraDirectionTypeFacesList = entry.getValue();
            for (Map.Entry<CameraDirectionType, List<HalfEdgeFace>> entry1 : mapCameraDirectionTypeFacesList.entrySet()) {
                cameraDirectionType = entry1.getKey();
                facesList = entry1.getValue();

                mapClassificationCamDirTypeFacesList.put(classifyId, mapCameraDirectionTypeFacesList);

                // calculate the texCoords of the vertices
                GaiaBoundingBox bbox = mapCameraDirectionTypeBBox.get(cameraDirectionType);
                Matrix4d modelViewMatrix = mapCameraDirectionTypeModelViewMatrix.get(cameraDirectionType);

                if (modelViewMatrix == null) {
                    log.info("makeBoxTexturesByObliqueCamera() : modelViewMatrix is null." + " camDirType = " + cameraDirectionType);
                    continue;
                }

                for (HalfEdgeFace halfEdgeFace : facesList) {
                    verticesOfFaces.clear();
                    verticesOfFaces = halfEdgeFace.getVertices(verticesOfFaces);
                    for (HalfEdgeVertex vertex : verticesOfFaces) {
                        if (visitedVerticesMap.containsKey(vertex)) {
                            continue;
                        }
                        visitedVerticesMap.put(vertex, vertex);

                        Vector3d vertexPosition = vertex.getPosition();
                        Vector4d vertexPosition4d = new Vector4d(vertexPosition.x, vertexPosition.y, vertexPosition.z, 1.0);
                        modelViewMatrix.transform(vertexPosition4d);
                        double x = vertexPosition4d.x;
                        double y = vertexPosition4d.y;
                        double z = vertexPosition4d.z;
                        double w = vertexPosition4d.w;
                        double texCoordX = (x - bbox.getMinX()) / bbox.getSizeX();
                        double texCoordY = (y - bbox.getMinY()) / bbox.getSizeY();

//                        if (texCoordX < 0.0 || texCoordX > 1.0 || texCoordY < 0.0 || texCoordY > 1.0) {
//                            log.info("makeBoxTexturesByObliqueCamera() : texCoordX or texCoordY is out of range." + "camDirType = " + cameraDirectionType);
//                        }

                        // invert the texCoordY
                        texCoordY = 1.0 - texCoordY;

                        // clamp the texCoords
                        Vector2d texCoord = new Vector2d(texCoordX, texCoordY);
                        GaiaTextureUtils.clampTextureCoordinate(texCoord, texCoordError);
                        vertex.setTexcoords(texCoord);
                    }
                }
            }
        }

        // save atlas texture data**********************************************************************************
        String netTempPathString = outputPathString + File.separator + "temp" + File.separator + "reMeshTemp";
        Path netTempPath = Paths.get(netTempPathString);
        // create dirs if not exists
        File netTempFile = netTempPath.toFile();
        if (!netTempFile.exists() && netTempFile.mkdirs()) {
            log.debug("info : netTemp folder created.");
        }

        String netSetFolderPathString = netTempPathString + File.separator + nodeName;
        Path netSetFolderPath = Paths.get(netSetFolderPathString);
        // create dirs if not exists
        File netSetFile = netSetFolderPath.toFile();
        if (!netSetFile.exists() && netTempFile.mkdirs()) {
            log.debug("info : netSet folder created.");
        }
        String netSetImagesFolderPathString = netSetFolderPathString + File.separator + "images";
        Path netSetImagesFolderPath = Paths.get(netSetImagesFolderPathString);
        // create dirs if not exists
        File netSetImagesFolder = netSetImagesFolderPath.toFile();
        if (!netSetImagesFolder.exists() && netSetImagesFolder.mkdirs()) {
            log.debug("info : netSetImages folder created.");
        }

        com.gaia3d.basic.texture.atlas.TextureAtlasManager textureAtlasManager = new com.gaia3d.basic.texture.atlas.TextureAtlasManager();
        textureAtlasManager.doAtlasTextureProcess(texturesAtlasDataList);
        textureAtlasManager.recalculateTexCoordsAfterTextureAtlasingObliqueCamera(halfEdgeSceneMaster, texturesAtlasDataList, mapClassificationCamDirTypeFacesList);

        //faceVisibilityDataManager.deleteObjects();

        String fileName = nodeName + "_Atlas";
        String extension = ".png";
        int bufferImageType = BufferedImage.TYPE_INT_ARGB;
        GaiaTexture atlasTexture = textureAtlasManager.makeAtlasTexture(texturesAtlasDataList, bufferImageType);

        if (atlasTexture == null) {
            log.info("makeAtlasTexture() : atlasTexture is null.");
            return;
        }

        // delete texturesAtlasDataList
        for (com.gaia3d.basic.texture.atlas.TexturesAtlasData texturesAtlasData : texturesAtlasDataList) {
            texturesAtlasData.deleteObjects();
        }

        atlasTexture.setPath(fileName + extension);
        atlasTexture.setParentPath(netSetImagesFolderPath.toString());

        // finally make material with texture for the halfEdgeScene
        GaiaMaterial material = new GaiaMaterial();
        material.setName("atlasTexturesMaterial");
        Map<TextureType, List<GaiaTexture>> textures = new HashMap<>();
        List<GaiaTexture> atlasTextures = new ArrayList<>();
        atlasTextures.add(atlasTexture);
        textures.put(TextureType.DIFFUSE, atlasTextures);
        material.setTextures(textures);

        int materialsCount = halfEdgeSceneMaster.getMaterials().size();
        material.setId(materialsCount);
        halfEdgeSceneMaster.getMaterials().add(material);

        List<HalfEdgePrimitive> primitives = new ArrayList<>();
        halfEdgeSceneMaster.extractPrimitives(primitives);
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.setMaterialId(materialsCount);
        }

        // Scissoring the atlas texture****************************************************************
        halfEdgeSceneMaster.scissorTextures();
        material = halfEdgeSceneMaster.getMaterials().get(materialsCount);
        textures = material.getTextures();
        atlasTextures = textures.get(TextureType.DIFFUSE);
        GaiaTexture atlasScissoredTexture = atlasTextures.getFirst();
        atlasScissoredTexture.setParentPath(netSetImagesFolderPath.toString());
        if (atlasScissoredTexture.getBufferedImage() == null) {
            log.info("atlasScissoredTexture.getBufferedImage() is null.");
            return;
        }

        // resize the atlas texture if necessary.
        BufferedImage atlasBufferedImage = atlasScissoredTexture.getBufferedImage();
        if(atlasBufferedImage.getWidth() > 1024 || atlasBufferedImage.getHeight() > 1024) {
            ImageResizer imageResizer = new ImageResizer();
            BufferedImage resized = imageResizer.resizeMultiStepSmart(atlasBufferedImage, lod);
            atlasBufferedImage.flush();
            atlasScissoredTexture.setBufferedImage(resized);
        }

        resultAtlasTextures.add(atlasScissoredTexture);

        // save the atlas image to disk
        try {
            String imagePath = atlasScissoredTexture.getFullPath();
            File imageFile = new File(imagePath);
            ImageIO.write(atlasScissoredTexture.getBufferedImage(), "png", imageFile);
        } catch (IOException e) {
            log.debug("Error writing image: {}", e);
        }
    }

    private MagoFboSet create9MagoFbos(
            GaiaBoundingBox nodeBBox,
            CameraDirectionType[] renderDirections,
            Map<CameraDirectionType, GaiaBoundingBox> mapCameraDirectionTypeBBox,
            Map<CameraDirectionType, Matrix4d> mapCameraDirectionTypeModelViewMatrix,
            Map<CameraDirectionType, Projection> mapCameraDirectionTypeProjection,
            double screenPixelsForMeter
    ) {
        MagoFboSet fboSet = new MagoFboSet();

        Camera camera = new Camera();

        List<Vector3d> transformedVertices =
                new ArrayList<>(8);

        List<Vector3d> bboxVertices =
                nodeBBox.getVertices();

        Vector3d bboxCenter =
                nodeBBox.getCenter();

        for (CameraDirectionType directionType : renderDirections) {
            Vector3d cameraDirection =
                    CameraDirectionType.getCameraDirection(directionType);

            camera.setPosition(bboxCenter);
            camera.setDirection(cameraDirection);
            camera.setUp(
                    camera.calculateUpVector(cameraDirection)
            );

            Matrix4d modelViewMatrix =
                    new Matrix4d(camera.getModelViewMatrix());

            transformedVertices.clear();

            for (Vector3d position : bboxVertices) {
                Vector4d transformedPosition =
                        new Vector4d(position, 1.0);

                modelViewMatrix.transform(
                        transformedPosition
                );

                transformedVertices.add(
                        new Vector3d(
                                transformedPosition.x,
                                transformedPosition.y,
                                transformedPosition.z
                        )
                );
            }

            GaiaBoundingBox transformedBBox =
                    new GaiaBoundingBox();

            transformedBBox.setFromPoints(
                    transformedVertices
            );

            float minX =
                    (float) transformedBBox.getMinX();

            float maxX =
                    (float) transformedBBox.getMaxX();

            float minY =
                    (float) transformedBBox.getMinY();

            float maxY =
                    (float) transformedBBox.getMaxY();

            float minZ =
                    (float) transformedBBox.getMinZ();

            float maxZ =
                    (float) transformedBBox.getMaxZ();

            float xLength =
                    maxX - minX;

            float yLength =
                    maxY - minY;

            int fboWidth = Math.max(
                    1,
                    (int) Math.ceil(
                            xLength * screenPixelsForMeter
                    )
            );

            int fboHeight = Math.max(
                    1,
                    (int) Math.ceil(
                            yLength * screenPixelsForMeter
                    )
            );

            float near =
                    -maxZ;

            float far =
                    -minZ;

            float maxSize =
                    Math.max(xLength, yLength);

            float zOffset =
                    Math.max(maxSize * 0.001f, 0.4f);

            float zSize =
                    maxZ - minZ;

            if (zSize < 2.0f) {
                zOffset += 1.0f;
            }

            near -= zOffset;
            far += zOffset;

            /*
             * Use the real framebuffer dimensions instead of 512 x 512.
             */
            Projection projection =
                    new Projection(
                            1,
                            fboWidth,
                            fboHeight
                    );

            projection.setProjectionOrthographic(
                    minX,
                    maxX,
                    minY,
                    maxY,
                    near,
                    far
            );

            fboSet.create(
                    directionType,
                    fboWidth,
                    fboHeight
            );

            mapCameraDirectionTypeBBox.put(
                    directionType,
                    transformedBBox
            );

            mapCameraDirectionTypeModelViewMatrix.put(
                    directionType,
                    modelViewMatrix
            );

            mapCameraDirectionTypeProjection.put(
                    directionType,
                    projection
            );
        }

        return fboSet;
    }

    private void finishRenderingSession(
            MagoRenderingSession renderingSession,
            MagoFboSet albedoFbos,
            MagoFboSet faceCodeFbos
    ) {
        renderingSession.readback();
        logComparison(renderingSession, albedoFbos, "albedo");
        logComparison(renderingSession, faceCodeFbos, "face-code");
    }


    private void logComparison(
            MagoRenderingSession renderingSession,
            MagoFboSet canonicalFbos,
            String passName
    ) {
        for (CameraDirectionType direction : renderDirections) {
            MagoFbo canonical = canonicalFbos.get(direction);
            MagoFbo candidate = renderingSession.getComparisonFbo(canonical);
            if (candidate == null) {
                continue;
            }

            int[] expected = canonical.getColorBuffer();
            int[] actual = candidate.getColorBuffer();
            int different = 0;
            for (int i = 0; i < expected.length; i++) {
                if (expected[i] != actual[i]) {
                    different++;
                }
            }
            double mismatchPercent = expected.length == 0
                    ? 0.0
                    : different * 100.0 / expected.length;
            log.info(
                    "Render comparison [{}:{}] mismatched pixels: {}/{} ({}%)",
                    passName,
                    direction,
                    different,
                    expected.length,
                    String.format(Locale.ROOT, "%.4f", mismatchPercent)
            );
        }
    }

    public void makeIntegralBoxTexturesByObliqueCamera9Directions(MagoRenderableScene magoRenderableScene,
                                                                  MagoRenderableScene magoDecimatedRenderableScene,
                                                                  MagoFboSet fboSet,
                                                                  MagoFboSet faceCodeFboSet,
                                                                  Map<CameraDirectionType, Matrix4d> mapCameraDirectionTypeModelViewMatrix,
                                                                  Map<CameraDirectionType, Projection> mapCameraDirectionTypeProjection,
                                                                  MagoRenderingSession renderingSession) {

        MagoShaderProgram shaderProgram =
                new MagoShaderProgram(
                        "texturedShader",
                        new MagoDefaultVertexShader(),
                        new MagoTexturedFragmentShader()
                );

        MagoRenderContext renderContext =
                new MagoRenderContext();

        renderContext.setShaderProgram(
                shaderProgram
        );

        renderContext.setDepthTestEnabled(true);
        renderContext.setCullFaceEnabled(true);
        renderContext.setBlendEnabled(false);

        renderContext.getUniforms().textureFilter = MagoTextureFilter.BILINEAR;
        renderContext.getUniforms().wrapS = MagoTextureWrap.CLAMP_TO_EDGE;
        renderContext.getUniforms().wrapT = MagoTextureWrap.CLAMP_TO_EDGE;
        renderContext.getUniforms().invertTextureV = false;

        // albedo render.
        // 9 camera render directions.
        for (CameraDirectionType cameraDirectionType : renderDirections) {
            renderIntegralAlbedoTextureByCameraDirection(
                    magoRenderableScene,
                    cameraDirectionType,
                    mapCameraDirectionTypeModelViewMatrix,
                    mapCameraDirectionTypeProjection,
                    fboSet,
                    renderingSession,
                    renderContext
            );
        }

        // Face color-coded render.
        MagoShaderProgram shaderProgramColorCode =
                new MagoShaderProgram(
                        "face-code",
                        new MagoDefaultVertexShader(),
                        new MagoFaceCodeFragmentShader()
                );

        renderContext.setShaderProgram(
                shaderProgramColorCode
        );

        renderContext.setDepthTestEnabled(true);
        renderContext.setCullFaceEnabled(true);
        renderContext.setBlendEnabled(false);
        renderContext.getUniforms().invertTextureV = false;

        // 9 camera render directions.
        for (CameraDirectionType cameraDirectionType : renderDirections) {
            renderIntegralColorCodeTextureByCameraDirection(
                    magoDecimatedRenderableScene,
                    cameraDirectionType,
                    mapCameraDirectionTypeModelViewMatrix,
                    mapCameraDirectionTypeProjection,
                    faceCodeFboSet,
                    renderingSession,
                    renderContext
            );
        }
    }

    private void save9MagoFboAsPng(
            MagoFboSet fboSet,
            String outputPathString,
            String nodeName
    ) {
        for (CameraDirectionType renderDirection : renderDirections) {
            saveMagoFboAsPng(
                    fboSet,
                    renderDirection,
                    outputPathString,
                    nodeName + renderDirection.getName() + ".png"
            );
        }
    }

    private void saveMagoFboAsPng(
            MagoFbo fbo,
            String outputPathString,
            String nodeName
    ) {
        Objects.requireNonNull(fbo, "fbo must not be null");
        BufferedImage image = fbo.getBufferedImage();

        Path outputDirectory = Paths.get(
                outputPathString,
                "mago-render-debug"
        );

        Path outputFile = outputDirectory.resolve(
                nodeName
                        + "_"
                        + ".png"
        );

        try {
            Files.createDirectories(outputDirectory);

            boolean written = ImageIO.write(
                    image,
                    "png",
                    outputFile.toFile()
            );

            if (!written) {
                throw new IOException(
                        "No PNG ImageIO writer is available."
                );
            }

            log.info(
                    "MagoGL debug render saved: {} ({}x{})",
                    outputFile,
                    image.getWidth(),
                    image.getHeight()
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to save MagoGL render: " + outputFile,
                    e
            );
        }
    }

    private void saveMagoFboAsPng(
            MagoFboSet fboSet,
            CameraDirectionType directionType,
            String outputPathString,
            String nodeName
    ) {
        Objects.requireNonNull(fboSet, "fboSet must not be null");
        Objects.requireNonNull(directionType, "directionType must not be null");

        MagoFbo fbo = fboSet.get(directionType);

        BufferedImage image = fbo.getBufferedImage();

        Path outputDirectory = Paths.get(
                outputPathString,
                "mago-render-debug"
        );

        Path outputFile = outputDirectory.resolve(
                nodeName
                        + "_"
                        + directionType.name()
                        + ".png"
        );

        try {
            Files.createDirectories(outputDirectory);

            boolean written = ImageIO.write(
                    image,
                    "png",
                    outputFile.toFile()
            );

            if (!written) {
                throw new IOException(
                        "No PNG ImageIO writer is available."
                );
            }

            log.info(
                    "MagoGL debug render saved: {} ({}x{})",
                    outputFile,
                    image.getWidth(),
                    image.getHeight()
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to save MagoGL render: " + outputFile,
                    e
            );
        }
    }

    private void renderIntegralAlbedoTextureByCameraDirection(
            MagoRenderableScene renderableScene,
            CameraDirectionType cameraDirectionType,
            Map<CameraDirectionType, Matrix4d> mapCameraDirectionTypeModelViewMatrix,
            Map<CameraDirectionType, Projection> mapCameraDirectionTypeProjection,
            MagoFboSet magoFboSet,
            MagoRenderingSession renderingSession,
            MagoRenderContext renderContext
    ) {
        Objects.requireNonNull(
                renderableScene,
                "renderableScene must not be null"
        );

        // 1. albedo-render.
        MagoFbo fbo =
                magoFboSet.get(cameraDirectionType);

        Matrix4d modelViewMatrix =
                mapCameraDirectionTypeModelViewMatrix.get(
                        cameraDirectionType
                );

        Projection projection =
                mapCameraDirectionTypeProjection.get(
                        cameraDirectionType
                );

        if (modelViewMatrix == null) {
            throw new IllegalStateException(
                    "No model-view matrix for direction: "
                            + cameraDirectionType
            );
        }

        if (projection == null) {
            throw new IllegalStateException(
                    "No projection for direction: "
                            + cameraDirectionType
            );
        }

        renderContext.setFbo(fbo);
        renderContext.setViewMatrix(modelViewMatrix);

        renderContext.setProjectionMatrix(
                projection.getProjMatrix()
        );

        renderContext.setPolygonMode(
                MagoPolygonMode.FILL
        );

        renderContext.setCullFaceEnabled(true);
        renderContext.setDepthTestEnabled(true);
        renderContext.setBlendEnabled(false);

        renderContext.setWireframeColor(
                0xFF000000
        );

        renderingSession.renderIntoFbo(
                renderableScene,
                renderContext
        );
    }

    private void renderIntegralColorCodeTextureByCameraDirection(
            MagoRenderableScene renderableScene,
            CameraDirectionType cameraDirectionType,
            Map<CameraDirectionType, Matrix4d> mapCameraDirectionTypeModelViewMatrix,
            Map<CameraDirectionType, Projection> mapCameraDirectionTypeProjection,
            MagoFboSet magoFboSet,
            MagoRenderingSession renderingSession,
            MagoRenderContext renderContext
    ) {
        Objects.requireNonNull(
                renderableScene,
                "renderableScene must not be null"
        );

        // 1. albedo-render.
        MagoFbo fbo =
                magoFboSet.get(cameraDirectionType);

        Matrix4d modelViewMatrix =
                mapCameraDirectionTypeModelViewMatrix.get(
                        cameraDirectionType
                );

        Projection projection =
                mapCameraDirectionTypeProjection.get(
                        cameraDirectionType
                );

        if (modelViewMatrix == null) {
            throw new IllegalStateException(
                    "No model-view matrix for direction: "
                            + cameraDirectionType
            );
        }

        if (projection == null) {
            throw new IllegalStateException(
                    "No projection for direction: "
                            + cameraDirectionType
            );
        }

        renderContext.setFbo(fbo);
        renderContext.setViewMatrix(modelViewMatrix);

        renderContext.setProjectionMatrix(
                projection.getProjMatrix()
        );

        renderContext.setPolygonMode(
                MagoPolygonMode.FILL
        );

        renderContext.setCullFaceEnabled(true);
        renderContext.setDepthTestEnabled(true);
        renderContext.setBlendEnabled(false);

        renderContext.setWireframeColor(
                0xFF000000
        );

        renderingSession.renderIntoFbo(
                renderableScene,
                renderContext
        );
    }
}
