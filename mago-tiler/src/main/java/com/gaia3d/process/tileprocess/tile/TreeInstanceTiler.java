package com.gaia3d.process.tileprocess.tile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.exception.TileProcessingException;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.command.mago.GlobalConstants;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.tileprocess.Tiler;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;
import com.gaia3d.process.tileprocess.tile.tileset.TilesetV2;
import com.gaia3d.process.tileprocess.tile.tileset.asset.AssetV1;
import com.gaia3d.process.tileprocess.tile.tileset.asset.AssetV2;
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
public class TreeInstanceTiler extends DefaultTiler implements Tiler {

    private static final GlobalOptions globalOptions = GlobalOptions.getInstance();
    private final double maximumGeometricError = 64.0;
    private double instanceGeometricError = 1.0;
    private final double maximumDistance = 1000.0; // 1km

    @Override
    public Tileset run(List<TileInfo> tileInfos) {
        if (!tileInfos.isEmpty()) {
            instanceGeometricError = calcGeometricError(List.of(tileInfos.get(0)));
        }
        if (instanceGeometricError < maximumGeometricError) {
            instanceGeometricError = maximumGeometricError;
        }

        GaiaBoundingBox globalBoundingBox = calcCartographicBoundingBox(tileInfos);
        Matrix4d transformMatrix = getTransformMatrixFromCartographic(globalBoundingBox);
        if (globalOptions.isClassicTransformMatrix()) {
            rotateX90(transformMatrix);
        }

        Node root = createRoot();
        root.setBoundingVolume(new BoundingVolume(globalBoundingBox, BoundingVolume.BoundingVolumeType.REGION));
        root.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
        root.setGeometricError(instanceGeometricError);

        try {
            createNode(root, tileInfos, null, 0);
        } catch (IOException e) {
            log.error("[ERROR] :", e);
            throw new RuntimeException(e);
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
        tileset.setGeometricError(instanceGeometricError);
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

    private void createNode(Node parentNode, List<TileInfo> tileInfos, List<TileInfo> inheritanceTileInfos, int nodeDepth) throws IOException {
        BoundingVolume parentBoundingVolume = parentNode.getBoundingVolume();
        BoundingVolume squareBoundingVolume = parentBoundingVolume.createSqureBoundingVolume();

        long instanceLimit = globalOptions.getMaxInstance();
        long instanceCount = tileInfos.size();
        boolean isRefineAdd = globalOptions.isRefineAdd();

        GaiaBoundingBox gaiaBoundingBox = parentNode.getBoundingBox();
        if (gaiaBoundingBox == null) {
            gaiaBoundingBox = calcCartographicBoundingBox(tileInfos);
        }
        double distance = gaiaBoundingBox.getLongestDistance();
        if (nodeDepth > globalOptions.getMaxNodeDepth()) {
            log.warn("[WARN][Tile] Node depth limit exceeded : {}", nodeDepth);
            Node childNode = createContentNode(parentNode, tileInfos, inheritanceTileInfos, 0);
            if (childNode != null) {
                parentNode.getChildren().add(childNode);
            }
            return;
        }

        if (instanceCount > instanceLimit || distance > maximumDistance) {
            List<List<TileInfo>> childrenScenes = squareBoundingVolume.distributeScene(tileInfos);
            for (int index = 0; index < childrenScenes.size(); index++) {
                List<TileInfo> childTileInfos = childrenScenes.get(index);
                Node childNode = createLogicalNode(parentNode, childTileInfos, index);
                if (childNode != null) {
                    parentNode.getChildren().add(childNode);
                    createNode(childNode, childTileInfos, inheritanceTileInfos, nodeDepth + 1);
                }
            }
        } else if (instanceCount >= 2) {
            List<List<TileInfo>> childrenScenes = squareBoundingVolume.distributeScene(tileInfos);
            for (int index = 0; index < childrenScenes.size(); index++) {
                List<TileInfo> childTileInfos = childrenScenes.get(index);
                Collections.shuffle(childTileInfos);
                Node childNode = createContentNode(parentNode, childTileInfos, inheritanceTileInfos, index);
                if (childNode != null) {
                    parentNode.getChildren().add(childNode);
                    Content content = childNode.getContent();
                    if (content != null) {
                        ContentInfo contentInfo = content.getContentInfo();
                        List<TileInfo> remainTileInfos = contentInfo.getRemainTileInfos();

                        List<List<TileInfo>> distributedInheritanceTileInfos = squareBoundingVolume.distributeScene(contentInfo.getTileInfos());
                        List<TileInfo> newInheritanceTileInfos = distributedInheritanceTileInfos.get(index);
                        if (isRefineAdd) {
                            createNode(childNode, remainTileInfos, newInheritanceTileInfos, nodeDepth + 1);
                        } else {
                            createNode(childNode, childTileInfos, inheritanceTileInfos, nodeDepth + 1);
                        }
                    } else {
                        createNode(childNode, childTileInfos, inheritanceTileInfos, nodeDepth + 1);
                    }
                }
            }
        } else if (!tileInfos.isEmpty()) {
            Node childNode = createContentNode(parentNode, tileInfos, inheritanceTileInfos, 0);
            if (childNode != null) {
                parentNode.getChildren().add(childNode);
                Content content = childNode.getContent();
                if (content != null) {
                    ContentInfo contentInfo = content.getContentInfo();
                    List<TileInfo> remainTileInfos = contentInfo.getRemainTileInfos();

                    createNode(childNode, remainTileInfos, inheritanceTileInfos, nodeDepth + 1);
                } else {
                    createNode(childNode, tileInfos, inheritanceTileInfos, nodeDepth + 1);
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
        GaiaBoundingBox childBoundingBox = calcCartographicBoundingBox(tileInfos);
        Matrix4d transformMatrix = getTransformMatrixFromCartographic(childBoundingBox);
        if (globalOptions.isClassicTransformMatrix()) {
            rotateX90(transformMatrix);
        }

        BoundingVolume boundingVolume = new BoundingVolume(childBoundingBox, BoundingVolume.BoundingVolumeType.REGION);
        geometricError = DecimalUtils.cutFast(geometricError);

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

    private Node createContentNode(Node parentNode, List<TileInfo> tileInfos, List<TileInfo> inheritanceTileInfos, int index) {
        if (tileInfos.isEmpty()) {
            return null;
        }
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        int minLevel = globalOptions.getMinLod();
        int maxLevel = globalOptions.getMaxLod();
        boolean refineAdd = globalOptions.isRefineAdd();

        GaiaBoundingBox childBoundingBox = calcCartographicBoundingBox(tileInfos);
        Matrix4d transformMatrix = getTransformMatrixFromCartographic(childBoundingBox);
        if (globalOptions.isClassicTransformMatrix()) {
            rotateX90(transformMatrix);
        }

        BoundingVolume boundingVolume = new BoundingVolume(childBoundingBox, BoundingVolume.BoundingVolumeType.REGION);

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

        if (refineAdd) {
            /*if (lod.getLevel() == 0) {
                lod = LevelOfDetail.LOD0;
            } else {
                lod = LevelOfDetail.LOD3;
            }*/
            lod = LevelOfDetail.getByLevel(lod.getLevel());
        }

        nodeCode = nodeCode + index;
        int lodError = lod.getGeometricError();
        if (refineAdd) {
            double parentGeometricError = parentNode.getGeometricError();
            if (parentGeometricError > 16) {
                lodError = 16;
            } else if (parentGeometricError > 1) {
                lodError = (int) (parentGeometricError / 2);
            }
        }

        log.info("[Tile][ContentNode][" + nodeCode + "][LOD{}][OBJECT{}]", lod.getLevel(), tileInfos.size());

        int divideSize = tileInfos.size() / 4;
        if (divideSize > GlobalConstants.DEFAULT_MAX_I3DM_FEATURE_COUNT) {
            divideSize = GlobalConstants.DEFAULT_MAX_I3DM_FEATURE_COUNT;
        } else if (divideSize < GlobalConstants.DEFAULT_MIN_I3DM_FEATURE_COUNT) {
            divideSize = GlobalConstants.DEFAULT_MIN_I3DM_FEATURE_COUNT;
        }

        List<TileInfo> resultInfos;
        List<TileInfo> remainInfos;
        List<TileInfo> totalResultInfos;

        if (refineAdd) {
            resultInfos = tileInfos.stream()
                    .limit(divideSize)
                    .collect(Collectors.toList());
            remainInfos = tileInfos.stream()
                    .skip(divideSize)
                    .collect(Collectors.toList());
            totalResultInfos = new ArrayList<>(resultInfos);

            if (inheritanceTileInfos != null && !inheritanceTileInfos.isEmpty()) {
                List<TileInfo> tempInheritanceTileInfos = boundingVolume.getVolumeIncludeScenes(inheritanceTileInfos, childBoundingBox);
                totalResultInfos.addAll(tempInheritanceTileInfos);
            }

            if (remainInfos.isEmpty() && lod != LevelOfDetail.LOD0) {
                remainInfos.addAll(tileInfos);
            }
        } else {
            resultInfos = tileInfos.stream()
                    .limit(tileInfos.size())
                    .collect(Collectors.toList());
            remainInfos = tileInfos.stream()
                    .skip(0)
                    .collect(Collectors.toList());
            totalResultInfos = new ArrayList<>(resultInfos);
        }

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
        childNode.setRefine(Node.RefineType.REPLACE);

        if (!resultInfos.isEmpty()) {
            ContentInfo contentInfo = new ContentInfo();
            contentInfo.setName(nodeCode);
            contentInfo.setLod(lod);
            contentInfo.setBoundingBox(childBoundingBox);
            contentInfo.setNodeCode(nodeCode);
            contentInfo.setTileInfos(totalResultInfos);
            contentInfo.setTempTileInfos(resultInfos);
            contentInfo.setRemainTileInfos(remainInfos);
            Content content = new Content();
            if (globalOptions.getTilesVersion().equals("1.0")) {
                content.setUri("data/" + nodeCode + ".i3dm");
            } else {
                content.setUri("data/" + nodeCode + ".glb");
            }
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
