package com.gaia3d.renderer;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.exchangable.SceneInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.entities.GaiaAAPlane;
import com.gaia3d.basic.geometry.octree.HalfEdgeOctreeFaces;
import com.gaia3d.basic.geometry.voxel.VoxelGrid3D;
import com.gaia3d.basic.geometry.voxel.VoxelizeParameters;
import com.gaia3d.basic.halfedge.*;
import com.gaia3d.basic.marchingcube.MarchingCube;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.remesher.CellGrid3D;
import com.gaia3d.basic.remesher.ReMeshParameters;
import com.gaia3d.basic.remesher.ReMesherVertexCluster;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.renderer.engine.*;
import com.gaia3d.renderer.engine.dataStructure.FaceVisibilityData;
import com.gaia3d.renderer.engine.dataStructure.FaceVisibilityDataManager;
import com.gaia3d.renderer.engine.dataStructure.GaiaScenesContainer;
import com.gaia3d.renderer.engine.dataStructure.IntegralReMeshParameters;
import com.gaia3d.renderer.engine.fbo.Fbo;
import com.gaia3d.renderer.engine.fbo.FboManager;
import com.gaia3d.renderer.engine.graph.ShaderManager;
import com.gaia3d.renderer.engine.graph.ShaderProgram;
import com.gaia3d.renderer.engine.scene.Camera;
import com.gaia3d.renderer.engine.scene.Projection;
import com.gaia3d.renderer.renderable.RenderableGaiaScene;
import com.gaia3d.util.GaiaTextureUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // Create the voxel grid.***
        VoxelGrid3D voxelGrid3D = new VoxelGrid3D(gridsCountX, gridsCountY, gridsCountZ, bboxAllScenes.clone());

        // Voxelizing XY plane.***
        log.info("starting voxelizing XY...");
        this.voxelizeXY(gridsCountX, gridsCountY, gridsCountZ, bboxAllScenes, voxelGrid3D);
        log.info("starting voxelizing XZ...");
        this.voxelizeXZ(gridsCountX, gridsCountY, gridsCountZ, bboxAllScenes, voxelGrid3D);
        log.info("starting voxelizing YZ...");
        this.voxelizeYZ(gridsCountX, gridsCountY, gridsCountZ, bboxAllScenes, voxelGrid3D);

        // make gaiaPrimitive by marchingCubes.***
        GaiaScene originalScene = scenes.get(0); // take the first scene as original scene

        voxelGrid3D.expand(1); // expand the voxel grid to avoid the artifacts.***
        float isoValue = 0.01f; // original.***
        isoValue = 0.8f; // for DC_Library scale 0.01 settings.***
        GaiaScene gaiaScene = MarchingCube.makeGaiaScene(voxelGrid3D, isoValue);
        log.info("MarchingCube process finished.");
        GaiaAttribute gaiaAttribute = new GaiaAttribute();
        gaiaScene.setAttribute(gaiaAttribute);
        gaiaScene.setOriginalPath(originalScene.getOriginalPath());

        // now, make textures by oblique camera.***
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

        // delete the gaiaScene to free the memory.***
        gaiaScene.clear();

        // now transfer the bufferedImage to gaiaSceneMaterial.***
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

        // return gl default values.***
        glEnable(GL_CULL_FACE);
    }

    private void voxelizeYZ(int gridsCountX, int gridsCountY, int gridsCountZ, GaiaBoundingBox bboxAllScenes, VoxelGrid3D voxelGrid3D) {
        // Voxelizing YZ plane.***
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
            cameraPosition.add(-xLength * 0.5f + i * zRange + zRange * 0.5f, 0, 0); // The last one is the center of the voxel.***
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

            // test : save the image of fbo.*************************************************************
//            int colorBufferedImageType = BufferedImage.TYPE_INT_ARGB;
//            colorFbo.bind();
//            BufferedImage colorImageTest = colorFbo.getBufferedImage(colorBufferedImageType);
//            colorFbo.unbind();
//
//            // test save images
//            try {
//                String path = "D:\\Result_mago3dTiler";
//                String fileName = "sliceYZ_" + i;
//                String extension = ".png";
//                String imagePath = path + "\\" + fileName + extension;
//                File imageFile = new File(imagePath);
//                ImageIO.write(colorImageTest, "png", imageFile);
//            } catch (IOException e) {
//                log.debug("Error writing image: {}", e);
//            }
//
//            // delete the image.***
//            colorImageTest.flush();
//            colorImageTest = null;
        }
    }

    private void voxelizeXZ(int gridsCountX, int gridsCountY, int gridsCountZ, GaiaBoundingBox bboxAllScenes, VoxelGrid3D voxelGrid3D) {
        // Voxelizing XZ plane.***
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

            // test : save the image of fbo.*************************************************************
//            int colorBufferedImageType = BufferedImage.TYPE_INT_ARGB;
//            colorFbo.bind();
//            BufferedImage colorImageTest = colorFbo.getBufferedImage(colorBufferedImageType);
//            colorFbo.unbind();
//
//            // test save images
//            try {
//                String path = "D:\\Result_mago3dTiler";
//                String fileName = "sliceXZ_" + i;
//                String extension = ".png";
//                String imagePath = path + "\\" + fileName + extension;
//                File imageFile = new File(imagePath);
//                ImageIO.write(colorImageTest, "png", imageFile);
//            } catch (IOException e) {
//                log.debug("Error writing image: {}", e);
//            }
        }

    }

    private void voxelizeXY(int gridsCountX, int gridsCountY, int gridsCountZ, GaiaBoundingBox bboxAllScenes, VoxelGrid3D voxelGrid3D) {
        // Voxelizing XY plane.***
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
            cameraPosition.add(0, 0, -zLength * 0.5f + i * zRange + zRange * 0.5f); // The last one is the center of the voxel.***
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

            // test : save the image of fbo.*************************************************************
//            int colorBufferedImageType = BufferedImage.TYPE_INT_ARGB;
//            colorFbo.bind();
//            BufferedImage colorImageTest = colorFbo.getBufferedImage(colorBufferedImageType);
//            colorFbo.unbind();
//
//            // test save images
//            try {
//                String path = "D:\\Result_mago3dTiler";
//                String fileName = "sliceXY_" + i;
//                String extension = ".png";
//                String imagePath = path + "\\" + fileName + extension;
//                File imageFile = new File(imagePath);
//                ImageIO.write(colorImageTest, "png", imageFile);
//            } catch (IOException e) {
//                log.debug("Error writing image: {}", e);
//            }
//
//            // delete the image.***
//            colorImageTest.flush();
//            colorImageTest = null;
        }
    }

    private void translateScene(GaiaScene gaiaScene, Vector3d translation) {
        List<GaiaPrimitive> primitives = new ArrayList<>();
        gaiaScene.extractPrimitives(primitives);

        for (GaiaPrimitive primitive : primitives) {
            List<GaiaVertex> vertices = primitive.getVertices();
            for (GaiaVertex vertex : vertices) {
                Vector3d position = vertex.getPosition();
                position.add(translation);
                vertex.setPosition(position);
            }
        }
    }

    public void integralReMeshByObliqueCameraV2(List<SceneInfo> sceneInfos, List<HalfEdgeScene> resultHalfEdgeScenes, ReMeshParameters reMeshParams, GaiaBoundingBox nodeBBox,
                                                Matrix4d nodeTMatrix, int maxScreenSize, String outputPathString, String nodeName, int lod) {
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
        integralReMeshParameters.createFBOsObliqueCamera(this.engine.getFboManager(), fboWidthColor, fboHeightColor);

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

        FaceVisibilityDataManager faceVisibilityDataManager = new FaceVisibilityDataManager();

        Vector3i nodeMinCellIndex = new Vector3i(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        Vector3i nodeMaxCellIndex = new Vector3i(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);

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

            //**********************************************************************************************************
            // reMesh the scene.****************************************************************************************
            // The "scenePositionRelToCellGrid" is the relative position of the scene respect the center of RootNode (Depth = 0). All scenes must be synchronized to the RootNode.
            Vector3d scenePositionRelToCellGrid = sceneInfo.getScenePosLC(); // relative position of the scene respect the center of RootNode (Depth = 0).
            Vector3d scenePosRelToCellGridNegative = new Vector3d(-scenePositionRelToCellGrid.x, -scenePositionRelToCellGrid.y, -scenePositionRelToCellGrid.z);

            gaiaScene.makeTriangularFaces();
            gaiaScene.spendTranformMatrix();
            gaiaScene.joinAllSurfaces();
            gaiaScene.weldVertices(weldError, false, false, false, false);
            gaiaScene.deleteDegeneratedFaces();
            List<GaiaMaterial> materials = gaiaScene.getMaterials();

            // delete materials.
            for (GaiaMaterial material : materials) {
                material.clear();
            }
            gaiaScene.getMaterials().clear();

            Vector3i sceneMinCellIndex = new Vector3i();
            Vector3i sceneMaxCellIndex = new Vector3i();
            vertexClusters.clear();
            translateScene(gaiaScene, scenePositionRelToCellGrid); // translate the scene to the cell grid position
            ReMesherVertexCluster.reMeshScene(gaiaScene, reMeshParams, vertexClusters, sceneMinCellIndex, sceneMaxCellIndex);
            translateScene(gaiaScene, scenePosRelToCellGridNegative); // translate the scene back to the original position
            vertexClusters.clear();

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
            //**********************************************************************************************************

            // now must translate to the relative position in the node.***
            GaiaNode gaiaNode = gaiaScene.getNodes().get(0);
            gaiaNode.setTransformMatrix(new Matrix4d(sceneTMatLC));
            gaiaNode.setPreMultipliedTransformMatrix(new Matrix4d(sceneTMatLC));
            gaiaScene.spendTranformMatrix();
            gaiaScene.joinAllSurfaces();
            gaiaScene.weldVertices(weldError, false, false, false, false);
            gaiaScene.deleteDegeneratedFaces();

            try {
                // render the scene
                log.info("Rendering the scene : " + i + " / " + scenesCount + ". LOD : " + lod);

                // for each gaiaScene, set the available faceIds, to use for colorCoded rendering
                List<GaiaFace> gaiaFaces = gaiaScene.extractGaiaFaces(null);
                for (GaiaFace gaiaFace : gaiaFaces) {
                    gaiaFace.setId(faceIdAvailable);
                    faceIdAvailable++;
                }
                HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(gaiaScene);
                int bufferedImageType = BufferedImage.TYPE_INT_ARGB;
                engine.makeIntegralBoxTexturesByObliqueCamera(halfEdgeScene, reMeshParams.getTexturePixelsForMeter(), bufferedImageType, nodeBBox, integralReMeshParameters,
                        mapClassifyIdToGaiaFaceToHalfEdgeFace, mapClassifyIdToGaiaFaceToCameraDirectionTypeInfo, mapClassificationCamDirTypeBBox,
                        mapClassificationCamDirTypeModelViewMatrix, mapClassificationCamDirTypeFacesList, faceVisibilityDataManager);
                // end of making oblique camera textures.***

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
                List<GaiaPrimitive> primitives = gaiaScene.extractPrimitives(null);
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
                //System.gc();
                counter = 0;
            }
        }


        //TEST_SaveImagesOfIntegralReMeshFBOs(integralReMeshParameters, "integral");

        // Join all surfaces and weld vertices of the gaiaSceneMaster.
        gaiaSceneMaster.joinAllSurfaces();
        gaiaSceneMaster.weldVertices(weldError, false, false, false, false);
        gaiaSceneMaster.deleteDegeneratedFaces();

        List<GaiaFace> gaiaFacesMaster = gaiaSceneMaster.extractGaiaFaces(null);
        if (gaiaFacesMaster.isEmpty()) {
            log.info("[ERROR] gaiaFacesMaster is empty");
            return;
        }

        HalfEdgeScene halfEdgeSceneMaster = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(gaiaSceneMaster);


        // Atlas texture.***********************************************************************************************
        // Here scissor the atlas textures.
        atlasTextureForIntegralReMesh(integralReMeshParameters, halfEdgeSceneMaster, mapClassifyIdToGaiaFaceToHalfEdgeFace,
                mapClassifyIdToGaiaFaceToCameraDirectionTypeInfo, mapClassificationCamDirTypeBBox,
                mapClassificationCamDirTypeModelViewMatrix, mapClassificationCamDirTypeFacesList,
                outputPathString, nodeName);
        // end of atlas texture.****************************************************************************************

        //if (makeHorizontalSkirt) {
        //halfEdgeSceneMaster.makeHorizontalSkirt();
        //}

        // vertical skirt.********************************************************************************************
        GaiaBoundingBox hedgeSceneBBox = halfEdgeSceneMaster.getBoundingBox();
        double skirtHeight = hedgeSceneBBox.getMaxSize() * 0.04;
        makeVerticalSkirtForIntegralReMesh(halfEdgeSceneMaster, reMeshParams, skirtHeight);
        // end of vertical skirt.*************************************************************************************

        int hola = 0;


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

    private void makeVerticalSkirtForIntegralReMesh(HalfEdgeScene halfEdgeSceneMaster, ReMeshParameters reMeshParameters, double skirtHeight) {
        //*******************************************************
        // note: the halfEdgeSceneMaster has only one surface.***
        //*******************************************************
        List<HalfEdgeSurface> surfaces = halfEdgeSceneMaster.extractSurfaces(null);
        if (surfaces.isEmpty()) {
            return;
        }

        CellGrid3D cellGrid3D = reMeshParameters.getCellGrid();

        Vector3i localMinCellIndex = new Vector3i(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        Vector3i localMaxCellIndex = new Vector3i(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);

        HalfEdgeSurface surface = surfaces.get(0);
        List<HalfEdgeVertex> vertices = surface.getVertices();
        int verticesCount = vertices.size();
        List<HalfEdge> halfEdges = surface.getHalfEdges();
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex startVertex = vertices.get(i);
            Vector3d startVertexPos = startVertex.getPosition();
            Vector3i startVertexCellIndex = cellGrid3D.getCellIndex(startVertexPos);

            if (startVertexCellIndex.x < localMinCellIndex.x) {
                localMinCellIndex.x = startVertexCellIndex.x;
            }
            if (startVertexCellIndex.y < localMinCellIndex.y) {
                localMinCellIndex.y = startVertexCellIndex.y;
            }
            if (startVertexCellIndex.z < localMinCellIndex.z) {
                localMinCellIndex.z = startVertexCellIndex.z;
            }

            if (startVertexCellIndex.x > localMaxCellIndex.x) {
                localMaxCellIndex.x = startVertexCellIndex.x;
            }
            if (startVertexCellIndex.y > localMaxCellIndex.y) {
                localMaxCellIndex.y = startVertexCellIndex.y;
            }
            if (startVertexCellIndex.z > localMaxCellIndex.z) {
                localMaxCellIndex.z = startVertexCellIndex.z;
            }
        }

        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            if (halfEdge.hasTwin()) {
                continue; // only process boundary half-edges
            }
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            Vector3d startVertexPos = startVertex.getPosition();
            HalfEdgeVertex endVertex = halfEdge.getEndVertex();
            Vector3d endVertexPos = endVertex.getPosition();
            Vector3i startVertexCellIndex = cellGrid3D.getCellIndex(startVertexPos);
            Vector3i endVertexCellIndex = cellGrid3D.getCellIndex(endVertexPos);

            // classify the boundary half-edge in south, north, west, east.***
            halfEdge.setClassifyId(-1); // default value for inner edges
            // south.***
            if (startVertexCellIndex.y == localMinCellIndex.y && endVertexCellIndex.y == localMinCellIndex.y) {
                halfEdge.setClassifyId(20); // 20 is the classifyId for south edges
                continue;
            }
            // north.***
            if (startVertexCellIndex.y == localMaxCellIndex.y && endVertexCellIndex.y == localMaxCellIndex.y) {
                halfEdge.setClassifyId(21); // 21 is the classifyId for north edges
                continue;
            }
            // west.***
            if (startVertexCellIndex.x == localMinCellIndex.x && endVertexCellIndex.x == localMinCellIndex.x) {
                halfEdge.setClassifyId(22); // 22 is the classifyId for west edges
                continue;
            }
            // east.***
            if (startVertexCellIndex.x == localMaxCellIndex.x && endVertexCellIndex.x == localMaxCellIndex.x) {
                halfEdge.setClassifyId(23); // 23 is the classifyId for east edges
                continue;
            }
        }

        HalfEdgeSkirtMaker.makeVerticalSkirtByClassifyId(halfEdgeSceneMaster, skirtHeight);
    }

    private void atlasTextureForIntegralReMesh(IntegralReMeshParameters integralReMeshParameters, HalfEdgeScene halfEdgeSceneMaster,
                                               Map<Integer, Map<GaiaFace, HalfEdgeFace>> mapClassifyIdToGaiaFaceToHalfEdgeFace,
                                               Map<Integer, Map<GaiaFace, CameraDirectionTypeInfo>> mapClassifyIdToGaiaFaceToCameraDirectionTypeInfo,
                                               Map<Integer, Map<CameraDirectionType, GaiaBoundingBox>> mapClassificationCamDirTypeBBox,
                                               Map<Integer, Map<CameraDirectionType, Matrix4d>> mapClassificationCamDirTypeModelViewMatrix,
                                               Map<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> mapClassificationCamDirTypeFacesList,
                                               String outputPathString, String nodeName) {
        List<HalfEdgeSurface> surfaces = halfEdgeSceneMaster.extractSurfaces(null);

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
        List<TexturesAtlasData> texturesAtlasDataList = new ArrayList<>();
        Map<String, Fbo> colorFboMap = integralReMeshParameters.getColorFboMap();

        Vector4f backgroundColor = integralReMeshParameters.getBackgroundColor();

        // ZNEG
        Fbo fboZNeg = colorFboMap.get("ZNEG");
        fboZNeg.bind();
        BufferedImage imageZNeg = fboZNeg.getBufferedImage(bufferedImageType);
        fboZNeg.unbind();
        imageZNeg = engine.eliminateBackGroundColor(imageZNeg, backgroundColor);
        if (imageZNeg != null) {
            TexturesAtlasData texturesAtlasDataYPosZNeg = new TexturesAtlasData();
            texturesAtlasDataYPosZNeg.setClassifyId(classificationId);
            texturesAtlasDataYPosZNeg.setCameraDirectionType(CameraDirectionType.CAMERA_DIRECTION_ZNEG);
            texturesAtlasDataYPosZNeg.setTextureImage(imageZNeg);
            texturesAtlasDataList.add(texturesAtlasDataYPosZNeg);
        }

        // YPOS_ZNEG
        Fbo fboYPosZNeg = colorFboMap.get("YPOS_ZNEG");
        fboYPosZNeg.bind();
        BufferedImage imageYPosZNeg = fboYPosZNeg.getBufferedImage(bufferedImageType);
        fboYPosZNeg.unbind();
        imageYPosZNeg = engine.eliminateBackGroundColor(imageYPosZNeg, backgroundColor);
        if (imageYPosZNeg != null) {
            TexturesAtlasData texturesAtlasDataYPosZNeg = new TexturesAtlasData();
            texturesAtlasDataYPosZNeg.setClassifyId(classificationId);
            texturesAtlasDataYPosZNeg.setCameraDirectionType(CameraDirectionType.CAMERA_DIRECTION_YPOS_ZNEG);
            texturesAtlasDataYPosZNeg.setTextureImage(imageYPosZNeg);
            texturesAtlasDataList.add(texturesAtlasDataYPosZNeg);
        }

        // YNEG_ZNEG
        Fbo fboYNegZNeg = colorFboMap.get("YNEG_ZNEG");
        fboYNegZNeg.bind();
        BufferedImage imageYNegZNeg = fboYNegZNeg.getBufferedImage(bufferedImageType);
        fboYNegZNeg.unbind();
        imageYNegZNeg = engine.eliminateBackGroundColor(imageYNegZNeg, backgroundColor);
        if (imageYNegZNeg != null) {
            TexturesAtlasData texturesAtlasDataYNegZNeg = new TexturesAtlasData();
            texturesAtlasDataYNegZNeg.setClassifyId(classificationId);
            texturesAtlasDataYNegZNeg.setCameraDirectionType(CameraDirectionType.CAMERA_DIRECTION_YNEG_ZNEG);
            texturesAtlasDataYNegZNeg.setTextureImage(imageYNegZNeg);
            texturesAtlasDataList.add(texturesAtlasDataYNegZNeg);
        }

        // XPOS_ZNEG
        Fbo fboXPosZNeg = colorFboMap.get("XPOS_ZNEG");
        fboXPosZNeg.bind();
        BufferedImage imageXPosZNeg = fboXPosZNeg.getBufferedImage(bufferedImageType);
        fboXPosZNeg.unbind();
        imageXPosZNeg = engine.eliminateBackGroundColor(imageXPosZNeg, backgroundColor);
        if (imageXPosZNeg != null) {
            TexturesAtlasData texturesAtlasDataXPosZNeg = new TexturesAtlasData();
            texturesAtlasDataXPosZNeg.setClassifyId(classificationId);
            texturesAtlasDataXPosZNeg.setCameraDirectionType(CameraDirectionType.CAMERA_DIRECTION_XPOS_ZNEG);
            texturesAtlasDataXPosZNeg.setTextureImage(imageXPosZNeg);
            texturesAtlasDataList.add(texturesAtlasDataXPosZNeg);
        }

        // XNEG_ZNEG
        Fbo fboXNegZNeg = colorFboMap.get("XNEG_ZNEG");
        fboXNegZNeg.bind();
        BufferedImage imageXNegZNeg = fboXNegZNeg.getBufferedImage(bufferedImageType);
        fboXNegZNeg.unbind();
        imageXNegZNeg = engine.eliminateBackGroundColor(imageXNegZNeg, backgroundColor);
        if (imageXNegZNeg != null) {
            TexturesAtlasData texturesAtlasDataXNegZNeg = new TexturesAtlasData();
            texturesAtlasDataXNegZNeg.setClassifyId(classificationId);
            texturesAtlasDataXNegZNeg.setCameraDirectionType(CameraDirectionType.CAMERA_DIRECTION_XNEG_ZNEG);
            texturesAtlasDataXNegZNeg.setTextureImage(imageXNegZNeg);
            texturesAtlasDataList.add(texturesAtlasDataXNegZNeg);
        }

        // There are no visible faces, so 1rst set the CAMERA_DIRECTION_ZNEG to all the halfEdgeFaces as default
        List<HalfEdgeFace> facesList = facesClassificationMap.get(classificationId);
        if (facesList == null) {
            log.error("atlasTextureForIntegralReMesh: facesList is null for classificationId: " + classificationId);
            return;
        }
        for (HalfEdgeFace halfEdgeFace : facesList) {
            halfEdgeFace.setCameraDirectionType(CameraDirectionType.CAMERA_DIRECTION_ZNEG);
        }

        // check visibility data manager.*******************************************************************************
        FaceVisibilityDataManager faceVisibilityDataManager = new FaceVisibilityDataManager();

        Map<String, Fbo> colorCodeFboMap = integralReMeshParameters.getColorCodeFboMap();
        Fbo fboColorCodeZNeg = colorCodeFboMap.get("ZNEG");
        updateFaceVisibilityData(CameraDirectionType.CAMERA_DIRECTION_ZNEG, fboColorCodeZNeg, faceVisibilityDataManager);
        Fbo fboColorCodeYPosZNeg = colorCodeFboMap.get("YPOS_ZNEG");
        updateFaceVisibilityData(CameraDirectionType.CAMERA_DIRECTION_YPOS_ZNEG, fboColorCodeYPosZNeg, faceVisibilityDataManager);
        Fbo fboColorCodeYNegZNeg = colorCodeFboMap.get("YNEG_ZNEG");
        updateFaceVisibilityData(CameraDirectionType.CAMERA_DIRECTION_YNEG_ZNEG, fboColorCodeYNegZNeg, faceVisibilityDataManager);
        Fbo fboColorCodeXPosZNeg = colorCodeFboMap.get("XPOS_ZNEG");
        updateFaceVisibilityData(CameraDirectionType.CAMERA_DIRECTION_XPOS_ZNEG, fboColorCodeXPosZNeg, faceVisibilityDataManager);
        Fbo fboColorCodeXNegZNeg = colorCodeFboMap.get("XNEG_ZNEG");
        updateFaceVisibilityData(CameraDirectionType.CAMERA_DIRECTION_XNEG_ZNEG, fboColorCodeXNegZNeg, faceVisibilityDataManager);
        // end of checking visibility data manager.********************************************************************

        // now assign face to each cameraDirectionType
        Map<GaiaFace, HalfEdgeFace> mapGaiaFaceToHalfEdgeFace = mapClassifyIdToGaiaFaceToHalfEdgeFace.computeIfAbsent(classificationId, k -> new HashMap<>());
        Map<GaiaFace, CameraDirectionTypeInfo> mapGaiaFaceToCameraDirectionTypeInfo = mapClassifyIdToGaiaFaceToCameraDirectionTypeInfo.computeIfAbsent(classificationId, k -> new HashMap<>());

        GaiaScene gaiaSceneFromFaces = HalfEdgeUtils.gaiaSceneFromHalfEdgeFaces(facesList, mapGaiaFaceToHalfEdgeFace);
        List<GaiaPrimitive> gaiaPrimitives = gaiaSceneFromFaces.extractPrimitives(null);
        for (GaiaPrimitive gaiaPrimitive : gaiaPrimitives) {
            List<GaiaSurface> gaiaSurfaces = gaiaPrimitive.getSurfaces();
            for (GaiaSurface surface : gaiaSurfaces) {
                List<GaiaFace> faces = surface.getFaces();
                for (GaiaFace face : faces) {
                    int faceId = face.getId();
                    CameraDirectionType bestCamDirType = faceVisibilityDataManager.getBestCameraDirectionTypeOfFace(faceId);
                    if (bestCamDirType == null) {
                        bestCamDirType = CameraDirectionType.CAMERA_DIRECTION_ZNEG;
                    } else {
                        int hola = 0;
                    }

                    // put it into map
                    CameraDirectionTypeInfo cameraDirectionTypeInfo = mapGaiaFaceToCameraDirectionTypeInfo.get(face);
                    if (cameraDirectionTypeInfo == null) {
                        cameraDirectionTypeInfo = new CameraDirectionTypeInfo();
                        mapGaiaFaceToCameraDirectionTypeInfo.put(face, cameraDirectionTypeInfo);
                    }

                    cameraDirectionTypeInfo.setCameraDirectionType(bestCamDirType);
                    cameraDirectionTypeInfo.setAngleDegree(120.0); // no used value
                    mapGaiaFaceToCameraDirectionTypeInfo.put(face, cameraDirectionTypeInfo);
                }
            }
        }

        faceVisibilityDataManager.deleteObjects();
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
        double texCoordError = 0.0025;
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

        // save atlas texture data.*************************************************************************************
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

        TextureAtlasManager textureAtlasManager = new TextureAtlasManager();
        textureAtlasManager.doAtlasTextureProcess(texturesAtlasDataList);
        textureAtlasManager.recalculateTexCoordsAfterTextureAtlasingObliqueCamera(halfEdgeSceneMaster, texturesAtlasDataList, mapClassificationCamDirTypeFacesList);

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

        BufferedImage atlasImage = atlasTexture.getBufferedImage();
        //TEST_SaveBufferedImage(atlasImage, "integral_atlasTexture");

        // delete texturesAtlasDataList
        for (TexturesAtlasData texturesAtlasData : texturesAtlasDataList) {
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

        // Scissoring the atlas texture.*******************************************************************
        halfEdgeSceneMaster.scissorTextures();
        material = halfEdgeSceneMaster.getMaterials().get(materialsCount);
        textures = material.getTextures();
        atlasTextures = textures.get(TextureType.DIFFUSE);
        GaiaTexture atlasScissoredTexture = atlasTextures.get(0);
        atlasScissoredTexture.setParentPath(netSetImagesFolderPath.toString());

        //TEST_SaveBufferedImage(atlasScissoredTexture.getBufferedImage(), "diffuse_atlasTexture");

        // save the atlas image to disk
        try {
            String imagePath = atlasScissoredTexture.getFullPath();
            File imageFile = new File(imagePath);
            ImageIO.write(atlasScissoredTexture.getBufferedImage(), "png", imageFile);
        } catch (IOException e) {
            log.debug("Error writing image: {}", e);
        }


        int hola = 0;
    }

    private void updateFaceVisibilityData(CameraDirectionType cameraDirectionType, Fbo colorCodeFbo, FaceVisibilityDataManager faceVisibilityDataManager) {
        colorCodeFbo.bind();

        // read pixels from fbo
        int fboWidth = colorCodeFbo.getFboWidth();
        int fboHeight = colorCodeFbo.getFboHeight();
        ByteBuffer pixels = colorCodeFbo.readPixels(GL_RGBA);

        // unbind the fbo
        colorCodeFbo.unbind();
        //fboManager.deleteFbo("colorCodeObliqueCamera");

        FaceVisibilityData faceVisibilityData = faceVisibilityDataManager.getFaceVisibilityData(cameraDirectionType);

        // determine visible triangles
        int pixelsCount = fboWidth * fboHeight;
        for (int i = 0; i < pixelsCount; i++) {
            int colorCode = pixels.getInt(i * 4);
            // background color is (1, 1, 1, 1). skip background color
            if (colorCode != 0xFFFFFFFF) {
                faceVisibilityData.incrementPixelFaceVisibility(colorCode);
            }
        }

        // delete pixels
        pixels.clear();
    }
}
