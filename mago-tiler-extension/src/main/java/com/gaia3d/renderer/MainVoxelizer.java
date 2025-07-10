package com.gaia3d.renderer;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.voxel.VoxelGrid3D;
import com.gaia3d.basic.geometry.voxel.VoxelizeParameters;
import com.gaia3d.basic.halfedge.HalfEdgeFace;
import com.gaia3d.basic.halfedge.HalfEdgeScene;
import com.gaia3d.basic.halfedge.HalfEdgeSurface;
import com.gaia3d.basic.halfedge.HalfEdgeUtils;
import com.gaia3d.basic.marchingcube.MarchingCube;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.renderer.engine.*;
import com.gaia3d.renderer.engine.dataStructure.GaiaScenesContainer;
import com.gaia3d.renderer.engine.fbo.Fbo;
import com.gaia3d.renderer.engine.fbo.FboManager;
import com.gaia3d.renderer.engine.graph.ShaderManager;
import com.gaia3d.renderer.engine.graph.ShaderProgram;
import com.gaia3d.renderer.engine.scene.Projection;
import com.gaia3d.renderer.engine.scene.Camera;
import com.gaia3d.renderer.renderable.RenderableGaiaScene;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.awt.image.BufferedImage;

import java.util.*;

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

        double texturePixelsForMeter = voxelizeParameters.getTexturePixelsForMeter();
        engine.makeBoxTexturesByObliqueCamera(halfEdgeScene, texturePixelsForMeter);
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
}
