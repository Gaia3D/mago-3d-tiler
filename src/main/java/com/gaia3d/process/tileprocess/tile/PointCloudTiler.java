package com.gaia3d.process.tileprocess.tile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.converter.kml.KmlInfo;
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
import java.util.stream.Collectors;

@Slf4j
public class PointCloudTiler implements Tiler {
    private static final int DEFUALT_MAX_COUNT = 20000;
    private static final int DEFUALT_MIN_LEVEL = 0;
    private static final int DEFUALT_MAX_LEVEL = 3;

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
            double result = boundingBox.getLongestDistance();
            return result;
        }).max().orElse(0.0d);
    }

    private double calcGeometricError(GaiaPointCloud pointCloud) {
        GaiaBoundingBox boundingBox = pointCloud.getGaiaBoundingBox();
        double result = boundingBox.getLongestDistance();
        return result;
    }

    private GaiaBoundingBox calcBoundingBox(List<TileInfo> tileInfos) {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        tileInfos.forEach(tileInfo -> {
            //KmlInfo kmlInfo = tileInfo.getKmlInfo();
            //Vector3d position = kmlInfo.getPosition();
            Vector3d position = tileInfo.getPointCloud().getGaiaBoundingBox().getCenter();
            //GaiaBoundingBox localBoundingBox = tileInfo.getScene().getBoundingBox();
            GaiaBoundingBox localBoundingBox = tileInfo.getPointCloud().getGaiaBoundingBox();
            //ocalBoundingBox = localBoundingBox.convertLocalToLonlatBoundingBox(position);
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
            //parentNode.setNodeCode(parentNode.getNodeCode() + index);
            createNode(parentNode, pointCloud, 16.0d);
        }
    }

    private void createNode(Node parentNode, GaiaPointCloud pointCloud, double geometricError) {
        int vertexLength = pointCloud.getVertices().size();

        List<GaiaPointCloud> divided = pointCloud.divideChunkSize(DEFUALT_MAX_COUNT);
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
        log.info(childNode.getNodeCode());

        if (vertexLength > 0) { // vertexLength > DEFUALT_MAX_COUNT
            List<GaiaPointCloud> distributes = remainPointCloud.distributeOct();
            distributes.forEach(distribute -> {
                if (distribute.getVertices().size() > 0) {
                    createNode(childNode, distribute, geometricError/2);
                }
            });
        } else {
            return;
        }
    }

    /*private void createNode(Node parentNode, List<TileInfo> tileInfos) throws IOException {
        BoundingVolume parentBoundingVolume = parentNode.getBoundingVolume();
        boolean refineAdd = command.hasOption(ProcessOptions.REFINE_ADD.getArgName());
        int maxCount = command.hasOption(ProcessOptions.MAX_COUNT.getArgName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MAX_COUNT.getArgName())) : DEFUALT_MAX_COUNT;
        if (tileInfos.size() > maxCount) {
            List<List<TileInfo>> childrenScenes = parentBoundingVolume.distributeScene(tileInfos);
            for (int index = 0; index < childrenScenes.size(); index++) {
                List<TileInfo> childTileInfos = childrenScenes.get(index);
                Node childNode = createStructNode(parentNode, childTileInfos, index);
                if (childNode != null) {
                    parentNode.getChildren().add(childNode);
                    createNode(childNode, childTileInfos);
                }
            }
        } else if (tileInfos.size() > 1) {
            List<List<TileInfo>> childrenScenes = parentBoundingVolume.distributeScene(tileInfos);
            for (int index = 0; index < childrenScenes.size(); index++) {
                List<TileInfo> childTileInfos = childrenScenes.get(index);

                Node childNode = createContentNode(parentNode, childTileInfos, index);
                if (childNode != null) {
                    parentNode.getChildren().add(childNode);
                    Content content = childNode.getContent();
                    if (content != null && refineAdd) {
                        ContentInfo contentInfo = content.getContentInfo();
                        createNode(childNode, contentInfo.getRemainTileInfos());
                    } else {
                        createNode(childNode, childTileInfos);
                    }
                }
            }
        } else if (tileInfos.size() > 0) {
            Node childNode = createContentNode(parentNode, tileInfos, 0);
            if (childNode != null) {
                parentNode.getChildren().add(childNode);
                createNode(childNode, tileInfos);
            }
        }
    }*/

    /*private Node createStructNode(Node parentNode, List<TileInfo> tileInfos, int index) {
        if (tileInfos.size() < 1) {
            return null;
        }
        String nodeCode = parentNode.getNodeCode();
        nodeCode = nodeCode + index;
        log.info("[StructNode ][" + nodeCode + "] : {}", tileInfos.size());

        double geometricError = calcGeometricError(tileInfos);
        GaiaBoundingBox childBoundingBox = calcBoundingBox(tileInfos);
        Matrix4d transformMatrix = getTransformMatrix(childBoundingBox);
        rotateX90(transformMatrix);

        BoundingVolume boundingVolume = new BoundingVolume(childBoundingBox);

        Node childNode = new Node();
        childNode.setParent(parentNode);
        childNode.setTransformMatrix(transformMatrix);
        childNode.setBoundingVolume(boundingVolume);
        childNode.setNodeCode(nodeCode);
        childNode.setGeometricError(geometricError);
        childNode.setRefine(Node.RefineType.REPLACE);
        childNode.setChildren(new ArrayList<>());
        return childNode;
    }*/

    /*private Node createContentNode(Node parentNode, List<TileInfo> tileInfos, int index) {
        if (tileInfos.size() < 1) {
            return null;
        }

        int minLevel = command.hasOption(ProcessOptions.MIN_LOD.getArgName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MIN_LOD.getArgName())) : DEFUALT_MIN_LEVEL;
        int maxLevel = command.hasOption(ProcessOptions.MAX_LOD.getArgName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MAX_LOD.getArgName())) : DEFUALT_MAX_LEVEL;
        boolean refineAdd = command.hasOption(ProcessOptions.REFINE_ADD.getArgName());

        GaiaBoundingBox childBoundingBox = calcBoundingBox(tileInfos);
        Matrix4d transformMatrix = getTransformMatrix(childBoundingBox);
        rotateX90(transformMatrix);

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

        log.info("[ContentNode][" + nodeCode + "][{}] : {}", lod.getLevel(), tileInfos.size());

        int lodError = refineAdd ? lod.getGeometricErrorBlock() : lod.getGeometricError();
        int lodErrorDouble = lodError * 2;

        List<TileInfo> resultInfos = tileInfos;
        List<TileInfo> remainInfos = new ArrayList<>();
        resultInfos = tileInfos.stream().filter(tileInfo -> {
            double geometricError = tileInfo.getBoundingBox().getLongestDistance();
            return geometricError >= lodErrorDouble;
        }).collect(Collectors.toList());
        remainInfos = tileInfos.stream().filter(tileInfo -> {
            double geometricError = tileInfo.getBoundingBox().getLongestDistance();
            return geometricError < lodErrorDouble;
        }).collect(Collectors.toList());

        Node childNode = new Node();
        childNode.setParent(parentNode);
        childNode.setTransformMatrix(transformMatrix);
        childNode.setBoundingVolume(boundingVolume);
        childNode.setNodeCode(nodeCode);
        childNode.setGeometricError(lodError);
        childNode.setChildren(new ArrayList<>());

        childNode.setRefine(refineAdd ? Node.RefineType.ADD : Node.RefineType.REPLACE);
        //childNode.setRefine(Node.RefineType.ADD);

        if (resultInfos.size() > 0) {
            ContentInfo contentInfo = new ContentInfo();
            contentInfo.setName(nodeCode);
            contentInfo.setLod(lod);
            contentInfo.setBoundingBox(childBoundingBox);
            contentInfo.setNodeCode(nodeCode);
            contentInfo.setTileInfos(resultInfos);
            contentInfo.setRemainTileInfos(remainInfos);

            Content content = new Content();
            content.setUri("data/" + nodeCode + ".b3dm");
            content.setContentInfo(contentInfo);
            childNode.setContent(content);
        } else {
            log.error("No content : {}", nodeCode);
        }
        return childNode;
    }*/

    private LevelOfDetail getLodByNodeCode(LevelOfDetail minLod, LevelOfDetail maxLod, String nodeCode) {
        LevelOfDetail levelOfDetail = LevelOfDetail.NONE;
        int minLevel = minLod.getLevel();
        int maxLevel = maxLod.getLevel();

        String[] splitCode = nodeCode.split("C");
        if (splitCode.length > 1) {
            String contentLevel = nodeCode.split("C")[1];
            int level = maxLevel - contentLevel.length();
            if (level < minLevel) {
                level = -1;
            }
            levelOfDetail = LevelOfDetail.getByLevel(level);
        } else {
            return maxLod;
        }

        return levelOfDetail;
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