package com.gaia3d.process.tileprocess.tile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.assimp.Converter;
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

@Slf4j
public class Gaia3DTiler implements Tiler {
    private static final int DEFUALT_MAX_COUNT = 256;
    private static final int DEFUALT_MIN_LEVEL = 0;
    private static final int DEFUALT_MAX_LEVEL = 3;

    private final CommandLine command;
    private final Converter converter;
    private final TilerOptions options;

    public Gaia3DTiler(TilerOptions tilerOptions, CommandLine command) {
        this.options = tilerOptions;
        this.converter = new AssimpConverter(command);
        this.command = command;
    }

    @Override
    public Tileset run(List<TileInfo> tileInfos) {
        double geometricError = calcGeometricError(tileInfos);

        GaiaBoundingBox globalBoundingBox = calcBoundingBox(tileInfos);
        Matrix4d transformMatrix = getTransformMatrix(globalBoundingBox);
        rotateX90(transformMatrix);

        Node root = createRoot();
        root.setBoundingVolume(new BoundingVolume(globalBoundingBox));
        root.setTransformMatrix(transformMatrix);
        root.setGeometricError(geometricError);

        try {
            createNode(root, tileInfos);
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
            GaiaBoundingBox boundingBox = tileInfo.getBoundingBox();
            //GaiaBoundingBox boundingBox = tileInfo.getScene().getBoundingBox();
            double result = boundingBox.getLongestDistance();
            if (result > 1000.0d) {
                log.warn("[Warn]{} is too long distance. check it please. (GeometricError)", result);
            }
            return result;
        }).max().orElse(0.0d);
    }

    private GaiaBoundingBox calcBoundingBox(List<TileInfo> tileInfos) {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        tileInfos.forEach(tileInfo -> {
            KmlInfo kmlInfo = tileInfo.getKmlInfo();
            Vector3d position = kmlInfo.getPosition();
            //GaiaBoundingBox localBoundingBox = tileInfo.getScene().getBoundingBox();
            GaiaBoundingBox localBoundingBox = tileInfo.getBoundingBox();
            localBoundingBox = localBoundingBox.convertLocalToLonlatBoundingBox(position);
            boundingBox.addBoundingBox(localBoundingBox);
        });
        return boundingBox;
    }

    private void createNode(Node parentNode, List<TileInfo> tileInfos) throws IOException {
        BoundingVolume parentBoundingVolume = parentNode.getBoundingVolume();
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
            int test = 0;
            List<List<TileInfo>> childrenScenes = parentBoundingVolume.distributeScene(tileInfos);
            for (int index = 0; index < childrenScenes.size(); index++) {
                List<TileInfo> childTileInfos = childrenScenes.get(index);

                test += childTileInfos.size();
                Node childNode = createContentNode(parentNode, childTileInfos, index);
                if (childNode != null) {
                    parentNode.getChildren().add(childNode);
                    createNode(childNode, childTileInfos);
                }
            }
            if (test != tileInfos.size()) {
                log.info("test : " + test + ", scenes : " + tileInfos.size());
            }
        } else if (tileInfos.size() > 0) {
            //Node childNode = createContentNode(parentNode, reloadScenes(tileInfos), 0);
            Node childNode = createContentNode(parentNode, tileInfos, 0);
            if (childNode != null) {
                parentNode.getChildren().add(childNode);
                createNode(childNode, tileInfos);
            }
        }
    }

    private Node createStructNode(Node parentNode, List<TileInfo> tileInfos, int index) {
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
    }

    private Node createContentNode(Node parentNode, List<TileInfo> tileInfos, int index) {
        if (tileInfos.size() < 1) {
            return null;
        }

        int minLevel = command.hasOption(ProcessOptions.MIN_LOD.getArgName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MIN_LOD.getArgName())) : DEFUALT_MIN_LEVEL;
        int maxLevel = command.hasOption(ProcessOptions.MAX_LOD.getArgName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MAX_LOD.getArgName())) : DEFUALT_MAX_LEVEL;

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

        Node childNode = new Node();
        childNode.setParent(parentNode);
        childNode.setTransformMatrix(transformMatrix);
        childNode.setBoundingVolume(boundingVolume);
        childNode.setNodeCode(nodeCode);
        childNode.setGeometricError(lod.getGeometricError());
        childNode.setRefine(Node.RefineType.REPLACE);
        childNode.setChildren(new ArrayList<>());

        ContentInfo contentInfo = new ContentInfo();
        contentInfo.setName(nodeCode);
        contentInfo.setLod(lod);
        contentInfo.setTileInfos(tileInfos);
        contentInfo.setBoundingBox(childBoundingBox);
        contentInfo.setNodeCode(nodeCode);

        Content content = new Content();
        content.setUri("data/" + nodeCode + ".b3dm");
        content.setContentInfo(contentInfo);
        childNode.setContent(content);
        return childNode;
    }

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

        /*int level;
        if (hasContent) {
            String contentLevel = nodeCode.split("C")[1];
            int level = contentLevel.length();
            if (maxLevel >= level) {
                int offset = maxLevel - level;
                if (minLevel < offset) {
                    offset = minLevel;
                }
                levelOfDetail = LevelOfDetail.getByLevel(offset);
            }
        } else {
            levelOfDetail = maxLod;
        }*/




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


        /*if (FormatType.KML == formatType) {
            double[] cartesian = GlobeUtils.geographicToCartesianWgs84(center.x, center.y, center.z);
            return GlobeUtils.normalAtCartesianPointWgs84(cartesian[0], cartesian[1], cartesian[2]);
        } else {
            Vector3d centerBottom = new Vector3d(center.x, center.y, boundingBox.getMinZ());
            ProjCoordinate centerPoint = new ProjCoordinate(centerBottom.x(), centerBottom.y(), centerBottom.z());
            ProjCoordinate translatedCenterPoint = GlobeUtils.transform(tilerOptions.getSource(), centerPoint);
            double[] cartesian = GlobeUtils.geographicToCartesianWgs84(translatedCenterPoint.x, translatedCenterPoint.y, centerBottom.z());
            return GlobeUtils.normalAtCartesianPointWgs84(cartesian[0], cartesian[1], cartesian[2]);
        }*/
    }

    private Asset createAsset() {
        Asset asset = new Asset();
        Extras extras = new Extras();
        Cesium cesium = new Cesium();
        Ion ion = new Ion();
        List<Credit> credits = new ArrayList<>();
        Credit credit = new Credit();
        credit.setHtml("<html>TEST</html>");
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
        root.setRefine(Node.RefineType.REPLACE);
        root.setChildren(new ArrayList<>());
        return root;
    }
}
