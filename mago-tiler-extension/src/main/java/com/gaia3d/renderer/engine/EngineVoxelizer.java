package com.gaia3d.renderer.engine;

import com.gaia3d.renderer.engine.dataStructure.GaiaScenesContainer;
import com.gaia3d.renderer.engine.fbo.FboManager;
import com.gaia3d.renderer.engine.graph.RenderEngine;
import com.gaia3d.renderer.engine.graph.ShaderManager;
import com.gaia3d.renderer.engine.graph.ShaderProgram;
import com.gaia3d.renderer.engine.graph.UniformsMap;
import com.gaia3d.renderer.engine.scene.Camera;
import com.gaia3d.renderer.engine.screen.ScreenQuad;
import com.gaia3d.renderer.renderable.SelectionColorManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;

@Getter
@Setter
@Slf4j
public class EngineVoxelizer {
    private GaiaScenesContainer gaiaScenesContainer;
    private SelectionColorManager selectionColorManager;
    private Window window;
    private ShaderManager shaderManager;
    private RenderEngine renderer;
    private FboManager fboManager;
    private ScreenQuad screenQuad;
    private Camera camera;
    private double midButtonXpos = 0;
    private double midButtonYpos = 0;
    private double leftButtonXpos = 0;
    private double leftButtonYpos = 0;
    private boolean leftButtonClicked = false;
    private boolean midButtonClicked = false;
    private boolean renderAxis = false;
    private int boxRenderingMaxSize = 600;
    private int testsCount = 0;

    public EngineVoxelizer(String windowTitle, Window.WindowOptions opts, IAppLogic appLogic) {
        window = new Window(windowTitle, opts, () -> {
            //resize();
            return null;
        });
    }

    public FboManager getFboManager() {
        if (fboManager == null) {
            fboManager = new FboManager();
        }
        return fboManager;
    }

    public GaiaScenesContainer getGaiaScenesContainer() {
        if (gaiaScenesContainer == null) {
            int windowWidth = window.getWidth();
            int windowHeight = window.getHeight();
            gaiaScenesContainer = new GaiaScenesContainer(windowWidth, windowHeight);
        }
        return gaiaScenesContainer;
    }

    private boolean checkGlError() {
        int glError = GL20.glGetError();
        if (glError != GL20.GL_NO_ERROR) {
            log.error("[ERROR] glError: {}", glError);
            return true;
        }
        return false;
    }

    public void deleteObjects() {
        if(fboManager != null) {
            fboManager.deleteAllFbos();
        }
        if(screenQuad != null) {
            screenQuad.cleanup();
        }
        if(shaderManager != null) {
            shaderManager.deleteAllShaderPrograms();
        }
        if(window != null) {
            window.cleanup();
        }
    }

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (window == null) {
            window = new Window("Mago3D", new Window.WindowOptions(), () -> {
                //resize();
                return null;
            });

            GL.createCapabilities();
        }

        long windowHandle = window.getWindowHandle();

        glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            if (this.midButtonClicked) {
                Vector3d pivot = new Vector3d(0.0d, 0.0d, -1.0d);
                float xoffset = (float) (this.midButtonXpos - xpos) * 0.01f;
                float yoffset = (float) (this.midButtonYpos - ypos) * 0.01f;
                camera.rotationOrbit(xoffset, yoffset, pivot);
            }
            this.midButtonXpos = xpos;
            this.midButtonYpos = ypos;

            if (this.leftButtonClicked) {
                // translate camera
                Vector3d translation = new Vector3d((xpos - this.leftButtonXpos) * 0.01f, (ypos - this.leftButtonYpos) * 0.01f, 0);
                //translation.y *= -1;
                camera.translate(translation);
            }

            this.leftButtonXpos = xpos;
            this.leftButtonYpos = ypos;
        });

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
            camera.moveFront((float) yoffset * 10.0f);
        });

        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            }

            if (key == GLFW_KEY_SPACE && action == GLFW_RELEASE) {
                // keep the camera position and target
                Vector3d keepCameraPosition = new Vector3d(camera.getPosition());
                Vector3d keepCameraDirection = new Vector3d(camera.getDirection());
                Vector3d keepCameraUp = new Vector3d(camera.getUp());

                // do something...

                // restore the camera position and target
                camera.setPosition(keepCameraPosition);
                camera.setDirection(keepCameraDirection);
                camera.setUp(keepCameraUp);
                camera.setDirty(true);

                gaiaScenesContainer.setCamera(camera);
                gaiaScenesContainer.getProjection().setProjectionType(0);
            }


            float rotationOffset = 0.1f;
            Vector3d pivot = new Vector3d(0.0d, 0.0d, -1.0d);

            if (key == GLFW_KEY_W && action == GLFW_RELEASE) {
                //camera.rotationOrbit(0, -rotationOffset, pivot);
                //camera.moveFront(-0.1f);
                this.renderer.setRenderWireFrame(!this.renderer.isRenderWireFrame());
            }
            if (key == GLFW_KEY_A && action == GLFW_RELEASE) {
                camera.rotationOrbit(rotationOffset, 0, pivot);
            }
            if (key == GLFW_KEY_S && action == GLFW_RELEASE) {
                camera.rotationOrbit(0, rotationOffset, pivot);
            }
            if (key == GLFW_KEY_D && action == GLFW_RELEASE) {
                camera.rotationOrbit(-rotationOffset, 0, pivot);
            }
        });

        if (this.shaderManager == null) {
            setupShader();
        }

        if (renderer == null) {
            renderer = new RenderEngine();
        }


        if (camera == null) {
            camera = new Camera();
        }

        if (selectionColorManager == null) {
            selectionColorManager = new SelectionColorManager();
        }

        if (fboManager == null) {
            fboManager = new FboManager();
        }

        if (screenQuad == null) {
            screenQuad = new ScreenQuad();
        }

        if (gaiaScenesContainer == null) {
            int windowWidth = window.getWidth();
            int windowHeight = window.getHeight();
            gaiaScenesContainer = new GaiaScenesContainer(windowWidth, windowHeight);
        }
        gaiaScenesContainer.setCamera(camera);
    }

    private String readResource(String resourceLocation) {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(resourceLocation);
        byte[] bytes = null;
        try {
            bytes = resourceAsStream.readAllBytes();
        } catch (IOException e) {
            log.error("[ERROR] Error reading resource: {}", e);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void setupShader() {
        shaderManager = new ShaderManager();

        GL.createCapabilities();

        URL url = getClass().getClassLoader().getResource("shaders");
        File shaderFolder = new File(url.getPath());

        //log.info("shaderFolder: {}", shaderFolder.getAbsolutePath());


//        log.info("vertexShaderText: {}", vertexShaderText);
//        log.info("fragmentShaderText: {}", fragmentShaderText);

        // create a scene shader program
        String vertexShaderText = readResource("shaders/sceneV330.vert");
        String fragmentShaderText = readResource("shaders/sceneV330.frag");
        java.util.List<ShaderProgram.ShaderModuleData> shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(vertexShaderText, GL20.GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(fragmentShaderText, GL20.GL_FRAGMENT_SHADER));
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

        // create depthShader
        vertexShaderText = readResource("shaders/depthV330.vert");
        fragmentShaderText = readResource("shaders/depthV330.frag");

        shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(vertexShaderText, GL20.GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(fragmentShaderText, GL20.GL_FRAGMENT_SHADER));
        sceneShaderProgram = shaderManager.createShaderProgram("depth", shaderModuleDataList);

        uniformNames = new ArrayList<>();
        uniformNames.add("uProjectionMatrix");
        uniformNames.add("uModelViewMatrix");
        uniformNames.add("uObjectMatrix");
        sceneShaderProgram.createUniforms(uniformNames);
        sceneShaderProgram.validate();

        // create a screen shader program
        vertexShaderText = readResource("shaders/screenV330.vert");
        fragmentShaderText = readResource("shaders/screenV330.frag");

        shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(vertexShaderText, GL20.GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(fragmentShaderText, GL20.GL_FRAGMENT_SHADER));
        ShaderProgram screenShaderProgram = shaderManager.createShaderProgram("screen", shaderModuleDataList);

        uniformNames = new ArrayList<>();
        uniformNames.add("texture0");
        screenShaderProgram.createUniforms(uniformNames);
        screenShaderProgram.validate();

        // create eliminateBackGroundColor shader
        vertexShaderText = readResource("shaders/eliminateBackGroundColorV330.vert");
        fragmentShaderText = readResource("shaders/eliminateBackGroundColorV330.frag");

        shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(vertexShaderText, GL20.GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(fragmentShaderText, GL20.GL_FRAGMENT_SHADER));
        ShaderProgram eliminateBackGroundColorShaderProgram = shaderManager.createShaderProgram("eliminateBackGroundColor", shaderModuleDataList);

        uniformNames = new ArrayList<>();
        uniformNames.add("uScreenWidth");
        uniformNames.add("uScreenHeight");
        uniformNames.add("texture0");
        uniformNames.add("uBackgroundColor");
        eliminateBackGroundColorShaderProgram.createUniforms(uniformNames);
        eliminateBackGroundColorShaderProgram.validate();

        // create a triangles colorCode shader program
        vertexShaderText = readResource("shaders/trianglesColorCodeV330.vert");
        fragmentShaderText = readResource("shaders/trianglesColorCodeV330.frag");
        shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(vertexShaderText, GL20.GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(fragmentShaderText, GL20.GL_FRAGMENT_SHADER));
        ShaderProgram trianglesColorCodeShaderProgram = shaderManager.createShaderProgram("trianglesColorCode", shaderModuleDataList);

        uniformNames = new ArrayList<>();
        uniformNames.add("uProjectionMatrix");
        uniformNames.add("uModelViewMatrix");
        uniformNames.add("uObjectMatrix");
        trianglesColorCodeShaderProgram.createUniforms(uniformNames);
        trianglesColorCodeShaderProgram.validate();

        // create a delimitedScene shader program
        vertexShaderText = readResource("shaders/sceneDelimitedV330.vert");
        fragmentShaderText = readResource("shaders/sceneDelimitedV330.frag");
        shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(vertexShaderText, GL20.GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(fragmentShaderText, GL20.GL_FRAGMENT_SHADER));
        ShaderProgram sceneDelimitedShaderProgram = shaderManager.createShaderProgram("sceneDelimited", shaderModuleDataList);

        uniformNames = new ArrayList<>();
        uniformNames.add("uProjectionMatrix");
        uniformNames.add("uModelViewMatrix");
        uniformNames.add("uObjectMatrix");
        uniformNames.add("texture0");
        uniformNames.add("uColorMode");
        uniformNames.add("uOneColor");
        uniformNames.add("bboxMin");// render only the part of the scene that is inside the bounding box
        uniformNames.add("bboxMax");// render only the part of the scene that is inside the bounding box
        sceneDelimitedShaderProgram.createUniforms(uniformNames);
        sceneDelimitedShaderProgram.validate();

        // create a triangles delimited colorCode shader program
        vertexShaderText = readResource("shaders/trianglesDelimitedColorCodeV330.vert");
        fragmentShaderText = readResource("shaders/trianglesDelimitedColorCodeV330.frag");
        shaderModuleDataList = new ArrayList<>();
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(vertexShaderText, GL20.GL_VERTEX_SHADER));
        shaderModuleDataList.add(new ShaderProgram.ShaderModuleData(fragmentShaderText, GL20.GL_FRAGMENT_SHADER));
        ShaderProgram trianglesDelimitedColorCodeShaderProgram = shaderManager.createShaderProgram("trianglesDelimitedColorCode", shaderModuleDataList);

        uniformNames = new ArrayList<>();
        uniformNames.add("uProjectionMatrix");
        uniformNames.add("uModelViewMatrix");
        uniformNames.add("uObjectMatrix");
        uniformNames.add("bboxMin");// render only the part of the scene that is inside the bounding box
        uniformNames.add("bboxMax");// render only the part of the scene that is inside the bounding box
        trianglesDelimitedColorCodeShaderProgram.createUniforms(uniformNames);
        trianglesDelimitedColorCodeShaderProgram.validate();
    }

    public void getRenderSceneImage(ShaderProgram sceneShaderProgram) {
        // Note : before to call this function, must bind the fbo
        sceneShaderProgram.bind();

        Camera camera = gaiaScenesContainer.getCamera();
        Matrix4d modelViewMatrix = camera.getModelViewMatrix();
        UniformsMap uniformsMap = sceneShaderProgram.getUniformsMap();
        uniformsMap.setUniformMatrix4fv("uModelViewMatrix", new Matrix4f(modelViewMatrix));

        renderer.render(gaiaScenesContainer, sceneShaderProgram);

        sceneShaderProgram.unbind();
    }


}
