
package com.gaia3d.process.postprocess;

import com.gaia3d.TilerExtensionModule;
import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.entities.GaiaAAPlane;
import com.gaia3d.basic.geometry.voxel.VoxelGrid3D;
import com.gaia3d.basic.geometry.voxel.VoxelizeParameters;
import com.gaia3d.basic.halfedge.DecimateParameters;
import com.gaia3d.basic.halfedge.HalfEdgeScene;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.util.GlobeUtils;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;

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
public class GaiaRelocationML implements PostProcess {
    @Override
    public ContentInfo run(ContentInfo contentInfo) {
        GaiaBoundingBox allBoundingBox = contentInfo.getBoundingBox();
        Vector3d centerCartographic = allBoundingBox.getCenter();
        Vector3d centerCartesian = GlobeUtils.geographicToCartesianWgs84(centerCartographic);
        Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerCartesian);
        Matrix4d transformMatrixInv = new Matrix4d(transformMatrix).invert();
        for (TileInfo tileInfo : contentInfo.getTileInfos()) {
            KmlInfo kmlInfo = tileInfo.getKmlInfo();
            Vector3d kmlCenter = kmlInfo.getPosition();
            kmlCenter = GlobeUtils.geographicToCartesianWgs84(kmlCenter);

            Matrix4d resultTransformMatrix = transformMatrixInv.translate(kmlCenter, new Matrix4d());

            double x = resultTransformMatrix.get(3, 0);
            double y = resultTransformMatrix.get(3, 1);
            double z = resultTransformMatrix.get(3, 2);

            Vector3d translation = new Vector3d(x, y, z);

            GaiaSet set = tileInfo.getSet();
            if (set == null) {
                log.error("GaiaSet is null");
                continue;
            }
            set.translate(translation);

            // test marching cube.********************************************************************************************
            GaiaScene scene = new GaiaScene(set);
            scene.setOriginalPath(tileInfo.getScenePath());
            scene.deleteNormals(); // necessary to render only the albedo texture.
            GaiaScene mcScene = marchingCubeVoxelization(tileInfo, scene);
            mcScene.weldVertices(1e-10, true, false, false, false);

            // now decimate the scene.***
            mcScene.calculateVertexNormals();

            // 1rst save the mcScene in a temp folder.***
            Path originalPath = scene.getOriginalPath();
            String originalFileName = originalPath.getFileName().toString();
            String rawOriginalFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
            Path tempFilePath = tileInfo.getTempPath();
            Path tempFolderPath = tempFilePath.getParent();
            Path outputFolderPath = tempFolderPath;
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


            GaiaSet mcSet = GaiaSet.fromGaiaScene(mcScene);
            tileInfo.setSet(mcSet);
        }
        return contentInfo;
    }

    private void decimateScene(GaiaScene scene) {
        List<GaiaScene> gaiaScenes = new ArrayList<>();
        gaiaScenes.add(scene);
        List<HalfEdgeScene> resultDecimatedHalfEdgeScenes = new ArrayList<>();
        DecimateParameters decimateParameters = new DecimateParameters();
        boolean makeSkirt = false;

        GaiaBoundingBox boundingBox = scene.getBoundingBox();
        double maxSize = boundingBox.getMaxSize();
        double texturePixelSize = 1.0;
        texturePixelSize = maxSize / 256.0;
        double texturePixelsForMeter = 1.0 / texturePixelSize;

        TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();
        List<GaiaAAPlane> cuttingPlanes = new ArrayList<>();
        //tilerExtensionModule.decimateAndCutByObliqueCamera(gaiaScenes, resultDecimatedHalfEdgeScenes, decimateParameters, halfEdgeOctree, cuttingPlanes, texturePixelsForMeter, makeSkirt);
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

        double voxelSizeMeter = 1.0;
        double texturePixelSize = 1.0;
        double texturePixelsForMeter = 4.0;

        voxelSizeMeter = maxSize / 17.0;
        texturePixelSize = maxSize / 256.0;
        texturePixelsForMeter = 1.0 / texturePixelSize;

        voxelizeParameters.setVoxelsForMeter(1.0 / voxelSizeMeter);
        voxelizeParameters.setTexturePixelsForMeter(texturePixelsForMeter);

        List<VoxelGrid3D> resultVoxelGrids = new ArrayList<>();
        List<GaiaScene> resultGaiaScenes = new ArrayList<>();
        tilerExtensionModule.voxelize(gaiaScenes, resultVoxelGrids, resultGaiaScenes, voxelizeParameters);
        tilerExtensionModule.deleteObjects();

        GaiaScene result = resultGaiaScenes.get(0);
        return result;
    }
}
