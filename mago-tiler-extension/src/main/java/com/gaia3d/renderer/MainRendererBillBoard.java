package com.gaia3d.renderer;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.entities.GaiaAAPlane;
import com.gaia3d.basic.geometry.octree.HalfEdgeOctreeFaces;
import com.gaia3d.basic.halfedge.DecimateParameters;
import com.gaia3d.basic.halfedge.HalfEdgeCutter;
import com.gaia3d.basic.halfedge.HalfEdgeScene;
import com.gaia3d.basic.halfedge.HalfEdgeUtils;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.renderer.engine.*;
import com.gaia3d.renderer.engine.dataStructure.GaiaScenesContainer;
import com.gaia3d.renderer.engine.scene.Camera;
import com.gaia3d.renderer.renderable.RenderableGaiaScene;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ALL")
@Slf4j
@Getter
@Setter
public class MainRendererBillBoard implements IAppLogic {
    private Engine engine = new Engine("MagoVisual3D", new Window.WindowOptions(), this);
    private InternDataConverter internDataConverter = new InternDataConverter();

    public void render() {
        // render the scene
        log.info("Rendering the scene...");
        try {
            engine.run();
        } catch (Exception e) {
            log.error("[ERROR] initializing the engine: ", e);
        }

    }

    public void setColorMode(int colorMode) {
        this.engine.getRenderer().setColorMode(colorMode);
        this.engine.getHalfEdgeRenderer().setColorMode(colorMode);
    }

    public void makeBillBoard(List<GaiaScene> scenes, List<GaiaScene> resultScenes, int verticalPlanesCount, int horizontalPlanesCount) {
        // Note : There are only one scene in the scenes list
        // Must init gl
        try {
            engine.init();
        } catch (Exception e) {
            log.error("[ERROR] initializing the engine: ", e);
        }

        GaiaScenesContainer gaiaScenesContainer = engine.getGaiaScenesContainer();
        //List<HalfEdgeScene> halfEdgeScenes = new ArrayList<>();
        List<RenderableGaiaScene> renderableGaiaScenes = new ArrayList<>();

        GaiaScene scene = scenes.get(0); // there are only one scene

        // do 90 degrees rotation in X axis
        scene.spendTransformMatrix();
        GaiaNode rootNode = scene.getNodes().get(0);
        Matrix4d rootTransformMatrix = rootNode.getTransformMatrix();
        scene.spendTransformMatrix();

        Matrix4d rotationMatrix = new Matrix4d().rotateX(Math.PI / 2);
        rootTransformMatrix.mul(rotationMatrix);
        scene.spendTransformMatrix();
        // end do 90 degrees rotation in X axis

        GaiaBoundingBox bbox = scene.updateBoundingBox();
        Vector3d bboxCenter = bbox.getCenter();

        // copy the gaiaScene
        GaiaScene gaiaSceneCopy = scene.clone();

        // 1rst, make the renderableGaiaScene
        RenderableGaiaScene renderableScene = InternDataConverter.getRenderableGaiaScene(gaiaSceneCopy);
        renderableGaiaScenes.add(renderableScene);
        gaiaScenesContainer.setRenderableGaiaScenes(renderableGaiaScenes);

        GaiaScene treeScene = new GaiaScene();
        GaiaNode treeRootNode = new GaiaNode();
        treeRootNode.setName("treeRoot");
        treeScene.getNodes().add(treeRootNode);
        GaiaNode treeNode = new GaiaNode();
        treeNode.setName("BillBoardNode");
        treeRootNode.getChildren().add(treeNode);
        GaiaMesh treeMesh = new GaiaMesh();
        treeNode.getMeshes().add(treeMesh);

        Camera camera = gaiaScenesContainer.getCamera();
        if (camera == null) {
            camera = new Camera();
        }

        int bufferImageType = BufferedImage.TYPE_INT_ARGB;
        List<BufferedImage> resultBufferedImages = new ArrayList<>();

        List<TexturesAtlasData> albedoTexturesAtlasDataList = new ArrayList<>();
        List<TexturesAtlasData> normalTexturesAtlasDataList = new ArrayList<>();

        List<GaiaFace> faces = new ArrayList<>();
        int classifyId = 0;

        // vertical rectangles
        int increAngDeg = verticalPlanesCount != 0 ? 180 / verticalPlanesCount : 0;
        for (int i = 0; i < verticalPlanesCount; i++) {
            Vector3d camDir = new Vector3d(0, 1, 0);
            camDir.rotateZ(Math.toRadians(i * increAngDeg));
            Vector3d camUp = new Vector3d(0, 0, 1);
            camera.setPosition(bboxCenter);
            camera.setDirection(camDir);
            camera.setUp(camUp);
            gaiaScenesContainer.setCamera(camera);

            resultBufferedImages.clear();
            GaiaPrimitive primitive = engine.makeRectangleTextureByCameraDirection4Tree(scene, camDir, resultBufferedImages, bufferImageType, classifyId);
            faces.clear();
            primitive.extractGaiaFaces(faces);
            for (int j = 0; j < faces.size(); j++) {
                GaiaFace face = faces.get(j);
                face.setClassifyId(classifyId);
            }
            BufferedImage albedoImage = resultBufferedImages.get(0);
            BufferedImage normalImage = resultBufferedImages.get(1);

            TexturesAtlasData albedoTexturesAtlasData = new TexturesAtlasData();
            albedoTexturesAtlasData.setTextureImage(albedoImage);
            albedoTexturesAtlasData.setClassifyId(classifyId);
            albedoTexturesAtlasDataList.add(albedoTexturesAtlasData);

            TexturesAtlasData normalTexturesAtlasData = new TexturesAtlasData();
            normalTexturesAtlasData.setTextureImage(normalImage);
            normalTexturesAtlasData.setClassifyId(classifyId);
            normalTexturesAtlasDataList.add(normalTexturesAtlasData);

            treeMesh.getPrimitives().add(primitive);
            classifyId++;
            int hola = 0;
        }

        float trunkHeight = 0.0f;
        // horizontal rectangles. here the camera direcction (0, 0, -1) is looking down always.
        Vector3d camDir = new Vector3d(0, 0, -1);
        Vector3d camUp = new Vector3d(0, 1, 0);
        Vector3d bboxTopCenter = new Vector3d(bboxCenter.x, bboxCenter.y, bbox.getMaxZ());
        camera.setPosition(bboxCenter);
        camera.setDirection(camDir);
        camera.setUp(camUp);
        gaiaScenesContainer.setCamera(camera);
        Vector3d bboxFloorCenter = bbox.getFloorCenter();
        double increDist = (bbox.getSizeZ() - trunkHeight) / (horizontalPlanesCount + 1);
        for (int i = 0; i < horizontalPlanesCount; i++) {
            resultBufferedImages.clear();
            GaiaBoundingBox delimiterBBox = new GaiaBoundingBox();
            double bboxFloorZ = -(increDist / 2) + trunkHeight + bboxFloorCenter.z + (i + 1) * increDist;
            double bboxCeilZ = -(increDist / 2) + trunkHeight + bboxFloorCenter.z + (i + 2) * increDist;

            if (i == 0) {
                //bboxFloorZ = bboxFloorCenter.z;
            }

            delimiterBBox.set(bbox.getMinX(), bbox.getMinY(), bboxFloorZ, bbox.getMaxX(), bbox.getMaxY(), bboxCeilZ);

            GaiaPrimitive primitive = engine.makeRectangleTextureByCameraDirectionTreeBillboradTopDown4Tree(scene, camDir, resultBufferedImages, bufferImageType, delimiterBBox, classifyId);
            faces.clear();
            primitive.extractGaiaFaces(faces);
            for (int j = 0; j < faces.size(); j++) {
                GaiaFace face = faces.get(j);
                face.setClassifyId(classifyId);
            }
            BufferedImage albedoImage = resultBufferedImages.get(0);
            BufferedImage normalImage = resultBufferedImages.get(1);

            TexturesAtlasData albedoTexturesAtlasData = new TexturesAtlasData();
            albedoTexturesAtlasData.setTextureImage(albedoImage);
            albedoTexturesAtlasData.setClassifyId(classifyId);
            albedoTexturesAtlasDataList.add(albedoTexturesAtlasData);

            TexturesAtlasData normalTexturesAtlasData = new TexturesAtlasData();
            normalTexturesAtlasData.setTextureImage(normalImage);
            normalTexturesAtlasData.setClassifyId(classifyId);
            normalTexturesAtlasDataList.add(normalTexturesAtlasData);

            treeMesh.getPrimitives().add(primitive);
            classifyId++;
            int hola = 0;
        }

        TextureAtlasManager textureAtlasManager = new TextureAtlasManager();
        textureAtlasManager.doAtlasTextureProcess(albedoTexturesAtlasDataList);
        textureAtlasManager.copyAtlasTextureProcess(albedoTexturesAtlasDataList, normalTexturesAtlasDataList);
        treeScene.joinAllSurfaces();
        textureAtlasManager.recalculateTexCoordsAfterTextureAtlasing(treeScene, albedoTexturesAtlasDataList);

        // albedo texture atlas
        GaiaTexture atlasAlbedoTexture = textureAtlasManager.makeAtlasTexture(albedoTexturesAtlasDataList, bufferImageType);
        BufferedImage albedoImage = atlasAlbedoTexture.getBufferedImage();
        atlasAlbedoTexture.setName("BillBoardAlbedoTexture");
        atlasAlbedoTexture.setFormat(albedoImage.getType());
        atlasAlbedoTexture.setWidth(albedoImage.getWidth());
        atlasAlbedoTexture.setHeight(albedoImage.getHeight());
        atlasAlbedoTexture.setType(TextureType.DIFFUSE);
        atlasAlbedoTexture.setPath("BillBoardAlbedoTexture" + ".png");

        // normal texture atlas
        GaiaTexture atlasNormalTexture = textureAtlasManager.makeAtlasTexture(normalTexturesAtlasDataList, bufferImageType);
        BufferedImage normalImage = atlasNormalTexture.getBufferedImage();
        atlasNormalTexture.setName("BillBoardNormalTexture");
        atlasNormalTexture.setFormat(normalImage.getType());
        atlasNormalTexture.setWidth(normalImage.getWidth());
        atlasNormalTexture.setHeight(normalImage.getHeight());
        atlasNormalTexture.setType(TextureType.NORMALS);
        atlasNormalTexture.setPath("BillBoardNormalTexture" + ".png");

        GaiaMaterial material = new GaiaMaterial();
        material.setName("BillBoardMaterial");
        int materialsCount = treeScene.getMaterials().size();
        material.setId(materialsCount);
        treeScene.getMaterials().add(material);

        Map<TextureType, List<GaiaTexture>> textures = new HashMap<>();
        List<GaiaTexture> diffuseTextures = new ArrayList<>();
        diffuseTextures.add(atlasAlbedoTexture);
        textures.put(TextureType.DIFFUSE, diffuseTextures);
        List<GaiaTexture> normalTextures = new ArrayList<>();
        normalTextures.add(atlasNormalTexture);
        textures.put(TextureType.NORMALS, normalTextures);
        material.setTextures(textures);
        material.setBlend(false);
        material.setShininess(1.0f);

        List<GaiaPrimitive> primitives = new ArrayList<>();
        treeScene.extractPrimitives(primitives);
        int primitiveCount = primitives.size();
        for (int i = 0; i < primitiveCount; i++) {
            GaiaPrimitive primitive = primitives.get(i);
            primitive.setMaterialIndex(0);
        }
        resultScenes.add(treeScene);
    }

    public void decimateAndCutByObliqueCamera(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters, HalfEdgeOctreeFaces octree, List<GaiaAAPlane> cuttingPlanes, double screenPixelsForMeter, boolean makeHorizontalSkirt) {
        // Note : There are only one scene in the scenes list
        // Must init gl
        try {
            engine.init();
        } catch (Exception e) {
            log.error("[ERROR] initializing the engine: ", e);
        }

        GaiaScenesContainer gaiaScenesContainer = engine.getGaiaScenesContainer();
        List<HalfEdgeScene> halfEdgeScenes = new ArrayList<>();
        int scenesCount = scenes.size();
        List<RenderableGaiaScene> renderableGaiaScenes = new ArrayList<>();

        boolean checkTexCoord = false;
        boolean checkNormal = false;
        boolean checkColor = false;
        boolean checkBatchId = false;
        double error = 1e-5; // weldError

        log.info("MainRenderer : Decimating the scene...");
        for (int i = 0; i < scenesCount; i++) {
            GaiaScene gaiaScene = scenes.get(i);

            // copy the gaiaScene
            GaiaScene gaiaSceneCopy = gaiaScene.clone();

            // 1rst, make the renderableGaiaScene
            RenderableGaiaScene renderableScene = InternDataConverter.getRenderableGaiaScene(gaiaSceneCopy);
            renderableGaiaScenes.add(renderableScene);

            // 2nd, make the halfEdgeScene
            gaiaSceneCopy.joinAllSurfaces();
            gaiaSceneCopy.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);
            gaiaSceneCopy.deleteDegeneratedFaces();

            // Must delete materials because we joined all surfaces into one surface
            int materialsCount = gaiaSceneCopy.getMaterials().size();
            for (int j = 0; j < materialsCount; j++) {
                GaiaMaterial material = gaiaSceneCopy.getMaterials().get(j);
                material.clear();
            }
            gaiaSceneCopy.getMaterials().clear();

            HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(gaiaSceneCopy);
            halfEdgeScenes.add(halfEdgeScene);
        }

        gaiaScenesContainer.setRenderableGaiaScenes(renderableGaiaScenes);
        engine.setGaiaScenesContainer(gaiaScenesContainer);
        engine.setRenderAxis(true);

        // take the halfEdgeScene and decimate and cut it
        HalfEdgeScene halfEdgeScene = halfEdgeScenes.get(0); // only one scene
        halfEdgeScene.decimate(decimateParameters);

        boolean scissorTextures = false;
        List<HalfEdgeScene> resultCutHalfEdgeScenes = HalfEdgeCutter.cutHalfEdgeSceneByGaiaAAPlanes(halfEdgeScene, cuttingPlanes, octree, scissorTextures, false);

        int lod = decimateParameters.getLod();
        int cutScenesCount = resultCutHalfEdgeScenes.size();
        int i = 0;
        int bufferImageType = BufferedImage.TYPE_INT_RGB;
        for (HalfEdgeScene cutHalfEdgeScene : resultCutHalfEdgeScenes) {
            log.info("makeBoxTexturesByObliqueCamera. cutScene : " + (i + 1) + " / " + cutScenesCount);
            GaiaBoundingBox bbox = cutHalfEdgeScene.getBoundingBox();
            double bboxMaxSize = bbox.getMaxSize();
            // now, cut the halfEdgeScene and make cube-textures by rendering
            double gridSpacing = bboxMaxSize / 3.0;
            if (lod == 1) {
                gridSpacing = bboxMaxSize / 5.0;
            }
            HalfEdgeOctreeFaces resultOctree = new HalfEdgeOctreeFaces(null, bbox.clone());
            HalfEdgeScene cuttedScene = HalfEdgeCutter.cutHalfEdgeSceneGridXYZ(cutHalfEdgeScene, gridSpacing, resultOctree); // original

            if (makeHorizontalSkirt) {
                cuttedScene.makeHorizontalSkirt();
            }

            // now make box textures for the cuttedScene
            engine.makeBoxTexturesByObliqueCamera(cuttedScene, screenPixelsForMeter, bufferImageType);

            cuttedScene.scissorTextures();
            resultHalfEdgeScenes.add(cuttedScene);

            i++;
        }

        // delete halfEdgeScenes
        for (HalfEdgeScene halfEdgeSceneToDelete : halfEdgeScenes) {
            halfEdgeSceneToDelete.deleteObjects();
        }
        halfEdgeScenes.clear();

        engine.getGaiaScenesContainer().deleteObjects();
        engine.deleteObjects();

    }

    /*public void getDepthRender(GaiaScene gaiaScene, int bufferedImageType, List<BufferedImage> resultImages, int maxDepthScreenSize) {
        // render the scene
        log.info("Rendering the scene...getDepthRender");

        // Must init gl
        try {
            engine.init();
        } catch (Exception e) {
            log.error("[ERROR] initializing the engine: ", e);
        }

        int screenWidth = 1000;
        int screenHeight = 600; // no used var

        //GaiaScenesContainer gaiaScenesContainer = new GaiaScenesContainer(screenWidth, screenHeight); // original
        GaiaScenesContainer gaiaScenesContainer = this.engine.getGaiaScenesContainer();

        // calculate the projectionMatrix for the camera
        GaiaBoundingBox bbox = gaiaScene.updateBoundingBox();
        Vector3d bboxCenter = bbox.getCenter();
        float xLength = (float) bbox.getSizeX();
        float yLength = (float) bbox.getSizeY();
        float zLength = (float) bbox.getSizeZ();

        Projection projection = new Projection(0, screenWidth, screenHeight);
        projection.setProjectionOrthographic(-xLength / 2.0f, xLength / 2.0f, -yLength / 2.0f, yLength / 2.0f, -zLength * 0.5f, zLength * 0.5f);
        gaiaScenesContainer.setProjection(projection);
        engine.setGaiaScenesContainer(gaiaScenesContainer);

        // Take FboManager from engine
        FboManager fboManager = engine.getFboManager();

        // create the fbo
        int fboWidthDepth = maxDepthScreenSize;
        int fboHeightDepth = maxDepthScreenSize;
        if (xLength > yLength) {
            fboWidthDepth = maxDepthScreenSize;
            fboHeightDepth = (int) (maxDepthScreenSize * yLength / xLength);
        } else {
            fboWidthDepth = (int) (maxDepthScreenSize * xLength / yLength);
            fboHeightDepth = maxDepthScreenSize;
        }

        fboWidthDepth = Math.max(fboWidthDepth, 1);
        fboHeightDepth = Math.max(fboHeightDepth, 1);

        Fbo depthFbo = fboManager.getOrCreateFbo("depthRender", fboWidthDepth, fboHeightDepth);

        // now set camera position
        Camera camera = new Camera();
        camera.setPosition(bboxCenter);
        camera.setDirection(new Vector3d(0, 0, -1));
        camera.setUp(new Vector3d(0, 1, 0));
        gaiaScenesContainer.setCamera(camera);

        // render the scenes
        int scenesCount = 1;
        List<RenderableGaiaScene> renderableGaiaScenes = new ArrayList<>();
        RenderableGaiaScene renderableScene = InternDataConverter.getRenderableGaiaScene(gaiaScene);
        renderableGaiaScenes.add(renderableScene);

        gaiaScenesContainer.setRenderableGaiaScenes(renderableGaiaScenes);

        try {
            // shader program
            ShaderManager shaderManager = engine.getShaderManager();
            ShaderProgram sceneShaderProgram = shaderManager.getShaderProgram("scene");

            // render the scene
            // depth render
            int[] width = new int[1];
            int[] height = new int[1];
            width[0] = depthFbo.getFboWidth();
            height[0] = depthFbo.getFboHeight();

            glViewport(0, 0, width[0], height[0]);
            ShaderProgram depthShaderProgram = shaderManager.getShaderProgram("depth");
            depthFbo.bind();

            glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // disable cull face
            glEnable(GL_DEPTH_TEST);

            // disable cull face
            glDisable(GL_CULL_FACE);

            log.info("Rendering the depth : " + 0 + " of scenesCount : " + scenesCount);
            engine.getRenderer().setColorMode(0); // set colorMode to 0 = uniqueColor
            engine.getRenderSceneImage(depthShaderProgram);
            engine.getRenderer().setColorMode(2); // set colorMode to 2 = textureColor
            depthFbo.unbind();

        } catch (Exception e) {
            log.error("[ERROR] initializing the engine: ", e);
        }

//        // delete renderableGaiaScenes
//        for (RenderableGaiaScene renderableSceneToDelete : renderableGaiaScenes) {
//            renderableSceneToDelete.deleteGLBuffers();
//        }

        // take the final rendered depthBuffer of the fbo
        int depthBufferedImageType = BufferedImage.TYPE_INT_ARGB;
        depthFbo.bind();
        BufferedImage depthImage = depthFbo.getBufferedImage(depthBufferedImageType);
        resultImages.add(depthImage);
        depthFbo.unbind();
    }*/

    /*public void getColorAndDepthRender(List<SceneInfo> sceneInfos, int bufferedImageType, List<BufferedImage> resultImages, GaiaBoundingBox nodeBBox,
                                       Matrix4d nodeTMatrix, int maxScreenSize, int maxDepthScreenSize) {
        // render the scene
        log.info("Rendering the scene...getColorAndDepthRender");

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
        int fboWidthDepth = maxDepthScreenSize;
        int fboHeightDepth = maxDepthScreenSize;
        if (xLength > yLength) {
            fboWidthColor = maxScreenSize;
            fboHeightColor = (int) (maxScreenSize * yLength / xLength);
            fboWidthDepth = maxDepthScreenSize;
            fboHeightDepth = (int) (maxDepthScreenSize * yLength / xLength);
        } else {
            fboWidthColor = (int) (maxScreenSize * xLength / yLength);
            fboHeightColor = maxScreenSize;
            fboWidthDepth = (int) (maxDepthScreenSize * xLength / yLength);
            fboHeightDepth = maxDepthScreenSize;
        }

        Fbo colorFbo = fboManager.getOrCreateFbo("colorRender", fboWidthColor, fboHeightColor);
        Fbo depthFbo = fboManager.getOrCreateFbo("depthRender", fboWidthDepth, fboHeightDepth);

        // now set camera position
        Camera camera = new Camera();
        camera.setPosition(bboxCenter);
        camera.setDirection(new Vector3d(0, 0, -1));
        camera.setUp(new Vector3d(0, 1, 0));
        gaiaScenesContainer.setCamera(camera);


        // clear the colorFbo
        colorFbo.bind();
        //glClearColor(0.9f, 0.1f, 0.9f, 1.0f);
        glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // clear the depthFbo
        depthFbo.bind();
        glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        int[] width = new int[1];
        int[] height = new int[1];


        // disable cull face
        glEnable(GL_DEPTH_TEST);

        // disable cull face
        glDisable(GL_CULL_FACE);

        Matrix4d nodeMatrixInv = new Matrix4d(nodeTMatrix);
        nodeMatrixInv.invert();

        // render the scenes
        int scenesCount = sceneInfos.size();
        List<RenderableGaiaScene> renderableGaiaScenes = new ArrayList<>();
        int counter = 0;
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
                GaiaNode gaiaNode = gaiaScene.getNodes().get(0);
                gaiaNode.setTransformMatrix(sceneTMatLC);
                gaiaNode.setPreMultipliedTransformMatrix(sceneTMatLC);
                RenderableGaiaScene renderableScene = InternDataConverter.getRenderableGaiaScene(gaiaScene);
                renderableGaiaScenes.add(renderableScene);
            } catch (Exception e) {
                log.error("[ERROR] reading the file: ", e);
            }

            gaiaScenesContainer.setRenderableGaiaScenes(renderableGaiaScenes);

            try {
                // shader program
                ShaderManager shaderManager = engine.getShaderManager();
                ShaderProgram sceneShaderProgram = shaderManager.getShaderProgram("scene");

                // render the scene
                // Bind the fbo
                width[0] = colorFbo.getFboWidth();
                height[0] = colorFbo.getFboHeight();

                glViewport(0, 0, width[0], height[0]);
                colorFbo.bind();
                log.info("Rendering the scene : " + i + " of scenesCount : " + scenesCount);
                engine.getRenderSceneImage(sceneShaderProgram);
                colorFbo.unbind();

                // depth render
                width[0] = depthFbo.getFboWidth();
                height[0] = depthFbo.getFboHeight();

                glViewport(0, 0, width[0], height[0]);
                ShaderProgram depthShaderProgram = shaderManager.getShaderProgram("depth");
                depthFbo.bind();
                log.info("Rendering the depth : " + i + " of scenesCount : " + scenesCount);
                engine.getRenderSceneImage(depthShaderProgram);
                depthFbo.unbind();

            } catch (Exception e) {
                log.error("[ERROR] initializing the engine: ", e);
            }

            // delete renderableGaiaScenes
            for (RenderableGaiaScene renderableScene : renderableGaiaScenes) {
                renderableScene.deleteGLBuffers();
            }

            if (gaiaSet != null) {
                gaiaSet.clear();
            }

            if (gaiaScene != null) {
                gaiaScene.clear();
            }

            counter++;
            if (counter > 20) {
                //System.gc();
                counter = 0;
            }
        }

        // take the final rendered colorBuffer of the fbo
        colorFbo.bind();
        BufferedImage image = colorFbo.getBufferedImage(bufferedImageType);
        resultImages.add(image);
        colorFbo.unbind();

        // take the final rendered depthBuffer of the fbo
        int depthBufferedImageType = BufferedImage.TYPE_INT_ARGB;
        depthFbo.bind();
        BufferedImage depthImage = depthFbo.getBufferedImage(depthBufferedImageType);
        resultImages.add(depthImage);
        depthFbo.unbind();

        // delete renderableGaiaScenes
        engine.deleteObjects();
        for (RenderableGaiaScene renderableScene : renderableGaiaScenes) {
            renderableScene.deleteGLBuffers();
        }
    }*/

    public void deleteObjects() {
        engine.deleteObjects();
    }

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

}
