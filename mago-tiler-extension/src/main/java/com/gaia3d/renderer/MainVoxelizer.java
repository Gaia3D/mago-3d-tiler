package com.gaia3d.renderer;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.exchangable.SceneInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.modifier.halfedge.HalfEdgeDecimator;
import com.gaia3d.basic.geometry.modifier.topology.*;
import com.gaia3d.basic.geometry.modifier.transform.GaiaBaker;
import com.gaia3d.basic.geometry.octree.OctreeBBoxInfo;
import com.gaia3d.basic.geometry.voxel.VoxelGrid3D;
import com.gaia3d.basic.geometry.voxel.VoxelizeParameters;
import com.gaia3d.basic.halfedge.*;
import com.gaia3d.basic.marchingcube.MarchingCube;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.remesher.*;
import com.gaia3d.basic.remesher.information.GaiaStatistics;
import com.gaia3d.basic.texture.atlas.TextureAtlasManager;
import com.gaia3d.basic.texture.atlas.TexturesAtlasData;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.renderer.engine.*;
import com.gaia3d.renderer.engine.Window;
import com.gaia3d.renderer.engine.dataStructure.*;
import com.gaia3d.renderer.engine.fbo.Fbo;
import com.gaia3d.renderer.engine.fbo.FboManager;
import com.gaia3d.renderer.engine.graph.ShaderManager;
import com.gaia3d.renderer.engine.graph.ShaderProgram;
import com.gaia3d.renderer.engine.scene.Camera;
import com.gaia3d.renderer.engine.scene.Projection;
import com.gaia3d.renderer.renderable.RenderableGaiaScene;
import com.gaia3d.util.GaiaTextureUtils;
import com.gaia3d.util.ImageUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.*;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

@Slf4j
@Getter
@Setter

public class MainVoxelizer implements IAppLogic {
    private Engine engine = new Engine("MagoVisual3D", new Window.WindowOptions(), this);

    @Override
    public void cleanup() {
    }

    @Override
    public void init(Window window, GaiaScenesContainer gaiaScenesContainer) {
    }

    @Override
    public void input(Window window, GaiaScenesContainer gaiaScenesContainer, long diffTimeMillis) {
    }

    @Override
    public void update(Window window, GaiaScenesContainer gaiaScenesContainer, long diffTimeMillis) {
    }

    public void deleteObjects() {
        engine.deleteObjects();
    }

    public void voxelize(List<GaiaScene> scenes, List<VoxelGrid3D> resultVoxelGrids, List<GaiaScene> resultGaiaScenes, VoxelizeParameters voxelizeParameters) {
        // render the scene
        log.info("Rendering the scene...getDepthRender");

        // Must init gl
        try {
            engine.init();
        } catch (Exception e) {
            log.error("[ERROR] initializing the engine: ", e);
        }

        // calculate the bbox of all scenes
        GaiaBoundingBox bboxAllScenes = new GaiaBoundingBox();
        for (GaiaScene scene : scenes) {
            bboxAllScenes.addBoundingBox(scene.updateBoundingBox());
        }

        GaiaScenesContainer gaiaScenesContainer = this.engine.getGaiaScenesContainer();

        // calculate the projectionMatrix for the camera
        Vector3d bboxCenter = bboxAllScenes.getCenter();
        float xLength = (float) bboxAllScenes.getSizeX();
        float yLength = (float) bboxAllScenes.getSizeY();
        float zLength = (float) bboxAllScenes.getSizeZ();

        double voxelsForMeter = voxelizeParameters.getVoxelsForMeter();
        int gridsCountX = (int) Math.ceil(voxelsForMeter * xLength);
        int gridsCountY = (int) Math.ceil(voxelsForMeter * yLength);
        int gridsCountZ = (int) Math.ceil(voxelsForMeter * zLength);

        log.info("voxelGrid3D : gridsCountX = {}, gridsCountY = {}, gridsCountZ = {}", gridsCountX, gridsCountY, gridsCountZ);

        Projection projection = new Projection(0, 1000, 1000);
        projection.setProjectionOrthographic(-xLength / 2.0f, xLength / 2.0f, -yLength / 2.0f, yLength / 2.0f, -zLength * 0.5f, zLength * 0.5f);
        gaiaScenesContainer.setProjection(projection);
        engine.setGaiaScenesContainer(gaiaScenesContainer);

        // Take FboManager from engine
        List<RenderableGaiaScene> renderableGaiaScenes = new ArrayList<>();
        int scenesCount = scenes.size();
        for (int i = 0; i < scenesCount; i++) {
            GaiaScene scene = scenes.get(i);
            RenderableGaiaScene renderableScene = InternDataConverter.getRenderableGaiaScene(scene);
            renderableGaiaScenes.add(renderableScene);
        }

        gaiaScenesContainer.setRenderableGaiaScenes(renderableGaiaScenes);

        // Create the voxel grid
        VoxelGrid3D voxelGrid3D = new VoxelGrid3D(gridsCountX, gridsCountY, gridsCountZ, bboxAllScenes.clone());

        // Voxelizing XY plane
        log.info("starting voxelizing XY...");
        this.voxelizeXY(gridsCountX, gridsCountY, gridsCountZ, bboxAllScenes, voxelGrid3D);
        log.info("starting voxelizing XZ...");
        this.voxelizeXZ(gridsCountX, gridsCountY, gridsCountZ, bboxAllScenes, voxelGrid3D);
        log.info("starting voxelizing YZ...");
        this.voxelizeYZ(gridsCountX, gridsCountY, gridsCountZ, bboxAllScenes, voxelGrid3D);

        // make gaiaPrimitive by marchingCubes
        GaiaScene originalScene = scenes.get(0); // take the first scene as original scene

        voxelGrid3D.expand(1); // expand the voxel grid to avoid the artifacts
        float isoValue = 0.01f; // original
        isoValue = 0.8f; // for DC_Library scale 0.01 settings
        GaiaScene gaiaScene = MarchingCube.makeGaiaScene(voxelGrid3D, isoValue);
        log.info("MarchingCube process finished.");
        GaiaAttribute gaiaAttribute = new GaiaAttribute();
        gaiaScene.setAttribute(gaiaAttribute);
        gaiaScene.setOriginalPath(originalScene.getOriginalPath());

        // now, make textures by oblique camera
        HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(gaiaScene);
        GaiaBoundingBox bboxHedgeScene = halfEdgeScene.getBoundingBox();

        List<HalfEdgeSurface> halfEdgeSurfaces = halfEdgeScene.extractSurfaces(null);
        for (HalfEdgeSurface halfEdgeSurface : halfEdgeSurfaces) {
            for (HalfEdgeFace face : halfEdgeSurface.getFaces()) {
                face.setClassifyId(0);
            }
        }

        int bufferImageType = BufferedImage.TYPE_INT_ARGB;
        double texturePixelsForMeter = voxelizeParameters.getTexturePixelsForMeter();
        engine.makeBoxTexturesByObliqueCamera(halfEdgeScene, texturePixelsForMeter, bufferImageType);
        GaiaScene gaiaSceneWithTextures = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);

        // delete the gaiaScene to free the memory
        gaiaScene.clear();

        // now transfer the bufferedImage to gaiaSceneMaterial
        List<GaiaMaterial> halfEdgeMaterials = halfEdgeScene.getMaterials();
        List<GaiaMaterial> gaiaMaterials = gaiaSceneWithTextures.getMaterials();
        int materialsCount = halfEdgeMaterials.size();
        for (int i = 0; i < materialsCount; i++) {
            GaiaMaterial halfEdgeMaterial = halfEdgeMaterials.get(i);
            Map<TextureType, List<GaiaTexture>> textures = halfEdgeMaterial.getTextures();
            TextureType textureTypeDiffuse = TextureType.DIFFUSE;
            List<GaiaTexture> texturesDiffuse = textures.get(textureTypeDiffuse);
            int texturesDiffuseCount = texturesDiffuse.size();
            for (int j = 0; j < texturesDiffuseCount; j++) {
                GaiaTexture texture = texturesDiffuse.get(j);
                BufferedImage textureImage = texture.getBufferedImage();
                if (textureImage != null) {
                    GaiaMaterial gaiaMaterial = gaiaMaterials.get(i);
                    Map<TextureType, List<GaiaTexture>> gaiaTextures = gaiaMaterial.getTextures();
                    List<GaiaTexture> gaiaTexturesDiffuse = gaiaTextures.get(textureTypeDiffuse);
                    GaiaTexture gaiaTexture = gaiaTexturesDiffuse.get(j);

                    gaiaTexture.setBufferedImage(textureImage);
                    texture.setBufferedImage(null);
                }
            }
        }

        halfEdgeScene.deleteObjects();
        resultGaiaScenes.add(gaiaSceneWithTextures);

        // return gl default values
        glEnable(GL_CULL_FACE);
    }

    private void voxelizeYZ(int gridsCountX, int gridsCountY, int gridsCountZ, GaiaBoundingBox bboxAllScenes, VoxelGrid3D voxelGrid3D) {
        // Voxelizing YZ plane
        Vector3d bboxCenter = bboxAllScenes.getCenter();
        float xLength = (float) bboxAllScenes.getSizeX();
        float yLength = (float) bboxAllScenes.getSizeY();
        float zLength = (float) bboxAllScenes.getSizeZ();

        FboManager fboManager = engine.getFboManager();
        Fbo colorFbo = fboManager.getOrCreateFbo("colorRender", gridsCountY, gridsCountZ);

        // create the fbo
        int fboWidth = gridsCountY;
        int fboHeight = gridsCountZ;

        fboWidth = Math.max(fboWidth, 1);
        fboHeight = Math.max(fboHeight, 1);

        int[] width = new int[1];
        int[] height = new int[1];
        width[0] = colorFbo.getFboWidth();
        height[0] = colorFbo.getFboHeight();

        glViewport(0, 0, width[0], height[0]);
        ShaderManager shaderManager = engine.getShaderManager();
        ShaderProgram sceneShaderProgram = shaderManager.getShaderProgram("scene");
        GaiaScenesContainer gaiaScenesContainer = this.engine.getGaiaScenesContainer();
        Projection projection = new Projection(0, 1000, 1000);
        float zRange = xLength / gridsCountX;
        projection.setProjectionOrthographic(-yLength / 2.0f, yLength / 2.0f, -zLength / 2.0f, zLength / 2.0f, -zRange * 0.5f, zRange * 0.5f);
        gaiaScenesContainer.setProjection(projection);
        // now set camera position
        Camera camera = new Camera();
        camera.setPosition(bboxCenter);
        camera.setDirection(new Vector3d(1, 0, 0));
        camera.setUp(new Vector3d(0, 0, 1));
        gaiaScenesContainer.setCamera(camera);
        for (int i = 0; i < gridsCountX; i++) {
            // set the camera position
            Vector3d cameraPosition = new Vector3d(bboxCenter);
            cameraPosition.add(-xLength * 0.5f + i * zRange + zRange * 0.5f, 0, 0); // The last one is the center of the voxel
            camera.setPosition(cameraPosition);

            colorFbo.bind();

            // render the scene
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // enable depth test
            glEnable(GL_DEPTH_TEST);

            // disable cull face
            glDisable(GL_CULL_FACE);

            engine.getRenderer().setColorMode(2); // set colorMode to 0 = textureColor
            engine.getRenderSceneImage(sceneShaderProgram);
            byte[] bufferArray = colorFbo.getBytesArray(GL_RGBA);

            colorFbo.unbind();

            voxelGrid3D.setVoxelsByAlphaYZ(i, bufferArray);
        }
    }

    private void voxelizeXZ(int gridsCountX, int gridsCountY, int gridsCountZ, GaiaBoundingBox bboxAllScenes, VoxelGrid3D voxelGrid3D) {
        // Voxelizing XZ plane
        Vector3d bboxCenter = bboxAllScenes.getCenter();
        float xLength = (float) bboxAllScenes.getSizeX();
        float yLength = (float) bboxAllScenes.getSizeY();
        float zLength = (float) bboxAllScenes.getSizeZ();

        FboManager fboManager = engine.getFboManager();
        Fbo colorFbo = fboManager.getOrCreateFbo("colorRender", gridsCountX, gridsCountZ);

        // create the fbo
        int fboWidth = gridsCountX;
        int fboHeight = gridsCountZ;

        fboWidth = Math.max(fboWidth, 1);
        fboHeight = Math.max(fboHeight, 1);

        int[] width = new int[1];
        int[] height = new int[1];
        width[0] = colorFbo.getFboWidth();
        height[0] = colorFbo.getFboHeight();

        glViewport(0, 0, width[0], height[0]);

        ShaderManager shaderManager = engine.getShaderManager();
        ShaderProgram sceneShaderProgram = shaderManager.getShaderProgram("scene");

        GaiaScenesContainer gaiaScenesContainer = this.engine.getGaiaScenesContainer();

        Projection projection = new Projection(0, 1000, 1000);
        float zRange = yLength / gridsCountY;
        projection.setProjectionOrthographic(-xLength / 2.0f, xLength / 2.0f, -zLength / 2.0f, zLength / 2.0f, -zRange * 0.5f, zRange * 0.5f);
        gaiaScenesContainer.setProjection(projection);
        // now set camera position
        Camera camera = new Camera();
        camera.setPosition(bboxCenter);
        camera.setDirection(new Vector3d(0, 1, 0));
        camera.setUp(new Vector3d(0, 0, 1));
        gaiaScenesContainer.setCamera(camera);

        for (int i = 0; i < gridsCountY; i++) {
            // set the camera position
            Vector3d cameraPosition = new Vector3d(bboxCenter);
            cameraPosition.add(0, -yLength * 0.5f + i * zRange + zRange * 0.5f, 0);
            camera.setPosition(cameraPosition);

            colorFbo.bind();

            // render the scene
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // enable depth test
            glEnable(GL_DEPTH_TEST);

            // disable cull face
            glDisable(GL_CULL_FACE);

            engine.getRenderer().setColorMode(2); // set colorMode to 0 = textureColor
            engine.getRenderSceneImage(sceneShaderProgram);

            byte[] bufferArray = colorFbo.getBytesArray(GL_RGBA);

            colorFbo.unbind();

            voxelGrid3D.setVoxelsByAlphaXZ(i, bufferArray);
        }

    }

    private void voxelizeXY(int gridsCountX, int gridsCountY, int gridsCountZ, GaiaBoundingBox bboxAllScenes, VoxelGrid3D voxelGrid3D) {
        // Voxelizing XY plane
        Vector3d bboxCenter = bboxAllScenes.getCenter();
        float xLength = (float) bboxAllScenes.getSizeX();
        float yLength = (float) bboxAllScenes.getSizeY();
        float zLength = (float) bboxAllScenes.getSizeZ();

        FboManager fboManager = engine.getFboManager();
        Fbo colorFbo = fboManager.getOrCreateFbo("colorRender", gridsCountX, gridsCountY);

        // create the fbo
        int fboWidth = gridsCountX;
        int fboHeight = gridsCountY;

        fboWidth = Math.max(fboWidth, 1);
        fboHeight = Math.max(fboHeight, 1);

        int[] width = new int[1];
        int[] height = new int[1];
        width[0] = colorFbo.getFboWidth();
        height[0] = colorFbo.getFboHeight();

        glViewport(0, 0, width[0], height[0]);
        ShaderManager shaderManager = engine.getShaderManager();
        ShaderProgram sceneShaderProgram = shaderManager.getShaderProgram("scene");

        GaiaScenesContainer gaiaScenesContainer = this.engine.getGaiaScenesContainer();

        Projection projection = new Projection(0, 1000, 1000);
        float zRange = zLength / gridsCountZ;
        projection.setProjectionOrthographic(-xLength / 2.0f, xLength / 2.0f, -yLength / 2.0f, yLength / 2.0f, -zRange * 0.5f, zRange * 0.5f);
        gaiaScenesContainer.setProjection(projection);

        // now set camera position
        Camera camera = new Camera();
        camera.setPosition(bboxCenter);
        camera.setDirection(new Vector3d(0, 0, -1));
        camera.setUp(new Vector3d(0, 1, 0));
        gaiaScenesContainer.setCamera(camera);

        for (int i = 0; i < gridsCountZ; i++) {
            // set the camera position
            Vector3d cameraPosition = new Vector3d(bboxCenter);
            cameraPosition.add(0, 0, -zLength * 0.5f + i * zRange + zRange * 0.5f); // The last one is the center of the voxel
            camera.setPosition(cameraPosition);

            colorFbo.bind();

            // render the scene
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // enable depth test
            glEnable(GL_DEPTH_TEST);

            // disable cull face
            glDisable(GL_CULL_FACE);

            engine.getRenderer().setColorMode(2); // set colorMode to 0 = textureColor
            engine.getRenderSceneImage(sceneShaderProgram);

            byte[] bufferArray = colorFbo.getBytesArray(GL_RGBA);

            colorFbo.unbind();

            voxelGrid3D.setVoxelsByAlphaXY(i, bufferArray);
        }
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
        // Must init gl
        try {
            engine.init();

            int screenWidth = 1000; // no used var
            int screenHeight = 600; // no used var

            GaiaScenesContainer gaiaScenesContainer = new GaiaScenesContainer(screenWidth, screenHeight);

            // calculate the projectionMatrix for the camera
            Vector3d bboxCenter = nodeBBox.getCenter();
            float xLength = (float) nodeBBox.getSizeX();
            float yLength = (float) nodeBBox.getSizeY();
            float zLength = (float) nodeBBox.getSizeZ();

            Projection projection = new Projection(0, screenWidth, screenHeight);
            projection.setProjectionOrthographic(-xLength / 2.0f, xLength / 2.0f, -yLength / 2.0f, yLength / 2.0f, -zLength * 0.5f, zLength * 0.5f);
            gaiaScenesContainer.setProjection(projection);
            engine.setGaiaScenesContainer(gaiaScenesContainer);

            // Take FboManager from engine
            FboManager fboManager = engine.getFboManager();

            // create the fbo
            int fboWidthColor = maxScreenSize;
            int fboHeightColor = maxScreenSize;
            if (xLength > yLength) {
                fboWidthColor = maxScreenSize;
                fboHeightColor = (int) (maxScreenSize * yLength / xLength);
            } else {
                fboWidthColor = (int) (maxScreenSize * xLength / yLength);
                fboHeightColor = maxScreenSize;
            }

            // now set camera position
            Camera camera = new Camera();
            camera.setPosition(bboxCenter);
            camera.setDirection(new Vector3d(0, 0, -1));
            camera.setUp(new Vector3d(0, 1, 0));
            gaiaScenesContainer.setCamera(camera);

            Matrix4d nodeMatrixInv = new Matrix4d(nodeTMatrix);
            nodeMatrixInv.invert();

            GaiaScene gaiaSceneMaster = null;
            double weldError = 1e-6; // 1e-6 is a good value for remeshing

            // IntegralReMeshParameters
            Vector4f backgroundColor = new Vector4f(1.0f, 0.0f, 1.0f, 1.0f);
            IntegralReMeshParameters integralReMeshParameters = new IntegralReMeshParameters();
            integralReMeshParameters.setBackgroundColor(backgroundColor);
            integralReMeshParameters.createFBOsObliqueCamera9Directions(this.engine.getFboManager(), fboWidthColor, fboHeightColor, GL30.GL_LINEAR, GL30.GL_LINEAR);

            // render the scenes
            int scenesCount = sceneInfos.size();
            List<RenderableGaiaScene> renderableGaiaScenes = new ArrayList<>();
            int counter = 0;
            int faceIdAvailable = 0;

            Map<Integer, Map<GaiaFace, HalfEdgeFace>> mapClassifyIdToGaiaFaceToHalfEdgeFace = new HashMap<>();
            Map<Integer, Map<GaiaFace, CameraDirectionTypeInfo>> mapClassifyIdToGaiaFaceToCameraDirectionTypeInfo = new HashMap<>();
            Map<Integer, Map<CameraDirectionType, GaiaBoundingBox>> mapClassificationCamDirTypeBBox = new HashMap<>();
            Map<Integer, Map<CameraDirectionType, Matrix4d>> mapClassificationCamDirTypeModelViewMatrix = new HashMap<>();
            Map<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> mapClassificationCamDirTypeFacesList = new HashMap<>();

            GaiaSceneCleaner cleaner = new GaiaSceneCleaner();
            GaiaWeldOptions weldOptions = GaiaWeldOptions.builder()
                    .error(weldError)
                    .checkTexCoord(false)
                    .checkNormal(false)
                    .checkColor(false)
                    .checkBatchId(false)
                    .build();
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

                renderableGaiaScenes.clear();

                // load the set file
                GaiaSet gaiaSet = null;
                GaiaScene gaiaScene = null;
                GaiaScene gaiaSceneCopy = null;
                Path path = Paths.get(scenePath);
                try {
                    gaiaSet = GaiaSet.readFile(path);
                    gaiaScene = new GaiaScene(gaiaSet);
                    gaiaSceneCopy = new GaiaScene(gaiaSet);

                    GaiaNode gaiaNode = gaiaSceneCopy.getNodes().getFirst();
                    gaiaNode.setTransformMatrix(new Matrix4d(sceneTMatLC));
                    gaiaNode.setPreMultipliedTransformMatrix(new Matrix4d(sceneTMatLC));
                    RenderableGaiaScene renderableScene = InternDataConverter.getRenderableGaiaScene(gaiaSceneCopy);
                    renderableGaiaScenes.add(renderableScene);
                } catch (Exception e) {
                    log.error("[ERROR] reading the file: ", e);
                }

                if (gaiaScene == null) {
                    // throw error
                    throw new RuntimeException("[ERROR] integralReMeshByObliqueCamera : GaiaScene is null");
                }

                if (gaiaSceneCopy != null) {
                    gaiaSceneCopy.clear();
                    gaiaSceneCopy = null;
                }

                gaiaScenesContainer.setRenderableGaiaScenes(renderableGaiaScenes);

                // decimate the scene.****************************************************************************************
                GaiaTriangulator triangulator = new GaiaTriangulator();
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

                // dominantPlaneProjector.*****************
//                double positionEpsilon = 1e-4;
//                double maxNormalAngleDeg = 15.0;
//                int minFacesPerCluster = 10;
//                DominantPlaneProjector dpp = new DominantPlaneProjector();
//                dpp.projectClustersOnScene_SimpleTest(
//                        gaiaScene,
//                        1e-6,  // positionEpsilon
//                        35.0,  // maxNormalAngleDeg
//                        15,    // minFacesPerCluster
//                        true   // true = proyectar al plano, false = mover por normal
//                );
                // End domimantPlaneProjector------------------


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
                double minBoxSize = nodeBoxSize / 18.0;
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

                    gaiaScene = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);

                    int bufferedImageType = BufferedImage.TYPE_INT_ARGB;
                    int texturePixelsForMeter = 20; // decimateParameters.getTexturePixelsForMeter();
                    engine.makeIntegralBoxTexturesByObliqueCamera9Directions(halfEdgeScene, texturePixelsForMeter, bufferedImageType, nodeBBox, integralReMeshParameters,
                            mapClassifyIdToGaiaFaceToHalfEdgeFace, mapClassifyIdToGaiaFaceToCameraDirectionTypeInfo, mapClassificationCamDirTypeBBox,
                            mapClassificationCamDirTypeModelViewMatrix, mapClassificationCamDirTypeFacesList);
                    // end of making oblique camera textures

                } catch (Exception e) {
                    log.error("[ERROR] initializing the engine: ", e);
                }

                // delete renderableGaiaScenes
                for (RenderableGaiaScene renderableScene : renderableGaiaScenes) {
                    renderableScene.deleteGLBuffers();
                }

                // Calculate globalBoundaryAnchors.*********************************************************************

                calculateGlobalBoundaryAnchors(gaiaScene, reMeshParams, scenePositionRelToCellGrid, i);
                // End calculating globalBoundaryAnchors.---------------------------------------------------------------

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

            // Finish LOD2 -> LOD3 transition anchors.***************************************
            if (lod == 2 && reMeshParams != null) {
                TileBoundaryAnchors lodTransitionTileAnchors =
                        reMeshParams.getTileBoundaryAnchors();

                GlobalBoundaryAnchors globalBoundaryAnchors =
                        reMeshParams.getGlobalBoundaryAnchors();

                if (lodTransitionTileAnchors != null && globalBoundaryAnchors != null) {
                    ReMesherVertexClusterV2.finishTileBoundaryAnchors(lodTransitionTileAnchors);

                    globalBoundaryAnchors.addMissingFromTileAnchors(lodTransitionTileAnchors);

                    log.debug("LOD2 -> LOD3 transition tile anchors cells = {}",
                            lodTransitionTileAnchors.frontierAveragePositions.size());

                    log.debug("LOD2 -> LOD3 globalBoundaryAnchors locked cells = {}",
                            globalBoundaryAnchors.lockedAveragePositions.size());

                    // Importante:
                    // TileBoundaryAnchors es temporal. Los anchors útiles ya quedaron bloqueados en global.
                    lodTransitionTileAnchors.clear();
                }
            }
            // End LOD2 -> LOD3 transition anchors.------------------------------------------

            // Join all surfaces and weld vertices of the gaiaSceneMaster.
            gaiaSceneMaster.joinAllSurfaces();
            GaiaWelder weld = new GaiaWelder(weldOptions);
            weld.apply(gaiaSceneMaster);
            cleaner.apply(gaiaSceneMaster);

//            // Make horizontal skirt.************************************************************************************
//            GaiaHorizontalSkirtMaker horizontalSkirtMaker = new GaiaHorizontalSkirtMaker();
//            double maxNodeBBoxSize = nodeBBox.getMaxSize();
//            horizontalSkirtMaker.addHorizontalSkirtsToScene(gaiaSceneMaster, nodeBBox, 0.1f, maxNodeBBoxSize*0.005);
//            // End making horizontal skirt.-------------------------------------------------------------------------------

            // Make frontier expansion.************************************************************************************
            GaiaFrontierExpander frontierExpander = new GaiaFrontierExpander();
            double maxNodeBBoxSize = nodeBBox.getMaxSize();
            frontierExpander.expandFrontiersToScene(gaiaSceneMaster, nodeBBox, 0.2, maxNodeBBoxSize * 0.003);
            // End making frontier expansion.------------------------------------------------------------------------------

            GaiaExtractor extractor = new GaiaExtractor();
            List<GaiaFace> gaiaFacesMaster = extractor.extractAllFaces(gaiaSceneMaster);
            if (gaiaFacesMaster.isEmpty()) {
                log.info("[ERROR] gaiaFacesMaster is empty");
                engine.deleteObjects();
                for (RenderableGaiaScene renderableScene : renderableGaiaScenes) {
                    renderableScene.deleteGLBuffers();
                }

                engine.deleteObjects();
                engine.getGaiaScenesContainer().deleteObjects();
                integralReMeshParameters.deleteFBOs(fboManager);
                return;
            }

            HalfEdgeScene halfEdgeSceneMaster = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(gaiaSceneMaster);

            // Here scissor the atlas textures.
            atlasTextureForIntegralReMesh9Directions(integralReMeshParameters, halfEdgeSceneMaster, mapClassifyIdToGaiaFaceToHalfEdgeFace,
                    mapClassifyIdToGaiaFaceToCameraDirectionTypeInfo, mapClassificationCamDirTypeBBox,
                    mapClassificationCamDirTypeModelViewMatrix, mapClassificationCamDirTypeFacesList,
                    outputPathString, nodeName);
            // end of atlas texture*************************************************************************************

            resultHalfEdgeScenes.add(halfEdgeSceneMaster);

            // delete renderableGaiaScenes
            engine.deleteObjects();
            for (RenderableGaiaScene renderableScene : renderableGaiaScenes) {
                renderableScene.deleteGLBuffers();
            }

            engine.deleteObjects();
            engine.getGaiaScenesContainer().deleteObjects();
            integralReMeshParameters.deleteFBOs(fboManager);
        } catch (Exception e) {
            log.error("[ERROR] initializing the engine: ", e);
        }
    }

    public void integralLeafScene(List<SceneInfo> sceneInfos,
                                  List<GaiaScene> resultGaiaScenes,
                                                GaiaBoundingBox nodeBBox,
                                                Matrix4d nodeTMatrix,
                                                int maxScreenSize,
                                                String outputPathString,
                                                String nodeName,
                                                int lod) {
        try {
            Matrix4d nodeMatrixInv = new Matrix4d(nodeTMatrix);
            nodeMatrixInv.invert();

            double weldError = 1e-6; // 1e-6 is a good value for remeshing

            // render the scenes
            int scenesCount = sceneInfos.size();
//            if(scenesCount == 1){
//
//            }

            GaiaSceneCleaner cleaner = new GaiaSceneCleaner();
            GaiaWeldOptions weldOptions = GaiaWeldOptions.builder()
                    .error(weldError)
                    .checkTexCoord(false)
                    .checkNormal(false)
                    .checkColor(false)
                    .checkBatchId(false)
                    .build();

            List<HalfEdgeScene> halfEdgeScenes = new ArrayList<>();
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

                Path path = Paths.get(scenePath);
                try {
                    gaiaSet = GaiaSet.readFile(path);
                    gaiaScene = new GaiaScene(gaiaSet);
                } catch (Exception e) {
                    log.error("[ERROR] reading the file: ", e);
                }

                if (gaiaScene == null) {
                    // throw error
                    throw new RuntimeException("[ERROR] integralReMeshByObliqueCamera : GaiaScene is null");
                }

                GaiaTriangulator triangulator = new GaiaTriangulator();
                triangulator.apply(gaiaScene);

                GaiaNode gaiaNode = gaiaScene.getNodes().getFirst();
                gaiaNode.setTransformMatrix(new Matrix4d(sceneTMatLC));
                gaiaNode.setPreMultipliedTransformMatrix(new Matrix4d(sceneTMatLC));

                GaiaBaker baker = new GaiaBaker();
                baker.apply(gaiaScene);

                HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(gaiaScene);
                halfEdgeScenes.add(halfEdgeScene);

                gaiaSet.clear();
                gaiaScene.clear();
            }

            atlasTextureForIntegralLeafScenes(halfEdgeScenes, resultGaiaScenes, outputPathString, nodeName);

            // delete halfEdgeScenes.
            for(HalfEdgeScene scene : halfEdgeScenes){
                scene.deleteObjects();
            }
        } catch (Exception e) {
            log.error("[ERROR] initializing the engine: ", e);
        }
    }

    private void calculateGlobalBoundaryAnchors(
            GaiaScene gaiaScene,
            ReMeshParameters reMeshParams,
            Vector3d scenePositionRelToCellGrid,
            int sceneId) {

        if (gaiaScene == null || reMeshParams == null || scenePositionRelToCellGrid == null) {
            return;
        }

        GlobalBoundaryAnchors globalBoundaryAnchors =
                reMeshParams.getGlobalBoundaryAnchors();

        if (globalBoundaryAnchors == null) {
            globalBoundaryAnchors = new GlobalBoundaryAnchors();
            reMeshParams.setGlobalBoundaryAnchors(globalBoundaryAnchors);
        }

        TileBoundaryAnchors lodTransitionTileAnchors =
                reMeshParams.getTileBoundaryAnchors();

        if (lodTransitionTileAnchors == null) {
            lodTransitionTileAnchors = new TileBoundaryAnchors();
            reMeshParams.setTileBoundaryAnchors(lodTransitionTileAnchors);
        }

        Vector3d scenePosRelToCellGridNegative = new Vector3d(
                -scenePositionRelToCellGrid.x,
                -scenePositionRelToCellGrid.y,
                -scenePositionRelToCellGrid.z
        );

        try {
            translateScene(gaiaScene, scenePositionRelToCellGrid);

            ReMesherVertexClusterV2.accumulateTileBoundaryAnchorsFromScene(
                    gaiaScene,
                    reMeshParams,                 // debe tener CellGrid3D de LOD3
                    lodTransitionTileAnchors,
                    globalBoundaryAnchors,
                    sceneId
            );
        } finally {
            translateScene(gaiaScene, scenePosRelToCellGridNegative);
        }
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
        // Must init gl
        try {
            engine.init();
        } catch (Exception e) {
            log.error("[ERROR] initializing the engine: ", e);
        }

        int screenWidth = 1000; // no used var
        int screenHeight = 600; // no used var

        GaiaScenesContainer gaiaScenesContainer = new GaiaScenesContainer(screenWidth, screenHeight);

        // calculate the projectionMatrix for the camera
        Vector3d bboxCenter = nodeBBox.getCenter();
        float xLength = (float) nodeBBox.getSizeX();
        float yLength = (float) nodeBBox.getSizeY();
        float zLength = (float) nodeBBox.getSizeZ();

        Projection projection = new Projection(0, screenWidth, screenHeight);
        projection.setProjectionOrthographic(-xLength / 2.0f, xLength / 2.0f, -yLength / 2.0f, yLength / 2.0f, -zLength * 0.5f, zLength * 0.5f);
        gaiaScenesContainer.setProjection(projection);
        engine.setGaiaScenesContainer(gaiaScenesContainer);

        // Take FboManager from engine
        FboManager fboManager = engine.getFboManager();

        // create the fbo
        int fboWidthColor = maxScreenSize;
        int fboHeightColor = maxScreenSize;
        if (xLength > yLength) {
            fboWidthColor = maxScreenSize;
            fboHeightColor = (int) (maxScreenSize * yLength / xLength);
        } else {
            fboWidthColor = (int) (maxScreenSize * xLength / yLength);
            fboHeightColor = maxScreenSize;
        }

        //Fbo colorFbo = fboManager.getOrCreateFbo("colorRender", fboWidthColor, fboHeightColor);

        // now set camera position
        Camera camera = new Camera();
        camera.setPosition(bboxCenter);
        camera.setDirection(new Vector3d(0, 0, -1));
        camera.setUp(new Vector3d(0, 1, 0));
        gaiaScenesContainer.setCamera(camera);

        Matrix4d nodeMatrixInv = new Matrix4d(nodeTMatrix);
        nodeMatrixInv.invert();

        Map<Vector3i, List<GaiaVertex>> vertexClusters = new HashMap<>();
        GaiaScene gaiaSceneMaster = null;
        double weldError = 1e-5; // 1e-6 is a good value for remeshing

        // IntegralReMeshParameters
        Vector4f backgroundColor = new Vector4f(1.0f, 0.0f, 1.0f, 1.0f);
        IntegralReMeshParameters integralReMeshParameters = new IntegralReMeshParameters();
        integralReMeshParameters.setBackgroundColor(backgroundColor);
        integralReMeshParameters.createFBOsObliqueCamera9Directions(this.engine.getFboManager(), fboWidthColor, fboHeightColor, GL_LINEAR, GL_LINEAR);

        /*
        // IntegralReMeshParameters
            Vector4f backgroundColor = new Vector4f(0.5f, 0.5f, 0.5f, 1.0f);
            IntegralReMeshParameters integralReMeshParameters = new IntegralReMeshParameters();
            integralReMeshParameters.setBackgroundColor(backgroundColor);
            integralReMeshParameters.createFBOsObliqueCamera9Directions(this.engine.getFboManager(), fboWidthColor, fboHeightColor, GL30.GL_LINEAR, GL30.GL_LINEAR);
         */

        // render the scenes
        int scenesCount = sceneInfos.size();
        List<RenderableGaiaScene> renderableGaiaScenes = new ArrayList<>();
        int counter = 0;
        int faceIdAvailable = 0;

        Map<Integer, Map<GaiaFace, HalfEdgeFace>> mapClassifyIdToGaiaFaceToHalfEdgeFace = new HashMap<>();
        Map<Integer, Map<GaiaFace, CameraDirectionTypeInfo>> mapClassifyIdToGaiaFaceToCameraDirectionTypeInfo = new HashMap<>();
        Map<Integer, Map<CameraDirectionType, GaiaBoundingBox>> mapClassificationCamDirTypeBBox = new HashMap<>();
        Map<Integer, Map<CameraDirectionType, Matrix4d>> mapClassificationCamDirTypeModelViewMatrix = new HashMap<>();
        Map<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> mapClassificationCamDirTypeFacesList = new HashMap<>();

        Vector3i nodeMinCellIndex = new Vector3i(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        Vector3i nodeMaxCellIndex = new Vector3i(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);

        // PASADA 1: acumular fronteras de todos los meshes del tile
        TileBoundaryAnchors tileBoundaryAnchors = reMeshParams.getTileBoundaryAnchors();
        if(tileBoundaryAnchors == null) {
            tileBoundaryAnchors =  new TileBoundaryAnchors();
            reMeshParams.setTileBoundaryAnchors(tileBoundaryAnchors);
        }else {
            tileBoundaryAnchors.clear();
        }

        GlobalBoundaryAnchors globalBoundaryAnchors = reMeshParams.getGlobalBoundaryAnchors();
        if(globalBoundaryAnchors == null) {
            globalBoundaryAnchors = new GlobalBoundaryAnchors();
            reMeshParams.setGlobalBoundaryAnchors(globalBoundaryAnchors);
        }

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

            renderableGaiaScenes.clear();

            // load the set file
            GaiaSet gaiaSet = null;
            GaiaScene gaiaScene = null;
            Path path = Paths.get(scenePath);
            try {
                gaiaSet = GaiaSet.readFile(path);
                gaiaScene = new GaiaScene(gaiaSet);
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

            GaiaTriangulator triangulator = new GaiaTriangulator();
            triangulator.apply(gaiaScene);
            GaiaBaker baker = new GaiaBaker();
            baker.apply(gaiaScene);
            gaiaScene.joinAllSurfaces();

            GaiaWeldOptions weldOptions = GaiaWeldOptions.builder()
                    .error(weldError)
                    .checkTexCoord(false)
                    .checkNormal(false)
                    .checkColor(false)
                    .checkBatchId(false)
                    .build();
            GaiaWelder weld = new GaiaWelder(weldOptions);
            weld.apply(gaiaScene);

            GaiaSceneCleaner cleaner = new GaiaSceneCleaner();
            cleaner.apply(gaiaScene);
            List<GaiaMaterial> materials = gaiaScene.getMaterials();

            translateScene(gaiaScene, scenePositionRelToCellGrid); // translate the scene to the cell grid position

            ReMesherVertexClusterV2.accumulateTileBoundaryAnchorsFromScene(
                    gaiaScene,
                    reMeshParams,
                    tileBoundaryAnchors,
                    globalBoundaryAnchors,
                    i
            );

            gaiaScene.clear();
            gaiaSet.clear();
        }

        ReMesherVertexClusterV2.finishTileBoundaryAnchors(tileBoundaryAnchors);

        // Bloquea esos anchors para tiles siguientes.
        globalBoundaryAnchors.addMissingFromTileAnchors(tileBoundaryAnchors);
        GaiaSceneCleaner cleaner = new GaiaSceneCleaner();
        GaiaWeldOptions weldOptions = GaiaWeldOptions.builder()
                .error(weldError)
                .checkTexCoord(false)
                .checkNormal(false)
                .checkColor(false)
                .checkBatchId(false)
                .build();
        GaiaWelder weld = new GaiaWelder(weldOptions);

        // PASADA 2: remeshear mesh por mesh
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

            renderableGaiaScenes.clear();

            // load the set file
            GaiaSet gaiaSet = null;
            GaiaScene gaiaScene = null;
            GaiaScene gaiaSceneCopy = null;
            Path path = Paths.get(scenePath);
            try {
                gaiaSet = GaiaSet.readFile(path);
                gaiaScene = new GaiaScene(gaiaSet);
                gaiaSceneCopy = new GaiaScene(gaiaSet);
                GaiaNode gaiaNode = gaiaSceneCopy.getNodes().get(0);
                gaiaNode.setTransformMatrix(new Matrix4d(sceneTMatLC));
                gaiaNode.setPreMultipliedTransformMatrix(new Matrix4d(sceneTMatLC));
                RenderableGaiaScene renderableScene = InternDataConverter.getRenderableGaiaScene(gaiaSceneCopy);
                renderableGaiaScenes.add(renderableScene);
            } catch (Exception e) {
                log.error("[ERROR] reading the file: ", e);
            }

            if (gaiaScene == null) {
                // throw error
                throw new RuntimeException("[ERROR] integralReMeshByObliqueCamera : GaiaScene is null");
            }

            gaiaScenesContainer.setRenderableGaiaScenes(renderableGaiaScenes);

            // reMesh the scene.****************************************************************************************
            // The "scenePositionRelToCellGrid" is the relative position of the scene respect the center of RootNode (Depth = 0). All scenes must be synchronized to the RootNode.
            Vector3d scenePositionRelToCellGrid = sceneInfo.getScenePosLC(); // relative position of the scene respect the center of RootNode (Depth = 0).
            Vector3d scenePosRelToCellGridNegative = new Vector3d(-scenePositionRelToCellGrid.x, -scenePositionRelToCellGrid.y, -scenePositionRelToCellGrid.z);
            GaiaTriangulator triangulator = new GaiaTriangulator();
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
            decimateParameters.setBasicValues(8.0, 0.001, 0.9, 40.0, 1000000, 5, 0.1);
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
            //vertexClusters.clear();
            translateScene(gaiaScene, scenePositionRelToCellGrid); // translate the scene to the cell grid position
            //WorldVertexClusters worldClusters = new WorldVertexClusters();
            //ReMesherVertexCluster.reMeshScene(gaiaScene, reMeshParams, worldClusters, sceneMinCellIndex, sceneMaxCellIndex);
            // new.*******************************************************

            ReMesherVertexClusterV2.reMeshScene(
                    gaiaScene,
                    reMeshParams,
                    tileBoundaryAnchors,
                    globalBoundaryAnchors,
                    sceneMinCellIndex,
                    sceneMaxCellIndex
            );
            // end new.-------------------------------------------------------------------------------
            //worldClusters.clearInteriorClusters();
            translateScene(gaiaScene, scenePosRelToCellGridNegative); // translate the scene back to the original position
            //vertexClusters.clear();

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
            decimateParameters.setBasicValues(8.0, 0.001, 0.0, 40.0, 1000000, 2, 0.1);
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
                HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(gaiaScene);
                int bufferedImageType = BufferedImage.TYPE_INT_ARGB;
//                engine.makeIntegralBoxTexturesByObliqueCamera(halfEdgeScene, reMeshParams.getTexturePixelsForMeter(), bufferedImageType, nodeBBox, integralReMeshParameters,
//                        mapClassifyIdToGaiaFaceToHalfEdgeFace, mapClassifyIdToGaiaFaceToCameraDirectionTypeInfo, mapClassificationCamDirTypeBBox,
//                        mapClassificationCamDirTypeModelViewMatrix, mapClassificationCamDirTypeFacesList, faceVisibilityDataManager);
                engine.makeIntegralBoxTexturesByObliqueCamera9Directions(halfEdgeScene, reMeshParams.getTexturePixelsForMeter(), bufferedImageType, nodeBBox, integralReMeshParameters,
                        mapClassifyIdToGaiaFaceToHalfEdgeFace, mapClassifyIdToGaiaFaceToCameraDirectionTypeInfo, mapClassificationCamDirTypeBBox,
                        mapClassificationCamDirTypeModelViewMatrix, mapClassificationCamDirTypeFacesList);
                // end of making oblique camera textures

            } catch (Exception e) {
                log.error("[ERROR] initializing the engine: ", e);
            }

            // delete renderableGaiaScenes
            for (RenderableGaiaScene renderableScene : renderableGaiaScenes) {
                renderableScene.deleteGLBuffers();
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

        // Join all surfaces and weld vertices of the gaiaSceneMaster.
        gaiaSceneMaster.joinAllSurfaces();
        weld.apply(gaiaSceneMaster);
        cleaner.apply(gaiaSceneMaster);

        // GaiaSkirtMaker.**********************************************************************************************
        double nodeBoxSizeX = nodeBBox.getSizeX();
        double nodeBoxSizeY = nodeBBox.getSizeY();
        double nodeBoxSizeZ = nodeBBox.getSizeZ();
        GaiaBoundingBox nodeBBoxCentered = new GaiaBoundingBox(-nodeBoxSizeX / 2.0, -nodeBoxSizeY / 2.0, -nodeBoxSizeZ / 2.0,
                nodeBoxSizeX / 2.0, nodeBoxSizeY / 2.0, nodeBoxSizeZ / 2.0
        );
        GaiaSkirtMaker skirtMaker = new GaiaSkirtMaker();
        double limitBoxSize = nodeBBox.getMaxSize() / 16.0;
        double tolerance = nodeBoxSizeX * 0.08;
        double skirtDepth = nodeBoxSizeX * 0.08;
        double maxSegmentLength = nodeBoxSizeX * 0.5;

        skirtMaker.addSkirtsToScene(
                gaiaSceneMaster,
                nodeBBoxCentered,
                tolerance,
                skirtDepth,
                maxSegmentLength
        );
        // end making skirt.--------------------------------------------------------------------------------------------

        gaiaSceneMaster.joinAllSurfaces();
        weld.apply(gaiaSceneMaster);
        cleaner.apply(gaiaSceneMaster);
        // end----------------------------------------------------------------------------------------------------------

        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaFace> gaiaFacesMaster = extractor.extractAllFaces(gaiaSceneMaster);
        if (gaiaFacesMaster.isEmpty()) {
            log.info("[ERROR] gaiaFacesMaster is empty");
            // delete renderableGaiaScenes
            engine.deleteObjects();
            for (RenderableGaiaScene renderableScene : renderableGaiaScenes) {
                renderableScene.deleteGLBuffers();
            }

            engine.deleteObjects();
            engine.getGaiaScenesContainer().deleteObjects();
            integralReMeshParameters.deleteFBOs(fboManager);
            return;
        }

        HalfEdgeScene halfEdgeSceneMaster = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(gaiaSceneMaster);

        // Here scissor the atlas textures.
        atlasTextureForIntegralReMesh9Directions(integralReMeshParameters, halfEdgeSceneMaster, mapClassifyIdToGaiaFaceToHalfEdgeFace,
                mapClassifyIdToGaiaFaceToCameraDirectionTypeInfo, mapClassificationCamDirTypeBBox,
                mapClassificationCamDirTypeModelViewMatrix, mapClassificationCamDirTypeFacesList,
                outputPathString, nodeName);
        // end of atlas texture*************************************************************************************

        //if (makeHorizontalSkirt) {
        //halfEdgeSceneMaster.makeHorizontalSkirt();
        //}

        resultHalfEdgeScenes.add(halfEdgeSceneMaster);

        // delete renderableGaiaScenes
        engine.deleteObjects();
        for (RenderableGaiaScene renderableScene : renderableGaiaScenes) {
            renderableScene.deleteGLBuffers();
        }

        engine.deleteObjects();
        engine.getGaiaScenesContainer().deleteObjects();
        integralReMeshParameters.deleteFBOs(fboManager);
    }

    private void atlasTextureForIntegralReMesh9Directions(IntegralReMeshParameters integralReMeshParameters, HalfEdgeScene halfEdgeSceneMaster,
                                                          Map<Integer, Map<GaiaFace, HalfEdgeFace>> mapClassifyIdToGaiaFaceToHalfEdgeFace,
                                                          Map<Integer, Map<GaiaFace, CameraDirectionTypeInfo>> mapClassifyIdToGaiaFaceToCameraDirectionTypeInfo,
                                                          Map<Integer, Map<CameraDirectionType, GaiaBoundingBox>> mapClassificationCamDirTypeBBox,
                                                          Map<Integer, Map<CameraDirectionType, Matrix4d>> mapClassificationCamDirTypeModelViewMatrix,
                                                          Map<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> mapClassificationCamDirTypeFacesList,
                                                          String outputPathString, String nodeName) {
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
        Map<String, Fbo> colorFboMap = integralReMeshParameters.getColorFboMap(); // original.***
        //Map<String, Fbo> colorFboMap = integralReMeshParameters.getColorCodeFboMap(); // test.***

        Vector4f backgroundColor = integralReMeshParameters.getBackgroundColor();
        Color backGroundColor = new Color(
                (int) (backgroundColor.x * 255),
                (int) (backgroundColor.y * 255),
                (int) (backgroundColor.z * 255)
        );

        // ZNEG
        Fbo fboZNeg = colorFboMap.get("ZNEG");
        fboZNeg.bind();
        BufferedImage imageZNeg = fboZNeg.getBufferedImage(bufferedImageType);
        fboZNeg.unbind();
        texAtlasManager.dilateBackgroundColor(imageZNeg, backGroundColor);
        if (imageZNeg != null) {
            com.gaia3d.basic.texture.atlas.TexturesAtlasData texturesAtlasDataYPosZNeg = new com.gaia3d.basic.texture.atlas.TexturesAtlasData();
            texturesAtlasDataYPosZNeg.setClassifyId(classificationId);
            texturesAtlasDataYPosZNeg.setCameraDirectionType(CameraDirectionType.ZNEG);
            texturesAtlasDataYPosZNeg.setTextureImage(imageZNeg);
            texturesAtlasDataList.add(texturesAtlasDataYPosZNeg);
        }

        // YPOS_ZNEG
        Fbo fboYPosZNeg = colorFboMap.get("YPOS_ZNEG");
        fboYPosZNeg.bind();
        BufferedImage imageYPosZNeg = fboYPosZNeg.getBufferedImage(bufferedImageType);
        fboYPosZNeg.unbind();
        texAtlasManager.dilateBackgroundColor(imageYPosZNeg, backGroundColor);
        if (imageYPosZNeg != null) {
            com.gaia3d.basic.texture.atlas.TexturesAtlasData texturesAtlasDataYPosZNeg = new com.gaia3d.basic.texture.atlas.TexturesAtlasData();
            texturesAtlasDataYPosZNeg.setClassifyId(classificationId);
            texturesAtlasDataYPosZNeg.setCameraDirectionType(CameraDirectionType.YPOS_ZNEG);
            texturesAtlasDataYPosZNeg.setTextureImage(imageYPosZNeg);
            texturesAtlasDataList.add(texturesAtlasDataYPosZNeg);
        }

        // YNEG_ZNEG
        Fbo fboYNegZNeg = colorFboMap.get("YNEG_ZNEG");
        fboYNegZNeg.bind();
        BufferedImage imageYNegZNeg = fboYNegZNeg.getBufferedImage(bufferedImageType);
        fboYNegZNeg.unbind();
        texAtlasManager.dilateBackgroundColor(imageYNegZNeg, backGroundColor);
        if (imageYNegZNeg != null) {
            com.gaia3d.basic.texture.atlas.TexturesAtlasData texturesAtlasDataYNegZNeg = new com.gaia3d.basic.texture.atlas.TexturesAtlasData();
            texturesAtlasDataYNegZNeg.setClassifyId(classificationId);
            texturesAtlasDataYNegZNeg.setCameraDirectionType(CameraDirectionType.YNEG_ZNEG);
            texturesAtlasDataYNegZNeg.setTextureImage(imageYNegZNeg);
            texturesAtlasDataList.add(texturesAtlasDataYNegZNeg);
        }

        // XPOS_ZNEG
        Fbo fboXPosZNeg = colorFboMap.get("XPOS_ZNEG");
        fboXPosZNeg.bind();
        BufferedImage imageXPosZNeg = fboXPosZNeg.getBufferedImage(bufferedImageType);
        fboXPosZNeg.unbind();
        texAtlasManager.dilateBackgroundColor(imageXPosZNeg, backGroundColor);
        if (imageXPosZNeg != null) {
            com.gaia3d.basic.texture.atlas.TexturesAtlasData texturesAtlasDataXPosZNeg = new com.gaia3d.basic.texture.atlas.TexturesAtlasData();
            texturesAtlasDataXPosZNeg.setClassifyId(classificationId);
            texturesAtlasDataXPosZNeg.setCameraDirectionType(CameraDirectionType.XPOS_ZNEG);
            texturesAtlasDataXPosZNeg.setTextureImage(imageXPosZNeg);
            texturesAtlasDataList.add(texturesAtlasDataXPosZNeg);
        }

        // XNEG_ZNEG
        Fbo fboXNegZNeg = colorFboMap.get("XNEG_ZNEG");
        fboXNegZNeg.bind();
        BufferedImage imageXNegZNeg = fboXNegZNeg.getBufferedImage(bufferedImageType);
        fboXNegZNeg.unbind();
        texAtlasManager.dilateBackgroundColor(imageXNegZNeg, backGroundColor);
        if (imageXNegZNeg != null) {
            com.gaia3d.basic.texture.atlas.TexturesAtlasData texturesAtlasDataXNegZNeg = new com.gaia3d.basic.texture.atlas.TexturesAtlasData();
            texturesAtlasDataXNegZNeg.setClassifyId(classificationId);
            texturesAtlasDataXNegZNeg.setCameraDirectionType(CameraDirectionType.XNEG_ZNEG);
            texturesAtlasDataXNegZNeg.setTextureImage(imageXNegZNeg);
            texturesAtlasDataList.add(texturesAtlasDataXNegZNeg);
        }

        // XPOS_YPOS_ZNEG
        Fbo fboXPosYPosZNeg = colorFboMap.get("XPOS_YPOS_ZNEG");
        fboXPosYPosZNeg.bind();
        BufferedImage imageXPosYPosZNeg = fboXPosYPosZNeg.getBufferedImage(bufferedImageType);
        fboXPosYPosZNeg.unbind();
        texAtlasManager.dilateBackgroundColor(imageXPosYPosZNeg, backGroundColor);
        if (imageXPosYPosZNeg != null) {
            com.gaia3d.basic.texture.atlas.TexturesAtlasData texturesAtlasDataXPosYPosZNeg = new com.gaia3d.basic.texture.atlas.TexturesAtlasData();
            texturesAtlasDataXPosYPosZNeg.setClassifyId(classificationId);
            texturesAtlasDataXPosYPosZNeg.setCameraDirectionType(CameraDirectionType.XPOS_YPOS_ZNEG);
            texturesAtlasDataXPosYPosZNeg.setTextureImage(imageXPosYPosZNeg);
            texturesAtlasDataList.add(texturesAtlasDataXPosYPosZNeg);
        }

        // XNEG_YPOS_ZNEG
        Fbo fboXNegYPosZNeg = colorFboMap.get("XNEG_YPOS_ZNEG");
        fboXNegYPosZNeg.bind();
        BufferedImage imageXNegYPosZNeg = fboXNegYPosZNeg.getBufferedImage(bufferedImageType);
        fboXNegYPosZNeg.unbind();
        texAtlasManager.dilateBackgroundColor(imageXNegYPosZNeg, backGroundColor);
        if (imageXNegYPosZNeg != null) {
            com.gaia3d.basic.texture.atlas.TexturesAtlasData texturesAtlasDataXNegYPosZNeg = new com.gaia3d.basic.texture.atlas.TexturesAtlasData();
            texturesAtlasDataXNegYPosZNeg.setClassifyId(classificationId);
            texturesAtlasDataXNegYPosZNeg.setCameraDirectionType(CameraDirectionType.XNEG_YPOS_ZNEG);
            texturesAtlasDataXNegYPosZNeg.setTextureImage(imageXNegYPosZNeg);
            texturesAtlasDataList.add(texturesAtlasDataXNegYPosZNeg);
        }

        // XPOS_YNEG_ZNEG
        Fbo fboXPosYNegZNeg = colorFboMap.get("XPOS_YNEG_ZNEG");
        fboXPosYNegZNeg.bind();
        BufferedImage imageXPosYNegZNeg = fboXPosYNegZNeg.getBufferedImage(bufferedImageType);
        fboXPosYNegZNeg.unbind();
        texAtlasManager.dilateBackgroundColor(imageXPosYNegZNeg, backGroundColor);
        if (imageXPosYNegZNeg != null) {
            com.gaia3d.basic.texture.atlas.TexturesAtlasData texturesAtlasDataXPosYNegZNeg = new com.gaia3d.basic.texture.atlas.TexturesAtlasData();
            texturesAtlasDataXPosYNegZNeg.setClassifyId(classificationId);
            texturesAtlasDataXPosYNegZNeg.setCameraDirectionType(CameraDirectionType.XPOS_YNEG_ZNEG);
            texturesAtlasDataXPosYNegZNeg.setTextureImage(imageXPosYNegZNeg);
            texturesAtlasDataList.add(texturesAtlasDataXPosYNegZNeg);
        }

        // XNEG_YNEG_ZNEG
        Fbo fboXNegYNegZNeg = colorFboMap.get("XNEG_YNEG_ZNEG");
        fboXNegYNegZNeg.bind();
        BufferedImage imageXNegYNegZNeg = fboXNegYNegZNeg.getBufferedImage(bufferedImageType);
        fboXNegYNegZNeg.unbind();
        texAtlasManager.dilateBackgroundColor(imageXNegYNegZNeg, backGroundColor);
        if (imageXNegYNegZNeg != null) {
            com.gaia3d.basic.texture.atlas.TexturesAtlasData texturesAtlasDataXNegYNegZNeg = new com.gaia3d.basic.texture.atlas.TexturesAtlasData();
            texturesAtlasDataXNegYNegZNeg.setClassifyId(classificationId);
            texturesAtlasDataXNegYNegZNeg.setCameraDirectionType(CameraDirectionType.XNEG_YNEG_ZNEG);
            texturesAtlasDataXNegYNegZNeg.setTextureImage(imageXNegYNegZNeg);
            texturesAtlasDataList.add(texturesAtlasDataXNegYNegZNeg);
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
        FaceVisibilityDataManagerV3 faceVisibilityDataManager = new FaceVisibilityDataManagerV3();

        Map<String, Fbo> colorCodeFboMap = integralReMeshParameters.getColorCodeFboMap();
        Map<GaiaFace, HalfEdgeFace> mapGaiaFaceToHalfEdgeFace = mapClassifyIdToGaiaFaceToHalfEdgeFace.computeIfAbsent(classificationId, k -> new HashMap<>());
        Map<GaiaFace, CameraDirectionTypeInfo> mapGaiaFaceToCameraDirectionTypeInfo = mapClassifyIdToGaiaFaceToCameraDirectionTypeInfo.computeIfAbsent(classificationId, k -> new HashMap<>());
        GaiaScene gaiaSceneFromFaces = HalfEdgeUtils.gaiaSceneFromHalfEdgeFaces(facesList, mapGaiaFaceToHalfEdgeFace);

        Fbo fboColorCodeZNeg = colorCodeFboMap.get("ZNEG");
        faceVisibilityDataManager.updateFaceInnerPointsVisibilityData(gaiaSceneFromFaces, CameraDirectionType.ZNEG, fboColorCodeZNeg, mapClassificationCamDirTypeModelViewMatrix, mapClassificationCamDirTypeBBox);
        Fbo fboColorCodeYPosZNeg = colorCodeFboMap.get("YPOS_ZNEG");
        faceVisibilityDataManager.updateFaceInnerPointsVisibilityData(gaiaSceneFromFaces, CameraDirectionType.YPOS_ZNEG, fboColorCodeYPosZNeg, mapClassificationCamDirTypeModelViewMatrix, mapClassificationCamDirTypeBBox);
        Fbo fboColorCodeYNegZNeg = colorCodeFboMap.get("YNEG_ZNEG");
        faceVisibilityDataManager.updateFaceInnerPointsVisibilityData(gaiaSceneFromFaces, CameraDirectionType.YNEG_ZNEG, fboColorCodeYNegZNeg, mapClassificationCamDirTypeModelViewMatrix, mapClassificationCamDirTypeBBox);
        Fbo fboColorCodeXPosZNeg = colorCodeFboMap.get("XPOS_ZNEG");
        faceVisibilityDataManager.updateFaceInnerPointsVisibilityData(gaiaSceneFromFaces, CameraDirectionType.XPOS_ZNEG, fboColorCodeXPosZNeg, mapClassificationCamDirTypeModelViewMatrix, mapClassificationCamDirTypeBBox);
        Fbo fboColorCodeXNegZNeg = colorCodeFboMap.get("XNEG_ZNEG");
        faceVisibilityDataManager.updateFaceInnerPointsVisibilityData(gaiaSceneFromFaces, CameraDirectionType.XNEG_ZNEG, fboColorCodeXNegZNeg, mapClassificationCamDirTypeModelViewMatrix, mapClassificationCamDirTypeBBox);
        Fbo fboColorCodeXPosYPosZNeg = colorCodeFboMap.get("XPOS_YPOS_ZNEG");
        faceVisibilityDataManager.updateFaceInnerPointsVisibilityData(gaiaSceneFromFaces, CameraDirectionType.XPOS_YPOS_ZNEG, fboColorCodeXPosYPosZNeg, mapClassificationCamDirTypeModelViewMatrix, mapClassificationCamDirTypeBBox);
        Fbo fboColorCodeXNegYPosZNeg = colorCodeFboMap.get("XNEG_YPOS_ZNEG");
        faceVisibilityDataManager.updateFaceInnerPointsVisibilityData(gaiaSceneFromFaces, CameraDirectionType.XNEG_YPOS_ZNEG, fboColorCodeXNegYPosZNeg, mapClassificationCamDirTypeModelViewMatrix, mapClassificationCamDirTypeBBox);
        Fbo fboColorCodeXPosYNegZNeg = colorCodeFboMap.get("XPOS_YNEG_ZNEG");
        faceVisibilityDataManager.updateFaceInnerPointsVisibilityData(gaiaSceneFromFaces, CameraDirectionType.XPOS_YNEG_ZNEG, fboColorCodeXPosYNegZNeg, mapClassificationCamDirTypeModelViewMatrix, mapClassificationCamDirTypeBBox);
        Fbo fboColorCodeXNegYNegZNeg = colorCodeFboMap.get("XNEG_YNEG_ZNEG");
        faceVisibilityDataManager.updateFaceInnerPointsVisibilityData(gaiaSceneFromFaces, CameraDirectionType.XNEG_YNEG_ZNEG, fboColorCodeXNegYNegZNeg, mapClassificationCamDirTypeModelViewMatrix, mapClassificationCamDirTypeBBox);
        // end of checking visibility data manager*****************************************************************

        // now assign face to each cameraDirectionType
        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaPrimitive> gaiaPrimitives = extractor.extractAllPrimitives(gaiaSceneFromFaces);

        // Solve CameraDirectionType for each face.***
        Map<GaiaFace, CameraDirectionType> mapFaceIdToBestCameraDirectionType = faceVisibilityDataManager.solveCameraDirectionTypeToFaces(gaiaPrimitives);

        // Assign the CameraDirectionType to faces.***
        for (GaiaPrimitive gaiaPrimitive : gaiaPrimitives) {
            List<GaiaSurface> gaiaSurfaces = gaiaPrimitive.getSurfaces();
            for (GaiaSurface surface : gaiaSurfaces) {
                List<GaiaFace> faces = surface.getFaces();
                for (GaiaFace face : faces) {
                    CameraDirectionTypeInfo cameraDirectionTypeInfo = new CameraDirectionTypeInfo();
                    cameraDirectionTypeInfo.setCameraDirectionType(mapFaceIdToBestCameraDirectionType.get(face));
                    mapGaiaFaceToCameraDirectionTypeInfo.put(face, cameraDirectionTypeInfo);
                }
            }
        }


        // end assign face to each cameraDirectionType.---

        // now set cameraDirectionType to halfEdgeFaces
        for (Map.Entry<GaiaFace, CameraDirectionTypeInfo> entry1 : mapGaiaFaceToCameraDirectionTypeInfo.entrySet()) {
            GaiaFace gaiaFace = entry1.getKey();
            CameraDirectionTypeInfo cameraDirectionTypeInfo = entry1.getValue();
            HalfEdgeFace halfEdgeFace = mapGaiaFaceToHalfEdgeFace.get(gaiaFace);
            halfEdgeFace.setCameraDirectionType(cameraDirectionTypeInfo.getCameraDirectionType());
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
                GaiaBoundingBox bbox = mapClassificationCamDirTypeBBox.get(classifyId).get(cameraDirectionType);
                Matrix4d modelViewMatrix = mapClassificationCamDirTypeModelViewMatrix.get(classifyId).get(cameraDirectionType);

                if (modelViewMatrix == null) {
                    log.info("makeBoxTexturesByObliqueCamera() : modelViewMatrix is null." + "camDirType = " + cameraDirectionType);
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

        faceVisibilityDataManager.deleteObjects();

//        String originalPathStr = halfEdgeSceneMaster.getOriginalPath().toString();
//        Path originalPath = Path.of(originalPathStr);
//        halfEdgeSceneMaster.setOriginalPath(originalPath);
//        //String originalPath = halfEdgeSceneMaster.getOriginalPath().toString();
//
//        // extract the originalProjectName from the originalPath
//        String originalProjectName = originalPathStr.substring(originalPathStr.lastIndexOf(File.separator) + 1);
//        String rawProjectName = originalProjectName.substring(0, originalProjectName.lastIndexOf("."));

        String fileName = nodeName + "_Atlas";
        String extension = ".png";
        int bufferImageType = BufferedImage.TYPE_INT_ARGB;
        GaiaTexture atlasTexture = textureAtlasManager.makeAtlasTexture(texturesAtlasDataList, bufferImageType);

        if (atlasTexture == null) {
            log.info("makeAtlasTexture() : atlasTexture is null.");
            return;
        }

        //BufferedImage atlasImage = atlasTexture.getBufferedImage();

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
        if(atlasScissoredTexture.getBufferedImage() == null) {
            log.info("atlasScissoredTexture.getBufferedImage() is null.");
            return;
        }

        // save the atlas image to disk
        try {
            String imagePath = atlasScissoredTexture.getFullPath();
            File imageFile = new File(imagePath);
            ImageIO.write(atlasScissoredTexture.getBufferedImage(), "png", imageFile);
        } catch (IOException e) {
            log.debug("Error writing image: {}", e);
        }
    }

    private void atlasTextureForIntegralLeafScenes(List<HalfEdgeScene> halfEdgeScenes,
                                                          List<GaiaScene> resultGaiaScenes,
                                                          String outputPathString, String nodeName) {
        if(halfEdgeScenes == null || halfEdgeScenes.isEmpty()){
            log.info("atlasTextureForIntegralLeafScenes: halfEdgeScenes is null or empty.");
            return;
        }

        List<GaiaTextureScissorDataFull> scissorDataFullList = new ArrayList<>();
        int classificationId = -1;
        int scissorExpandPixels = 2;
        int scenesCount = halfEdgeScenes.size();
        List<HalfEdgePrimitive> primitives = new ArrayList<>();
        GaiaAttribute gaiaAttribute = null;
        for(int i=0; i<scenesCount; i++){
            HalfEdgeScene scene = halfEdgeScenes.get(i);
            if(gaiaAttribute == null) {
                // Take the 1rst gaiaAttribute.
                gaiaAttribute = scene.getAttribute().getCopy();
            }
            primitives.clear();
            primitives = scene.extractPrimitives(primitives);
            int primitivesCount = primitives.size();
            for(int j=0; j<primitivesCount; j++) {
                HalfEdgePrimitive primitive = primitives.get(j);
                int materialId = primitive.getMaterialIndex();
                GaiaMaterial material = scene.getMaterials().get(materialId);
                Map<TextureType, List<GaiaTexture>> textures = material.getTextures();
                List<GaiaTexture> diffuseTextures = textures.get(TextureType.DIFFUSE);
                if(diffuseTextures == null || diffuseTextures.isEmpty()){
                    continue;
                }
                GaiaTexture diffuseTexture = diffuseTextures.getFirst();
                BufferedImage bufferedImage = diffuseTexture.getBufferedImage();
                if(bufferedImage == null){
                    continue;
                }
                classificationId += 1; // a classificationId for primitive.

                List<HalfEdgeSurface> surfaces = primitive.getSurfaces();
                int surfacesCount = surfaces.size();
                for(int k=0; k<surfacesCount; k++){
                    HalfEdgeSurface surface = surfaces.get(k);
                    List<List<HalfEdgeFace>> weldedFacesGroups = new ArrayList<>();
                    WeldedFacesFinder.getWeldedFacesGroups(surface, weldedFacesGroups);

                    int weldedFacesGroupsCount = weldedFacesGroups.size();
                    for(int l=0; l<weldedFacesGroupsCount; l++) {
                        List<HalfEdgeFace> weldedFacesGroup = weldedFacesGroups.get(l);
                        GaiaTextureScissorDataFull scissorDataFull = new GaiaTextureScissorDataFull();
                        scissorDataFull.setClassifyId(classificationId);
                        scissorDataFull.setFaces(weldedFacesGroup);
                        scissorDataFull.takeScissoredImageFromMotherImage(bufferedImage);
                        scissorDataFull.expandScissorImage(scissorExpandPixels);

                        if(scissorDataFull.getCurrentBoundary() == null){
                            log.error("atlasTextureForIntegralLeafScenes: scissorDataFull.getCurrentBoundary() is null for classificationId: " + classificationId);
                            continue;
                        }

                        scissorDataFullList.add(scissorDataFull);
                    }
                }

                // delete BufferedImage.
                bufferedImage.flush();
                bufferedImage = null;
            }
        }

        TextureAtlasManager textureAtlasManager = new TextureAtlasManager();
        textureAtlasManager.doAtlasTextureProcessByScissorDatesFull(scissorDataFullList);
        int bufferImageType = BufferedImage.TYPE_INT_ARGB;
        GaiaTexture atlasTexture = textureAtlasManager.makeAtlasTextureScissorDataFull(scissorDataFullList, bufferImageType);

        if (atlasTexture == null) {
            log.info("makeAtlasTexture() : atlasTexture is null.");
            return;
        }

        int atlasWidth = atlasTexture.getWidth();
        int atlasHeight = atlasTexture.getHeight();
        for (GaiaTextureScissorDataFull scissorDataFull : scissorDataFullList) {
            scissorDataFull.recalculateTexCoordsForAtlas(atlasWidth, atlasHeight);
        }

        List<HalfEdgeFace> allFaces = new ArrayList<>();
        for (GaiaTextureScissorDataFull scissorDataFull : scissorDataFullList) {
            List<HalfEdgeFace> faces = scissorDataFull.getFaces();
            allFaces.addAll(faces);
        }

        Map<GaiaFace, HalfEdgeFace> mapGaiaFaceToHalfEdgeFace = new HashMap<>();
        GaiaScene resultGaiaScene = HalfEdgeUtils.gaiaSceneFromHalfEdgeFaces(allFaces, mapGaiaFaceToHalfEdgeFace);
        resultGaiaScene.setAttribute(gaiaAttribute);

        // make the material with the atlasTexture.
        GaiaMaterial material = new GaiaMaterial();
        material.setName("atlasTextureMaterial");
        Map<TextureType, List<GaiaTexture>> textures = new HashMap<>();
        List<GaiaTexture> atlasTextures = new ArrayList<>();
        atlasTextures.add(atlasTexture);
        textures.put(TextureType.DIFFUSE, atlasTextures);
        material.setTextures(textures);
        material.setId(0);

        resultGaiaScene.getMaterials().clear();
        resultGaiaScene.getMaterials().add(material);

        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaPrimitive> gaiaPrimitives = extractor.extractAllPrimitives(resultGaiaScene);
        int primitivesCount = gaiaPrimitives.size();
        for(int i=0; i<primitivesCount; i++){
            GaiaPrimitive primitive = gaiaPrimitives.get(i);
            primitive.setMaterialIndex(0);
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

        String fileName = nodeName + "_Atlas";
        String extension = ".png";


        atlasTexture.setPath(fileName + extension);
        atlasTexture.setParentPath(netSetImagesFolderPath.toString());

        textures = material.getTextures();
        atlasTextures = textures.get(TextureType.DIFFUSE);
        GaiaTexture atlasScissoredTexture = atlasTextures.getFirst();
        atlasScissoredTexture.setParentPath(netSetImagesFolderPath.toString());

        // save the atlas image to disk
        try {
            String imagePath = atlasScissoredTexture.getFullPath();
            File imageFile = new File(imagePath);
            ImageIO.write(atlasScissoredTexture.getBufferedImage(), "png", imageFile);
        } catch (IOException e) {
            log.debug("Error writing image: {}", e);
        }

        resultGaiaScenes.add(resultGaiaScene);

        // delete scissorDataFullList.
        for(GaiaTextureScissorDataFull scissorDataFull : scissorDataFullList){
            scissorDataFull.clear();
        }
    }
}
