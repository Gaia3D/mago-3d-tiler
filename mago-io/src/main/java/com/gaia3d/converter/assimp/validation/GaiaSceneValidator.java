package com.gaia3d.converter.assimp.validation;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.types.TextureType;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;

public class GaiaSceneValidator {
    public static final int MAX_DETAILS = 100;
    public static final double NORMAL_LENGTH_EPSILON = 1e-3;
    public static final double DEGENERATE_TRIANGLE_EPSILON = 1e-12;

    public GaiaSceneValidationReport validate(File sourceFile, List<GaiaScene> scenes) {
        GaiaSceneValidationReport report = new GaiaSceneValidationReport(sourceFile);
        validateSourceFile(report, sourceFile);
        if (scenes == null || scenes.isEmpty()) {
            report.addIssue("No scenes were converted.");
            return report;
        }

        report.setSceneCount(scenes.size());
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        for (int sceneIndex = 0; sceneIndex < scenes.size(); sceneIndex++) {
            GaiaScene scene = scenes.get(sceneIndex);
            String scenePath = "scene[" + sceneIndex + "]";
            if (scene == null) {
                report.addIssue(scenePath + " is null.");
                continue;
            }
            validateScene(report, scene, scenePath, boundingBox);
        }
        if (boundingBox.isInit()) {
            report.setBoundingBox(boundingBox);
            report.setCenter(boundingBox.getCenter());
        } else {
            report.addIssue("No finite positions were available to calculate the bounding box.");
        }
        return report;
    }

    private void validateSourceFile(GaiaSceneValidationReport report, File sourceFile) {
        boolean exists = sourceFile != null && sourceFile.isFile();
        boolean readable = exists && Files.isReadable(sourceFile.toPath());
        report.setSourceFileExists(exists);
        report.setSourceFileReadable(readable);
        if (!exists) {
            report.addIssue("Source file does not exist or is not a regular file: " + report.sourceName());
        } else if (!readable) {
            report.addIssue("Source file is not readable: " + report.sourceName());
        }
    }

    private void validateScene(GaiaSceneValidationReport report, GaiaScene scene, String scenePath, GaiaBoundingBox boundingBox) {
        if (scene.getTranslation() != null && !isFinite(scene.getTranslation())) {
            report.addIssue(scenePath + " has invalid translation: " + scene.getTranslation());
        }

        List<GaiaMaterial> materials = scene.getMaterials();
        validateMaterials(report, materials, scenePath);
        int materialCount = materials == null ? 0 : materials.size();

        List<GaiaNode> nodes = scene.getNodes();
        if (nodes == null || nodes.isEmpty()) {
            report.addIssue(scenePath + " has no nodes.");
            return;
        }

        Set<GaiaNode> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            validateNode(report, nodes.get(nodeIndex), scenePath + "/node[" + nodeIndex + "]", new Matrix4d().identity(), materialCount, boundingBox, visited);
        }
    }

    private void validateMaterials(GaiaSceneValidationReport report, List<GaiaMaterial> materials, String scenePath) {
        if (materials == null) {
            report.addIssue(scenePath + " material list is null.");
            return;
        }
        report.setMaterialCount(report.getMaterialCount() + materials.size());
        for (int materialIndex = 0; materialIndex < materials.size(); materialIndex++) {
            GaiaMaterial material = materials.get(materialIndex);
            String materialPath = scenePath + "/material[" + materialIndex + "]";
            if (material == null) {
                report.addIssue(materialPath + " is null.");
                continue;
            }
            Map<TextureType, List<GaiaTexture>> textures = material.getTextures();
            if (textures == null) {
                report.addIssue(materialPath + " texture map is null.");
                continue;
            }
            for (Map.Entry<TextureType, List<GaiaTexture>> entry : textures.entrySet()) {
                List<GaiaTexture> textureList = entry.getValue();
                if (textureList == null) {
                    report.addIssue(materialPath + "/texture[" + entry.getKey() + "] list is null.");
                    continue;
                }
                for (int textureIndex = 0; textureIndex < textureList.size(); textureIndex++) {
                    validateTexture(report, textureList.get(textureIndex), materialPath + "/texture[" + entry.getKey() + "][" + textureIndex + "]");
                }
            }
        }
    }

    private void validateTexture(GaiaSceneValidationReport report, GaiaTexture texture, String path) {
        report.setTextureCount(report.getTextureCount() + 1);
        if (texture == null) {
            report.setInvalidTexturePathCount(report.getInvalidTexturePathCount() + 1);
            report.addIssue(path + " is null.");
            return;
        }

        String texturePath = texture.getPath();
        if (texturePath == null || texturePath.isBlank()) {
            report.setInvalidTexturePathCount(report.getInvalidTexturePathCount() + 1);
            report.addIssue(path + " has an empty path.");
            return;
        }

        try {
            Path resolved = Path.of(texturePath);
            if (!resolved.isAbsolute()) {
                String parentPath = texture.getParentPath();
                Path base = parentPath == null || parentPath.isBlank() ? sourceParent(report.getSourceFile()) : Path.of(parentPath);
                if (base != null) {
                    resolved = base.resolve(resolved);
                }
            }
            resolved = resolved.toAbsolutePath().normalize();
            if (!Files.isRegularFile(resolved)) {
                report.setMissingExternalResourceCount(report.getMissingExternalResourceCount() + 1);
                report.addIssue(path + " does not exist: " + resolved);
            } else if (!Files.isReadable(resolved)) {
                report.setUnreadableExternalResourceCount(report.getUnreadableExternalResourceCount() + 1);
                report.addIssue(path + " is not readable: " + resolved);
            }
        } catch (InvalidPathException exception) {
            report.setInvalidTexturePathCount(report.getInvalidTexturePathCount() + 1);
            report.addIssue(path + " has an invalid path: " + texturePath);
        }
    }

    private Path sourceParent(File sourceFile) {
        return sourceFile == null ? null : sourceFile.toPath().toAbsolutePath().getParent();
    }

    private void validateNode(GaiaSceneValidationReport report, GaiaNode node, String path, Matrix4d parentTransform, int materialCount, GaiaBoundingBox boundingBox, Set<GaiaNode> visited) {
        if (node == null) {
            report.addIssue(path + " is null.");
            return;
        }
        if (!visited.add(node)) {
            report.addIssue(path + " contains a repeated or cyclic node reference.");
            return;
        }
        report.setNodeCount(report.getNodeCount() + 1);
        report.setTransformCount(report.getTransformCount() + 1);

        Matrix4d worldTransform = new Matrix4d(parentTransform);
        Matrix4d localTransform = node.getTransformMatrix();
        if (!isFinite(localTransform)) {
            report.setInvalidTransformCount(report.getInvalidTransformCount() + 1);
            report.addIssue(path + " has a null or non-finite transform matrix: " + localTransform);
        } else {
            worldTransform.mul(localTransform);
        }

        List<GaiaMesh> meshes = node.getMeshes();
        if (meshes == null) {
            report.addIssue(path + " mesh list is null.");
        } else {
            report.setMeshCount(report.getMeshCount() + meshes.size());
            for (int meshIndex = 0; meshIndex < meshes.size(); meshIndex++) {
                validateMesh(report, meshes.get(meshIndex), path + "/mesh[" + meshIndex + "]", worldTransform, materialCount, boundingBox);
            }
        }

        List<GaiaNode> children = node.getChildren();
        if (children == null) {
            report.addIssue(path + " child list is null.");
            return;
        }
        for (int childIndex = 0; childIndex < children.size(); childIndex++) {
            validateNode(report, children.get(childIndex), path + "/node[" + childIndex + "]", worldTransform, materialCount, boundingBox, visited);
        }
    }

    private void validateMesh(GaiaSceneValidationReport report, GaiaMesh mesh, String path, Matrix4d worldTransform, int materialCount, GaiaBoundingBox boundingBox) {
        if (mesh == null) {
            report.addIssue(path + " is null.");
            return;
        }
        List<GaiaPrimitive> primitives = mesh.getPrimitives();
        if (primitives == null || primitives.isEmpty()) {
            report.addIssue(path + " has no primitives.");
            return;
        }
        for (int primitiveIndex = 0; primitiveIndex < primitives.size(); primitiveIndex++) {
            validatePrimitive(report, primitives.get(primitiveIndex), path + "/primitive[" + primitiveIndex + "]", worldTransform, materialCount, boundingBox);
        }
    }

    private void validatePrimitive(GaiaSceneValidationReport report, GaiaPrimitive primitive, String path, Matrix4d worldTransform, int materialCount, GaiaBoundingBox boundingBox) {
        if (primitive == null) {
            report.addIssue(path + " is null.");
            return;
        }
        report.setPrimitiveCount(report.getPrimitiveCount() + 1);
        Integer materialIndex = primitive.getMaterialIndex();
        if (materialIndex == null || materialIndex < -1 || materialIndex >= materialCount) {
            report.setInvalidMaterialIndexCount(report.getInvalidMaterialIndexCount() + 1);
            report.addIssue(path + " has invalid material index " + materialIndex + " for materialCount=" + materialCount);
        }

        List<GaiaVertex> vertices = primitive.getVertices();
        if (vertices == null || vertices.isEmpty()) {
            report.addIssue(path + " has no vertices.");
            return;
        }
        report.setVertexCount(report.getVertexCount() + vertices.size());
        validateVertices(report, vertices, path, worldTransform, boundingBox);

        List<GaiaSurface> surfaces = primitive.getSurfaces();
        if (surfaces == null || surfaces.isEmpty()) {
            report.addIssue(path + " has no surfaces.");
            return;
        }
        for (int surfaceIndex = 0; surfaceIndex < surfaces.size(); surfaceIndex++) {
            validateSurface(report, surfaces.get(surfaceIndex), vertices, path + "/surface[" + surfaceIndex + "]");
        }
    }

    private void validateVertices(GaiaSceneValidationReport report, List<GaiaVertex> vertices, String path, Matrix4d worldTransform, GaiaBoundingBox boundingBox) {
        int positions = 0;
        int normals = 0;
        int texcoords = 0;
        int colors = 0;
        for (int vertexIndex = 0; vertexIndex < vertices.size(); vertexIndex++) {
            GaiaVertex vertex = vertices.get(vertexIndex);
            String vertexPath = path + "/vertex[" + vertexIndex + "]";
            if (vertex == null) {
                report.setInvalidVertexCount(report.getInvalidVertexCount() + 1);
                report.addIssue(vertexPath + " is null.");
                continue;
            }

            Vector3d position = vertex.getPosition();
            if (!isFinite(position)) {
                report.setInvalidPositionCount(report.getInvalidPositionCount() + 1);
                report.addIssue(vertexPath + " has invalid position: " + position);
            } else {
                positions++;
                report.setPositionCount(report.getPositionCount() + 1);
                Vector3d transformed = worldTransform.transformPosition(new Vector3d(position));
                if (isFinite(transformed)) {
                    boundingBox.addPoint(transformed);
                } else {
                    report.setInvalidPositionCount(report.getInvalidPositionCount() + 1);
                    report.addIssue(vertexPath + " produces a non-finite transformed position: " + transformed);
                }
            }

            Vector3d normal = vertex.getNormal();
            if (normal != null) {
                normals++;
                report.setNormalCount(report.getNormalCount() + 1);
                if (!isFinite(normal)) {
                    report.setInvalidNormalCount(report.getInvalidNormalCount() + 1);
                    report.addIssue(vertexPath + " has invalid normal: " + normal);
                } else {
                    double length = normal.length();
                    if (length <= NORMAL_LENGTH_EPSILON || Math.abs(1.0 - length) > NORMAL_LENGTH_EPSILON) {
                        report.setSuspiciousNormalCount(report.getSuspiciousNormalCount() + 1);
                        report.addIssue(vertexPath + " has suspicious normal length: " + length);
                    }
                }
            }

            Vector2d texcoord = vertex.getTexcoords();
            if (texcoord != null) {
                texcoords++;
                report.setTexcoordCount(report.getTexcoordCount() + 1);
                if (!isFinite(texcoord)) {
                    report.setInvalidTexcoordCount(report.getInvalidTexcoordCount() + 1);
                    report.addIssue(vertexPath + " has invalid texcoord: " + texcoord);
                }
            }

            byte[] color = vertex.getColor();
            if (color != null) {
                colors++;
                report.setColorCount(report.getColorCount() + 1);
                if (color.length != 4) {
                    report.setInvalidColorCount(report.getInvalidColorCount() + 1);
                    report.addIssue(vertexPath + " has invalid color component count: " + color.length);
                }
            }
            if (!Float.isFinite(vertex.getBatchId())) {
                report.setInvalidBatchIdCount(report.getInvalidBatchIdCount() + 1);
                report.addIssue(vertexPath + " has invalid batchId: " + vertex.getBatchId());
            }
        }
        validateAttributeCount(report, path, "positions", positions, vertices.size());
        validateOptionalAttributeCount(report, path, "normals", normals, vertices.size());
        validateOptionalAttributeCount(report, path, "texcoords", texcoords, vertices.size());
        validateOptionalAttributeCount(report, path, "colors", colors, vertices.size());
    }

    private void validateAttributeCount(GaiaSceneValidationReport report, String path, String name, int count, int vertices) {
        if (count != vertices) {
            report.addIssue(path + " has mismatched " + name + ". " + name + "=" + count + ", vertices=" + vertices);
        }
    }

    private void validateOptionalAttributeCount(GaiaSceneValidationReport report, String path, String name, int count, int vertices) {
        if (count != 0 && count != vertices) {
            report.addIssue(path + " has partial " + name + ". " + name + "=" + count + ", vertices=" + vertices);
        }
    }

    private void validateSurface(GaiaSceneValidationReport report, GaiaSurface surface, List<GaiaVertex> vertices, String path) {
        if (surface == null) {
            report.addIssue(path + " is null.");
            return;
        }
        List<GaiaFace> faces = surface.getFaces();
        if (faces == null || faces.isEmpty()) {
            report.addIssue(path + " has no faces.");
            return;
        }
        for (int faceIndex = 0; faceIndex < faces.size(); faceIndex++) {
            validateFace(report, faces.get(faceIndex), vertices, path + "/face[" + faceIndex + "]");
        }
    }

    private void validateFace(GaiaSceneValidationReport report, GaiaFace face, List<GaiaVertex> vertices, String path) {
        if (face == null) {
            report.setInvalidFaceCount(report.getInvalidFaceCount() + 1);
            report.addIssue(path + " is null.");
            return;
        }
        int[] indices = face.getIndices();
        if (indices == null || indices.length == 0) {
            report.setInvalidFaceCount(report.getInvalidFaceCount() + 1);
            report.addIssue(path + " has no indices.");
            return;
        }

        report.setFaceCount(report.getFaceCount() + 1);
        report.setIndexCount(report.getIndexCount() + indices.length);
        if (indices.length != 3) {
            report.setNonTriangleFaceCount(report.getNonTriangleFaceCount() + 1);
            report.addIssue(path + " is not triangular. indices=" + indices.length);
        }

        boolean validVertices = true;
        for (int index : indices) {
            if (index < 0 || index >= vertices.size()) {
                validVertices = false;
                report.setInvalidIndexCount(report.getInvalidIndexCount() + 1);
                report.addIssue(path + " has out-of-range index " + index + " for vertexCount=" + vertices.size());
            } else if (vertices.get(index) == null || !isFinite(vertices.get(index).getPosition())) {
                validVertices = false;
            }
        }
        if (validVertices && indices.length == 3 && isDegenerate(vertices, indices)) {
            report.setDegenerateTriangleCount(report.getDegenerateTriangleCount() + 1);
            report.addIssue(path + " is degenerate.");
        }
    }

    private boolean isDegenerate(List<GaiaVertex> vertices, int[] indices) {
        if (indices[0] == indices[1] || indices[1] == indices[2] || indices[2] == indices[0]) {
            return true;
        }
        Vector3d p0 = vertices.get(indices[0]).getPosition();
        Vector3d p1 = vertices.get(indices[1]).getPosition();
        Vector3d p2 = vertices.get(indices[2]).getPosition();
        Vector3d edge0 = new Vector3d(p1).sub(p0);
        Vector3d edge1 = new Vector3d(p2).sub(p0);
        double maxEdgeLengthSquared = Math.max(edge0.lengthSquared(), edge1.lengthSquared());
        if (maxEdgeLengthSquared == 0.0) {
            return true;
        }
        double crossLengthSquared = edge0.cross(edge1).lengthSquared();
        return crossLengthSquared <= DEGENERATE_TRIANGLE_EPSILON * DEGENERATE_TRIANGLE_EPSILON * maxEdgeLengthSquared * maxEdgeLengthSquared;
    }

    private boolean isFinite(Matrix4d value) {
        return value != null && Double.isFinite(value.m00()) && Double.isFinite(value.m01()) && Double.isFinite(value.m02()) && Double.isFinite(value.m03()) && Double.isFinite(value.m10()) && Double.isFinite(value.m11()) && Double.isFinite(value.m12()) && Double.isFinite(value.m13()) && Double.isFinite(value.m20()) && Double.isFinite(value.m21()) && Double.isFinite(value.m22()) && Double.isFinite(value.m23()) && Double.isFinite(value.m30()) && Double.isFinite(value.m31()) && Double.isFinite(value.m32()) && Double.isFinite(value.m33());
    }

    private boolean isFinite(Vector3d value) {
        return value != null && Double.isFinite(value.x()) && Double.isFinite(value.y()) && Double.isFinite(value.z());
    }

    private boolean isFinite(Vector2d value) {
        return value != null && Double.isFinite(value.x()) && Double.isFinite(value.y());
    }
}
