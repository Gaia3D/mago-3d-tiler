package com.gaia3d.process.tileprocess.tile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.exception.TileProcessingException;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
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
import org.joml.Vector3d;
import org.locationtech.proj4j.CoordinateReferenceSystem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("ALL")
@Slf4j
@NoArgsConstructor
public class Batched3DModelTiler extends DefaultTiler implements Tiler {

    public final GlobalOptions globalOptions = GlobalOptions.getInstance();

    @Override
    public Tileset run(List<TileInfo> tileInfos) {
        double geometricError = calcGeometricError(tileInfos);
        geometricError = DecimalUtils.cutFast(geometricError);

        GaiaBoundingBox boundingBox = null;
        Matrix4d transformMatrix = null;

        Node root = createRoot();
        CoordinateReferenceSystem sourceCrs = globalOptions.getSourceCrs();
        if (sourceCrs != null && sourceCrs.getName().equals("EPSG:4978")) {
            log.info("[INFO] Using EPSG:4978 coordinate system.");
            boundingBox = calcCartesianBoundingBox(tileInfos);
            transformMatrix = getTransformMatrixFromCartesian(boundingBox);
            root.setBoundingVolume(new BoundingVolume(boundingBox, BoundingVolume.BoundingVolumeType.BOX));
        } else {
            boundingBox = calcCartographicBoundingBox(tileInfos);
            transformMatrix = getTransformMatrixFromCartographic(boundingBox);
            root.setBoundingVolume(new BoundingVolume(boundingBox, BoundingVolume.BoundingVolumeType.REGION));
        }

        if (globalOptions.isClassicTransformMatrix() && transformMatrix != null) {
            rotateX90(transformMatrix);
        }
        root.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
        root.setGeometricError(geometricError);

        try {
            createNode(root, tileInfos, 0);
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
        tileset.setGeometricError(geometricError);
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

    private void createNode(Node parentNode, List<TileInfo> tileInfos, int nodeDepth) throws IOException {
        BoundingVolume parentBoundingVolume = parentNode.getBoundingVolume();
        BoundingVolume squareBoundingVolume = parentBoundingVolume.createSqureBoundingVolume();

        boolean refineAdd = globalOptions.isRefineAdd();
        long triangleLimit = globalOptions.getMaxTriangles();
        long totalTriangleCount = tileInfos.stream().mapToLong(TileInfo::getTriangleCount).sum();
        log.debug("[TriangleCount] Total : {}", totalTriangleCount);
        log.debug("[Tile][ContentNode][OBJECT] : {}", tileInfos.size());
        if (nodeDepth > globalOptions.getMaxNodeDepth()) {
            log.warn("[WARN][Tile] Node depth limit exceeded : {}", nodeDepth);
            Node childNode = createContentNode(parentNode, tileInfos, 0);
            if (childNode != null) {
                parentNode.getChildren().add(childNode);
            }
            return;
        }

        if (tileInfos.size() <= 1) {
            Node childNode = createContentNode(parentNode, tileInfos, 0);
            if (childNode != null) {
                parentNode.getChildren().add(childNode);
                createNode(childNode, tileInfos, nodeDepth + 1);
            }
        } else if (totalTriangleCount > triangleLimit) {
            List<List<TileInfo>> childrenScenes = squareBoundingVolume.distributeScene(tileInfos);

            // is samp transform matrix for all children nodes
            GaiaBoundingBox matrixBoundingBox = new GaiaBoundingBox();
            for (TileInfo tileInfo : tileInfos) {
                GaiaBoundingBox boundingBox = tileInfo.getBoundingBox();
                Vector3d center = boundingBox.getCenter();
                matrixBoundingBox.addPoint(center);
            }

            for (int index = 0; index < childrenScenes.size(); index++) {
                List<TileInfo> childTileInfos = childrenScenes.get(index);
                Node childNode = createLogicalNode(parentNode, childTileInfos, index);
                if (childNode != null) {
                    parentNode.getChildren().add(childNode);
                    createNode(childNode, childTileInfos, nodeDepth + 1);
                }
            }
        } else if (totalTriangleCount > 1) {
            List<List<TileInfo>> childrenScenes = squareBoundingVolume.distributeScene(tileInfos);
            for (int index = 0; index < childrenScenes.size(); index++) {
                List<TileInfo> childTileInfos = childrenScenes.get(index);

                Node childNode = createContentNode(parentNode, childTileInfos, index);
                if (childNode != null) {
                    parentNode.getChildren().add(childNode);
                    Content content = childNode.getContent();
                    if (content != null && refineAdd) {
                        ContentInfo contentInfo = content.getContentInfo();
                        createNode(childNode, contentInfo.getRemainTileInfos(), nodeDepth + 1);
                    } else {
                        createNode(childNode, childTileInfos, nodeDepth + 1);
                    }
                }
            }
        } else {
            Node childNode = createContentNode(parentNode, tileInfos, 0);
            if (childNode != null) {
                parentNode.getChildren().add(childNode);
                createNode(childNode, tileInfos, nodeDepth + 1);
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

        double geometricError = calcGeometricError(tileInfos);

        BoundingVolume boundingVolume;
        GaiaBoundingBox boundingBox = null;
        Matrix4d transformMatrix = null;

        Node root = createRoot();
        CoordinateReferenceSystem sourceCrs = globalOptions.getSourceCrs();
        if (sourceCrs != null && sourceCrs.getName().equals("EPSG:4978")) {
            log.info("[INFO] Using EPSG:4978 coordinate system.");
            boundingBox = calcCartesianBoundingBox(tileInfos);
            transformMatrix = getTransformMatrixFromCartesian(boundingBox);
            boundingVolume = new BoundingVolume(boundingBox, BoundingVolume.BoundingVolumeType.BOX);
        } else {
            boundingBox = calcCartographicBoundingBox(tileInfos);
            transformMatrix = getTransformMatrixFromCartographic(boundingBox);
            boundingVolume = new BoundingVolume(boundingBox, BoundingVolume.BoundingVolumeType.REGION);
        }
        if (globalOptions.isClassicTransformMatrix()) {
            rotateX90(transformMatrix);
        }
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

    private Node createContentNode(Node parentNode, List<TileInfo> tileInfos, int index) {
        if (tileInfos.isEmpty()) {
            return null;
        }
        int minLevel = globalOptions.getMinLod();
        int maxLevel = globalOptions.getMaxLod();
        boolean refineAdd = globalOptions.isRefineAdd();

        /*GaiaBoundingBox childBoundingBox = calcCartographicBoundingBox(tileInfos);
        Matrix4d transformMatrix = getTransformMatrix(childBoundingBox);
        if (globalOptions.isClassicTransformMatrix()) {
            rotateX90(transformMatrix);
        }




        CoordinateReferenceSystem sourceCrs = globalOptions.getCrs();
        BoundingVolume boundingVolume;
        if (sourceCrs != null && sourceCrs.getName().equals("EPSG:4978")) {
            log.info("[INFO] Using EPSG:4978 coordinate system.");
            boundingVolume = new BoundingVolume(childBoundingBox, BoundingVolume.BoundingVolumeType.BOX);
        } else {
            boundingVolume = new BoundingVolume(childBoundingBox, BoundingVolume.BoundingVolumeType.REGION);
        }*/



        BoundingVolume boundingVolume;
        GaiaBoundingBox boundingBox = null;
        Matrix4d transformMatrix = null;

        Node root = createRoot();
        CoordinateReferenceSystem sourceCrs = globalOptions.getSourceCrs();
        if (sourceCrs != null && sourceCrs.getName().equals("EPSG:4978")) {
            log.info("[INFO] Using EPSG:4978 coordinate system.");
            boundingBox = calcCartesianBoundingBox(tileInfos);
            transformMatrix = getTransformMatrixFromCartesian(boundingBox);
            boundingVolume = new BoundingVolume(boundingBox, BoundingVolume.BoundingVolumeType.BOX);
        } else {
            boundingBox = calcCartographicBoundingBox(tileInfos);
            transformMatrix = getTransformMatrixFromCartographic(boundingBox);
            boundingVolume = new BoundingVolume(boundingBox, BoundingVolume.BoundingVolumeType.REGION);
        }
        if (globalOptions.isClassicTransformMatrix()) {
            rotateX90(transformMatrix);
        }


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

        int lodError = refineAdd ? lod.getGeometricErrorBlock() : lod.getGeometricError();

        List<TileInfo> resultInfos;
        List<TileInfo> remainInfos;
        resultInfos = tileInfos.stream().filter(tileInfo -> {
            double geometricError = tileInfo.getBoundingBox().getLongestDistance();
            return geometricError >= lodError;
        }).collect(Collectors.toList());
        remainInfos = tileInfos.stream().filter(tileInfo -> {
            double geometricError = tileInfo.getBoundingBox().getLongestDistance();
            return geometricError < lodError;
        }).collect(Collectors.toList());

        Node childNode = new Node();
        childNode.setParent(parentNode);
        childNode.setTransformMatrix(transformMatrix, globalOptions.isClassicTransformMatrix());
        childNode.setBoundingVolume(boundingVolume);
        childNode.setNodeCode(nodeCode);
        childNode.setGeometricError(lodError + 0.1);
        childNode.setChildren(new ArrayList<>());

        childNode.setRefine(refineAdd ? Node.RefineType.ADD : Node.RefineType.REPLACE);
        if (!resultInfos.isEmpty()) {
            ContentInfo contentInfo = new ContentInfo();
            contentInfo.setName(nodeCode);
            contentInfo.setLod(lod);
            contentInfo.setBoundingBox(boundingBox);
            contentInfo.setNodeCode(nodeCode);
            contentInfo.setTileInfos(resultInfos);
            contentInfo.setRemainTileInfos(remainInfos);
            contentInfo.setTransformMatrix(transformMatrix);

            Content content = new Content();
            if (globalOptions.getTilesVersion().equals("1.0")) {
                content.setUri("data/" + nodeCode + ".b3dm");
            } else {
                content.setUri("data/" + nodeCode + ".glb");
            }
            content.setContentInfo(contentInfo);
            childNode.setContent(content);
        } else {
            log.debug("[Tile][ContentNode][{}] No Contents", nodeCode);
        }
        return childNode;
    }

    private LevelOfDetail getLodByNodeCode(LevelOfDetail minLod, LevelOfDetail maxLod, String nodeCode) {
        int minLevel = minLod.getLevel();
        int maxLevel = maxLod.getLevel();
        String[] splitCode = nodeCode.split("C");
        if (splitCode.length > 1) {
            String contentLevel = nodeCode.split("C")[1];
            int level = maxLevel - contentLevel.length();
            if (level < minLevel) {
                level = -1;
            }
            return LevelOfDetail.getByLevel(level);
        } else {
            return maxLod;
        }
    }
}
