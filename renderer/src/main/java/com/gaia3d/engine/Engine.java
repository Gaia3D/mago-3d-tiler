package com.gaia3d.engine;

//import com.gaia3d.converter.*;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.AssimpConverter;
import com.gaia3d.engine.dataStructure.GaiaScenesContainer;
import com.gaia3d.engine.fbo.Fbo;
import com.gaia3d.engine.fbo.FboManager;
import com.gaia3d.engine.graph.RenderEngine;
import com.gaia3d.engine.graph.ShaderManager;
import com.gaia3d.engine.graph.ShaderProgram;
import com.gaia3d.engine.scene.Camera;
import com.gaia3d.engine.screen.ScreenQuad;
import com.gaia3d.renderable.RenderableGaiaScene;
import lombok.Getter;
import org.joml.Vector3d;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;


@Getter
public class Engine {
    private Window window;
    private ShaderManager shaderManager;
    private RenderEngine renderer;
    private FboManager fboManager;
    private ScreenQuad screenQuad;

    private Camera camera;

    GaiaScenesContainer gaiaScenesContainer;

    private double midButtonXpos = 0;
    private double midButtonYpos = 0;
    private double leftButtonXpos = 0;
    private double leftButtonYpos = 0;
    private boolean leftButtonClicked = false;
    private boolean midButtonClicked = false;

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

    public void run() {
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

    private void init() {
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
            camera.moveFront((float)yoffset * 0.2f);
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


        shaderManager = new ShaderManager();
        setupShader();

        renderer = new RenderEngine();

        camera = new Camera();

        fboManager = new FboManager();

        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        fboManager.createFbo("colorRender", windowWidth, windowHeight);

        screenQuad = new ScreenQuad();


        gaiaScenesContainer = new GaiaScenesContainer(windowWidth, windowHeight);
        gaiaScenesContainer.setCamera(camera);

        // Test load a 3d file.***
        String filePath = "D:\\data\\unit-test\\ComplicatedModels25\\DC_library_del_3DS\\DC_library_del.3ds";
        //String filePath = "D:\\data\\unit-test\\ComplicatedModels25\\CK_del_3DS\\CK_del.3ds";
        //String filePath = "D:\\data\\unit-test\\ComplicatedModels25\\dogok_library_del_3DS\\dogok_library_del.3ds";
        Converter assimpConverter = new AssimpConverter();
        List<GaiaScene> gaiaScenes = assimpConverter.load(filePath);
        RenderableGaiaScene renderableGaiaScene = InternDataConverter.getRenderableGaiaScene(gaiaScenes.get(0));
        gaiaScenesContainer.addRenderableGaiaScene(renderableGaiaScene);

    }

    private void setupShader() {
        GL.createCapabilities();

        // create a scene shader program
        List<ShaderProgram.ShaderModuleData> shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("D:/Java_Projects/mago-3d-tiler/renderer/src/main/resources/shaders/sceneV330.vert", GL20.GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("D:/Java_Projects/mago-3d-tiler/renderer/src/main/resources/shaders/sceneV330.frag", GL20.GL_FRAGMENT_SHADER));
        ShaderProgram sceneShaderProgram = shaderManager.createShaderProgram("scene", shaderModuleDataList);


        List<String> uniformNames = new ArrayList<>();
        uniformNames.add("uProjectionMatrix");
        uniformNames.add("uModelViewMatrix");
        uniformNames.add("uObjectMatrix");
        uniformNames.add("texture0");
        uniformNames.add("uColorMode");
        uniformNames.add("uOneColor");
        sceneShaderProgram.createUniforms(uniformNames);
        sceneShaderProgram.validate();
        //sceneShaderProgram.getUniformsMap().setUniform1i("texture0", 0); // texture channel 0

        // create a screen shader program
        shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("D:/Java_Projects/mago-3d-tiler/renderer/src/main/resources/shaders/screenV330.vert", GL20.GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData("D:/Java_Projects/mago-3d-tiler/renderer/src/main/resources/shaders/screenV330.frag", GL20.GL_FRAGMENT_SHADER));
        ShaderProgram screenShaderProgram = shaderManager.createShaderProgram("screen", shaderModuleDataList);

        uniformNames = new ArrayList<>();
        uniformNames.add("texture0");
        screenShaderProgram.createUniforms(uniformNames);
        screenShaderProgram.validate();

    }

    private void draw() {
        // render into frame buffer.***
        Fbo colorRenderFbo = fboManager.getFbo("colorRender");
        colorRenderFbo.bind();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // render scene objects.***
        ShaderProgram sceneShaderProgram = shaderManager.getShaderProgram("scene");
        sceneShaderProgram.bind();
        renderer.render(gaiaScenesContainer, sceneShaderProgram);
        sceneShaderProgram.unbind();

        colorRenderFbo.unbind();

        // now render to windows.***
        int colorRenderTextureId = colorRenderFbo.getColorTextureId();
        GL20.glEnable(GL20.GL_TEXTURE_2D);
        GL20.glActiveTexture(GL20.GL_TEXTURE0);
        GL20.glBindTexture(GL20.GL_TEXTURE_2D, colorRenderTextureId);

        ShaderProgram screenShaderProgram = shaderManager.getShaderProgram("screen");
        screenShaderProgram.bind();
        screenQuad.render();

        screenShaderProgram.unbind();
    }

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.

        // 사용자가 창을 닫거다 esc키를 누를 때까지 랜더링 루프를 실행합니다.
        long windowHandle = window.getWindowHandle();
        while (!glfwWindowShouldClose(windowHandle)) {
            int[] width = new int[1];
            int[] height = new int[1];
            glfwGetWindowSize(windowHandle, width, height);
            glViewport(0, 0, width[0], height[0]);

            glEnable(GL_DEPTH_TEST);
            glPointSize(5.0f);
            // 클리어 컬러를 적용합니다.
            glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
            glClearDepth(1.0f);
            // 프레임 버퍼 클리어
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            draw();
            // 색상버퍼 교체
            glfwSwapBuffers(windowHandle);
            // 이벤트를 폴링상태로 둡니다. key 콜백이 실행되려면 폴링상태가 활성화 되어있어야 합니다.
            glfwPollEvents();
        }
    }

}
