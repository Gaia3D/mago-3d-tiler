package com.gaia3d.process.tileprocess.tile;

import com.fasterxml.jackson.annotation.JsonInclude;
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
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class PointCloudTiler extends DefaultTiler implements Tiler {
    @Override
    public Tileset run(List<TileInfo> tileInfos) {
        double geometricError = calcGeometricError(tileInfos);

        GaiaBoundingBox globalBoundingBox = calcBoundingBox(tileInfos);

        double minX = globalBoundingBox.getMinX();
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
        cubeBoundingBox.addPoint(new Vector3d(maxX, maxY, maxZ));

        globalBoundingBox = cubeBoundingBox;

        Matrix4d transformMatrix = getTransformMatrix(globalBoundingBox);
        rotateX90(transformMatrix);

        Node root = createRoot();
        root.setBoundingBox(globalBoundingBox);
        // root만 큐브로
        root.setBoundingVolume(new BoundingVolume(globalBoundingBox));
        root.setTransformMatrix(transformMatrix, true);
        root.setGeometricError(geometricError);

        try {
            createRootNode(root, tileInfos);
        } catch (IOException e) {
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
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tilesetFile))) {
            String result = objectMapper.writeValueAsString(tileset);
            log.info("[Tiling][Tileset] write 'tileset.json' file.");
            writer.write(result);
            globalOptions.setTilesetSize(result.length());
        } catch (IOException e) {
            throw new TileProcessingException(e.getMessage());
        }
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
        BoundingVolume parentBoundingVolume = parentNode.getBoundingVolume();
        parentNode.setBoundingVolume(parentBoundingVolume);
        parentNode.setRefine(Node.RefineType.ADD);

        List<GaiaPointCloud> pointClouds = tileInfos.stream()
                .map(TileInfo::getPointCloud)
                .collect(Collectors.toList());
        int index = 0;
        for (GaiaPointCloud pointCloud : pointClouds) {
            pointCloud.setCode((index++) + "");
            createNode(parentNode, pointCloud, 16.0d);
        }
    }

    private void createNode(Node parentNode, GaiaPointCloud pointCloud, double geometricError) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        int vertexLength = pointCloud.getVertices().size();

        int pointLimit = globalOptions.getPointLimit();
        int pointScale = globalOptions.getPointScale();

        List<GaiaPointCloud> divided = pointCloud.divideChunkSize(pointLimit * pointScale);
        GaiaPointCloud selfPointCloud = divided.get(0);
        GaiaPointCloud remainPointCloud = divided.get(1);

        GaiaBoundingBox childBoundingBox = selfPointCloud.getGaiaBoundingBox();
        Matrix4d transformMatrix = getTransformMatrix(childBoundingBox);
        rotateX90(transformMatrix);
        BoundingVolume boundingVolume = new BoundingVolume(childBoundingBox);

        double geometricErrorCalc = calcGeometricError(selfPointCloud);
        double calculatedGeometricError = geometricErrorCalc / pointScale / 12;

        Node childNode = new Node();
        childNode.setParent(parentNode);
        childNode.setTransformMatrix(transformMatrix, true);
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
        log.info("[Tiling][ContentNode][{}]",childNode.getNodeCode());

        if (vertexLength > 0) { // vertexLength > DEFUALT_MAX_COUNT
            List<GaiaPointCloud> distributes = remainPointCloud.distribute();
            distributes.forEach(distribute -> {
                if (!distribute.getVertices().isEmpty()) {
                    createNode(childNode, distribute, geometricError / 2);
                }
            });
        }
    }
}