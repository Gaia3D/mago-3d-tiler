package com.gaia3d.process.tileprocess.tile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.process.ProcessOptions;
import com.gaia3d.process.TilerOptions;
import com.gaia3d.process.tileprocess.Tiler;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;
import com.gaia3d.process.tileprocess.tile.tileset.asset.*;
import com.gaia3d.process.tileprocess.tile.tileset.node.BoundingVolume;
import com.gaia3d.process.tileprocess.tile.tileset.node.Content;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import com.gaia3d.util.GlobeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PointCloudTiler implements Tiler {
    private static final int DEFUALT_MAX_COUNT = 20000;

    private final CommandLine command;
    private final TilerOptions options;

    public PointCloudTiler(TilerOptions tilerOptions, CommandLine command) {
        this.options = tilerOptions;
        this.command = command;
    }

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
        root.setTransformMatrix(transformMatrix);
        root.setGeometricError(geometricError);

        try {
            createRootNode(root, tileInfos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Asset asset = createAsset();
        Tileset tileset = new Tileset();
        tileset.setGeometricError(geometricError);
        tileset.setAsset(asset);
        tileset.setRoot(root);
        return tileset;
    }

    public void writeTileset(Tileset tileset) {
        File tilesetFile = this.options.getOutputPath().resolve("tileset.json").toFile();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tilesetFile))) {
            String result = objectMapper.writeValueAsString(tileset);
            writer.write(result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private double calcGeometricError(List<TileInfo> tileInfos) {
        return tileInfos.stream().mapToDouble(tileInfo -> {
            GaiaBoundingBox boundingBox = tileInfo.getPointCloud().getGaiaBoundingBox();
            return boundingBox.getLongestDistance();
        }).max().orElse(0.0d);
    }

    private double calcGeometricError(GaiaPointCloud pointCloud) {
        GaiaBoundingBox boundingBox = pointCloud.getGaiaBoundingBox();
        return boundingBox.getLongestDistance();
    }

    private GaiaBoundingBox calcBoundingBox(List<TileInfo> tileInfos) {
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
                .toList();
        int index = 0;
        for (GaiaPointCloud pointCloud : pointClouds) {
            pointCloud.setCode((index++) + "");
            createNode(parentNode, pointCloud, 16.0d);
        }
    }

    private void createNode(Node parentNode, GaiaPointCloud pointCloud, double geometricError) {
        int vertexLength = pointCloud.getVertices().size();

        int maxPoints = command.hasOption(ProcessOptions.MAX_POINTS.getArgName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MAX_POINTS.getArgName())) : DEFUALT_MAX_COUNT;

        List<GaiaPointCloud> divided = pointCloud.divideChunkSize(maxPoints);
        GaiaPointCloud selfPointCloud = divided.get(0);
        GaiaPointCloud remainPointCloud = divided.get(1);

        GaiaBoundingBox childBoundingBox = selfPointCloud.getGaiaBoundingBox();
        Matrix4d transformMatrix = getTransformMatrix(childBoundingBox);

        //Matrix4d transformMatrix = new Matrix4d();
        //transformMatrix.identity();
        rotateX90(transformMatrix);
        BoundingVolume boundingVolume = new BoundingVolume(childBoundingBox);

        double geometricErrorCalc = calcGeometricError(selfPointCloud);

        Node childNode = new Node();
        childNode.setParent(parentNode);
        childNode.setTransformMatrix(transformMatrix);
        childNode.setBoundingBox(childBoundingBox);
        childNode.setBoundingVolume(boundingVolume);
        childNode.setRefine(Node.RefineType.ADD);
        childNode.setChildren(new ArrayList<>());
        childNode.setNodeCode(parentNode.getNodeCode() + pointCloud.getCode());
        childNode.setGeometricError(geometricErrorCalc/2);
        childNode.setGeometricError(geometricError);

        TileInfo selfTileInfo = TileInfo.builder()
                .pointCloud(selfPointCloud)
                .boundingBox(childBoundingBox)
                .build();
        List<TileInfo> tileInfos = new ArrayList<>();
        tileInfos.add(selfTileInfo);

        ContentInfo contentInfo = new ContentInfo();
        contentInfo.setName("gaiaPointcloud");
        contentInfo.setLod(LevelOfDetail.LOD0);
        contentInfo.setBoundingBox(childBoundingBox);
        contentInfo.setNodeCode(childNode.getNodeCode());
        contentInfo.setTileInfos(tileInfos);

        Content content = new Content();
        content.setUri("data/" + childNode.getNodeCode() + ".pnts");
        content.setContentInfo(contentInfo);
        childNode.setContent(content);

        parentNode.getChildren().add(childNode);
        log.info("[ContentNode][{}] Point cloud nodes calculated.",childNode.getNodeCode());

        if (vertexLength > 0) { // vertexLength > DEFUALT_MAX_COUNT
            List<GaiaPointCloud> distributes = remainPointCloud.distributeOct();
            distributes.forEach(distribute -> {
                if (!distribute.getVertices().isEmpty()) {
                    createNode(childNode, distribute, geometricError/2);
                }
            });
        } else {
            return;
        }
    }

    private void rotateX90(Matrix4d matrix) {
        Matrix4d rotationMatrix = new Matrix4d();
        rotationMatrix.identity();
        rotationMatrix.rotateX(Math.toRadians(-90));
        matrix.mul(rotationMatrix, matrix);
    }

    private Matrix4d getTransformMatrix(GaiaBoundingBox boundingBox) {
        Vector3d center = boundingBox.getCenter();
        double[] cartesian = GlobeUtils.geographicToCartesianWgs84(center.x, center.y, center.z);
        return GlobeUtils.normalAtCartesianPointWgs84(cartesian[0], cartesian[1], cartesian[2]);
    }

    private Asset createAsset() {
        Asset asset = new Asset();
        Extras extras = new Extras();
        Cesium cesium = new Cesium();
        Ion ion = new Ion();
        List<Credit> credits = new ArrayList<>();
        Credit credit = new Credit();
        credit.setHtml("<html>Gaia3D</html>");
        credits.add(credit);
        cesium.setCredits(credits);
        extras.setIon(ion);
        extras.setCesium(cesium);
        asset.setExtras(extras);
        return asset;
    }

    private Node createRoot() {
        Node root = new Node();
        root.setParent(root);
        root.setNodeCode("R");
        root.setRefine(Node.RefineType.ADD);
        root.setChildren(new ArrayList<>());
        return root;
    }
}