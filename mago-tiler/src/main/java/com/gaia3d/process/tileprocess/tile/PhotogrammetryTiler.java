package com.gaia3d.process.tileprocess.tile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.exception.TileProcessingException;
import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.exchangable.SceneInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.entities.GaiaAAPlane;
import com.gaia3d.basic.geometry.modifier.halfedge.HalfEdgeDecimator;
import com.gaia3d.basic.geometry.modifier.topology.GaiaTriangulator;
import com.gaia3d.basic.geometry.modifier.topology.GaiaWeldOptions;
import com.gaia3d.basic.geometry.modifier.topology.GaiaWelder;
import com.gaia3d.basic.geometry.octree.GaiaOctree;
import com.gaia3d.basic.geometry.octree.HalfEdgeOctreeFaces;
import com.gaia3d.basic.halfedge.*;
import com.gaia3d.basic.magogl.MagoDepthGridScaler;
import com.gaia3d.basic.magogl.MagoFbo;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.remesher.CellGrid3D;
import com.gaia3d.basic.remesher.GlobalBoundaryAnchors;
import com.gaia3d.basic.remesher.ReMeshParameters;
import com.gaia3d.basic.types.LevelOfDetail;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.command.mago.GlobalConstants;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.process.tileprocess.PhotogrammetryBatcher;
import com.gaia3d.process.tileprocess.Tiler;
import com.gaia3d.process.tileprocess.multithread.CutAndScissorMT;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;
import com.gaia3d.process.tileprocess.tile.tileset.TilesetV2;
import com.gaia3d.process.tileprocess.tile.tileset.asset.AssetV1;
import com.gaia3d.process.tileprocess.tile.tileset.asset.AssetV2;
import com.gaia3d.process.tileprocess.tile.tileset.node.BoundingVolume;
import com.gaia3d.process.tileprocess.tile.tileset.node.Content;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import com.gaia3d.render.MagoLeafTileManager;
import com.gaia3d.render.MagoReTextureByObliqueCamera;
import com.gaia3d.util.DecimalUtils;
import com.gaia3d.util.GlobeUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.Vector4d;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor
public class PhotogrammetryTiler extends DefaultTiler implements Tiler {
    public final GlobalOptions globalOptions = GlobalOptions.getInstance();
    public int projectMaxDepthIdx = -1;
    public Map<Integer, List<TileInfo>> mapLodToTileInfos = new HashMap<>();

    protected static double depthToZ01(
            float depth,
            double minZ,
            double maxZ,
            boolean depthInverted
    ) {
        double clampedDepth =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                depth
                        )
                );

        double normalizedHeight =
                depthInverted
                        ? 1.0 - clampedDepth
                        : clampedDepth;

        return minZ
                + (maxZ - minZ)
                * normalizedHeight;
    }

    @Override
    public Tileset run(List<TileInfo> tileInfos) throws FileNotFoundException {
        return runModeMagoGL(tileInfos);
    }

    protected void addTileInfoInLod(int lod, TileInfo tileInfo) {
        // check mapLodToTileInfos
        if (!mapLodToTileInfos.containsKey(lod)) {
            mapLodToTileInfos.put(lod, new ArrayList<>());
        }

        mapLodToTileInfos.get(lod).add(tileInfo);
    }

    public boolean integralReMeshScenesMagoGLST(List<TileInfo> tileInfos,
                                                int lod,
                                                int nodeDepth,
                                                Node rootNode,
                                                int maxDepth,
                                                DecimateParameters decimateParameters,
                                                double pixelsForMeter,
                                                double screenPixelsForMeter,
                                                ReMeshParameters reMeshParams) {
        // 1rst, find all tileInfos that intersects with the node
        log.info("Creating reMesh nodes for nodeDepth : " + nodeDepth + " of maxDepth : " + maxDepth);
        List<Node> nodes = new ArrayList<>();
        List<Node> intersectedNodes = new ArrayList<>();

        List<TileInfo> tileInfosOfNode = new ArrayList<>();
        boolean makeVerticalSkirt = true;
        boolean cellSizeGreaterThanTileInfosBBox = false;

        // ReMeshParameters
        GaiaBoundingBox rootNodeBBoxLC = rootNode.calculateLocalBoundingBox();
        Matrix4d rootTransformMatrix = getNodeTransformMatrix(rootNode);
        Matrix4d rootTransformMatrixInverse = new Matrix4d(rootTransformMatrix);
        rootTransformMatrixInverse.invert();

        Map<Node, List<TileInfo>> nodeTileInfosMap = new HashMap<>();
        for (TileInfo tileInfo : tileInfos) {
            GaiaBoundingBox cartographicBBox = tileInfo.getCartographicBBox();
            if (cartographicBBox == null) {
                log.error("[ERROR] cartographicBBox is null.");
                continue;
            }

            intersectedNodes.clear();
            //*************************************************************************************************************************************
            // in integral-reMesh, the intersection between node and tileInfo must be between node and the cartographicCenterDegree of tileInfo
            Vector3d cartographicCenterDegree = cartographicBBox.getCenter();
            rootNode.getIntersectedNodesAsOctree(cartographicCenterDegree, nodeDepth, intersectedNodes);
            //*************************************************************************************************************************************

            int intersectedNodesCount = intersectedNodes.size();
            for (int i = 0; i < intersectedNodesCount; i++) {
                Node node = intersectedNodes.get(i);
                if (node.getDepth() != nodeDepth) {
                    continue;
                }
                List<TileInfo> tileInfosOfNodeList = nodeTileInfosMap.computeIfAbsent(node, k -> new ArrayList<>());
                tileInfosOfNodeList.add(tileInfo);
            }
        }

        // Re mesh by vertex clustering************************************************************************
        double maxSize = rootNodeBBoxLC.getMaxSize();
        // the maxSize is for rootNode that has maxDepth
        // so, the maxSize rof lod is maxSize / Math.pow(2, maxDepth - lod);
        if (lod > 0) {
            maxSize = maxSize / Math.pow(2, maxDepth - lod);
        }

        // ifc round bridge settings
        double texturePixelSize = maxSize / 512.0;
        double texturePixelsForMeter = 1.0 / texturePixelSize;

        CellGrid3D cellGrid = createReMeshCellGridForLod(lod, maxDepth, rootNodeBBoxLC, tileInfos);
        reMeshParams.setTexturePixelsForMeter(texturePixelsForMeter);
        reMeshParams.setCellGrid(cellGrid);
        // End reMeshParameters

        nodes = new ArrayList<>(nodeTileInfosMap.keySet());

        int nodesCount = nodes.size();
        for (int i = 0; i < nodesCount; i++) {
            tileInfosOfNode.clear();
            Node node = nodes.get(i);

            tileInfosOfNode = nodeTileInfosMap.get(node);

            int tileInfosOfNodeCount = tileInfosOfNode.size();
            if (tileInfosOfNodeCount == 0) {
                continue;
            }

            node.setRefine(Node.RefineType.REPLACE);

            // create sceneInfos
            List<SceneInfo> sceneInfos = new ArrayList<>();
            for (int j = 0; j < tileInfosOfNodeCount; j++) {
                TileInfo tileInfo = tileInfosOfNode.get(j);
                SceneInfo sceneInfo = new SceneInfo();
                sceneInfo.setScenePath(tileInfo.getTempPath().toString());
                TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
                Vector3d geoCoordPosition = tileTransformInfo.getPosition();
                Vector3d posWC = GlobeUtils.geographicToCartesianWgs84(geoCoordPosition);
                Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(posWC);
                sceneInfo.setTransformMatrix(transformMatrix);

                // for remeshParams***************************************************************************************
                Vector3d geoCoordCenter = tileInfo.getTileTransformInfo().getPosition(); // use TileTransformInfo position instead of KmlInfo position
                Vector3d scenePosWC = GlobeUtils.geographicToCartesianWgs84(geoCoordCenter);
                Vector4d scenePosLC4d = new Vector4d(scenePosWC.x, scenePosWC.y, scenePosWC.z, 1.0);
                scenePosLC4d = rootTransformMatrixInverse.transform(scenePosLC4d);
                Vector3d scenePosLC = new Vector3d(scenePosLC4d.x, scenePosLC4d.y, scenePosLC4d.z);

                sceneInfo.setScenePosLC(scenePosLC);

                sceneInfos.add(sceneInfo);
            }

            if (sceneInfos.isEmpty()) {
                log.error("[ERROR] Error : sceneInfos is empty.");
                continue;
            }

            Vector3d nodeCenterGeoCoordRad = node.getBoundingVolume().calcCenter();
            Vector3d nodeCenterGeoCoordDeg = new Vector3d(Math.toDegrees(nodeCenterGeoCoordRad.x), Math.toDegrees(nodeCenterGeoCoordRad.y), nodeCenterGeoCoordRad.z);
            Vector3d nodePosWC = GlobeUtils.geographicToCartesianWgs84(nodeCenterGeoCoordDeg);
            Matrix4d nodeTMatrix = node.getTransformMatrix();
            if (nodeTMatrix == null) {
                nodeTMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(nodePosWC);
            }
            GaiaBoundingBox nodeBBoxLC = node.calculateLocalBoundingBox();

            log.debug("nodeCode : " + node.getNodeCode() + " currNodeIdx : " + i + " / " + nodesCount);
            int maxScreenSize = 512;

            List<HalfEdgeScene> resultHalfEdgeScenes = new ArrayList<>();
            String outputPathString = globalOptions.getOutputPath();
            String tempPath = globalOptions.getTempPath();
            String nodeName = "node_L_" + nodeDepth + "_" + i;

            MagoReTextureByObliqueCamera magoReTextureByObliqueCamera = new MagoReTextureByObliqueCamera();
            magoReTextureByObliqueCamera.integralReMeshByObliqueCameraV2(sceneInfos,
                    resultHalfEdgeScenes,
                    reMeshParams,
                    nodeBBoxLC,
                    nodeTMatrix,
                    maxScreenSize,
                    outputPathString,
                    nodeName,
                    lod,
                    node);
            //************************************************************************************************************************************************
            if (resultHalfEdgeScenes.isEmpty()) {
                log.info("IntegralReMesh resultHalfEdgeScenes is empty.");
                continue;
            }

            HalfEdgeScene halfEdgeScene = resultHalfEdgeScenes.get(0);
            GaiaScene gaiaScene = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);
            makeContentsForNode(node, gaiaScene, lod, nodeDepth, i);
        }

        return cellSizeGreaterThanTileInfosBBox;
    }


    protected void createNetSurfaceNodesMagoGL(Node rootNode,
                                               List<TileInfo> tileInfos,
                                               int nodeDepth,
                                               int maxDepth,
                                               DecimateParameters decimateParameters,
                                               ReMeshParameters reMeshParams) {
        // 1rst, find all tileInfos that intersects with the node
        log.info("Creating netSurface nodes for nodeDepth : " + nodeDepth + " of maxDepth : " + maxDepth);
        List<Node> nodes = new ArrayList<>();
        List<Node> intersectedNodes = new ArrayList<>();

        List<TileInfo> tileInfosOfNode = new ArrayList<>();
        //TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();
        boolean makeVerticalSkirt = true;
        if(reMeshParams == null){
            log.error("[ERROR] reMeshParams is null.");
            return;
        }
        GlobalBoundaryAnchors  globalBoundaryAnchors = reMeshParams.getGlobalBoundaryAnchors();

        Map<Node, List<TileInfo>> nodeTileInfosMap = new HashMap<>();
        for (TileInfo tileInfo : tileInfos) {
            GaiaBoundingBox cartographicBBox = tileInfo.getCartographicBBox();
            if (cartographicBBox == null) {
                log.error("[ERROR] cartographicBBox is null.");
                continue;
            }

            intersectedNodes.clear();
            rootNode.getIntersectedNodesAsOctree(cartographicBBox, nodeDepth, intersectedNodes);

            int intersectedNodesCount = intersectedNodes.size();
            for (int i = 0; i < intersectedNodesCount; i++) {
                Node node = intersectedNodes.get(i);
                if (node.getDepth() != nodeDepth) {
                    continue;
                }
                List<TileInfo> tileInfosOfNodeList = nodeTileInfosMap.computeIfAbsent(node, k -> new ArrayList<>());
                tileInfosOfNodeList.add(tileInfo);
            }
        }

        nodes = new ArrayList<>(nodeTileInfosMap.keySet());

        MagoReTextureByObliqueCamera reTexturer = new MagoReTextureByObliqueCamera();

        int nodesCount = nodes.size();
        for (int i = 0; i < nodesCount; i++) {
            tileInfosOfNode.clear();
            Node node = nodes.get(i);
            tileInfosOfNode = nodeTileInfosMap.get(node);

            int tileInfosOfNodeCount = tileInfosOfNode.size();
            if (tileInfosOfNodeCount == 0) {
                continue;
            }

            node.setRefine(Node.RefineType.REPLACE);

            // create sceneInfos
            List<SceneInfo> sceneInfos = new ArrayList<>();
            for (int j = 0; j < tileInfosOfNodeCount; j++) {
                TileInfo tileInfo = tileInfosOfNode.get(j);
                SceneInfo sceneInfo = new SceneInfo();
                sceneInfo.setScenePath(tileInfo.getTempPath().toString());
                TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
                Vector3d geoCoordPosition = tileTransformInfo.getPosition();
                Vector3d posWC = GlobeUtils.geographicToCartesianWgs84(geoCoordPosition);
                Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(posWC);
                sceneInfo.setTransformMatrix(transformMatrix);
                sceneInfos.add(sceneInfo);
            }

            if (sceneInfos.isEmpty()) {
                log.error("[ERROR] Error : sceneInfos is empty.");
                continue;
            }

            // render the sceneInfos and obtain the color and depth images
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

            MagoFbo magoFbo = reTexturer.renderTopView(sceneInfos,
                    nodeBBoxLC,
                    nodeTMatrix,
                    maxScreenSize,
                    maxDepthScreenSize);

            BufferedImage bufferedImageColor = magoFbo.getBufferedImage();

            // now, make a halfEdgeScene from the bufferedImages
            String tempPathString = globalOptions.getTempPath();
            String netTempPathString = Path.of(tempPathString, "netTemp").toString();
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
                log.error("[ERROR] : ", e);
            }

            // Regular-net-mesh.************************************************************
            float[][] depthValuesRaw = magoFbo.getDepthGridFlippedY();
            System.out.println("Verifying depth grid RAW...");
            int numCols = 150;
            int numRows = 150;
            float[][] depthValues = MagoDepthGridScaler.resizeDepthNearestVerified(
                    depthValuesRaw,
                    numCols,
                    numRows
            );

            // control frontier points.
            CellGrid3D cellGrid = reMeshParams.getCellGrid(); // o desde donde tengas tus ReMeshParameters
            applyBoundaryAnchorsToDepthValues(
                    depthValues,
                    nodeBBoxLC,
                    globalBoundaryAnchors,
                    cellGrid,
                    3,
                    false
            );

            System.out.println("Verifying depth grid...");

            HalfEdgeScene halfEdgeScene = HalfEdgeUtils.getHalfEdgeSceneRectangularNet(numCols, numRows, depthValues, nodeBBoxLC, makeVerticalSkirt);
            if (halfEdgeScene == null) {
                log.info("info : halfEdgeScene is null.");
                continue;
            }
            // End creating regularNetMesh.-----------------------------------------------------

            HalfEdgeDecimator decimator = new HalfEdgeDecimator(decimateParameters);
            decimator.apply(halfEdgeScene);
            //halfEdgeScene.decimate(decimateParameters); // new

            if (halfEdgeScene.getTrianglesCount() == 0) {
                continue;
            }

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

            int lod = maxDepth - nodeDepth;
            makeContentsForNode(node, gaiaScene, lod, nodeDepth, i);

            // delete scenes
            halfEdgeScene.deleteObjects();
            gaiaScene.clear();
        }
    }

    protected void applyBoundaryAnchorsToDepthValues(
            float[][] depthValues,
            GaiaBoundingBox nodeBBoxLC,
            GlobalBoundaryAnchors globalBoundaryAnchors,
            CellGrid3D cellGrid,
            int bandPixels,
            boolean depthInverted
    ) {
        if (depthValues == null || depthValues.length == 0) {return;}
        if (depthValues[0] == null || depthValues[0].length == 0) {return;}
        if (nodeBBoxLC == null) {return;}
        if (globalBoundaryAnchors == null) {return;}
        if (cellGrid == null) {return;}
        if (globalBoundaryAnchors.lockedAveragePositions.isEmpty()) {return;}
        if (bandPixels <= 0) {return;}

        float DEPTH_EPSILON =
                1.0e-6f;

        // IMPORTANT:
        // depthValues viene de bufferedImageToFloatMatrix()
        // y tiene formato [x][y] = [col][row].
        int numCols = depthValues.length;       // width
        int numRows = depthValues[0].length;    // height

        double minX = nodeBBoxLC.getMinX();
        double maxX = nodeBBoxLC.getMaxX();
        double minY = nodeBBoxLC.getMinY();
        double maxY = nodeBBoxLC.getMaxY();
        double minZ = nodeBBoxLC.getMinZ();
        double maxZ = nodeBBoxLC.getMaxZ();

        double sizeX = maxX - minX;
        double sizeY = maxY - minY;
        double sizeZ = maxZ - minZ;

        if (Math.abs(sizeX) < 1e-12) {return;}
        if (Math.abs(sizeY) < 1e-12) {return;}
        if (Math.abs(sizeZ) < 1e-12) {return;}

        Map<Long, List<Vector3d>> anchorsByXYCell =
                buildAnchorsByXYCell(globalBoundaryAnchors);

        double probeZ = (minZ + maxZ) * 0.5;

        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {

                int distToBorder = Math.min(
                        Math.min(col, numCols - 1 - col),
                        Math.min(row, numRows - 1 - row)
                );

                if (distToBorder >= bandPixels) {
                    continue;
                }

                double u = (col + 0.5) / (double) numCols;
                double v = (row + 0.5) / (double) numRows;

                double x = minX + u * sizeX;

                // BufferedImage: row = 0 arriba.
                // Si ves que se aplica al borde contrario en Y, cambia a:
                // double y = minY + v * sizeY;
                double y = maxY - v * sizeY;

                Vector3i probeCell = new Vector3i(
                        cellGrid.getCellIndex(new Vector3d(x, y, probeZ))
                );

                Vector3d anchor = findNearestAnchorByXYCell(
                        anchorsByXYCell,
                        probeCell.x,
                        probeCell.y,
                        x,
                        y,
                        1
                );

                if (anchor == null) {
                    continue;
                }

                float oldDepth =
                        depthValues[col][row];

                /*
                 * No aplicamos anchors sobre píxeles sin geometría.
                 * El clear depth es 1.0.
                 */
                if (!Float.isFinite(oldDepth)
                        || oldDepth >= 1.0f - DEPTH_EPSILON) {

                    continue;
                }

                /*
                 * Reconstruimos la altura actual representada por
                 * el depth del píxel.
                 */
                double oldZ =
                        depthToZ01(
                                oldDepth,
                                minZ,
                                maxZ,
                                depthInverted
                        );

                double dx =
                        anchor.x - x;

                double dy =
                        anchor.y - y;

                double distXY =
                        Math.sqrt(
                                dx * dx + dy * dy
                        );

                double pixelSizeX =
                        sizeX / numCols;

                double pixelSizeY =
                        sizeY / numRows;

                double pixelSize =
                        Math.max(
                                pixelSizeX,
                                pixelSizeY
                        );

                double maxAnchorDistanceXY =
                        pixelSize * 6.0;

                if (distXY > maxAnchorDistanceXY) {
                    continue;
                }

                /*
                 * Este filtro es fundamental:
                 * evita seleccionar un anchor de tejado para
                 * un píxel perteneciente al terreno o a una fachada baja.
                 */
                double deltaZ =
                        Math.abs(
                                anchor.z - oldZ
                        );

                double maxAnchorDeltaZ =
                        Math.max(
                                0.5,
                                sizeZ * 0.03
                        );

                if (deltaZ > maxAnchorDeltaZ) {
                    continue;
                }

                /*
                 * Solo tolerancia numérica.
                 * El margen anterior del 10 % permitía anchors
                 * claramente fuera del bbox.
                 */
                double zTolerance =
                        Math.max(
                                1.0e-6,
                                sizeZ * 1.0e-6
                        );

                if (anchor.z < minZ - zTolerance
                        || anchor.z > maxZ + zTolerance) {

                    continue;
                }

                double safeAnchorZ =
                        Math.max(
                                minZ,
                                Math.min(
                                        maxZ,
                                        anchor.z
                                )
                        );

                float anchorDepth =
                        zToDepth01(
                                safeAnchorZ,
                                minZ,
                                maxZ,
                                depthInverted
                        );

                if (!Float.isFinite(anchorDepth)) {
                    continue;
                }

                /*
                 * Un depth exactamente 0 o 1 puede colisionar con
                 * los extremos del volumen y con el clear depth.
                 */
                anchorDepth =
                        Math.max(
                                DEPTH_EPSILON,
                                Math.min(
                                        1.0f - DEPTH_EPSILON,
                                        anchorDepth
                                )
                        );

                double weight =
                        1.0
                                - distToBorder
                                / (double) bandPixels;

                weight =
                        Math.max(
                                0.0,
                                Math.min(
                                        1.0,
                                        weight
                                )
                        );

                /*
                 * Smoothstep.
                 */
                weight =
                        weight
                                * weight
                                * (3.0 - 2.0 * weight);

                float newDepth =
                        (float) (
                                oldDepth * (1.0 - weight)
                                        + anchorDepth * weight
                        );

                depthValues[col][row] =
                        newDepth;
            }
        }
    }

    protected float zToDepth01(double z, double minZ, double maxZ, boolean depthInverted) {
        double sizeZ = maxZ - minZ;

        if (Math.abs(sizeZ) < 1e-12) {
            return 0.0f;
        }

        double t = (z - minZ) / sizeZ;
        t = Math.max(0.0, Math.min(1.0, t));

        if (depthInverted) {
            t = 1.0 - t;
        }

        return (float) t;
    }

    protected Map<Long, List<Vector3d>> buildAnchorsByXYCell(
            GlobalBoundaryAnchors globalBoundaryAnchors
    ) {
        Map<Long, List<Vector3d>> result = new HashMap<>();

        if (globalBoundaryAnchors == null ||
                globalBoundaryAnchors.lockedAveragePositions == null) {
            return result;
        }

        for (Map.Entry<Vector3i, Vector3d> entry :
                globalBoundaryAnchors.lockedAveragePositions.entrySet()) {

            Vector3i cell = entry.getKey();
            Vector3d anchor = entry.getValue();

            if (cell == null || anchor == null) {
                continue;
            }

            long key = packXY(cell.x, cell.y);

            result.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(new Vector3d(anchor));
        }

        return result;
    }

    protected Vector3d findNearestAnchorByXYCell(
            Map<Long, List<Vector3d>> anchorsByXYCell,
            int ix,
            int iy,
            double x,
            double y,
            int neighborRadius
    ) {
        if (anchorsByXYCell == null || anchorsByXYCell.isEmpty()) {
            return null;
        }

        Vector3d best = null;
        double bestDist2 = Double.POSITIVE_INFINITY;

        int r = Math.max(0, neighborRadius);

        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {

                long key = packXY(ix + dx, iy + dy);
                List<Vector3d> anchors = anchorsByXYCell.get(key);

                if (anchors == null || anchors.isEmpty()) {
                    continue;
                }

                for (Vector3d anchor : anchors) {
                    if (anchor == null) {continue;}

                    double ddx = anchor.x - x;
                    double ddy = anchor.y - y;
                    double dist2 = ddx * ddx + ddy * ddy;

                    if (dist2 < bestDist2) {
                        bestDist2 = dist2;
                        best = anchor;
                    }
                }
            }
        }

        return best == null ? null : new Vector3d(best);
    }

    protected long packXY(int x, int y) {
        return (((long) x) << 32) ^ (y & 0xffffffffL);
    }

    protected CellGrid3D createReMeshCellGridForLod(
            int lod,
            int maxDepth,
            GaiaBoundingBox rootNodeBBoxLC,
            List<TileInfo> tileInfos) {

        double maxSize = rootNodeBBoxLC.getMaxSize();

        if (lod > 0) {
            maxSize = maxSize / Math.pow(2, maxDepth - lod);
        }

        double cellSize = maxSize / 26.0;

        double averageBBoxMinSize = calculateAverageBBoxMinSize(tileInfos);

        double factor = 0.9;
        if (cellSize > averageBBoxMinSize * factor) {
            cellSize = averageBBoxMinSize * factor;
        }

        Vector3d cellGridOrigin = new Vector3d(0.0, 0.0, 0.0);
        return new CellGrid3D(cellGridOrigin, cellSize);
    }

    public Tileset runModeMagoGL(List<TileInfo> tileInfos) throws FileNotFoundException {
        // In photoRealistic, 1rst make an empty octTree.              *
        // then use rectangleCakeCutter to fill the octTree.           *
        if (tileInfos.isEmpty()) {
            throw new TileProcessingException("Error : tileInfos is empty.");
        }
        GaiaBoundingBox globalBoundingBox = calcCartographicBoundingBox(tileInfos);

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

        double desiredLeafDist = GlobalConstants.REALISTIC_LEAF_TILE_SIZE;
        log.debug("DesiredLeafDist : " + desiredLeafDist);

        projectMaxDepthIdx = (int) Math.ceil(HalfEdgeUtils.log2(distanceFinal / desiredLeafDist));
        double desiredDistanceBetweenLat = desiredLeafDist * Math.pow(2, projectMaxDepthIdx);
        double desiredAngRadLat = GlobeUtils.angRadLatitudeForDistance(minLatRad, desiredDistanceBetweenLat);
        double desiredAngRadLon = GlobeUtils.angRadLongitudeForDistance(minLatRad, desiredDistanceBetweenLat);
        double desiredAngDegLat = Math.toDegrees(desiredAngRadLat);
        double desiredAngDegLon = Math.toDegrees(desiredAngRadLon);
        maxLonDeg = minLonDeg + desiredAngDegLon;
        maxLatDeg = minLatDeg + desiredAngDegLat;
        // end calculates the rootOctTree size.---

        // make CUBE boundingBox
        globalBoundingBox.setMaxZ(globalBoundingBox.getMinZ() + desiredDistanceBetweenLat);// make CUBE boundingBox
        globalBoundingBox = new GaiaBoundingBox(minLonDeg, minLatDeg, globalBoundingBox.getMinZ(), maxLonDeg, maxLatDeg, globalBoundingBox.getMaxZ(), false);

        Matrix4d transformMatrix = getTransformMatrixFromCartographic(globalBoundingBox);
        if (globalOptions.isClassicTransformMatrix()) {
            rotateX90(transformMatrix);
        }

        Node root = createRoot();
        root.setNodeCode("R");
        root.setDepth(0);
        root.setBoundingVolume(new BoundingVolume(globalBoundingBox, BoundingVolume.BoundingVolumeType.REGION));
        root.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());

        /* Start lod 0 processes */
        int lod = 0;
        List<TileInfo> tileInfosCopy = this.getTileInfosCopy(tileInfos, lod, null);

        // current depth.
        GaiaBoundingBox rootNodeBBoxLC = root.calculateLocalBoundingBox();
        int currDepth = projectMaxDepthIdx - lod;
        Map<Node, List<TileInfo>> nodeTileInfoMap = new HashMap<>();
        CutAndScissorMT cutAndScissorMT = new CutAndScissorMT(3);
        CutAndScissorMT.CutAndScissorResult cutScissorResult = cutAndScissorMT.apply(tileInfosCopy, root, projectMaxDepthIdx, rootNodeBBoxLC);
        mapLodToTileInfos = cutScissorResult.tileInfosByLod();
        Map<Integer, CutAndScissorMT.LodBoundaryAnchors> boundaryAnchorsByLod = cutScissorResult.boundaryAnchorsByLod();
        List<TileInfo> cuttedTileInfos = mapLodToTileInfos.get(lod);
        integralLeafScenesMT(cuttedTileInfos, lod, currDepth, root, projectMaxDepthIdx, 4); // 4 threads
        //integralLeafScenesST(cuttedTileInfos, lod, currDepth, root, projectMaxDepthIdx);
        /* End lod 0 processes */

        DecimateParameters decimateParameters = new DecimateParameters();
        ReMeshParameters reMeshParamsLod2Lod3 = new ReMeshParameters();
        for (int d = 1; d <= projectMaxDepthIdx; d++) {
            lod = d;
            tileInfosCopy.clear();
            nodeTileInfoMap.clear();
            tileInfosCopy = this.getTileInfosCopy(tileInfos, lod, tileInfosCopy);
            double screenPixelsForMeterLod1 = 18.0;
            double screenPixelsForMeter = 0.0;

            if (d == 0) {
                decimateParameters.setBasicValues(2.0, 0.02, 0.1, 36.0, 1000000, 1, 0.01);
                screenPixelsForMeter = screenPixelsForMeterLod1;
            } else if (d == 1) {
                decimateParameters.setBasicValues(10.0, 0.001, 0.9, 40.0, 1000000, 5, 0.02);
                screenPixelsForMeter = screenPixelsForMeterLod1;
            } else if (d == 2) {
                decimateParameters.setBasicValues(12.0, 0.001, 0.9, 40.0, 1000000, 5, 0.02);
                screenPixelsForMeter = screenPixelsForMeterLod1 / 2.0;
            } else {
                decimateParameters.setBasicValues(10.0, 0.008, 1.0, 36.0, 1000000, 5, 0.02);
                screenPixelsForMeter = screenPixelsForMeterLod1 / 4.0;
            }

            // decimate and cut scenes
            decimateParameters.setLod(d);
            currDepth = projectMaxDepthIdx - lod;
            cuttedTileInfos.clear();
            cuttedTileInfos = mapLodToTileInfos.get(lod);
            if (integralDecimateScenesMagoGLST(cuttedTileInfos, lod, currDepth, root, projectMaxDepthIdx, decimateParameters, reMeshParamsLod2Lod3, screenPixelsForMeter)) {
                break;
            }


            if (d >= 2) {
                break;
            }
        }

        // net surfaces with boxTextures
        // start with L =3 .***
        //TileBoundaryAnchors anchorsOfReMesh = new TileBoundaryAnchors();
        ReMeshParameters reMeshParams = null;
        ReMeshParameters reMeshParamsLod7 = null;
        for (int d = 3; d <= projectMaxDepthIdx; d++) { // test d=1
            lod = d;
            currDepth = projectMaxDepthIdx - lod;
            double boxSizeForCurrDepth = desiredDistanceBetweenLat / Math.pow(2, (currDepth + 1));
            double pixelsForMeter = 80.0 / boxSizeForCurrDepth;
            tileInfosCopy.clear();
            nodeTileInfoMap.clear();
            tileInfosCopy = this.getTileInfosCopy(tileInfos, lod, tileInfosCopy);
            double screenPixelsForMeterLod1 = 22.0;
            double screenPixelsForMeter = 0.0;
            // public void setBasicValues(double maxDiffAngDegrees, double hedgeMinLength, double frontierMaxDiffAngDeg, double maxAspectRatio, int maxCollapsesCount)
            decimateParameters.setBasicValues(10.0, 0.5, 1.0, 6.0, 1000000, 1, 0.02);
            decimateParameters.setLod(d);
            if (d <= 3) {
                decimateParameters.setBasicValues(10.0, 0.001, 1.0, 15.0, 1000000, 1, 0.02);
                screenPixelsForMeter = screenPixelsForMeterLod1 / 2.0;
            } else if (d == 4) {
                decimateParameters.setBasicValues(12.0, 0.001, 1.0, 15.0, 1000000, 1, 0.02);
                screenPixelsForMeter = screenPixelsForMeterLod1 / 3.0;
            } else if (d == 5) {
                decimateParameters.setBasicValues(13.0, 0.001, 1.0, 15.0, 1000000, 1, 0.02);
                screenPixelsForMeter = screenPixelsForMeterLod1 / 8.0;
            } else if (d >= 6) {
                decimateParameters.setBasicValues(14.0, 0.001, 1.0, 15.0, 1000000, 1, 0.02);
                screenPixelsForMeter = screenPixelsForMeterLod1 / 16.0;
            }

//            if (d == 3) {
//                reMeshParams = reMeshParamsLod2Lod3;
//            } else {
//                reMeshParams = new ReMeshParameters();
//            }

            reMeshParams = new ReMeshParameters();

            configureBoundaryAnchors(
                    reMeshParams,
                    d,
                    boundaryAnchorsByLod
            );

            // make netSurfaces and decimate and cut scenes
            currDepth = projectMaxDepthIdx - lod;

            cuttedTileInfos.clear();
            cuttedTileInfos = mapLodToTileInfos.get(lod);
            if (integralReMeshScenesMagoGLST(cuttedTileInfos, lod, currDepth, root, projectMaxDepthIdx, decimateParameters, pixelsForMeter, screenPixelsForMeter, reMeshParams)) {
                break;
            }

            if (d == 7) {
                reMeshParamsLod7 = reMeshParams;
            }

            if (d >= 7) {
                break;
            }
        }

        // Check if is necessary netSurfaces nodes
        double maxDiffAngDeg = 20.0;
        double hedgeMinLength = 1.5;
        hedgeMinLength = 0.1;
        double frontierMaxDiffAngDeg = 1.0;
        double maxAspectRatio = 15.0;
        decimateParameters.setBasicValues(maxDiffAngDeg, hedgeMinLength, frontierMaxDiffAngDeg, maxAspectRatio, 1000000, 20, 1.8);
        //lod = 8;
        lod++;
        for (int depth = projectMaxDepthIdx - lod; depth >= 0; depth--) {
            tileInfosCopy.clear();
            tileInfosCopy = this.getTileInfosCopy(tileInfos, 0, tileInfosCopy);
            createNetSurfaceNodesMagoGL(root, tileInfosCopy, depth, projectMaxDepthIdx, decimateParameters, reMeshParamsLod7); // last reMeshParams
        }

        // now, delete nodes that have no contents
        root.deleteNoContentNodes();
        setGeometryErrorToNodeManual(root, projectMaxDepthIdx, desiredLeafDist);

        // once tileset is created, we fit the region of the root node to the bounding box of the root node's contents
        BoundingVolume rootBoundingVolume = null;
        for (Node child : root.getChildren()) {
            // check if the child has content
            if (child.getContent() != null) {
                BoundingVolume childBoundingVolume = child.getBoundingVolume();
                double[] childRegion = childBoundingVolume.getRegion();
                if (rootBoundingVolume == null) {
                    rootBoundingVolume = new BoundingVolume();
                    double[] rootRegion = new double[6];
                    System.arraycopy(childRegion, 0, rootRegion, 0, 6);
                    rootBoundingVolume.setRegion(rootRegion);
                } else {
                    double[] rootRegion = rootBoundingVolume.getRegion();
                    // minx, miny, maxx, maxy, minz, maxz
                    rootRegion[0] = Math.min(rootRegion[0], childRegion[0]);
                    rootRegion[1] = Math.min(rootRegion[1], childRegion[1]);
                    rootRegion[2] = Math.max(rootRegion[2], childRegion[2]);
                    rootRegion[3] = Math.max(rootRegion[3], childRegion[3]);
                    rootRegion[4] = Math.min(rootRegion[4], childRegion[4]);
                    rootRegion[5] = Math.max(rootRegion[5], childRegion[5]);
                }
            }
        }

        Tileset tileset;
        if (globalOptions.getTilesVersion().equals("1.0")) {
            tileset = new Tileset();
            AssetV1 asset = new AssetV1();
            tileset.setAsset(asset);
        } else {
            tileset = new TilesetV2();
            AssetV2 asset = new AssetV2();
            tileset.setAsset(asset);
        }
        double rootGeometricError = root.getGeometricError();
        if (rootGeometricError < 500.0) {
            rootGeometricError = 500.0;
            root.setGeometricError(rootGeometricError);
        }
        root.cutFastRegionDecimals();
        tileset.setGeometricError(rootGeometricError);
        tileset.setRoot(root);
        return tileset;
    }

    public boolean integralDecimateScenesMagoGLST(List<TileInfo> tileInfos,
                                                  int lod,
                                                  int nodeDepth,
                                                  Node rootNode,
                                                  int maxDepth,
                                                  DecimateParameters decimateParameters,
                                                  ReMeshParameters reMeshParams,
                                                  double screenPixelsForMeter) {
        // 1rst, find all tileInfos that intersects with the node
        log.info("Creating netSurface nodes for nodeDepth : " + nodeDepth + " of maxDepth : " + maxDepth);
        List<Node> nodes = new ArrayList<>();
        List<Node> intersectedNodes = new ArrayList<>();

        List<TileInfo> tileInfosOfNode = new ArrayList<>();
        boolean makeVerticalSkirt = true;
        boolean cellSizeGreaterThanTileInfosBBox = false;

        // ReMeshParameters
        GaiaBoundingBox rootNodeBBoxLC = rootNode.calculateLocalBoundingBox();
        Matrix4d rootTransformMatrix = getNodeTransformMatrix(rootNode);
        Matrix4d rootTransformMatrixInverse = new Matrix4d(rootTransformMatrix);
        rootTransformMatrixInverse.invert();

        Map<Node, List<TileInfo>> nodeTileInfosMap = new HashMap<>();
        for (TileInfo tileInfo : tileInfos) {
            GaiaBoundingBox cartographicBBox = tileInfo.getCartographicBBox();
            if (cartographicBBox == null) {
                log.error("[ERROR] cartographicBBox is null.");
                continue;
            }

            intersectedNodes.clear();
            //*************************************************************************************************************************************
            // in integral-reMesh, the intersection between node and tileInfo must be between node and the cartographicCenterDegree of tileInfo
            Vector3d cartographicCenterDegree = cartographicBBox.getCenter();
            rootNode.getIntersectedNodesAsOctree(cartographicCenterDegree, nodeDepth, intersectedNodes);
            //*************************************************************************************************************************************

            int intersectedNodesCount = intersectedNodes.size();
            for (int i = 0; i < intersectedNodesCount; i++) {
                Node node = intersectedNodes.get(i);
                if (node.getDepth() != nodeDepth) {
                    continue;
                }
                List<TileInfo> tileInfosOfNodeList = nodeTileInfosMap.computeIfAbsent(node, k -> new ArrayList<>());
                tileInfosOfNodeList.add(tileInfo);
            }
        }

        // Calculate the cellGrid3d for calculate the tileBoundaryAnchors and globalBoundaryAnchors.
        int targetReMeshLod = lod;
        if (lod == 2) {
            targetReMeshLod = 3;
        }
        CellGrid3D cellGrid = createReMeshCellGridForLod(targetReMeshLod, maxDepth, rootNodeBBoxLC, tileInfos);
        reMeshParams.setCellGrid(cellGrid);

        // 1rst, calculate the average bbox minSize among tileInfos
        double bboxMinSize = Double.MAX_VALUE;
        double averageBBoxMinSize = 0.0;
        int tileInfosCount = tileInfos.size();
        for (int i = 0; i < tileInfosCount; i++) {
            TileInfo tileInfo = tileInfos.get(i);
            GaiaBoundingBox bbox = tileInfo.getBoundingBox();
            double tileMinSizeX = bbox.getSizeX();
            double tileMinSizeY = bbox.getSizeY();

            if (tileMinSizeX < tileMinSizeY) {
                bboxMinSize = tileMinSizeX;
            } else {
                bboxMinSize = tileMinSizeY;
            }

            averageBBoxMinSize += bboxMinSize;
        }

        averageBBoxMinSize = averageBBoxMinSize / (double) tileInfosCount;

        // Re mesh by vertex clustering************************************************************************
        double maxSize = rootNodeBBoxLC.getMaxSize();
        // the maxSize is for rootNode that has maxDepth
        // so, the maxSize rof lod is maxSize / Math.pow(2, maxDepth - lod);
        if (lod > 0) {
            maxSize = maxSize / Math.pow(2, maxDepth - lod);
        }

        // ifc round bridge settings
        double voxelSizeMeter = maxSize / 30.0;
        double texturePixelSize = maxSize / 512.0;
        double texturePixelsForMeter = 1.0 / texturePixelSize;

        nodes = new ArrayList<>(nodeTileInfosMap.keySet());

        int nodesCount = nodes.size();
        for (int i = 0; i < nodesCount; i++) {
            tileInfosOfNode.clear();
            Node node = nodes.get(i);

            tileInfosOfNode = nodeTileInfosMap.get(node);

            int tileInfosOfNodeCount = tileInfosOfNode.size();
            if (tileInfosOfNodeCount == 0) {
                continue;
            }

            node.setRefine(Node.RefineType.REPLACE);

            // create sceneInfos
            List<SceneInfo> sceneInfos = new ArrayList<>();
            for (int j = 0; j < tileInfosOfNodeCount; j++) {
                TileInfo tileInfo = tileInfosOfNode.get(j);
                SceneInfo sceneInfo = new SceneInfo();
                sceneInfo.setScenePath(tileInfo.getTempPath().toString());
                TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
                Vector3d geoCoordPosition = tileTransformInfo.getPosition();
                Vector3d posWC = GlobeUtils.geographicToCartesianWgs84(geoCoordPosition);
                Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(posWC);
                sceneInfo.setTransformMatrix(transformMatrix);

                // for remeshParams***************************************************************************************
                Vector3d geoCoordCenter = tileInfo.getTileTransformInfo().getPosition(); // use TileTransformInfo position instead of KmlInfo position
                Vector3d scenePosWC = GlobeUtils.geographicToCartesianWgs84(geoCoordCenter);
                Vector4d scenePosLC4d = new Vector4d(scenePosWC.x, scenePosWC.y, scenePosWC.z, 1.0);
                scenePosLC4d = rootTransformMatrixInverse.transform(scenePosLC4d);
                Vector3d scenePosLC = new Vector3d(scenePosLC4d.x, scenePosLC4d.y, scenePosLC4d.z);

                sceneInfo.setScenePosLC(scenePosLC);

                sceneInfos.add(sceneInfo);
            }

            if (sceneInfos.isEmpty()) {
                log.error("[ERROR] Error : sceneInfos is empty.");
                continue;
            }

            Vector3d nodeCenterGeoCoordRad = node.getBoundingVolume().calcCenter();
            Vector3d nodeCenterGeoCoordDeg = new Vector3d(Math.toDegrees(nodeCenterGeoCoordRad.x), Math.toDegrees(nodeCenterGeoCoordRad.y), nodeCenterGeoCoordRad.z);
            Vector3d nodePosWC = GlobeUtils.geographicToCartesianWgs84(nodeCenterGeoCoordDeg);
            Matrix4d nodeTMatrix = node.getTransformMatrix();
            if (nodeTMatrix == null) {
                nodeTMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(nodePosWC);
            }
            GaiaBoundingBox nodeBBoxLC = node.calculateLocalBoundingBox();

            log.info("nodeCode : " + node.getNodeCode() + " currNodeIdx : " + i + " / " + nodesCount);
            int maxScreenSize = 512;

            List<HalfEdgeScene> resultHalfEdgeScenes = new ArrayList<>();
            String outputPathString = globalOptions.getOutputPath();
            String nodeName = "node_L_" + nodeDepth + "_" + i;

            MagoReTextureByObliqueCamera magoReTextureByObliqueCamera = new MagoReTextureByObliqueCamera();
            magoReTextureByObliqueCamera.integralDecimateByObliqueCamera(sceneInfos,
                    resultHalfEdgeScenes,
                    decimateParameters,
                    reMeshParams,
                    nodeBBoxLC,
                    nodeTMatrix,
                    maxScreenSize,
                    outputPathString,
                    nodeName,
                    lod);
            //************************************************************************************************************************************************
            if (resultHalfEdgeScenes.isEmpty()) {
                log.info("IntegralReMesh resultHalfEdgeScenes is empty.");
                continue;
            }

            HalfEdgeScene halfEdgeScene = resultHalfEdgeScenes.getFirst();
            GaiaScene gaiaScene = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);
            makeContentsForNode(node, gaiaScene, lod, nodeDepth, i);
        }

        return cellSizeGreaterThanTileInfosBBox;
    }

    protected boolean configureBoundaryAnchors(
            ReMeshParameters reMeshParameters,
            int anchorLod,
            Map<Integer, CutAndScissorMT.LodBoundaryAnchors>
                    boundaryAnchorsByLod
    ) {
        if (reMeshParameters == null
                || boundaryAnchorsByLod == null) {
            return false;
        }

        CutAndScissorMT.LodBoundaryAnchors lodAnchors =
                boundaryAnchorsByLod.get(anchorLod);

        if (lodAnchors == null
                || lodAnchors.cellGrid() == null
                || lodAnchors.globalBoundaryAnchors() == null) {

            log.warn(
                    "Boundary anchors not available for LOD {}",
                    anchorLod
            );

            reMeshParameters.setCellGrid(null);
            reMeshParameters.setGlobalBoundaryAnchors(null);

            return false;
        }

        reMeshParameters.setCellGrid(
                lodAnchors.cellGrid()
        );

        reMeshParameters.setGlobalBoundaryAnchors(
                lodAnchors.globalBoundaryAnchors()
        );

        log.info(
                "Boundary anchors configured. "
                        + "LOD={}, anchors={}",
                anchorLod,
                lodAnchors.globalBoundaryAnchors().size()
        );

        return true;
    }

    public boolean integralLeafScenesST(List<TileInfo> tileInfos,
                                      int lod,
                                      int nodeDepth,
                                      Node rootNode,
                                      int maxDepth) {
        log.info("Creating netSurface nodes for nodeDepth : " + nodeDepth + " of maxDepth : " + maxDepth);
        List<Node> nodes = new ArrayList<>();
        List<Node> intersectedNodes = new ArrayList<>();

        List<TileInfo> tileInfosOfNode = new ArrayList<>();
        boolean cellSizeGreaterThanTileInfosBBox = false;

        // ReMeshParameters
        GaiaBoundingBox rootNodeBBoxLC = rootNode.calculateLocalBoundingBox();
        Matrix4d rootTransformMatrix = getNodeTransformMatrix(rootNode);
        Matrix4d rootTransformMatrixInverse = new Matrix4d(rootTransformMatrix);
        rootTransformMatrixInverse.invert();

        Map<Node, List<TileInfo>> nodeTileInfosMap = new HashMap<>();
        for (TileInfo tileInfo : tileInfos) {
            GaiaBoundingBox cartographicBBox = tileInfo.getCartographicBBox();
            if (cartographicBBox == null) {
                log.error("[ERROR] cartographicBBox is null.");
                continue;
            }

            intersectedNodes.clear();
            //*************************************************************************************************************************************
            // The intersection between node and tileInfo must be between node and the cartographicCenterDegree of tileInfo
            Vector3d cartographicCenterDegree = cartographicBBox.getCenter();
            rootNode.getIntersectedNodesAsOctree(cartographicCenterDegree, nodeDepth, intersectedNodes);
            //*************************************************************************************************************************************

            int intersectedNodesCount = intersectedNodes.size();
            for (int i = 0; i < intersectedNodesCount; i++) {
                Node node = intersectedNodes.get(i);
                if (node.getDepth() != nodeDepth) {
                    continue;
                }
                List<TileInfo> tileInfosOfNodeList = nodeTileInfosMap.computeIfAbsent(node, k -> new ArrayList<>());
                tileInfosOfNodeList.add(tileInfo);
            }
        }

        // 1rst, calculate the average bbox minSize among tileInfos
        double bboxMinSize = Double.MAX_VALUE;
        double averageBBoxMinSize = 0.0;
        int tileInfosCount = tileInfos.size();
        for (int i = 0; i < tileInfosCount; i++) {
            TileInfo tileInfo = tileInfos.get(i);
            GaiaBoundingBox bbox = tileInfo.getBoundingBox();
            double tileMinSizeX = bbox.getSizeX();
            double tileMinSizeY = bbox.getSizeY();

            if (tileMinSizeX < tileMinSizeY) {
                bboxMinSize = tileMinSizeX;
            } else {
                bboxMinSize = tileMinSizeY;
            }

            averageBBoxMinSize += bboxMinSize;
        }

        averageBBoxMinSize = averageBBoxMinSize / (double) tileInfosCount;

        nodes = new ArrayList<>(nodeTileInfosMap.keySet());

        MagoLeafTileManager magoLeafTileManager = new MagoLeafTileManager();

        int nodesCount = nodes.size();
        for (int i = 0; i < nodesCount; i++) {
            tileInfosOfNode.clear();
            Node node = nodes.get(i);

            tileInfosOfNode = nodeTileInfosMap.get(node);

            int tileInfosOfNodeCount = tileInfosOfNode.size();
            if (tileInfosOfNodeCount == 0) {
                continue;
            }

            node.setRefine(Node.RefineType.REPLACE);

            // create sceneInfos
            List<SceneInfo> sceneInfos = new ArrayList<>();
            for (int j = 0; j < tileInfosOfNodeCount; j++) {
                TileInfo tileInfo = tileInfosOfNode.get(j);
                SceneInfo sceneInfo = new SceneInfo();
                sceneInfo.setScenePath(tileInfo.getTempPath().toString());
                TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
                Vector3d geoCoordPosition = tileTransformInfo.getPosition();
                Vector3d posWC = GlobeUtils.geographicToCartesianWgs84(geoCoordPosition);
                Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(posWC);
                sceneInfo.setTransformMatrix(transformMatrix);

                // for remeshParams***************************************************************************************
                Vector3d geoCoordCenter = tileInfo.getTileTransformInfo().getPosition(); // use TileTransformInfo position instead of KmlInfo position
                Vector3d scenePosWC = GlobeUtils.geographicToCartesianWgs84(geoCoordCenter);
                Vector4d scenePosLC4d = new Vector4d(scenePosWC.x, scenePosWC.y, scenePosWC.z, 1.0);
                scenePosLC4d = rootTransformMatrixInverse.transform(scenePosLC4d);
                Vector3d scenePosLC = new Vector3d(scenePosLC4d.x, scenePosLC4d.y, scenePosLC4d.z);

                sceneInfo.setScenePosLC(scenePosLC);

                sceneInfos.add(sceneInfo);
            }

            if (sceneInfos.isEmpty()) {
                log.error("[ERROR] Error : sceneInfos is empty.");
                continue;
            }

            Vector3d nodeCenterGeoCoordRad = node.getBoundingVolume().calcCenter();
            Vector3d nodeCenterGeoCoordDeg = new Vector3d(Math.toDegrees(nodeCenterGeoCoordRad.x), Math.toDegrees(nodeCenterGeoCoordRad.y), nodeCenterGeoCoordRad.z);
            Vector3d nodePosWC = GlobeUtils.geographicToCartesianWgs84(nodeCenterGeoCoordDeg);
            Matrix4d nodeTMatrix = node.getTransformMatrix();
            if (nodeTMatrix == null) {
                nodeTMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(nodePosWC);
            }
            GaiaBoundingBox nodeBBoxLC = node.calculateLocalBoundingBox();

            log.info("nodeCode : " + node.getNodeCode() + " currNodeIdx : " + i + " / " + nodesCount);
            int maxScreenSize = 2048; // better than 512.***

            List<GaiaScene> resultGaiaScenes = new ArrayList<>();
            String outputPathString = globalOptions.getOutputPath();
            String nodeName = "node_L_" + nodeDepth + "_" + i;
            magoLeafTileManager.integralLeafScene(sceneInfos,
                    resultGaiaScenes,
                    nodeBBoxLC,
                    nodeTMatrix,
                    maxScreenSize,
                    outputPathString,
                    nodeName,
                    lod);
            //************************************************************************************************************************************************
            if (resultGaiaScenes.isEmpty()) {
                log.info("IntegralReMesh resultHalfEdgeScenes is empty.");
                continue;
            }

            GaiaScene gaiaScene = resultGaiaScenes.getFirst();
            makeContentsForNode(node, gaiaScene, lod, nodeDepth, i);
        }

        return cellSizeGreaterThanTileInfosBBox;
    }

    public boolean integralLeafScenesMT(
            List<TileInfo> tileInfos,
            int lod,
            int nodeDepth,
            Node rootNode,
            int maxDepth,
            int threadsCount
    ) {
        log.info(
                "Creating integral leaf nodes for nodeDepth: {} of maxDepth: {}",
                nodeDepth,
                maxDepth
        );

        if (tileInfos == null || tileInfos.isEmpty()) {
            return false;
        }

        if (rootNode == null) {
            throw new IllegalArgumentException(
                    "rootNode must not be null"
            );
        }

        Matrix4d rootTransformMatrix =
                getNodeTransformMatrix(rootNode);

        Matrix4d rootTransformMatrixInverse =
                new Matrix4d(rootTransformMatrix).invert();

        /*
         * 1rst phase sequential:
         * assign each TileInfo to its nodes.
         */
        Map<Node, List<TileInfo>> nodeTileInfosMap =
                new IdentityHashMap<>();

        List<Node> intersectedNodes =
                new ArrayList<>();

        for (TileInfo tileInfo : tileInfos) {
            if (tileInfo == null) {
                continue;
            }

            GaiaBoundingBox cartographicBBox =
                    tileInfo.getCartographicBBox();

            if (cartographicBBox == null) {
                log.error(
                        "cartographicBBox is null for TileInfo: {}",
                        tileInfo.getTempPath()
                );
                continue;
            }

            intersectedNodes.clear();

            Vector3d cartographicCenterDegree =
                    cartographicBBox.getCenter();

            rootNode.getIntersectedNodesAsOctree(
                    cartographicCenterDegree,
                    nodeDepth,
                    intersectedNodes
            );

            for (Node node : intersectedNodes) {
                if (node == null
                        || node.getDepth() != nodeDepth) {
                    continue;
                }

                nodeTileInfosMap
                        .computeIfAbsent(
                                node,
                                ignored -> new ArrayList<>()
                        )
                        .add(tileInfo);
            }
        }

        if (nodeTileInfosMap.isEmpty()) {
            log.warn(
                    "No nodes with TileInfos found for depth {}",
                    nodeDepth
            );
            return false;
        }

        /*
         * Create a stable list of works.
         * The index is fixed before launching the threads.
         */
        List<NodeIntegralWork> works =
                new ArrayList<>(nodeTileInfosMap.size());

        int nodeIndex = 0;

        for (Map.Entry<Node, List<TileInfo>> entry
                : nodeTileInfosMap.entrySet()) {

            Node node = entry.getKey();
            List<TileInfo> nodeTileInfos = entry.getValue();

            if (nodeTileInfos == null
                    || nodeTileInfos.isEmpty()) {
                continue;
            }

            node.setRefine(Node.RefineType.REPLACE);

            works.add(
                    new NodeIntegralWork(
                            nodeIndex,
                            node,
                            List.copyOf(nodeTileInfos)
                    )
            );

            nodeIndex++;
        }

        if (works.isEmpty()) {
            return false;
        }

        int availableProcessors =
                Runtime.getRuntime().availableProcessors();

        int realThreadCount = Math.min(
                works.size(),
                Math.min(
                        Math.max(1, threadsCount),
                        availableProcessors
                )
        );

        log.debug(
                "Integrating {} nodes using {} threads",
                works.size(),
                realThreadCount
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        realThreadCount,
                        new IntegralLeafThreadFactory()
                );

        CompletionService<NodeIntegralResult> completionService =
                new ExecutorCompletionService<>(executor);

        for (NodeIntegralWork work : works) {
            completionService.submit(
                    () -> processIntegralNode(
                            work,
                            lod,
                            nodeDepth,
                            rootTransformMatrixInverse
                    )
            );
        }

        executor.shutdown();

        try {
            for (int i = 0; i < works.size(); i++) {
                Future<NodeIntegralResult> future =
                        completionService.take();

                NodeIntegralResult result =
                        future.get();

                int completed = i + 1;

                if (result == null
                        || result.gaiaScene() == null) {

                    log.warn(
                            "Integral leaf node produced no scene: {} / {}",
                            completed,
                            works.size()
                    );
                    continue;
                }

                /*
                 * Executed in the main thread.
                 * This avoids potential races within
                 * makeContentsForNode().
                 */
                makeContentsForNode(
                        result.node(),
                        result.gaiaScene(),
                        lod,
                        nodeDepth,
                        result.nodeIndex()
                );

                log.info(
                        "Integral leaf node completed: {} / {}. Node: {}",
                        completed,
                        works.size(),
                        result.node().getNodeCode()
                );
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();

            throw new RuntimeException(
                    "Integral leaf processing interrupted",
                    e
            );

        } catch (ExecutionException e) {
            executor.shutdownNow();

            Throwable cause = e.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw new RuntimeException(
                    "Integral leaf worker failed",
                    cause
            );
        }

        /*
         * In the original code, this was always false.
         */
        return false;
    }

    private NodeIntegralResult processIntegralNode(
            NodeIntegralWork work,
            int lod,
            int nodeDepth,
            Matrix4d rootTransformMatrixInverse
    ) {
        Node node =
                work.node();

        List<SceneInfo> sceneInfos =
                createSceneInfos(
                        work.tileInfos(),
                        rootTransformMatrixInverse
                );

        if (sceneInfos.isEmpty()) {
            log.warn(
                    "No SceneInfo generated for node: {}",
                    node.getNodeCode()
            );

            return new NodeIntegralResult(
                    work.nodeIndex(),
                    node,
                    null
            );
        }

        Vector3d nodeCenterGeoCoordRad =
                node.getBoundingVolume().calcCenter();

        Vector3d nodeCenterGeoCoordDeg =
                new Vector3d(
                        Math.toDegrees(nodeCenterGeoCoordRad.x),
                        Math.toDegrees(nodeCenterGeoCoordRad.y),
                        nodeCenterGeoCoordRad.z
                );

        Vector3d nodePositionWorld =
                GlobeUtils.geographicToCartesianWgs84(
                        nodeCenterGeoCoordDeg
                );

        Matrix4d nodeTransformMatrix =
                node.getTransformMatrix();

        if (nodeTransformMatrix == null) {
            nodeTransformMatrix =
                    GlobeUtils.transformMatrixAtCartesianPointWgs84(
                            nodePositionWorld
                    );
        } else {
            /*
             * Create a defensive copy because Matrix4d is mutable.
             */
            nodeTransformMatrix =
                    new Matrix4d(nodeTransformMatrix);
        }

        GaiaBoundingBox nodeBoundingBoxLocal =
                node.calculateLocalBoundingBox();

        int maxScreenSize = 2048;

        List<GaiaScene> resultGaiaScenes =
                new ArrayList<>(1);

        String outputPathString =
                globalOptions.getOutputPath();

        /*
         * The name depends on the index assigned before launching
         * the threads, not on their completion order.
         */
        String nodeName =
                "node_L_"
                        + nodeDepth
                        + "_"
                        + work.nodeIndex();

        /*
         * Use one instance per worker.
         * The manager is not shared between nodes.
         */
        MagoLeafTileManager magoLeafTileManager =
                new MagoLeafTileManager();

        log.debug(
                "Integrating node {} with {} source scenes on thread {}",
                node.getNodeCode(),
                sceneInfos.size(),
                Thread.currentThread().getName()
        );

        magoLeafTileManager.integralLeafScene(
                sceneInfos,
                resultGaiaScenes,
                nodeBoundingBoxLocal,
                nodeTransformMatrix,
                maxScreenSize,
                outputPathString,
                nodeName,
                lod
        );

        if (resultGaiaScenes.isEmpty()) {
            log.warn(
                    "Integral leaf result is empty for node: {}",
                    node.getNodeCode()
            );

            return new NodeIntegralResult(
                    work.nodeIndex(),
                    node,
                    null
            );
        }

        return new NodeIntegralResult(
                work.nodeIndex(),
                node,
                resultGaiaScenes.getFirst()
        );
    }

    private List<SceneInfo> createSceneInfos(
            List<TileInfo> tileInfos,
            Matrix4d rootTransformMatrixInverse
    ) {
        List<SceneInfo> sceneInfos =
                new ArrayList<>(tileInfos.size());

        for (TileInfo tileInfo : tileInfos) {
            if (tileInfo == null
                    || tileInfo.getTempPath() == null) {
                continue;
            }

            TileTransformInfo tileTransformInfo =
                    tileInfo.getTileTransformInfo();

            if (tileTransformInfo == null
                    || tileTransformInfo.getPosition() == null) {

                log.warn(
                        "TileTransformInfo is null for TileInfo: {}",
                        tileInfo.getTempPath()
                );
                continue;
            }

            Vector3d geographicPosition =
                    tileTransformInfo.getPosition();

            Vector3d positionWorld =
                    GlobeUtils.geographicToCartesianWgs84(
                            geographicPosition
                    );

            Matrix4d transformMatrix =
                    GlobeUtils.transformMatrixAtCartesianPointWgs84(
                            positionWorld
                    );

            Vector4d positionLocal4d =
                    new Vector4d(
                            positionWorld.x,
                            positionWorld.y,
                            positionWorld.z,
                            1.0
                    );

            /*
             * transform() dont must modify the matrix.
             * We use Vector4d independent in each iteration.
             */
            rootTransformMatrixInverse.transform(
                    positionLocal4d
            );

            Vector3d positionLocal =
                    new Vector3d(
                            positionLocal4d.x,
                            positionLocal4d.y,
                            positionLocal4d.z
                    );

            SceneInfo sceneInfo =
                    new SceneInfo();

            sceneInfo.setScenePath(
                    tileInfo.getTempPath().toString()
            );

            sceneInfo.setTransformMatrix(
                    transformMatrix
            );

            sceneInfo.setScenePosLC(
                    positionLocal
            );

            sceneInfos.add(sceneInfo);
        }

        return sceneInfos;
    }

    protected void cutAndScissorAllLod(List<TileInfo> tileInfos, Node rootNode) {
        // Single-threading
        List<TileInfo> finalTileInfosCopy = new ArrayList<>();
        Map<Integer, List<TileInfo>> tileInfoListMap = new ConcurrentHashMap<>();
        log.info("Cutting and Scissor process is started. Total tileInfos : {}", tileInfos.size());

        int counter = 0;
        int maxDepth = projectMaxDepthIdx;
        // start with the mas lod possible.
        int lod = 7;
        if (lod > projectMaxDepthIdx) {
            lod = projectMaxDepthIdx;
        }
        for (TileInfo tileInfo : tileInfos) {
            log.info("CutRectangleCake : " + counter + " / " + tileInfos.size() + " LOD : " + lod);
            BoundingVolume rootNodeBoundingVolume = rootNode.getBoundingVolume();
            BoundingVolume rootNodeBoundingVolumeCopy = new BoundingVolume(rootNodeBoundingVolume);

            List<TileInfo> singleTileInfoList = new ArrayList<>();
            singleTileInfoList.add(tileInfo);
            String tileInfoName = tileInfo.getTempPath().getFileName().toString();
            log.info("[Tile][PhotoRealistic][{}/{}] - Cut RectangleCake one shoot... : {}", tileInfos.size(), tileInfoName);

            List<TileInfo> resultTileInfoList = new ArrayList<>();
            try {
                cutRectangleCakeAllLod(tileInfo, lod, rootNodeBoundingVolumeCopy, maxDepth, resultTileInfoList);
            } catch (Exception e) {
                log.error("[ERROR] :", e);
                throw new RuntimeException(e);
            }

            //resultTileInfos.addAll(resultTileInfoList);
            counter++;
        }
    }

    private void cutRectangleCakeAllLod(TileInfo tileInfo,
                                        int lod,
                                        BoundingVolume rootNodeBoundingVolume,
                                        int depthIdx,
                                        List<TileInfo> resultTileInfos) throws FileNotFoundException {
        boolean someSceneCut = false;

        // load the first scene of the tileInfo
        Path path = tileInfo.getTempPath();

        GaiaBoundingBox setBBox = tileInfo.getBoundingBox();
        if (setBBox == null) {
            log.error("[ERROR] setBBox is null.");
        }
        TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
        double errorWeld = 1e-6;

        List<GaiaAAPlane> allPlanes = new ArrayList<>();

        // load the file
        GaiaSet gaiaSet = null;
        try {
            gaiaSet = GaiaSet.readFile(path);
        } catch (Exception e) {
            log.error("[ERROR] reading GaiaSet from path: " + path, e);
            return;
        }
        if (gaiaSet == null) {
            return;
        }

        GaiaScene scene = new GaiaScene(gaiaSet);
        scene.deleteNormals();

        GaiaTriangulator triangulator = new GaiaTriangulator();
        triangulator.apply(scene);

        GaiaWeldOptions weldOptions = GaiaWeldOptions.builder().error(errorWeld).checkTexCoord(true).checkNormal(false).checkColor(false).checkBatchId(false).build();
        GaiaWelder weld = new GaiaWelder(weldOptions);
        weld.apply(scene);

        HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);

        Matrix4d transformMatrix = new Matrix4d();
        GaiaBoundingBox boundingBox = this.getCuttingPlanesAndLocalBoundingBox(tileInfo, lod, rootNodeBoundingVolume, depthIdx, allPlanes, transformMatrix);

        log.debug("cutting rectangle cake one shoot. lod : " + lod);

        boolean scissorTextures = true;
        boolean makeSkirt = GlobalConstants.MAKE_SKIRT;

        // create tileInfos for the cut scenes
        String tempPathString = globalOptions.getTempPath();
        String cutTempPathString = Path.of(tempPathString, "cutTemp").toString();
        Path cutTempPath = Paths.get(cutTempPathString);
        // create directory if not exists
        if (!cutTempPath.toFile().exists() && cutTempPath.toFile().mkdirs()) {
            log.debug("cutTemp folder created.");
        }

//        Path cutTempLodPath = cutTempPath.resolve("lod" + lod);
//        if (!cutTempLodPath.toFile().exists() && cutTempLodPath.toFile().mkdirs()) {
//            log.debug("cutTempLod folder created.");
//        }

        HalfEdgeOctreeFaces resultOctree = new HalfEdgeOctreeFaces(null, boundingBox);
        resultOctree.setLimitDepth(depthIdx - lod);

        List<TileInfo> cutTileInfos = this.cutHalfEdgeSceneByGaiaAAPlanesAndSaveTileInfosAllLodMode(halfEdgeScene,
                scissorTextures,
                makeSkirt,
                tileInfo,
                lod,
                rootNodeBoundingVolume,
                depthIdx,
                cutTempPath);
        resultTileInfos.addAll(cutTileInfos);

        // delete halfEdgeScene
        halfEdgeScene.deleteObjects();
        gaiaSet.clear();
        scene.clear();
    }

    public List<TileInfo> cutHalfEdgeSceneByGaiaAAPlanesAndSaveTileInfosAllLodMode(HalfEdgeScene halfEdgeScene,
                                                                                   boolean scissorTextures,
                                                                                   boolean makeSkirt,
                                                                                   TileInfo motherTileInfo,
                                                                                   int lod,
                                                                                   BoundingVolume rootNodeBoundingVolume,
                                                                                   int projectMaxDepth,
                                                                                   Path cutTempPath) {
        TileTransformInfo tileTransformInfo = motherTileInfo.getTileTransformInfo();
        Vector3d geoCoordPosition = tileTransformInfo.getPosition();

        List<TileInfo> cutTileInfos = new ArrayList<>();
        List<GaiaAAPlane> planes = new ArrayList<>();

        for (int currLod = lod; currLod >= 0; currLod--) {
            // calculate the AAPlanes to cut.***
            Matrix4d transformMatrix = new Matrix4d();
            GaiaBoundingBox boundingBox = null;
            planes.clear();
            try {
                boundingBox = this.getCuttingPlanesAndLocalBoundingBox(motherTileInfo, currLod, rootNodeBoundingVolume, projectMaxDepth, planes, transformMatrix);
            } catch (Exception e) {
                log.error("[ERROR] calculating cutting planes for LOD " + currLod, e);
                continue;
            }

            HalfEdgeOctreeFaces resultOctree = new HalfEdgeOctreeFaces(null, boundingBox);
            resultOctree.setLimitDepth(projectMaxDepth - currLod);

            double error = 1e-4;
            int planesCount = planes.size();
            boolean cut = false;
            PlaneCutResult planeCutResult = new PlaneCutResult();
            for (int i = 0; i < planesCount; i++) {
                GaiaAAPlane plane = planes.get(i);
                if (halfEdgeScene.cutByPlane(plane.getPlaneType(), plane.getPoint(), error, planeCutResult)) {
                    cut = true;
                }
            }

            halfEdgeScene.deleteDegeneratedFaces();

            // now, distribute faces into octree
            //resultOctree.getFaces().clear();
            List<HalfEdgeSurface> surfaces = halfEdgeScene.extractSurfaces(null);
            for (HalfEdgeSurface surface : surfaces) {
                List<HalfEdgeFace> faces = surface.getFaces();
                for (HalfEdgeFace face : faces) {
                    if (face.getStatus() == ObjectStatus.DELETED) {
                        continue;
                    }
                    resultOctree.addContent(face);
                }
            }

            resultOctree.distributeFacesToTargetDepth(resultOctree.getLimitDepth());
            List<GaiaOctree<HalfEdgeFace>> octreesWithContents = resultOctree.extractOctreesWithContents();

            // set the classifyId for each face
            int octreesCount = octreesWithContents.size();
            for (int j = 0; j < octreesCount; j++) {
                HalfEdgeOctreeFaces octree = (HalfEdgeOctreeFaces) octreesWithContents.get(j);
                List<HalfEdgeFace> faces = octree.getContents();
                for (HalfEdgeFace face : faces) {
                    face.setClassifyId(j);
                }
            }

            for (int j = 0; j < octreesCount; j++) {
                int classifyId = j;

                // create a new HalfEdgeScene
                HalfEdgeScene cuttedScene = halfEdgeScene.cloneByClassifyId(classifyId);
                cuttedScene.deleteDegeneratedFaces();
                cuttedScene.deleteNoUsedMaterials();
                cuttedScene.removeDeletedObjects();

                if (scissorTextures && cut) {
                    cuttedScene.scissorTexturesByMotherScene(halfEdgeScene.getMaterials());
                }

                if (cuttedScene == null) {
                    log.info("cuttedScene is null");
                    continue;
                }

                GaiaScene gaiaSceneCut = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(cuttedScene);
                List<GaiaMaterial> materials = cuttedScene.getMaterials(); // keep material.
                List<GaiaMaterial> voidMaterials = new ArrayList<>();
                cuttedScene.setMaterials(voidMaterials);
                cuttedScene.deleteObjects();// delete to save memory.

                GaiaBoundingBox boundingBoxCutLC = new GaiaBoundingBox();
                GaiaBoundingBox cartographicBoundingBox = this.calculateCartographicBoundingBox(gaiaSceneCut, transformMatrix, boundingBoxCutLC);

                // create an originalPath for the cut scene
                Path cutScenePath = Paths.get("");
                gaiaSceneCut.setOriginalPath(cutScenePath);

                GaiaSet gaiaSetCut = GaiaSet.fromGaiaScene(gaiaSceneCut);

                // delete the contents of the gaiaSceneCut before create tileInfo**********************
                gaiaSceneCut.getNodes().forEach(GaiaNode::clear); // new 20260420.

                // Save.*****************************************************************************************
                Path cutTempLodPath = cutTempPath.resolve("lod" + currLod);
                if (!cutTempLodPath.toFile().exists() && cutTempLodPath.toFile().mkdirs()) {
                    log.debug("cutTempLod folder created.");
                }

                UUID identifier = UUID.randomUUID();
                Path gaiaSetCutFolderPath = cutTempLodPath.resolve(identifier.toString());
                if (!gaiaSetCutFolderPath.toFile().exists() && gaiaSetCutFolderPath.toFile().mkdirs()) {
                    log.debug("gaiaSetCut folder created.");
                }

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
                        if (texture == null) {
                            continue;
                        }
                        // check if exist bufferedImage of the texture
                        if (texture.getBufferedImage() == null) {
                            // load the image
                            texture.loadImage();
                        }

                        if (texture.getBufferedImage() == null) {
                            continue;
                        }

                        if (currLod > 4) {
                            // resize the texture to half size
                            BufferedImage originalImage = texture.getBufferedImage();
                            if (originalImage == null) {
                                log.error("originalImage is null.");
                                continue;
                            }
                            if (originalImage.getWidth() > 4096 || originalImage.getHeight() > 4096) {
                                int newWidth = Math.min(originalImage.getWidth(), 4096);
                                int newHeight = Math.min(originalImage.getHeight(), 4096);
                                BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
                                Graphics2D g = resizedImage.createGraphics();
                                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                                g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
                                g.dispose();
                                texture.setBufferedImage(resizedImage);
                            }

                        }

                        texture.setParentPath(imagesPath.toString());
                        texture.saveImage(texture.getFullPath());
                    }
                }

                boolean copyTexturesToNewPath = false;
                Path tempPathLod = gaiaSetCut.writeFileForPR(gaiaSetCutFolderPath, copyTexturesToNewPath);

                /////////////////////////////////////////////////////////////////////////////////////////

                // create a new tileInfo for the cut scene
                TileInfo tileInfoCut = TileInfo.builder().scene(gaiaSceneCut).outputPath(motherTileInfo.getOutputPath()).build();
                tileInfoCut.setTempPath(tempPathLod);
                Matrix4d transformMatrixCut = new Matrix4d(motherTileInfo.getTransformMatrix());
                tileInfoCut.setTransformMatrix(transformMatrixCut);
                tileInfoCut.setBoundingBox(boundingBoxCutLC);
                tileInfoCut.setCartographicBBox(cartographicBoundingBox);

                // make a kmlInfo for the cut scene
                // In reality, we must recalculate the position of the cut scene. Provisionally, we use the same position
                TileTransformInfo tileTransformInfoCut = TileTransformInfo.builder().position(geoCoordPosition).build();
                tileInfoCut.setTileTransformInfo(tileTransformInfoCut);
                //cutTileInfos.add(tileInfoCut);

                addTileInfoInLod(currLod, tileInfoCut);

                cuttedScene.deleteObjects();
                gaiaSetCut.clear();
                gaiaSceneCut.clear();
            }
        }

        return cutTileInfos;
    }

    protected double calculateAverageBBoxMinSize(List<TileInfo> tileInfos) {
        double bboxMinSize = Double.MAX_VALUE;
        double averageBBoxMinSize = 0.0;
        int tileInfosCount = tileInfos.size();
        for (int i = 0; i < tileInfosCount; i++) {
            TileInfo tileInfo = tileInfos.get(i);
            GaiaBoundingBox bbox = tileInfo.getBoundingBox();
            double tileMinSizeX = bbox.getSizeX();
            double tileMinSizeY = bbox.getSizeY();

            if (tileMinSizeX < tileMinSizeY) {
                bboxMinSize = tileMinSizeX;
            } else {
                bboxMinSize = tileMinSizeY;
            }

            averageBBoxMinSize += bboxMinSize;
        }

        return averageBBoxMinSize / (double) tileInfosCount;
    }

    protected void makeContentsForNode(Node node, GaiaScene gaiaScene, int lod, int nodeDepth, int nodeIdx){
        GaiaAttribute gaiaAttribute = gaiaScene.getAttribute();
        String nodeCode = node.getNodeCode();
        gaiaAttribute.setNodeName(nodeCode);

        // Set samplers for photorealistic-mesh type 3d-tiles.*********************************************
        int GL_CLAMP_TO_EDGE = 33071;
        int GL_LINEAR = 9729;
        int GL_LINEAR_MIPMAP_LINEAR = 9987;
        //GL_NEAREST_MIPMAP_LINEAR: 9986, GL_LINEAR_MIPMAP_LINEAR: 9987

        List<GaiaMaterial> materials = gaiaScene.getMaterials();
        if(materials.size() > 1){
            log.warn("Warning : more than 1 material exists in the scene. nodeCode : " + node.getNodeCode() + " materialsCount : " + materials.size());
        }
        int matId = 0;
        for(GaiaMaterial material : materials){
            material.setId(matId);
            GaiaSamplers gaiaSampler = material.getSamplers();
            gaiaSampler.setMinFilter(GL_LINEAR);
            gaiaSampler.setMagFilter(GL_LINEAR);
            gaiaSampler.setWrapS(GL_CLAMP_TO_EDGE);
            gaiaSampler.setWrapT(GL_CLAMP_TO_EDGE);
            matId++;
        }



        String outputPathString = globalOptions.getOutputPath();
        String nodeName = "node_L_" + nodeDepth + "_" + nodeIdx;
        String netTempPathString = outputPathString + File.separator + "temp" + File.separator + "reMeshTemp";
        String netSetFolderPathString = netTempPathString + File.separator + nodeName;
        Path netSetFolderPath = Paths.get(netSetFolderPathString);

        // set originalPath if no exist.
        if(gaiaScene.getOriginalPath() == null){
            Path originalPath = Path.of("noOriginalPath");
            gaiaScene.setOriginalPath(originalPath);
        }

        // make contents for the node
        Vector3d nodeCenterGeoCoordRad = node.getBoundingVolume().calcCenter();
        Vector3d nodeCenterGeoCoordDeg = new Vector3d(Math.toDegrees(nodeCenterGeoCoordRad.x), Math.toDegrees(nodeCenterGeoCoordRad.y), nodeCenterGeoCoordRad.z);
        Vector3d nodePosWC = GlobeUtils.geographicToCartesianWgs84(nodeCenterGeoCoordDeg);
        Matrix4d nodeTMatrix = node.getTransformMatrix();
        if (nodeTMatrix == null) {
            nodeTMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(nodePosWC);
        }
        GaiaBoundingBox nodeBBoxLC = node.calculateLocalBoundingBox();

        List<TileInfo> netTileInfos = new ArrayList<>();
        TileInfo tileInfoNet = TileInfo.builder().scene(gaiaScene).outputPath(netSetFolderPath).build();
        //tileInfoNet.setTempPath(netSetPath);
        Matrix4d transformMatrixNet = new Matrix4d(nodeTMatrix);
        tileInfoNet.setTransformMatrix(transformMatrixNet);
        tileInfoNet.setBoundingBox(nodeBBoxLC);
        tileInfoNet.setCartographicBBox(null);

        // make a kmlInfo for the cut scene
        TileTransformInfo tileTransformInfoCut = TileTransformInfo.builder().position(nodeCenterGeoCoordDeg).build();
        tileInfoNet.setTileTransformInfo(tileTransformInfoCut);
        netTileInfos.add(tileInfoNet);

        ContentInfo contentInfo = new ContentInfo();

        contentInfo.setName(nodeCode);
        LevelOfDetail lodLevel = LevelOfDetail.getByLevel(lod);
        int lodError = lodLevel.getGeometricError();
        contentInfo.setLod(lodLevel);
        GaiaBoundingBox nodeCartographicBBox = node.calculateCartographicBoundingBox();
        contentInfo.setBoundingBox(nodeCartographicBBox); // must be cartographicBBox
        contentInfo.setNodeCode(nodeCode);
        contentInfo.setTileInfos(netTileInfos);
        contentInfo.setRemainTileInfos(null);
        contentInfo.setTransformMatrix(nodeTMatrix);

        Content content = new Content();
        if (globalOptions.getTilesVersion().equals("1.0")) {
            content.setUri("data/" + nodeCode + ".b3dm");
        } else {
            content.setUri("data/" + nodeCode + ".glb");
        }
        content.setContentInfo(contentInfo);
        if (node.getContent() != null) {
            log.info("Error : node.getContent() is not null. lod : " + lod + " nodeCode : " + nodeCode);
        }
        node.setContent(content);

        // check if gaiaRootNode has transformMatrix.
        List<GaiaNode> nodes = gaiaScene.getNodes();
        GaiaNode rootNode = nodes.get(0);
        if(rootNode.getTransformMatrix() == null){
            log.warn("Warning : rootNode.getTransformMatrix() is null. Set to identity matrix. nodeCode : " + nodeCode);
            Matrix4d identityMatrix = new Matrix4d();
            identityMatrix.identity();
            rootNode.setTransformMatrix(identityMatrix);
        }


        // save the b3dm/glb.
        PhotogrammetryBatcher photogrammetryBatcher = new PhotogrammetryBatcher();
        if (globalOptions.getTilesVersion()
                .equals("1.0")) {
            photogrammetryBatcher.runV1(contentInfo, gaiaScene);
        } else {
            photogrammetryBatcher.runV2(contentInfo, gaiaScene);
        }


        // delete scenes
        gaiaScene.clear();
    }

    private void setGeometryErrorToNodeManual(Node node, int maxDepth, double leafTileSize) {
        int lod = maxDepth - node.getDepth();

        double geometricError = 0.00001;

        if (node.getDepth() > 0) {
            if (lod == 0) {
                geometricError = 0.001;
            } else if (lod == 1) {
                geometricError = 1.0;
            } else if (lod == 2) {
                geometricError = 2.0;
            }
        }

        if (lod > 0) {
            BoundingVolume bVolume = node.getBoundingVolume();
            if (bVolume != null) {
                double[] region = bVolume.getRegion();
                if (region != null) {
                    double minLatRad = region[1];
                    double maxLatRad = region[3];

                    // calculate the distance between minLatRad and maxLatRad in meters
                    double earthRadius = 6378137.0; // in meters
                    double latDiff = maxLatRad - minLatRad;
                    double nodeSize = earthRadius * latDiff;
                    geometricError = nodeSize * 0.055; // 5% of the node size
                }
            }
        }

        node.setGeometricError(geometricError);
        List<Node> children = node.getChildren();
        if (children != null) {
            for (Node child : children) {
                setGeometryErrorToNodeManual(child, maxDepth, leafTileSize);
            }
        }
    }

    private Matrix4d getNodeTransformMatrix(Node node) {
        Vector3d nodeCenterGeoCoordRad = node.getBoundingVolume().calcCenter();
        Vector3d nodeCenterGeoCoordDeg = new Vector3d(Math.toDegrees(nodeCenterGeoCoordRad.x), Math.toDegrees(nodeCenterGeoCoordRad.y), nodeCenterGeoCoordRad.z);
        Vector3d nodePosWC = GlobeUtils.geographicToCartesianWgs84(nodeCenterGeoCoordDeg);
        Matrix4d nodeTMatrix = node.getTransformMatrix();
        if (nodeTMatrix == null) {
            nodeTMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(nodePosWC);
        }
        return nodeTMatrix;
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

    private GaiaBoundingBox getCuttingPlanesAndLocalBoundingBox(TileInfo tileInfo,
                                                                int lod,
                                                                BoundingVolume rootNodeBoundingVolume,
                                                                int depthIdx,
                                                                List<GaiaAAPlane> resultPlanes,
                                                                Matrix4d resultTransformMatrix) {
        // Note : tileInfos must contain only one tileInfo
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
            log.error("[ERROR] setBBox is null.");
        }
        TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
        Vector3d geoCoordPosition = tileTransformInfo.getPosition();
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

            // Longitude plane: create a point with lonDeg, geoCoordPosition.y, 0.0
            samplePointGeoCoord = new Vector3d(lonDeg, geoCoordPosition.y, 0.0);
            samplePointWC = GlobeUtils.geographicToCartesianWgs84(samplePointGeoCoord);
            transformMatrixInv.transformPosition(samplePointWC, samplePointLC);

            // check local min max X
            if (samplePointLC.x < localMinX) {
                localMinX = samplePointLC.x;
            }
            if (samplePointLC.x > localMaxX) {
                localMaxX = samplePointLC.x;
            }

            // check if the planeLC cuts the setBBox
            if (samplePointLC.x > setBBox.getMinX() && samplePointLC.x < setBBox.getMaxX()) {
                if (i > 0 && i < lonDivisions.size() - 1) {
                    GaiaAAPlane planeYZ = new GaiaAAPlane();
                    planeYZ.setPlaneType(PlaneType.YZ);
                    planeYZ.setPoint(new Vector3d(samplePointLC));
                    planesYZ.add(planeYZ);
                }
            }

            // Latitude plane: create a point with geoCoordPosition.x, latDeg, 0.0
            samplePointGeoCoord = new Vector3d(geoCoordPosition.x, latDeg, 0.0);
            samplePointWC = GlobeUtils.geographicToCartesianWgs84(samplePointGeoCoord);
            transformMatrixInv.transformPosition(samplePointWC, samplePointLC);

            // check local min max Y
            if (samplePointLC.y < localMinY) {
                localMinY = samplePointLC.y;
            }
            if (samplePointLC.y > localMaxY) {
                localMaxY = samplePointLC.y;
            }

            // check if the planeLC cuts the setBBox
            if (samplePointLC.y > setBBox.getMinY() && samplePointLC.y < setBBox.getMaxY()) {
                if (i > 0 && i < latDivisions.size() - 1) {
                    GaiaAAPlane planeXZ = new GaiaAAPlane();
                    planeXZ.setPlaneType(PlaneType.XZ);
                    planeXZ.setPoint(new Vector3d(samplePointLC));
                    planesXZ.add(planeXZ);
                }
            }

            // Altitude plane: create a point with geoCoordPosition.x, geoCoordPosition.y, 0.0
            samplePointGeoCoord = new Vector3d(geoCoordPosition.x, geoCoordPosition.y, altitude);
            samplePointWC = GlobeUtils.geographicToCartesianWgs84(samplePointGeoCoord);
            transformMatrixInv.transformPosition(samplePointWC, samplePointLC);

            // check local min max Z
            if (samplePointLC.z < localMinZ) {
                localMinZ = samplePointLC.z;
            }
            if (samplePointLC.z > localMaxZ) {
                localMaxZ = samplePointLC.z;
            }

            // check if the planeLC cuts the setBBox
            if (samplePointLC.z > setBBox.getMinZ() && samplePointLC.z < setBBox.getMaxZ()) {
                if (i > 0 && i < altDivisions.size() - 1) {
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

        GaiaBoundingBox resultBBox = new GaiaBoundingBox(localMinX, localMinY, localMinZ, localMaxX, localMaxY, localMaxZ, true);
        return resultBBox;
    }

    private GaiaBoundingBox calculateCartographicBoundingBox(GaiaScene gaiaScene, Matrix4d transformMatrix, GaiaBoundingBox resultBoundingBoxLC) {
//        GaiaScene gaiaSceneCut = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);
        GaiaBoundingBox boundingBoxCutLC = gaiaScene.updateBoundingBox();
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

    @Override
    public void writeTileset(Tileset tileset) {
        Node rootNode = tileset.getRoot();
        if (rootNode == null) {
            log.error("[ERROR] Tileset root node is null");
            throw new TileProcessingException("Tileset root node is null");
        } else if (rootNode.getBoundingVolume() == null) {
            log.error("[ERROR] Tileset root node bounding volume is null");
            throw new TileProcessingException("Tileset root node bounding volume is null");
        } else if (rootNode.getGeometricError() == 0 && tileset.getGeometricError() == 0) {
            log.error("[ERROR] Tileset root node geometric error is 0");
            throw new TileProcessingException("Tileset root node geometric error is 0");
        } else if (rootNode.getChildren() == null || rootNode.getChildren().isEmpty()) {
            log.error("[ERROR] Tileset root node children is null or empty");
            throw new TileProcessingException("Tileset root node children is null or empty");
        }

        Path outputPath = new File(globalOptions.getOutputPath()).toPath();
        File tilesetFile = outputPath.resolve("tileset.json").toFile();
        ObjectMapper objectMapper = new ObjectMapper();
        if (!globalOptions.isDebug()) {
            objectMapper.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
        }
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        //objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        try {
            java.nio.file.Files.createDirectories(outputPath);
        } catch (IOException e) {
            log.error("[ERROR] Failed to create output directory: {}", outputPath, e);
            throw new TileProcessingException("Failed to create output directory: " + outputPath, e);
        }
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

    private static final class IntegralLeafThreadFactory
            implements ThreadFactory {

        private final AtomicInteger counter =
                new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread =
                    new Thread(
                            runnable,
                            "integral-leaf-"
                                    + counter.incrementAndGet()
                    );

            thread.setDaemon(false);

            return thread;
        }
    }

    private record NodeIntegralWork(
            int nodeIndex,
            Node node,
            List<TileInfo> tileInfos
    ) {
    }

    private record NodeIntegralResult(
            int nodeIndex,
            Node node,
            GaiaScene gaiaScene
    ) {
    }
}
