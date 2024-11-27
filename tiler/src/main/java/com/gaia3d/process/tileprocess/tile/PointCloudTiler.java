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
    @Override
    public Tileset run(List<TileInfo> tileInfos) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        GaiaBoundingBox globalBoundingBox = calcBoundingBox(tileInfos);

       /* double minX = globalBoundingBox.getMinX();
        double maxX = globalBoundingBox.getMaxX();
        double minY = globalBoundingBox.getMinY();
        double maxY = globalBoundingBox.getMaxY();
        double minZ = globalBoundingBox.getMinZ();
        double maxZ = globalBoundingBox.getMaxZ();

        double x = (maxX - minX);
        double y = (maxY - minY);
        double maxLength = Math.max(x, y);

        double xoffset = maxLength - x;
        double yoffset = maxLength - y;
        maxX += xoffset;
        maxY += yoffset;
        GaiaBoundingBox cubeBoundingBox = new GaiaBoundingBox();
        cubeBoundingBox.addPoint(new Vector3d(minX, minY, minZ));
        cubeBoundingBox.addPoint(new Vector3d(maxX, maxY, maxZ));*/
        //globalBoundingBox = calcSquareBoundingBox(globalBoundingBox);

        CoordinateReferenceSystem source = globalOptions.getCrs();
        GaiaBoundingBox originalBoundingBox = globalBoundingBox;
        Vector3d originalMinPosition = originalBoundingBox.getMinPosition();
        Vector3d originalMaxPosition = originalBoundingBox.getMaxPosition();

        BasicCoordinateTransform transformer = new BasicCoordinateTransform(source, GlobeUtils.wgs84);
        ProjCoordinate transformedMinCoordinate = transformer.transform(new ProjCoordinate(originalMinPosition.x, originalMinPosition.y, originalMinPosition.z), new ProjCoordinate());
        Vector3d minPosition = new Vector3d(transformedMinCoordinate.x, transformedMinCoordinate.y, originalMinPosition.z);
        ProjCoordinate transformedMaxCoordinate = transformer.transform(new ProjCoordinate(originalMaxPosition.x, originalMaxPosition.y, originalMaxPosition.z), new ProjCoordinate());
        Vector3d maxPosition = new Vector3d(transformedMaxCoordinate.x, transformedMaxCoordinate.y, originalMaxPosition.z);

        GaiaBoundingBox transformedBoundingBox = new GaiaBoundingBox();
        transformedBoundingBox.addPoint(minPosition);
        transformedBoundingBox.addPoint(maxPosition);

        Matrix4d transformMatrix = getTransformMatrix(originalBoundingBox);
        rotateX90(transformMatrix);

        //double geometricError = calcGeometricError(tileInfos);
        //GaiaBoundingBox originalCoordinateBoundingBox = originalCoordinateBoundingBox(globalBoundingBox);
        double geometricError = calcGeometricError(originalBoundingBox);

        // diagonal
        if (geometricError < 32.0) {
            geometricError = 32.0;
        } else if (geometricError > 1000.0) {
            geometricError = 1000.0;
        }

        Node root = createRoot();
        root.setNodeCode("R");
        root.setBoundingBox(transformedBoundingBox);
        root.setRefine(Node.RefineType.ADD);

        BoundingVolume boundingVolume = new BoundingVolume(transformedBoundingBox);
        //BoundingVolume square = boundingVolume.createSqureBoundingVolume();

        // root만 큐브로
        root.setBoundingVolume(boundingVolume);
        root.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
        root.setGeometricError(geometricError / 3);

        try {
            createRootNode(root, tileInfos);
        } catch (IOException e) {
            log.error("Error : {}", e.getMessage());
            throw new TileProcessingException(e.getMessage());
        }

        Asset asset = createAsset();
        Tileset tileset = new Tileset();
        tileset.setGeometricError(geometricError);
        tileset.setAsset(asset);
        tileset.setRoot(root);
        return tileset;
    }

    public void writeTileset(Tileset tileset) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        File outputPath = new File(globalOptions.getOutputPath());
        File tilesetFile = new File(outputPath, "tileset.json");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tilesetFile))) {
            String result = objectMapper.writeValueAsString(tileset);
            log.info("[Tile][Tileset] write 'tileset.json' file.");
            writer.write(result);
            globalOptions.setTilesetSize(result.length());
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new TileProcessingException(e.getMessage());
        }
    }

    private double calcGeometricError(GaiaBoundingBox boundingBox) {
        return boundingBox.getLongestDistance();
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
        int rootPointLimit = globalOptions.getMaximumPointPerTile() / 8;
        for (GaiaPointCloud pointCloud : pointClouds) {
            pointCloud.setCode((index++) + "");
            pointCloud.maximize();
            List<GaiaPointCloud> allPointClouds = new ArrayList<>();
            createNode(allPointClouds, index, maximumIndex, parentNode, pointCloud, rootPointLimit);
            minimizeAllPointCloud(index, maximumIndex, allPointClouds);
        }
    }

    private void createNode(List<GaiaPointCloud> allPointClouds, int index, int maximumIndex, Node parentNode, GaiaPointCloud pointCloud, int pointLimit) {
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
        double maximumGeometricError = 24.0;
        double geometricErrorCalc = calcGeometricError(childBoundingBox);
        double calculatedGeometricError = geometricErrorCalc / 72;
        if (calculatedGeometricError > maximumGeometricError) {
            calculatedGeometricError = maximumGeometricError;
        } else {
            calculatedGeometricError = Math.floor(calculatedGeometricError);
        }

        if (calculatedGeometricError < 1.0) {
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
        contentInfo.setName("points-cloud");
        contentInfo.setLod(LevelOfDetail.LOD0);
        contentInfo.setBoundingBox(childBoundingBox);
        contentInfo.setNodeCode(childNode.getNodeCode());
        contentInfo.setTileInfos(tileInfos);

        Content content = new Content();
        content.setUri("data/" + childNode.getNodeCode() + ".pnts");
        content.setContentInfo(contentInfo);
        childNode.setContent(content);

        parentNode.getChildren().add(childNode);
        log.info("[Tile][{}/{}][ContentNode][{}]", index, maximumIndex, childNode.getNodeCode());

        if (vertexLength > 0) { // vertexLength > DEFUALT_MAX_COUNT
            //GaiaBoundingBox remainBoundingBox = calcSquareBoundingBox(remainPointCloud.getGaiaBoundingBox());
            //remainPointCloud.setGaiaBoundingBox(remainBoundingBox);
            List<GaiaPointCloud> distributes = remainPointCloud.distribute();
            distributes.forEach(distribute -> {
                if (!distribute.getVertices().isEmpty()) {
                    int newPointLimit = pointLimit / 3 * 5; // (5/3)
                    if (newPointLimit > globalOptions.getMaximumPointPerTile()) {
                        newPointLimit = globalOptions.getMaximumPointPerTile();
                    }
                    createNode(allPointClouds, index, maximumIndex, childNode, distribute, newPointLimit);
                }
            });
        }
    }

    private GaiaBoundingBox originalCoordinateBoundingBox(GaiaBoundingBox worldBoundingBox) {
        Vector3d minPosition = worldBoundingBox.getMinPosition();
        Vector3d maxPosition = worldBoundingBox.getMaxPosition();

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        CoordinateReferenceSystem source = globalOptions.getCrs();
        BasicCoordinateTransform transformer = new BasicCoordinateTransform(GlobeUtils.wgs84, source);
        ProjCoordinate minProjCoordinate = new ProjCoordinate(minPosition.x, minPosition.y);
        ProjCoordinate maxProjCoordinate = new ProjCoordinate(maxPosition.x, maxPosition.y);
        transformer.transform(minProjCoordinate, minProjCoordinate);
        transformer.transform(maxProjCoordinate, maxProjCoordinate);

        Vector3d localMin = new Vector3d(minProjCoordinate.x, minProjCoordinate.y, minPosition.z);
        Vector3d localMax = new Vector3d(maxProjCoordinate.x, maxProjCoordinate.y, maxPosition.z);
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        boundingBox.addPoint(localMin);
        boundingBox.addPoint(localMax);
        return boundingBox;
    }

    /*@Deprecated
    private void minimizeTreeNode(Node node) {
        List<Node> children = node.getChildren();
        children.forEach(this::minimizeTreeNode);

        Content content = node.getContent();
        if (content == null) {
            return;
        }
        ContentInfo contentInfo = content.getContentInfo();
        List<TileInfo> tileInfos = contentInfo.getTileInfos();
        tileInfos.forEach(tileInfo -> {
            GaiaPointCloud pointCloud = tileInfo.getPointCloud();
            File tempPath = new File(GlobalOptions.getInstance().getOutputPath(), "temp");
            File tempFile = new File(tempPath, UUID.randomUUID().toString());
            pointCloud.minimize(tempFile);
            log.info("[Tile][Minimize][{}]", tempFile.getName());
        });
    }*/

    private GaiaBoundingBox calcSquareBoundingBox(GaiaBoundingBox gaiaBoundingBox) {
        double minX = gaiaBoundingBox.getMinX();
        double maxX = gaiaBoundingBox.getMaxX();
        double minY = gaiaBoundingBox.getMinY();
        double maxY = gaiaBoundingBox.getMaxY();
        double minZ = gaiaBoundingBox.getMinZ();
        double maxZ = gaiaBoundingBox.getMaxZ();

        double x = (maxX - minX);
        double y = (maxY - minY);
        double maxLength = Math.max(x, y);

        double xOffset = maxLength - x;
        double yOffset = maxLength - y;
        maxX += xOffset;
        maxY += yOffset;

        /*double xOffsetHalf = xOffset / 2;
        double yOffsetHalf = yOffset / 2;
        minX -= xOffsetHalf;
        minY -= yOffsetHalf;
        maxX += xOffsetHalf;
        maxY += yOffsetHalf;*/
        GaiaBoundingBox cubeBoundingBox = new GaiaBoundingBox();
        cubeBoundingBox.addPoint(new Vector3d(minX, minY, minZ));
        cubeBoundingBox.addPoint(new Vector3d(maxX, maxY, maxZ));
        return cubeBoundingBox;
    }

    private void minimizeAllPointCloud(int index, int maximumIndex, List<GaiaPointCloud> allPointClouds) {
        int size = allPointClouds.size();
        AtomicInteger atomicInteger = new AtomicInteger(0);
        allPointClouds.forEach(pointCloud -> {
            File tempPath = new File(GlobalOptions.getInstance().getOutputPath(), "temp");
            File tempFile = new File(tempPath, UUID.randomUUID().toString());
            pointCloud.minimize(tempFile);
            log.info("[Tile][{}/{}][Minimize][{}/{}] Write temp file : {}", index, maximumIndex, atomicInteger.getAndIncrement(), size, tempFile.getName());
        });
    }
}