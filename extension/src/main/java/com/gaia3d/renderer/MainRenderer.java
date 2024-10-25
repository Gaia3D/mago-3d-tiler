package com.gaia3d.renderer;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.renderer.engine.Engine;
import com.gaia3d.renderer.engine.IAppLogic;
import com.gaia3d.renderer.engine.InternDataConverter;
import com.gaia3d.renderer.engine.Window;
import com.gaia3d.renderer.engine.dataStructure.GaiaScenesContainer;
import com.gaia3d.renderer.engine.fbo.FboManager;
import com.gaia3d.renderer.engine.scene.Camera;
import com.gaia3d.renderer.engine.scene.Projection;
import com.gaia3d.renderer.renderable.RenderableGaiaScene;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MainRenderer implements IAppLogic {
    private Engine engine = new Engine("MagoVisual3D", new Window.WindowOptions(), this);

    public void render() {
        // render the scene
        log.info("Rendering the scene...");
        try{
            engine.run();
        } catch (Exception e) {
            log.error("Error initializing the engine: " + e.getMessage());
        }

    }

    public void render(List<GaiaScene> gaiaScenes, int bufferedImageType, List<BufferedImage> resultImages) {
        // render the scene
        log.info("Rendering the scene...");

        // Must init gl.***
        try{
            engine.init();
        } catch (Exception e) {
            log.error("Error initializing the engine: " + e.getMessage());
        }

        int screenWidth = 1000;
        int screenHeight = 600;

        InternDataConverter internDataConverter = new InternDataConverter();
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
        int maxScreenSize = 2048;
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
        fboManager.createFbo("colorRender", fboWidth, fboHeight);

        // now set camera position.***
        Camera camera = new Camera();
        camera.setPosition(bboxCenter);
        camera.setDirection(new Vector3d(0, 0, -1));
        camera.setUp(new Vector3d(0, 1, 0));
        gaiaScenesContainer.setCamera(camera);

        try{
            BufferedImage image = engine.getRenderSceneImage(bufferedImageType);
            resultImages.add(image);
        } catch (Exception e) {
            log.error("Error initializing the engine: " + e.getMessage());
        }

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
