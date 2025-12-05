package com.gaia3d.process.tileprocess.tile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.exception.TileProcessingException;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.pointcloud.GaiaPointCloudOld;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.pointcloud.GaiaLasPoint;
import com.gaia3d.converter.pointcloud.GaiaPointCloud;
import com.gaia3d.process.tileprocess.Tiler;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;
import com.gaia3d.process.tileprocess.tile.tileset.TilesetV2;
import com.gaia3d.process.tileprocess.tile.tileset.asset.AssetV1;
import com.gaia3d.process.tileprocess.tile.tileset.asset.AssetV2;
import com.gaia3d.process.tileprocess.tile.tileset.node.BoundingVolume;
import com.gaia3d.process.tileprocess.tile.tileset.node.Content;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import com.gaia3d.util.GlobeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class PointCloudTiler extends DefaultTiler implements Tiler {
    private final int MAXIMUM_DEPTH = 12;

    @Override
    public Tileset run(List<TileInfo> tileInfos) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        GaiaBoundingBox globalBoundingBox = calcCartographicBoundingBox(tileInfos);
        GaiaBoundingBox cubeGlobalBoundingBox = toCube(globalBoundingBox);
        Matrix4d transformMatrix = getTransformMatrixFromCartographic(globalBoundingBox);
        rotateX90(transformMatrix);

        double geographicError = calcGeometricErrorFromDegreeBoundingBox(cubeGlobalBoundingBox) / 2;
        log.info("[Tile][Tileset] Global Geographic Geometric Error: {}", geographicError);

        Node root = createRoot();
        root.setNodeCode("R");
        root.setBoundingBox(cubeGlobalBoundingBox);
        root.setRefine(Node.RefineType.ADD);
        root.setGeometricError(geographicError);
        BoundingVolume boundingVolume = new BoundingVolume(globalBoundingBox, BoundingVolume.BoundingVolumeType.REGION);

        // root만 큐브로
        root.setBoundingVolume(boundingVolume);
        root.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
        try {
            createRootNode(root, tileInfos);
        } catch (IOException e) {
            log.error("[ERROR] :", e);
            throw new TileProcessingException(e.getMessage());
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
        tileset.setGeometricError(geographicError);
        tileset.setRoot(root);
        return tileset;
    }

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

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        File outputPath = new File(globalOptions.getOutputPath());
        File tilesetFile = new File(outputPath, "tileset.json");
        ObjectMapper objectMapper = new ObjectMapper();
        if (!globalOptions.isDebug()) {
            objectMapper.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
        }
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
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

    private double calcChildrenGeometricError(GaiaBoundingBox boundingBox, int pointLimit) {
        double geographicError = calcGeometricErrorFromDegreeBoundingBox(boundingBox) / 2;
        double pointsFactor = pointLimit < 1000 ? 1 : ((double) 1000 / pointLimit);
        double calculatedGeometricError = geographicError * pointsFactor;

        if (calculatedGeometricError < 0.01) {
            calculatedGeometricError = 0.01;
        } else if (calculatedGeometricError < 0.1) {
            calculatedGeometricError = 0.1;
        } else if (calculatedGeometricError > 0.5 && geographicError < 1.0) {
            calculatedGeometricError = 1.0;
        }
        return calculatedGeometricError;
    }

    private double calcGeometricErrorFromDegreeBoundingBox(GaiaBoundingBox boundingBox) {
        Vector3d minPosition = boundingBox.getMinPosition();
        Vector3d maxPosition = boundingBox.getMaxPosition();
        double[] minLatLon = new double[]{minPosition.x, minPosition.y};
        double[] maxLatLon = new double[]{maxPosition.x, maxPosition.y};
        double[] distanced = GlobeUtils.distanceBetweenDegrees(minLatLon, maxLatLon);
        double verticalDistance = Math.abs(maxPosition.z - minPosition.z);
        return Math.sqrt(distanced[0] * distanced[0] + distanced[1] * distanced[1] + verticalDistance * verticalDistance);
    }

    private double calcGeometricError(GaiaPointCloudOld pointCloud) {
        GaiaBoundingBox boundingBox = pointCloud.getGaiaBoundingBox();
        return boundingBox.getLongestDistance();
    }

    @Override
    protected double calcGeometricError(List<TileInfo> tileInfos) {
        return tileInfos.stream().mapToDouble(tileInfo -> {
            GaiaBoundingBox boundingBox = tileInfo.getPointCloud().getGaiaBoundingBox();
            return boundingBox.getLongestDistance();
        }).max().orElse(0.0d);
    }

    @Override
    protected GaiaBoundingBox calcCartographicBoundingBox(List<TileInfo> tileInfos) {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        tileInfos.forEach(tileInfo -> {
            GaiaBoundingBox localBoundingBox = tileInfo.getPointCloud().getGaiaBoundingBox();
            boundingBox.addBoundingBox(localBoundingBox);
        });
        return boundingBox;
    }

    private void createRootNode(Node parentNode, List<TileInfo> tileInfos) throws IOException {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        List<GaiaPointCloud> pointClouds = tileInfos.stream()
                .map(TileInfo::getPointCloud)
                .toList();
        int index = 0;
        int maximumIndex = pointClouds.size();
        int rootPointLimit = globalOptions.getMaximumPointPerTile() / 32;
        for (GaiaPointCloud pointCloud : pointClouds) {
            GaiaBoundingBox box = pointCloud.getGaiaBoundingBox();
            box = toCube(box);
            pointCloud.setGaiaBoundingBox(box);

            log.info("[Tile][{}/{}] original Point Count : {}", (index + 1), maximumIndex, pointCloud.getPointCount());
            pointCloud.setCode((index++) + "");
            int chunkSize = pointCloud.CHUNK_SIZE;
            int chunkCount = pointCloud.getChunkCount(chunkSize);
            log.info("[Tile][{}/{}][{}/{}] Chunk Size : {} / Chunk Count : {}", index, maximumIndex, index, maximumIndex, chunkSize, chunkCount);
            long offset = 0;

            GaiaPointCloud rootTile = null;
            for (int i = 0; i < chunkCount; i++) {
                GaiaPointCloud chunk = pointCloud.readChunk(chunkSize, offset);
                offset += chunkSize;
                log.info("[Tile][{}/{}][{}/{}][Chunk {}/{}] Point Count : {}", index, maximumIndex, index, maximumIndex, (i + 1), chunkCount, chunk.getPointCount());

                if (rootTile == null) {
                    log.info("[Tile][{}/{}][{}/{}][Chunk {}/{}] Creating Root Tile", index, maximumIndex, index, maximumIndex, (i + 1), chunkCount);
                    rootTile = createNode(index, parentNode, null, chunk, rootPointLimit, 0);
                } else {
                    Node firstChildNode = parentNode.getChildren().getLast();
                    log.info("[Tile][{}/{}][{}/{}][Chunk {}/{}] Expanding Nodes", index, maximumIndex, index, maximumIndex, (i + 1), chunkCount);
                    expandNode(index, firstChildNode, rootTile, chunk, rootPointLimit, 0);
                }

                //long rootAllPointCount = rootTile.getAllPointCount();
                //int maximumDepth = rootTile.getMaxDepth();
                //int nodeCount = rootTile.getNodeCount();
                //log.info("[Tile][{}/{}][{}/{}][Chunk {}/{}] Created All Point Count : {}, Max Depth : {}, Node Count : {}", index, maximumIndex, index, maximumIndex, (i + 1), chunkCount, rootAllPointCount, maximumDepth, nodeCount);
                List<GaiaPointCloud> childrenPointClouds = rootTile.getAllLeaves();
                minimizeAllPointCloud(index, maximumIndex, childrenPointClouds);
                chunk.clearPoints();
            }
            log.info("[Tile][{}/{}] completed.", index, maximumIndex);
        }
    }

    private void expandNode(int index, Node parentNode, GaiaPointCloud parent, GaiaPointCloud target, int pointLimit, int depth) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();

        Node currentNode = parentNode.getChildren().stream().filter(node -> node.getNodeCode().equals(parentNode.getNodeCode() + target.getCode()))
                .findFirst()
                .orElse(null);
        GaiaPointCloud currentPointCloud = parent.getChildren().stream()
                .filter(child -> child.getCode().equals(target.getCode()))
                .findFirst()
                .orElse(null);

        if (parent.getCode().equals("R") && target.getCode().equals("R")) {
            currentNode = parentNode;
            currentPointCloud = parent;
            GaiaBoundingBox cubeBoundingBox = target.getGaiaBoundingBox();
            target.setGaiaBoundingBox(cubeBoundingBox);
            List<GaiaPointCloud> distributes = target.distribute();
            for (GaiaPointCloud distribute : distributes) {
                if (distribute.getPointCount() < 1) {
                    continue;
                }

                int newPointLimit = (int) (pointLimit * 2.0f);
                if (newPointLimit > globalOptions.getMaximumPointPerTile()) {
                    newPointLimit = globalOptions.getMaximumPointPerTile();
                }

                int newDepth = depth + 1;
                if (newDepth < MAXIMUM_DEPTH) {
                    expandNode(index, currentNode, currentPointCloud, distribute, newPointLimit, newDepth);
                }
            }
            return;
        }

        if ((currentNode == null) != (currentPointCloud == null)) {
            throw new TileProcessingException("Node or PointCloud with the same code already exists: " + target.getCode());
        }

        boolean hasMatchedNodeAndPointCloud = (currentNode != null && currentPointCloud != null);
        if (hasMatchedNodeAndPointCloud) {
            GaiaBoundingBox cubeBoundingBox = currentPointCloud.getGaiaBoundingBox();
            GaiaBoundingBox fitBoundingBox = calcFitBoundingBox(target);
            double dimensionRatio = calcDimensionRatio(fitBoundingBox, cubeBoundingBox);
            int chunkPointLimit = (int) (pointLimit * dimensionRatio);

            List<GaiaPointCloud> divided;
            long remainPointCount = currentPointCloud.getLimitPointCount() - currentPointCloud.getPointCount();
            if (remainPointCount > 0) {
                divided = target.divideChunkSize((int) remainPointCount);
                currentPointCloud.maximize(false);
                GaiaPointCloud newSelfPointCloud = divided.getFirst();
                currentPointCloud.combine(newSelfPointCloud);
                if (currentPointCloud.getPointCount() == currentPointCloud.getLimitPointCount()) {
                    File tempPath = new File(GlobalOptions.getInstance().getTempPath());
                    File tempFile = new File(tempPath, UUID.randomUUID().toString());
                    currentPointCloud.minimize(tempFile);
                }
            } else {
                divided = target.divideChunkSize(chunkPointLimit);
            }

            GaiaPointCloud remainPointCloud = divided.getLast();
            if (remainPointCloud.getPointCount() > 0) {
                List<GaiaPointCloud> distributes = remainPointCloud.distribute();
                for (GaiaPointCloud distribute : distributes) {
                    if (distribute.getPointCount() < 1) {
                        continue;
                    }

                    int newPointLimit = (int) (pointLimit * 2.0f);
                    if (newPointLimit > globalOptions.getMaximumPointPerTile()) {
                        newPointLimit = globalOptions.getMaximumPointPerTile();
                    }

                    int newDepth = depth + 1;
                    if (newDepth < MAXIMUM_DEPTH) {
                        expandNode(index, currentNode, currentPointCloud, distribute, newPointLimit, newDepth);
                    }
                }
            }
        } else {
            createNode(index, parentNode, parent, target, pointLimit, depth);
        }
    }

    private GaiaPointCloud createNode(int index, Node parentNode, GaiaPointCloud parent, GaiaPointCloud pointCloud, int pointLimit, int depth) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        GaiaBoundingBox cubeBoundingBox = pointCloud.getGaiaBoundingBox();
        GaiaBoundingBox fitBoundingBox = calcFitBoundingBox(pointCloud);
        double dimensionRatio = calcDimensionRatio(fitBoundingBox, cubeBoundingBox);
        int chunkPointLimit = (int) (pointLimit * dimensionRatio);

        pointCloud.setLimitPointCount(chunkPointLimit);
        List<GaiaPointCloud> divided = pointCloud.divideChunkSize(chunkPointLimit);
        GaiaPointCloud selfPointCloud = divided.getFirst();
        selfPointCloud.setParent(parent);
        if (parent != null) {
            parent.addChild(selfPointCloud);
        }

        Matrix4d transformMatrix = getTransformMatrixFromCartographic(fitBoundingBox);
        rotateX90(transformMatrix);
        double calculatedGeometricError = calcChildrenGeometricError(cubeBoundingBox, pointLimit);
        Node childNode = createChildNode(index, parent, parentNode, transformMatrix, selfPointCloud, cubeBoundingBox, fitBoundingBox, calculatedGeometricError);
        parentNode.getChildren().add(childNode);

        GaiaPointCloud remainPointCloud = divided.getLast();
        if (remainPointCloud.getPointCount() > 0) {
            List<GaiaPointCloud> distributes = remainPointCloud.distribute();
            for (GaiaPointCloud distribute : distributes) {
                if (distribute.getPointCount() < 1) {
                    continue;
                }

                int newPointLimit = (int) (pointLimit * 2.0f);
                if (newPointLimit > globalOptions.getMaximumPointPerTile()) {
                    newPointLimit = globalOptions.getMaximumPointPerTile();
                }

                int newDepth = depth + 1;
                if (newDepth < MAXIMUM_DEPTH) {
                    createNode(index, childNode, selfPointCloud, distribute, newPointLimit, newDepth);
                }
            }
        }
        return selfPointCloud;
    }


    private Node createChildNode(int index, GaiaPointCloud parent, Node parentNode, Matrix4d transformMatrix, GaiaPointCloud pointCloud, GaiaBoundingBox cubeBoundingBox, GaiaBoundingBox fitBoundingBox, double calculatedGeometricError) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        BoundingVolume boundingVolume = new BoundingVolume(fitBoundingBox, BoundingVolume.BoundingVolumeType.REGION);

        Node childNode = new Node();
        childNode.setParent(parentNode);
        childNode.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
        childNode.setBoundingBox(cubeBoundingBox);
        childNode.setBoundingVolume(boundingVolume);
        childNode.setRefine(Node.RefineType.ADD);
        childNode.setChildren(new ArrayList<>());
        if (parentNode == null) {
            throw new TileProcessingException("Parent node is null when creating child node.");
        }
        childNode.setNodeCode(parentNode.getNodeCode() + pointCloud.getCode());
        if (parent == null) {
            childNode.setNodeCode(childNode.getNodeCode() + index);
        }
        childNode.setGeometricError(calculatedGeometricError);

        TileInfo selfTileInfo = TileInfo.builder()
                .pointCloud(pointCloud)
                .boundingBox(cubeBoundingBox)
                .build();
        List<TileInfo> tileInfos = new ArrayList<>();
        tileInfos.add(selfTileInfo);

        ContentInfo contentInfo = new ContentInfo();
        contentInfo.setName("P" + childNode.getNodeCode());
        contentInfo.setLod(LevelOfDetail.LOD0);
        contentInfo.setBoundingBox(cubeBoundingBox);
        contentInfo.setNodeCode(childNode.getNodeCode());
        contentInfo.setTileInfos(tileInfos);

        Content content = new Content();
        if (globalOptions.getTilesVersion().equals("1.0")) {
            content.setUri("data/" + childNode.getNodeCode() + ".pnts");
        } else {
            content.setUri("data/" + childNode.getNodeCode() + ".glb");
        }
        content.setContentInfo(contentInfo);

        if (pointCloud.getPointCount() < 1) {
            log.debug("[warn] PointCloud point count is less than 1 for node: {}", childNode.getNodeCode());
        } else {
            childNode.setContent(content);
        }

        return childNode;
    }

    private double calcDimensionRatio(GaiaBoundingBox fitBoundingBox, GaiaBoundingBox cubeBoundingBox) {
        Vector3d fitVolumeSize = fitBoundingBox.getSize();
        if (fitVolumeSize.x == 0 && fitVolumeSize.y == 0 && fitVolumeSize.z == 0) {
            fitVolumeSize = new Vector3d(1, 1, 1);
        }
        Vector3d cubeVolumeSize = cubeBoundingBox.getSize();
        return (fitVolumeSize.x * fitVolumeSize.y) / (cubeVolumeSize.x * cubeVolumeSize.y);
    }

    private GaiaBoundingBox calcFitBoundingBox(GaiaPointCloud pointCloud) {
        GaiaBoundingBox fitBoundingBox = new GaiaBoundingBox();
        pointCloud.getLasPoints().forEach(point -> {
            fitBoundingBox.addPoint(point.getPosition());
        });
        return fitBoundingBox;
    }

    private GaiaBoundingBox calcApproximateFitBoundingBox(GaiaPointCloud pointCloud) {
        //TODO: I Think, We can improve this method later.
        // it solves performance issue when point cloud has too many points.
        // if recalculating full bounding box is faster, we can use calcFitBoundingBox method directly.

        // approximate fitting box
        int approximatePointCount = 10;
        GaiaBoundingBox fitBoundingBox = new GaiaBoundingBox();
        long totalPoints = pointCloud.getPointCount();
        Vector3d position = new Vector3d();
        if (totalPoints <= approximatePointCount) {
            pointCloud.getLasPoints().forEach(point -> {
                position.set(point.getPosition());
                fitBoundingBox.addPoint(position);
            });
            return fitBoundingBox;
        }
        long step = Math.max(1, totalPoints / approximatePointCount);
        for (long i = 0; i < totalPoints; i += step) {
            List<GaiaLasPoint> lasPoints = pointCloud.getLasPoints();
            GaiaLasPoint point = lasPoints.get((int)i);
            position.set(point.getPosition());
            fitBoundingBox.addPoint(position);
        }

        return fitBoundingBox;
    }

    private static GaiaBoundingBox toCube(GaiaBoundingBox box) {
        // degree with height
        Vector3d min = box.getMinPosition();
        Vector3d max = box.getMaxPosition();
        Vector3d center = box.getFloorCenter();

        // degree -> world coordinate
        Vector3d minPosition = GlobeUtils.geographicToCartesianWgs84(min);
        Vector3d maxPosition = GlobeUtils.geographicToCartesianWgs84(max);
        Vector3d centerCartesian = GlobeUtils.geographicToCartesianWgs84(center);
        Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerCartesian);
        Matrix4d transformMatrixInv = new Matrix4d(transformMatrix).invert();

        minPosition = transformMatrixInv.transformPosition(minPosition, new Vector3d());
        maxPosition = transformMatrixInv.transformPosition(maxPosition, new Vector3d());

        double deltaX = Math.abs(maxPosition.x - minPosition.x);
        double deltaY = Math.abs(maxPosition.y - minPosition.y);
        double deltaZ = Math.abs(maxPosition.z - minPosition.z);

        double maxDelta = Math.max(deltaX, Math.max(deltaY, deltaZ));
        double halfSize = maxDelta / 2.0;

        double xOffset = maxDelta - deltaX;
        double yOffset = maxDelta - deltaY;
        double zOffset = maxDelta - deltaZ;

        Vector3d newMin = new Vector3d(minPosition.x, minPosition.y, minPosition.z);
        Vector3d newMax = new Vector3d(maxPosition.x + xOffset, maxPosition.y + yOffset, maxPosition.z + zOffset);

        box = new GaiaBoundingBox();
        // world coordinate -> degree
        Vector3d transformedMin = transformMatrix.transformPosition(newMin);
        Vector3d transformedMax = transformMatrix.transformPosition(newMax);

        Vector3d cubeLonLatMin = GlobeUtils.cartesianToGeographicWgs84(transformedMin);
        Vector3d cubeLonLatMax = GlobeUtils.cartesianToGeographicWgs84(transformedMax);

        box.addPoint(cubeLonLatMin);
        box.addPoint(cubeLonLatMax);

        return box;
    }

    private void minimizeAllPointCloud(int index, int maximumIndex, List<GaiaPointCloud> allPointClouds) {
        int size = allPointClouds.size();

        long totalPoints = allPointClouds.stream()
                .mapToLong(GaiaPointCloud::getPointCount)
                .sum();
        log.debug("[Tile][{}/{}][Minimize][TotalPointClouds:{}][TotalPoints:{}]", index, maximumIndex, size, totalPoints);
        allPointClouds.forEach(pointCloud -> {
            File tempPath = new File(GlobalOptions.getInstance().getTempPath());
            File tempFile = new File(tempPath, UUID.randomUUID().toString());
            pointCloud.minimize(tempFile);
        });
    }
}