package com.gaia3d.process.tileprocess.tile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.exception.TileProcessingException;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.pointcloud.GaiaPointCloudOld;
import com.gaia3d.command.mago.GlobalOptions;
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

    private double rootGeometricError = 0.0d;
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
        rootGeometricError = geographicError;

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

    private double calcGeometricError(GaiaBoundingBox boundingBox) {
        return boundingBox.getLongestDistance();
    }

    // convert to meters
    private double calcGeometricErrorFromWgs84(GaiaBoundingBox boundingBox) {
        Vector3d minPosition = boundingBox.getMinPosition();
        Vector3d maxPosition = boundingBox.getMaxPosition();

        Vector3d minTransformed = GlobeUtils.geographicToCartesianWgs84(minPosition);
        Vector3d maxTransformed = GlobeUtils.geographicToCartesianWgs84(maxPosition);
        return minTransformed.distance(maxTransformed);
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
            //pointCloud.maximize(false);

            log.info("[Tile][{}/{}] original Point Count : {}", (index + 1), maximumIndex, pointCloud.getPointCount());
            pointCloud.setCode((index++) + "");
            List<GaiaPointCloud> allPointClouds = new ArrayList<>();
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
                    rootTile = createNode(allPointClouds, parentNode, null, chunk, rootPointLimit, 0);
                } else {
                    Node firstChildNode = parentNode.getChildren().getFirst();
                    log.info("[Tile][{}/{}][{}/{}][Chunk {}/{}] Expanding Nodes", index, maximumIndex, index, maximumIndex, (i + 1), chunkCount);
                    expandNode(allPointClouds, firstChildNode, rootTile, chunk, rootPointLimit, 0);
                }

                long rootAllPointCount = rootTile.getAllPointCount();
                int maximumDepth = rootTile.getMaxDepth();
                int nodeCount = rootTile.getNodeCount();
                log.info("[Tile][{}/{}][{}/{}][Chunk {}/{}] Created All Point Count : {}, Max Depth : {}, Node Count : {}", index, maximumIndex, index, maximumIndex, (i + 1), chunkCount, rootAllPointCount, maximumDepth, nodeCount);
                chunk.clearPoints();
            }
            List<GaiaPointCloud> childrenPointClouds = rootTile.getAllLeaves();
            minimizeAllPointCloud(index, maximumIndex, childrenPointClouds);
            log.info("[Tile][{}/{}] completed.", index, maximumIndex);
        }
    }

    private void expandNode(List<GaiaPointCloud> allPointClouds, Node currentNode, GaiaPointCloud source, GaiaPointCloud target, int pointLimit, int depth) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        GaiaBoundingBox cubeBoundingBox = source.getGaiaBoundingBox();
        GaiaBoundingBox fitBoundingBox = calcFitBoundingBox(target);
        double dimensionRatio = calcDimensionRatio(fitBoundingBox, cubeBoundingBox);
        int chunkPointLimit = (int) (pointLimit * dimensionRatio);

        boolean hasNodeChildren = currentNode.getChildren() != null && !currentNode.getChildren().isEmpty();
        boolean hasChildren = source.getChildren() != null && !source.getChildren().isEmpty();

        if (hasChildren != hasNodeChildren) {
            log.error("[ERROR] Source has children but current node has no children. Source code: {}, Current node code: {}", source.getCode(), currentNode.getNodeCode());
            throw new TileProcessingException("Source has children but current node has no children.");
        }

        if (hasChildren && hasNodeChildren) {
            // 자식노드 있음
            // 자식노드 있으면 본인 노드는 꽉찬상태
            List<GaiaPointCloud> children = source.getChildren();
            long remainPointCount = source.getLimitPointCount() - source.getPointCount();
            List<GaiaPointCloud> divided = target.divideChunkSize((int) remainPointCount);

            GaiaPointCloud remainPointCloud = divided.getLast();
            if (remainPointCloud.getPointCount() > 0) {
                // 자식노드 추가/수정
                List<GaiaPointCloud> distributes = remainPointCloud.distribute();
                for (GaiaPointCloud distribute : distributes) {
                    if (distribute.getPointCount() < 1) {
                        continue;
                    }

                    GaiaPointCloud sameCodeChild = children.stream()
                            .filter(child -> child.getCode().equals(distribute.getCode()))
                            .findFirst()
                            .orElse(null);
                    Node childNode = currentNode.getChildren().stream()
                            .filter(node -> node.getNodeCode().equals(currentNode.getNodeCode() + distribute.getCode()))
                            .findFirst()
                            .orElse(null);
                    if (childNode == null) {
                        childNode = createChildNode(currentNode, getTransformMatrixFromCartographic(fitBoundingBox), distribute, cubeBoundingBox, fitBoundingBox, calcChildrenGeometricError(cubeBoundingBox, pointLimit));
                        currentNode.getChildren().add(childNode);
                        log.warn("[WARN] Child node not found for code: {}. Created new child node: {}", distribute.getCode(), childNode.getNodeCode());
                    }

                    if (sameCodeChild != null) {
                        /*if (childNode == null) {
                            log.error("[ERROR] Child node is null for code: {}", distribute.getCode());
                            continue;
                        }*/

                        // 동일 코드 자식노드 있음
                        int newPointLimit = (int) (pointLimit * 2.0f);
                        if (newPointLimit > globalOptions.getMaximumPointPerTile()) {
                            newPointLimit = globalOptions.getMaximumPointPerTile();
                        }

                        int newDepth = depth + 1;
                        if (newDepth < MAXIMUM_DEPTH) {
                            expandNode(allPointClouds, childNode, sameCodeChild, distribute, newPointLimit, newDepth);
                        }
                    } else {
                        int newPointLimit = (int) (pointLimit * 2.0f);
                        if (newPointLimit > globalOptions.getMaximumPointPerTile()) {
                            newPointLimit = globalOptions.getMaximumPointPerTile();
                        }
                        int newDepth = depth + 1;
                        if (newDepth < MAXIMUM_DEPTH) {
                            boolean hasNewChildren = source.getChildren() != null && !source.getChildren().isEmpty();
                            if (hasNewChildren) {
                                expandNode(allPointClouds, currentNode, source, distribute, newPointLimit, newDepth);
                            } else {
                                createNode(allPointClouds, currentNode, source, distribute, newPointLimit, newDepth);
                            }
                        }
                    }
                }
            }
        } else {
            // 자식노드 없음
            // 본인 노드가 꽉차지 않음
            List<GaiaPointCloud> divided;
            if (source != null) {
                long remainPointCount = source.getLimitPointCount() - source.getPointCount();
                divided = target.divideChunkSize((int) remainPointCount);
                if (remainPointCount > 0) {
                    GaiaPointCloud newSelfPointCloud = divided.getFirst();
                    source.combine(newSelfPointCloud);
                }
            } else {
                divided = target.divideChunkSize(chunkPointLimit);
            }

            // 자식노드 추가 생성
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
                        createNode(allPointClouds, currentNode, source, distribute, newPointLimit, newDepth);
                    }
                }
            }
        }
    }

    private GaiaPointCloud createNode(List<GaiaPointCloud> allPointClouds, Node parentNode, GaiaPointCloud parent, GaiaPointCloud pointCloud, int pointLimit, int depth) {
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
        Node childNode = createChildNode(parentNode, transformMatrix, selfPointCloud, cubeBoundingBox, fitBoundingBox, calculatedGeometricError);

        Node existingChild = parentNode.getChildren().stream()
                .filter(node -> node.getNodeCode().equals(childNode.getNodeCode()))
                .findFirst()
                .orElse(null);
        if (existingChild != null) {
            log.warn("[WARN] Parent node already has children. Adding another child node: {}", childNode.getNodeCode());
        }
        parentNode.getChildren().add(childNode);
        allPointClouds.add(selfPointCloud);

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
                    createNode(allPointClouds, childNode, selfPointCloud, distribute, newPointLimit, newDepth);
                }
            }
        }
        return selfPointCloud;
    }


    private Node createChildNode(Node parentNode, Matrix4d transformMatrix, GaiaPointCloud pointCloud, GaiaBoundingBox cubeBoundingBox, GaiaBoundingBox fitBoundingBox, double calculatedGeometricError) {
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
            log.error("[ERROR] Parent node is null for creating child node.");
        }
        childNode.setNodeCode(parentNode.getNodeCode() + pointCloud.getCode());
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
        childNode.setContent(content);

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
            Vector3d position = point.getVec3Position();
            fitBoundingBox.addPoint(position);
        });
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

        //printJvmMemory();
        long totalPoints = allPointClouds.stream()
                .mapToLong(GaiaPointCloud::getPointCount)
                .sum();
        log.info("[Tile][{}/{}][Minimize][TotalPointClouds:{}][TotalPoints:{}]", index, maximumIndex, size, totalPoints);
        AtomicInteger atomicInteger = new AtomicInteger(1);
        allPointClouds.forEach(pointCloud -> {
            File tempPath = new File(GlobalOptions.getInstance().getTempPath());
            File tempFile = new File(tempPath, UUID.randomUUID().toString());
            pointCloud.minimize(tempFile);
            //log.info("[Tile][{}/{}][Minimize][{}/{}] Write temp file : {}", index, maximumIndex, atomicInteger.getAndIncrement(), size, tempFile.getName());
        });
    }

    private void printJvmMemory() {
        String javaHeapSize = System.getProperty("java.vm.name") + " " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "MB";
        long maxMem = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        long totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024;
        long usedMem = totalMem - freeMem;
        double pct = usedMem * 100.0 / maxMem;
        log.info("[Tile] Java Heap Size: {} / MaxMem: {}MB / TotalMem: {}MB / FreeMem: {}MB / UsedMem: {}MB / Pct: {}%", javaHeapSize, maxMem, totalMem, freeMem, usedMem, pct);
    }
}