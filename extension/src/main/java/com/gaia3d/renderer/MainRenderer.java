package com.gaia3d.renderer;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.exchangable.SceneInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.halfedge.HalfEdgeScene;
import com.gaia3d.basic.halfedge.HalfEdgeUtils;
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
import org.lwjgl.opengl.GL;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

@Slf4j
@Getter
@Setter
public class MainRenderer implements IAppLogic {
    private Engine engine = new Engine("MagoVisual3D", new Window.WindowOptions(), this);
    private InternDataConverter internDataConverter = new InternDataConverter();

    public void render() {
        // render the scene
        log.info("Rendering the scene...");
        try{
            engine.run();
        } catch (Exception e) {
            log.error("Error initializing the engine: ", e);
        }

    }

    public void setColorMode(int colorMode)
    {
        this.engine.getRenderer().setColorMode(colorMode);
        this.engine.getHalfEdgeRenderer().setColorMode(colorMode);
    }

    public void renderDecimate(List<GaiaScene> scenes, List<GaiaScene> resultScenes) {

        // Must init gl.***
        try{
            engine.init();
        } catch (Exception e) {
            log.error("Error initializing the engine: " ,e);
        }

        GaiaScenesContainer gaiaScenesContainer = engine.getGaiaScenesContainer();
        List<HalfEdgeScene> halfEdgeScenes = new ArrayList<>();
        int scenesCount = scenes.size();
        List<RenderableGaiaScene> renderableGaiaScenes = new ArrayList<>();

        boolean checkTexCoord = false;
        boolean checkNormal = false;
        boolean checkColor = false;
        boolean checkBatchId = false;
        double error = 1e-4;

        for(int i = 0; i < scenesCount; i++)
        {
            GaiaScene gaiaScene = scenes.get(i);

            // 1rst, make the renderableGaiaScene.***
            RenderableGaiaScene renderableScene = internDataConverter.getRenderableGaiaScene(gaiaScene);
            renderableGaiaScenes.add(renderableScene);

            // 2nd, make the halfEdgeScene.***
            gaiaScene.joinAllSurfaces();
            gaiaScene.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);
            gaiaScene.deleteDegeneratedFaces();

            // Must delete materials because we joined all surfaces into one surface.***
            int materialsCount = gaiaScene.getMaterials().size();
            for (int j = 0; j < materialsCount; j++) {
                GaiaMaterial material = gaiaScene.getMaterials().get(j);
                material.clear();
            }
            gaiaScene.getMaterials().clear();

            HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(gaiaScene);
            halfEdgeScenes.add(halfEdgeScene);
        }

        engine.setHalfEdgeScenes(halfEdgeScenes);

        gaiaScenesContainer.setRenderableGaiaScenes(renderableGaiaScenes);
        engine.setGaiaScenesContainer(gaiaScenesContainer);
        engine.setRenderAxis(true);

        Camera camera = engine.getCamera();
        camera.setPosition(new Vector3d(0, 0, 200));

        FboManager fboManager = engine.getFboManager();
        Window window = engine.getWindow();
        int fboWidthColor = window.getWidth();
        int fboHeightColor = window.getHeight();
        Fbo colorFbo = fboManager.getOrCreateFbo("colorRender", fboWidthColor, fboHeightColor);

        log.info("Rendering the scene...");
        try{
            engine.run();
        } catch (Exception e) {
            log.error("Error initializing the engine: ", e);
        }
    }

    public void decimate(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes) {

        // Must init gl.***
        try{
            engine.init();
        } catch (Exception e) {
            log.error("Error initializing the engine: " ,e);
        }

        GaiaScenesContainer gaiaScenesContainer = engine.getGaiaScenesContainer();
        List<HalfEdgeScene> halfEdgeScenes = new ArrayList<>();
        int scenesCount = scenes.size();
        List<RenderableGaiaScene> renderableGaiaScenes = new ArrayList<>();

        boolean checkTexCoord = false;
        boolean checkNormal = false;
        boolean checkColor = false;
        boolean checkBatchId = false;
        double error = 1e-4;

        log.info("MainRenderer : Decimating the scene...");

        for(int i = 0; i < scenesCount; i++)
        {
            GaiaScene gaiaScene = scenes.get(i);

            // copy the gaiaScene.***
            GaiaScene gaiaSceneCopy = gaiaScene.clone();

            // 1rst, make the renderableGaiaScene.***
            RenderableGaiaScene renderableScene = internDataConverter.getRenderableGaiaScene(gaiaSceneCopy);
            renderableGaiaScenes.add(renderableScene);

            // 2nd, make the halfEdgeScene.***
            gaiaSceneCopy.joinAllSurfaces();
            gaiaSceneCopy.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);
            gaiaSceneCopy.deleteDegeneratedFaces();

            // Must delete materials because we joined all surfaces into one surface.***
            int materialsCount = gaiaSceneCopy.getMaterials().size();
            for (int j = 0; j < materialsCount; j++) {
                GaiaMaterial material = gaiaSceneCopy.getMaterials().get(j);
                material.clear();
            }
            gaiaSceneCopy.getMaterials().clear();

            HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(gaiaSceneCopy);
            halfEdgeScenes.add(halfEdgeScene);
        }

        engine.setHalfEdgeScenes(halfEdgeScenes);

        gaiaScenesContainer.setRenderableGaiaScenes(renderableGaiaScenes);
        engine.setGaiaScenesContainer(gaiaScenesContainer);
        engine.setRenderAxis(true);

        Camera camera = engine.getCamera();
        camera.setPosition(new Vector3d(0, 0, 200));

        FboManager fboManager = engine.getFboManager();
        Window window = engine.getWindow();
        int fboWidthColor = window.getWidth();
        int fboHeightColor = window.getHeight();
        Fbo colorFbo = fboManager.getOrCreateFbo("colorRender", fboWidthColor, fboHeightColor);

        log.info("Rendering the scene...");
        try{
            engine.decimate(halfEdgeScenes, resultHalfEdgeScenes);
        } catch (Exception e) {
            log.error("Error initializing the engine: ", e);
        }
    }

    public void getColorAndDepthRender(List<SceneInfo> sceneInfos, int bufferedImageType, List<BufferedImage> resultImages, GaiaBoundingBox nodeBBox,
                                       Matrix4d nodeTMatrix, int maxScreenSize, int maxDepthScreenSize) {
        // render the scene
        log.info("Rendering the scene...getColorAndDepthRender");

        // Must init gl.***
        try{
            engine.init();
        } catch (Exception e) {
            log.error("Error initializing the engine: " ,e);
        }

        int screenWidth = 1000; // no used var.***
        int screenHeight = 600; // no used var.***

        GaiaScenesContainer gaiaScenesContainer = new GaiaScenesContainer(screenWidth, screenHeight);

        // calculate the projectionMatrix for the camera.***
        Vector3d bboxCenter = nodeBBox.getCenter();
        float xLength = (float)nodeBBox.getSizeX();
        float yLength = (float)nodeBBox.getSizeY();
        float zLength = (float)nodeBBox.getSizeZ();

        Projection projection = new Projection(0, screenWidth, screenHeight);
        projection.setProjectionOrthographic(-xLength/2.0f, xLength/2.0f, -yLength/2.0f, yLength/2.0f, -zLength * 0.5f, zLength * 0.5f);
        gaiaScenesContainer.setProjection(projection);
        engine.setGaiaScenesContainer(gaiaScenesContainer);

        // Take FboManager from engine.***
        FboManager fboManager = engine.getFboManager();

        // create the fbo.***
        int fboWidthColor = maxScreenSize;
        int fboHeightColor = maxScreenSize;
        int fboWidthDepth = maxDepthScreenSize;
        int fboHeightDepth = maxDepthScreenSize;
        if(xLength > yLength)
        {
            fboWidthColor = maxScreenSize;
            fboHeightColor = (int)(maxScreenSize * yLength / xLength);
            fboWidthDepth = maxDepthScreenSize;
            fboHeightDepth = (int)(maxDepthScreenSize * yLength / xLength);
        }
        else
        {
            fboWidthColor = (int)(maxScreenSize * xLength / yLength);
            fboHeightColor = maxScreenSize;
            fboWidthDepth = (int)(maxDepthScreenSize * xLength / yLength);
            fboHeightDepth = maxDepthScreenSize;
        }

        Fbo colorFbo = fboManager.getOrCreateFbo("colorRender", fboWidthColor, fboHeightColor);
        Fbo depthFbo = fboManager.getOrCreateFbo("depthRender", fboWidthDepth, fboHeightDepth);

        // now set camera position.***
        Camera camera = new Camera();
        camera.setPosition(bboxCenter);
        camera.setDirection(new Vector3d(0, 0, -1));
        camera.setUp(new Vector3d(0, 1, 0));
        gaiaScenesContainer.setCamera(camera);


        // clear the colorFbo.***
        colorFbo.bind();
        //glClearColor(0.9f, 0.1f, 0.9f, 1.0f);
        glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // clear the depthFbo.***
        depthFbo.bind();
        glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        int[] width = new int[1];
        int[] height = new int[1];


        // disable cull face.***
        glEnable(GL_DEPTH_TEST);

        // disable cull face.***
        glDisable(GL_CULL_FACE);

        Matrix4d nodeMatrixInv = new Matrix4d(nodeTMatrix);
        nodeMatrixInv.invert();

        // render the scenes.***
        int scenesCount = sceneInfos.size();
        List<RenderableGaiaScene> renderableGaiaScenes = new ArrayList<>();
        int counter = 0;
        for(int i=0; i<scenesCount; i++)
        {
            // load and render, one by one.***
            SceneInfo sceneInfo = sceneInfos.get(i);
            String scenePath = sceneInfo.getScenePath();
            Matrix4d sceneTMat = sceneInfo.getTransformMatrix();

            // must find the local position of the scene rel to node.***
            Vector3d scenePosWC = new Vector3d(sceneTMat.m30(), sceneTMat.m31(), sceneTMat.m32());
            Vector3d scenePosLC = nodeMatrixInv.transformPosition(scenePosWC, new Vector3d());

            // calculate the local sceneTMat.***
            Matrix4d sceneTMatLC = new Matrix4d();
            sceneTMatLC.identity();
            sceneTMatLC.m30(scenePosLC.x);
            sceneTMatLC.m31(scenePosLC.y);
            sceneTMatLC.m32(scenePosLC.z);


            renderableGaiaScenes.clear();

            // load the set file.***
            GaiaSet gaiaSet = null;
            GaiaScene gaiaScene = null;
            Path path = Paths.get(scenePath);
            try
            {
                gaiaSet = GaiaSet.readFile(path);
                gaiaScene = new GaiaScene(gaiaSet);
                GaiaNode gaiaNode = gaiaScene.getNodes().get(0);
                gaiaNode.setTransformMatrix(sceneTMatLC);
                gaiaNode.setPreMultipliedTransformMatrix(sceneTMatLC);
                RenderableGaiaScene renderableScene = internDataConverter.getRenderableGaiaScene(gaiaScene);
                renderableGaiaScenes.add(renderableScene);
            }
            catch (Exception e)
            {
                log.error("Error reading the file: " ,e);
            }

            gaiaScenesContainer.setRenderableGaiaScenes(renderableGaiaScenes);

            try{
                // shader program.***
                ShaderManager shaderManager = engine.getShaderManager();
                ShaderProgram sceneShaderProgram = shaderManager.getShaderProgram("scene");

                // render the scene.***
                // Bind the fbo.***
                width[0] = colorFbo.getFboWidth();
                height[0] = colorFbo.getFboHeight();

                glViewport(0, 0, width[0], height[0]);
                colorFbo.bind();
                log.info("Rendering the scene : " + i + " of scenesCount : " + scenesCount);
                engine.getRenderSceneImage(sceneShaderProgram);
                colorFbo.unbind();

                // depth render.***
                width[0] = depthFbo.getFboWidth();
                height[0] = depthFbo.getFboHeight();

                glViewport(0, 0, width[0], height[0]);
                ShaderProgram depthShaderProgram = shaderManager.getShaderProgram("depth");
                depthFbo.bind();
                log.info("Rendering the depth : " + i + " of scenesCount : " + scenesCount);
                engine.getRenderSceneImage(depthShaderProgram);
                depthFbo.unbind();

            } catch (Exception e) {
                log.error("Error initializing the engine: ", e);
            }

            // delete renderableGaiaScenes.***
            for(RenderableGaiaScene renderableScene : renderableGaiaScenes)
            {
                renderableScene.deleteGLBuffers();
            }

            if(gaiaSet != null)
            {
                gaiaSet.clear();
            }

            if(gaiaScene != null)
            {
                gaiaScene.clear();
            }

            counter++;
            if(counter > 20)
            {
                System.gc();
                counter = 0;
            }
        }

        // take the final rendered colorBuffer of the fbo.***
        colorFbo.bind();
        BufferedImage image = colorFbo.getBufferedImage(bufferedImageType);
        resultImages.add(image);
        colorFbo.unbind();

        // take the final rendered depthBuffer of the fbo.***
        int depthBufferedImageType = BufferedImage.TYPE_INT_ARGB;
        depthFbo.bind();
        BufferedImage depthImage = depthFbo.getBufferedImage(depthBufferedImageType);
        resultImages.add(depthImage);
        depthFbo.unbind();

        // delete renderableGaiaScenes.***
        for(RenderableGaiaScene renderableScene : renderableGaiaScenes)
        {
            renderableScene.deleteGLBuffers();
        }
    }

    public void render(List<GaiaScene> gaiaScenes, int bufferedImageType, List<BufferedImage> resultImages, int maxScreenSize) {
        // render the scene
        log.info("Rendering the scene...");

        // Must init gl.***
        try{
            engine.init();
        } catch (Exception e) {
            log.error("Error initializing the engine: ", e);
        }

        int screenWidth = 1000;
        int screenHeight = 600;

        List<RenderableGaiaScene> renderableGaiaScenes = new ArrayList<>();
        GaiaBoundingBox gaiaBoundingBox = null;

        int gaiaSceneCount = gaiaScenes.size();
        for(int i = 0; i < gaiaSceneCount; i++)
        {
            GaiaScene gaiaScene = gaiaScenes.get(i);
            GaiaBoundingBox bbox = gaiaScene.getBoundingBox();
            if(i == 0)
            {
                gaiaBoundingBox = bbox;
            }
            else
            {
                gaiaBoundingBox.addBoundingBox(bbox);
                gaiaBoundingBox.addBoundingBox(bbox);
            }
            RenderableGaiaScene renderableScene = internDataConverter.getRenderableGaiaScene(gaiaScene);
            renderableGaiaScenes.add(renderableScene);
        }

        if(gaiaBoundingBox == null)
        {
            log.error("Error: gaiaBoundingBox is null.");
            return;
        }

        GaiaScenesContainer gaiaScenesContainer = new GaiaScenesContainer(screenWidth, screenHeight);
        gaiaScenesContainer.setRenderableGaiaScenes(renderableGaiaScenes);

        Vector3d bboxCenter = gaiaBoundingBox.getCenter();
        float xLength = (float)gaiaBoundingBox.getSizeX();
        float yLength = (float)gaiaBoundingBox.getSizeY();
        float zLength = (float)gaiaBoundingBox.getSizeZ();

        // calculate the projectionMatrix for the camera.***
        Projection projection = new Projection(0, screenWidth, screenHeight);
        projection.setProjectionOrthographic(-xLength/2.0f, xLength/2.0f, -yLength/2.0f, yLength/2.0f, -zLength*2.0f, zLength*2.0f);
        gaiaScenesContainer.setProjection(projection);
        engine.setGaiaScenesContainer(gaiaScenesContainer);

        // Take FboManager from engine.***
        FboManager fboManager = engine.getFboManager();

        // create the fbo.***
        int fboWidth = maxScreenSize;
        int fboHeight = maxScreenSize;
        if(xLength > yLength)
        {
            fboWidth = maxScreenSize;
            fboHeight = (int)(maxScreenSize * yLength / xLength);
        }
        else
        {
            fboWidth = (int)(maxScreenSize * xLength / yLength);
            fboHeight = maxScreenSize;
        }

        Fbo colorFbo = fboManager.getOrCreateFbo("colorRender", fboWidth, fboHeight);

        // now set camera position.***
        Camera camera = new Camera();
        camera.setPosition(bboxCenter);
        camera.setDirection(new Vector3d(0, 0, -1));
        camera.setUp(new Vector3d(0, 1, 0));
        gaiaScenesContainer.setCamera(camera);

        try{
            colorFbo.bind();

            int[] width = new int[1];
            int[] height = new int[1];
            width[0] = colorFbo.getFboWidth();
            height[0] = colorFbo.getFboHeight();

            glViewport(0, 0, width[0], height[0]);
            //glClearColor(0.9f, 0.1f, 0.9f, 1.0f);
            glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glEnable(GL_DEPTH_TEST);

            // disable cull face.***
            glDisable(GL_CULL_FACE);

            // shader program.***
            ShaderManager shaderManager = engine.getShaderManager();
            ShaderProgram sceneShaderProgram = shaderManager.getShaderProgram("scene");

            // render the scene.***
            engine.getRenderSceneImage(sceneShaderProgram);

            // make the bufferImage.***
            BufferedImage image = colorFbo.getBufferedImage(bufferedImageType);

            colorFbo.unbind();

            resultImages.add(image);
        } catch (Exception e) {
            log.error("Error initializing the engine : ", e);
        }

        // delete renderableGaiaScenes.***
        for(RenderableGaiaScene renderableScene : renderableGaiaScenes)
        {
            renderableScene.deleteGLBuffers();
        }

    }

    public void deleteObjects()
    {
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
