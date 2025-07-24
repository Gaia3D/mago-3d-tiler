package com.gaia3d.process.tileprocess;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;
import com.gaia3d.process.tileprocess.tile.tileset.TilesetV2;
import com.gaia3d.process.tileprocess.tile.tileset.asset.AssetV1;
import com.gaia3d.process.tileprocess.tile.tileset.asset.AssetV2;
import com.gaia3d.process.tileprocess.tile.tileset.node.BoundingVolume;
import com.gaia3d.process.tileprocess.tile.tileset.node.Content;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class TileMerger {

    private static final GlobalOptions globalOptions = GlobalOptions.getInstance();
    private final int MINIMUM_DEPTH = 2;
    private final int MAXIMUM_DEPTH = 16;

    public void merge() {
        log.info("[Merge] Starting tileset merging.");

        String tilesetName = "tileset.json";
        File inputPath = new File(globalOptions.getInputPath());
        File outputPath = new File(globalOptions.getOutputPath());
        File tilesetPath = new File(outputPath, tilesetName);

        // find all tileset.json files
        log.info("[Merge] searching for tileset.json files in {}.", inputPath);
        List<File> tilesetJsons;
        if (globalOptions.isRecursive()) {
            tilesetJsons = findAllTilesetJsons(inputPath);
        } else {
            tilesetJsons = findAllTilesetJsons(inputPath, MINIMUM_DEPTH);
        }
        log.info("[Merge] found {} tileset.json files.", tilesetJsons.size());
        if (tilesetJsons.isEmpty()) {
            log.warn("[Merge] No tileset.json files found.");
            return;
        }

        log.info("[Merge] parsing tileset.json files.");
        // parse all tileset.json files
        Map<File, Tileset> tilesets = parseTilesetJsons(tilesetJsons);

        log.info("[Merge] merging tileset.json files.");
        // calculate bounding box and geospatial information and merge tilesets
        Tileset tileset = mergeTilesets(tilesets);

        log.info("[Merge] writing merged tileset.json -> {}", tilesetPath.getAbsolutePath());
        // write merged tileset.json
        writeTilesetJson(tilesetPath, tileset);
        log.info("[Merge] End tileset combining.");
    }

    private void writeTilesetJson(File tilesetPath, Tileset tileset) {
        ObjectMapper objectMapper = new ObjectMapper();
        if (!globalOptions.isDebug()) {
            objectMapper.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
        }
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

        try {
            objectMapper.writeValue(tilesetPath, tileset);
            log.info("[Merge] Tileset.json is written to {}", tilesetPath);
        } catch (IOException e) {
            log.error("[ERROR] Failed to write tileset.json.", e);
            throw new RuntimeException(e);
        }
    }

    private Map<File, Tileset> parseTilesetJsons(List<File> tilesetJsons) {
        //List<Tileset> tilesets = new ArrayList<>();
        Map<File, Tileset> tilesetMap = new HashMap<>();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

        for (File tilesetJson : tilesetJsons) {
            try {
                if (globalOptions.getTilesVersion().equals("1.0")) {
                    Tileset tileset = objectMapper.readValue(tilesetJson, Tileset.class);
                    tilesetMap.put(tilesetJson, tileset);
                } else {
                    TilesetV2 tileset = objectMapper.readValue(tilesetJson, TilesetV2.class);
                    tilesetMap.put(tilesetJson, tileset);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return tilesetMap;
    }

    private Tileset mergeTilesets(Map<File, Tileset> tilesetMap) {
        File inputPath = new File(globalOptions.getInputPath());

        double geometricError = 0.0;
        Tileset mergedTileset = new Tileset();

        // region calculate bounding box
        double[] globalBoundingRegion = new double[6];
        globalBoundingRegion[0] = Double.MAX_VALUE;
        globalBoundingRegion[1] = Double.MAX_VALUE;
        globalBoundingRegion[2] = -Double.MAX_VALUE;
        globalBoundingRegion[3] = -Double.MAX_VALUE;
        globalBoundingRegion[4] = Double.MAX_VALUE;
        globalBoundingRegion[5] = -Double.MAX_VALUE;

        Node root = new Node();
        root.setRefine(Node.RefineType.ADD);

        List<Node> children = new ArrayList<>();
        List<File> tilesetFiles = new ArrayList<>(tilesetMap.keySet());
        for (File tilesetFile : tilesetFiles) {
            Tileset tileset = tilesetMap.get(tilesetFile);
            Node tilesetRoot = tileset.getRoot();
            double tilesetGeometricError = tileset.getGeometricError();
            if (tilesetGeometricError == 0.0) {
                tilesetGeometricError = tilesetRoot.getGeometricError();
            }

            geometricError = Math.max(geometricError, tilesetGeometricError);

            Node newChildNode = new Node();
            newChildNode.setRefine(Node.RefineType.REPLACE);
            newChildNode.setGeometricError(tilesetGeometricError);
            if (tilesetRoot.getTransform() != null)
                newChildNode.setTransform(tilesetRoot.getTransform());
            if (tilesetRoot.getBoundingVolume() != null)
                newChildNode.setBoundingVolume(tilesetRoot.getBoundingVolume());

            BoundingVolume boundingVolume = tilesetRoot.getBoundingVolume();
            //BoundingVolume.BoundingVolumeType boundingVolumeType = boundingVolume.getType();

            if (boundingVolume.getBox() != null) {
                // calculate bounding box
            } else if (boundingVolume.getSphere() != null) {
                // calculate bounding sphere
            } else if (boundingVolume.getRegion() != null) {
                // calculate bounding region
                double[] boundingBox = boundingVolume.getRegion();
                globalBoundingRegion[0] = Math.min(globalBoundingRegion[0], boundingBox[0]); // minX
                globalBoundingRegion[1] = Math.min(globalBoundingRegion[1], boundingBox[1]); // minY
                globalBoundingRegion[2] = Math.max(globalBoundingRegion[2], boundingBox[2]); // maxX
                globalBoundingRegion[3] = Math.max(globalBoundingRegion[3], boundingBox[3]); // maxY
                globalBoundingRegion[4] = Math.min(globalBoundingRegion[4], boundingBox[4]); // minZ
                globalBoundingRegion[5] = Math.max(globalBoundingRegion[5], boundingBox[5]); // maxZ
            }

            String uri = getRelativePath(inputPath, tilesetFile);

            Content content = new Content();
            content.setUri(uri);
            newChildNode.setContent(content);

            children.add(newChildNode);
            //root.setChildren(children);
        }


        List<Node> dividedChildren = divideQuadTree(children, null, geometricError, globalBoundingRegion, 0);
        root.setChildren(dividedChildren);

        geometricError = Math.min(geometricError, globalOptions.getMaxGeometricError());

        if (globalOptions.getTilesVersion().equals("1.0")) {
            AssetV1 asset = new AssetV1();
            mergedTileset.setAsset(asset);
        } else {
            AssetV2 asset = new AssetV2();
            mergedTileset.setAsset(asset);
        }
        mergedTileset.setGeometricError(geometricError);
        mergedTileset.setRoot(root);

        BoundingVolume globalBoundingVolume = new BoundingVolume(BoundingVolume.BoundingVolumeType.REGION);
        globalBoundingVolume.setRegion(globalBoundingRegion);
        root.setGeometricError(geometricError);
        root.setBoundingVolume(globalBoundingVolume);

        return mergedTileset;
    }

    private List<Node> divideQuadTree(List<Node> inputChildren, List<Node> outputChildren, double geometricError, double[] globalBoundingRegion, double depth) {
        int maxDepth = MAXIMUM_DEPTH;

        if (outputChildren == null) {
            outputChildren = new ArrayList<>();
        }

        double minX = globalBoundingRegion[0]; // minX
        double minY = globalBoundingRegion[1]; // minY
        double maxX = globalBoundingRegion[2]; // maxX
        double maxY = globalBoundingRegion[3]; // maxY
        double minZ = globalBoundingRegion[4]; // minZ
        double maxZ = globalBoundingRegion[5]; // maxZ

        double centerX = (minX + maxX) / 2;
        double centerY = (minY + maxY) / 2;
        //double centerZ = (minZ + maxZ) / 2;

        Node nodeA = new Node();
        nodeA.setRefine(Node.RefineType.ADD);
        nodeA.setGeometricError(geometricError);
        BoundingVolume boundingVolumeA = new BoundingVolume(BoundingVolume.BoundingVolumeType.REGION);
        boundingVolumeA.setRegion(new double[]{minX, minY, centerX, centerY, minZ, maxZ});
        nodeA.setBoundingVolume(boundingVolumeA);
        nodeA.setChildren(new ArrayList<>());

        Node nodeB = new Node();
        nodeB.setRefine(Node.RefineType.ADD);
        nodeB.setGeometricError(geometricError);
        BoundingVolume boundingVolumeB = new BoundingVolume(BoundingVolume.BoundingVolumeType.REGION);
        boundingVolumeB.setRegion(new double[]{centerX, minY, maxX, centerY, minZ, maxZ});
        nodeB.setBoundingVolume(boundingVolumeB);
        nodeB.setChildren(new ArrayList<>());

        Node nodeC = new Node();
        nodeC.setRefine(Node.RefineType.ADD);
        nodeC.setGeometricError(geometricError);
        BoundingVolume boundingVolumeC = new BoundingVolume(BoundingVolume.BoundingVolumeType.REGION);
        boundingVolumeC.setRegion(new double[]{minX, centerY, centerX, maxY, minZ, maxZ});
        nodeC.setBoundingVolume(boundingVolumeC);
        nodeC.setChildren(new ArrayList<>());

        Node nodeD = new Node();
        nodeD.setRefine(Node.RefineType.ADD);
        nodeD.setGeometricError(geometricError);
        BoundingVolume boundingVolumeD = new BoundingVolume(BoundingVolume.BoundingVolumeType.REGION);
        boundingVolumeD.setRegion(new double[]{centerX, centerY, maxX, maxY, minZ, maxZ});
        nodeD.setBoundingVolume(boundingVolumeD);
        nodeD.setChildren(new ArrayList<>());

        for (Node child : inputChildren) {
            BoundingVolume childBoundingVolume = child.getBoundingVolume();
            double[] childRegion = childBoundingVolume.getRegion();
            double childMinX = childRegion[0];
            double childMinY = childRegion[1];
            double childMaxX = childRegion[2];
            double childMaxY = childRegion[3];

            double childCenterX = (childMinX + childMaxX) / 2;
            double childCenterY = (childMinY + childMaxY) / 2;

            if (childCenterX >= minX && childCenterX <= centerX && childCenterY >= minY && childCenterY <= centerY) {
                nodeA.getChildren().add(child);
            } else if (childCenterX >= centerX && childCenterX <= maxX && childCenterY >= minY && childCenterY <= centerY) {
                nodeB.getChildren().add(child);
            } else if (childCenterX >= minX && childCenterX <= centerX && childCenterY >= centerY && childCenterY <= maxY) {
                nodeC.getChildren().add(child);
            } else if (childCenterX >= centerX && childCenterX <= maxX && childCenterY >= centerY && childCenterY <= maxY) {
                nodeD.getChildren().add(child);
            }
        }

        if (!nodeA.getChildren().isEmpty()) {
            if (nodeA.getChildren().size() > 1 && depth < maxDepth) {
                List<Node> newTree = divideQuadTree(nodeA.getChildren(), null, geometricError, nodeA.getBoundingVolume().getRegion(), depth + 1);
                nodeA.setChildren(newTree);
            }
            nodeA.recalculateBoundingRegion();
            outputChildren.add(nodeA);
        }
        if (!nodeB.getChildren().isEmpty()) {
            if (nodeB.getChildren().size() > 1 && depth < maxDepth) {
                List<Node> newTree = divideQuadTree(nodeB.getChildren(), null, geometricError, nodeB.getBoundingVolume().getRegion(), depth + 1);
                nodeB.setChildren(newTree);
            }
            nodeB.recalculateBoundingRegion();
            outputChildren.add(nodeB);
        }
        if (!nodeC.getChildren().isEmpty()) {
            if (nodeC.getChildren().size() > 1 && depth < maxDepth) {
                List<Node> newTree = divideQuadTree(nodeC.getChildren(), null, geometricError, nodeC.getBoundingVolume().getRegion(), depth + 1);
                nodeC.setChildren(newTree);
            }
            nodeC.recalculateBoundingRegion();
            outputChildren.add(nodeC);
        }
        if (!nodeD.getChildren().isEmpty()) {
            if (nodeD.getChildren().size() > 1 && depth < maxDepth) {
                List<Node> newTree = divideQuadTree(nodeD.getChildren(), null, geometricError, nodeD.getBoundingVolume().getRegion(), depth + 1);
                nodeD.setChildren(newTree);
            }
            nodeD.recalculateBoundingRegion();
            outputChildren.add(nodeD);
        }

        return outputChildren;
    }

    private List<File> findAllTilesetJsons(File inputPath) {
        int maxDepth = 2;
        if (globalOptions.isRecursive()) {
            maxDepth = MAXIMUM_DEPTH;
        }




        List<File> files = (List<File>) FileUtils.listFiles(inputPath, new String[]{"json"}, true);
        files.removeIf(file -> !file.getName().equals("tileset.json"));
        return files;
    }

    private List<File> findAllTilesetJsons(File inputPath, int maxDepth) {
        List<File> files = new ArrayList<>();
        if (inputPath.isDirectory()) {
            File[] subFiles = inputPath.listFiles();
            if (subFiles != null) {
                for (File subFile : subFiles) {
                    if (subFile.isDirectory() && maxDepth > 0) {
                        files.addAll(findAllTilesetJsons(subFile, maxDepth - 1));
                    } else if (subFile.getName().equals("tileset.json")) {
                        files.add(subFile);
                    }
                }
            }
        }
        return files;
    }

    /* getRelativePath */
    private String getRelativePath(File parent, File child) {
        return parent.toURI().relativize(child.toURI()).getPath();
    }
}
