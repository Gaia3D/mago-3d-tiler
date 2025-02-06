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
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.GaiaTexture;
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
import com.gaia3d.util.StringUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.apache.commons.io.FileUtils.deleteDirectory;

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

        // make globalBoundingBox as square.***
        double minLonDeg = globalBoundingBox.getMinX();
        double minLatDeg = globalBoundingBox.getMinY();
        double maxLonDeg = globalBoundingBox.getMaxX();
        double maxLatDeg = globalBoundingBox.getMaxY();

        // calculate the rootOctTree size.***
        double minLatRad = Math.toRadians(minLatDeg);
        double maxLatRad = Math.toRadians(maxLatDeg);
        double minLonRad = Math.toRadians(minLonDeg);
        double maxLonRad = Math.toRadians(maxLonDeg);

        // find max distance.***
        double distanceBetweenLat = GlobeUtils.distanceBetweenLatitudesRad(minLatRad, maxLatRad);
        double distanceBetweenLon = GlobeUtils.distanceBetweenLongitudesRad(minLatRad, minLonRad, maxLonRad);
        double distanceFinal = Math.max(distanceBetweenLat, distanceBetweenLon);
        double desiredLeafDist = 25.0;
        desiredLeafDist = 25.0; // test.***

        int desiredDepth = (int) Math.ceil(HalfEdgeUtils.log2(distanceFinal / desiredLeafDist));
        double desiredDistanceBetweenLat = desiredLeafDist * Math.pow(2, desiredDepth);
        double desiredAngRadLat = GlobeUtils.angRadLatitudeForDistance(minLatRad, desiredDistanceBetweenLat);
        double desiredAngRadLon = GlobeUtils.angRadLongitudeForDistance(minLatRad, desiredDistanceBetweenLat);
        double desiredAngDegLat = Math.toDegrees(desiredAngRadLat);
        double desiredAngDegLon = Math.toDegrees(desiredAngRadLon);
        maxLonDeg = minLonDeg + desiredAngDegLon;
        maxLatDeg = minLatDeg + desiredAngDegLat;
        // end calculate the rootOctTree size.---

        // make CUBE boundingBox.***
        globalBoundingBox.setMaxZ(globalBoundingBox.getMinZ() + desiredDistanceBetweenLat);// make CUBE boundingBox.***
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
        int currDepth = desiredDepth - lod;
        Map<Node, List<TileInfo>> nodeTileInfoMap = new HashMap<>();

        multiThreadCuttingAndScissorProcess(tileInfosCopy, lod, root, desiredDepth);

        // distribute contents to node in the correspondent depth.***
        // After process "cutRectangleCake", in tileInfosCopy there are tileInfos that are cut by the boundary planes of the nodes.***
        distributeContentsToNodesOctTree(root, tileInfosCopy, currDepth, nodeTileInfoMap);
        makeContentsForNodes(nodeTileInfoMap, lod);

        /* End lod 0 process */

        int netSurfaceStartLod = 3;

        DecimateParameters decimateParameters = new DecimateParameters();
        for (int d = 1; d < desiredDepth; d++) {
            lod = d;
            tileInfosCopy.clear();
            nodeTileInfoMap.clear();
            tileInfosCopy = this.getTileInfosCopy(tileInfos, lod, tileInfosCopy);

            if (d == 1) {
                decimateParameters.setBasicValues(5.0, 0.4, 0.9, 32.0, 1000000, 1, 1.0);
            } else if (d == 2) {
                decimateParameters.setBasicValues(10.0, 0.4, 1.0, 32.0, 1000000, 2, 1.5);
            } else if (d == 3) {
                decimateParameters.setBasicValues(15.0, 0.6, 1.0, 32.0, 1000000, 2, 2.0);
            } else if (d == 4) {
                decimateParameters.setBasicValues(20.0, 0.8, 1.0, 32.0, 1000000, 2, 2.5);
            } else {
                decimateParameters.setBasicValues(25.0, 0.2, 0.9, 32.0, 1000000, 2, 1.0);
            }

            multiThreadCuttingAndScissorProcess(tileInfosCopy, lod, root, desiredDepth);
            decimateScenes(tileInfosCopy, lod, decimateParameters);

            currDepth = desiredDepth - lod;
            distributeContentsToNodesOctTree(root, tileInfosCopy, currDepth, nodeTileInfoMap);
            makeContentsForNodes(nodeTileInfoMap, lod);

            if (d >= (netSurfaceStartLod)) {
                break;
            }
        }

        // net surfaces with boxTextures.***
        for (int d = netSurfaceStartLod; d < desiredDepth; d++) {
            lod = d;
            currDepth = desiredDepth - lod;
            double boxSizeForCurrDepth = desiredDistanceBetweenLat / Math.pow(2, currDepth);
            double pixelsForMeter = 180.0 / boxSizeForCurrDepth;
            tileInfosCopy.clear();
            nodeTileInfoMap.clear();
            tileInfosCopy = this.getTileInfosCopy(tileInfos, lod, tileInfosCopy);
            // public void setBasicValues(double maxDiffAngDegrees, double hedgeMinLength, double frontierMaxDiffAngDeg, double maxAspectRatio, int maxCollapsesCount)
            decimateParameters.setBasicValues(10.0, 0.5, 1.0, 6.0, 1000000, 1, 1.8);
            if (d == 3) {
                decimateParameters.setBasicValues(15.0, 1.0, 1.0, 15.0, 1000000, 1, 1.8);
            } else if (d == 4) {
                decimateParameters.setBasicValues(20.0, 1.2, 1.0, 15.0, 1000000, 1, 1.8);
            } else if (d == 5) {
                decimateParameters.setBasicValues(25.0, 1.5, 1.0, 15.0, 1000000, 1, 1.8);
            } else if (d == 6) {
                decimateParameters.setBasicValues(30.0, 2.0, 1.0, 15.0, 1000000, 1, 1.8);
            }

            multiThreadCuttingAndScissorProcess(tileInfosCopy, lod, root, desiredDepth);
            makeNetSurfacesWithBoxTextures(tileInfosCopy, lod, decimateParameters, pixelsForMeter);

            currDepth = desiredDepth - lod;
            distributeContentsToNodesOctTree(root, tileInfosCopy, currDepth, nodeTileInfoMap);
            makeContentsForNodes(nodeTileInfoMap, lod);

            if (d >= 4) {
                break;
            }
        }

        // Check if is necessary netSurfaces nodes.***
        lod = 5;
        for (int depth = desiredDepth - lod; depth >= 0; depth--) {
            tileInfosCopy.clear();
            tileInfosCopy = this.getTileInfosCopy(tileInfos, 0, tileInfosCopy);
            createNetSurfaceNodes(root, tileInfosCopy, depth, desiredDepth);
        }


        // now, delete nodes that have no contents.***
        root.deleteNoContentNodes();

        //setGeometryErrorToNodeAutomatic(root, desiredDepth);
        setGeometryErrorToNodeManual(root, desiredDepth);

        root.setGeometricError(1000.0);
        Asset asset = createAsset();
        Tileset tileset = new Tileset();
        tileset.setAsset(asset);
        tileset.setRoot(root);
        tileset.setGeometricError(1000.0);
        return tileset;
    }

    private void multiThreadCuttingAndScissorProcess(List<TileInfo> tileInfos, int lod, Node rootNode, int maxDepth) {
        // multi-threading.***
        ExecutorService executorService = Executors.newFixedThreadPool(globalOptions.getMultiThreadCount());
        List<Runnable> tasks = new ArrayList<>();
        List<TileInfo> finalTileInfosCopy = new ArrayList<>();

        log.info("Cutting and Scissor process is started. Total tileInfos : {}", tileInfos.size());

        int tileInfosCount = tileInfos.size();
        AtomicInteger atomicProcessCount = new AtomicInteger(0);
        for (TileInfo tileInfo : tileInfos) {
            List<TileInfo> singleTileInfoList = new ArrayList<>();
            singleTileInfoList.add(tileInfo);
            String tileInfoName = tileInfo.getTempPath().getFileName().toString();
            Runnable callableTask = () -> {
                try {
                    int processCount = atomicProcessCount.incrementAndGet();
                    log.info("[Tile][PhotoRealistic][{}/{}] Generating tile : {}", processCount, tileInfosCount, tileInfoName);
                    log.info("[Tile][PhotoRealistic][{}/{}] - Cut RectangleCake one shoot... : {}", processCount, tileInfosCount, tileInfoName);
                    cutRectangleCakeOneShoot(singleTileInfoList, lod, rootNode, maxDepth);
                    finalTileInfosCopy.addAll(singleTileInfoList);
                } catch (IOException e) {
                    log.error("Error :", e);
                    throw new RuntimeException(e);
                }
            };
            tasks.add(callableTask);
        }

        try {
            executeThread(executorService, tasks);
        } catch (InterruptedException e) {
            log.error("Error :", e);
            throw new RuntimeException(e);
        }


        log.info("Cutting and Scissor process is done. Total tileInfos : {}", finalTileInfosCopy.size());

        //******
        ExecutorService executorService2 = Executors.newFixedThreadPool(globalOptions.getMultiThreadCount());
        List<Runnable> tasks2 = new ArrayList<>();

        int tileInfosCount2 = finalTileInfosCopy.size();
        AtomicInteger atomicProcessCount2 = new AtomicInteger(0);
        for (TileInfo tileInfo : finalTileInfosCopy) {
            List<TileInfo> singleTileInfoList = new ArrayList<>();
            singleTileInfoList.add(tileInfo);
            String tileInfoName = tileInfo.getTempPath().getFileName().toString();
            Runnable callableTask = () -> {
                int processCount = atomicProcessCount2.incrementAndGet();
                log.info("[Tile][PhotoRealistic][{}/{}] - ScissorTextures... : {}", processCount, tileInfosCount2, tileInfoName);
                scissorTextures(singleTileInfoList);
                log.info("[Tile][PhotoRealistic][{}/{}] - Make Skirt... : {}", processCount, tileInfosCount2, tileInfoName);
                makeSkirt(singleTileInfoList);
                log.info("[Tile][PhotoRealistic][{}/{}] Tile creation is done. : {}", processCount, tileInfosCount2, tileInfoName);
            };
            tasks2.add(callableTask);
        }

        try {
            executeThread(executorService2, tasks2);
        } catch (InterruptedException e) {
            log.error("Error :", e);
            throw new RuntimeException(e);
        }

        tileInfos.clear();
        tileInfos.addAll(finalTileInfosCopy);
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

            // load the file.***
            GaiaSet gaiaSet;
            try {
                gaiaSet = GaiaSet.readFile(tempPath);
                if (gaiaSet == null) {
                    log.error("Error : gaiaSet is null. pth : " + tempPath);
                    continue;
                }
            } catch (IOException e) {
                log.error("Error : ", e);
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

            tilerExtensionModule.makeNetSurfacesWithBoxTexturesObliqueCamera(gaiaSceneList, resultDecimatedScenes, decimateParameters, pixelsForMeter);

            if(resultDecimatedScenes.isEmpty()) {
                // sometimes the resultDecimatedScenes is empty because when rendering the scene, the scene is almost out of the camera.***
                log.error("Error : resultDecimatedScenes is empty.");
                gaiaSet.clear(); // delete gaiaSet.***
                scene.clear(); // delete scene.***
                continue;
            }

            HalfEdgeScene halfEdgeSceneLod = resultDecimatedScenes.get(0);

            // Save the textures in a temp folder.***
            List<GaiaMaterial> materials = halfEdgeSceneLod.getMaterials();
            for (GaiaMaterial material : materials) {
                List<GaiaTexture> textures = material.getTextures().get(TextureType.DIFFUSE);
                for (GaiaTexture texture : textures) {
                    // change the texture name.***
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
            gaiaSet.clear(); // delete gaiaSet.***
            scene.clear(); // delete scene.***
            tempSetLod1.clear(); // delete tempSetLod1.***
            sceneLod1.clear(); // delete sceneLod1.***
        }
    }

    public void decimateScenes(List<TileInfo> tileInfos, int lod, DecimateParameters decimateParameters) {
        log.info("Decimating scenes for lod : " + lod);
        TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();
        List<GaiaScene> gaiaSceneList = new ArrayList<>();
        List<HalfEdgeScene> resultDecimatedScenes = new ArrayList<>();

        int tileInfosCount = tileInfos.size();
        for (int i = 0; i < tileInfosCount; i++) {
            log.info("Decimating scene : " + i + " of " + tileInfosCount);
            TileInfo tileInfo = tileInfos.get(i);
            Path tempPath = tileInfo.getTempPath();
            Path tempFolder = tempPath.getParent();

            // load the file.***
            GaiaSet gaiaSet;
            try {
                gaiaSet = GaiaSet.readFile(tempPath);
                if (gaiaSet == null) {
                    log.error("Error : gaiaSet is null. pth : " + tempPath);
                    continue;
                }
            } catch (IOException e) {
                log.error("Error : ", e);
                throw new RuntimeException(e);
            }
            GaiaScene scene = new GaiaScene(gaiaSet);
            scene.setOriginalPath(tileInfo.getTempPath());
            //scene.setOriginalPath(tileInfo.getScenePath());
            scene.makeTriangleFaces();

            gaiaSceneList.clear();
            resultDecimatedScenes.clear();
            gaiaSceneList.add(scene);

            tilerExtensionModule.decimateByObliqueCamera(gaiaSceneList, resultDecimatedScenes, decimateParameters);

            if (resultDecimatedScenes.isEmpty()) {
                log.error("Error : resultDecimatedScenes is empty." + tempPath);
                continue;
            }

            HalfEdgeScene halfEdgeSceneLod = resultDecimatedScenes.get(0);

            // Save the textures in a temp folder.***
            List<GaiaMaterial> materials = halfEdgeSceneLod.getMaterials();
            for (GaiaMaterial material : materials) {
                List<GaiaTexture> textures = material.getTextures().get(TextureType.DIFFUSE);
                for (GaiaTexture texture : textures) {
                    // change the texture name.***
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
            //tempPathLod.add(currTempPathLod);
        }
    }

    public void decimateScenesByObliqueCamera(List<TileInfo> tileInfos, int lod, DecimateParameters decimateParameters) {
        log.info("Decimating scenes for lod : " + lod);
        TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();
        List<GaiaScene> gaiaSceneList = new ArrayList<>();
        List<HalfEdgeScene> resultDecimatedScenes = new ArrayList<>();

        int tileInfosCount = tileInfos.size();
        for (int i = 0; i < tileInfosCount; i++) {
            log.info("Decimating scene : " + i + " of " + tileInfosCount);
            TileInfo tileInfo = tileInfos.get(i);
            Path tempPath = tileInfo.getTempPath();
            Path tempFolder = tempPath.getParent();

            // load the file.***
            GaiaSet gaiaSet;
            try {
                gaiaSet = GaiaSet.readFile(tempPath);
                if (gaiaSet == null) {
                    log.error("Error : gaiaSet is null. pth : " + tempPath);
                    continue;
                }
            } catch (IOException e) {
                log.error("Error : ", e);
                throw new RuntimeException(e);
            }
            GaiaScene scene = new GaiaScene(gaiaSet);
            scene.setOriginalPath(tileInfo.getScenePath());
            scene.makeTriangleFaces();

            gaiaSceneList.clear();
            resultDecimatedScenes.clear();
            gaiaSceneList.add(scene);
            tilerExtensionModule.decimateByObliqueCamera(gaiaSceneList, resultDecimatedScenes, decimateParameters);

            if (resultDecimatedScenes.isEmpty()) {
                log.error("Error : resultDecimatedScenes is empty." + tempPath);
                continue;
            }

            HalfEdgeScene halfEdgeSceneLod = resultDecimatedScenes.get(0);

            // Save the textures in a temp folder.***
            List<GaiaMaterial> materials = halfEdgeSceneLod.getMaterials();
            for (GaiaMaterial material : materials) {
                List<GaiaTexture> textures = material.getTextures().get(TextureType.DIFFUSE);
                for (GaiaTexture texture : textures) {
                    // change the texture name.***
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

            String aux = "lod" + lod;
            Path tempFolderLod = tempFolder.resolve(aux);
            Path currTempPathLod = tempSetLod1.writeFile(tempFolderLod, tileInfo.getSerial(), tempSetLod1.getAttribute()/*, scale*/);
            tileInfo.setTempPath(currTempPathLod);
            //tempPathLod.add(currTempPathLod);
        }
    }

    private double getNodeLatitudesLengthInMeters(Node node) {
        // make globalBoundingBox as square.***
        double[] region = node.getBoundingVolume().getRegion();
        double minLatRad = region[1];
        double maxLatRad = region[3];
        return GlobeUtils.distanceBetweenLatitudesRad(minLatRad, maxLatRad);
    }

    private void createNetSurfaceNodes(Node rootNode, List<TileInfo> tileInfos, int nodeDepth, int maxDepth) {
        // 1rst, find all tileInfos that intersects with the node.***
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

            // create sceneInfos.***
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
                log.info("Error : sceneInfos is empty.");
                continue;
            }

            // render the sceneInfos and obtain the color and depth images.************************************************************
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
                log.info("Error : resultImages size is less than 2.");
                continue;
            }
            BufferedImage bufferedImageColor = resultImages.get(0);
            BufferedImage bufferedImageDepth = resultImages.get(1);

            // now, make a halfEdgeScene from the bufferedImages.*********************************************************************
            String outputPathString = globalOptions.getOutputPath();
            String netTempPathString = outputPathString + File.separator + "netTemp";
            Path netTempPath = Paths.get(netTempPathString);
            // create dirs if not exists.***
            File netTempFile = netTempPath.toFile();
            if (!netTempFile.exists() && netTempFile.mkdirs()) {
                log.debug("info : netTemp folder created.");
            }

            String netSetFolderPathString = netTempPathString + File.separator + "netSet_nodeDepth_" + nodeDepth + "_" + i;
            Path netSetFolderPath = Paths.get(netSetFolderPathString);
            // create dirs if not exists.***
            File netSetFile = netSetFolderPath.toFile();
            if (!netSetFile.exists() && netTempFile.mkdirs()) {
                log.debug("info : netSet folder created.");
            }
            String netSetImagesFolderPathString = netSetFolderPathString + File.separator + "images";
            Path netSetImagesFolderPath = Paths.get(netSetImagesFolderPathString);
            // create dirs if not exists.***
            File netSetImagesFolder = netSetImagesFolderPath.toFile();
            if (!netSetImagesFolder.exists() && netSetImagesFolder.mkdirs()) {
                log.debug("info : netSetImages folder created.");
            }

            // save the bufferedImageColor into the netSetImagesFolder.***
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

            // now, create material for the halfEdgeScene.***
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

            // now set materialId to the halfEdgeScene.***
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

            //calculate lod.***
            int lod = maxDepth - nodeDepth;
            double netSurfaceGeometricError = (lod + 1);
            node.setGeometricError(netSurfaceGeometricError);

            // make contents for the node.***
            List<TileInfo> netTileInfos = new ArrayList<>();
            TileInfo tileInfoNet = TileInfo.builder().scene(gaiaScene).outputPath(netSetFolderPath).build();
            tileInfoNet.setTempPath(netSetPath);
            Matrix4d transformMatrixNet = new Matrix4d(nodeTMatrix);
            tileInfoNet.setTransformMatrix(transformMatrixNet);
            tileInfoNet.setBoundingBox(nodeBBoxLC);
            tileInfoNet.setCartographicBBox(null);

            // make a kmlInfo for the cut scene.***
            KmlInfo kmlInfoCut = KmlInfo.builder().position(nodeCenterGeoCoordDeg).build();
            tileInfoNet.setKmlInfo(kmlInfoCut);
            netTileInfos.add(tileInfoNet);

            ContentInfo contentInfo = new ContentInfo();
            String nodeCode = node.getNodeCode();
            contentInfo.setName(nodeCode);
            LevelOfDetail lodLevel = LevelOfDetail.getByLevel(3);
            int lodError = lodLevel.getGeometricError();
            contentInfo.setLod(lodLevel);
            contentInfo.setBoundingBox(nodeCartographicBBox); // must be cartographicBBox.***
            contentInfo.setNodeCode(node.getNodeCode());
            contentInfo.setTileInfos(netTileInfos);
            contentInfo.setRemainTileInfos(null);
            contentInfo.setTransformMatrix(nodeTMatrix);

            Content content = new Content();
            content.setUri("data/" + nodeCode + ".b3dm");
            content.setContentInfo(contentInfo);
            node.setContent(content);

            // delete scenes.***
            halfEdgeScene.deleteObjects();
            gaiaScene.clear();
            gaiaSet.clear();

            // test save resultImages.***
            String sceneName = "mosaicRenderTest_" + i + "_color";
            String sceneRawName = sceneName;
            imageExtension = "png";
            String outputFolderPath = globalOptions.getOutputPath();
            try {
                File outputFile = new File(outputFolderPath, sceneRawName + "." + imageExtension);
                log.info("[Write Image] : {}", outputFile.getAbsoluteFile());
                ImageIO.write(bufferedImageColor, "png", outputFile);
            } catch (Exception e) {
                log.error("error : ", e);
            }

            try {
                sceneName = "mosaicRenderTest_" + i + "_depth";
                imageExtension = "png";
                File outputFile = new File(outputFolderPath, sceneName + "." + imageExtension);
                log.info("[Write Image] : {}", outputFile.getAbsoluteFile());
                ImageIO.write(bufferedImageDepth, "png", outputFile);
            } catch (Exception e) {
                log.error("error : ", e);
            }
        }
    }

    private void createNetSurfaceNodesByPyramidDeformation(Node rootNode, List<TileInfo> tileInfos, int nodeDepth, int maxDepth) {
        // 1rst, find all tileInfos that intersects with the node.***
        log.info("Creating netSurface nodes for nodeDepth : " + nodeDepth + " of maxDepth : " + maxDepth);
        List<Node> nodes = new ArrayList<>();
        rootNode.getNodesByDepth(nodeDepth, nodes);
        List<TileInfo> tileInfosOfNode = new ArrayList<>();
        TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();

        int nodesCount = nodes.size();
        for (int i = 0; i < nodesCount; i++) {
            tileInfosOfNode.clear();
            Node node = nodes.get(i);
            for (TileInfo tileInfo : tileInfos) {
                if (intersectsNodeWithTileInfo(node, tileInfo)) {
                    tileInfosOfNode.add(tileInfo);
                }
            }

            int tileInfosOfNodeCount = tileInfosOfNode.size();
            if (tileInfosOfNodeCount == 0) continue;

            node.setRefine(Node.RefineType.REPLACE);

            // create sceneInfos.***
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

            // render the sceneInfos and obtain the color and depth images.************************************************************
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
            int maxScreenSize = GlobalOptions.REALISTIC_SCREEN_COLOR_TEXTURE_SIZE;
            int maxDepthScreenSize = GlobalOptions.REALISTIC_SCREEN_DEPTH_TEXTURE_SIZE;

            List<HalfEdgeScene> resultHalfEdgeScenes = new ArrayList<>();
            tilerExtensionModule.makeNetSurfacesByPyramidDeformationRender(sceneInfos, bufferedImageType, resultHalfEdgeScenes, resultImages, nodeBBoxLC, nodeTMatrix, maxScreenSize, maxDepthScreenSize);

            if (resultHalfEdgeScenes.isEmpty()) {
                log.info("info : resultHalfEdgeScenes is empty.");
                continue;
            }

            BufferedImage bufferedImageColor = resultImages.get(0);

            // now, make a halfEdgeScene from the bufferedImages.*********************************************************************
            String outputPathString = globalOptions.getOutputPath();
            String netTempPathString = outputPathString + File.separator + "netTemp";
            Path netTempPath = Paths.get(netTempPathString);
            // create dirs if not exists.***
            File netTempFile = netTempPath.toFile();
            if (!netTempFile.exists() && netTempFile.mkdirs()) {
                log.debug("netTemp folder created.");
            }

            String netSetFolderPathString = netTempPathString + File.separator + "netSet_nodeDepth_" + nodeDepth + "_" + i;
            Path netSetFolderPath = Paths.get(netSetFolderPathString);
            // create dirs if not exists.***
            File netSetFile = netSetFolderPath.toFile();
            if (!netSetFile.exists() && netSetFile.mkdirs()) {
                log.debug("netSet folder created.");
            }
            String netSetImagesFolderPathString = netSetFolderPathString + File.separator + "images";
            Path netSetImagesFolderPath = Paths.get(netSetImagesFolderPathString);
            // create dirs if not exists.***
            File netSetImagesFolder = netSetImagesFolderPath.toFile();
            if (!netSetImagesFolder.exists() && netSetImagesFolder.mkdirs()) {
                log.debug("netSetImages folder created.");
            }

            // save the bufferedImageColor into the netSetImagesFolder.***
            String imageExtension = "png";
            String imagePath = "netScene_" + nodeDepth + "_" + i + "_color" + "." + imageExtension;
            try {
                File file = new File(netSetImagesFolderPathString + File.separator + imagePath);
                log.info("[Write Image] : {}", file.getAbsoluteFile());
                ImageIO.write(bufferedImageColor, "png", file);
            } catch (Exception e) {
                log.error("error : ", e);
            }

            HalfEdgeScene halfEdgeScene = resultHalfEdgeScenes.get(0);
            if (halfEdgeScene == null) {
                log.warn("halfEdgeScene is null.");
                continue;
            }
            double maxDiffAngDeg = 35.0;
            double hedgeMinLength = 0.5;
            double frontierMaxDiffAngDeg = 30.0;
            double maxAspectRatio = 6.0;
            DecimateParameters decimateParameters = new DecimateParameters();
            decimateParameters.setBasicValues(maxDiffAngDeg, hedgeMinLength, frontierMaxDiffAngDeg, maxAspectRatio, 1000000, 2, 1.8);
            halfEdgeScene.doTrianglesReduction(decimateParameters);
            if (halfEdgeScene.getTrianglesCount() == 0) continue;

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

            // now set materialId to the halfEdgeScene.***
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

            //calculate lod.***
            int lod = maxDepth - nodeDepth;
            double netSurfaceGeometricError = (lod + 1);
            node.setGeometricError(netSurfaceGeometricError);

            // make contents for the node.***
            List<TileInfo> netTileInfos = new ArrayList<>();
            TileInfo tileInfoNet = TileInfo.builder().scene(gaiaScene).outputPath(netSetFolderPath).build();
            tileInfoNet.setTempPath(netSetPath);
            Matrix4d transformMatrixNet = new Matrix4d(nodeTMatrix);
            tileInfoNet.setTransformMatrix(transformMatrixNet);
            tileInfoNet.setBoundingBox(nodeBBoxLC);
            tileInfoNet.setCartographicBBox(null);

            // make a kmlInfo for the cut scene.***
            KmlInfo kmlInfoCut = KmlInfo.builder().position(nodeCenterGeoCoordDeg).build();
            tileInfoNet.setKmlInfo(kmlInfoCut);
            netTileInfos.add(tileInfoNet);

            ContentInfo contentInfo = new ContentInfo();
            String nodeCode = node.getNodeCode();
            contentInfo.setName(nodeCode);
            LevelOfDetail lodLevel = LevelOfDetail.getByLevel(3);
            contentInfo.setLod(lodLevel);
            contentInfo.setBoundingBox(nodeCartographicBBox); // must be cartographicBBox.***
            contentInfo.setNodeCode(node.getNodeCode());
            contentInfo.setTileInfos(netTileInfos);
            contentInfo.setRemainTileInfos(null);
            contentInfo.setTransformMatrix(nodeTMatrix);

            Content content = new Content();
            content.setUri("data/" + nodeCode + ".b3dm");
            content.setContentInfo(contentInfo);
            node.setContent(content);

            // delete scenes.***
            halfEdgeScene.deleteObjects();
            gaiaScene.clear();
            gaiaSet.clear();

            // test save resultImages.***
            String sceneName = "mosaicRenderTest_" + i + "_color";
            imageExtension = "png";
            String outputFolderPath = globalOptions.getOutputPath();
            try {
                File outputFile = new File(outputFolderPath, sceneName + "." + imageExtension);
                log.info("[Write Image] : {}", outputFile.getAbsoluteFile());
                ImageIO.write(bufferedImageColor, "png", outputFile);
            } catch (Exception e) {
                log.error("error : ", e);
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

            Node childNode = rootNode.getIntersectedNode(tileInfoCenterGeoCoordRad, nodeDepth);
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

            // change the tempPath of the tileInfos by tempPathLod.***
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

            // load the file.***
            try {
                GaiaSet gaiaSet = GaiaSet.readFile(path);
                if (gaiaSet == null) {
                    log.error("Error : gaiaSet is null. pth : " + path);
                    continue;
                }
                GaiaScene scene = new GaiaScene(gaiaSet);

                HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);
                halfEdgeScene.makeSkirt();

                // once scene is scissored, must change the materials of the gaiaSet and overwrite the file.***
                GaiaScene skirtScene = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);
                GaiaSet gaiaSet2 = GaiaSet.fromGaiaScene(skirtScene);

                // overwrite the file.***
                gaiaSet2.writeFileInThePath(path);

                scene.clear();
                gaiaSet.clear();
                halfEdgeScene.deleteObjects();
                gaiaSet2.clear();
                skirtScene.clear();
            } catch (IOException e) {
                log.error("Error : ", e);
                throw new RuntimeException(e);
            }
        }
    }

    private void scissorTextures(List<TileInfo> tileInfos) {
        int tileInfosCount = tileInfos.size();
        for (int i = 0; i < tileInfosCount; i++) {
            TileInfo tileInfo = tileInfos.get(i);
            Path path = tileInfo.getTempPath();

            // load the file.***
            try {
                GaiaSet gaiaSet = GaiaSet.readFileForPR(path); // "readFileForPR" do not modify the textures path.***
                if (gaiaSet == null) {
                    log.error("Error : gaiaSet is null. pth : " + path);
                    continue;
                }
                GaiaScene scene = new GaiaScene(gaiaSet);

                HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);
                log.debug("Scissoring textures of tile : " + i + " of : " + tileInfosCount);
                halfEdgeScene.scissorTextures();

                // after scissors, the atlas images are into the textures of the materials.***
                List<GaiaMaterial> materials = halfEdgeScene.getMaterials();

                // once scene is scissored, must change the materials of the gaiaSet and overwrite the file.***
                GaiaScene scissorsScene = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);
                GaiaSet gaiaSet2 = GaiaSet.fromGaiaScene(scissorsScene);

                // save material atlas textures.***
                Path parentPath = path.getParent();
                Path imagesPath = parentPath.resolve("images");
                // make directories if not exists.***
                File imagesFolder = imagesPath.toFile();
                if (!imagesFolder.exists() && imagesFolder.mkdirs()) {
                    log.debug("images folder created.");
                }
                for (GaiaMaterial material : materials) {
                    List<GaiaTexture> textures = material.getTextures().get(TextureType.DIFFUSE);
                    for (GaiaTexture texture : textures) {
                        // check if exist bufferedImage of the texture.***
                        if(texture.getBufferedImage() == null) {
                            // load the image.***
                            texture.loadImage();
                        }

                        if(texture.getBufferedImage() == null) {
                            int hola = 0;
                        }

                        texture.setParentPath(imagesPath.toString());
                        texture.saveImage(texture.getFullPath());
                    }
                }

                // overwrite the file.***
                //gaiaSet2.writeFileInThePath(path); // old.***
                boolean copyTextures = false;
                gaiaSet2.writeFileForPR(parentPath, copyTextures);

                scene.clear();
                gaiaSet.clear();
                halfEdgeScene.deleteObjects();
                scissorsScene.clear();
                gaiaSet2.clear();
            } catch (IOException e) {
                log.error("Error : ", e);
                throw new RuntimeException(e);
            }
        }
    }

    private boolean cutRectangleCakeOneShoot(List<TileInfo> tileInfos, int lod, Node rootNode, int maxDepth) throws FileNotFoundException {
        // calculate the divisions of the rectangle cake.***
        // int maxDepth = rootNode.findMaxDepth();
        int currDepth = maxDepth - lod;

        // the maxDepth corresponds to lod0.***
        List<Node> nodes = new ArrayList<>();
        rootNode.getNodesByDepth(maxDepth, nodes);
        BoundingVolume boundingVolume = rootNode.getBoundingVolume();
        double minLonDeg = Math.toDegrees(boundingVolume.getRegion()[0]);
        double minLatDeg = Math.toDegrees(boundingVolume.getRegion()[1]);
        double maxLonDeg = Math.toDegrees(boundingVolume.getRegion()[2]);
        double maxLatDeg = Math.toDegrees(boundingVolume.getRegion()[3]);
        double minAlt = boundingVolume.getRegion()[4];
        double maxAlt = boundingVolume.getRegion()[5];

        double divisionsCount = Math.pow(2, currDepth);

        List<Double> lonDivisions = new ArrayList<>();
        List<Double> latDivisions = new ArrayList<>();
        List<Double> altDivisions = new ArrayList<>();

        double lonStep = (maxLonDeg - minLonDeg) / divisionsCount;
        double latStep = (maxLatDeg - minLatDeg) / divisionsCount;
        double altStep = (maxAlt - minAlt) / divisionsCount;

        // exclude the first and last divisions, so i = 1 and i < divisionsCount.***
        // but we must include the 1rst and last divisions to calculate the localBBox.***
        for (int i = 0; i <= divisionsCount; i++) {
            lonDivisions.add(minLonDeg + i * lonStep);
            latDivisions.add(minLatDeg + i * latStep);
            altDivisions.add(minAlt + i * altStep);
        }

        // now, cut the scene by the divisions.***
        boolean someSceneCut = false;

        // load the first scene of the tileInfo.***
        TileInfo tileInfo = tileInfos.get(0);
        Path path = tileInfo.getTempPath();

        GaiaBoundingBox setBBox = tileInfo.getBoundingBox();
        if (setBBox == null) {
            log.error("Error : setBBox is null.");
        }
        KmlInfo kmlInfo = tileInfo.getKmlInfo();
        Vector3d geoCoordPosition = kmlInfo.getPosition();
        Vector3d posWC = GlobeUtils.geographicToCartesianWgs84(geoCoordPosition);
        Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(posWC);
        Matrix4d transformMatrixInv = new Matrix4d(transformMatrix);
        transformMatrixInv.invert();

        boolean checkTexCoord = true;
        boolean checkNormal = false;
        boolean checkColor = false;
        boolean checkBatchId = false;
        double errorWeld = 1e-4;
        Vector3d samplePointLC = new Vector3d();

        // make GaiaAAPlanes.***
        List<GaiaAAPlane> planesYZ = new ArrayList<>();
        List<GaiaAAPlane> planesXZ = new ArrayList<>();
        List<GaiaAAPlane> planesXY = new ArrayList<>();

        Vector3d samplePointGeoCoord;
        Vector3d samplePointWC;
        GaiaBoundingBox localBBox = new GaiaBoundingBox();

        for (int i = 0; i < lonDivisions.size(); i++) {
            double lonDeg = lonDivisions.get(i);
            double latDeg = latDivisions.get(i);
            double altitude = altDivisions.get(i);

            // Longitude plane : create a point with lonDeg, geoCoordPosition.y, 0.0.***
            samplePointGeoCoord = new Vector3d(lonDeg, geoCoordPosition.y, 0.0);
            samplePointWC = GlobeUtils.geographicToCartesianWgs84(samplePointGeoCoord);
            transformMatrixInv.transformPosition(samplePointWC, samplePointLC);

            localBBox.addPoint(samplePointLC);

            // check if the planeLC cuts the setBBox.***
            if (samplePointLC.x > setBBox.getMinX() && samplePointLC.x < setBBox.getMaxX()) {
                if(i > 0 && i < lonDivisions.size() - 1) {
                    GaiaAAPlane planeYZ = new GaiaAAPlane();
                    planeYZ.setPlaneType(PlaneType.YZ);
                    planeYZ.setPoint(new Vector3d(samplePointLC));
                    planesYZ.add(planeYZ);
                }
            }

            // Latitude plane : create a point with geoCoordPosition.x, latDeg, 0.0.***
            samplePointGeoCoord = new Vector3d(geoCoordPosition.x, latDeg, 0.0);
            samplePointWC = GlobeUtils.geographicToCartesianWgs84(samplePointGeoCoord);
            transformMatrixInv.transformPosition(samplePointWC, samplePointLC);

            localBBox.addPoint(samplePointLC);

            // check if the planeLC cuts the setBBox.***
            if (samplePointLC.y > setBBox.getMinY() && samplePointLC.y < setBBox.getMaxY()){
                if(i > 0 && i < latDivisions.size() - 1) {
                    GaiaAAPlane planeXZ = new GaiaAAPlane();
                    planeXZ.setPlaneType(PlaneType.XZ);
                    planeXZ.setPoint(new Vector3d(samplePointLC));
                    planesXZ.add(planeXZ);
                }
            }

            // Altitude plane : create a point with geoCoordPosition.x, geoCoordPosition.y, 0.0.***
            samplePointGeoCoord = new Vector3d(geoCoordPosition.x, geoCoordPosition.y, altitude);
            samplePointWC = GlobeUtils.geographicToCartesianWgs84(samplePointGeoCoord);
            transformMatrixInv.transformPosition(samplePointWC, samplePointLC);

            localBBox.addPoint(samplePointLC);

            // check if the planeLC cuts the setBBox.***
            if (samplePointLC.z > setBBox.getMinZ() && samplePointLC.z < setBBox.getMaxZ()){
                if(i > 0 && i < altDivisions.size() - 1) {
                    GaiaAAPlane planeXY = new GaiaAAPlane();
                    planeXY.setPlaneType(PlaneType.XY);
                    planeXY.setPoint(new Vector3d(samplePointLC));
                    planesXY.add(planeXY);
                }
            }

        }

        List<GaiaAAPlane> allPlanes = new ArrayList<>();
        allPlanes.addAll(planesYZ);
        allPlanes.addAll(planesXZ);
        allPlanes.addAll(planesXY);

        // load the file.***
        GaiaSet gaiaSet = GaiaSet.readFile(path);
        if (gaiaSet == null) return false;

        GaiaScene scene = new GaiaScene(gaiaSet);
        scene.deleteNormals();
        scene.makeTriangleFaces();
        scene.weldVertices(errorWeld, checkTexCoord, checkNormal, checkColor, checkBatchId);
        HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);
        HalfEdgeOctree resultOctree = new HalfEdgeOctree(null);
        resultOctree.setSize(localBBox.getMinX(), localBBox.getMinY(), localBBox.getMinZ(), localBBox.getMaxX(), localBBox.getMaxY(), localBBox.getMaxZ());
        resultOctree.makeTreeByMaxDepth(currDepth);
        log.debug("cutting rectangle cake one shoot. lod : " + lod);
        List<HalfEdgeScene> halfEdgeCutScenes = HalfEdgeCutter.cutHalfEdgeSceneByGaiaAAPlanes(halfEdgeScene, allPlanes, resultOctree);

        // create tileInfos for the cut scenes.***
        String outputPathString = globalOptions.getOutputPath();
        String cutTempPathString = outputPathString + File.separator + "cutTemp";
        Path cutTempPath = Paths.get(cutTempPathString);
        // create directory if not exists.***
        if (!cutTempPath.toFile().exists() && cutTempPath.toFile().mkdirs()) {
            log.debug("cutTemp folder created.");
        }

        Path cutTempLodPath = cutTempPath.resolve("lod" + lod);
        if (!cutTempLodPath.toFile().exists() && cutTempLodPath.toFile().mkdirs()) {
            log.debug("cutTempLod folder created.");
        }

        List<TileInfo> cutTileInfos = new ArrayList<>();

        for (HalfEdgeScene halfEdgeCutScene : halfEdgeCutScenes) {
            GaiaScene gaiaSceneCut = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeCutScene);
            GaiaBoundingBox boundingBoxCutLC = gaiaSceneCut.getBoundingBox();

            // Calculate cartographicBoundingBox.***
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

            GaiaBoundingBox cartographicBoundingBox = new GaiaBoundingBox(minLonDegCut, minLatDegCut, geoCoordLeftDownBottom.z, maxLonDegCut, maxLatDegCut, geoCoordLeftDownUp.z, false);

            // create an originalPath for the cut scene.***
            Path cutScenePath = Paths.get("");
            gaiaSceneCut.setOriginalPath(cutScenePath);

            GaiaSet gaiaSetCut = GaiaSet.fromGaiaScene(gaiaSceneCut);
            UUID identifier = UUID.randomUUID();
            Path gaiaSetCutFolderPath = cutTempLodPath.resolve(identifier.toString());
            if (!gaiaSetCutFolderPath.toFile().exists() && gaiaSetCutFolderPath.toFile().mkdirs()) {
                log.debug("gaiaSetCut folder created.");
            }
            boolean copyTexturesToNewPath = false;
            Path tempPathLod = gaiaSetCut.writeFileForPR(gaiaSetCutFolderPath, copyTexturesToNewPath);

            // delete the contents of the gaiaSceneCut.************************************************
            gaiaSceneCut.getNodes().forEach(GaiaNode::clear);
            // end delete the contents of the gaiaSceneCut.--------------------------------------------

            // create a new tileInfo for the cut scene.***
            TileInfo tileInfoCut = TileInfo.builder().scene(gaiaSceneCut).outputPath(tileInfo.getOutputPath()).build();
            tileInfoCut.setTempPath(tempPathLod);
            Matrix4d transformMatrixCut = new Matrix4d(tileInfo.getTransformMatrix());
            tileInfoCut.setTransformMatrix(transformMatrixCut);
            tileInfoCut.setBoundingBox(boundingBoxCutLC);
            tileInfoCut.setCartographicBBox(cartographicBoundingBox);


            // make a kmlInfo for the cut scene.***
            // In reality, we must recalculate the position of the cut scene. Provisionally, we use the same position.***
            // In reality, we must recalculate the position of the cut scene. Provisionally, we use the same position.***
            KmlInfo kmlInfoCut = KmlInfo.builder().position(geoCoordPosition).build();
            tileInfoCut.setKmlInfo(kmlInfoCut);
            cutTileInfos.add(tileInfoCut);

            halfEdgeCutScene.deleteObjects();
        }

        tileInfos.clear();
        tileInfos.addAll(cutTileInfos);
        someSceneCut = true;

        int hola = 0;

//        // cut by longitudes.***
//        for (double lonDeg : lonDivisions) {
//            if (cutRectangleCakeByLongitudeDeg(tileInfos, lod, lonDeg)) {
//                someSceneCut = true;
//                //System.gc();
//            }
//        }
//
//        // cut by altitudes.***
//        for (double alt : altDivisions) {
//            if (cutRectangleCakeByAltitude(tileInfos, lod, alt)) {
//                someSceneCut = true;
//                //System.gc();
//            }
//        }
//
//        // cut by latitudes.***
//        for (double latDeg : latDivisions) {
//            if (cutRectangleCakeByLatitudeDeg(tileInfos, lod, latDeg)) {
//                someSceneCut = true;
//                //System.gc();
//            }
//        }

        System.gc();
        return someSceneCut;
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
            log.error("Error : ", e);
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
            log.warn("[Tile] Node depth limit exceeded : {}", nodeDepth);
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
            // Note : in each node, has NodeCode.***

            List<TileInfo> tileInfos = entry.getValue();
            for (TileInfo tileInfo : tileInfos) {
                GaiaScene scene = tileInfo.getScene();
                if (scene == null) {
                    log.error("makeContentsForNodes() Error : scene is null.");
                }
            }

            GaiaBoundingBox childBoundingBox = calcBoundingBox(tileInfos); // cartographicBBox.***
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
            childNode.setBoundingVolume(boundingVolume);
            childNode.setGeometricError(lodError + 0.1);

            childNode.setRefine(Node.RefineType.REPLACE);
            if (!tileInfos.isEmpty()) {
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
                childNode.setContent(content);
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
        List<TileInfo> remainInfos; // small buildings, to add after as ADD.***
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

        childNode.setRefine(refineAdd ? Node.RefineType.ADD : Node.RefineType.REPLACE);
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
