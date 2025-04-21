package com.gaia3d.process.tileprocess.tile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.exception.TileProcessingException;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.tileprocess.Tiler;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;
import com.gaia3d.process.tileprocess.tile.tileset.asset.Asset;
import com.gaia3d.process.tileprocess.tile.tileset.node.BoundingVolume;
import com.gaia3d.process.tileprocess.tile.tileset.node.Content;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import com.gaia3d.util.DecimalUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("ALL")
@Slf4j
@NoArgsConstructor
public class Instanced3DModelTiler extends DefaultTiler implements Tiler {

    private static final GlobalOptions globalOptions = GlobalOptions.getInstance();
    private final double maximumGeometricError = 64.0;

    private double instanceGeometricError = 1.0;

    @Override
    public Tileset run(List<TileInfo> tileInfos) {
        if (!tileInfos.isEmpty()) {
            instanceGeometricError = calcGeometricError(List.of(tileInfos.get(0)));
        }
        if (instanceGeometricError < maximumGeometricError) {
            instanceGeometricError = maximumGeometricError;
        }

        GaiaBoundingBox globalBoundingBox = calcBoundingBox(tileInfos);
        Matrix4d transformMatrix = getTransformMatrix(globalBoundingBox);
        if (globalOptions.isClassicTransformMatrix()) {
            rotateX90(transformMatrix);
        }

        Node root = createRoot();
        root.setBoundingVolume(new BoundingVolume(globalBoundingBox));
        root.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
        root.setGeometricError(instanceGeometricError);

        try {
            createNode(root, tileInfos);
        } catch (IOException e) {
            log.error("[ERROR] :", e);
            throw new RuntimeException(e);
        }

        Asset asset = createAsset();
        Tileset tileset = new Tileset();
        //tileset.setGeometricError(instanceGeometricError);
        tileset.setAsset(asset);
        tileset.setRoot(root);
        return tileset;
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
            log.error("[ERROR] :", e);
            throw new TileProcessingException(e.getMessage());
        }
    }

    private void createNode(Node parentNode, List<TileInfo> tileInfos) throws IOException {
        BoundingVolume parentBoundingVolume = parentNode.getBoundingVolume();
        BoundingVolume squareBoundingVolume = parentBoundingVolume.createSqureBoundingVolume();

        long instanceLimit = globalOptions.getMaxInstance();
        //long instanceLimit = globalOptions.getMaxInstance();
        long instanceCount = tileInfos.size();
        boolean isRefineAdd = globalOptions.isRefineAdd();

        if (instanceCount > instanceLimit) {
            List<List<TileInfo>> childrenScenes = squareBoundingVolume.distributeScene(tileInfos);
            for (int index = 0; index < childrenScenes.size(); index++) {
                List<TileInfo> childTileInfos = childrenScenes.get(index);
                Node childNode = createLogicalNode(parentNode, childTileInfos, index);
                if (childNode != null) {
                    parentNode.getChildren().add(childNode);
                    createNode(childNode, childTileInfos);
                }
            }
        } else if (instanceCount > 1) {
            List<List<TileInfo>> childrenScenes = squareBoundingVolume.distributeScene(tileInfos);
            for (int index = 0; index < childrenScenes.size(); index++) {
                List<TileInfo> childTileInfos = childrenScenes.get(index);
                // shuffle
                Collections.shuffle(childTileInfos);

                Node childNode = createContentNode(parentNode, childTileInfos, index);
                if (childNode != null) {
                    parentNode.getChildren().add(childNode);
                    Content content = childNode.getContent();
                    if (content != null) {
                        ContentInfo contentInfo = content.getContentInfo();

                        if (isRefineAdd) {
                            createNode(childNode, contentInfo.getRemainTileInfos());
                        } else {
                            createNode(childNode, childTileInfos);
                        }
                    } else {
                        createNode(childNode, childTileInfos);
                    }
                }
            }
        } else if (!tileInfos.isEmpty()) {
            Node childNode = createContentNode(parentNode, tileInfos, 0);
            if (childNode != null) {
                parentNode.getChildren().add(childNode);
                Content content = childNode.getContent();
                if (content != null) {
                    ContentInfo contentInfo = content.getContentInfo();
                    createNode(childNode, contentInfo.getRemainTileInfos());
                } else {
                    createNode(childNode, tileInfos);
                }
            }
        }
    }

    private Node createLogicalNode(Node parentNode, List<TileInfo> tileInfos, int index) {
        if (tileInfos.isEmpty()) {
            return null;
        }
        String nodeCode = parentNode.getNodeCode();
        nodeCode = nodeCode + index;
        log.info("[Tile][LogicalNode][" + nodeCode + "][OBJECT{}]", tileInfos.size());

        double geometricError = instanceGeometricError;
        GaiaBoundingBox childBoundingBox = calcBoundingBox(tileInfos);
        Matrix4d transformMatrix = getTransformMatrix(childBoundingBox);
        //rotateX90(transformMatrix);
        if (globalOptions.isClassicTransformMatrix()) {
            rotateX90(transformMatrix);
        }

        BoundingVolume boundingVolume = new BoundingVolume(childBoundingBox);
        geometricError = DecimalUtils.cut(geometricError);

        Node childNode = new Node();
        childNode.setParent(parentNode);
        childNode.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
        childNode.setBoundingVolume(boundingVolume);
        childNode.setNodeCode(nodeCode);
        childNode.setGeometricError(geometricError);
        childNode.setRefine(Node.RefineType.REPLACE);
        childNode.setChildren(new ArrayList<>());
        return childNode;
    }

    private Node createContentNode(Node parentNode, List<TileInfo> tileInfos, int index) {
        if (tileInfos.isEmpty()) {
            return null;
        }
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        int minLevel = globalOptions.getMinLod();
        int maxLevel = globalOptions.getMaxLod();
        boolean refineAdd = globalOptions.isRefineAdd();

        GaiaBoundingBox childBoundingBox = calcBoundingBox(tileInfos);
        Matrix4d transformMatrix = getTransformMatrix(childBoundingBox);
        if (globalOptions.isClassicTransformMatrix()) {
            rotateX90(transformMatrix);
        }

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

        log.info("[Tile][ContentNode][" + nodeCode + "][LOD{}][OBJECT{}]", lod.getLevel(), tileInfos.size());

        //int lodError = refineAdd ? lod.getGeometricErrorBlock() : lod.getGeometricError();
        int lodError = lod.getGeometricError();
        if (refineAdd) {
            lodError = lod.getGeometricError() * 2;
        }

        int divideSize = tileInfos.size() / 4;
        if (divideSize > GlobalOptions.DEFAULT_MAX_I3DM_FEATURE_COUNT) {
            divideSize = GlobalOptions.DEFAULT_MAX_I3DM_FEATURE_COUNT;
        } else if (divideSize < GlobalOptions.DEFAULT_MIN_I3DM_FEATURE_COUNT) {
            divideSize = GlobalOptions.DEFAULT_MIN_I3DM_FEATURE_COUNT;
        }
        //int divideSize = globalOptions.getMaxInstance();


        // divide by globalOptions.getMaxInstance()
        List<TileInfo> resultInfos = tileInfos.stream()
                .limit(divideSize)
                .collect(Collectors.toList());
        List<TileInfo> remainInfos = tileInfos.stream()
                .skip(divideSize)
                .collect(Collectors.toList());

        Node childNode = new Node();
        childNode.setParent(parentNode);
        childNode.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
        childNode.setBoundingVolume(boundingVolume);
        childNode.setNodeCode(nodeCode);

        if (lodError < 1.0) {
            lodError = 1;
        }
        childNode.setGeometricError(lodError);
        childNode.setChildren(new ArrayList<>());

        //childNode.setRefine(refineAdd ? Node.RefineType.ADD : Node.RefineType.REPLACE);
        if (refineAdd) {
            childNode.setRefine(Node.RefineType.ADD);
        } else {
            childNode.setRefine(Node.RefineType.REPLACE);
        }

        if (!resultInfos.isEmpty()) {
            ContentInfo contentInfo = new ContentInfo();
            contentInfo.setName(nodeCode);
            contentInfo.setLod(lod);
            contentInfo.setBoundingBox(childBoundingBox);
            contentInfo.setNodeCode(nodeCode);
            contentInfo.setTileInfos(resultInfos);
            contentInfo.setRemainTileInfos(remainInfos);
            Content content = new Content();
            content.setUri("data/" + nodeCode + ".i3dm");
            content.setContentInfo(contentInfo);
            childNode.setContent(content);
        } else {
            log.warn("[WARN][Tile][ContentNode][{}] No content", nodeCode);
        }
        return childNode;
    }

    private LevelOfDetail getLodByNodeCode(LevelOfDetail minLod, LevelOfDetail maxLod, String nodeCode) {
        LevelOfDetail levelOfDetail;
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
}
