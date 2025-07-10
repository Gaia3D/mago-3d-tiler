package com.gaia3d.renderer.engine;

import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.halfedge.*;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.renderer.engine.dataStructure.FaceVisibilityData;
import com.gaia3d.renderer.engine.dataStructure.FaceVisibilityDataManager;
import com.gaia3d.renderer.engine.dataStructure.GaiaScenesContainer;
import com.gaia3d.renderer.engine.fbo.Fbo;
import com.gaia3d.renderer.engine.fbo.FboManager;
import com.gaia3d.renderer.engine.graph.*;
import com.gaia3d.renderer.engine.scene.Camera;
import com.gaia3d.renderer.engine.scene.Projection;
import com.gaia3d.renderer.engine.screen.ScreenQuad;
import com.gaia3d.renderer.renderable.RenderableGaiaScene;
import com.gaia3d.renderer.renderable.RenderablePrimitive;
import com.gaia3d.renderer.renderable.SelectionColorManager;
import com.gaia3d.util.GaiaColorUtils;
import com.gaia3d.util.GaiaSceneUtils;
import com.gaia3d.util.GaiaTextureUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.*;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Math;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

@SuppressWarnings("ALL")
@Getter
@Setter
@Slf4j
public class Engine {
    private GaiaScenesContainer gaiaScenesContainer;
    private SelectionColorManager selectionColorManager;
    private List<HalfEdgeScene> halfEdgeScenes = new ArrayList<>();
    private List<GaiaScene> gaiaScenes = new ArrayList<>();
    private Window window;
    private ShaderManager shaderManager;
    private RenderEngine renderer;
    private HalfEdgeRenderer halfEdgeRenderer;
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

    public Engine(String windowTitle, Window.WindowOptions opts, IAppLogic appLogic) {
        window = new Window(windowTitle, opts, () -> {
            resize();
            return null;
        });

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
        if (fboManager != null) {
            fboManager.deleteAllFbos();
        }
        if (screenQuad != null) {
            screenQuad.cleanup();
        }
        if (shaderManager != null) {
            shaderManager.deleteAllShaderPrograms();
        }
        if (window != null) {
            window.cleanup();
        }
    }


    private void resize() {
        //scene.resize(window.getWidth(), window.getHeight());
    }

    public void run() throws IOException {
        log.info("Hello LWJGL " + Version.getVersion() + "!");

        init();
        loop();

        long windowHandle = window.getWindowHandle();
        glfwFreeCallbacks(windowHandle);
        glfwDestroyWindow(windowHandle);

        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public GaiaScenesContainer getGaiaScenesContainer() {
        if (gaiaScenesContainer == null) {
            int windowWidth = window.getWidth();
            int windowHeight = window.getHeight();
            gaiaScenesContainer = new GaiaScenesContainer(windowWidth, windowHeight);
        }
        return gaiaScenesContainer;
    }

    public FboManager getFboManager() {
        if (fboManager == null) {
            fboManager = new FboManager();
        }
        return fboManager;
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

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (window == null) {
            window = new Window("Mago3D", new Window.WindowOptions(), () -> {
                resize();
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

            if (key == GLFW_KEY_P && action == GLFW_RELEASE) {
                // pyramid deformation
                // keep the camera position and target
                Vector3d keepCameraPosition = new Vector3d(camera.getPosition());
                Vector3d keepCameraDirection = new Vector3d(camera.getDirection());
                Vector3d keepCameraUp = new Vector3d(camera.getUp());

                // make a depthMap and normalMap

                // do pyramidDeformation
                GaiaScene gaiaScene = gaiaScenes.get(0);
                GaiaBoundingBox bbox = gaiaScene.updateBoundingBox(); // before to set the transformMatrix
                double minH = bbox.getMinZ();
                double maxH = bbox.getMaxZ() * 1.1;
                double dist = 6.0;
                GaiaSceneUtils.deformSceneByVerticesConvexity(gaiaScene, dist, minH, maxH);

                // now, update the renderableScene
                InternDataConverter internDataConverter = new InternDataConverter();
                RenderableGaiaScene renderableScene = InternDataConverter.getRenderableGaiaScene(gaiaScene);
                this.getGaiaScenesContainer().getRenderableGaiaScenes().set(0, renderableScene);

                // restore the camera position and target
                camera.setPosition(keepCameraPosition);
                camera.setDirection(keepCameraDirection);
                camera.setUp(keepCameraUp);
                camera.setDirty(true);

                gaiaScenesContainer.setCamera(camera);
                gaiaScenesContainer.getProjection().setProjectionType(0);
            }

            if (key == GLFW_KEY_E && action == GLFW_RELEASE) {
                // Eliminate the background color
                // keep the camera position and target
                Vector3d keepCameraPosition = new Vector3d(camera.getPosition());
                Vector3d keepCameraDirection = new Vector3d(camera.getDirection());
                Vector3d keepCameraUp = new Vector3d(camera.getUp());

                // make a depthMap and normalMap

                // do pyramidDeformation
                GaiaScene gaiaScene = gaiaScenes.get(0);
                GaiaBoundingBox bbox = gaiaScene.updateBoundingBox(); // before to set the transformMatrix
                double bboxMaxSize = bbox.getMaxSize();

                List<HalfEdgeScene> resultHalfEdgeScenes = new ArrayList<>();
                double pixelsForMeter = 128.0 / bboxMaxSize;
                DecimateParameters decimateParameters = new DecimateParameters();
                decimateParameters.setBasicValues(10.0, 1.0, 3.0, 10.0, 1000000, 1, 0.5);
                //this.makeNetSurfaces_TEST(gaiaScenes, resultHalfEdgeScenes, decimateParameters, pixelsForMeter);

                HalfEdgeScene halfEdgeSceneResult = resultHalfEdgeScenes.get(0);
                this.getHalfEdgeScenes().clear();
                this.getHalfEdgeScenes().add(halfEdgeSceneResult);

                GaiaScene gaiaSceneResult = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeSceneResult);

                // now, update the renderableScene
                InternDataConverter internDataConverter = new InternDataConverter();
                RenderableGaiaScene renderableScene = InternDataConverter.getRenderableGaiaScene(gaiaSceneResult);
                this.getGaiaScenesContainer().getRenderableGaiaScenes().set(0, renderableScene);

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
            if (key == GLFW_KEY_C && action == GLFW_RELEASE) {
                int colorType = renderer.getColorMode();
                colorType++;
                if (colorType > 2) {
                    colorType = 0;
                }
                renderer.setColorMode(colorType);

                colorType = halfEdgeRenderer.getColorMode();
                colorType++;
                if (colorType > 2) {
                    colorType = 0;
                }
                halfEdgeRenderer.setColorMode(colorType);
            }
            if (key == GLFW_KEY_W && action == GLFW_RELEASE) {
                //camera.rotationOrbit(0, -rotationOffset, pivot);
                //camera.moveFront(-0.1f);
                this.halfEdgeRenderer.setRenderWireFrame(!this.halfEdgeRenderer.isRenderWireFrame());
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

        if (halfEdgeRenderer == null) {
            halfEdgeRenderer = new HalfEdgeRenderer();
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

    private RenderableGaiaScene getColorCodedRenderableScene(GaiaScene gaiaScene) {
        // 1rst, unWeld the vertices
        gaiaScene.unWeldVertices();

        // now, make a map<GaiaFace, Integer> for the faces
        List<GaiaFace> faces = gaiaScene.extractGaiaFaces(null);
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            GaiaFace face = faces.get(i);
            face.setId(i);
        }

        List<GaiaPrimitive> primitives = gaiaScene.extractPrimitives(null);
        int primitivesCount = primitives.size();
        for (int i = 0; i < primitivesCount; i++) {
            GaiaPrimitive primitive = primitives.get(i);
            List<GaiaFace> primitiveFaces = primitive.extractGaiaFaces(null);
            for (GaiaFace face : primitiveFaces) {
                int faceId = face.getId();
                byte[] byteColor = GaiaColorUtils.decodeColor4(faceId);
                int[] faceIndices = face.getIndices();
                for (int index : faceIndices) {
                    GaiaVertex vertex = primitive.getVertices().get(index);
                    vertex.setColor(byteColor);
                }
            }
        }

        RenderableGaiaScene renderableSceneColorCoded = InternDataConverter.getRenderableGaiaScene(gaiaScene);

        return renderableSceneColorCoded;
    }

    public BufferedImage eliminateBackGroundColor(BufferedImage originalImage) {
        if (originalImage == null) return null;

        int fboWidth = originalImage.getWidth();
        int fboHeight = originalImage.getHeight();
        if (fboWidth <= 0 || fboHeight <= 0) return null;

        try {
            Fbo fbo = fboManager.getOrCreateFbo("default", fboWidth, fboHeight);
            fbo.bind();

            int[] width = new int[1];
            int[] height = new int[1];
            width[0] = fbo.getFboWidth();
            height[0] = fbo.getFboHeight();

            glViewport(0, 0, width[0], height[0]);
            glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
            glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
            glDisable(GL20.GL_DEPTH_TEST);

            // enable cull face
            glEnable(GL20.GL_CULL_FACE);

            int minFilter = GL20.GL_NEAREST; // GL_LINEAR, GL_NEAREST
            int magFilter = GL20.GL_NEAREST;
            int wrapS = GL20.GL_REPEAT; // GL_CLAMP_TO_EDGE
            int wrapT = GL20.GL_REPEAT;
            int iterationsCount = 10;
            BufferedImage image = originalImage;
            boolean resizeToPowerOf2 = false;

            GL20.glEnable(GL20.GL_TEXTURE_2D);
            GL20.glActiveTexture(GL20.GL_TEXTURE0);

            // shader program
            ShaderManager shaderManager = getShaderManager();
            ShaderProgram shaderProgram = shaderManager.getShaderProgram("eliminateBackGroundColor");

            shaderProgram.bind();
            // set uniforms
            UniformsMap uniformsMap = shaderProgram.getUniformsMap();
            uniformsMap.setUniform1i("uTexture", 0);
            uniformsMap.setUniform1f("uScreenWidth", (float) fboWidth);
            uniformsMap.setUniform1f("uScreenHeight", (float) fboHeight);
            uniformsMap.setUniform3fv("uBackgroundColor", new Vector3f(0.5f, 0.5f, 0.5f));

            int bufferedImageType = BufferedImage.TYPE_INT_ARGB;

            for (int i = 0; i < iterationsCount; i++) {
                // create the texture
                int textureId = RenderableTexturesUtils.createGlTextureFromBufferedImage(image, minFilter, magFilter, wrapS, wrapT, resizeToPowerOf2);
                GL20.glBindTexture(GL20.GL_TEXTURE_2D, textureId);

                screenQuad.render();

                // make the bufferImage
                image = fbo.getBufferedImage(bufferedImageType);

                // delete the texture
                GL20.glDeleteTextures(textureId);
            }

            fbo.unbind();
            shaderProgram.unbind();

            // return depth test
            glEnable(GL20.GL_DEPTH_TEST);

            return image;
        } catch (Exception e) {
            log.error("[ERROR] Error initializing the engine : ", e);
        }

        return null;
    }

    public void makeBoxTexturesByObliqueCamera(HalfEdgeScene halfEdgeScene, double screenPixelsForMeter) {
        // Must know all faces classification ids
        // 1rst, extract all surfaces
        List<HalfEdgeSurface> surfaces = halfEdgeScene.extractSurfaces(null);

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

        List<TexturesAtlasData> texturesAtlasDataList = new ArrayList<>();

        Map<Integer, Map<GaiaFace, HalfEdgeFace>> mapClassifyIdToGaiaFaceToHalfEdgeFace = new HashMap<>();
        Map<Integer, Map<GaiaFace, CameraDirectionTypeInfo>> mapClassifyIdToGaiaFaceToCameraDirectionTypeInfo = new HashMap<>();
        Map<Integer, Map<CameraDirectionType, GaiaBoundingBox>> mapClassificationCamDirTypeBBox = new HashMap<>();
        Map<Integer, Map<CameraDirectionType, Matrix4d>> mapClassificationCamDirTypeModelViewMatrix = new HashMap<>();

        Map<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> mapClassificationCamDirTypeFacesList = new HashMap<>();
        CameraDirectionType cameraDirectionType = CameraDirectionType.CAMERA_DIRECTION_UNKNOWN;
        int classifiedFacesCount = facesClassificationMap.size();
        int count = 0;
        boolean testBool = false;
        for (Map.Entry<Integer, List<HalfEdgeFace>> entry : facesClassificationMap.entrySet()) {
            log.info("makeBoxTexturesByObliqueCamera : " + count + " / " + classifiedFacesCount);
            FaceVisibilityDataManager faceVisibilityDataManager = new FaceVisibilityDataManager();
            int classificationId = entry.getKey();
            List<HalfEdgeFace> facesList = entry.getValue();

            Map<GaiaFace, HalfEdgeFace> mapGaiaFaceToHalfEdgeFace = mapClassifyIdToGaiaFaceToHalfEdgeFace.computeIfAbsent(classificationId, k -> new HashMap<>());
            Map<GaiaFace, CameraDirectionTypeInfo> mapGaiaFaceToCameraDirectionTypeInfo = mapClassifyIdToGaiaFaceToCameraDirectionTypeInfo.computeIfAbsent(classificationId, k -> new HashMap<>());

            GaiaScene gaiaSceneFromFaces = HalfEdgeUtils.gaiaSceneFromHalfEdgeFaces(facesList, mapGaiaFaceToHalfEdgeFace);
            RenderableGaiaScene renderableGaiaSceneColorCoded = getColorCodedRenderableScene(gaiaSceneFromFaces);

            GaiaBoundingBox sceneBbox = gaiaSceneFromFaces.updateBoundingBox();
            double sceneMaxSize = sceneBbox.getMaxSize();
            double sceneHeight = sceneBbox.getSizeZ();
            double ratioHW = sceneHeight / sceneMaxSize;
            if (ratioHW < 0.06) {
                log.info("ratioHW < 0.06");
            }

            // now, set projection matrix as orthographic, and set camera's position and target
            // calculate the projectionMatrix for the camera
            int maxScreenSize = boxRenderingMaxSize;

            // to calculate the texCoords, we need the transformed bbox and the modelViewMatrix
            Map<CameraDirectionType, GaiaBoundingBox> mapCameraDirectionTypeBBox = mapClassificationCamDirTypeBBox.computeIfAbsent(classificationId, k -> new HashMap<>());
            Map<CameraDirectionType, Matrix4d> mapCameraDirectionTypeModelViewMatrix = mapClassificationCamDirTypeModelViewMatrix.computeIfAbsent(classificationId, k -> new HashMap<>());

            // ZNeg texture
            cameraDirectionType = CameraDirectionType.CAMERA_DIRECTION_ZNEG;
            BufferedImage imageZNeg = makeColorCodeTextureByCameraDirection(gaiaSceneFromFaces, renderableGaiaSceneColorCoded, cameraDirectionType, maxScreenSize, mapCameraDirectionTypeBBox, mapCameraDirectionTypeModelViewMatrix, screenPixelsForMeter, faceVisibilityDataManager);
            imageZNeg = eliminateBackGroundColor(imageZNeg);

            if (imageZNeg != null) {
                TexturesAtlasData texturesAtlasDataYPosZNeg = new TexturesAtlasData();
                texturesAtlasDataYPosZNeg.setClassifyId(classificationId);
                texturesAtlasDataYPosZNeg.setCameraDirectionType(CameraDirectionType.CAMERA_DIRECTION_ZNEG);
                texturesAtlasDataYPosZNeg.setTextureImage(imageZNeg);
                texturesAtlasDataList.add(texturesAtlasDataYPosZNeg);
            }


            if (ratioHW > 0.06) {
                // YPosZNeg texture
                cameraDirectionType = CameraDirectionType.CAMERA_DIRECTION_YPOS_ZNEG;
                BufferedImage imageYpoZNeg = makeColorCodeTextureByCameraDirection(gaiaSceneFromFaces, renderableGaiaSceneColorCoded, cameraDirectionType, maxScreenSize, mapCameraDirectionTypeBBox, mapCameraDirectionTypeModelViewMatrix, screenPixelsForMeter, faceVisibilityDataManager);
                imageYpoZNeg = eliminateBackGroundColor(imageYpoZNeg);

                if (imageYpoZNeg != null) {
                    TexturesAtlasData texturesAtlasDataYPosZNeg = new TexturesAtlasData();
                    texturesAtlasDataYPosZNeg.setClassifyId(classificationId);
                    texturesAtlasDataYPosZNeg.setCameraDirectionType(CameraDirectionType.CAMERA_DIRECTION_YPOS_ZNEG);
                    texturesAtlasDataYPosZNeg.setTextureImage(imageYpoZNeg);
                    texturesAtlasDataList.add(texturesAtlasDataYPosZNeg);
                }

                // XNegZNeg texture
                cameraDirectionType = CameraDirectionType.CAMERA_DIRECTION_XNEG_ZNEG;
                BufferedImage imageXNegZNeg = makeColorCodeTextureByCameraDirection(gaiaSceneFromFaces, renderableGaiaSceneColorCoded, cameraDirectionType, maxScreenSize, mapCameraDirectionTypeBBox, mapCameraDirectionTypeModelViewMatrix, screenPixelsForMeter, faceVisibilityDataManager);
                imageXNegZNeg = eliminateBackGroundColor(imageXNegZNeg);

                if (imageXNegZNeg != null) {
                    TexturesAtlasData texturesAtlasDataXNegZNeg = new TexturesAtlasData();
                    texturesAtlasDataXNegZNeg.setClassifyId(classificationId);
                    texturesAtlasDataXNegZNeg.setCameraDirectionType(CameraDirectionType.CAMERA_DIRECTION_XNEG_ZNEG);
                    texturesAtlasDataXNegZNeg.setTextureImage(imageXNegZNeg);
                    texturesAtlasDataList.add(texturesAtlasDataXNegZNeg);
                }

                // YNegZNeg texture
                cameraDirectionType = CameraDirectionType.CAMERA_DIRECTION_YNEG_ZNEG;
                BufferedImage imageYNegZNeg = makeColorCodeTextureByCameraDirection(gaiaSceneFromFaces, renderableGaiaSceneColorCoded, cameraDirectionType, maxScreenSize, mapCameraDirectionTypeBBox, mapCameraDirectionTypeModelViewMatrix, screenPixelsForMeter, faceVisibilityDataManager);
                imageYNegZNeg = eliminateBackGroundColor(imageYNegZNeg);

                if (imageYNegZNeg != null) {
                    TexturesAtlasData texturesAtlasDataYNegZNeg = new TexturesAtlasData();
                    texturesAtlasDataYNegZNeg.setClassifyId(classificationId);
                    texturesAtlasDataYNegZNeg.setCameraDirectionType(CameraDirectionType.CAMERA_DIRECTION_YNEG_ZNEG);
                    texturesAtlasDataYNegZNeg.setTextureImage(imageYNegZNeg);
                    texturesAtlasDataList.add(texturesAtlasDataYNegZNeg);
                }

                // XPosZNeg texture
                cameraDirectionType = CameraDirectionType.CAMERA_DIRECTION_XPOS_ZNEG;
                BufferedImage imageXPosZNeg = makeColorCodeTextureByCameraDirection(gaiaSceneFromFaces, renderableGaiaSceneColorCoded, cameraDirectionType, maxScreenSize, mapCameraDirectionTypeBBox, mapCameraDirectionTypeModelViewMatrix, screenPixelsForMeter, faceVisibilityDataManager);
                imageXPosZNeg = eliminateBackGroundColor(imageXPosZNeg);

                if (imageXPosZNeg != null) {
                    TexturesAtlasData texturesAtlasDataXPosZNeg = new TexturesAtlasData();
                    texturesAtlasDataXPosZNeg.setClassifyId(classificationId);
                    texturesAtlasDataXPosZNeg.setCameraDirectionType(CameraDirectionType.CAMERA_DIRECTION_XPOS_ZNEG);
                    texturesAtlasDataXPosZNeg.setTextureImage(imageXPosZNeg);
                    texturesAtlasDataList.add(texturesAtlasDataXPosZNeg);
                }
            }

            // delete the renderableScene-colorCoded
            renderableGaiaSceneColorCoded.deleteGLBuffers();

            // There are no visible faces, so 1rst set the CAMERA_DIRECTION_ZNEG to all the halfEdgeFaces as default
            for (HalfEdgeFace halfEdgeFace : facesList) {
                halfEdgeFace.setCameraDirectionType(CameraDirectionType.CAMERA_DIRECTION_ZNEG);
            }

            // now assign face to each cameraDirectionType
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

            count++;
        }

        halfEdgeScene.splitFacesByBestObliqueCameraDirectionToProject();

        // now, for each classifyId - CameraDirectionType, calculate the texCoords
        Map<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> mapFaceGroupByClassifyIdAndObliqueCamDirType = new HashMap<>();
        List<HalfEdgeSurface> halfEdgeSurfaces = halfEdgeScene.extractSurfaces(null);
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

        List<HalfEdgeVertex> verticesOfFaces = new ArrayList<>();
        Map<HalfEdgeVertex, HalfEdgeVertex> visitedVerticesMap = new HashMap<>();
        double texCoordError = 0.0025;
        for (Map.Entry<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> entry : mapFaceGroupByClassifyIdAndObliqueCamDirType.entrySet()) {
            int classifyId = entry.getKey();
            Map<CameraDirectionType, List<HalfEdgeFace>> mapCameraDirectionTypeFacesList = entry.getValue();
            for (Map.Entry<CameraDirectionType, List<HalfEdgeFace>> entry1 : mapCameraDirectionTypeFacesList.entrySet()) {
                cameraDirectionType = entry1.getKey();
                List<HalfEdgeFace> facesList = entry1.getValue();

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

                        if (texCoordX < 0.0 || texCoordX > 1.0 || texCoordY < 0.0 || texCoordY > 1.0) {
                            log.info("makeBoxTexturesByObliqueCamera() : texCoordX or texCoordY is out of range." + "camDirType = " + cameraDirectionType);
                        }

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

        doAtlasTextureProcess(halfEdgeScene, texturesAtlasDataList);
        recalculateTexCoordsAfterTextureAtlasingObliqueCamera(halfEdgeScene, texturesAtlasDataList, mapClassificationCamDirTypeFacesList);

        String originalPath = halfEdgeScene.getOriginalPath().toString();

        // extract the originalProjectName from the originalPath
        String originalProjectName = originalPath.substring(originalPath.lastIndexOf(File.separator) + 1);
        String rawProjectName = originalProjectName.substring(0, originalProjectName.lastIndexOf("."));

        //make tempFolder if no exists.***
        /*String tempFolderPath = this.getTempFolderPath();
        File tempFolder = new File(tempFolderPath);
        if (!tempFolder.exists()) {
            tempFolder.mkdirs();
        }*/
        String fileName = rawProjectName + "_AtlasB";
        String extension = ".png";
        GaiaTexture atlasTexture = makeAtlasTexture(texturesAtlasDataList);

        // delete texturesAtlasDataList
        for (TexturesAtlasData texturesAtlasData : texturesAtlasDataList) {
            texturesAtlasData.deleteObjects();
        }

        if (atlasTexture == null) {
            log.info("makeAtlasTexture() : atlasTexture is null.");
            return;
        }
        atlasTexture.setPath(fileName + extension);

        // finally make material with texture for the halfEdgeScene
        GaiaMaterial material = new GaiaMaterial();
        material.setName("atlasTexturesMaterial");
        Map<TextureType, List<GaiaTexture>> textures = new HashMap<>();
        List<GaiaTexture> atlasTextures = new ArrayList<>();
        atlasTextures.add(atlasTexture);
        textures.put(TextureType.DIFFUSE, atlasTextures);
        material.setTextures(textures);

        int materialsCount = halfEdgeScene.getMaterials().size();
        material.setId(materialsCount);
        halfEdgeScene.getMaterials().add(material);

        List<HalfEdgePrimitive> primitives = new ArrayList<>();
        halfEdgeScene.extractPrimitives(primitives);
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.setMaterialId(materialsCount);
        }
    }

    private int getMaxWidth(List<TexturesAtlasData> compareImages) {
        int result = compareImages.stream().mapToInt(textureScissorData -> (int) textureScissorData.getBatchedBoundary().getMaxX()).max().orElse(0);
        return result;
    }

    private int getMaxHeight(List<TexturesAtlasData> compareImages) {
        int result = compareImages.stream().mapToInt(textureScissorData -> (int) textureScissorData.getBatchedBoundary().getMaxY()).max().orElse(0);
        return result;
    }

    private GaiaTexture makeAtlasTexture(List<TexturesAtlasData> texAtlasDatasList) {
        int imageType = BufferedImage.TYPE_INT_RGB;

        // calculate the maxWidth and maxHeight
        // TODO : is it wrong to calculate the maxWidth and maxHeight by using the batchedBoundary?***
        int maxWidth = getMaxWidth(texAtlasDatasList);
        int maxHeight = getMaxHeight(texAtlasDatasList);

        if (maxWidth == 0 || maxHeight == 0) {
            log.error("[ERROR] makeAtlasTexture() : maxWidth or maxHeight is 0.");
            return null;
        }

        GaiaTexture textureAtlas = new GaiaTexture();
        log.info("[Tile][Photogrammetry][makeAtlasTexture] Atlas maxWidth : " + maxWidth + " , maxHeight : " + maxHeight);
        textureAtlas.createImage(maxWidth, maxHeight, imageType);

        // draw the images into textureAtlas
        log.debug("HalfEdgeSurface.scissorTextures() : draw the images into textureAtlas.");
        Graphics2D g2d = textureAtlas.getBufferedImage().createGraphics();
        int textureAtlasDatasCount = texAtlasDatasList.size();
        for (int i = 0; i < textureAtlasDatasCount; i++) {
            TexturesAtlasData textureAtlasData = texAtlasDatasList.get(i);
            GaiaRectangle currentBoundary = textureAtlasData.getCurrentBoundary();
            GaiaRectangle batchedBoundary = textureAtlasData.getBatchedBoundary();
            GaiaRectangle originBoundary = textureAtlasData.getOriginalBoundary();

            BufferedImage subImage = textureAtlasData.getTextureImage();

//            Color randomColor = new Color((float) Math.random(), (float) Math.random(), (float) Math.random(), 0.8f);
//            BufferedImage randomColoredImage = new BufferedImage(subImage.getWidth(), subImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
//            Graphics2D randomGraphics = randomColoredImage.createGraphics();
//            randomGraphics.setColor(randomColor);
//            randomGraphics.fillRect(0, 0, subImage.getWidth(), subImage.getHeight());
//            randomGraphics.dispose();
//            g2d.drawImage(randomColoredImage, (int) batchedBoundary.getMinX(), (int) batchedBoundary.getMinY(), null); // test code
//            // end test.--------------------------------------------------------------------------------------------------------------------------------

            g2d.drawImage(subImage, (int) batchedBoundary.getMinX(), (int) batchedBoundary.getMinY(), null); // original code

        }
        g2d.dispose();

        return textureAtlas;
    }

    private void recalculateTexCoordsAfterTextureAtlasingObliqueCamera(HalfEdgeScene halfEdgeScene, List<TexturesAtlasData> texAtlasDatasList,
                                                                       Map<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> mapClassificationCamDirTypeFacesList) {
        int maxWidth = getMaxWidth(texAtlasDatasList);
        int maxHeight = getMaxHeight(texAtlasDatasList);

        if (maxWidth == 0 || maxHeight == 0) {
            return;
        }

        Map<HalfEdgeVertex, HalfEdgeVertex> groupVertexMapMemSave = new HashMap<>();
        Map<HalfEdgeVertex, HalfEdgeVertex> visitedVertexMapMemSave = new HashMap<>();
        visitedVertexMapMemSave.clear();

        List<HalfEdgeVertex> faceVerticesMemSave = new ArrayList<>();

        int texAtlasDatasCount = texAtlasDatasList.size();
        for (int i = 0; i < texAtlasDatasCount; i++) {
            TexturesAtlasData texAtlasData = texAtlasDatasList.get(i);
            int classifyId = texAtlasData.getClassifyId();
            //PlaneType planeType = texAtlasData.getPlaneType(); // old old old old old old
            CameraDirectionType cameraDirectionType = texAtlasData.getCameraDirectionType();
            List<HalfEdgeFace> faceGroup = mapClassificationCamDirTypeFacesList.get(classifyId).get(cameraDirectionType);

            if (faceGroup == null) {
                continue;
            }

            GaiaRectangle originalBoundary = texAtlasData.getOriginalBoundary();
            GaiaRectangle batchedBoundary = texAtlasData.getBatchedBoundary();

            double texWidth = texAtlasData.getTextureImage().getWidth();
            double texHeight = texAtlasData.getTextureImage().getHeight();
            double xPixelSize = 1.0 / texWidth;
            double yPixelSize = 1.0 / texHeight;

            // obtain all vertex of the faceGroup
            groupVertexMapMemSave.clear();
            int facesCount = faceGroup.size();
            for (int j = 0; j < facesCount; j++) {
                HalfEdgeFace face = faceGroup.get(j);
                faceVerticesMemSave.clear();
                faceVerticesMemSave = face.getVertices(faceVerticesMemSave);
                int verticesCount = faceVerticesMemSave.size();
                for (int k = 0; k < verticesCount; k++) {
                    HalfEdgeVertex vertex = faceVerticesMemSave.get(k);
                    groupVertexMapMemSave.put(vertex, vertex);
                }
            }

            // now, calculate the vertex list from the map
            List<HalfEdgeVertex> vertexList = new ArrayList<>(groupVertexMapMemSave.values());
            int verticesCount = vertexList.size();
            double texCoordErrore = 0.0025;
            for (int k = 0; k < verticesCount; k++) {
                HalfEdgeVertex vertex = vertexList.get(k);

                // calculate the real texCoords
                Vector2d texCoord = vertex.getTexcoords();
                double x = texCoord.x;
                double y = texCoord.y;

                double pixelX = x * texWidth;
                double pixelY = y * texHeight;

                // transform the texCoords to texCoordRelToCurrentBoundary
                double xRel = (pixelX - originalBoundary.getMinX()) / originalBoundary.getWidth();
                double yRel = (pixelY - originalBoundary.getMinY()) / originalBoundary.getHeight();

                // clamp the texRelCoords
                xRel = Math.max(0.0 + xPixelSize, Math.min(1.0 - xPixelSize, xRel));
                yRel = Math.max(0.0 + yPixelSize, Math.min(1.0 - yPixelSize, yRel));

                // transform the texCoordRelToCurrentBoundary to atlasBoundary using batchedBoundary
                double xAtlas = (batchedBoundary.getMinX() + xRel * batchedBoundary.getWidth()) / maxWidth;
                double yAtlas = (batchedBoundary.getMinY() + yRel * batchedBoundary.getHeight()) / maxHeight;

                if (xAtlas < 0.0 || xAtlas > 1.0 || yAtlas < 0.0 || yAtlas > 1.0) {
                    log.info("recalculateTexCoordsAfterTextureAtlasingObliqueCamera() : xAtlas or yAtlas is out of range.");
                }

                // clamp the texAtlasCoords
                Vector2d texCoordFinal = new Vector2d(xAtlas, yAtlas);
                GaiaTextureUtils.clampTextureCoordinate(texCoordFinal, texCoordErrore);
                vertex.setTexcoords(texCoordFinal);
            }
        }
    }

    private void doAtlasTextureProcess(HalfEdgeScene halfEdgeScene, List<TexturesAtlasData> texAtlasDatasList) {
        // 1rst, sort the texAtlasData by width and height
        List<TexturesAtlasData> texAtlasDataWidther = new ArrayList<>();
        List<TexturesAtlasData> texAtlasDataHigher = new ArrayList<>();
        int texAtlasDataCount = texAtlasDatasList.size();
        for (int i = 0; i < texAtlasDataCount; i++) {
            TexturesAtlasData texAtlasDataAux = texAtlasDatasList.get(i);
            GaiaRectangle originalBoundary = texAtlasDataAux.getOriginalBoundary();

            double w = originalBoundary.getWidth();
            double h = originalBoundary.getHeight();
            if (w > h) {
                texAtlasDataWidther.add(texAtlasDataAux);
            } else {
                texAtlasDataHigher.add(texAtlasDataAux);
            }
        }

        // now, sort each list by width and height
        texAtlasDataWidther.sort((o1, o2) -> {
            GaiaRectangle originalBoundary1 = o1.getOriginalBoundary();
            GaiaRectangle originalBoundary2 = o2.getOriginalBoundary();
            double w1 = originalBoundary1.getWidth();
            double w2 = originalBoundary2.getWidth();
            return Double.compare(w2, w1);
        });

        texAtlasDataHigher.sort((o1, o2) -> {
            GaiaRectangle originalBoundary1 = o1.getOriginalBoundary();
            GaiaRectangle originalBoundary2 = o2.getOriginalBoundary();
            double h1 = originalBoundary1.getHeight();
            double h2 = originalBoundary2.getHeight();
            return Double.compare(h2, h1);
        });

        // make an unique atlasDataList alternating the texAtlasDataWidther and texAtlasDataHigher
        texAtlasDatasList.clear();
        int texAtlasDataWidtherCount = texAtlasDataWidther.size();
        int texAtlasDataHigherCount = texAtlasDataHigher.size();
        int texAtlasDataMaxCount = Math.max(texAtlasDataWidtherCount, texAtlasDataHigherCount);
        for (int i = 0; i < texAtlasDataMaxCount; i++) {
            if (i < texAtlasDataWidtherCount) {
                texAtlasDatasList.add(texAtlasDataWidther.get(i));
            }
            if (i < texAtlasDataHigherCount) {
                texAtlasDatasList.add(texAtlasDataHigher.get(i));
            }
        }

        // now, make the atlas texture
        GaiaRectangle beforeMosaicRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
        List<GaiaRectangle> rectangleList = new ArrayList<>();

        TreeMap<Double, List<GaiaRectangle>> maxXrectanglesMap = new TreeMap<>();

        Vector2d bestPosition = new Vector2d();
        List<TexturesAtlasData> currProcessTextureAtlasDates = new ArrayList<>();
        texAtlasDataCount = texAtlasDatasList.size();
        for (int i = 0; i < texAtlasDataCount; i++) {
            TexturesAtlasData texAtlasDataAux = texAtlasDatasList.get(i);
            GaiaRectangle originBoundary = texAtlasDataAux.getOriginalBoundary();

            GaiaRectangle batchedBoundary = null;
            if (i == 0) {
                // the 1rst textureScissorData
                batchedBoundary = new GaiaRectangle(0.0, 0.0, originBoundary.getWidthInt(), originBoundary.getHeightInt());
                texAtlasDataAux.setBatchedBoundary(batchedBoundary);
                beforeMosaicRectangle.copyFrom(batchedBoundary);
            } else {
                // 1rst, find the best position for image into atlas
                bestPosition = this.getBestPositionMosaicInAtlas(currProcessTextureAtlasDates, texAtlasDataAux, bestPosition, beforeMosaicRectangle, rectangleList, maxXrectanglesMap);
                batchedBoundary = new GaiaRectangle(bestPosition.x, bestPosition.y, bestPosition.x + originBoundary.getWidthInt(), bestPosition.y + originBoundary.getHeightInt());
                texAtlasDataAux.setBatchedBoundary(batchedBoundary);
                beforeMosaicRectangle.addBoundingRectangle(batchedBoundary);
            }

            rectangleList.add(batchedBoundary);
            currProcessTextureAtlasDates.add(texAtlasDataAux);

            // map
            double maxX = batchedBoundary.getMaxX();

            List<GaiaRectangle> list_rectanglesMaxX = maxXrectanglesMap.computeIfAbsent(maxX, k -> new ArrayList<>());
            list_rectanglesMaxX.add(batchedBoundary);
        }
    }

    private Vector2d getBestPositionMosaicInAtlas(List<TexturesAtlasData> currProcessTextureAtlasDates, TexturesAtlasData texAtlasDataToPutInMosaic,
                                                  Vector2d resultVec, GaiaRectangle beforeMosaicRectangle, List<GaiaRectangle> list_rectangles, TreeMap<Double, List<GaiaRectangle>> map_maxXrectangles) {
        if (resultVec == null) {
            resultVec = new Vector2d();
        }

        double currPosX, currPosY;
        double candidatePosX = 0.0, candidatePosY = 0.0;
        double currMosaicPerimeter, candidateMosaicPerimeter;
        candidateMosaicPerimeter = -1.0;
        double error = 1.0 - 1e-6;

        // Now, try to find the best positions to put our rectangle
        int existentTexAtlasDataCount = currProcessTextureAtlasDates.size();
        for (int i = 0; i < existentTexAtlasDataCount; i++) {
            TexturesAtlasData existentTexAtlasData = currProcessTextureAtlasDates.get(i);
            GaiaRectangle currRect = existentTexAtlasData.getBatchedBoundary();

            // for each existent rectangles, there are 2 possibles positions: leftUp & rightDown
            // in this 2 possibles positions we put our leftDownCorner of rectangle of "splitData_toPutInMosaic"

            // If in some of two positions our rectangle intersects with any other rectangle, then discard
            // If no intersects with others rectangles, then calculate the mosaic-perimeter.
            // We choose the minor perimeter of the mosaic

            double width = texAtlasDataToPutInMosaic.getOriginalBoundary().getWidthInt();
            double height = texAtlasDataToPutInMosaic.getOriginalBoundary().getHeightInt();

            // 1- leftUp corner
            currPosX = currRect.getMinX();
            currPosY = currRect.getMaxY();

            // setup our rectangle
            if (texAtlasDataToPutInMosaic.getBatchedBoundary() == null) {
                texAtlasDataToPutInMosaic.setBatchedBoundary(new GaiaRectangle(0.0, 0.0, 0.0, 0.0));
            }
            texAtlasDataToPutInMosaic.getBatchedBoundary().setMinX(currPosX);
            texAtlasDataToPutInMosaic.getBatchedBoundary().setMinY(currPosY);
            texAtlasDataToPutInMosaic.getBatchedBoundary().setMaxX(currPosX + width);
            texAtlasDataToPutInMosaic.getBatchedBoundary().setMaxY(currPosY + height);

            // put our rectangle into mosaic & check that no intersects with another rectangles
            if (!this.intersectsRectangleAtlasingProcess(list_rectangles, texAtlasDataToPutInMosaic.getBatchedBoundary(), map_maxXrectangles)) {
                GaiaRectangle afterMosaicRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
                afterMosaicRectangle.copyFrom(beforeMosaicRectangle);
                afterMosaicRectangle.addBoundingRectangle(texAtlasDataToPutInMosaic.getBatchedBoundary());

                // calculate the perimeter of the mosaic
                if (candidateMosaicPerimeter < 0.0) {
                    candidateMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    candidatePosX = currPosX;
                    candidatePosY = currPosY;
                } else {
                    currMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    if (candidateMosaicPerimeter >= currMosaicPerimeter * error) {
                        candidateMosaicPerimeter = currMosaicPerimeter;
                        candidatePosX = currPosX;
                        candidatePosY = currPosY;
                        break; // test delete**************************
                    }
                }
            }

            // 2- rightDown corner
            currPosX = currRect.getMaxX();
            currPosY = currRect.getMinY();

            // setup our rectangle
            texAtlasDataToPutInMosaic.getBatchedBoundary().setMinX(currPosX);
            texAtlasDataToPutInMosaic.getBatchedBoundary().setMinY(currPosY);
            texAtlasDataToPutInMosaic.getBatchedBoundary().setMaxX(currPosX + width);
            texAtlasDataToPutInMosaic.getBatchedBoundary().setMaxY(currPosY + height);

            // put our rectangle into mosaic & check that no intersects with another rectangles
            if (!this.intersectsRectangleAtlasingProcess(list_rectangles, texAtlasDataToPutInMosaic.getBatchedBoundary(), map_maxXrectangles)) {
                GaiaRectangle afterMosaicRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
                afterMosaicRectangle.copyFrom(beforeMosaicRectangle);
                afterMosaicRectangle.addBoundingRectangle(texAtlasDataToPutInMosaic.getBatchedBoundary());

                // calculate the perimeter of the mosaic
                if (candidateMosaicPerimeter < 0.0) {
                    candidateMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    candidatePosX = currPosX;
                    candidatePosY = currPosY;
                } else {
                    currMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    if (candidateMosaicPerimeter >= currMosaicPerimeter * error) {
                        candidateMosaicPerimeter = currMosaicPerimeter;
                        candidatePosX = currPosX;
                        candidatePosY = currPosY;
                        break; // test delete**************************
                    }
                }
            }
        }

        resultVec.set(candidatePosX, candidatePosY);

        return resultVec;
    }

    private boolean intersectsRectangleAtlasingProcess(List<GaiaRectangle> listRectangles, GaiaRectangle rectangle, TreeMap<Double, List<GaiaRectangle>> map_maxXrectangles) {
        // this function returns true if the rectangle intersects with any existent rectangle of the listRectangles
        boolean intersects = false;
        double error = 10E-5;

        double currRectMinX = rectangle.getMinX();

        // check with map_maxXrectangles all rectangles that have maxX > currRectMinX
        for (Map.Entry<Double, List<GaiaRectangle>> entry : map_maxXrectangles.tailMap(currRectMinX).entrySet()) {
            List<GaiaRectangle> existentRectangles = entry.getValue();

            int existentRectanglesCount = existentRectangles.size();
            for (int i = 0; i < existentRectanglesCount; i++) {
                GaiaRectangle existentRectangle = existentRectangles.get(i);
                if (existentRectangle == rectangle) {
                    continue;
                }
                if (existentRectangle.intersects(rectangle, error)) {
                    return true;
                }
            }
        }


//        for (GaiaRectangle existentRectangle : listRectangles) {
//            if (existentRectangle == rectangle) {
//                continue;
//            }
//            if (existentRectangle.intersects(rectangle, error)) {
//                intersects = true;
//                break;
//            }
//        }
        return intersects;
    }

    private void renderIntoFbo(Fbo fbo, ShaderProgram shaderProgram, GaiaScenesContainer gaiaScenesContainer, Vector4f clearColor, boolean blendColors) {
        // render the renderableScene
        try {
            fbo.bind();

            int[] width = new int[1];
            int[] height = new int[1];
            width[0] = fbo.getFboWidth();
            height[0] = fbo.getFboHeight();

            glViewport(0, 0, width[0], height[0]);
            if (clearColor != null) {
                glClearColor(clearColor.x, clearColor.y, clearColor.z, clearColor.w);
                glClear(GL_COLOR_BUFFER_BIT);
            }
            glClear(GL_DEPTH_BUFFER_BIT);
            glEnable(GL_DEPTH_TEST);

            // enable cull face
            glEnable(GL_CULL_FACE);

            // set blend func
            if (blendColors) {
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            } else {
                glDisable(GL_BLEND);
            }

            // render the scene
            shaderProgram.bind();

            Camera camera = gaiaScenesContainer.getCamera();
            Matrix4d modelViewMatrix = camera.getModelViewMatrix();
            UniformsMap uniformsMap = shaderProgram.getUniformsMap();
            uniformsMap.setUniformMatrix4fv("uModelViewMatrix", new Matrix4f(modelViewMatrix));
            renderer.render(gaiaScenesContainer, shaderProgram);

            shaderProgram.unbind();

            fbo.unbind();
        } catch (Exception e) {
            log.error("[ERROR] Error initializing the engine : ", e);
        }
    }

    private BufferedImage makeColorCodeTextureByCameraDirection(GaiaScene gaiaScene, RenderableGaiaScene renderableScene, CameraDirectionType cameraDirectionType,
                                                                int maxScreenSize, Map<CameraDirectionType, GaiaBoundingBox> mapCameraDirectionTypeBBox, Map<CameraDirectionType, Matrix4d> mapCameraDirectionTypeModelViewMatrix, double screenPixelsForMeter, FaceVisibilityDataManager faceVisibilityDataManager) {
        // Calculate bbox relative to camera direction
        GaiaBoundingBox bbox = gaiaScene.updateBoundingBox();
        Vector3d bboxCenter = bbox.getCenter();

        // expanded box for shader (delimiting box with a little buffer)
        GaiaBoundingBox expandedBBox = bbox.clone();
        double expandedMaxSize = expandedBBox.getMaxSize();
        expandedBBox.expand(expandedMaxSize * 0.02);

        // set camera position
        Vector3d camDir = CameraDirectionType.getCameraDirection(cameraDirectionType);
        Camera camera = gaiaScenesContainer.getCamera();
        camera.setPosition(bboxCenter);
        camera.setDirection(camDir);
        Vector3d up = camera.calculateUpVector(camDir);
        camera.setUp(up);
        gaiaScenesContainer.setCamera(camera);

        Matrix4d modelViewMatrix = camera.getModelViewMatrix();

        List<Vector3d> transformedVertices = new ArrayList<>();

        //******************************************************************************************************************
        // If the scene was spend the node's transform, you can use the following code to get the transformed vertices
        List<GaiaPrimitive> gaiaPrimitives = gaiaScene.extractPrimitives(null);
        List<GaiaVertex> gaiaVertices = new ArrayList<>();
        for (GaiaPrimitive gaiaPrimitive : gaiaPrimitives) {
            gaiaVertices.addAll(gaiaPrimitive.getVertices());
        }
        for (GaiaVertex vertex : gaiaVertices) {
            Vector4d transformedPos4d = new Vector4d();
            Vector3d vertexPos3d = vertex.getPosition();
            Vector4d vertexPos4d = new Vector4d(vertexPos3d.x, vertexPos3d.y, vertexPos3d.z, 1.0);
            modelViewMatrix.transform(vertexPos4d, transformedPos4d);
            Vector3d transformedPos3d = new Vector3d(transformedPos4d.x, transformedPos4d.y, transformedPos4d.z);
            transformedVertices.add(transformedPos3d);
        }
        // End of transform the vertices relative to camera.-------------------------------------------------------------

        //*******************************************************************************************************************
        // If the scene was not spend the node's transform, you can use the following code to get the transformed vertices
//        List<GaiaVertex> finalVertices = new ArrayList<>();
//        gaiaScene.getFinalVerticesCopy(finalVertices);
//        for (GaiaVertex vertex : finalVertices) {
//            Vector4d transformedPos4d = new Vector4d();
//            Vector3d vertexPos3d = vertex.getPosition();
//            Vector4d vertexPos4d = new Vector4d(vertexPos3d.x, vertexPos3d.y, vertexPos3d.z, 1.0);
//            modelViewMatrix.transform(vertexPos4d, transformedPos4d);
//            Vector3d transformedPos3d = new Vector3d(transformedPos4d.x, transformedPos4d.y, transformedPos4d.z);
//            transformedVertices.add(transformedPos3d);
//        }
        // End of transform the vertices relative to camera.-------------------------------------------------------------

        GaiaBoundingBox bboxTransformed = new GaiaBoundingBox();
        bboxTransformed.setFromPoints(transformedVertices);

        mapCameraDirectionTypeBBox.put(cameraDirectionType, bboxTransformed);
        mapCameraDirectionTypeModelViewMatrix.put(cameraDirectionType, new Matrix4d(modelViewMatrix));

        // now we can calculate the projection matrix
        float xLength = (float) bboxTransformed.getSizeX();
        float yLength = (float) bboxTransformed.getSizeY();

        float maxX = (float) bboxTransformed.getMaxX();
        float maxY = (float) bboxTransformed.getMaxY();
        float maxZ = (float) bboxTransformed.getMaxZ();
        float minX = (float) bboxTransformed.getMinX();
        float minY = (float) bboxTransformed.getMinY();
        float minZ = (float) bboxTransformed.getMinZ();
        // attention! : near = -maxZ, far = -minZ
        float near = -maxZ;
        float far = -minZ;

        float maxSize = Math.max(xLength, yLength);
        float zOffSet = maxSize * 0.001f;
        if (zOffSet < 0.4) {
            zOffSet = 0.4f;
        }

        far += zOffSet; // make a little more far
        near -= zOffSet; // make a little more near

        Projection projection = gaiaScenesContainer.getProjection();
        projection.setProjectionOrthographic(minX, maxX, minY, maxY, near, far);
        gaiaScenesContainer.setProjection(projection);

        // Take FboManager from engine
        FboManager fboManager = this.getFboManager();

        // create the fbo
        int fboWidth = maxScreenSize;
        int fboHeight = maxScreenSize;

        fboWidth = (int) (xLength * screenPixelsForMeter);
        fboHeight = (int) (yLength * screenPixelsForMeter);

        fboWidth = Math.max(fboWidth, 1);
        fboHeight = Math.max(fboHeight, 1);

        Fbo colorFbo = fboManager.getOrCreateFbo("colorRenderBoxTexObliqueCamera", fboWidth, fboHeight);
        Fbo colorCodeFbo = fboManager.getOrCreateFbo("colorCodeObliqueCamera", fboWidth, fboHeight);

        // render the renderableScene
        // shader program
        ShaderManager shaderManager = getShaderManager();

        // color render
        ShaderProgram sceneShaderProgram = shaderManager.getShaderProgram("sceneDelimited");
        sceneShaderProgram.bind();
        // set uniform map
        UniformsMap uniformsMap = sceneShaderProgram.getUniformsMap();
        uniformsMap.setUniform3fv("bboxMin", new Vector3f((float) expandedBBox.getMinX(), (float) expandedBBox.getMinY(), (float) expandedBBox.getMinZ()));
        uniformsMap.setUniform3fv("bboxMax", new Vector3f((float) expandedBBox.getMaxX(), (float) expandedBBox.getMaxY(), (float) expandedBBox.getMaxZ()));
        Vector4f clearColor = new Vector4f(0.5f, 0.5f, 0.5f, 1.0f);
        renderIntoFbo(colorFbo, sceneShaderProgram, gaiaScenesContainer, clearColor, true);
        colorFbo.bind();
        BufferedImage image = colorFbo.getBufferedImage(BufferedImage.TYPE_INT_RGB);

//        // as a test, paint the image in a color. Red
//        Color color = new Color(255, 0, 0);
//        if (cameraDirectionType == CameraDirectionType.CAMERA_DIRECTION_ZNEG) {
//            color = new Color(0, 255, 255);
//        } else if (cameraDirectionType == CameraDirectionType.CAMERA_DIRECTION_XPOS_ZNEG) {
//            color = new Color(255, 0, 0);
//        }
//        else if (cameraDirectionType == CameraDirectionType.CAMERA_DIRECTION_XNEG_ZNEG) {
//            color = new Color(100, 0, 0);
//        }
//        else if (cameraDirectionType == CameraDirectionType.CAMERA_DIRECTION_YPOS_ZNEG) {
//            color = new Color(0, 255, 0);
//        }
//        else if (cameraDirectionType == CameraDirectionType.CAMERA_DIRECTION_YNEG_ZNEG) {
//            color = new Color(0, 100, 0);
//        }
//
//        Graphics2D g2d = image.createGraphics();
//        g2d.setColor(color);
//        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
//        g2d.dispose();
//        // end test


        colorFbo.unbind();
        fboManager.deleteFbo("colorRenderBoxTexObliqueCamera");

        // colorCoded render
        RenderableGaiaScene renderableSceneCurrent = gaiaScenesContainer.getRenderableGaiaScenes().get(0);
        gaiaScenesContainer.getRenderableGaiaScenes().clear();
        gaiaScenesContainer.getRenderableGaiaScenes().add(renderableScene);

        ShaderProgram colorCodeShaderProgram = shaderManager.getShaderProgram("trianglesDelimitedColorCode");
        colorCodeShaderProgram.bind();
        uniformsMap = sceneShaderProgram.getUniformsMap();
        uniformsMap.setUniform3fv("bboxMin", new Vector3f((float) expandedBBox.getMinX(), (float) expandedBBox.getMinY(), (float) expandedBBox.getMinZ()));
        uniformsMap.setUniform3fv("bboxMax", new Vector3f((float) expandedBBox.getMaxX(), (float) expandedBBox.getMaxY(), (float) expandedBBox.getMaxZ()));
        clearColor.set(1.0f, 1.0f, 1.0f, 1.0f);
        renderIntoFbo(colorCodeFbo, colorCodeShaderProgram, gaiaScenesContainer, clearColor, false);
        //colorCodeFbo.bind();
        //BufferedImage imageColorCode = colorCodeFbo.getBufferedImage(BufferedImage.TYPE_INT_ARGB);
        //colorCodeFbo.unbind();
        //fboManager.deleteFbo("colorCodeObliqueCamera");

        // restore the current renderableScene
        gaiaScenesContainer.getRenderableGaiaScenes().clear();
        gaiaScenesContainer.getRenderableGaiaScenes().add(renderableSceneCurrent);

        // test save images
        try {
            String randomId = String.valueOf(UUID.randomUUID());
            String path = "D:\\Result_mago3dTiler";
            String fileName = "albedo_" + randomId;
            String extension = ".png";
            String imagePath = path + "\\" + fileName + extension;
            File imageFile = new File(imagePath);
            ImageIO.write(image, "png", imageFile);
        } catch (IOException e) {
            log.debug("Error writing image: {}", e);
        }

        // test save images
//        try {
//            String randomId = String.valueOf(UUID.randomUUID());
//            String path = "D:\\Result_mago3dTiler";
//            String fileName = "colorCoded_" + randomId;
//            String extension = ".png";
//            String imagePath = path + "\\" + fileName + extension;
//            File imageFile = new File(imagePath);
//            ImageIO.write(imageColorCode, "png", imageFile);
//        } catch (IOException e) {
//            log.debug("Error writing image: {}", e);
//        }

        // check the colorCoded image
        colorCodeFbo.bind();

        // read pixels from fbo
        fboWidth = colorCodeFbo.getFboWidth();
        fboHeight = colorCodeFbo.getFboHeight();
        ByteBuffer pixels = colorCodeFbo.readPixels(GL_RGBA);

        // unbind the fbo
        colorCodeFbo.unbind();
        fboManager.deleteFbo("colorCodeObliqueCamera");

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

        return image;
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

        log.info("shaderFolder: {}", shaderFolder.getAbsolutePath());


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

    private void takeColorCodedPhoto(RenderableGaiaScene renderableGaiaScene, Fbo fbo, ShaderProgram shaderProgram) {
        fbo.bind();
        glViewport(0, 0, fbo.getFboWidth(), fbo.getFboHeight()); // 500 x 500
        glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);

        // render scene objects
        shaderProgram.bind();

        Camera camera = gaiaScenesContainer.getCamera();
        Matrix4d modelViewMatrix = camera.getModelViewMatrix();
        UniformsMap uniformsMap = shaderProgram.getUniformsMap();
        uniformsMap.setUniformMatrix4fv("uModelViewMatrix", new Matrix4f(modelViewMatrix));

        // disable cull face
        glDisable(GL_CULL_FACE);
        renderer.renderColorCoded(renderableGaiaScene, selectionColorManager, shaderProgram);
        shaderProgram.unbind();

        fbo.unbind();

        // return the viewport to window size
        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        glViewport(0, 0, windowWidth, windowHeight);
    }

    private void determineExteriorAndInteriorObjects(Fbo fbo) {
        // bind the fbo
        fbo.bind();

        // read pixels from fbo
        int fboWidth = fbo.getFboWidth();
        int fboHeight = fbo.getFboHeight();
        ByteBuffer pixels = ByteBuffer.allocateDirect(fboWidth * fboHeight * 4);
        glReadPixels(0, 0, fboWidth, fboHeight, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

        // unbind the fbo
        fbo.unbind();

        // determine exterior and interior objects
        int pixelsCount = fboWidth * fboHeight;
        for (int i = 0; i < pixelsCount; i++) {
            int colorCode = pixels.getInt(i * 4);
            // background color is (1, 1, 1, 1). skip background color
            if (colorCode == 0xFFFFFFFF) {
                continue;
            }
            RenderablePrimitive renderablePrimitive = (RenderablePrimitive) selectionColorManager.mapColorRenderable.get(colorCode);
            if (renderablePrimitive != null) {
                // determine exterior or interior
                // 0 = interior, 1 = exterior, -1 = unknown
                renderablePrimitive.setStatus(1);
            }
        }
    }

    private RenderableGaiaScene processExteriorInterior(GaiaScene gaiaScene) {
        RenderableGaiaScene renderableGaiaScene = InternDataConverter.getRenderableGaiaScene(gaiaScene);
        gaiaScenesContainer.addRenderableGaiaScene(renderableGaiaScene);
        GaiaBoundingBox bbox = gaiaScene.updateBoundingBox();
        float maxLength = (float) bbox.getLongestDistance();
        float bboxHight = (float) bbox.getMaxZ() - (float) bbox.getMinZ();
        float semiMaxLength = maxLength / 2.0f;
        semiMaxLength *= 150.0f;

        // render into frame buffer
        Fbo colorRenderFbo = fboManager.getFbo("colorCodeRender");

        // render scene objects
        ShaderProgram sceneShaderProgram = shaderManager.getShaderProgram("colorCode");
        sceneShaderProgram.bind();
        Matrix4f projectionOrthoMatrix = new Matrix4f().ortho(-semiMaxLength, semiMaxLength, -semiMaxLength, semiMaxLength, -semiMaxLength * 10.0f, semiMaxLength * 10.0f);

        // make colorRenderableMap
        java.util.List<RenderablePrimitive> allRenderablePrimitives = new ArrayList<>();
        renderableGaiaScene.extractRenderablePrimitives(allRenderablePrimitives);
        int renderablePrimitivesCount = allRenderablePrimitives.size();
        for (int i = 0; i < renderablePrimitivesCount; i++) {
            RenderablePrimitive renderablePrimitive = allRenderablePrimitives.get(i);
            renderablePrimitive.setStatus(0); // init as interior
            int colorCode = selectionColorManager.getAvailableColor();
            renderablePrimitive.setColorCode(colorCode);
            selectionColorManager.mapColorRenderable.put(colorCode, renderablePrimitive);
        }

        UniformsMap uniformsMap = sceneShaderProgram.getUniformsMap();
        uniformsMap.setUniformMatrix4fv("uProjectionMatrix", projectionOrthoMatrix);

        // take 8 photos at the top, 8 photos at lateral, 8 photos at the bottom
        // top photos
        Camera camera = gaiaScenesContainer.getCamera();
        double increAngRad = Math.toRadians(360.0 / 8.0);
        Matrix4d rotMat = new Matrix4d();
        rotMat.rotateZ(increAngRad);
        Vector3d cameraPosition = new Vector3d(0, -semiMaxLength, semiMaxLength);
        Vector3d cameraTarget = new Vector3d(0, 0, 0);

        for (int i = 0; i < 8; i++) {
            // set camera position
            camera.calculateCameraXYPlane(cameraPosition, cameraTarget);

            takeColorCodedPhoto(renderableGaiaScene, colorRenderFbo, sceneShaderProgram);
            determineExteriorAndInteriorObjects(colorRenderFbo);

            // rotate camPos.
            rotMat.transformPosition(cameraPosition.x, cameraPosition.y, cameraPosition.z, cameraPosition);
        }

        // lateral photos
        cameraPosition = new Vector3d(0, -semiMaxLength, 0);
        cameraTarget = new Vector3d(0, 0, 0);
        for (int i = 0; i < 8; i++) {
            // set camera position
            camera.calculateCameraXYPlane(cameraPosition, cameraTarget);

            takeColorCodedPhoto(renderableGaiaScene, colorRenderFbo, sceneShaderProgram);
            determineExteriorAndInteriorObjects(colorRenderFbo);

            // rotate camPos.
            rotMat.transformPosition(cameraPosition.x, cameraPosition.y, cameraPosition.z, cameraPosition);
        }

        // bottom photos
        cameraPosition = new Vector3d(0, -semiMaxLength, -semiMaxLength);
        cameraTarget = new Vector3d(0, 0, 0);
        for (int i = 0; i < 8; i++) {
            // set camera position
            camera.calculateCameraXYPlane(cameraPosition, cameraTarget);

            takeColorCodedPhoto(renderableGaiaScene, colorRenderFbo, sceneShaderProgram);
            determineExteriorAndInteriorObjects(colorRenderFbo);

            // rotate camPos.
            rotMat.transformPosition(cameraPosition.x, cameraPosition.y, cameraPosition.z, cameraPosition);
        }

        // return camera position
        cameraPosition = new Vector3d(0, 0, -semiMaxLength);
        cameraTarget = new Vector3d(0, 0, 0);

        // set camera position
        camera.calculateCameraXYPlane(cameraPosition, cameraTarget);


        return renderableGaiaScene;
    }

    public Map<GaiaPrimitive, Integer> getExteriorAndInteriorGaiaPrimitivesMap(GaiaScene gaiaScene, Map<GaiaPrimitive, Integer> mapPrimitiveStatus) {
        RenderableGaiaScene renderableGaiaScene = processExteriorInterior(gaiaScene);

        java.util.List<RenderablePrimitive> allRenderablePrimitives = new ArrayList<>();
        renderableGaiaScene.extractRenderablePrimitives(allRenderablePrimitives);
        int renderablePrimitivesCount = allRenderablePrimitives.size();

        // finally make exteriorGaiaSet & interiorGaiaSet
        if (mapPrimitiveStatus == null) {
            mapPrimitiveStatus = new HashMap<>();
        } else {
            mapPrimitiveStatus.clear();
        }

        for (int i = 0; i < renderablePrimitivesCount; i++) {
            RenderablePrimitive renderablePrimitive = allRenderablePrimitives.get(i);
            int status = renderablePrimitive.getStatus();
            if (status == 1) {
                mapPrimitiveStatus.put(renderablePrimitive.getOriginalGaiaPrimitive(), 1);
            } else if (status == 0) {
                mapPrimitiveStatus.put(renderablePrimitive.getOriginalGaiaPrimitive(), 0);
            }
        }

        return mapPrimitiveStatus;
    }

    private void deletePrimitivesByStatus(GaiaNode gaiaNode, int statusToDelete, Map<GaiaPrimitive, Integer> mapPrimitiveStatus) {
        java.util.List<GaiaMesh> gaiaMeshes = gaiaNode.getMeshes();
        int meshesCount = gaiaMeshes.size();
        for (int i = 0; i < meshesCount; i++) {
            GaiaMesh gaiaMesh = gaiaMeshes.get(i);
            java.util.List<GaiaPrimitive> gaiaPrimitives = gaiaMesh.getPrimitives();
            int primitivesCount = gaiaPrimitives.size();
            for (int j = 0; j < primitivesCount; j++) {
                GaiaPrimitive gaiaPrimitive = gaiaPrimitives.get(j);
                int status = mapPrimitiveStatus.get(gaiaPrimitive);
                if (status == statusToDelete) {
                    gaiaPrimitives.remove(j);
                    j--;
                    primitivesCount--;
                }
            }

            // check if the gaiaMesh has no primitives
            if (gaiaPrimitives.isEmpty()) {
                gaiaMeshes.remove(i);
                i--;
                meshesCount--;
            }
        }

        java.util.List<GaiaNode> children = gaiaNode.getChildren();
        int childrenCount = children.size();
        for (int i = 0; i < childrenCount; i++) {
            GaiaNode child = children.get(i);
            deletePrimitivesByStatus(child, statusToDelete, mapPrimitiveStatus);
        }
    }


    public void getExteriorAndInteriorGaiaScenes(GaiaScene gaiaScene, java.util.List<GaiaScene> resultExteriorGaiaScenes, java.util.List<GaiaScene> resultInteriorGaiaScenes) {
        Map<GaiaPrimitive, Integer> mapPrimitiveStatus = getExteriorAndInteriorGaiaPrimitivesMap(gaiaScene, null);

        // finally make exteriorGaiaSet & interiorGaiaSet
        GaiaScene exteriorGaiaScene = gaiaScene.clone();
        GaiaScene interiorGaiaScene = gaiaScene.clone();
        resultExteriorGaiaScenes.add(exteriorGaiaScene);
        resultInteriorGaiaScenes.add(interiorGaiaScene);

        // delete interior primitives from exteriorGaiaScene, and delete exterior primitives from interiorGaiaScene
        java.util.List<GaiaNode> exteriorNodes = exteriorGaiaScene.getNodes();
        int extNodesCount = exteriorNodes.size();
        for (int i = 0; i < extNodesCount; i++) {
            GaiaNode gaiaNode = exteriorNodes.get(i);
            deletePrimitivesByStatus(gaiaNode, 0, mapPrimitiveStatus);
        }

        java.util.List<GaiaNode> interiorNodes = interiorGaiaScene.getNodes();
        int intNodesCount = interiorNodes.size();
        for (int i = 0; i < intNodesCount; i++) {
            GaiaNode gaiaNode = interiorNodes.get(i);
            deletePrimitivesByStatus(gaiaNode, 1, mapPrimitiveStatus);
        }
    }

    public void getExteriorAndInteriorGaiaSets(GaiaScene gaiaScene, java.util.List<GaiaSet> resultExteriorGaiaSets, java.util.List<GaiaSet> resultInteriorGaiaSets) {
        RenderableGaiaScene renderableGaiaScene = processExteriorInterior(gaiaScene);

        java.util.List<RenderablePrimitive> allRenderablePrimitives = new ArrayList<>();
        renderableGaiaScene.extractRenderablePrimitives(allRenderablePrimitives);
        int renderablePrimitivesCount = allRenderablePrimitives.size();

        // finally make exteriorGaiaSet & interiorGaiaSet
        GaiaSet exteriorGaiaSet = new GaiaSet();
        GaiaSet interiorGaiaSet = new GaiaSet();
        resultExteriorGaiaSets.add(exteriorGaiaSet);
        resultInteriorGaiaSets.add(interiorGaiaSet);
        java.util.List<GaiaBufferDataSet> exteriorBufferDatas = new ArrayList<>();
        List<GaiaBufferDataSet> interiorBufferDatas = new ArrayList<>();
        exteriorGaiaSet.setBufferDataList(exteriorBufferDatas);
        interiorGaiaSet.setBufferDataList(interiorBufferDatas);
        for (int i = 0; i < renderablePrimitivesCount; i++) {
            RenderablePrimitive renderablePrimitive = allRenderablePrimitives.get(i);
            int status = renderablePrimitive.getStatus();
            if (status == 1) {
                GaiaBufferDataSet gaiaBufferDataSet = renderablePrimitive.getOriginalBufferDataSet();
                exteriorBufferDatas.add(gaiaBufferDataSet);
            } else if (status == 0) {
                GaiaBufferDataSet gaiaBufferDataSet = renderablePrimitive.getOriginalBufferDataSet();
                interiorBufferDatas.add(gaiaBufferDataSet);
            }
        }
    }

    private void renderScreenQuad(int texId) {
        // render to windows using screenQuad
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

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
        // render into frame buffer
        FboManager fboManager = this.getFboManager();
        Window window = this.getWindow();
        int fboWidthColor = window.getWidth();
        int fboHeightColor = window.getHeight();
        Fbo colorRenderFbo = fboManager.getOrCreateFbo("colorRender", fboWidthColor, fboHeightColor);

        colorRenderFbo.bind();

        int[] width = new int[1];
        int[] height = new int[1];
        width[0] = colorRenderFbo.getFboWidth();
        height[0] = colorRenderFbo.getFboHeight();
        glViewport(0, 0, width[0], height[0]);

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // render scene objects
        ShaderProgram sceneShaderProgram = shaderManager.getShaderProgram("scene");

        sceneShaderProgram.bind(); // bind the shader program

        // set modelViewMatrix and projectionMatrix
        UniformsMap uniformsMap = sceneShaderProgram.getUniformsMap();
        Matrix4f projectionMatrix = gaiaScenesContainer.getProjection().getProjMatrix();

        uniformsMap.setUniformMatrix4fv("uProjectionMatrix", projectionMatrix);
        Camera camera = gaiaScenesContainer.getCamera();
        Matrix4d modelViewMatrix = camera.getModelViewMatrix();
        uniformsMap.setUniformMatrix4fv("uModelViewMatrix", new Matrix4f(modelViewMatrix));

        Matrix4f identityMatrix = new Matrix4f();
        identityMatrix.identity();
        uniformsMap.setUniformMatrix4fv("uObjectMatrix", identityMatrix);

        // colorMode = 0: oneColor, 1: vertexColor, 2: textureColor
        uniformsMap.setUniform1i("uColorMode", 0);
        Vector4f oneColor = new Vector4f(1, 0.5f, 0.25f, 1);
        uniformsMap.setUniform4fv("uOneColor", oneColor);


        if (renderAxis) {
            renderer.renderAxis(sceneShaderProgram);
        }

        if (!gaiaScenesContainer.getRenderableGaiaScenes().isEmpty()) {
            renderer.render(gaiaScenesContainer, sceneShaderProgram);
        }

        if (!this.halfEdgeScenes.isEmpty()) {
            halfEdgeRenderer.renderHalfEdgeScenes(halfEdgeScenes, sceneShaderProgram);
        }
        sceneShaderProgram.unbind(); // unbind the shader program

        colorRenderFbo.unbind();

        // now render to windows using screenQuad
        int colorRenderTextureId = colorRenderFbo.getColorTextureId();
        renderScreenQuad(colorRenderTextureId);


//        // render colorCoded fbo
//        int colorCodeRenderTextureId = fboManager.getFbo("colorCodeRender").getColorTextureId();
//        renderScreenQuad(colorCodeRenderTextureId);

    }

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.

        int[] width = new int[1];
        int[] height = new int[1];
        long windowHandle = window.getWindowHandle();
        while (!glfwWindowShouldClose(windowHandle)) {

            glfwGetWindowSize(windowHandle, width, height);
            glViewport(0, 0, width[0], height[0]);

            glEnable(GL_DEPTH_TEST);
            glPointSize(5.0f);
            glClearColor(0.5f, 0.23f, 0.98f, 1.0f);
            glClearDepth(1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            draw();
            glfwSwapBuffers(windowHandle);
            glfwPollEvents();
        }
    }

    public void deleteBuffer(int vboId) {
        GL20.glDeleteBuffers(vboId);
    }


}
