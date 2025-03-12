package com.gaia3d.processPhR.tileProcessPhR;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.TilerExtensionModule;
import com.gaia3d.basic.exception.TileProcessingException;
import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.exchangable.SceneInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.entities.GaiaAAPlane;
import com.gaia3d.basic.geometry.octree.HalfEdgeOctree;
import com.gaia3d.basic.halfedge.*;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.process.tileprocess.Tiler;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.DefaultTiler;
import com.gaia3d.process.tileprocess.tile.LevelOfDetail;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;
import com.gaia3d.process.tileprocess.tile.tileset.asset.Asset;
import com.gaia3d.process.tileprocess.tile.tileset.node.BoundingVolume;
import com.gaia3d.process.tileprocess.tile.tileset.node.Content;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import com.gaia3d.util.DecimalUtils;
import com.gaia3d.util.GaiaSceneUtils;
import com.gaia3d.util.GlobeUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor
public class Batched3DModelTilerPhR extends DefaultTiler implements Tiler {
    public final GlobalOptions globalOptions = GlobalOptions.getInstance();

    @Override
    public Tileset run(List<TileInfo> tileInfos) throws FileNotFoundException {
        //**************************************************************
        // In photoRealistic, 1rst make an empty octTree.              *
        // then use rectangleCakeCutter to fill the octTree.           *
        //**************************************************************
        GaiaBoundingBox globalBoundingBox = calcBoundingBox(tileInfos);

        // make globalBoundingBox as square
        double minLonDeg = globalBoundingBox.getMinX();
        double minLatDeg = globalBoundingBox.getMinY();
        double maxLonDeg = globalBoundingBox.getMaxX();
        double maxLatDeg = globalBoundingBox.getMaxY();

        // calculate the rootOctTree size
        double minLatRad = Math.toRadians(minLatDeg);
        double maxLatRad = Math.toRadians(maxLatDeg);
        double minLonRad = Math.toRadians(minLonDeg);
        double maxLonRad = Math.toRadians(maxLonDeg);

        // find max distance
        double distanceBetweenLat = GlobeUtils.distanceBetweenLatitudesRad(minLatRad, maxLatRad);
        double distanceBetweenLon = GlobeUtils.distanceBetweenLongitudesRad(minLatRad, minLonRad, maxLonRad);
        double distanceFinal = Math.max(distanceBetweenLat, distanceBetweenLon);

        double desiredLeafDist = GlobalOptions.REALISTIC_LEAF_TILE_SIZE;

        int projectMaxDepthIdx = (int) Math.ceil(HalfEdgeUtils.log2(distanceFinal / desiredLeafDist));
        //int desiredDepthsCount = projectMaxDepthIdx + 1; // because the root is depth 0
        double desiredDistanceBetweenLat = desiredLeafDist * Math.pow(2, projectMaxDepthIdx);
        double desiredAngRadLat = GlobeUtils.angRadLatitudeForDistance(minLatRad, desiredDistanceBetweenLat);
        double desiredAngRadLon = GlobeUtils.angRadLongitudeForDistance(minLatRad, desiredDistanceBetweenLat);
        double desiredAngDegLat = Math.toDegrees(desiredAngRadLat);
        double desiredAngDegLon = Math.toDegrees(desiredAngRadLon);
        maxLonDeg = minLonDeg + desiredAngDegLon;
        maxLatDeg = minLatDeg + desiredAngDegLat;
        double minZ = globalBoundingBox.getMinZ();
        double maxZ = globalBoundingBox.getMaxZ();
        // end calculate the rootOctTree size.---

        // make CUBE boundingBox
        globalBoundingBox.setMaxZ(globalBoundingBox.getMinZ() + desiredDistanceBetweenLat);// make CUBE boundingBox
        globalBoundingBox = new GaiaBoundingBox(minLonDeg, minLatDeg, globalBoundingBox.getMinZ(), maxLonDeg, maxLatDeg, globalBoundingBox.getMaxZ(), false);

        Matrix4d transformMatrix = getTransformMatrix(globalBoundingBox);
        if (globalOptions.isClassicTransformMatrix()) {
            rotateX90(transformMatrix);
        }

        Node root = createRoot();
        root.setNodeCode("R");
        root.setDepth(0);
        root.setBoundingVolume(new BoundingVolume(globalBoundingBox));
        root.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());

        /* Start lod 0 process */
        int lod = 0;
        List<TileInfo> tileInfosCopy = this.getTileInfosCopy(tileInfos, lod, null);

        //int maxDepth = root.findMaxDepth();
        int currDepth = projectMaxDepthIdx - lod;
        Map<Node, List<TileInfo>> nodeTileInfoMap = new HashMap<>();

        List<TileInfo> cuttedTileInfos = new ArrayList<>();
        cuttingAndScissorProcessST(tileInfosCopy, lod, root, cuttedTileInfos, projectMaxDepthIdx);

        // distribute contents to node in the correspondent depth
        // After process "cutRectangleCake", in tileInfosCopy there are tileInfos that are cut by the boundary planes of the nodes
        distributeContentsToNodesOctTree(root, cuttedTileInfos, currDepth, nodeTileInfoMap);
        makeContentsForNodes(nodeTileInfoMap, lod);
        /* End lod 0 process */

        //int netSurfaceStartLod = 3;

        DecimateParameters decimateParameters = new DecimateParameters();
        for (int d = 1; d < projectMaxDepthIdx; d++) {
            lod = d;
            tileInfosCopy.clear();
            nodeTileInfoMap.clear();
            tileInfosCopy = this.getTileInfosCopy(tileInfos, lod, tileInfosCopy);
            double screenPixelsForMeterLod1 = 22.0;
            double screenPixelsForMeter = 0.0;
            if (d == 1) {
                decimateParameters.setBasicValues(6.0, 0.4, 0.9, 32.0, 1000000, 1, 1.0);
                decimateParameters.setLod(d);
                screenPixelsForMeter = screenPixelsForMeterLod1;
            } else if (d == 2) {
                decimateParameters.setBasicValues(11.0, 0.4, 1.0, 32.0, 1000000, 2, 1.5);
                decimateParameters.setLod(d);
                screenPixelsForMeter = screenPixelsForMeterLod1 / 2.0;
            } else if (d == 3) {
                decimateParameters.setBasicValues(16.0, 0.6, 1.0, 32.0, 1000000, 2, 2.0);
                decimateParameters.setLod(d);
                screenPixelsForMeter = screenPixelsForMeterLod1 / 4.0;
            } else if (d == 4) {
                decimateParameters.setBasicValues(22.0, 0.8, 1.0, 32.0, 1000000, 2, 2.5);
                decimateParameters.setLod(d);
                screenPixelsForMeter = screenPixelsForMeterLod1 / 8.0;
            } else {
                decimateParameters.setBasicValues(25.0, 0.2, 0.9, 32.0, 1000000, 2, 1.0);
                decimateParameters.setLod(d);
                screenPixelsForMeter = screenPixelsForMeterLod1 / 16.0;
            }

            // decimate and cut scenes
            currDepth = projectMaxDepthIdx - lod;
            decimateAndCutScenes(tileInfosCopy, lod, root, projectMaxDepthIdx, decimateParameters, screenPixelsForMeter);
            distributeContentsToNodesOctTree(root, tileInfosCopy, currDepth, nodeTileInfoMap);
            makeContentsForNodes(nodeTileInfoMap, lod);

            if (d >= 3) {
                break;
            }

        }

        // net surfaces with boxTextures
        for (int d = 4; d < projectMaxDepthIdx; d++) {
            lod = d;
            currDepth = projectMaxDepthIdx - lod;
            double boxSizeForCurrDepth = desiredDistanceBetweenLat / Math.pow(2, (currDepth + 1));
            double pixelsForMeter = 180.0 / boxSizeForCurrDepth;
            tileInfosCopy.clear();
            nodeTileInfoMap.clear();
            tileInfosCopy = this.getTileInfosCopy(tileInfos, lod, tileInfosCopy);
            double screenPixelsForMeterLod1 = 22.0;
            double screenPixelsForMeter = 0.0;
            // public void setBasicValues(double maxDiffAngDegrees, double hedgeMinLength, double frontierMaxDiffAngDeg, double maxAspectRatio, int maxCollapsesCount)
            decimateParameters.setBasicValues(10.0, 0.5, 1.0, 6.0, 1000000, 1, 1.8);
            decimateParameters.setLod(3);
            if (d == 3) {
                decimateParameters.setBasicValues(16.0, 1.0, 1.0, 15.0, 1000000, 1, 1.8);
                screenPixelsForMeter = screenPixelsForMeterLod1 / 2.0;
            } else if (d == 4) {
                decimateParameters.setBasicValues(21.0, 1.2, 1.0, 15.0, 1000000, 1, 1.8);
                screenPixelsForMeter = screenPixelsForMeterLod1 / 3.0;
            } else if (d == 5) {
                decimateParameters.setBasicValues(26.0, 1.5, 1.0, 15.0, 1000000, 1, 1.8);
                screenPixelsForMeter = screenPixelsForMeterLod1 / 8.0;
            } else if (d == 6) {
                decimateParameters.setBasicValues(30.0, 2.0, 1.0, 15.0, 1000000, 1, 1.8);
                screenPixelsForMeter = screenPixelsForMeterLod1 / 16.0;
            }

            // make netSurfaces and decimate and cut scenes
            currDepth = projectMaxDepthIdx - lod;
            decimateNetSurfacesAndCutScenes(tileInfosCopy, lod, root, projectMaxDepthIdx, decimateParameters, pixelsForMeter, screenPixelsForMeter);
            distributeContentsToNodesOctTree(root, tileInfosCopy, currDepth, nodeTileInfoMap);
            makeContentsForNodes(nodeTileInfoMap, lod);

            if (d >= 5) {
                break;
            }
        }

        // Check if is necessary netSurfaces nodes
        lod = 5;
        for (int depth = projectMaxDepthIdx - lod; depth >= 0; depth--) {
            tileInfosCopy.clear();
            tileInfosCopy = this.getTileInfosCopy(tileInfos, 0, tileInfosCopy);
            createNetSurfaceNodes(root, tileInfosCopy, depth, projectMaxDepthIdx);
        }


        // now, delete nodes that have no contents
        root.deleteNoContentNodes();
        setGeometryErrorToNodeManual(root, projectMaxDepthIdx);

        root.setGeometricError(1000.0);
        Asset asset = createAsset();
        Tileset tileset = new Tileset();
        tileset.setAsset(asset);
        tileset.setRoot(root);
        tileset.setGeometricError(1000.0);
        return tileset;
    }


    private void cuttingAndScissorProcessST(List<TileInfo> tileInfos, int lod, Node rootNode, List<TileInfo> resultTileInfos, int maxDepth) {
        // Single-threading
        List<TileInfo> finalTileInfosCopy = new ArrayList<>();

        // Usamos un ConcurrentHashMap para almacenar los resultados por índice seguro
        Map<Integer, List<TileInfo>> tileInfoListMap = new ConcurrentHashMap<>();

        log.info("Cutting and Scissor process is started. Total tileInfos : {}", tileInfos.size());



        int counter = 0;
        for (TileInfo tileInfo : tileInfos) {
            log.info("CutRectangleCake : " + counter + " / " + tileInfos.size());
            BoundingVolume rootNodeBoundingVolume = rootNode.getBoundingVolume();
            BoundingVolume rootNodeBoundingVolumeCopy = new BoundingVolume(rootNodeBoundingVolume);


            List<TileInfo> singleTileInfoList = new ArrayList<>();
            singleTileInfoList.add(tileInfo);
            String tileInfoName = tileInfo.getTempPath().getFileName().toString();
            log.info("[Tile][PhotoRealistic][{}/{}] - Cut RectangleCake one shoot... : {}", tileInfos.size(), tileInfoName);

            List<TileInfo> resultTileInfoList = new ArrayList<>();
            try{
                cutRectangleCakeOneShoot(singleTileInfoList, lod, rootNodeBoundingVolumeCopy, maxDepth, resultTileInfoList);
            }catch (Exception e){
                log.error("[ERROR] :", e);
                throw new RuntimeException(e);
            }


            resultTileInfos.addAll(resultTileInfoList);

            counter++;
        }



    }

    private void cuttingAndScissorProcessMT(List<TileInfo> tileInfos, int lod, Node rootNode, List<TileInfo> resultTileInfos, int maxDepth) {
        // multi-threading
        ExecutorService executorService = Executors.newFixedThreadPool(globalOptions.getMultiThreadCount());
        List<Runnable> tasks = new ArrayList<>();
        List<TileInfo> finalTileInfosCopy = new ArrayList<>();
        List<List<TileInfo>> tileInfoListList = new ArrayList<>();

        // make an asynchronous arrayList
        Map<Integer, List<TileInfo>> tileInfoListMap = new ConcurrentHashMap<>();


        log.info("Cutting and Scissor process is started. Total tileInfos : {}", tileInfos.size());

        int tileInfosCount = tileInfos.size();
        AtomicInteger atomicProcessCount = new AtomicInteger(0);
        int counter = 0;
        for (TileInfo tileInfo : tileInfos) {
            List<TileInfo> resultTileInfoList = tileInfoListMap.computeIfAbsent(counter, k -> new ArrayList<>());
            BoundingVolume rootNodeBoundingVolume = rootNode.getBoundingVolume();
            BoundingVolume rootNodeBoundingVolumeCopy = new BoundingVolume(rootNodeBoundingVolume);
            counter++;

            Runnable callableTask = () -> {
                try {
                    List<TileInfo> singleTileInfoList = new ArrayList<>();
                    singleTileInfoList.add(tileInfo);
                    String tileInfoName = tileInfo.getTempPath().getFileName().toString();

                    int processCount = atomicProcessCount.incrementAndGet();
                    log.info("[Tile][PhotoRealistic][{}/{}] Generating tile : {}", processCount, tileInfosCount, tileInfoName);
                    log.info("[Tile][PhotoRealistic][{}/{}] - Cut RectangleCake one shoot... : {}", processCount, tileInfosCount, tileInfoName);
                    cutRectangleCakeOneShoot(singleTileInfoList, lod, rootNodeBoundingVolumeCopy, maxDepth, resultTileInfoList);

                    //tileInfoListList.add(cuttedTileInfoList);
                } catch (IOException e) {
                    log.error("[ERROR] ", e);
                    throw new RuntimeException(e);
                }
            };
            tasks.add(callableTask);
        }

        try {
            executeThread(executorService, tasks);
        } catch (InterruptedException e) {
            log.error("[ERROR] ", e);
            throw new RuntimeException(e);
        }

        // use map.
        tileInfoListMap.forEach((k, v) -> {
            finalTileInfosCopy.addAll(v);
        });

    }

    private void setGeometryErrorToNodeManual(Node node, int maxDepth) {
        int lod = maxDepth - node.getDepth();

        double geometricError;
        if (lod == 0) {
            geometricError = 0.01;
        } else if (lod == 1) {
            geometricError = 1.0;
        } else if (lod == 2) {
            geometricError = 2.0;
        } else if (lod == 3) {
            geometricError = 4.0;
        } else if (lod == 4) {
            geometricError = 8.0;
        } else if (lod == 5) {
            geometricError = 16.0;
        } else if (lod == 6) {
            geometricError = 32.0;
        } else if (lod == 7) {
            geometricError = 64.0;
        } else if (lod > 7) {
            geometricError = 128.0;
        } else {
            // less than 0
            geometricError = 0.01;
        }

        //double geometricError = (lod * factor + 0.01);
        node.setGeometricError(geometricError);
        List<Node> children = node.getChildren();
        if (children != null) {
            for (Node child : children) {
                setGeometryErrorToNodeManual(child, maxDepth);
            }
        }
    }

    // automatic calculate geometric error
    private void setGeometryErrorToNodeAutomatic(Node node, int maxDepth) {
        int lod = maxDepth - node.getDepth();
        double factor = 1.0;
        double geometricError = (lod * factor + 0.01);
        node.setGeometricError(geometricError);
        List<Node> children = node.getChildren();
        if (children != null) {
            int childrenCount = children.size();
            for (Node child : children) {
                setGeometryErrorToNodeAutomatic(child, maxDepth);
            }
        }
    }

    private void makeNetSurfacesWithBoxTextures(List<TileInfo> tileInfos, int lod, DecimateParameters decimateParameters, double pixelsForMeter) {
        log.info("making netSurfaces scenes for lod : " + lod);
        TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();
        List<GaiaScene> gaiaSceneList = new ArrayList<>();
        List<HalfEdgeScene> resultDecimatedScenes = new ArrayList<>();

        int tileInfosCount = tileInfos.size();
        for (int i = 0; i < tileInfosCount; i++) {
            log.info("making netSurfaces scene : " + i + " of " + tileInfosCount);
            TileInfo tileInfo = tileInfos.get(i);
            Path tempPath = tileInfo.getTempPath();
            Path tempFolder = tempPath.getParent();

            // load the file
            GaiaSet gaiaSet;
            try {
                gaiaSet = GaiaSet.readFile(tempPath);
                if (gaiaSet == null) {
                    log.error("[ERROR] gaiaSet is null. pth : " + tempPath);
                    continue;
                }
            } catch (IOException e) {
                log.error("[ERROR] :", e);
                throw new RuntimeException(e);
            }
            GaiaScene scene = new GaiaScene(gaiaSet);
            scene.setOriginalPath(tileInfo.getTempPath());
            //scene.setOriginalPath(tileInfo.getScenePath());
            scene.makeTriangleFaces();

            gaiaSceneList.clear();
            resultDecimatedScenes.clear();
            gaiaSceneList.add(scene);

            if (!GaiaSceneUtils.checkSceneMaterials(scene)) {
                log.error("Error : scene has objects that uses materials that are not in the scene.");
                continue;
            }

            double screenPixelsForMeter = 20.0;
            tilerExtensionModule.makeNetSurfacesWithBoxTexturesObliqueCamera(gaiaSceneList, resultDecimatedScenes, decimateParameters, pixelsForMeter, screenPixelsForMeter);

            if(resultDecimatedScenes.isEmpty()) {
                // sometimes the resultDecimatedScenes is empty because when rendering the scene, the scene is almost out of the camera
                log.error("Error : resultDecimatedScenes is empty.");
                gaiaSet.clear(); // delete gaiaSet
                scene.clear(); // delete scene
                continue;
            }

            HalfEdgeScene halfEdgeSceneLod = resultDecimatedScenes.get(0);

            // Save the textures in a temp folder
            List<GaiaMaterial> materials = halfEdgeSceneLod.getMaterials();
            for (GaiaMaterial material : materials) {
                List<GaiaTexture> textures = material.getTextures().get(TextureType.DIFFUSE);
                for (GaiaTexture texture : textures) {
                    // change the texture name
                    String texturePath = texture.getPath();
                    String rawTexturePath = texturePath.substring(0, texturePath.lastIndexOf("."));
                    String extension = texturePath.substring(texturePath.lastIndexOf("."));
                    String newTexturePath = rawTexturePath + "_" + lod + "." + extension;
                    texture.setPath(newTexturePath);
                    texture.setParentPath(tempFolder.toString());
                    texture.saveImage(texture.getFullPath());
                }
            }

            GaiaScene sceneLod1 = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeSceneLod);

            GaiaSet tempSetLod1 = GaiaSet.fromGaiaScene(sceneLod1);
            halfEdgeSceneLod.deleteObjects();

            LevelOfDetail levelOfDetail = LevelOfDetail.getByLevel(lod);
            float scale = levelOfDetail.getTextureScale();

            String aux = "lod" + lod;
            Path tempFolderLod = tempFolder.resolve(aux);
            Path currTempPathLod = tempSetLod1.writeFile(tempFolderLod, tileInfo.getSerial(), tempSetLod1.getAttribute()/*, scale*/);
            tileInfo.setTempPath(currTempPathLod);
            gaiaSet.clear(); // delete gaiaSet
            scene.clear(); // delete scene
            tempSetLod1.clear(); // delete tempSetLod1
            sceneLod1.clear(); // delete sceneLod1
        }
    }



    public void decimateNetSurfacesAndCutScenes(List<TileInfo> tileInfos, int lod, Node rootNode, int maxDepth, DecimateParameters decimateParameters,
                                                double pixelsForMeter, double screenPixelsForMeter)
    {
        log.info("Decimating netSurfaces and cutting scenes for lod : " + lod);
        TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();
        List<GaiaScene> gaiaSceneList = new ArrayList<>();
        List<HalfEdgeScene> resultDecimatedScenes = new ArrayList<>();

        List<TileInfo> newTileInfos = new ArrayList<>();

        int tileInfosCount = tileInfos.size();
        for (int i = 0; i < tileInfosCount; i++) {
            log.info("Decimating netSurfaces and cutting scene : " + i + " of " + tileInfosCount + " for lod : " + lod);
            TileInfo tileInfo = tileInfos.get(i);
            Path tempPath = tileInfo.getTempPath();
            Path tempFolder = tempPath.getParent();

            // load the file
            GaiaSet gaiaSet;
            try {
                gaiaSet = GaiaSet.readFile(tempPath);
                if (gaiaSet == null) {
                    log.error("Error : gaiaSet is null. pth : " + tempPath);
                    continue;
                }
            } catch (IOException e) {
                log.error("[ERROR] :", e);
                throw new RuntimeException(e);
            }
            GaiaScene scene = new GaiaScene(gaiaSet);
            GaiaBoundingBox gaiaSceneBBox = scene.getBoundingBox();
            scene.setOriginalPath(tileInfo.getTempPath());
            //scene.setOriginalPath(tileInfo.getScenePath());
            scene.makeTriangleFaces();

            gaiaSceneList.clear();
            resultDecimatedScenes.clear();
            gaiaSceneList.add(scene);

            List<GaiaAAPlane> cuttingPlanes = new ArrayList<>();
            Matrix4d transformMatrix = new Matrix4d();
            HalfEdgeOctree halfEdgeOctree = null;
            try{
                BoundingVolume rootNodeBoundingVolume = rootNode.getBoundingVolume();
                halfEdgeOctree = this.getCuttingPlanesAndHalfEdgeOctree(tileInfo, lod, rootNodeBoundingVolume, maxDepth, cuttingPlanes, transformMatrix);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            GaiaBoundingBox motherBBoxLC = new GaiaBoundingBox();
            GaiaBoundingBox motherCartographicBoundingBox = this.calculateCartographicBoundingBox(scene, transformMatrix, motherBBoxLC);

            boolean makeSkirt = GlobalOptions.MAKE_SKIRT;
            tilerExtensionModule.decimateNetSurfaceAndCutByObliqueCamera(gaiaSceneList, resultDecimatedScenes, decimateParameters, halfEdgeOctree,
                    cuttingPlanes, pixelsForMeter, screenPixelsForMeter, makeSkirt);

            if (resultDecimatedScenes.isEmpty()) {
                log.error("Error : resultDecimatedScenes is empty." + tempPath);
                continue;
            }

            // create tileInfos for the cut scenes
            String outputPathString = globalOptions.getOutputPath();
            String cutTempPathString = outputPathString + File.separator + "temp" + File.separator + "cutTemp";
            Path cutTempPath = Paths.get(cutTempPathString);
            // create directory if not exists
            if (!cutTempPath.toFile().exists() && cutTempPath.toFile().mkdirs()) {
                log.debug("cutTemp folder created.");
            }

            Path cutTempLodPath = cutTempPath.resolve("lod" + lod);
            if (!cutTempLodPath.toFile().exists() && cutTempLodPath.toFile().mkdirs()) {
                log.debug("cutTempLod folder created.");
            }

            int resultDecimatedScenesCount = resultDecimatedScenes.size();
            for(int j = 0; j < resultDecimatedScenesCount; j++) {
                HalfEdgeScene halfEdgeSceneLod = resultDecimatedScenes.get(j);
//                GaiaBoundingBox halfEdgeSceneBBox = halfEdgeSceneLod.getBoundingBox();
//                if(!gaiaSceneBBox.isBoxInside(halfEdgeSceneBBox)) {
//                    log.error("Error : gaiaSceneBBox does not intersect with halfEdgeSceneBBox.");
//                }

                GaiaBoundingBox boundingBoxCutLC = new GaiaBoundingBox();
                GaiaScene gaiaSceneCut = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeSceneLod);
                GaiaBoundingBox cartographicBoundingBox = this.calculateCartographicBoundingBox(gaiaSceneCut, transformMatrix, boundingBoxCutLC);

                if(motherCartographicBoundingBox.getMaxZ() < cartographicBoundingBox.getMaxZ()) {
                    log.error("Error : motherCartographicBoundingBox does not intersect with cartographicBoundingBox.");
                }

                GaiaSet gaiaSetCut = GaiaSet.fromGaiaScene(gaiaSceneCut);


                LevelOfDetail levelOfDetail = LevelOfDetail.getByLevel(lod);
                float scale = levelOfDetail.getTextureScale();

                Path cutScenePath = Paths.get("");
                gaiaSceneCut.setOriginalPath(cutScenePath);

                UUID identifier = UUID.randomUUID();
                Path gaiaSetCutFolderPath = cutTempLodPath.resolve(identifier.toString());
                if (!gaiaSetCutFolderPath.toFile().exists() && gaiaSetCutFolderPath.toFile().mkdirs()) {
                    log.debug("gaiaSetCut folder created.");
                }
                boolean copyTexturesToNewPath = false;
                Path tempPathLod = gaiaSetCut.writeFileForPR(gaiaSetCutFolderPath, copyTexturesToNewPath);

                // save material atlas textures
                Path parentPath = gaiaSetCutFolderPath;
                Path imagesPath = parentPath.resolve("images");
                // make directories if not exists
                File imagesFolder = imagesPath.toFile();
                if (!imagesFolder.exists() && imagesFolder.mkdirs()) {
                    log.debug("images folder created.");
                }
                List<GaiaMaterial> materials = halfEdgeSceneLod.getMaterials();
                for (GaiaMaterial material : materials) {
                    List<GaiaTexture> textures = material.getTextures().get(TextureType.DIFFUSE);
                    for (GaiaTexture texture : textures) {
                        // check if exist bufferedImage of the texture

                        if(texture.getBufferedImage() == null) {
                            int hola = 0;
                        }

                        texture.setParentPath(imagesPath.toString());
                        texture.saveImage(texture.getFullPath());
                    }
                }

                // now can delete the halfEdgeScene
                halfEdgeSceneLod.deleteObjects();

                // delete the contents of the gaiaSceneCut*********************************************
                gaiaSceneCut.getNodes().forEach(GaiaNode::clear);
                // end delete the contents of the gaiaSceneCut.--------------------------------------------

                TileInfo newTileInfo = TileInfo.builder().scene(gaiaSceneCut).outputPath(tempPathLod).build();
                newTileInfo.setTransformMatrix(new Matrix4d(transformMatrix));
                newTileInfo.setBoundingBox(boundingBoxCutLC);
                newTileInfo.setCartographicBBox(cartographicBoundingBox);

                KmlInfo kmlInfoCut = KmlInfo.builder().position(tileInfo.getKmlInfo().getPosition()).build();
                newTileInfo.setKmlInfo(kmlInfoCut);

                newTileInfo.setTempPath(tempPathLod);
                newTileInfos.add(newTileInfo);
            }
        }

        tileInfos.clear();
        tileInfos.addAll(newTileInfos);
    }

    public void decimateAndCutScenes(List<TileInfo> tileInfos, int lod, Node rootNode, int maxDepth, DecimateParameters decimateParameters, double screenPixelsForMeter) {
        log.info("Decimating and cutting scenes for lod : " + lod);
        TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();
        List<GaiaScene> gaiaSceneList = new ArrayList<>();
        List<HalfEdgeScene> resultDecimatedScenes = new ArrayList<>();

        List<TileInfo> newTileInfos = new ArrayList<>();

        int tileInfosCount = tileInfos.size();
        for (int i = 0; i < tileInfosCount; i++) {
            log.info("Decimating and cutting scene : " + (i+1) + " of " + tileInfosCount + " for lod : " + lod);
            TileInfo tileInfo = tileInfos.get(i);
            Path tempPath = tileInfo.getTempPath();
            Path tempFolder = tempPath.getParent();

            // load the file
            GaiaSet gaiaSet;
            try {
                gaiaSet = GaiaSet.readFile(tempPath);
                if (gaiaSet == null) {
                    log.error("Error : gaiaSet is null. pth : " + tempPath);
                    continue;
                }
            } catch (IOException e) {
                log.error("[ERROR] :", e);
                throw new RuntimeException(e);
            }
            GaiaScene scene = new GaiaScene(gaiaSet);
            scene.setOriginalPath(tileInfo.getTempPath());
            scene.makeTriangleFaces();

            gaiaSceneList.clear();
            resultDecimatedScenes.clear();
            gaiaSceneList.add(scene);

            List<GaiaAAPlane> cuttingPlanes = new ArrayList<>();
            Matrix4d transformMatrix = new Matrix4d();
            HalfEdgeOctree halfEdgeOctree = null;
            try{
                BoundingVolume rootNodeBoundingVolume = rootNode.getBoundingVolume();
                BoundingVolume rootNodeBoundingVolumeCopy = new BoundingVolume(rootNodeBoundingVolume);
                halfEdgeOctree = this.getCuttingPlanesAndHalfEdgeOctree(tileInfo, lod, rootNodeBoundingVolumeCopy, maxDepth, cuttingPlanes, transformMatrix);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            boolean makeSkirt = GlobalOptions.MAKE_SKIRT;
            tilerExtensionModule.decimateAndCutByObliqueCamera(gaiaSceneList, resultDecimatedScenes, decimateParameters, halfEdgeOctree, cuttingPlanes, screenPixelsForMeter, makeSkirt);

            if (resultDecimatedScenes.isEmpty()) {
                log.error("Error : resultDecimatedScenes is empty." + tempPath);
                continue;
            }

            // create tileInfos for the cut scenes
            String outputPathString = globalOptions.getOutputPath();
            String cutTempPathString = outputPathString + File.separator + "temp" + File.separator + "cutTemp";
            Path cutTempPath = Paths.get(cutTempPathString);
            // create directory if not exists
            if (!cutTempPath.toFile().exists() && cutTempPath.toFile().mkdirs()) {
                log.debug("cutTemp folder created.");
            }

            Path cutTempLodPath = cutTempPath.resolve("lod" + lod);
            if (!cutTempLodPath.toFile().exists() && cutTempLodPath.toFile().mkdirs()) {
                log.debug("cutTempLod folder created.");
            }

            int resultDecimatedScenesCount = resultDecimatedScenes.size();
            for(int j = 0; j < resultDecimatedScenesCount; j++) {
                HalfEdgeScene halfEdgeSceneLod = resultDecimatedScenes.get(j);

                int halfEdgeFacesCount = halfEdgeSceneLod.getFacesCount();

                GaiaBoundingBox boundingBoxCutLC = new GaiaBoundingBox();
                GaiaScene gaiaSceneCut = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeSceneLod);

                int gaiaSceneFacesCount = gaiaSceneCut.getFacesCount();

                if(halfEdgeFacesCount != gaiaSceneFacesCount) {
                    log.error("Error : halfEdgeFacesCount is different from gaiaSceneFacesCount.");
                }

                GaiaBoundingBox cartographicBoundingBox = this.calculateCartographicBoundingBox(gaiaSceneCut, transformMatrix, boundingBoxCutLC);
                GaiaSet gaiaSetCut = GaiaSet.fromGaiaScene(gaiaSceneCut);

                Path cutScenePath = Paths.get("");
                gaiaSceneCut.setOriginalPath(cutScenePath);

                UUID identifier = UUID.randomUUID();
                Path gaiaSetCutFolderPath = cutTempLodPath.resolve(identifier.toString());
                if (!gaiaSetCutFolderPath.toFile().exists() && gaiaSetCutFolderPath.toFile().mkdirs()) {
                    log.debug("gaiaSetCut folder created.");
                }
                boolean copyTexturesToNewPath = false;
                Path tempPathLod = gaiaSetCut.writeFileForPR(gaiaSetCutFolderPath, copyTexturesToNewPath);

                // save material atlas textures
                Path parentPath = gaiaSetCutFolderPath;
                Path imagesPath = parentPath.resolve("images");
                // make directories if not exists
                File imagesFolder = imagesPath.toFile();
                if (!imagesFolder.exists() && imagesFolder.mkdirs()) {
                    log.debug("images folder created.");
                }
                List<GaiaMaterial> materials = halfEdgeSceneLod.getMaterials();
                for (GaiaMaterial material : materials) {
                    List<GaiaTexture> textures = material.getTextures().get(TextureType.DIFFUSE);
                    for (GaiaTexture texture : textures) {
                        // check if exist bufferedImage of the texture

                        if(texture.getBufferedImage() == null) {
                            int hola = 0;
                        }

                        texture.setParentPath(imagesPath.toString());
                        texture.saveImage(texture.getFullPath());
                    }
                }

                // now can delete the halfEdgeScene
                halfEdgeSceneLod.deleteObjects();

                // delete the contents of the gaiaSceneCut*********************************************
                gaiaSceneCut.getNodes().forEach(GaiaNode::clear);
                // end delete the contents of the gaiaSceneCut.--------------------------------------------

                TileInfo newTileInfo = TileInfo.builder().scene(gaiaSceneCut).outputPath(tempPathLod).build();
                newTileInfo.setTransformMatrix(new Matrix4d(transformMatrix));
                newTileInfo.setBoundingBox(boundingBoxCutLC);
                newTileInfo.setCartographicBBox(cartographicBoundingBox);

                KmlInfo kmlInfoCut = KmlInfo.builder().position(tileInfo.getKmlInfo().getPosition()).build();
                newTileInfo.setKmlInfo(kmlInfoCut);

                newTileInfo.setTempPath(tempPathLod);
                newTileInfos.add(newTileInfo);
            }
        }

        tileInfos.clear();
        tileInfos.addAll(newTileInfos);
    }

    private double getNodeLatitudesLengthInMeters(Node node) {
        // make globalBoundingBox as square
        double[] region = node.getBoundingVolume().getRegion();
        double minLatRad = region[1];
        double maxLatRad = region[3];
        return GlobeUtils.distanceBetweenLatitudesRad(minLatRad, maxLatRad);
    }

    private void createNetSurfaceNodes(Node rootNode, List<TileInfo> tileInfos, int nodeDepth, int maxDepth) {
        // 1rst, find all tileInfos that intersects with the node
        log.info("Creating netSurface nodes for nodeDepth : " + nodeDepth + " of maxDepth : " + maxDepth);
        List<Node> nodes = new ArrayList<>();
        rootNode.getNodesByDepth(nodeDepth, nodes);
        List<TileInfo> tileInfosOfNode = new ArrayList<>();
        TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();

        int nodesCount = nodes.size();
        for (int i = 0; i < nodesCount; i++) {
            tileInfosOfNode.clear();
            Node node = nodes.get(i);
            int tileInfosCount = tileInfos.size();
            for (TileInfo tileInfo : tileInfos) {
                if (intersectsNodeWithTileInfo(node, tileInfo)) {
                    tileInfosOfNode.add(tileInfo);
                }
            }

            int tileInfosOfNodeCount = tileInfosOfNode.size();
            if (tileInfosOfNodeCount == 0) continue;

            node.setRefine(Node.RefineType.REPLACE);

            // create sceneInfos
            List<SceneInfo> sceneInfos = new ArrayList<>();
            for (int j = 0; j < tileInfosOfNodeCount; j++) {
                TileInfo tileInfo = tileInfosOfNode.get(j);
                SceneInfo sceneInfo = new SceneInfo();
                sceneInfo.setScenePath(tileInfo.getTempPath().toString());
                KmlInfo kmlInfo = tileInfo.getKmlInfo();
                Vector3d geoCoordPosition = kmlInfo.getPosition();
                Vector3d posWC = GlobeUtils.geographicToCartesianWgs84(geoCoordPosition);
                Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(posWC);
                sceneInfo.setTransformMatrix(transformMatrix);
                sceneInfos.add(sceneInfo);
            }

            if(sceneInfos.isEmpty()) {
                log.error("[ERROR] Error : sceneInfos is empty.");
                continue;
            }

            // render the sceneInfos and obtain the color and depth images*********************************************************
            List<BufferedImage> resultImages = new ArrayList<>();
            int bufferedImageType = BufferedImage.TYPE_INT_RGB;

            Vector3d nodeCenterGeoCoordRad = node.getBoundingVolume().calcCenter();
            Vector3d nodeCenterGeoCoordDeg = new Vector3d(Math.toDegrees(nodeCenterGeoCoordRad.x), Math.toDegrees(nodeCenterGeoCoordRad.y), nodeCenterGeoCoordRad.z);
            Vector3d nodePosWC = GlobeUtils.geographicToCartesianWgs84(nodeCenterGeoCoordDeg);
            Matrix4d nodeTMatrix = node.getTransformMatrix();
            if (nodeTMatrix == null) {
                nodeTMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(nodePosWC);
            }
            GaiaBoundingBox nodeBBoxLC = node.calculateLocalBoundingBox();
            GaiaBoundingBox nodeCartographicBBox = node.calculateCartographicBoundingBox();

            log.info("nodeCode : " + node.getNodeCode() + "currNodeIdx : " + i + "of : " + nodesCount);
            int maxScreenSize = 1024;
            int maxDepthScreenSize = 180;
            tilerExtensionModule.getColorAndDepthRender(sceneInfos, bufferedImageType, resultImages, nodeBBoxLC, nodeTMatrix, maxScreenSize, maxDepthScreenSize);

            if(resultImages.size() < 2) {
                log.error("[ERROR] resultImages size is less than 2.");
                continue;
            }
            BufferedImage bufferedImageColor = resultImages.get(0);
            BufferedImage bufferedImageDepth = resultImages.get(1);

            // now, make a halfEdgeScene from the bufferedImages******************************************************************
            String outputPathString = globalOptions.getOutputPath();
            String netTempPathString = outputPathString + File.separator + "temp" + File.separator +  "netTemp";
            Path netTempPath = Paths.get(netTempPathString);
            // create dirs if not exists
            File netTempFile = netTempPath.toFile();
            if (!netTempFile.exists() && netTempFile.mkdirs()) {
                log.debug("info : netTemp folder created.");
            }

            String netSetFolderPathString = netTempPathString + File.separator + "netSet_nodeDepth_" + nodeDepth + "_" + i;
            Path netSetFolderPath = Paths.get(netSetFolderPathString);
            // create dirs if not exists
            File netSetFile = netSetFolderPath.toFile();
            if (!netSetFile.exists() && netTempFile.mkdirs()) {
                log.debug("info : netSet folder created.");
            }
            String netSetImagesFolderPathString = netSetFolderPathString + File.separator + "images";
            Path netSetImagesFolderPath = Paths.get(netSetImagesFolderPathString);
            // create dirs if not exists
            File netSetImagesFolder = netSetImagesFolderPath.toFile();
            if (!netSetImagesFolder.exists() && netSetImagesFolder.mkdirs()) {
                log.debug("info : netSetImages folder created.");
            }

            // save the bufferedImageColor into the netSetImagesFolder
            String imageExtension = "png";
            String imagePath = "netScene_" + nodeDepth + "_" + i + "_color" + "." + imageExtension;
            try {
                File file = new File(netSetImagesFolderPathString + File.separator + imagePath);
                log.info("[Write Image] : {}", file.getAbsoluteFile());
                ImageIO.write(bufferedImageColor, "png", file);
            } catch (Exception e) {
                log.error("error: ", e);
            }

            float[][] depthValues = bufferedImageToFloatMatrix(bufferedImageDepth);
            int numCols = bufferedImageDepth.getWidth();
            int numRows = bufferedImageDepth.getHeight();
            HalfEdgeScene halfEdgeScene = HalfEdgeUtils.getHalfEdgeSceneRectangularNet(numCols, numRows, depthValues, nodeBBoxLC);
            if (halfEdgeScene == null) {
                log.info("info : halfEdgeScene is null.");
                continue;
            }
            double maxDiffAngDeg = 35.0;
            //double hedgeMinLength = getNodeLatitudesLengthInMeters(node)/1000.0;
            double hedgeMinLength = 0.5;
            double frontierMaxDiffAngDeg = 30.0;
            double maxAspectRatio = 6.0;
            DecimateParameters decimateParameters = new DecimateParameters();
            decimateParameters.setBasicValues(maxDiffAngDeg, hedgeMinLength, frontierMaxDiffAngDeg, maxAspectRatio, 1000000, 2, 1.8);
            halfEdgeScene.doTrianglesReduction(decimateParameters);

            if (halfEdgeScene.getTrianglesCount() == 0) continue;

            // now, create material for the halfEdgeScene
            List<GaiaMaterial> materials = new ArrayList<>();

            GaiaMaterial material = new GaiaMaterial();
            List<GaiaTexture> textures = new ArrayList<>();
            GaiaTexture gaiaTexture = new GaiaTexture();
            gaiaTexture.setPath(imagePath);
            gaiaTexture.setParentPath(netSetImagesFolderPathString);
            textures.add(gaiaTexture);
            material.getTextures().put(TextureType.DIFFUSE, textures);
            material.setId(0);
            materials.add(material);
            halfEdgeScene.setMaterials(materials);

            // now set materialId to the halfEdgeScene
            int materialId = 0;
            halfEdgeScene.setMaterialId(materialId);

            GaiaScene gaiaScene = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);
            GaiaSet gaiaSet = GaiaSet.fromGaiaScene(gaiaScene);
            Path netSetPath = Paths.get(netSetFolderPathString + File.separator + "netSet_nodeDepth_" + nodeDepth + "_" + i + ".tmp");
            gaiaSet.writeFileInThePath(netSetPath);

            List<GaiaNode> gaiaNodes = gaiaScene.getNodes();
            for (GaiaNode gaiaNode : gaiaNodes) {
                gaiaNode.clear();
            }

            //calculate lod
            int lod = maxDepth - nodeDepth;
            double netSurfaceGeometricError = (lod + 1);
            node.setGeometricError(netSurfaceGeometricError);

            // make contents for the node
            List<TileInfo> netTileInfos = new ArrayList<>();
            TileInfo tileInfoNet = TileInfo.builder().scene(gaiaScene).outputPath(netSetFolderPath).build();
            tileInfoNet.setTempPath(netSetPath);
            Matrix4d transformMatrixNet = new Matrix4d(nodeTMatrix);
            tileInfoNet.setTransformMatrix(transformMatrixNet);
            tileInfoNet.setBoundingBox(nodeBBoxLC);
            tileInfoNet.setCartographicBBox(null);

            // make a kmlInfo for the cut scene
            KmlInfo kmlInfoCut = KmlInfo.builder().position(nodeCenterGeoCoordDeg).build();
            tileInfoNet.setKmlInfo(kmlInfoCut);
            netTileInfos.add(tileInfoNet);

            ContentInfo contentInfo = new ContentInfo();
            String nodeCode = node.getNodeCode();
            contentInfo.setName(nodeCode);
            LevelOfDetail lodLevel = LevelOfDetail.getByLevel(3);
            int lodError = lodLevel.getGeometricError();
            contentInfo.setLod(lodLevel);
            contentInfo.setBoundingBox(nodeCartographicBBox); // must be cartographicBBox
            contentInfo.setNodeCode(node.getNodeCode());
            contentInfo.setTileInfos(netTileInfos);
            contentInfo.setRemainTileInfos(null);
            contentInfo.setTransformMatrix(nodeTMatrix);

            Content content = new Content();
            content.setUri("data/" + nodeCode + ".b3dm");
            content.setContentInfo(contentInfo);
            if(node.getContent() != null) {
                log.info("Error : node.getContent() is not null. NetSurfaces lod 5 or more******************************");
            }
            node.setContent(content);

            // delete scenes
            halfEdgeScene.deleteObjects();
            gaiaScene.clear();
            gaiaSet.clear();

            // test save resultImages
            String sceneName = "mosaicRenderTest_" + i + "_color";
            String sceneRawName = sceneName;
            imageExtension = "png";
            String outputFolderPath = globalOptions.getOutputPath();
            try {
                File outputFile = new File(outputFolderPath, sceneRawName + "." + imageExtension);
                log.info("[Write Image] : {}", outputFile.getAbsoluteFile());
                ImageIO.write(bufferedImageColor, "png", outputFile);
            } catch (Exception e) {
                log.error("[ERROR] :", e);
            }

            try {
                sceneName = "mosaicRenderTest_" + i + "_depth";
                imageExtension = "png";
                File outputFile = new File(outputFolderPath, sceneName + "." + imageExtension);
                log.info("[Write Image] : {}", outputFile.getAbsoluteFile());
                ImageIO.write(bufferedImageDepth, "png", outputFile);
            } catch (Exception e) {
                log.error("[ERROR] :", e);
            }
        }
    }

    public float unpackDepth32(float[] packedDepth) {
        if (packedDepth.length != 4) {
            throw new IllegalArgumentException("packedDepth debe tener exactamente 4 elementos.");
        }

        for (int i = 0; i < 4; i++) {
            packedDepth[i] -= 1.0f / 512.0f;
        }
        return packedDepth[0] + packedDepth[1] / 256.0f + packedDepth[2] / (256.0f * 256.0f) + packedDepth[3] / 16777216.0f;
    }

    private float[][] bufferedImageToFloatMatrix(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        float[][] floatMatrix = new float[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Color color = new Color(image.getRGB(i, j), true);
                float r = color.getRed() / 255.0f;
                float g = color.getGreen() / 255.0f;
                float b = color.getBlue() / 255.0f;
                float a = color.getAlpha() / 255.0f;

                float depth = unpackDepth32(new float[]{r, g, b, a});
                floatMatrix[i][j] = depth;
            }
        }

        return floatMatrix;
    }

    private boolean intersectsNodeWithTileInfo(Node node, TileInfo tileInfo) {
        BoundingVolume boundingVolume = node.getBoundingVolume();
        double[] region = boundingVolume.getRegion();  // [minLonDeg, minLatDeg, maxLonDeg, maxLatDeg]

        // compare longitudes
        double minLonDeg = Math.toDegrees(region[0]);
        double maxLonDeg = Math.toDegrees(region[2]);
        GaiaBoundingBox tileBoundingBox = tileInfo.getCartographicBBox();

        if (maxLonDeg < tileBoundingBox.getMinX() || minLonDeg > tileBoundingBox.getMaxX()) return false;

        // compare latitudes
        double minLatDeg = Math.toDegrees(region[1]);
        double maxLatDeg = Math.toDegrees(region[3]);

        if (maxLatDeg < tileBoundingBox.getMinY() || minLatDeg > tileBoundingBox.getMaxY()) return false;

        // compare altitudes
        double minAlt = region[4];
        double maxAlt = region[5];

        return !(maxAlt < tileBoundingBox.getMinZ()) && !(minAlt > tileBoundingBox.getMaxZ());
    }

    private void distributeContentsToNodesOctTree(Node rootNode, List<TileInfo> tileInfos, int nodeDepth, Map<Node, List<TileInfo>> nodeTileInfoMap) {
        // distribute contents to node in the correspondent depth
        // Here, the tileInfos are cutTileInfos by node's boundary planes, so we can use tileInfoCenterGeoCoordRad
        for (TileInfo tileInfo : tileInfos) {
            GaiaBoundingBox cartographicBBox = tileInfo.getCartographicBBox();
            if(cartographicBBox == null) {
                log.error("Error : cartographicBBox is null.");
                continue;
            }

            Vector3d geoCoordCenter = cartographicBBox.getCenter();
            double centerLonRad = Math.toRadians(geoCoordCenter.x);
            double centerLatRad = Math.toRadians(geoCoordCenter.y);
            double centerAlt = geoCoordCenter.z;
            Vector3d tileInfoCenterGeoCoordRad = new Vector3d(centerLonRad, centerLatRad, centerAlt);

            Node childNode = rootNode.getIntersectedNodeAsOctree(tileInfoCenterGeoCoordRad, nodeDepth);
            double minLatRad = childNode.getBoundingVolume().getRegion()[1];
            double maxLatRad = childNode.getBoundingVolume().getRegion()[3];
            double distanceBetweenLat = GlobeUtils.distanceBetweenLatitudesRad(minLatRad, maxLatRad);
            if (childNode == null) {
                log.error("Error : childNode is null.");
                continue;
            }

            nodeTileInfoMap.computeIfAbsent(childNode, k -> new ArrayList<>()).add(tileInfo);
            List<TileInfo> tileInfosInNode = nodeTileInfoMap.get(childNode);
            tileInfosInNode.add(tileInfo);
        }
    }

    private List<TileInfo> getTileInfosCopy(List<TileInfo> tileInfos, int lod, List<TileInfo> resultTileInfosCopy) {
        if (resultTileInfosCopy == null) {
            resultTileInfosCopy = new ArrayList<>();
        }

        for (TileInfo tileInfo : tileInfos) {
            TileInfo tileInfoCopy = tileInfo.clone();

            // change the tempPath of the tileInfos by tempPathLod
            List<Path> tempPathLod = tileInfoCopy.getTempPathLod();
            if (tempPathLod != null) {
                Path pathLod = tempPathLod.get(lod);
                if (pathLod != null) {
                    tileInfoCopy.setTempPath(pathLod);
                }
            }
            resultTileInfosCopy.add(tileInfoCopy);
        }

        return resultTileInfosCopy;
    }

    private void makeSkirt(List<TileInfo> tileInfos) {
        for (TileInfo tileInfo : tileInfos) {
            Path path = tileInfo.getTempPath();

            // load the file
            try {
                GaiaSet gaiaSet = GaiaSet.readFile(path);
                if (gaiaSet == null) {
                    log.error("Error : gaiaSet is null. pth : " + path);
                    continue;
                }
                GaiaScene scene = new GaiaScene(gaiaSet);

                HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);
                halfEdgeScene.makeSkirt();

                // once scene is scissored, must change the materials of the gaiaSet and overwrite the file
                GaiaScene skirtScene = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);
                GaiaSet gaiaSet2 = GaiaSet.fromGaiaScene(skirtScene);

                // overwrite the file
                gaiaSet2.writeFileInThePath(path);

                scene.clear();
                gaiaSet.clear();
                halfEdgeScene.deleteObjects();
                gaiaSet2.clear();
                skirtScene.clear();
            } catch (IOException e) {
                log.error("[ERROR] :", e);
                throw new RuntimeException(e);
            }
        }
    }

    private HalfEdgeOctree getCuttingPlanesAndHalfEdgeOctree(TileInfo tileInfo, int lod, BoundingVolume rootNodeBoundingVolume, int depthIdx,
                                                             List<GaiaAAPlane> resultPlanes, Matrix4d resultTransformMatrix) throws FileNotFoundException {
        //****************************************************
        // Note : tileInfos must contain only one tileInfo
        //****************************************************
        // calculate the divisions of the rectangle cake
        // int maxDepth = rootNode.findMaxDepth();
        int depthCount = depthIdx + 1;
        int currDepth = depthIdx - lod;

        // the maxDepth corresponds to lod0
        //List<Node> nodes = new ArrayList<>();
        //rootNode.getNodesByDepth(currDepth, nodes);
        BoundingVolume boundingVolume = rootNodeBoundingVolume;
        double minLonDeg = Math.toDegrees(boundingVolume.getRegion()[0]);
        double minLatDeg = Math.toDegrees(boundingVolume.getRegion()[1]);
        double maxLonDeg = Math.toDegrees(boundingVolume.getRegion()[2]);
        double maxLatDeg = Math.toDegrees(boundingVolume.getRegion()[3]);
        double minAlt = boundingVolume.getRegion()[4];
        double maxAlt = boundingVolume.getRegion()[5];

        double divisionsCount = Math.pow(2, (currDepth));

        List<Double> lonDivisions = new ArrayList<>();
        List<Double> latDivisions = new ArrayList<>();
        List<Double> altDivisions = new ArrayList<>();

        double lonStep = (maxLonDeg - minLonDeg) / divisionsCount;
        double latStep = (maxLatDeg - minLatDeg) / divisionsCount;
        double altStep = (maxAlt - minAlt) / divisionsCount;

        // exclude the first and last divisions, so i = 1 and i < divisionsCount
        // but we must include the 1rst and last divisions to calculate the localBBox
        for (int i = 0; i <= divisionsCount; i++) {
            lonDivisions.add(minLonDeg + i * lonStep);
            latDivisions.add(minLatDeg + i * latStep);
            altDivisions.add(minAlt + i * altStep);
        }

        // now, cut the scene by the divisions
        boolean someSceneCut = false;

        GaiaBoundingBox setBBox = tileInfo.getBoundingBox();
        if (setBBox == null) {
            log.error("Error : setBBox is null.");
        }
        KmlInfo kmlInfo = tileInfo.getKmlInfo();
        Vector3d geoCoordPosition = kmlInfo.getPosition();
        Vector3d posWC = GlobeUtils.geographicToCartesianWgs84(geoCoordPosition);
        Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(posWC);
        resultTransformMatrix.set(transformMatrix);
        Matrix4d transformMatrixInv = new Matrix4d(transformMatrix);
        transformMatrixInv.invert();

        Vector3d samplePointLC = new Vector3d();

        // make GaiaAAPlanes
        List<GaiaAAPlane> planesYZ = new ArrayList<>();
        List<GaiaAAPlane> planesXZ = new ArrayList<>();
        List<GaiaAAPlane> planesXY = new ArrayList<>();

        Vector3d samplePointGeoCoord;
        Vector3d samplePointWC;

        double localMinX = Double.MAX_VALUE;
        double localMinY = Double.MAX_VALUE;
        double localMinZ = Double.MAX_VALUE;
        double localMaxX = -Double.MAX_VALUE;
        double localMaxY = -Double.MAX_VALUE;
        double localMaxZ = -Double.MAX_VALUE;

        for (int i = 0; i < lonDivisions.size(); i++) {
            double lonDeg = lonDivisions.get(i);
            double latDeg = latDivisions.get(i);
            double altitude = altDivisions.get(i);

            // Longitude plane : create a point with lonDeg, geoCoordPosition.y, 0.0
            samplePointGeoCoord = new Vector3d(lonDeg, geoCoordPosition.y, 0.0);
            samplePointWC = GlobeUtils.geographicToCartesianWgs84(samplePointGeoCoord);
            transformMatrixInv.transformPosition(samplePointWC, samplePointLC);

            // check local min max X
            if (samplePointLC.x < localMinX) localMinX = samplePointLC.x;
            if (samplePointLC.x > localMaxX) localMaxX = samplePointLC.x;

            // check if the planeLC cuts the setBBox
            if (samplePointLC.x > setBBox.getMinX() && samplePointLC.x < setBBox.getMaxX()) {
                if(i > 0 && i < lonDivisions.size() - 1) {
                    GaiaAAPlane planeYZ = new GaiaAAPlane();
                    planeYZ.setPlaneType(PlaneType.YZ);
                    planeYZ.setPoint(new Vector3d(samplePointLC));
                    planesYZ.add(planeYZ);
                }
            }

            // Latitude plane : create a point with geoCoordPosition.x, latDeg, 0.0
            samplePointGeoCoord = new Vector3d(geoCoordPosition.x, latDeg, 0.0);
            samplePointWC = GlobeUtils.geographicToCartesianWgs84(samplePointGeoCoord);
            transformMatrixInv.transformPosition(samplePointWC, samplePointLC);

            // check local min max Y
            if (samplePointLC.y < localMinY) localMinY = samplePointLC.y;
            if (samplePointLC.y > localMaxY) localMaxY = samplePointLC.y;

            // check if the planeLC cuts the setBBox
            if (samplePointLC.y > setBBox.getMinY() && samplePointLC.y < setBBox.getMaxY()){
                if(i > 0 && i < latDivisions.size() - 1) {
                    GaiaAAPlane planeXZ = new GaiaAAPlane();
                    planeXZ.setPlaneType(PlaneType.XZ);
                    planeXZ.setPoint(new Vector3d(samplePointLC));
                    planesXZ.add(planeXZ);
                }
            }

            // Altitude plane : create a point with geoCoordPosition.x, geoCoordPosition.y, 0.0
            samplePointGeoCoord = new Vector3d(geoCoordPosition.x, geoCoordPosition.y, altitude);
            samplePointWC = GlobeUtils.geographicToCartesianWgs84(samplePointGeoCoord);
            transformMatrixInv.transformPosition(samplePointWC, samplePointLC);

            // check local min max Z
            if (samplePointLC.z < localMinZ) localMinZ = samplePointLC.z;
            if (samplePointLC.z > localMaxZ) localMaxZ = samplePointLC.z;

            // check if the planeLC cuts the setBBox
            if (samplePointLC.z > setBBox.getMinZ() && samplePointLC.z < setBBox.getMaxZ()){
                if(i > 0 && i < altDivisions.size() - 1) {
                    GaiaAAPlane planeXY = new GaiaAAPlane();
                    planeXY.setPlaneType(PlaneType.XY);
                    planeXY.setPoint(new Vector3d(samplePointLC));
                    planesXY.add(planeXY);
                }
            }
        }

        resultPlanes.addAll(planesXY);
        resultPlanes.addAll(planesXZ);
        resultPlanes.addAll(planesYZ);


        HalfEdgeOctree resultOctree = new HalfEdgeOctree(null);
        resultOctree.setSize(localMinX, localMinY, localMinZ, localMaxX, localMaxY, localMaxZ);
        resultOctree.setMaxDepth(currDepth);

        return resultOctree;
    }



    private GaiaBoundingBox calculateCartographicBoundingBox(GaiaScene gaiaScene, Matrix4d transformMatrix, GaiaBoundingBox resultBoundingBoxLC)
    {
//        GaiaScene gaiaSceneCut = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);
        GaiaBoundingBox boundingBoxCutLC = gaiaScene.getBoundingBox();
        resultBoundingBoxLC.set(boundingBoxCutLC);

        // Calculate cartographicBoundingBox
        double minPosLCX = boundingBoxCutLC.getMinX();
        double minPosLCY = boundingBoxCutLC.getMinY();
        double minPosLCZ = boundingBoxCutLC.getMinZ();

        double maxPosLCX = boundingBoxCutLC.getMaxX();
        double maxPosLCY = boundingBoxCutLC.getMaxY();
        double maxPosLCZ = boundingBoxCutLC.getMaxZ();

        Vector3d leftDownBottomLC = new Vector3d(minPosLCX, minPosLCY, minPosLCZ);
        Vector3d rightDownBottomLC = new Vector3d(maxPosLCX, minPosLCY, minPosLCZ);
        Vector3d rightUpBottomLC = new Vector3d(maxPosLCX, maxPosLCY, minPosLCZ);

        Vector3d leftDownUpLC = new Vector3d(minPosLCX, minPosLCY, maxPosLCZ);

        Vector3d leftDownBottomWC = transformMatrix.transformPosition(leftDownBottomLC);
        Vector3d geoCoordLeftDownBottom = GlobeUtils.cartesianToGeographicWgs84(leftDownBottomWC);

        Vector3d rightDownBottomWC = transformMatrix.transformPosition(rightDownBottomLC);
        Vector3d geoCoordRightDownBottom = GlobeUtils.cartesianToGeographicWgs84(rightDownBottomWC);

        Vector3d rightUpBottomWC = transformMatrix.transformPosition(rightUpBottomLC);
        Vector3d geoCoordRightUpBottom = GlobeUtils.cartesianToGeographicWgs84(rightUpBottomWC);

        Vector3d leftDownUpWC = transformMatrix.transformPosition(leftDownUpLC);
        Vector3d geoCoordLeftDownUp = GlobeUtils.cartesianToGeographicWgs84(leftDownUpWC);

        double minLonDegCut = geoCoordLeftDownBottom.x;
        double minLatDegCut = geoCoordLeftDownBottom.y;
        double maxLonDegCut = geoCoordRightDownBottom.x;
        double maxLatDegCut = geoCoordRightUpBottom.y;

        return new GaiaBoundingBox(minLonDegCut, minLatDegCut, geoCoordLeftDownBottom.z, maxLonDegCut, maxLatDegCut, geoCoordLeftDownUp.z, false);
    }

    private void cutRectangleCakeOneShoot(List<TileInfo> tileInfos, int lod, BoundingVolume rootNodeBoundingVolume, int depthIdx,
                                          List<TileInfo> resultTileInfos) throws FileNotFoundException {
        //****************************************************
        // Note : tileInfos must contain only one tileInfo
        //****************************************************

        // now, cut the scene by the divisions
        boolean someSceneCut = false;

        // load the first scene of the tileInfo
        TileInfo tileInfo = tileInfos.get(0);
        Path path = tileInfo.getTempPath();

        GaiaBoundingBox setBBox = tileInfo.getBoundingBox();
        if (setBBox == null) {
            log.error("Error : setBBox is null.");
        }
        KmlInfo kmlInfo = tileInfo.getKmlInfo();
        Vector3d geoCoordPosition = kmlInfo.getPosition();

        boolean checkTexCoord = true;
        boolean checkNormal = false;
        boolean checkColor = false;
        boolean checkBatchId = false;
        double errorWeld = 1e-4;

        List<GaiaAAPlane> allPlanes = new ArrayList<>();

        // load the file
        GaiaSet gaiaSet = GaiaSet.readFile(path);
        if (gaiaSet == null) return;

        GaiaScene scene = new GaiaScene(gaiaSet);

        scene.deleteNormals();
        scene.makeTriangleFaces();
        scene.weldVertices(errorWeld, checkTexCoord, checkNormal, checkColor, checkBatchId);
        HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);

        Matrix4d transformMatrix = new Matrix4d();
        HalfEdgeOctree resultOctree = this.getCuttingPlanesAndHalfEdgeOctree(tileInfo, lod, rootNodeBoundingVolume, depthIdx, allPlanes, transformMatrix);

        log.debug("cutting rectangle cake one shoot. lod : " + lod);


        double testOctreSize = resultOctree.getMaxSize();
        boolean scissorTextures = true;
        boolean makeSkirt = GlobalOptions.MAKE_SKIRT;


        // create tileInfos for the cut scenes
        String outputPathString = globalOptions.getOutputPath();
        String cutTempPathString = outputPathString + File.separator + "temp" + File.separator + "cutTemp";
        Path cutTempPath = Paths.get(cutTempPathString);
        // create directory if not exists
        if (!cutTempPath.toFile().exists() && cutTempPath.toFile().mkdirs()) {
            log.debug("cutTemp folder created.");
        }

        Path cutTempLodPath = cutTempPath.resolve("lod" + lod);
        if (!cutTempLodPath.toFile().exists() && cutTempLodPath.toFile().mkdirs()) {
            log.debug("cutTempLod folder created.");
        }

        List<TileInfo> cutTileInfos = this.cutHalfEdgeSceneByGaiaAAPlanesAndSaveTileInfos(halfEdgeScene, allPlanes, resultOctree, scissorTextures, makeSkirt, cutTempLodPath, transformMatrix, tileInfo);
        resultTileInfos.addAll(cutTileInfos);

        // delete halfEdgeScene
        halfEdgeScene.deleteObjects();
        gaiaSet.clear();
        scene.clear();
        System.gc();
        //return cutTileInfos;
    }

    public List<TileInfo> cutHalfEdgeSceneByGaiaAAPlanesAndSaveTileInfos(HalfEdgeScene halfEdgeScene, List<GaiaAAPlane> planes, HalfEdgeOctree resultOctree,
                                                                     boolean scissorTextures, boolean makeSkirt, Path cutTempLodPath, Matrix4d transformMatrix,
                                                                TileInfo motherTileInfo)
    {
        KmlInfo kmlInfo = motherTileInfo.getKmlInfo();
        Vector3d geoCoordPosition = kmlInfo.getPosition();

        double error = 1e-4;
        int planesCount = planes.size();
        for (int i = 0; i < planesCount; i++) {
            GaiaAAPlane plane = planes.get(i);
            halfEdgeScene.cutByPlane(plane.getPlaneType(), plane.getPoint(), error);
        }

        halfEdgeScene.deleteDegeneratedFaces();

        // now, distribute faces into octree
        resultOctree.getFaces().clear();
        List<HalfEdgeSurface> surfaces = halfEdgeScene.extractSurfaces(null);
        for (HalfEdgeSurface surface : surfaces) {
            List<HalfEdgeFace> faces = surface.getFaces();
            for (HalfEdgeFace face : faces) {
                if(face.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }
                resultOctree.getFaces().add(face);
            }
        }

        resultOctree.distributeFacesToTargetDepth(resultOctree.getMaxDepth());
        List<HalfEdgeOctree> octreesWithContents = new ArrayList<>();
        resultOctree.extractOctreesWithFaces(octreesWithContents);

        // now, separate the surface by the octrees
        //List<HalfEdgeScene> resultScenes = new ArrayList<>();
        List<TileInfo> cutTileInfos = new ArrayList<>();

        // set the classifyId for each face
        int octreesCount = octreesWithContents.size();
        for (int j = 0; j < octreesCount; j++) {
            HalfEdgeOctree octree = octreesWithContents.get(j);
            List<HalfEdgeFace> faces = octree.getFaces();
            for (HalfEdgeFace face : faces) {
                face.setClassifyId(j);
            }
        }

        for (int j = 0; j < octreesCount; j++) {
            int classifyId = j;

            // create a new HalfEdgeScene
            HalfEdgeScene cuttedScene = halfEdgeScene.cloneByClassifyId(classifyId);
            cuttedScene.deleteNoUsedMaterials();
            if(scissorTextures) {
                cuttedScene.scissorTexturesByMotherScene(halfEdgeScene.getMaterials());
            }

            if(makeSkirt) {
                cuttedScene.makeSkirt();
            }

            if(cuttedScene == null) {
                log.info("cuttedScene is null");
                continue;
            }


            //***************************************************************************************************************************
            GaiaScene gaiaSceneCut = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(cuttedScene);

            GaiaBoundingBox boundingBoxCutLC = new GaiaBoundingBox();

            GaiaBoundingBox cartographicBoundingBox = this.calculateCartographicBoundingBox(gaiaSceneCut, transformMatrix, boundingBoxCutLC);

            // create an originalPath for the cut scene
            Path cutScenePath = Paths.get("");
            gaiaSceneCut.setOriginalPath(cutScenePath);

            GaiaSet gaiaSetCut = GaiaSet.fromGaiaScene(gaiaSceneCut);
            UUID identifier = UUID.randomUUID();
            Path gaiaSetCutFolderPath = cutTempLodPath.resolve(identifier.toString());
            if (!gaiaSetCutFolderPath.toFile().exists() && gaiaSetCutFolderPath.toFile().mkdirs()) {
                log.debug("gaiaSetCut folder created.");
            }

            List<GaiaMaterial> materials = cuttedScene.getMaterials();
            // save the materials

            // save material atlas textures/////////////////////////////////////////////////////
            //Path parentPath = path.getParent();
            Path imagesPath = gaiaSetCutFolderPath.resolve("images");
            // make directories if not exists
            File imagesFolder = imagesPath.toFile();
            if (!imagesFolder.exists() && imagesFolder.mkdirs()) {
                log.debug("images folder created.");
            }
            for (GaiaMaterial material : materials) {
                List<GaiaTexture> textures = material.getTextures().get(TextureType.DIFFUSE);
                for (GaiaTexture texture : textures) {
                    if(texture == null){
                        continue;
                    }
                    // check if exist bufferedImage of the texture
                    if(texture.getBufferedImage() == null) {
                        // load the image
                        texture.loadImage();
                    }

                    if(texture.getBufferedImage() == null) {
                        continue;
                    }

                    texture.setParentPath(imagesPath.toString());
                    texture.saveImage(texture.getFullPath());
                }
            }

            boolean copyTexturesToNewPath = false;
            Path tempPathLod = gaiaSetCut.writeFileForPR(gaiaSetCutFolderPath, copyTexturesToNewPath);


            /// //////////////////////////////////////////////////////////////////////////////////////


            // delete the contents of the gaiaSceneCut before create tileInfo**********************
            gaiaSceneCut.getNodes().forEach(GaiaNode::clear);

            // create a new tileInfo for the cut scene
            TileInfo tileInfoCut = TileInfo.builder().scene(gaiaSceneCut).outputPath(motherTileInfo.getOutputPath()).build();
            tileInfoCut.setTempPath(tempPathLod);
            Matrix4d transformMatrixCut = new Matrix4d(motherTileInfo.getTransformMatrix());
            tileInfoCut.setTransformMatrix(transformMatrixCut);
            tileInfoCut.setBoundingBox(boundingBoxCutLC);
            tileInfoCut.setCartographicBBox(cartographicBoundingBox);


            // make a kmlInfo for the cut scene
            // In reality, we must recalculate the position of the cut scene. Provisionally, we use the same position
            // In reality, we must recalculate the position of the cut scene. Provisionally, we use the same position
            KmlInfo kmlInfoCut = KmlInfo.builder().position(geoCoordPosition).build();
            tileInfoCut.setKmlInfo(kmlInfoCut);
            cutTileInfos.add(tileInfoCut);

            cuttedScene.deleteObjects();
            gaiaSetCut.clear();
            gaiaSceneCut.clear();
        }

        return cutTileInfos;
    }

    public void writeTileset(Tileset tileset) {
        Path outputPath = new File(globalOptions.getOutputPath()).toPath();
        File tilesetFile = outputPath.resolve("tileset.json").toFile();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        //objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tilesetFile))) {
            String result = objectMapper.writeValueAsString(tileset);
            log.info("[Tile][Tileset] write 'tileset.json' file.");
            writer.write(result);
            globalOptions.setTilesetSize(result.length());
        } catch (IOException e) {
            log.error("[ERROR] :", e);
            throw new TileProcessingException(e.getMessage());
        }
    }

    private void createNode(Node parentNode, List<TileInfo> tileInfos, int nodeDepth) throws IOException {
        BoundingVolume parentBoundingVolume = parentNode.getBoundingVolume();
        BoundingVolume squareBoundingVolume = parentBoundingVolume.createSqureBoundingVolume();

        boolean refineAdd = globalOptions.isRefineAdd();
        long triangleLimit = globalOptions.getMaxTriangles();
        long totalTriangleCount = tileInfos.stream().mapToLong(TileInfo::getTriangleCount).sum();
        log.debug("[TriangleCount] Total : {}", totalTriangleCount);
        log.debug("[Tile][ContentNode][OBJECT] : {}", tileInfos.size());

        if (nodeDepth > globalOptions.getMaxNodeDepth()) {
            log.warn("[WARN][Tile] Node depth limit exceeded : {}", nodeDepth);
            Node childNode = createContentNode(parentNode, tileInfos, 0);
            if (childNode != null) {
                parentNode.getChildren().add(childNode);
            }
            return;
        }

        if (tileInfos.size() <= 1) {
            Node childNode = createContentNode(parentNode, tileInfos, 0);
            if (childNode != null) {
                parentNode.getChildren().add(childNode);
                createNode(childNode, tileInfos, nodeDepth + 1);
            }
        } else if (totalTriangleCount > triangleLimit) {
            List<List<TileInfo>> childrenScenes = squareBoundingVolume.distributeScene(tileInfos);
            for (int index = 0; index < childrenScenes.size(); index++) {
                List<TileInfo> childTileInfos = childrenScenes.get(index);
                Node childNode = createLogicalNode(parentNode, childTileInfos, index);
                if (childNode != null) {
                    parentNode.getChildren().add(childNode);
                    createNode(childNode, childTileInfos, nodeDepth + 1);
                }
            }
        } else if (totalTriangleCount > 1) {
            List<List<TileInfo>> childrenScenes = squareBoundingVolume.distributeScene(tileInfos);
            for (int index = 0; index < childrenScenes.size(); index++) {
                List<TileInfo> childTileInfos = childrenScenes.get(index);

                Node childNode = createContentNode(parentNode, childTileInfos, index);
                if (childNode != null) {
                    parentNode.getChildren().add(childNode);
                    Content content = childNode.getContent();
                    if (content != null && refineAdd) {
                        ContentInfo contentInfo = content.getContentInfo();
                        createNode(childNode, contentInfo.getRemainTileInfos(), nodeDepth + 1);
                    } else {
                        createNode(childNode, childTileInfos, nodeDepth + 1);
                    }
                }
            }
        } else if (tileInfos.size() <= 4 || !tileInfos.isEmpty()) {
            Node childNode = createContentNode(parentNode, tileInfos, 0);
            if (childNode != null) {
                parentNode.getChildren().add(childNode);
                createNode(childNode, tileInfos, nodeDepth + 1);
            }
        }
    }

    private Node createLogicalNode(Node parentNode, List<TileInfo> tileInfos, int index) {
        if (tileInfos.isEmpty()) {
            return null;
        }
        String nodeCode = parentNode.getNodeCode();
        nodeCode = nodeCode + index;
        log.info("[Tile][LogicalNode][" + nodeCode + "][OBJECT{}]", tileInfos.size());

        double geometricError = calcGeometricError(tileInfos);
        GaiaBoundingBox boundingBox = calcBoundingBox(tileInfos);
        Matrix4d transformMatrix = getTransformMatrix(boundingBox);
        if (globalOptions.isClassicTransformMatrix()) {
            rotateX90(transformMatrix);
        }
        BoundingVolume boundingVolume = new BoundingVolume(boundingBox);
        geometricError = DecimalUtils.cut(geometricError);

        Node childNode = new Node();
        childNode.setParent(parentNode);
        childNode.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
        childNode.setBoundingVolume(boundingVolume);
        childNode.setNodeCode(nodeCode);
        childNode.setGeometricError(geometricError);
        childNode.setRefine(Node.RefineType.REPLACE);
        childNode.setChildren(new ArrayList<>());
        return childNode;
    }

    private void makeContentsForNodes(Map<Node, List<TileInfo>> nodeTileInfoMap, int lod) {
        for (Map.Entry<Node, List<TileInfo>> entry : nodeTileInfoMap.entrySet()) {
            Node childNode = entry.getKey();
            if (childNode == null) {
                log.error("makeContentsForNodes() Error : childNode is null.");
                continue;
            }

            List<TileInfo> tileInfos = entry.getValue();
            for (TileInfo tileInfo : tileInfos) {
                GaiaScene scene = tileInfo.getScene();
                if (scene == null) {
                    log.error("makeContentsForNodes() Error : scene is null.");
                }
            }

            GaiaBoundingBox childBoundingBox = calcBoundingBox(tileInfos); // cartographicBBox
            Matrix4d transformMatrix = getTransformMatrix(childBoundingBox);
            if (globalOptions.isClassicTransformMatrix()) {
                rotateX90(transformMatrix);
            }
            BoundingVolume boundingVolume = new BoundingVolume(childBoundingBox);
            LevelOfDetail lodLevel = LevelOfDetail.getByLevel(lod);
            int lodError = lodLevel.getGeometricError();

            if (lod == 0) {
                lodError = 0;
            } else if (lod == 1) {
                lodError = 1;
            } else if (lod == 2) {
                lodError = 3;
            }

            childNode.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
            //childNode.setBoundingVolume(boundingVolume);
            childNode.setGeometricError(lodError + 0.001);


            if (!tileInfos.isEmpty()) {
                childNode.setRefine(Node.RefineType.REPLACE);
                ContentInfo contentInfo = new ContentInfo();
                String nodeCode = childNode.getNodeCode();
                contentInfo.setName(nodeCode);
                contentInfo.setLod(lodLevel);
                contentInfo.setBoundingBox(childBoundingBox);
                contentInfo.setNodeCode(nodeCode);
                contentInfo.setTileInfos(tileInfos);
                contentInfo.setTransformMatrix(transformMatrix);

                Content content = new Content();
                content.setUri("data/" + nodeCode + ".b3dm");
                content.setContentInfo(contentInfo);
                if(childNode.getContent() != null) {
                    log.error("Error : childNode.getContent() is not null.");
                }
                childNode.setContent(content);
            }
            else {
                childNode.setRefine(Node.RefineType.ADD);
            }
        }
    }

    private Node createContentNode(Node parentNode, List<TileInfo> tileInfos, int index) {
        if (tileInfos.isEmpty()) {
            return null;
        }
        int minLevel = globalOptions.getMinLod();
        int maxLevel = globalOptions.getMaxLod();
        boolean refineAdd = globalOptions.isRefineAdd();

        GaiaBoundingBox childBoundingBox = calcBoundingBox(tileInfos);
        Matrix4d transformMatrix = getTransformMatrix(childBoundingBox);
        if (globalOptions.isClassicTransformMatrix()) {
            rotateX90(transformMatrix);
        }
        BoundingVolume boundingVolume = new BoundingVolume(childBoundingBox);

        String nodeCode = parentNode.getNodeCode();
        LevelOfDetail minLod = LevelOfDetail.getByLevel(minLevel);
        LevelOfDetail maxLod = LevelOfDetail.getByLevel(maxLevel);
        boolean hasContent = nodeCode.contains("C");
        if (!hasContent) {
            nodeCode = nodeCode + "C";
        }
        LevelOfDetail lod = getLodByNodeCode(minLod, maxLod, nodeCode);
        if (lod == LevelOfDetail.NONE) {
            return null;
        }
        nodeCode = nodeCode + index;
        log.info("[Tile][ContentNode][" + nodeCode + "][LOD{}][OBJECT{}]", lod.getLevel(), tileInfos.size());

        int lodError = refineAdd ? lod.getGeometricErrorBlock() : lod.getGeometricError();
        List<TileInfo> resultInfos;
        List<TileInfo> remainInfos; // small buildings, to add after as ADD
        resultInfos = tileInfos.stream().filter(tileInfo -> {
            double geometricError = tileInfo.getBoundingBox().getLongestDistance();
            return geometricError >= lodError;
        }).collect(Collectors.toList());
        remainInfos = tileInfos.stream().filter(tileInfo -> {
            double geometricError = tileInfo.getBoundingBox().getLongestDistance();
            return geometricError < lodError;
        }).collect(Collectors.toList());

        Node childNode = new Node();
        childNode.setParent(parentNode);
        childNode.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
        childNode.setBoundingVolume(boundingVolume);
        childNode.setNodeCode(nodeCode);
        childNode.setGeometricError(lodError + 0.01);
        childNode.setChildren(new ArrayList<>());

        childNode.setRefine(Node.RefineType.REPLACE);
        if (!resultInfos.isEmpty()) {
            ContentInfo contentInfo = new ContentInfo();
            contentInfo.setName(nodeCode);
            contentInfo.setLod(lod);
            contentInfo.setBoundingBox(childBoundingBox);
            contentInfo.setNodeCode(nodeCode);
            contentInfo.setTileInfos(resultInfos);
            contentInfo.setRemainTileInfos(remainInfos);
            contentInfo.setTransformMatrix(transformMatrix);

            Content content = new Content();
            content.setUri("data/" + nodeCode + ".b3dm");
            content.setContentInfo(contentInfo);
            childNode.setContent(content);
        } else {
            log.debug("[Tile][ContentNode][{}] No Contents", nodeCode);
        }
        return childNode;
    }

    private LevelOfDetail getLodByNodeCode(LevelOfDetail minLod, LevelOfDetail maxLod, String nodeCode) {
        int minLevel = minLod.getLevel();
        int maxLevel = maxLod.getLevel();
        String[] splitCode = nodeCode.split("C");
        if (splitCode.length > 1) {
            String contentLevel = nodeCode.split("C")[1];
            int level = maxLevel - contentLevel.length();
            if (level < minLevel) {
                level = -1;
            }
            return LevelOfDetail.getByLevel(level);
        } else {
            return maxLod;
        }
    }


    // for multi-threading
    private void executeThread(ExecutorService executorService, List<Runnable> tasks) throws InterruptedException {
        try {
            for (Runnable task : tasks) {
                Future<?> future = executorService.submit(task);
                /*if (globalOptions.isDebug()) {
                    future.get();
                }*/
            }
        } catch (Exception e) {
            log.error("Failed to execute thread.", e);
            throw new RuntimeException(e);
        }
        executorService.shutdown();
        do {
            if (executorService.isTerminated()) {
                executorService.shutdownNow();
            }
        } while (!executorService.awaitTermination(2, TimeUnit.SECONDS));
    }
}
