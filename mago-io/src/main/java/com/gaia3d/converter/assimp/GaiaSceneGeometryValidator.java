package com.gaia3d.converter.assimp;

import com.gaia3d.basic.model.GaiaFace;
import com.gaia3d.basic.model.GaiaMesh;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaPrimitive;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.GaiaSurface;
import com.gaia3d.basic.model.GaiaVertex;
import lombok.Getter;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GaiaSceneGeometryValidator {
    private static final int MAX_DETAILS = 20;
    private static final double NORMAL_LENGTH_EPSILON = 1e-3;
    private static final double DEGENERATE_TRIANGLE_EPSILON = 1e-12;

    public ValidationReport validate(File sourceFile, List<GaiaScene> scenes) {
        ValidationReport report = new ValidationReport(sourceFile);
        if (scenes == null || scenes.isEmpty()) {
            report.addIssue("No scenes were converted.");
            return report;
        }

        for (int sceneIndex = 0; sceneIndex < scenes.size(); sceneIndex++) {
            GaiaScene scene = scenes.get(sceneIndex);
            if (scene == null) {
                report.addIssue(path(sceneIndex, -1, -1, -1) + " scene is null.");
                continue;
            }
            validateScene(report, scene, sceneIndex);
        }
        return report;
    }

    private void validateScene(ValidationReport report, GaiaScene scene, int sceneIndex) {
        if (scene.getNodes() == null || scene.getNodes().isEmpty()) {
            report.addIssue(path(sceneIndex, -1, -1, -1) + " scene has no nodes.");
            return;
        }

        for (int nodeIndex = 0; nodeIndex < scene.getNodes().size(); nodeIndex++) {
            validateNode(report, scene.getNodes().get(nodeIndex), sceneIndex, nodeIndex);
        }
    }

    private void validateNode(ValidationReport report, GaiaNode node, int sceneIndex, int nodeIndex) {
        if (node == null) {
            report.addIssue(path(sceneIndex, nodeIndex, -1, -1) + " node is null.");
            return;
        }

        List<GaiaMesh> meshes = node.getMeshes();
        if (meshes != null) {
            for (int meshIndex = 0; meshIndex < meshes.size(); meshIndex++) {
                validateMesh(report, meshes.get(meshIndex), sceneIndex, nodeIndex, meshIndex);
            }
        }

        List<GaiaNode> children = node.getChildren();
        if (children != null) {
            for (int childIndex = 0; childIndex < children.size(); childIndex++) {
                validateNode(report, children.get(childIndex), sceneIndex, childIndex);
            }
        }
    }

    private void validateMesh(ValidationReport report, GaiaMesh mesh, int sceneIndex, int nodeIndex, int meshIndex) {
        if (mesh == null) {
            report.addIssue(path(sceneIndex, nodeIndex, meshIndex, -1) + " mesh is null.");
            return;
        }

        List<GaiaPrimitive> primitives = mesh.getPrimitives();
        if (primitives == null || primitives.isEmpty()) {
            report.addIssue(path(sceneIndex, nodeIndex, meshIndex, -1) + " mesh has no primitives.");
            return;
        }

        for (int primitiveIndex = 0; primitiveIndex < primitives.size(); primitiveIndex++) {
            validatePrimitive(report, primitives.get(primitiveIndex), sceneIndex, nodeIndex, meshIndex, primitiveIndex);
        }
    }

    private void validatePrimitive(ValidationReport report, GaiaPrimitive primitive, int sceneIndex, int nodeIndex, int meshIndex, int primitiveIndex) {
        String path = path(sceneIndex, nodeIndex, meshIndex, primitiveIndex);
        if (primitive == null) {
            report.addIssue(path + " primitive is null.");
            return;
        }

        List<GaiaVertex> vertices = primitive.getVertices();
        if (vertices == null || vertices.isEmpty()) {
            report.addIssue(path + " primitive has no vertices.");
            return;
        }

        report.primitiveCount++;
        report.vertexCount += vertices.size();
        validateVertices(report, vertices, path);

        List<GaiaSurface> surfaces = primitive.getSurfaces();
        if (surfaces == null || surfaces.isEmpty()) {
            report.addIssue(path + " primitive has no surfaces.");
            return;
        }

        for (int surfaceIndex = 0; surfaceIndex < surfaces.size(); surfaceIndex++) {
            validateSurface(report, surfaces.get(surfaceIndex), vertices, path + "/surface[" + surfaceIndex + "]");
        }
    }

    private void validateVertices(ValidationReport report, List<GaiaVertex> vertices, String path) {
        int normals = 0;
        int texcoords = 0;
        for (int vertexIndex = 0; vertexIndex < vertices.size(); vertexIndex++) {
            GaiaVertex vertex = vertices.get(vertexIndex);
            if (vertex == null) {
                report.invalidVertexCount++;
                report.addIssue(path + "/vertex[" + vertexIndex + "] vertex is null.");
                continue;
            }

            Vector3d position = vertex.getPosition();
            if (!isFinite(position)) {
                report.invalidPositionCount++;
                report.addIssue(path + "/vertex[" + vertexIndex + "] has invalid position: " + position);
            }

            Vector3d normal = vertex.getNormal();
            if (normal != null) {
                normals++;
                if (!isFinite(normal)) {
                    report.invalidNormalCount++;
                    report.addIssue(path + "/vertex[" + vertexIndex + "] has invalid normal: " + normal);
                } else {
                    double length = normal.length();
                    if (length <= NORMAL_LENGTH_EPSILON || Math.abs(1.0 - length) > NORMAL_LENGTH_EPSILON) {
                        report.suspiciousNormalCount++;
                        report.addIssue(path + "/vertex[" + vertexIndex + "] has suspicious normal length: " + length);
                    }
                }
            }

            Vector2d texcoord = vertex.getTexcoords();
            if (texcoord != null) {
                texcoords++;
                if (!isFinite(texcoord)) {
                    report.invalidTexcoordCount++;
                    report.addIssue(path + "/vertex[" + vertexIndex + "] has invalid texcoord: " + texcoord);
                } else if (texcoord.x() < 0.0 || texcoord.x() > 1.0 || texcoord.y() < 0.0 || texcoord.y() > 1.0) {
                    report.outOfRangeTexcoordCount++;
                    report.addIssue(path + "/vertex[" + vertexIndex + "] has out-of-range texcoord: " + texcoord);
                }
            }
        }

        if (normals != 0 && normals != vertices.size()) {
            report.addIssue(path + " has partial normals. normals=" + normals + ", vertices=" + vertices.size());
        }
        if (texcoords != 0 && texcoords != vertices.size()) {
            report.addIssue(path + " has partial texcoords. texcoords=" + texcoords + ", vertices=" + vertices.size());
        }
    }

    private void validateSurface(ValidationReport report, GaiaSurface surface, List<GaiaVertex> vertices, String path) {
        if (surface == null) {
            report.addIssue(path + " surface is null.");
            return;
        }

        List<GaiaFace> faces = surface.getFaces();
        if (faces == null || faces.isEmpty()) {
            report.addIssue(path + " surface has no faces.");
            return;
        }

        for (int faceIndex = 0; faceIndex < faces.size(); faceIndex++) {
            validateFace(report, faces.get(faceIndex), vertices, path + "/face[" + faceIndex + "]");
        }
    }

    private void validateFace(ValidationReport report, GaiaFace face, List<GaiaVertex> vertices, String path) {
        if (face == null) {
            report.invalidFaceCount++;
            report.addIssue(path + " face is null.");
            return;
        }

        int[] indices = face.getIndices();
        if (indices == null || indices.length == 0) {
            report.invalidFaceCount++;
            report.addIssue(path + " face has no indices.");
            return;
        }

        report.faceCount++;
        report.indexCount += indices.length;
        if (indices.length != 3) {
            report.nonTriangleFaceCount++;
            report.addIssue(path + " face is not triangular. indices=" + indices.length);
        }

        boolean validRange = true;
        for (int index : indices) {
            if (index < 0 || index >= vertices.size()) {
                validRange = false;
                report.invalidIndexCount++;
                report.addIssue(path + " has out-of-range index " + index + " for vertexCount=" + vertices.size());
            }
        }

        if (validRange && indices.length == 3 && isDegenerate(vertices, indices)) {
            report.degenerateTriangleCount++;
            report.addIssue(path + " is degenerate.");
        }
    }

    private boolean isDegenerate(List<GaiaVertex> vertices, int[] indices) {
        Vector3d p0 = vertices.get(indices[0]).getPosition();
        Vector3d p1 = vertices.get(indices[1]).getPosition();
        Vector3d p2 = vertices.get(indices[2]).getPosition();
        if (!isFinite(p0) || !isFinite(p1) || !isFinite(p2)) {
            return true;
        }

        Vector3d edge0 = new Vector3d(p1).sub(p0);
        Vector3d edge1 = new Vector3d(p2).sub(p0);
        return edge0.cross(edge1).lengthSquared() <= DEGENERATE_TRIANGLE_EPSILON;
    }

    private boolean isFinite(Vector3d value) {
        return value != null && Double.isFinite(value.x()) && Double.isFinite(value.y()) && Double.isFinite(value.z());
    }

    private boolean isFinite(Vector2d value) {
        return value != null && Double.isFinite(value.x()) && Double.isFinite(value.y());
    }

    private String path(int sceneIndex, int nodeIndex, int meshIndex, int primitiveIndex) {
        StringBuilder builder = new StringBuilder("scene[").append(sceneIndex).append("]");
        if (nodeIndex >= 0) {
            builder.append("/node[").append(nodeIndex).append("]");
        }
        if (meshIndex >= 0) {
            builder.append("/mesh[").append(meshIndex).append("]");
        }
        if (primitiveIndex >= 0) {
            builder.append("/primitive[").append(primitiveIndex).append("]");
        }
        return builder.toString();
    }

    @Getter
    public static class ValidationReport {
        private final File sourceFile;
        private final List<String> details = new ArrayList<>();
        private int primitiveCount;
        private int vertexCount;
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
        private int outOfRangeTexcoordCount;
        private int degenerateTriangleCount;
        private int issueCount;

        private ValidationReport(File sourceFile) {
            this.sourceFile = sourceFile;
        }

        public boolean hasIssues() {
            return issueCount > 0 ||
                    invalidVertexCount > 0 ||
                    invalidFaceCount > 0 ||
                    invalidIndexCount > 0 ||
                    nonTriangleFaceCount > 0 ||
                    invalidPositionCount > 0 ||
                    invalidNormalCount > 0 ||
                    suspiciousNormalCount > 0 ||
                    invalidTexcoordCount > 0 ||
                    outOfRangeTexcoordCount > 0 ||
                    degenerateTriangleCount > 0;
        }

        public String toSummaryString() {
            return "source=" + sourceName() +
                    ", primitives=" + primitiveCount +
                    ", vertices=" + vertexCount +
                    ", faces=" + faceCount +
                    ", indices=" + indexCount +
                    ", invalidVertices=" + invalidVertexCount +
                    ", invalidFaces=" + invalidFaceCount +
                    ", invalidIndices=" + invalidIndexCount +
                    ", nonTriangleFaces=" + nonTriangleFaceCount +
                    ", invalidPositions=" + invalidPositionCount +
                    ", invalidNormals=" + invalidNormalCount +
                    ", suspiciousNormals=" + suspiciousNormalCount +
                    ", invalidTexcoords=" + invalidTexcoordCount +
                    ", outOfRangeTexcoords=" + outOfRangeTexcoordCount +
                    ", degenerateTriangles=" + degenerateTriangleCount +
                    ", issues=" + issueCount;
        }

        public String toDetailString() {
            if (details.isEmpty()) {
                return toSummaryString();
            }
            return toSummaryString() + System.lineSeparator() + String.join(System.lineSeparator(), details);
        }

        private void addIssue(String issue) {
            issueCount++;
            if (details.size() < MAX_DETAILS) {
                details.add(" - " + issue);
            }
        }

        private String sourceName() {
            if (sourceFile == null) {
                return "unknown";
            }
            return sourceFile.getAbsolutePath();
        }
    }
}
