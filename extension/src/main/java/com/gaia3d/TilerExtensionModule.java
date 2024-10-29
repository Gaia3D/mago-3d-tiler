package com.gaia3d;

import com.gaia3d.basic.exchangable.SceneInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.renderer.MainRenderer;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

@Slf4j
public class TilerExtensionModule implements ExtensionModuleFrame {

    @Override
    public String getName() {
        return "Extension Project";
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public GaiaScene executePhotorealistic(GaiaScene gaiaScene, Map<String, Object> options) {
        // TODO: Implement this method
        log.info("----------------------------------------");
        log.info("Extension has been applied.");
        log.info("----------------------------------------");


        return null;
    }

    public void getColorAndDepthRender(List<SceneInfo> sceneInfos, int bufferedImageType, List<BufferedImage> resultImages, GaiaBoundingBox nodeBBox, Matrix4d nodeTMatrix, int maxScreenSize)
    {
        MainRenderer renderer = new MainRenderer();
        renderer.getColorAndDepthRender(sceneInfos, bufferedImageType, resultImages, nodeBBox, nodeTMatrix, maxScreenSize);
    }


    public void getRenderScene(List<GaiaScene> scenes, int bufferedImageType, int maxScreenSize, List<BufferedImage> resultImages)
    {
        MainRenderer renderer = new MainRenderer();
        renderer.render(scenes, bufferedImageType, resultImages, maxScreenSize);
    }
}
