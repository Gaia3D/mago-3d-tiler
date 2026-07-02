package com.gaia3d.process.tileprocess;

import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gaia3d.util.DecimalUtils;
import com.gaia3d.util.GlobeUtils;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TilesetRootTransformUpdater {
    private static final String TILESET_JSON = "tileset.json";
    private final ObjectMapper objectMapper;

    public TilesetRootTransformUpdater() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
    }

    public void update(String inputPath, String outputPath, String transformText) {
        if (inputPath == null || inputPath.isBlank()) {
            throw new IllegalArgumentException("Input path is required.");
        }
        if (outputPath == null || outputPath.isBlank()) {
            throw new IllegalArgumentException("Output path is required.");
        }

        File inputTileset = resolveInputTileset(inputPath);
        File outputTileset = resolveOutputTileset(outputPath);
        double[] transform = parseTransform(transformText);

        update(inputTileset, outputTileset, transform);
    }

    void update(File inputTileset, File outputTileset, double[] transform) {
        if (!inputTileset.exists() || !inputTileset.isFile()) {
            throw new IllegalArgumentException("tileset.json does not exist: " + inputTileset.getAbsolutePath());
        }
        if (transform == null || transform.length != 16) {
            throw new IllegalArgumentException("Root transform must contain exactly 16 numeric values.");
        }

        try {
            JsonNode tilesetNode = objectMapper.readTree(inputTileset);
            if (!(tilesetNode instanceof ObjectNode tilesetObject)) {
                throw new IllegalArgumentException("tileset.json root must be a JSON object.");
            }

            JsonNode rootNode = tilesetObject.get("root");
            if (!(rootNode instanceof ObjectNode rootObject)) {
                throw new IllegalArgumentException("tileset.json must contain a root object.");
            }

            Matrix4d inputTransform = toMatrix(transform);
            Matrix4d rootTransform = new Matrix4d(inputTransform);
            JsonNode existingRootTransformNode = rootObject.get("transform");
            JsonNode beforeRootBoundingVolume = copyJson(rootObject.get("boundingVolume"));
            if (existingRootTransformNode != null && !existingRootTransformNode.isNull()) {
                rootTransform.mul(toMatrix(readTransform(existingRootTransformNode)));
            }
            JsonNode beforeRootTransform = copyJson(existingRootTransformNode);

            ArrayNode transformNode = objectMapper.createArrayNode();
            for (double value : toArray(rootTransform)) {
                transformNode.add(value);
            }
            rootObject.set("transform", transformNode);
            updateBoundingVolumes(rootObject, rootTransform, inputTransform);
            logRootChanges(beforeRootTransform, rootObject.get("transform"), beforeRootBoundingVolume, rootObject.get("boundingVolume"));

            File parent = outputTileset.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Failed to create output directory: " + parent.getAbsolutePath());
            }
            backupOriginal(inputTileset, outputTileset);
            objectMapper.writeValue(outputTileset, tilesetObject);
            log.info("[UpdateRootTransform] tileset.json is written to {}", outputTileset.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to update tileset root transform.", e);
        }
    }

    double[] parseTransform(String transformText) {
        if (transformText == null || transformText.isBlank()) {
            throw new IllegalArgumentException("Root transform is required.");
        }

        String normalized = transformText
                .replace('[', ' ')
                .replace(']', ' ')
                .replace(';', ' ')
                .replace(',', ' ')
                .trim();
        String[] values = normalized.split("\\s+");
        if (values.length != 16) {
            throw new IllegalArgumentException("Root transform must contain exactly 16 numeric values.");
        }

        double[] transform = new double[16];
        for (int i = 0; i < values.length; i++) {
            transform[i] = Double.parseDouble(values[i]);
        }
        return transform;
    }

    private void backupOriginal(File inputTileset, File outputTileset) throws IOException {
        File backup = nextBackupFile(outputTileset);
        Files.copy(inputTileset.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        log.info("[UpdateRootTransform] Original tileset.json is backed up to {}", backup.getAbsolutePath());
    }

    private void updateBoundingVolumes(ObjectNode node, Matrix4d cumulativeTransform, Matrix4d regionTransform) {
        updateBoundingVolume(node, cumulativeTransform, regionTransform);
        JsonNode contentNode = node.get("content");
        if (contentNode instanceof ObjectNode contentObject) {
            updateBoundingVolume(contentObject, cumulativeTransform, regionTransform);
        }

        JsonNode childrenNode = node.get("children");
        if (childrenNode == null || !childrenNode.isArray()) {
            return;
        }

        for (JsonNode childNode : childrenNode) {
            if (!(childNode instanceof ObjectNode childObject)) {
                continue;
            }

            Matrix4d childTransform = new Matrix4d(cumulativeTransform);
            JsonNode transformNode = childObject.get("transform");
            if (transformNode != null && !transformNode.isNull()) {
                childTransform.mul(toMatrix(readTransform(transformNode)));
            }
            updateBoundingVolumes(childObject, childTransform, regionTransform);
        }
    }

    private void updateBoundingVolume(ObjectNode owner, Matrix4d cumulativeTransform, Matrix4d regionTransform) {
        JsonNode boundingVolumeNode = owner.get("boundingVolume");
        if (!(boundingVolumeNode instanceof ObjectNode boundingVolumeObject)) {
            return;
        }

        BoundingVolumePoints boundingVolumePoints = toCartesianPoints(boundingVolumeObject);
        if (boundingVolumePoints.points().isEmpty()) {
            log.warn("[UpdateRootTransform] Unsupported or empty boundingVolume is skipped.");
            return;
        }

        Matrix4d transform = boundingVolumePoints.region() ? regionTransform : cumulativeTransform;
        double[] region = toTransformedRegion(boundingVolumePoints.points(), transform);
        ArrayNode regionNode = objectMapper.createArrayNode();
        for (double value : region) {
            regionNode.add(value);
        }

        boundingVolumeObject.remove("box");
        boundingVolumeObject.remove("sphere");
        boundingVolumeObject.set("region", regionNode);
    }

    private BoundingVolumePoints toCartesianPoints(ObjectNode boundingVolume) {
        JsonNode boxNode = boundingVolume.get("box");
        if (boxNode != null && boxNode.isArray() && boxNode.size() == 12) {
            return new BoundingVolumePoints(boxCorners(readArray(boxNode, 12)), false);
        }

        JsonNode sphereNode = boundingVolume.get("sphere");
        if (sphereNode != null && sphereNode.isArray() && sphereNode.size() == 4) {
            return new BoundingVolumePoints(sphereSamplePoints(readArray(sphereNode, 4)), false);
        }

        JsonNode regionNode = boundingVolume.get("region");
        if (regionNode != null && regionNode.isArray() && regionNode.size() == 6) {
            return new BoundingVolumePoints(regionCornerPoints(readArray(regionNode, 6)), true);
        }

        return new BoundingVolumePoints(List.of(), false);
    }

    private double[] toTransformedRegion(List<Vector3d> cartesianPoints, Matrix4d cumulativeTransform) {
        double minLonRad = Double.MAX_VALUE;
        double minLatRad = Double.MAX_VALUE;
        double maxLonRad = -Double.MAX_VALUE;
        double maxLatRad = -Double.MAX_VALUE;
        double minAlt = Double.MAX_VALUE;
        double maxAlt = -Double.MAX_VALUE;

        for (Vector3d point : cartesianPoints) {
            Vector3d transformedPoint = new Vector3d(point).mulPosition(cumulativeTransform);
            Vector3d geographic = GlobeUtils.cartesianToGeographicWgs84(transformedPoint);
            double lonRad = Math.toRadians(geographic.x);
            double latRad = Math.toRadians(geographic.y);

            minLonRad = Math.min(minLonRad, lonRad);
            minLatRad = Math.min(minLatRad, latRad);
            maxLonRad = Math.max(maxLonRad, lonRad);
            maxLatRad = Math.max(maxLatRad, latRad);
            minAlt = Math.min(minAlt, geographic.z);
            maxAlt = Math.max(maxAlt, geographic.z);
        }

        return new double[]{
                DecimalUtils.cutFast(minLonRad),
                DecimalUtils.cutFast(minLatRad),
                DecimalUtils.cutFast(maxLonRad),
                DecimalUtils.cutFast(maxLatRad),
                DecimalUtils.cutFast(minAlt),
                DecimalUtils.cutFast(maxAlt)
        };
    }

    private List<Vector3d> boxCorners(double[] box) {
        Vector3d center = new Vector3d(box[0], box[1], box[2]);
        Vector3d halfAxisX = new Vector3d(box[3], box[4], box[5]);
        Vector3d halfAxisY = new Vector3d(box[6], box[7], box[8]);
        Vector3d halfAxisZ = new Vector3d(box[9], box[10], box[11]);

        List<Vector3d> corners = new ArrayList<>(8);
        int[] signs = {-1, 1};
        for (int xSign : signs) {
            for (int ySign : signs) {
                for (int zSign : signs) {
                    corners.add(new Vector3d(center)
                            .add(new Vector3d(halfAxisX).mul(xSign))
                            .add(new Vector3d(halfAxisY).mul(ySign))
                            .add(new Vector3d(halfAxisZ).mul(zSign)));
                }
            }
        }
        return corners;
    }

    private List<Vector3d> sphereSamplePoints(double[] sphere) {
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

    private List<Vector3d> regionCornerPoints(double[] region) {
        List<Vector3d> points = new ArrayList<>(8);
        double[] longitudes = {region[0], region[2]};
        double[] latitudes = {region[1], region[3]};
        double[] altitudes = {region[4], region[5]};

        for (double lonRad : longitudes) {
            for (double latRad : latitudes) {
                for (double altitude : altitudes) {
                    points.add(GlobeUtils.geographicToCartesianWgs84(new Vector3d(
                            Math.toDegrees(lonRad),
                            Math.toDegrees(latRad),
                            altitude
                    )));
                }
            }
        }
        return points;
    }

    private Matrix4d toMatrix(double[] transform) {
        return new Matrix4d().set(transform);
    }

    private double[] toArray(Matrix4d transform) {
        return transform.get(new double[16]);
    }

    private double[] readTransform(JsonNode transformNode) {
        return readArray(transformNode, 16);
    }

    private double[] readArray(JsonNode arrayNode, int expectedSize) {
        if (!arrayNode.isArray() || arrayNode.size() != expectedSize) {
            throw new IllegalArgumentException("Expected numeric array size " + expectedSize + ".");
        }

        double[] values = new double[expectedSize];
        for (int i = 0; i < expectedSize; i++) {
            JsonNode value = arrayNode.get(i);
            if (!value.isNumber()) {
                throw new IllegalArgumentException("Expected numeric array value.");
            }
            values[i] = value.asDouble();
        }
        return values;
    }

    private void logRootChanges(JsonNode beforeTransform, JsonNode afterTransform, JsonNode beforeBoundingVolume, JsonNode afterBoundingVolume) {
        log.info("[UpdateRootTransform] Root transform before: {}", toLogString(beforeTransform));
        log.info("[UpdateRootTransform] Root transform after : {}", toLogString(afterTransform));
        log.info("[UpdateRootTransform] Root boundingVolume before: {}", toLogString(beforeBoundingVolume));
        log.info("[UpdateRootTransform] Root boundingVolume after : {}", toLogString(afterBoundingVolume));
    }

    private JsonNode copyJson(JsonNode node) {
        return node == null ? null : node.deepCopy();
    }

    private String toLogString(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return "<none>";
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (IOException e) {
            return node.toString();
        }
    }

    private File nextBackupFile(File tilesetFile) {
        File parent = tilesetFile.getParentFile();
        File backup = new File(parent, "tileset-original.json");
        if (!backup.exists()) {
            return backup;
        }

        int index = 1;
        do {
            backup = new File(parent, "tileset-original-" + index + ".json");
            index++;
        } while (backup.exists());
        return backup;
    }

    private File resolveInputTileset(String inputPath) {
        File input = new File(inputPath);
        if (input.isDirectory()) {
            return new File(input, TILESET_JSON);
        }
        return input;
    }

    private File resolveOutputTileset(String outputPath) {
        File output = new File(outputPath);
        if (output.exists() && output.isDirectory()) {
            return new File(output, TILESET_JSON);
        }
        if (!TILESET_JSON.equalsIgnoreCase(output.getName()) && !output.getName().toLowerCase().endsWith(".json")) {
            return new File(output, TILESET_JSON);
        }
        return output;
    }

    private record BoundingVolumePoints(List<Vector3d> points, boolean region) {
    }
}
