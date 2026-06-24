package com.gaia3d.converter.assimp.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class GaiaSceneValidationReport {
    private final File sourceFile;
    private final List<String> details = new ArrayList<>();
    private boolean sourceFileExists;
    private boolean sourceFileReadable;
    private int sceneCount;
    private int nodeCount;
    private int transformCount;
    private int meshCount;
    private int primitiveCount;
    private int materialCount;
    private int textureCount;
    private int vertexCount;
    private int positionCount;
    private int normalCount;
    private int texcoordCount;
    private int colorCount;
    private int faceCount;
    private int indexCount;
    private int invalidVertexCount;
    private int invalidFaceCount;
    private int invalidIndexCount;
    private int nonTriangleFaceCount;
    private int invalidPositionCount;
    private int invalidNormalCount;
    private int suspiciousNormalCount;
    private int invalidTexcoordCount;
    private int invalidColorCount;
    private int invalidBatchIdCount;
    private int outOfRangeTexcoordCount;
    private int degenerateTriangleCount;
    private int invalidTransformCount;
    private int invalidMaterialIndexCount;
    private int invalidTexturePathCount;
    private int missingExternalResourceCount;
    private int unreadableExternalResourceCount;
    private GaiaBoundingBox boundingBox;
    private Vector3d center;
    private int issueCount;

    public GaiaSceneValidationReport(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public boolean hasIssues() {
        return issueCount > 0;
    }

    public String toSummaryString() {
        return "source=" + sourceName()
                + ", sourceExists=" + sourceFileExists
                + ", sourceReadable=" + sourceFileReadable
                + ", scenes=" + sceneCount
                + ", nodes=" + nodeCount
                + ", transforms=" + transformCount
                + ", meshes=" + meshCount
                + ", primitives=" + primitiveCount
                + ", materials=" + materialCount
                + ", textures=" + textureCount
                + ", vertices=" + vertexCount
                + ", positions=" + positionCount
                + ", normals=" + normalCount
                + ", texcoords=" + texcoordCount
                + ", colors=" + colorCount
                + ", faces=" + faceCount
                + ", indices=" + indexCount
                + ", boundingBox=" + boundingBoxString()
                + ", center=" + center
                + ", invalidVertices=" + invalidVertexCount
                + ", invalidFaces=" + invalidFaceCount
                + ", invalidIndices=" + invalidIndexCount
                + ", nonTriangleFaces=" + nonTriangleFaceCount
                + ", invalidPositions=" + invalidPositionCount
                + ", invalidNormals=" + invalidNormalCount
                + ", suspiciousNormals=" + suspiciousNormalCount
                + ", invalidTexcoords=" + invalidTexcoordCount
                + ", invalidColors=" + invalidColorCount
                + ", invalidBatchIds=" + invalidBatchIdCount
                + ", outOfRangeTexcoords=" + outOfRangeTexcoordCount
                + ", degenerateTriangles=" + degenerateTriangleCount
                + ", invalidTransforms=" + invalidTransformCount
                + ", invalidMaterialIndices=" + invalidMaterialIndexCount
                + ", invalidTexturePaths=" + invalidTexturePathCount
                + ", missingExternalResources=" + missingExternalResourceCount
                + ", unreadableExternalResources=" + unreadableExternalResourceCount
                + ", issues=" + issueCount;
    }

    public String toDetailString() {
        if (details.isEmpty()) {
            return toSummaryString();
        }
        return toSummaryString() + System.lineSeparator() + String.join(System.lineSeparator(), details);
    }

    public void addIssue(String issue) {
        issueCount++;
        if (details.size() < GaiaSceneValidator.MAX_DETAILS) {
            details.add(" - " + issue);
        }
    }

    public String toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        root.put("source", sourceName());
        root.put("sourceFileExists", sourceFileExists);
        root.put("sourceFileReadable", sourceFileReadable);
        root.put("hasIssues", hasIssues());
        root.put("issueCount", issueCount);

        ObjectNode counts = root.putObject("counts");
        counts.put("scenes", sceneCount);
        counts.put("nodes", nodeCount);
        counts.put("transforms", transformCount);
        counts.put("meshes", meshCount);
        counts.put("primitives", primitiveCount);
        counts.put("materials", materialCount);
        counts.put("textures", textureCount);
        counts.put("vertices", vertexCount);
        counts.put("positions", positionCount);
        counts.put("normals", normalCount);
        counts.put("texcoords", texcoordCount);
        counts.put("colors", colorCount);
        counts.put("faces", faceCount);
        counts.put("indices", indexCount);

        ObjectNode issues = root.putObject("invalidCounts");
        issues.put("invalidVertices", invalidVertexCount);
        issues.put("invalidFaces", invalidFaceCount);
        issues.put("invalidIndices", invalidIndexCount);
        issues.put("nonTriangleFaces", nonTriangleFaceCount);
        issues.put("invalidPositions", invalidPositionCount);
        issues.put("invalidNormals", invalidNormalCount);
        issues.put("suspiciousNormals", suspiciousNormalCount);
        issues.put("invalidTexcoords", invalidTexcoordCount);
        issues.put("invalidColors", invalidColorCount);
        issues.put("invalidBatchIds", invalidBatchIdCount);
        issues.put("outOfRangeTexcoords", outOfRangeTexcoordCount);
        issues.put("degenerateTriangles", degenerateTriangleCount);
        issues.put("invalidTransforms", invalidTransformCount);
        issues.put("invalidMaterialIndices", invalidMaterialIndexCount);
        issues.put("invalidTexturePaths", invalidTexturePathCount);
        issues.put("missingExternalResources", missingExternalResourceCount);
        issues.put("unreadableExternalResources", unreadableExternalResourceCount);

        if (boundingBox != null && boundingBox.isInit()) {
            ObjectNode bbox = root.putObject("boundingBox");
            ObjectNode min = bbox.putObject("min");
            min.put("x", boundingBox.getMinX());
            min.put("y", boundingBox.getMinY());
            min.put("z", boundingBox.getMinZ());
            ObjectNode max = bbox.putObject("max");
            max.put("x", boundingBox.getMaxX());
            max.put("y", boundingBox.getMaxY());
            max.put("z", boundingBox.getMaxZ());
        } else {
            root.putNull("boundingBox");
        }

        if (center != null) {
            ObjectNode c = root.putObject("center");
            c.put("x", center.x());
            c.put("y", center.y());
            c.put("z", center.z());
        } else {
            root.putNull("center");
        }

        ArrayNode detailsNode = root.putArray("details");
        details.forEach(detailsNode::add);

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize validation report to JSON", e);
        }
    }

    public void writeJsonFile(File outputFile) throws IOException {
        Files.writeString(outputFile.toPath(), toJson(), StandardCharsets.UTF_8);
    }

    public String sourceName() {
        if (sourceFile == null) {
            return "unknown";
        }
        return sourceFile.getAbsolutePath();
    }

    private String boundingBoxString() {
        if (boundingBox == null || !boundingBox.isInit()) {
            return "none";
        }
        return "[min=(" + boundingBox.getMinX() + ", " + boundingBox.getMinY() + ", " + boundingBox.getMinZ()
                + "), max=(" + boundingBox.getMaxX() + ", " + boundingBox.getMaxY() + ", " + boundingBox.getMaxZ() + ")]";
    }
}
