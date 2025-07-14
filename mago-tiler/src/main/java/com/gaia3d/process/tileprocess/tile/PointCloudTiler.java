package com.gaia3d.process.tileprocess.tile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.exception.TileProcessingException;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.tileprocess.Tiler;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;
import com.gaia3d.process.tileprocess.tile.tileset.TilesetV2;
import com.gaia3d.process.tileprocess.tile.tileset.asset.*;
import com.gaia3d.process.tileprocess.tile.tileset.node.BoundingVolume;
import com.gaia3d.process.tileprocess.tile.tileset.node.Content;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import com.gaia3d.util.GlobeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.proj4j.BasicCoordinateTransform;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class PointCloudTiler extends DefaultTiler implements Tiler {

    private double rootGeometricError = 0.0d;
    private final int MAXIMUM_DEPTH = 12;

    @Override
    public Tileset run(List<TileInfo> tileInfos) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        GaiaBoundingBox globalBoundingBox = calcBoundingBox(tileInfos);

        CoordinateReferenceSystem source = globalOptions.getCrs();
        Vector3d originalMinPosition = globalBoundingBox.getMinPosition();
        Vector3d originalMaxPosition = globalBoundingBox.getMaxPosition();

        BasicCoordinateTransform transformer = new BasicCoordinateTransform(source, GlobeUtils.wgs84);
        ProjCoordinate transformedMinCoordinate = transformer.transform(new ProjCoordinate(originalMinPosition.x, originalMinPosition.y, originalMinPosition.z), new ProjCoordinate());
        Vector3d minPosition = new Vector3d(transformedMinCoordinate.x, transformedMinCoordinate.y, originalMinPosition.z);
        ProjCoordinate transformedMaxCoordinate = transformer.transform(new ProjCoordinate(originalMaxPosition.x, originalMaxPosition.y, originalMaxPosition.z), new ProjCoordinate());
        Vector3d maxPosition = new Vector3d(transformedMaxCoordinate.x, transformedMaxCoordinate.y, originalMaxPosition.z);

        GaiaBoundingBox transformedBoundingBox = new GaiaBoundingBox();
        transformedBoundingBox.addPoint(minPosition);
        transformedBoundingBox.addPoint(maxPosition);

        Matrix4d transformMatrix = getTransformMatrix(globalBoundingBox);
        rotateX90(transformMatrix);

        double geometricError = calcGeometricErrorFromWgs84(transformedBoundingBox);
        double minimumGeometricError = 64.0;
        double maximumGeometricError = 1000.0;
        if (geometricError < minimumGeometricError) {
            geometricError = minimumGeometricError;
        } else if (geometricError > maximumGeometricError) {
            geometricError = maximumGeometricError;
        }
        rootGeometricError = geometricError;

        Node root = createRoot();
        root.setNodeCode("R");
        root.setBoundingBox(transformedBoundingBox);
        root.setRefine(Node.RefineType.ADD);

        BoundingVolume boundingVolume = new BoundingVolume(transformedBoundingBox);

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
        tileset.setGeometricError(geometricError);
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

    private double calcGeometricError(GaiaPointCloud pointCloud) {
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
    protected GaiaBoundingBox calcBoundingBox(List<TileInfo> tileInfos) {
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
                .collect(Collectors.toList());
        int index = 0;
        int maximumIndex = pointClouds.size();
        int rootPointLimit = globalOptions.getMaximumPointPerTile() / 16;
        for (GaiaPointCloud pointCloud : pointClouds) {
            pointCloud.setCode((index++) + "");
            pointCloud.maximize();
            List<GaiaPointCloud> allPointClouds = new ArrayList<>();
            createNode(allPointClouds, index, maximumIndex, parentNode, pointCloud, rootPointLimit, 0);
            minimizeAllPointCloud(index, maximumIndex, allPointClouds);
        }
    }

    private void createNode(List<GaiaPointCloud> allPointClouds, int index, int maximumIndex, Node parentNode, GaiaPointCloud pointCloud, int pointLimit, int depth) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();

        allPointClouds.add(pointCloud);
        int vertexLength = pointCloud.getVertices().size();
        List<GaiaPointCloud> divided = pointCloud.divideChunkSize(pointLimit);
        GaiaPointCloud selfPointCloud = divided.get(0);
        allPointClouds.add(selfPointCloud);
        GaiaPointCloud remainPointCloud = divided.get(1);

        GaiaBoundingBox childBoundingBox = selfPointCloud.getGaiaBoundingBox();
        Vector3d originalMinPosition = childBoundingBox.getMinPosition();
        Vector3d originalMaxPosition = childBoundingBox.getMaxPosition();

        CoordinateReferenceSystem source = globalOptions.getCrs();
        BasicCoordinateTransform transformer = new BasicCoordinateTransform(source, GlobeUtils.wgs84);
        ProjCoordinate transformedMinCoordinate = transformer.transform(new ProjCoordinate(originalMinPosition.x, originalMinPosition.y, originalMinPosition.z), new ProjCoordinate());
        Vector3d minPosition = new Vector3d(transformedMinCoordinate.x, transformedMinCoordinate.y, originalMinPosition.z);
        ProjCoordinate transformedMaxCoordinate = transformer.transform(new ProjCoordinate(originalMaxPosition.x, originalMaxPosition.y, originalMaxPosition.z), new ProjCoordinate());
        Vector3d maxPosition = new Vector3d(transformedMaxCoordinate.x, transformedMaxCoordinate.y, originalMaxPosition.z);

        GaiaBoundingBox transformedBoundingBox = new GaiaBoundingBox();
        transformedBoundingBox.addPoint(minPosition);
        transformedBoundingBox.addPoint(maxPosition);

        Matrix4d transformMatrix = getTransformMatrix(childBoundingBox);
        rotateX90(transformMatrix);
        BoundingVolume boundingVolume = new BoundingVolume(transformedBoundingBox);

        int attenuation;
        if (depth < 2) {
            attenuation = 48;
        } else if (depth < 3) {
            attenuation = 64;
        } else if (depth < 4) {
            attenuation = 80;
        } else if (depth < 5) {
            attenuation = 96;
        } else {
            attenuation = 128;
        }

        double calcValue = (1000 / rootGeometricError) * attenuation;
        double maximumGeometricError = 36.0;
        double geometricErrorCalc = calcGeometricErrorFromWgs84(transformedBoundingBox);
        double calculatedGeometricError = geometricErrorCalc / calcValue;
        if (calculatedGeometricError > maximumGeometricError) {
            calculatedGeometricError = maximumGeometricError;
        } else {
            calculatedGeometricError = Math.floor(calculatedGeometricError);
        }

        if (geometricErrorCalc < 25 && calculatedGeometricError < 0.1) {
            calculatedGeometricError = 0.1;
        } else if (calculatedGeometricError < 1.0) {
            calculatedGeometricError = 1.0;
        }

        Node childNode = new Node();
        childNode.setParent(parentNode);
        childNode.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
        childNode.setBoundingBox(childBoundingBox);
        childNode.setBoundingVolume(boundingVolume);
        childNode.setRefine(Node.RefineType.ADD);
        childNode.setChildren(new ArrayList<>());
        childNode.setNodeCode(parentNode.getNodeCode() + pointCloud.getCode());
        childNode.setGeometricError(calculatedGeometricError);

        TileInfo selfTileInfo = TileInfo.builder()
                .pointCloud(selfPointCloud)
                .boundingBox(childBoundingBox)
                .build();
        List<TileInfo> tileInfos = new ArrayList<>();
        tileInfos.add(selfTileInfo);

        ContentInfo contentInfo = new ContentInfo();
        contentInfo.setName("points-cloud" + childNode.getNodeCode());
        contentInfo.setLod(LevelOfDetail.LOD0);
        contentInfo.setBoundingBox(childBoundingBox);
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

        parentNode.getChildren().add(childNode);
        log.info("[Tile][{}/{}][ContentNode][{}]", index, maximumIndex, childNode.getNodeCode());

        if (vertexLength > 0) {
            List<GaiaPointCloud> distributes = remainPointCloud.distribute();
            distributes.forEach(distribute -> {
                if (!distribute.getVertices().isEmpty()) {
                    int newPointLimit = (int) (pointLimit * 1.75d); // (/3)
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

    private void minimizeAllPointCloud(int index, int maximumIndex, List<GaiaPointCloud> allPointClouds) {
        int size = allPointClouds.size();
        AtomicInteger atomicInteger = new AtomicInteger(1);
        printJvmMemory();
        allPointClouds.forEach(pointCloud -> {
            File tempPath = new File(GlobalOptions.getInstance().getOutputPath(), "temp");
            File tempFile = new File(tempPath, UUID.randomUUID().toString());
            pointCloud.minimize(tempFile);
            log.info("[Tile][{}/{}][Minimize][{}/{}] Write temp file : {}", index, maximumIndex, atomicInteger.getAndIncrement(), size, tempFile.getName());
        });
        //System.gc();
    }

    private void printJvmMemory() {
        String javaHeapSize = System.getProperty("java.vm.name") + " " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "MB";
        long maxMem = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        long totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024;
        long usedMem = totalMem - freeMem;
        // 퍼센트
        double pct = usedMem * 100.0 / maxMem;
        log.info("[Tile] Java Heap Size: {} / MaxMem: {}MB / TotalMem: {}MB / FreeMem: {}MB / UsedMem: {}MB / Pct: {}%", javaHeapSize, maxMem, totalMem, freeMem, usedMem, pct);
    }
}