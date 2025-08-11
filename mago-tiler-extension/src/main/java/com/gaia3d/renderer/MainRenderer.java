package com.gaia3d.renderer;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.exchangable.SceneInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.entities.GaiaAAPlane;
import com.gaia3d.basic.geometry.octree.HalfEdgeOctreeFaces;
import com.gaia3d.basic.halfedge.*;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.renderer.engine.Engine;
import com.gaia3d.renderer.engine.IAppLogic;
import com.gaia3d.renderer.engine.InternDataConverter;
import com.gaia3d.renderer.engine.Window;
import com.gaia3d.renderer.engine.dataStructure.GaiaScenesContainer;
import com.gaia3d.renderer.engine.fbo.Fbo;
import com.gaia3d.renderer.engine.fbo.FboManager;
import com.gaia3d.renderer.engine.graph.ShaderManager;
import com.gaia3d.renderer.engine.graph.ShaderProgram;
import com.gaia3d.renderer.engine.scene.Camera;
import com.gaia3d.renderer.engine.scene.Projection;
import com.gaia3d.renderer.renderable.RenderableGaiaScene;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

@SuppressWarnings("ALL")
@Slf4j
@Getter
@Setter
public class MainRenderer implements IAppLogic {
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

    public void decimateAndCutByObliqueCamera(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters,
                                              HalfEdgeOctreeFaces octree, List<GaiaAAPlane> cuttingPlanes, double screenPixelsForMeter, boolean makeHorizontalSkirt) {
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
                // hola
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

    public void decimateNetSurfaceAndCutByObliqueCamera(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters,
                                                        HalfEdgeOctreeFaces octree, List<GaiaAAPlane> cuttingPlanes, double depthTexPixelsForMeter, double screenPixelsForMeter, boolean makeHorizontalSkirt) {
        // Note : There are only one scene in the scenes list
        // Must init gl
        try {
            engine.init();
        } catch (Exception e) {
            log.error("[ERROR] initializing the engine: ", e);
        }

        int scenesCount = scenes.size();

        HalfEdgeScene netSurfaceHalfEdgeScene = null;
        GaiaBoundingBox gaiaSceneBbox = null;
        boolean makeVerticalSkirt = true;

        for (int i = 0; i < scenesCount; i++) {
            GaiaScene gaiaScene = scenes.get(i);
            gaiaSceneBbox = gaiaScene.updateBoundingBox();
            double bboxMaxSize = gaiaSceneBbox.getMaxSize();
            int maxDepthScreenSize = (int) Math.ceil(depthTexPixelsForMeter * bboxMaxSize);
            if (maxDepthScreenSize < 8) {
                maxDepthScreenSize = 8;
            }

            if (maxDepthScreenSize > 1024) {
                maxDepthScreenSize = 1024;
            }

            log.info("Engine.makeNetSurfaces() : maxDepthScreenSize = " + maxDepthScreenSize);

            List<BufferedImage> depthRenderedImages = new ArrayList<>();
            this.engine.getGaiaScenesContainer().deleteObjects();
            getDepthRender(gaiaScene, BufferedImage.TYPE_INT_ARGB, depthRenderedImages, maxDepthScreenSize);

            BufferedImage depthRenderedImage = depthRenderedImages.get(0);


            // make the netSurface by using the depthRenderedImage
            float[][] depthValues = com.gaia3d.util.ImageUtils.bufferedImageToFloatMatrix(depthRenderedImage);
            int numCols = depthRenderedImage.getWidth();
            int numRows = depthRenderedImage.getHeight();
            netSurfaceHalfEdgeScene = HalfEdgeUtils.getHalfEdgeSceneRectangularNet(numCols, numRows, depthValues, gaiaSceneBbox, makeVerticalSkirt);
            if (netSurfaceHalfEdgeScene == null) {
                return;
            }
            netSurfaceHalfEdgeScene.setOriginalPath(gaiaScene.getOriginalPath());

            // decimate
            netSurfaceHalfEdgeScene.decimate(decimateParameters);
        }
        // End make net surface**********

        List<HalfEdgeScene> halfEdgeScenes = new ArrayList<>();
        List<RenderableGaiaScene> renderableGaiaScenes = new ArrayList<>();

        halfEdgeScenes.add(netSurfaceHalfEdgeScene);

        // take the halfEdgeScene and decimate and cut it
        HalfEdgeScene halfEdgeScene = halfEdgeScenes.get(0); // only one scene
        halfEdgeScene.decimate(decimateParameters);
        boolean scissorTextures = false;
        List<HalfEdgeScene> resultCutHalfEdgeScenes = HalfEdgeCutter.cutHalfEdgeSceneByGaiaAAPlanes(halfEdgeScene, cuttingPlanes, octree, scissorTextures, false);

        int cutScenesCount = resultCutHalfEdgeScenes.size();
        int i = 0;
        int bufferImageType = BufferedImage.TYPE_INT_RGB;
        for (HalfEdgeScene cutHalfEdgeScene : resultCutHalfEdgeScenes) {
            log.info("makeBoxTexturesByObliqueCamera. cutScene : " + (i + 1) + " / " + cutScenesCount);
            GaiaBoundingBox bbox = cutHalfEdgeScene.getBoundingBox();
            double bboxMaxSize = bbox.getMaxSize();
            // now, cut the halfEdgeScene and make cube-textures by rendering
            double gridSpacing = bboxMaxSize / 3.0;
            HalfEdgeOctreeFaces resultOctree = new HalfEdgeOctreeFaces(null, bbox.clone());
            HalfEdgeScene cuttedScene = HalfEdgeCutter.cutHalfEdgeSceneGridXYZ(cutHalfEdgeScene, gridSpacing, resultOctree);

            if (makeHorizontalSkirt) {
                cuttedScene.makeHorizontalSkirt();
            }

            // now make box textures for the cuttedScene
            engine.makeBoxTexturesByObliqueCamera(cuttedScene, screenPixelsForMeter, bufferImageType);
            cuttedScene.scissorTextures();
            resultHalfEdgeScenes.add(cuttedScene);

            i++;
        }

        engine.deleteObjects();
        engine.getGaiaScenesContainer().deleteObjects();
    }

    public void getDepthRender(GaiaScene gaiaScene, int bufferedImageType, List<BufferedImage> resultImages, int maxDepthScreenSize) {
        // render the scene
        log.info("Rendering the scene...getDepthRender");

        // Must init gl
        try {
            engine.init();
        } catch (Exception e) {
            log.error("[ERROR] initializing the engine: ", e);
        }

        int screenWidth = 1000; // no used var
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
    }

    public void getColorAndDepthRender(List<SceneInfo> sceneInfos, int bufferedImageType, List<BufferedImage> resultImages, GaiaBoundingBox nodeBBox,
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
    }

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
