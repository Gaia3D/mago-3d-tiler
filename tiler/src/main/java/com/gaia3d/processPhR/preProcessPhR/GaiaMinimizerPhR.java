package com.gaia3d.processPhR.preProcessPhR;

import com.gaia3d.TilerExtensionModule;
import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.halfedge.HalfEdgeScene;
import com.gaia3d.basic.halfedge.HalfEdgeUtils;
import com.gaia3d.basic.halfedge.PlaneType;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.preprocess.PreProcess;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


@Slf4j
//@AllArgsConstructor
public class GaiaMinimizerPhR implements PreProcess {
    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();
        scene.deleteNormals(); // test delete.**************************************

        if (scene != null) {
            log.info("Welding vertices in GaiaScene");
            GlobalOptions globalOptions = GlobalOptions.getInstance();

            // 1rst, must weld vertices.***
            boolean checkTexCoord = true;
            boolean checkNormal = false;
            boolean checkColor = false;
            boolean checkBatchId = false;
            double error = 1e-6;
            scene.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);
            scene.deleteDegeneratedFaces();
//            HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);
//            halfEdgeScene.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);
//            scene.clear();
//            scene = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);

//            // Test create a rectangularNet scene.***
//            boolean calculateTexCoords = true;
//            int numCols = 157;
//            int numRows = 214;
//            double width = 100;
//            double height = 100;
//            GaiaScene rectScene = GaiaSceneUtils.getSceneRectangularNet(numCols, numRows, width, height, calculateTexCoords);
//            GaiaNode rootNode = rectScene.getNodes().get(0);
//            GaiaNode node = rootNode.getChildren().get(0);
//            GaiaMesh mesh = node.getMeshes().get(0);
//            GaiaPrimitive primitive = mesh.getPrimitives().get(0);
//            Vector3d translate = new Vector3d(0, 0, 20);
//            primitive.translate(translate);
//            primitive.setMaterialIndex(0);
//            scene.getNodes().clear();
//            scene.getNodes().add(rootNode);
//            // End test.------------------------


            List<Path> tempPathLod = new ArrayList<>();
            Path tempFolder = tileInfo.getTempPath();

            // Lod 0.************************************************************************************************************
            log.info("Minimize GaiaScene LOD 0 , Path : {}", tileInfo.getTempPath());

//                        // test.***
//                        HalfEdgeScene halfEdgeSceneLod0 = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);
//                        halfEdgeSceneLod0.TEST_cutScene();
//                        GaiaScene sceneLod0 = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeSceneLod0);
//                        // end test.***

            GaiaSet tempSetLod0 = GaiaSet.fromGaiaScene(scene);
            Path tempPathLod0 = tempSetLod0.writeFile(tileInfo.getTempPath(), tileInfo.getSerial(), tempSetLod0.getAttribute());
            tileInfo.setTempPath(tempPathLod0);
            tempPathLod.add(tempPathLod0);

            // Lod 1.************************************************************************************************************
            log.info("Minimize GaiaScene LOD 1");
            log.info("Making HalfEdgeScene from GaiaScene");
            HalfEdgeScene halfEdgeSceneLod1 = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);

            log.info("Doing triangles reduction in HalfEdgeScene");
            double maxDiffAngDegrees = 70.0;
            double hedgeMinLength = 0.25;
            double frontierMaxDiffAngDeg = 40.0;
            double maxAspectRatio = 6.0;
            halfEdgeSceneLod1.doTrianglesReduction(maxDiffAngDegrees, frontierMaxDiffAngDeg, hedgeMinLength, maxAspectRatio);
            //halfEdgeScene.calculateNormals();


            log.info("Making GaiaScene from HalfEdgeScene");
            GaiaScene sceneLod1 = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeSceneLod1);
            halfEdgeSceneLod1.deleteObjects();

            GaiaSet tempSetLod1 = GaiaSet.fromGaiaScene(sceneLod1);

            Path tempFolderLod1 = tempFolder.resolve("lod1");
            Path tempPathLod1 = tempSetLod1.writeFile(tempFolderLod1, tileInfo.getSerial(), tempSetLod1.getAttribute());
            tempPathLod.add(tempPathLod1);


            // Lod 2.************************************************************************************************************
            // In Lod2, change the texture by a topView render.***
            List<GaiaScene> gaiaSceneList = new ArrayList<>();
            gaiaSceneList.add(scene);
            //TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();
            List<BufferedImage> resultImages = new ArrayList<>();
            int bufferedImageType = BufferedImage.TYPE_INT_RGB;
            int maxScreenSize = 1024;
            TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();
            tilerExtensionModule.getRenderScene(gaiaSceneList, bufferedImageType, maxScreenSize, resultImages);

            if(resultImages.size() == 0)
            {
                log.error("resultImages.size() == 0");
                int hola = 0;
            }

            // test.***
            String sceneName = scene.getOriginalPath().getFileName().toString();
            String sceneRawName = sceneName.substring(0, sceneName.lastIndexOf("."));
            String outputFolderPath = globalOptions.getOutputPath();
            File file = new File(outputFolderPath + sceneRawName + ".jpg");
            try
            {
                ImageIO.write(resultImages.get(0), "JPG", file);
            }
            catch (Exception e)
            {
                log.error("Error Log : ", e);
            }
            // end test.---
            // end change texture.-----------------------------------

            log.info("Minimize GaiaScene LOD 2");
            checkTexCoord = false;
            scene.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);
            scene.deleteDegeneratedFaces();

            log.info("Making HalfEdgeScene from GaiaScene");
            HalfEdgeScene halfEdgeSceneLod2 = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);

            log.info("Doing triangles reduction in HalfEdgeScene");
            maxDiffAngDegrees = 45.0;
            hedgeMinLength = 0.5;
            frontierMaxDiffAngDeg = 30.0;
            maxAspectRatio = 6.0;
            halfEdgeSceneLod2.doTrianglesReduction(maxDiffAngDegrees, frontierMaxDiffAngDeg, hedgeMinLength, maxAspectRatio);
            //halfEdgeScene.calculateNormals();

            // As we will change the texture by a topView render, must recalculate the texCoords.***
            GaiaBoundingBox gaiaBoundingBox = halfEdgeSceneLod2.getBoundingBox();
            halfEdgeSceneLod2.setBoxTexCoordsXY(gaiaBoundingBox);

            // change the diffuse texture of the material by a topView render.***
            List<GaiaMaterial> materials = halfEdgeSceneLod2.getUsingMaterialsWithTextures(null);
            int materialsCount = materials.size();
            for (int i = 0; i < materialsCount; i++)
            {
                GaiaMaterial material = materials.get(i);
                List<GaiaTexture> textures = material.getTextures().get(TextureType.DIFFUSE);
                int texturesCount = textures.size();
                for (int j = 0; j < texturesCount; j++)
                {
                    GaiaTexture texture = textures.get(j);
                    String parentPath = texture.getParentPath();
                    String path = texture.getPath();
                    String pathRawOfPath = path.substring(0, path.lastIndexOf("."));
                    String imageExtension = path.substring(path.lastIndexOf(".") + 1);
                    String newPath = pathRawOfPath + "_topView." + imageExtension;

                    // set the new path to the texture.***
                    texture.setPath(newPath);

                    // change the texture by a topView render.***
                    // 1rst, get the topView render image.***
                    BufferedImage topViewImage = resultImages.get(0);
                    if (topViewImage == null)
                    {
                        log.error("topViewImage is null");
                        continue;
                    }

                    // 2nd, save the topViewImage.***
                    String topViewImagePath = parentPath + newPath;
                    File topViewImageFile = new File(topViewImagePath);
                    try{
                        ImageIO.write(topViewImage, imageExtension, topViewImageFile);
                    }
                    catch (Exception e)
                    {
                        log.error("Error Log : ", e);
                    }

                    // close the image.***
                    topViewImage = null;
                }
            }

            log.info("Making GaiaScene from HalfEdgeScene");
            GaiaScene sceneLod2 = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeSceneLod2);
            halfEdgeSceneLod2.deleteObjects();

            GaiaSet tempSetLod2 = GaiaSet.fromGaiaScene(sceneLod2);

            Path tempFolderLod2 = tempFolder.resolve("lod2");
            Path tempPathLod2 = tempSetLod2.writeFile(tempFolderLod2, tileInfo.getSerial(), tempSetLod2.getAttribute());
            tempPathLod.add(tempPathLod2);

            // set tempPathLod to tileInfo.***
            tileInfo.setTempPathLod(tempPathLod);

            if (tempSetLod0 != null) {
                tempSetLod0.clear();
                tempSetLod0 = null;
            }

            if (tempSetLod1 != null) {
                tempSetLod1.clear();
                tempSetLod1 = null;
            }

            if (tempSetLod2 != null) {
                tempSetLod2.clear();
                tempSetLod2 = null;
            }

            if (scene != null) {
                scene.clear();
                scene = null;
            }

            if(sceneLod2 != null)
            {
                sceneLod2.clear();
                sceneLod2 = null;
            }

        }

        System.gc();
        return tileInfo;
    }

    private List<HalfEdgeScene> testCutHalfEdgeScene(HalfEdgeScene halfEdgeScene) {
        List<HalfEdgeScene> halfEdgeCutScenes = null;
        PlaneType planeType = PlaneType.XZ;

//        if(halfEdgeScene.cutByPlane(planeType, samplePointLC, error))
//        {
//            deletedTileInfoMap.put(tileInfo, tileInfo);
//            // once scene is cut, then save the 2 scenes and delete the original.***
//            halfEdgeScene.classifyFacesIdByPlane(planeType, samplePointLC);
//
//            halfEdgeCutScenes = HalfEdgeUtils.getCopyHalfEdgeScenesByFaceClassifyId(halfEdgeScene, null);
//
//            // create tileInfos for the cut scenes.***
//            for(HalfEdgeScene halfEdgeCutScene : halfEdgeCutScenes)
//            {
//                GaiaScene gaiaSceneCut = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeCutScene);
//
//                // create an originalPath for the cut scene.***
//                Path cutScenePath = Paths.get("");
//                gaiaSceneCut.setOriginalPath(cutScenePath);
//
//                GaiaSet gaiaSetCut = GaiaSet.fromGaiaScene(gaiaSceneCut);
//                UUID identifier = UUID.randomUUID();
//                Path gaiaSetCutFolderPath = cutTempLodPath.resolve(identifier.toString());
//                if(!gaiaSetCutFolderPath.toFile().exists())
//                {
//                    gaiaSetCutFolderPath.toFile().mkdirs();
//                }
//
//                Path tempPathLod = gaiaSetCut.writeFile(gaiaSetCutFolderPath);
//
//                // create a new tileInfo for the cut scene.***
//                TileInfo tileInfoCut = TileInfo.builder().scene(gaiaSceneCut).outputPath(tileInfo.getOutputPath()).build();
//                tileInfoCut.setTempPath(tempPathLod);
//
//                // make a kmlInfo for the cut scene.***
//                // In reality, we must recalculate the position of the cut scene. Provisionally, we use the same position.***
//                // In reality, we must recalculate the position of the cut scene. Provisionally, we use the same position.***
//                KmlInfo kmlInfoCut = KmlInfo.builder().position(geoCoordPosition).build();
//                tileInfoCut.setKmlInfo(kmlInfoCut);
//                cutTileInfos.add(tileInfoCut);
//            }
//
//        }

        return halfEdgeCutScenes;
    }
}
