package com.gaia3d.process.preprocess;

import com.gaia3d.TilerExtensionModule;
import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.voxel.VoxelGrid3D;
import com.gaia3d.basic.geometry.voxel.VoxelizeParameters;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.GaiaTextureUtils;
import com.gaia3d.util.ImageUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@AllArgsConstructor
public class GaiaMinimizerML implements PreProcess {

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();
        minimizeGaiaScene(tileInfo, scene);

        //GaiaPointCloud pointCloud = tileInfo.getPointCloud();
        //minimizeGaiaPointCloud(tileInfo, pointCloud);
        return tileInfo;
    }

    private void minimizeGaiaScene(TileInfo tileInfo, GaiaScene scene) {
        if (scene != null) {
            Matrix4d tileInfoTransformMatrix = tileInfo.getTransformMatrix();
            scene.spendTranformMatrix();
            GaiaScene mcScene = marchingCubeVoxelization(tileInfo, scene);

            // 1rst save the mcScene in a temp folder.***
            Path originalPath = scene.getOriginalPath();
            String originalFileName = originalPath.getFileName().toString();
            String rawOriginalFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
            Path outputFolderPath = tileInfo.getOutputPath();
            Path tempMLPath = outputFolderPath.resolve("tempML");
            Path sceneMLPath = tempMLPath.resolve(rawOriginalFileName);
            Path sceneMLImagesPath = sceneMLPath.resolve("images");

            // create folders.***
            File sceneMLImagesFolder = new File(sceneMLImagesPath.toString());
            if (!sceneMLImagesFolder.exists()) {
                sceneMLImagesFolder.mkdirs();
            }

            // save the textures of marchingCubeScene.***
            List<GaiaMaterial> materials = mcScene.getMaterials();
            int materialsCount = materials.size();
            for (int i = 0; i < materialsCount; i++) {
                GaiaMaterial material = materials.get(i);
                Map<TextureType, List<GaiaTexture>> textures = material.getTextures();
                TextureType textureType = TextureType.DIFFUSE;
                List<GaiaTexture> textureList = textures.get(textureType);
                if (textureList != null) {
                    int textureListCount = textureList.size();
                    for (int j = 0; j < textureListCount; j++) {
                        GaiaTexture texture = textureList.get(j);

                        // set parentPath to the texture.***
                        texture.setParentPath(sceneMLImagesPath.toString());

                        // save the bufferedImage into the sceneMLImagesPath.***
                        BufferedImage bufferedImage = texture.getBufferedImage();

                        try {
                            String imagePath = sceneMLImagesPath + File.separator + texture.getPath();
                            File imageFile = new File(imagePath);
                            ImageIO.write(bufferedImage, "png", imageFile);
                        } catch (IOException e) {
                            log.debug("Error writing image: {}", e);
                        }
                        int hola = 0;
                    }
                }
            }

            GaiaSet tempSet = GaiaSet.fromGaiaScene(mcScene);
            Path tempPath = tempSet.writeFile(tileInfo.getTempPath(), tileInfo.getSerial(), tempSet.getAttribute());
            tileInfo.setTempPath(tempPath);
            tempSet.clear();
            tempSet = null;
            scene.clear();
            scene = null;
        }
    }

    private GaiaScene marchingCubeVoxelization(TileInfo tileInfo, GaiaScene scene) {
        if (scene == null) {
            return null;
        }

        List<GaiaScene> gaiaScenes = new ArrayList<>();
        gaiaScenes.add(scene);

        GaiaBoundingBox boundingBox = scene.getBoundingBox();
        double maxSize = boundingBox.getMaxSize();

        TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();
        VoxelizeParameters voxelizeParameters = new VoxelizeParameters();

        double voxelSizeMeter = 2.0;
        double texturePixelsForMeter = 4.0;

        // DC_Library scale 0.01 settings.***
        voxelSizeMeter = 0.5;
        texturePixelsForMeter = 30.0;

        // tree settings.***
        voxelSizeMeter = 0.2;
        texturePixelsForMeter = 80.0;

        voxelSizeMeter = 400.0;
        texturePixelsForMeter = 0.01;

        voxelSizeMeter = 100.0;
        texturePixelsForMeter = 0.01;

        voxelSizeMeter = maxSize / 1000.0;
        texturePixelsForMeter = 0.01;
//
//        voxelSizeMeter = 0.1; // tree.***

        // thailand settings.***
//        voxelSizeMeter = 5.0;
//        texturePixelsForMeter = 10.0;

        voxelizeParameters.setVoxelsForMeter(1.0 / voxelSizeMeter);

        voxelizeParameters.setTexturePixelsForMeter(texturePixelsForMeter);
        List<VoxelGrid3D> resultVoxelGrids = new ArrayList<>();
        List<GaiaScene> resultGaiaScenes = new ArrayList<>();
        tilerExtensionModule.voxelize(gaiaScenes, resultVoxelGrids, resultGaiaScenes, voxelizeParameters);
        tilerExtensionModule.deleteObjects();

        GaiaScene result = resultGaiaScenes.get(0);
        return result;
    }

    private void minimizeGaiaPointCloud(TileInfo tileInfo, GaiaPointCloud pointCloud) {
        if (pointCloud != null) {
            //Path tempPath = tempSet.writeFile(tileInfo.getTempPath(), tileInfo.getSerial(), tempSet.getAttribute());
            tileInfo.setTempPath(tileInfo.getOutputPath().resolve("temp"));

            GaiaAttribute attribute = pointCloud.getGaiaAttribute();
            String id = attribute.getIdentifier().toString();

            File tempFile = new File(tileInfo.getTempPath().toString(), id);
            pointCloud.minimize(tempFile);

            log.info("Minimized point cloud: {}", tempFile.getAbsolutePath());
        }
    }
}
