package com.gaia3d.renderer.engine;

import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.geometry.tessellator.GaiaTessellator;
import com.gaia3d.basic.geometry.tessellator.Point2DTess;
import com.gaia3d.basic.geometry.tessellator.Polygon2DTess;
import com.gaia3d.basic.model.GaiaMesh;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaPrimitive;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.renderer.engine.dataStructure.GaiaScenesContainer;
import com.gaia3d.renderer.engine.fbo.Fbo;
import com.gaia3d.renderer.engine.fbo.FboManager;
import com.gaia3d.renderer.engine.graph.RenderEngine;
import com.gaia3d.renderer.engine.graph.ShaderManager;
import com.gaia3d.renderer.engine.graph.ShaderProgram;
import com.gaia3d.renderer.engine.graph.UniformsMap;
import com.gaia3d.renderer.engine.scene.Camera;
import com.gaia3d.renderer.engine.screen.ScreenQuad;
import com.gaia3d.renderer.renderable.RenderableGaiaScene;
import com.gaia3d.renderer.renderable.RenderablePrimitive;
import com.gaia3d.renderer.renderable.SelectionColorManager;
import lombok.Getter;
import lombok.Setter;
import org.joml.*;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.opengl.GL11.*;

@Getter
@Setter
public class Engine {
    private Window window;
    private ShaderManager shaderManager;
    private RenderEngine renderer;
    private FboManager fboManager;
    private ScreenQuad screenQuad;

    private Camera camera;

    GaiaScenesContainer gaiaScenesContainer;
    SelectionColorManager selectionColorManager;

    private double midButtonXpos = 0;
    private double midButtonYpos = 0;
    private double leftButtonXpos = 0;
    private double leftButtonYpos = 0;
    private boolean leftButtonClicked = false;
    private boolean midButtonClicked = false;
    private boolean renderAxis = false;

    private boolean checkGlError()
    {
        int glError = GL20.glGetError();
        if(glError != GL20.GL_NO_ERROR) {
            System.out.println("glError: " + glError);
            return true;
        }
        return false;
    }

    public Engine(String windowTitle, Window.WindowOptions opts, IAppLogic appLogic) {
        window = new Window(windowTitle, opts, () -> {
            resize();
            return null;
        });

    }

    private void resize() {
        //scene.resize(window.getWidth(), window.getHeight());
    }

    public void run() throws IOException {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        init();
        loop();

        // 창의 콜백을 해제하고 창을 삭제합니다.
        long windowHandle = window.getWindowHandle();
        glfwFreeCallbacks(windowHandle);
        glfwDestroyWindow(windowHandle);

        // GLFW를 종료하고 에러 콜백을 해제합니다.
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public void getRenderSceneImage(ShaderProgram sceneShaderProgram)
    {
        //***********************************************************
        // Note : before to call this function, must bind the fbo.***
        //***********************************************************
        sceneShaderProgram.bind();

        Camera camera = gaiaScenesContainer.getCamera();
        Matrix4d modelViewMatrix = camera.getModelViewMatrix();
        UniformsMap uniformsMap = sceneShaderProgram.getUniformsMap();
        uniformsMap.setUniformMatrix4fv("uModelViewMatrix", new Matrix4f(modelViewMatrix));

        renderer.render(gaiaScenesContainer, sceneShaderProgram);

        sceneShaderProgram.unbind();
    }

    public void init() {
        // 에러 콜백을 설정합니다. System.err의 에러 메세지를 출력 기본으로 구현합니다.
        GLFWErrorCallback.createPrint(System.err).set();


        long windowHandle = window.getWindowHandle();
        // 마우스 위치 콜백
        glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            if (this.midButtonClicked) {
                Vector3d pivot = new Vector3d(0.0d,0.0d,-1.0d);
                float xoffset = (float) (this.midButtonXpos - xpos) * 0.01f;
                float yoffset = (float) (this.midButtonYpos - ypos) * 0.01f;
                camera.rotationOrbit(xoffset, yoffset, pivot);
            }
            this.midButtonXpos = xpos;
            this.midButtonYpos = ypos;

            if(this.leftButtonClicked)
            {
                // translate camera
                Vector3d translation = new Vector3d((xpos - this.leftButtonXpos) * 0.01f, (ypos - this.leftButtonYpos) * 0.01f, 0);
                //translation.y *= -1;
                camera.translate(translation);
            }

            this.leftButtonXpos = xpos;
            this.leftButtonYpos = ypos;
        });


        // 마우스 버튼 이벤트
        glfwSetMouseButtonCallback(windowHandle, (window, key, action, mode) -> {
            if (key == GLFW_MOUSE_BUTTON_3 && action == GLFW_PRESS) {
                this.midButtonClicked = true;
            } else if (key == GLFW_MOUSE_BUTTON_3 && action == GLFW_RELEASE) {
                this.midButtonClicked = false;
            }

            // check left button
            if (key == GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS) {
                this.leftButtonClicked = true;
            } else if (key == GLFW_MOUSE_BUTTON_1 && action == GLFW_RELEASE) {
                this.leftButtonClicked = false;
            }

        });

        glfwSetScrollCallback(windowHandle, (window, xoffset, yoffset) -> {
            camera.moveFront((float)yoffset * 10.0f);
        });

        // 키보드 콜백 이벤트를 설정합니다. 키를 눌렀을 때, 누르고 있을 때, 떼었을 때에 따라 바꿔줍니다.
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            }

            float rotationOffset = 0.1f;
            Vector3d pivot = new Vector3d(0.0d,0.0d,-1.0d);
            if (key == GLFW_KEY_W) {
                //camera.rotationOrbit(0, -rotationOffset, pivot);
                camera.moveFront(-0.1f);
            }
            if (key == GLFW_KEY_A) {
                camera.rotationOrbit(rotationOffset, 0, pivot);
            }
            if (key == GLFW_KEY_S) {
                camera.rotationOrbit(0, rotationOffset, pivot);
            }
            if (key == GLFW_KEY_D) {
                camera.rotationOrbit(-rotationOffset, 0, pivot);
            }
        });

        setupShader();

        if(renderer == null)
        {
            renderer = new RenderEngine();
        }

        if(camera == null)
        {
            camera = new Camera();
        }

        if(selectionColorManager == null)
        {
            selectionColorManager = new SelectionColorManager();
        }

        if(fboManager == null)
        {
            fboManager = new FboManager();
            int windowWidth = window.getWidth();
            int windowHeight = window.getHeight();
            fboManager.createFbo("colorRender", windowWidth, windowHeight);

            // now, create a 500 x 500 fbo for colorCode render.***
            fboManager.createFbo("colorCodeRender", 500, 500);
        }

        if(screenQuad == null) {
            screenQuad = new ScreenQuad();
        }

        if(gaiaScenesContainer == null) {
            int windowWidth = window.getWidth();
            int windowHeight = window.getHeight();
            gaiaScenesContainer = new GaiaScenesContainer(windowWidth, windowHeight);
        }
        gaiaScenesContainer.setCamera(camera);


        int hola2 = 0;
    }


    private void setupShader() {
        shaderManager = new ShaderManager();

        GL.createCapabilities();
        String shaderFolder = "D:/Java_Projects/mago-3d-tiler/extension/src/main/resources/shaders/";

        // create a scene shader program.*************************************************************************************************
        java.util.List<ShaderProgram.ShaderModuleData> shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(shaderFolder + "sceneV330.vert", GL20.GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(shaderFolder + "sceneV330.frag", GL20.GL_FRAGMENT_SHADER));
        ShaderProgram sceneShaderProgram = shaderManager.createShaderProgram("scene", shaderModuleDataList);


        java.util.List<String> uniformNames = new ArrayList<>();
        uniformNames.add("uProjectionMatrix");
        uniformNames.add("uModelViewMatrix");
        uniformNames.add("uObjectMatrix");
        uniformNames.add("texture0");
        uniformNames.add("uColorMode");
        uniformNames.add("uOneColor");
        sceneShaderProgram.createUniforms(uniformNames);
        sceneShaderProgram.validate();
        //sceneShaderProgram.getUniformsMap().setUniform1i("texture0", 0); // texture channel 0

        // create a screen shader program.*************************************************************************************************
        shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(shaderFolder + "screenV330.vert", GL20.GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(shaderFolder + "screenV330.frag", GL20.GL_FRAGMENT_SHADER));
        ShaderProgram screenShaderProgram = shaderManager.createShaderProgram("screen", shaderModuleDataList);

        uniformNames = new ArrayList<>();
        uniformNames.add("texture0");
        screenShaderProgram.createUniforms(uniformNames);
        screenShaderProgram.validate();

        // create a colorCode shader program.****************************************************************************************************
        shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(shaderFolder + "colorCodeV330.vert", GL20.GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(shaderFolder + "colorCodeV330.frag", GL20.GL_FRAGMENT_SHADER));
        ShaderProgram colorCodeShaderProgram = shaderManager.createShaderProgram("colorCode", shaderModuleDataList);

        uniformNames = new ArrayList<>();
        uniformNames.add("uProjectionMatrix");
        uniformNames.add("uModelViewMatrix");
        uniformNames.add("uObjectMatrix");
        uniformNames.add("uColorCode");
        colorCodeShaderProgram.createUniforms(uniformNames);
        colorCodeShaderProgram.validate();

        // create depthShader.****************************************************************************************************
        shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(shaderFolder + "depthV330.vert", GL20.GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(shaderFolder + "depthV330.frag", GL20.GL_FRAGMENT_SHADER));
        sceneShaderProgram = shaderManager.createShaderProgram("depth", shaderModuleDataList);

        uniformNames = new ArrayList<>();
        uniformNames.add("uProjectionMatrix");
        uniformNames.add("uModelViewMatrix");
        uniformNames.add("uObjectMatrix");
        sceneShaderProgram.createUniforms(uniformNames);
        sceneShaderProgram.validate();
    }

    private void takeColorCodedPhoto(RenderableGaiaScene renderableGaiaScene, Fbo fbo, ShaderProgram shaderProgram)
    {
        fbo.bind();
        glViewport(0, 0, fbo.getFboWidth(), fbo.getFboHeight()); // 500 x 500
        glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);

        // render scene objects.***
        shaderProgram.bind();

        Camera camera = gaiaScenesContainer.getCamera();
        Matrix4d modelViewMatrix = camera.getModelViewMatrix();
        UniformsMap uniformsMap = shaderProgram.getUniformsMap();
        uniformsMap.setUniformMatrix4fv("uModelViewMatrix", new Matrix4f(modelViewMatrix));

        // disable cull face.***
        glDisable(GL_CULL_FACE);
        renderer.renderColorCoded(renderableGaiaScene, selectionColorManager, shaderProgram);
        shaderProgram.unbind();

        fbo.unbind();

        // return the viewport to window size.***
        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        glViewport(0, 0, windowWidth, windowHeight);
    }

    private void determineExteriorAndInteriorObjects(Fbo fbo)
    {
        // bind the fbo.***
        fbo.bind();

        // read pixels from fbo.***
        int fboWidth = fbo.getFboWidth();
        int fboHeight = fbo.getFboHeight();
        ByteBuffer pixels = ByteBuffer.allocateDirect(fboWidth * fboHeight * 4);
        glReadPixels(0, 0, fboWidth, fboHeight, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

        // unbind the fbo.***
        fbo.unbind();

        // determine exterior and interior objects.***
        int pixelsCount = fboWidth * fboHeight;
        for(int i=0; i<pixelsCount; i++)
        {
            int colorCode = pixels.getInt(i * 4);
            // background color is (1, 1, 1, 1). skip background color.***
            if(colorCode == 0xFFFFFFFF)
            {
                continue;
            }
            RenderablePrimitive renderablePrimitive = (RenderablePrimitive) selectionColorManager.mapColorRenderable.get(colorCode);
            if(renderablePrimitive != null)
            {
                // determine exterior or interior.***
                // 0 = interior, 1 = exterior, -1 = unknown.***
                renderablePrimitive.setStatus(1);
            }
        }
    }

    private RenderableGaiaScene processExteriorInterior(GaiaScene gaiaScene)
    {
        RenderableGaiaScene renderableGaiaScene = InternDataConverter.getRenderableGaiaScene(gaiaScene);
        gaiaScenesContainer.addRenderableGaiaScene(renderableGaiaScene);
        GaiaBoundingBox bbox = gaiaScene.getBoundingBox();
        float maxLength = (float)bbox.getLongestDistance();
        float bboxHight = (float)bbox.getMaxZ() - (float)bbox.getMinZ();
        float semiMaxLength = maxLength / 2.0f;
        semiMaxLength *= 150.0f;

        // render into frame buffer.***
        Fbo colorRenderFbo = fboManager.getFbo("colorCodeRender");

        // render scene objects.***
        ShaderProgram sceneShaderProgram = shaderManager.getShaderProgram("colorCode");
        sceneShaderProgram.bind();
        Matrix4f projectionOrthoMatrix = new Matrix4f().ortho(-semiMaxLength, semiMaxLength, -semiMaxLength, semiMaxLength, -semiMaxLength * 10.0f, semiMaxLength * 10.0f);

        // make colorRenderableMap.***
        java.util.List<RenderablePrimitive> allRenderablePrimitives = new ArrayList<>();
        renderableGaiaScene.extractRenderablePrimitives(allRenderablePrimitives);
        int renderablePrimitivesCount = allRenderablePrimitives.size();
        for(int i=0; i<renderablePrimitivesCount; i++)
        {
            RenderablePrimitive renderablePrimitive = allRenderablePrimitives.get(i);
            renderablePrimitive.setStatus(0); // init as interior.***
            int colorCode = selectionColorManager.getAvailableColor();
            renderablePrimitive.setColorCode(colorCode);
            selectionColorManager.mapColorRenderable.put(colorCode, renderablePrimitive);
        }

        UniformsMap uniformsMap = sceneShaderProgram.getUniformsMap();
        uniformsMap.setUniformMatrix4fv("uProjectionMatrix", projectionOrthoMatrix);

        // take 8 photos at the top, 8 photos at lateral, 8 photos at the bottom.***
        // top photos.***
        Camera camera = gaiaScenesContainer.getCamera();
        double increAngRad = Math.toRadians(360.0 / 8.0);
        Matrix4d rotMat = new Matrix4d();
        rotMat.rotateZ(increAngRad);
        Vector3d cameraPosition = new Vector3d(0, -semiMaxLength, semiMaxLength);
        Vector3d cameraTarget = new Vector3d(0, 0, 0);

        for(int i=0; i<8; i++)
        {
            // set camera position.***
            camera.calculateCameraXYPlane(cameraPosition, cameraTarget);

            takeColorCodedPhoto(renderableGaiaScene, colorRenderFbo, sceneShaderProgram);
            determineExteriorAndInteriorObjects(colorRenderFbo);

            // rotate camPos.
            rotMat.transformPosition(cameraPosition.x, cameraPosition.y, cameraPosition.z, cameraPosition);
        }

        // lateral photos.***
        cameraPosition = new Vector3d(0, -semiMaxLength, 0);
        cameraTarget = new Vector3d(0, 0, 0);
        for(int i=0; i<8; i++)
        {
            // set camera position.***
            camera.calculateCameraXYPlane(cameraPosition, cameraTarget);

            takeColorCodedPhoto(renderableGaiaScene, colorRenderFbo, sceneShaderProgram);
            determineExteriorAndInteriorObjects(colorRenderFbo);

            // rotate camPos.
            rotMat.transformPosition(cameraPosition.x, cameraPosition.y, cameraPosition.z, cameraPosition);
        }

        // bottom photos.***
        cameraPosition = new Vector3d(0, -semiMaxLength, -semiMaxLength);
        cameraTarget = new Vector3d(0, 0, 0);
        for(int i=0; i<8; i++)
        {
            // set camera position.***
            camera.calculateCameraXYPlane(cameraPosition, cameraTarget);

            takeColorCodedPhoto(renderableGaiaScene, colorRenderFbo, sceneShaderProgram);
            determineExteriorAndInteriorObjects(colorRenderFbo);

            // rotate camPos.
            rotMat.transformPosition(cameraPosition.x, cameraPosition.y, cameraPosition.z, cameraPosition);
        }

        // return camera position.***
        cameraPosition = new Vector3d(0, 0, -semiMaxLength);
        cameraTarget = new Vector3d(0, 0, 0);

        // set camera position.***
        camera.calculateCameraXYPlane(cameraPosition, cameraTarget);


        return renderableGaiaScene;
    }

    public Map<GaiaPrimitive, Integer> getExteriorAndInteriorGaiaPrimitivesMap(GaiaScene gaiaScene, Map<GaiaPrimitive, Integer> mapPrimitiveStatus)
    {
        RenderableGaiaScene renderableGaiaScene = processExteriorInterior(gaiaScene);

        java.util.List<RenderablePrimitive> allRenderablePrimitives = new ArrayList<>();
        renderableGaiaScene.extractRenderablePrimitives(allRenderablePrimitives);
        int renderablePrimitivesCount = allRenderablePrimitives.size();

        // finally make exteriorGaiaSet & interiorGaiaSet.***
        if(mapPrimitiveStatus == null)
        {
            mapPrimitiveStatus = new HashMap<>();
        }
        else
        {
            mapPrimitiveStatus.clear();
        }

        for(int i=0; i<renderablePrimitivesCount; i++)
        {
            RenderablePrimitive renderablePrimitive = allRenderablePrimitives.get(i);
            int status = renderablePrimitive.getStatus();
            if(status == 1)
            {
                mapPrimitiveStatus.put(renderablePrimitive.getOriginalGaiaPrimitive(), 1);
            }
            else if(status == 0)
            {
                mapPrimitiveStatus.put(renderablePrimitive.getOriginalGaiaPrimitive(), 0);
            }
        }

        return mapPrimitiveStatus;
    }

    private void deletePrimitivesByStatus(GaiaNode gaiaNode, int statusToDelete, Map<GaiaPrimitive, Integer> mapPrimitiveStatus)
    {
        java.util.List<GaiaMesh> gaiaMeshes = gaiaNode.getMeshes();
        int meshesCount = gaiaMeshes.size();
        for(int i=0; i<meshesCount; i++)
        {
            GaiaMesh gaiaMesh = gaiaMeshes.get(i);
            java.util.List<GaiaPrimitive> gaiaPrimitives = gaiaMesh.getPrimitives();
            int primitivesCount = gaiaPrimitives.size();
            for(int j=0; j<primitivesCount; j++)
            {
                GaiaPrimitive gaiaPrimitive = gaiaPrimitives.get(j);
                int status = mapPrimitiveStatus.get(gaiaPrimitive);
                if(status == statusToDelete)
                {
                    gaiaPrimitives.remove(j);
                    j--;
                    primitivesCount--;
                }
            }

            // check if the gaiaMesh has no primitives.***
            if(gaiaPrimitives.size() == 0)
            {
                gaiaMeshes.remove(i);
                i--;
                meshesCount--;
            }
        }

        java.util.List<GaiaNode> children = gaiaNode.getChildren();
        int childrenCount = children.size();
        for(int i=0; i<childrenCount; i++)
        {
            GaiaNode child = children.get(i);
            deletePrimitivesByStatus(child, statusToDelete, mapPrimitiveStatus);
        }
    }


    public void getExteriorAndInteriorGaiaScenes(GaiaScene gaiaScene, java.util.List<GaiaScene> resultExteriorGaiaScenes, java.util.List<GaiaScene> resultInteriorGaiaScenes) {
        Map<GaiaPrimitive, Integer> mapPrimitiveStatus = getExteriorAndInteriorGaiaPrimitivesMap(gaiaScene, null);

        // finally make exteriorGaiaSet & interiorGaiaSet.***
        GaiaScene exteriorGaiaScene = gaiaScene.clone();
        GaiaScene interiorGaiaScene = gaiaScene.clone();
        resultExteriorGaiaScenes.add(exteriorGaiaScene);
        resultInteriorGaiaScenes.add(interiorGaiaScene);

        // delete interior primitives from exteriorGaiaScene, and delete exterior primitives from interiorGaiaScene.***
        java.util.List<GaiaNode> exteriorNodes = exteriorGaiaScene.getNodes();
        int extNodesCount = exteriorNodes.size();
        for(int i=0; i<extNodesCount; i++)
        {
            GaiaNode gaiaNode = exteriorNodes.get(i);
            deletePrimitivesByStatus(gaiaNode, 0, mapPrimitiveStatus);
        }

        java.util.List<GaiaNode> interiorNodes = interiorGaiaScene.getNodes();
        int intNodesCount = interiorNodes.size();
        for(int i=0; i<intNodesCount; i++)
        {
            GaiaNode gaiaNode = interiorNodes.get(i);
            deletePrimitivesByStatus(gaiaNode, 1, mapPrimitiveStatus);
        }
    }

    public void getExteriorAndInteriorGaiaSets(GaiaScene gaiaScene, java.util.List<GaiaSet> resultExteriorGaiaSets, java.util.List<GaiaSet> resultInteriorGaiaSets)
    {
        RenderableGaiaScene renderableGaiaScene = processExteriorInterior(gaiaScene);

        java.util.List<RenderablePrimitive> allRenderablePrimitives = new ArrayList<>();
        renderableGaiaScene.extractRenderablePrimitives(allRenderablePrimitives);
        int renderablePrimitivesCount = allRenderablePrimitives.size();

        // finally make exteriorGaiaSet & interiorGaiaSet.***
        GaiaSet exteriorGaiaSet = new GaiaSet();
        GaiaSet interiorGaiaSet = new GaiaSet();
        resultExteriorGaiaSets.add(exteriorGaiaSet);
        resultInteriorGaiaSets.add(interiorGaiaSet);
        java.util.List<GaiaBufferDataSet> exteriorBufferDatas = new ArrayList<>();
        List<GaiaBufferDataSet> interiorBufferDatas = new ArrayList<>();
        exteriorGaiaSet.setBufferDataList(exteriorBufferDatas);
        interiorGaiaSet.setBufferDataList(interiorBufferDatas);
        for(int i=0; i<renderablePrimitivesCount; i++)
        {
            RenderablePrimitive renderablePrimitive = allRenderablePrimitives.get(i);
            int status = renderablePrimitive.getStatus();
            if(status == 1)
            {
                GaiaBufferDataSet gaiaBufferDataSet = renderablePrimitive.getOriginalBufferDataSet();
                exteriorBufferDatas.add(gaiaBufferDataSet);
            }
            else if(status == 0)
            {
                GaiaBufferDataSet gaiaBufferDataSet = renderablePrimitive.getOriginalBufferDataSet();
                interiorBufferDatas.add(gaiaBufferDataSet);
            }
        }
    }

    private void renderScreenQuad(int texId)
    {
        // render to windows using screenQuad.***
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0); // any frame buffer binded.***

        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        glViewport(0, 0, windowWidth, windowHeight);

        GL20.glEnable(GL20.GL_TEXTURE_2D);
        GL20.glActiveTexture(GL20.GL_TEXTURE0);
        GL20.glBindTexture(GL20.GL_TEXTURE_2D, texId);

        ShaderProgram screenShaderProgram = shaderManager.getShaderProgram("screen");
        screenShaderProgram.bind();
        screenQuad.render();
        screenShaderProgram.unbind();
    }

    private void draw() {
        // render into frame buffer.***
        Fbo colorRenderFbo = fboManager.getFbo("colorRender");
        colorRenderFbo.bind();

        int[] width = new int[1];
        int[] height = new int[1];
        width[0] = colorRenderFbo.getFboWidth();
        height[0] = colorRenderFbo.getFboHeight();
        glfwGetWindowSize(window.getWindowHandle(), width, height);
        glViewport(0, 0, width[0], height[0]);

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // render scene objects.***
        ShaderProgram sceneShaderProgram = shaderManager.getShaderProgram("scene");
        sceneShaderProgram.bind();
        if(renderAxis)
        {
            renderer.renderAxis(sceneShaderProgram);
        }
        renderer.render(gaiaScenesContainer, sceneShaderProgram);
        sceneShaderProgram.unbind();

        colorRenderFbo.unbind();

        // now render to windows using screenQuad.***
        int colorRenderTextureId = colorRenderFbo.getColorTextureId();
        renderScreenQuad(colorRenderTextureId);


        // render colorCoded fbo.***
//        int colorCodeRenderTextureId = fboManager.getFbo("colorCodeRender").getColorTextureId();
//        renderScreenQuad(colorCodeRenderTextureId);

    }

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.

        // 사용자가 창을 닫거다 esc키를 누를 때까지 랜더링 루프를 실행합니다.
        int[] width = new int[1];
        int[] height = new int[1];
        long windowHandle = window.getWindowHandle();
        while (!glfwWindowShouldClose(windowHandle)) {

            glfwGetWindowSize(windowHandle, width, height);
            glViewport(0, 0, width[0], height[0]);

            glEnable(GL_DEPTH_TEST);
            glPointSize(5.0f);
            // 클리어 컬러를 적용합니다.
            glClearColor(0.5f, 0.23f, 0.98f, 1.0f);
            glClearDepth(1.0f);
            // 프레임 버퍼 클리어
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            draw();
            // 색상버퍼 교체
            glfwSwapBuffers(windowHandle);
            // 이벤트를 폴링상태로 둡니다. key 콜백이 실행되려면 폴링상태가 활성화 되어있어야 합니다.
            glfwPollEvents();
        }
    }

    public void deleteBuffer(int vboId)
    {
        GL20.glDeleteBuffers(vboId);
    }

}
