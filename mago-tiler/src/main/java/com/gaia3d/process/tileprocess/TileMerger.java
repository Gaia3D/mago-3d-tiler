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
import com.gaia3d.util.DecimalUtils;
import com.gaia3d.util.GlobeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class TileMerger {

    private GlobalOptions globalOptions = GlobalOptions.getInstance();
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
        tilesetJsons = findAllTilesetJsons(inputPath);
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
                if ("1.0".equals(globalOptions.getTilesVersion())) {
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
            if (tilesetRoot.getTransform() != null) {newChildNode.setTransform(tilesetRoot.getTransform());}
            BoundingVolume normalizedBoundingVolume = toRegionBoundingVolume(tilesetRoot.getBoundingVolume(), tilesetRoot.getTransform());
            newChildNode.setBoundingVolume(normalizedBoundingVolume);

            if (normalizedBoundingVolume != null && normalizedBoundingVolume.getRegion() != null) {
                double[] boundingBox = normalizedBoundingVolume.getRegion();
                globalBoundingRegion[0] = Math.min(globalBoundingRegion[0], boundingBox[0]); // minX
                globalBoundingRegion[1] = Math.min(globalBoundingRegion[1], boundingBox[1]); // minY
                globalBoundingRegion[2] = Math.max(globalBoundingRegion[2], boundingBox[2]); // maxX
                globalBoundingRegion[3] = Math.max(globalBoundingRegion[3], boundingBox[3]); // maxY
                globalBoundingRegion[4] = Math.min(globalBoundingRegion[4], boundingBox[4]); // minZ
                globalBoundingRegion[5] = Math.max(globalBoundingRegion[5], boundingBox[5]); // maxZ
            } else {
                log.warn("[Merge] Skipping tileset with unsupported or empty bounding volume: {}", tilesetFile.getAbsolutePath());
                continue;
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

        if ("1.0".equals(globalOptions.getTilesVersion())) {
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

    static BoundingVolume toRegionBoundingVolume(BoundingVolume boundingVolume, float[] transformArray) {
        if (boundingVolume == null) {
            return null;
        }
        if (boundingVolume.getRegion() != null) {
            return new BoundingVolume(boundingVolume);
        }

        Matrix4d transform = toMatrix4d(transformArray);
        if (boundingVolume.getBox() != null) {
            return cartesianPointsToRegion(boxCorners(boundingVolume.getBox()), transform);
        }
        if (boundingVolume.getSphere() != null) {
            return cartesianPointsToRegion(sphereSamplePoints(boundingVolume.getSphere()), transform);
        }
        return null;
    }

    private static Matrix4d toMatrix4d(float[] transformArray) {
        if (transformArray == null || transformArray.length != 16) {
            return null;
        }
        return new Matrix4d().set(transformArray);
    }

    private static List<Vector3d> boxCorners(double[] box) {
        Vector3d center = new Vector3d(box[0], box[1], box[2]);
        Vector3d halfAxisX = new Vector3d(box[3], box[4], box[5]);
        Vector3d halfAxisY = new Vector3d(box[6], box[7], box[8]);
        Vector3d halfAxisZ = new Vector3d(box[9], box[10], box[11]);

        List<Vector3d> corners = new ArrayList<>(8);
        int[] signs = {-1, 1};
        for (int xSign : signs) {
            for (int ySign : signs) {
                for (int zSign : signs) {
                    Vector3d corner = new Vector3d(center)
                            .add(new Vector3d(halfAxisX).mul(xSign))
                            .add(new Vector3d(halfAxisY).mul(ySign))
                            .add(new Vector3d(halfAxisZ).mul(zSign));
                    corners.add(corner);
                }
            }
        }
        return corners;
    }

    private static List<Vector3d> sphereSamplePoints(double[] sphere) {
        double cx = sphere[0];
        double cy = sphere[1];
        double cz = sphere[2];
        double radius = sphere[3];

        List<Vector3d> points = new ArrayList<>(7);
        points.add(new Vector3d(cx, cy, cz));
        points.add(new Vector3d(cx + radius, cy, cz));
        points.add(new Vector3d(cx - radius, cy, cz));
        points.add(new Vector3d(cx, cy + radius, cz));
        points.add(new Vector3d(cx, cy - radius, cz));
        points.add(new Vector3d(cx, cy, cz + radius));
        points.add(new Vector3d(cx, cy, cz - radius));
        return points;
    }

    private static BoundingVolume cartesianPointsToRegion(List<Vector3d> cartesianPoints, Matrix4d transform) {
        if (cartesianPoints == null || cartesianPoints.isEmpty()) {
            return null;
        }

        double minLonRad = Double.MAX_VALUE;
        double minLatRad = Double.MAX_VALUE;
        double maxLonRad = -Double.MAX_VALUE;
        double maxLatRad = -Double.MAX_VALUE;
        double minAlt = Double.MAX_VALUE;
        double maxAlt = -Double.MAX_VALUE;

        for (Vector3d point : cartesianPoints) {
            Vector3d transformedPoint = new Vector3d(point);
            if (transform != null) {
                transformedPoint.mulPosition(transform);
            }

            Vector3d geographic = GlobeUtils.cartesianToGeographicWgs84(transformedPoint);
            double lonRad = Math.toRadians(geographic.x);
            double latRad = Math.toRadians(geographic.y);
            double alt = geographic.z;

            minLonRad = Math.min(minLonRad, lonRad);
            minLatRad = Math.min(minLatRad, latRad);
            maxLonRad = Math.max(maxLonRad, lonRad);
            maxLatRad = Math.max(maxLatRad, latRad);
            minAlt = Math.min(minAlt, alt);
            maxAlt = Math.max(maxAlt, alt);
        }

        BoundingVolume regionBoundingVolume = new BoundingVolume(BoundingVolume.BoundingVolumeType.REGION);
        regionBoundingVolume.setRegion(new double[]{
                DecimalUtils.cutFast(minLonRad),
                DecimalUtils.cutFast(minLatRad),
                DecimalUtils.cutFast(maxLonRad),
                DecimalUtils.cutFast(maxLatRad),
                DecimalUtils.cutFast(minAlt),
                DecimalUtils.cutFast(maxAlt)
        });
        return regionBoundingVolume;
    }
}
