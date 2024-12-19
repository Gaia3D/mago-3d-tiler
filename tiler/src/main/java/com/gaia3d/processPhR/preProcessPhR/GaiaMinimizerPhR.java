package com.gaia3d.processPhR.preProcessPhR;

import com.gaia3d.TilerExtensionModule;
import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.halfedge.DecimateParameters;
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
            double error = 1e-4;
            scene.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);
            scene.deleteDegeneratedFaces();

//            // test render.****************************************************************
//            TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();
//            List<GaiaScene> gaiaSceneList = new ArrayList<>();
//            gaiaSceneList.add(scene);
//            List<GaiaScene> resultScenes = new ArrayList<>();
//            tilerExtensionModule.renderPyramidDeformation(gaiaSceneList, resultScenes);
//            // End test.-----------------------------------------------------------------

            List<Path> tempPathLod = new ArrayList<>();
            Path tempFolder = tileInfo.getTempPath();

            // Lod 0.************************************************************************************************************
            log.info("Minimize GaiaScene LOD 0 , Path : {}", tileInfo.getTempPath());

            GaiaSet tempSetLod0 = GaiaSet.fromGaiaScene(scene);
            Path tempPathLod0 = tempSetLod0.writeFile(tileInfo.getTempPath(), tileInfo.getSerial(), tempSetLod0.getAttribute());
            tileInfo.setTempPath(tempPathLod0);
            tempPathLod.add(tempPathLod0);

            if (tempSetLod0 != null) {
                tempSetLod0.clear();
                tempSetLod0 = null;
            }

            if (scene != null) {
                scene.clear();
                scene = null;
            }

        }

        System.gc();
        return tileInfo;
    }

    public TileInfo run_old(TileInfo tileInfo) {
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

            if (tempSetLod0 != null) {
                tempSetLod0.clear();
                tempSetLod0 = null;
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
