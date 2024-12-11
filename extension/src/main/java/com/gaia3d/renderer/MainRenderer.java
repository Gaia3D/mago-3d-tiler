package com.gaia3d.renderer;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.exchangable.SceneInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.octree.HalfEdgeOctree;
import com.gaia3d.basic.halfedge.DecimateParameters;
import com.gaia3d.basic.halfedge.HalfEdgeCutter;
import com.gaia3d.basic.halfedge.HalfEdgeScene;
import com.gaia3d.basic.halfedge.HalfEdgeUtils;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaPrimitive;
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
import com.gaia3d.util.GaiaPrimitiveUtils;
import com.gaia3d.util.GaiaSceneUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL;

import java.awt.*;
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

    public void renderPyramidDeformation(List<GaiaScene> scenes, List<GaiaScene> resultScenes) {

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

            engine.getGaiaScenes().add(gaiaScene);
        }

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

    public void decimate(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters) {

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
            engine.decimate(halfEdgeScenes, resultHalfEdgeScenes, decimateParameters);
        } catch (Exception e) {
            log.error("Error initializing the engine: ", e);
        }
    }

    public void getDepthRender(GaiaScene gaiaScene, int bufferedImageType, List<BufferedImage> resultImages, int maxDepthScreenSize) {
        // render the scene
        log.info("Rendering the scene...getDepthRender");

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
        GaiaBoundingBox bbox = gaiaScene.getBoundingBox();
        Vector3d bboxCenter = bbox.getCenter();
        float xLength = (float)bbox.getSizeX();
        float yLength = (float)bbox.getSizeY();
        float zLength = (float)bbox.getSizeZ();

        Projection projection = new Projection(0, screenWidth, screenHeight);
        projection.setProjectionOrthographic(-xLength/2.0f, xLength/2.0f, -yLength/2.0f, yLength/2.0f, -zLength * 0.5f, zLength * 0.5f);
        gaiaScenesContainer.setProjection(projection);
        engine.setGaiaScenesContainer(gaiaScenesContainer);

        // Take FboManager from engine.***
        FboManager fboManager = engine.getFboManager();

        // create the fbo.***
        int fboWidthDepth = maxDepthScreenSize;
        int fboHeightDepth = maxDepthScreenSize;
        if(xLength > yLength)
        {
            fboWidthDepth = maxDepthScreenSize;
            fboHeightDepth = (int)(maxDepthScreenSize * yLength / xLength);
        }
        else
        {
            fboWidthDepth = (int)(maxDepthScreenSize * xLength / yLength);
            fboHeightDepth = maxDepthScreenSize;
        }

        fboWidthDepth = Math.max(fboWidthDepth, 1);
        fboHeightDepth = Math.max(fboHeightDepth, 1);

        Fbo depthFbo = fboManager.getOrCreateFbo("depthRender", fboWidthDepth, fboHeightDepth);

        // now set camera position.***
        Camera camera = new Camera();
        camera.setPosition(bboxCenter);
        camera.setDirection(new Vector3d(0, 0, -1));
        camera.setUp(new Vector3d(0, 1, 0));
        gaiaScenesContainer.setCamera(camera);

        // render the scenes.***
        int scenesCount = 1;
        List<RenderableGaiaScene> renderableGaiaScenes = new ArrayList<>();
        RenderableGaiaScene renderableScene = internDataConverter.getRenderableGaiaScene(gaiaScene);
        renderableGaiaScenes.add(renderableScene);

        gaiaScenesContainer.setRenderableGaiaScenes(renderableGaiaScenes);

        try{
            // shader program.***
            ShaderManager shaderManager = engine.getShaderManager();
            ShaderProgram sceneShaderProgram = shaderManager.getShaderProgram("scene");

            // render the scene.***
            // depth render.***
            int[] width = new int[1];
            int[] height = new int[1];
            width[0] = depthFbo.getFboWidth();
            height[0] = depthFbo.getFboHeight();

            glViewport(0, 0, width[0], height[0]);
            ShaderProgram depthShaderProgram = shaderManager.getShaderProgram("depth");
            depthFbo.bind();

            glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // disable cull face.***
            glEnable(GL_DEPTH_TEST);

            // disable cull face.***
            glDisable(GL_CULL_FACE);

            log.info("Rendering the depth : " + 0 + " of scenesCount : " + scenesCount);
            engine.getRenderSceneImage(depthShaderProgram);
            depthFbo.unbind();

        } catch (Exception e) {
            log.error("Error initializing the engine: ", e);
        }

//        // delete renderableGaiaScenes.***
//        for(RenderableGaiaScene renderableSceneToDelete : renderableGaiaScenes) {
//            renderableSceneToDelete.deleteGLBuffers();
//        }

        // take the final rendered depthBuffer of the fbo.***
        int depthBufferedImageType = BufferedImage.TYPE_INT_ARGB;
        depthFbo.bind();
        BufferedImage depthImage = depthFbo.getBufferedImage(depthBufferedImageType);
        resultImages.add(depthImage);
        depthFbo.unbind();
    }

    private float[][] bufferedImageToFloatMatrix(BufferedImage image)
    {
        int width = image.getWidth();
        int height = image.getHeight();
        float[][] floatMatrix = new float[width][height];
        for(int i = 0; i < width; i++)
        {
            for(int j = 0; j < height; j++)
            {
                Color color = new Color(image.getRGB(i, j), true);
                float r = color.getRed()/255.0f;
                float g = color.getGreen()/255.0f;
                float b = color.getBlue()/255.0f;
                float a = color.getAlpha()/255.0f;

                float depth = unpackDepth32(new float[]{r, g, b, a});
                floatMatrix[i][j] = depth;
                int hola = 0;
            }
        }

        return floatMatrix;
    }

    public float unpackDepth32(float[] packedDepth)
    {
        if (packedDepth.length != 4) {
            throw new IllegalArgumentException("packedDepth debe tener exactamente 4 elementos.");
        }

        // Ajuste del valor final (equivalente a packedDepth - 1.0 / 512.0)
        for (int i = 0; i < 4; i++) {
            packedDepth[i] -= 1.0f / 512.0f;
        }

        // Producto punto para recuperar la profundidad original
        return packedDepth[0]
                + packedDepth[1] / 256.0f
                + packedDepth[2] / (256.0f * 256.0f)
                + packedDepth[3] / 16777216.0f;
    }

    public void makeNetSurfaces(List<GaiaScene> scenes, List<HalfEdgeScene> resultHalfEdgeScenes, DecimateParameters decimateParameters, double pixelsForMeter) {

        // Must init gl.***
        try{
            engine.init();
        } catch (Exception e) {
            log.error("Error initializing the engine: " ,e);
        }

        GaiaScenesContainer gaiaScenesContainer = engine.getGaiaScenesContainer();
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
//            renderableGaiaScenes.clear();
//            RenderableGaiaScene renderableScene = internDataConverter.getRenderableGaiaScene(gaiaScene);
//            renderableGaiaScenes.add(renderableScene);
//            gaiaScenesContainer.setRenderableGaiaScenes(renderableGaiaScenes);


            GaiaBoundingBox bbox = gaiaScene.getBoundingBox();
            double bboxMaxSize = Math.max(bbox.getSizeX(), bbox.getSizeY());
            int maxDepthScreenSize = (int)Math.ceil(pixelsForMeter * bboxMaxSize);
            if(maxDepthScreenSize < 8)
            {
                maxDepthScreenSize = 8;
            }

            if(maxDepthScreenSize > 1024)
            {
                maxDepthScreenSize = 1024;
            }

            log.info("Engine.makeNetSurfaces() : maxDepthScreenSize = " + maxDepthScreenSize);

            List<BufferedImage> depthRenderedImages = new ArrayList<>();
            getDepthRender(gaiaScene, BufferedImage.TYPE_INT_ARGB, depthRenderedImages, maxDepthScreenSize);

            BufferedImage depthRenderedImage = depthRenderedImages.get(0);

            // make the netSurface by using the depthRenderedImage.***
            float[][] depthValues = bufferedImageToFloatMatrix(depthRenderedImage);
            int numCols = depthRenderedImage.getWidth();
            int numRows = depthRenderedImage.getHeight();
            HalfEdgeScene halfEdgeScene = HalfEdgeUtils.getHalfEdgeSceneRectangularNet(numCols, numRows, depthValues, bbox);
            if(halfEdgeScene == null)
            {
                return;
            }
            halfEdgeScene.setOriginalPath(gaiaScene.getOriginalPath());

            // decimate.***
            halfEdgeScene.doTrianglesReductionOneIteration(decimateParameters);

            // now, cut the halfEdgeScene and make cube-textures by rendering.***
            double gridSpacing = 50.0;
            HalfEdgeOctree resultOctree = new HalfEdgeOctree(null);
            log.info("Engine.decimate() : cutHalfEdgeSceneGridXYZ.");
            HalfEdgeScene cuttedScene = HalfEdgeCutter.cutHalfEdgeSceneGridXYZ(halfEdgeScene, gridSpacing, resultOctree);
            cuttedScene.splitFacesByBestPlanesToProject();

            // now make box textures for the cuttedScene.***
            log.info("Engine.decimate() : makeBoxTexturesForHalfEdgeScene.");
            engine.makeBoxTexturesForHalfEdgeScene(cuttedScene);

            resultHalfEdgeScenes.add(cuttedScene);

            gaiaScenesContainer.deleteObjects();
            halfEdgeScene.deleteObjects();
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

//    private void doPyramidDeformation(RenderableGaiaScene renderableScene, GaiaBoundingBox originalBBox, GaiaBoundingBox deformedBBox) {
//        List<GaiaPrimitive> primitives = renderableScene.extractPrimitives(null);
//        for (GaiaPrimitive primitive : primitives) {
//            GaiaPrimitiveUtils.doPyramidDeformation(primitive, originalBBox, deformedBBox);
//        }
//    }

    public void makeNetSurfacesByPyramidDeformationRender(List<SceneInfo> sceneInfos, int bufferedImageType, List<HalfEdgeScene> resultHalfEdgeScenes, List<BufferedImage> resultImages,
                                                          GaiaBoundingBox nodeBBox, Matrix4d nodeTMatrix, int maxScreenSize, int maxDepthScreenSize) {
        // render the scene
        log.info("making net surfaces by pyramid deformation...");

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
        Vector3d bboxCenter = nodeBBox.getCenter(); // used to locate the camera.***

        // must calculate the deformedNodeBBox.***
        //GaiaBoundingBox deformedNodeBBox = nodeBBox.clone();
//        double deformationHeight = nodeBBox.getMaxSize() * 0.5;
//
//        deformedNodeBBox.setMinX(nodeBBox.getMinX() - deformationHeight);
//        deformedNodeBBox.setMaxX(nodeBBox.getMaxX() + deformationHeight);
//        deformedNodeBBox.setMinY(nodeBBox.getMinY() - deformationHeight);
//        deformedNodeBBox.setMaxY(nodeBBox.getMaxY() + deformationHeight);

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
            GaiaScene deformadGaiaScene = null;
            Path path = Paths.get(scenePath);
            try
            {
                gaiaSet = GaiaSet.readFile(path);
                gaiaScene = new GaiaScene(gaiaSet);
                gaiaScene.makeTriangleFaces();
                GaiaBoundingBox bbox = gaiaScene.getBoundingBox(); // before to set the transformMatrix.***

                GaiaNode gaiaNode = gaiaScene.getNodes().get(0);
                gaiaNode.setTransformMatrix(sceneTMatLC);
                gaiaNode.setPreMultipliedTransformMatrix(sceneTMatLC);


                double minH = bbox.getMinZ();
                double maxH = bbox.getMaxZ() * 2.0;
                double dist = 6.0;

                GaiaSceneUtils.deformSceneByVerticesConvexity(gaiaScene, dist, minH, maxH);

//                HalfEdgeScene deformedHalfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(gaiaScene);
//                //HalfEdgeUtils.deformHalfEdgeSurfaceByVerticesConvexConcave(deformedHalfEdgeScene, 3.5);
//
//                deformadGaiaScene = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(deformedHalfEdgeScene);
//                gaiaNode = deformadGaiaScene.getNodes().get(0);
//                gaiaNode.setTransformMatrix(sceneTMatLC);
//                gaiaNode.setPreMultipliedTransformMatrix(sceneTMatLC);

                // do pyramid deformation.***
                //log.info("Doing pyramid deformation...");
                //GaiaSceneUtils.doPyramidDeformation(deformadGaiaScene, nodeBBox, deformedNodeBBox);

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

            if(deformadGaiaScene != null)
            {
                deformadGaiaScene.clear();
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
        //resultImages.add(depthImage); // no need to add the depthImage.***
        depthFbo.unbind();

        // delete renderableGaiaScenes.***
        for(RenderableGaiaScene renderableScene : renderableGaiaScenes)
        {
            renderableScene.deleteGLBuffers();
        }

        // now make a halfEdgeScene from the depthImage.************************************************************************
        float[][] depthValues = bufferedImageToFloatMatrix(depthImage);
        int numCols = depthImage.getWidth();
        int numRows = depthImage.getHeight();
        HalfEdgeScene halfEdgeScene = HalfEdgeUtils.getHalfEdgeSceneRectangularNet(numCols, numRows, depthValues, nodeBBox);
        if(halfEdgeScene == null)
        {
            return;
        }

        Path path = Paths.get("noPath");
        halfEdgeScene.setOriginalPath(path);

        // now, do pyramid deformation inverse.***
        GaiaScene restoredGaiaScene = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);
        //GaiaSceneUtils.doPyramidDeformationInverse(restoredGaiaScene, nodeBBox, deformedNodeBBox);

        HalfEdgeScene restoredHalfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(restoredGaiaScene);
        resultHalfEdgeScenes.add(restoredHalfEdgeScene);

        // delete the halfEdgeScene.***
        halfEdgeScene.deleteObjects();

        // delete the restoredGaiaScene.***
        restoredGaiaScene.clear();

    }
}
