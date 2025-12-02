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
            log.info("[Tile][{}/{}] original Point Count : {}", (index + 1), maximumIndex, pointCloud.getPointCount());
            pointCloud.setCode((index++) + "");
            pointCloud.maximize(false);

            GaiaBoundingBox box = pointCloud.getGaiaBoundingBox();
            box = toCube(box);
            pointCloud.setGaiaBoundingBox(box);

            List<GaiaPointCloud> allPointClouds = new ArrayList<>();
            createNode(allPointClouds, index, maximumIndex, parentNode, pointCloud, rootPointLimit, 0);
            pointCloud.clearPoints();
            minimizeAllPointCloud(index, maximumIndex, allPointClouds);
            log.info("[Tile][{}/{}] completed.", index, maximumIndex);
        }
    }


    private void createNode(List<GaiaPointCloud> allPointClouds, int index, int maximumIndex, Node parentNode, GaiaPointCloud pointCloud, int pointLimit, int depth) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();

        GaiaBoundingBox cubeBoundingBox = pointCloud.getGaiaBoundingBox();
        GaiaBoundingBox fitBoundingBox = new GaiaBoundingBox();
        long allPointsCount = pointCloud.getLasPoints().size();
        pointCloud.getLasPoints().forEach(point -> {
            Vector3d position = point.getVec3Position();
            fitBoundingBox.addPoint(position);
        });

        Vector3d fitVolumeSize = fitBoundingBox.getSize();
        if (allPointsCount == 0) {
            log.warn("[Tile][{}/{}][Depth:{}] No points in the point cloud, skipping node creation.", index, maximumIndex, depth);
            return;
        } else if (allPointsCount == 1 || (fitVolumeSize.x == 0 && fitVolumeSize.y == 0 && fitVolumeSize.z == 0)) {
            log.warn("[Tile][{}/{}][Depth:{}] Single point or zero volume size detected, adjusting fit volume size to (1,1,1).", index, maximumIndex, depth);
            fitVolumeSize = new Vector3d(1, 1, 1);
        }

        Vector3d cubeVolumeSize = cubeBoundingBox.getSize();
        double dimensionRatio = (fitVolumeSize.x * fitVolumeSize.y) / (cubeVolumeSize.x * cubeVolumeSize.y);
        int chunkPointLimit = (int) (pointLimit * dimensionRatio);
        log.debug("[Tile][{}/{}][Depth:{}] Child Bounding Box Dimension Ratio: {} ({} x {} / {} x {}) => Chunk Point Limit: {}", index, maximumIndex, depth, dimensionRatio,
                String.format("%.2f", fitVolumeSize.x), String.format("%.2f", fitVolumeSize.y),
                String.format("%.2f", cubeVolumeSize.x), String.format("%.2f", cubeVolumeSize.y),
                chunkPointLimit);

        long vertexLength = pointCloud.getLasPoints().size();
        List<GaiaPointCloud> divided = pointCloud.divideChunkSize(chunkPointLimit);
        GaiaPointCloud selfPointCloud = divided.get(0);
        GaiaPointCloud remainPointCloud = divided.get(1);

        Matrix4d transformMatrix = getTransformMatrixFromCartographic(fitBoundingBox);
        rotateX90(transformMatrix);
        BoundingVolume boundingVolume = new BoundingVolume(fitBoundingBox, BoundingVolume.BoundingVolumeType.REGION);

        double geographicError = calcGeometricErrorFromDegreeBoundingBox(cubeBoundingBox) / 2;
        double pointsFactor = pointLimit < 1000 ? 1 : ((double) 1000 / pointLimit);
        double calculatedGeometricError = geographicError * pointsFactor;

        if (calculatedGeometricError < 0.01) {
            calculatedGeometricError = 0.01;
        } else if (calculatedGeometricError < 0.1) {
            calculatedGeometricError = 0.1;
        } else if (calculatedGeometricError > 0.5 && geographicError < 1.0) {
            calculatedGeometricError = 1.0;
        }

        Node childNode = new Node();
        childNode.setParent(parentNode);
        childNode.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
        childNode.setBoundingBox(cubeBoundingBox);
        childNode.setBoundingVolume(boundingVolume);
        childNode.setRefine(Node.RefineType.ADD);
        childNode.setChildren(new ArrayList<>());
        childNode.setNodeCode(parentNode.getNodeCode() + pointCloud.getCode());
        childNode.setGeometricError(calculatedGeometricError);

        TileInfo selfTileInfo = TileInfo.builder()
                .pointCloud(selfPointCloud)
                .boundingBox(cubeBoundingBox)
                .build();
        List<TileInfo> tileInfos = new ArrayList<>();

        tileInfos.add(selfTileInfo);

        ContentInfo contentInfo = new ContentInfo();
        contentInfo.setName("points-cloud" + childNode.getNodeCode());
        contentInfo.setLod(LevelOfDetail.LOD0);
        contentInfo.setBoundingBox(cubeBoundingBox);
        contentInfo.setNodeCode(childNode.getNodeCode());
        contentInfo.setTileInfos(tileInfos);
        allPointClouds.add(selfPointCloud);

        Content content = new Content();
        if (globalOptions.getTilesVersion().equals("1.0")) {
            content.setUri("data/" + childNode.getNodeCode() + ".pnts");
        } else {
            content.setUri("data/" + childNode.getNodeCode() + ".glb");
        }
        content.setContentInfo(contentInfo);
        childNode.setContent(content);

        parentNode.getChildren().add(childNode);
        log.info("[Tile][{}/{}][ContentNode][{}] Octree Tiling", index, maximumIndex, childNode.getNodeCode());

        if (remainPointCloud.getPointCount() > 0) {
            List<GaiaPointCloud> distributes = remainPointCloud.distribute();
            long distributeCount = distributes.stream()
                    .mapToLong(GaiaPointCloud::getPointCount)
                    .sum();

            log.debug("[Tile][{}/{}][Distribute][Depth:{}][Distributes:{}]", index, maximumIndex, depth, distributes.size());
            log.debug("[Tile][{}/{}][Distribute][Depth:{}][OriginalPoints:{}]", index, maximumIndex, depth, vertexLength);
            log.debug("[Tile][{}/{}][Distribute][Depth:{}][selfPoints:{}]", index, maximumIndex, depth, selfPointCloud.getPointCount());
            log.debug("[Tile][{}/{}][Distribute][Depth:{}][RemainPoints:{}]", index, maximumIndex, depth, remainPointCloud.getPointCount());

            if (vertexLength != selfPointCloud.getPointCount() + distributeCount) {
                log.warn("- [Tile][{}/{}][Distribute][Depth:{}] Point count mismatch! {} != {} + {}", index, maximumIndex, depth, vertexLength, selfPointCloud.getPointCount(), remainPointCloud.getPointCount());
            }

            distributes.forEach(distribute -> {
                if (!distribute.getLasPoints().isEmpty()) {
                    int newPointLimit = (int) (pointLimit * 2.0f);
                    if (newPointLimit > globalOptions.getMaximumPointPerTile()) {
                        newPointLimit = globalOptions.getMaximumPointPerTile();
                    }

                    int newDepth = depth + 1;
                    if (newDepth < MAXIMUM_DEPTH) {
                        createNode(allPointClouds, index, maximumIndex, childNode, distribute, newPointLimit, newDepth);
                    } else {
                        log.info("[Tile][{}/{}][DepthLimit][{}]", index, maximumIndex, newDepth);
                    }
                }
            });
        }
    }


    public static GaiaBoundingBox toCube(GaiaBoundingBox box) {
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
            log.info("[Tile][{}/{}][Minimize][{}/{}] Write temp file : {}", index, maximumIndex, atomicInteger.getAndIncrement(), size, tempFile.getName());
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