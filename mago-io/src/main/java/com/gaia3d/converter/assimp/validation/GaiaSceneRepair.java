package com.gaia3d.converter.assimp.validation;

import com.gaia3d.basic.model.*;
import com.gaia3d.basic.types.TextureType;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Repairs GaiaScene data based on issues detected by GaiaSceneValidator.
 * <p>
 * Textures  : tries to locate missing textures near the source file; drops them if not found.
 * Normals   : regenerates per-vertex normals when invalid or suspiciously non-unit normals are found.
 * Geometry  : removes faces that reference NaN/Inf positions; resets non-finite batch IDs to 0.
 * Faces     : removes degenerate triangles (zero area or repeated indices).
 */
@Slf4j
public class GaiaSceneRepair {

    public void repair(GaiaSceneValidationReport report, List<GaiaScene> scenes) {
        if (scenes == null || scenes.isEmpty()) {return;}

        if (report.getMissingExternalResourceCount() > 0
                || report.getUnreadableExternalResourceCount() > 0
                || report.getInvalidTexturePathCount() > 0) {
            Path searchDir = sourceParent(report.getSourceFile());
            log.info("[REPAIR] Repairing textures. missingCount={}, searchDir={}",
                    report.getMissingExternalResourceCount(), searchDir);
            for (GaiaScene scene : scenes) {
                if (scene != null) {repairTextures(scene, searchDir);}
            }
        }

        if (report.getInvalidNormalCount() > 0 || report.getSuspiciousNormalCount() > 0) {
            log.info("[REPAIR] Repairing normals. invalidCount={}, suspiciousCount={}",
                    report.getInvalidNormalCount(), report.getSuspiciousNormalCount());
            for (GaiaScene scene : scenes) {
                if (scene != null) {repairNormalsInNodes(scene.getNodes());}
            }
        }

        if (report.getInvalidPositionCount() > 0
                || report.getInvalidVertexCount() > 0
                || report.getInvalidBatchIdCount() > 0) {
            log.info("[REPAIR] Repairing geometry. invalidPositions={}, invalidVertices={}, invalidBatchIds={}",
                    report.getInvalidPositionCount(), report.getInvalidVertexCount(), report.getInvalidBatchIdCount());
            for (GaiaScene scene : scenes) {
                if (scene != null) {repairGeometryInNodes(scene.getNodes());}
            }
        }

        if (report.getDegenerateTriangleCount() > 0 || report.getInvalidFaceCount() > 0) {
            log.info("[REPAIR] Removing degenerate/invalid faces. degenerateCount={}, invalidFaceCount={}",
                    report.getDegenerateTriangleCount(), report.getInvalidFaceCount());
            for (GaiaScene scene : scenes) {
                if (scene != null) {repairFacesInNodes(scene.getNodes());}
            }
        }

        pruneEmptyScenes(scenes);
    }

    /* --- Pruning: remove empty primitives → meshes → nodes → scenes --- */

    private void pruneEmptyScenes(List<GaiaScene> scenes) {
        for (GaiaScene scene : scenes) {
            if (scene != null) {pruneEmptyNodes(scene.getNodes());}
        }
        int before = scenes.size();
        scenes.removeIf(s -> s == null || isEmptyScene(s));
        int removed = before - scenes.size();
        if (removed > 0) {
            log.warn("[REPAIR] Pruned {} empty scene(s)", removed);
        }
    }

    private void pruneEmptyNodes(List<GaiaNode> nodes) {
        if (nodes == null) {return;}
        for (GaiaNode node : nodes) {
            if (node == null) {continue;}
            pruneEmptyNodes(node.getChildren());
            pruneEmptyMeshes(node.getMeshes());
        }
        int before = nodes.size();
        nodes.removeIf(n -> n == null || isEmptyNode(n));
        int removed = before - nodes.size();
        if (removed > 0) {
            log.warn("[REPAIR] Pruned {} empty node(s)", removed);
        }
    }

    private void pruneEmptyMeshes(List<GaiaMesh> meshes) {
        if (meshes == null) {return;}
        for (GaiaMesh mesh : meshes) {
            if (mesh == null || mesh.getPrimitives() == null) {continue;}
            pruneEmptyPrimitives(mesh.getPrimitives());
        }
        meshes.removeIf(m -> m == null || isEmptyMesh(m));
    }

    private void pruneEmptyPrimitives(List<GaiaPrimitive> primitives) {
        if (primitives == null) {return;}
        for (GaiaPrimitive primitive : primitives) {
            if (primitive == null || primitive.getSurfaces() == null) {continue;}
            primitive.getSurfaces().removeIf(s -> s == null || s.getFaces() == null || s.getFaces().isEmpty());
        }
        primitives.removeIf(p -> p == null || isEmptyPrimitive(p));
    }

    private boolean isEmptyScene(GaiaScene scene) {
        return scene.getNodes() == null || scene.getNodes().isEmpty();
    }

    private boolean isEmptyNode(GaiaNode node) {
        boolean noMeshes = node.getMeshes() == null || node.getMeshes().isEmpty();
        boolean noChildren = node.getChildren() == null || node.getChildren().isEmpty();
        return noMeshes && noChildren;
    }

    private boolean isEmptyMesh(GaiaMesh mesh) {
        return mesh.getPrimitives() == null || mesh.getPrimitives().isEmpty();
    }

    private boolean isEmptyPrimitive(GaiaPrimitive primitive) {
        return primitive.getVertices() == null || primitive.getVertices().isEmpty()
                || primitive.getSurfaces() == null || primitive.getSurfaces().isEmpty();
    }

    /* --- Texture repair */
    private void repairTextures(GaiaScene scene, Path searchDir) {
        List<GaiaMaterial> materials = scene.getMaterials();
        if (materials == null) {return;}
        for (GaiaMaterial material : materials) {
            if (material == null || material.getTextures() == null) {continue;}
            for (Map.Entry<TextureType, List<GaiaTexture>> entry : material.getTextures().entrySet()) {
                List<GaiaTexture> repaired = repairTextureList(entry.getValue(), searchDir);
                entry.setValue(repaired);
            }
        }
    }

    private List<GaiaTexture> repairTextureList(List<GaiaTexture> textures, Path searchDir) {
        if (textures == null) {return new ArrayList<>();}
        List<GaiaTexture> result = new ArrayList<>();
        for (GaiaTexture texture : textures) {
            if (texture == null) {continue;}
            if (resolve(texture, searchDir)) {
                result.add(texture);
            } else {
                log.warn("[REPAIR] Texture not found anywhere, dropping: {}", texture.getPath());
            }
        }
        return result;
    }

    private boolean resolve(GaiaTexture texture, Path searchDir) {
        // 1. Check the path as-is (absolute or relative to parentPath)
        Path resolved = resolveAsIs(texture);
        if (resolved != null && Files.isRegularFile(resolved)) {return true;}

        // 2. Search by filename in the source file's directory
        if (searchDir == null) {return false;}

        String filename;
        try {
            filename = Path.of(texture.getPath()).getFileName().toString();
        } catch (InvalidPathException e) {
            return false;
        }

        // exact match
        Path candidate = searchDir.resolve(filename);
        if (Files.isRegularFile(candidate)) {
            log.info("[REPAIR] Texture found by name: {} → {}", texture.getPath(), candidate);
            texture.setPath(candidate.toString());
            return true;
        }

        // case-insensitive match
        Path found = findCaseInsensitive(searchDir, filename);
        if (found != null) {
            log.info("[REPAIR] Texture found case-insensitively: {} → {}", texture.getPath(), found);
            texture.setPath(found.toString());
            return true;
        }

        return false;
    }

    private Path resolveAsIs(GaiaTexture texture) {
        String texPath = texture.getPath();
        if (texPath == null || texPath.isBlank()) {return null;}
        try {
            Path p = Path.of(texPath);
            if (p.isAbsolute()) {return p.normalize();}
            String parent = texture.getParentPath();
            if (parent != null && !parent.isBlank()) {
                return Path.of(parent).resolve(p).toAbsolutePath().normalize();
            }
        } catch (InvalidPathException e) { /* fall through */ }
        return null;
    }

    private Path findCaseInsensitive(Path dir, String filename) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(p -> p.getFileName().toString().equalsIgnoreCase(filename)).findFirst().orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    /* Normal repair */
    private void repairNormalsInNodes(List<GaiaNode> nodes) {
        if (nodes == null) {return;}
        for (GaiaNode node : nodes) {
            if (node == null) {continue;}
            if (node.getMeshes() != null) {
                for (GaiaMesh mesh : node.getMeshes()) {
                    if (mesh == null || mesh.getPrimitives() == null) {continue;}
                    for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                        if (primitive != null) {repairNormalsInPrimitive(primitive);}
                    }
                }
            }
            repairNormalsInNodes(node.getChildren());
        }
    }

    private void repairNormalsInPrimitive(GaiaPrimitive primitive) {
        if (primitive.getVertices() == null) {return;}
        boolean needsRepair = primitive.getVertices().stream().filter(Objects::nonNull).anyMatch(v -> !isValidNormal(v.getNormal()));
        if (needsRepair) {
            log.info("[REPAIR] Regenerating normals for primitive with {} vertices", primitive.getVertices().size());
            primitive.calculateVertexNormals();
        }
    }

    private boolean isValidNormal(Vector3d normal) {
        if (normal == null || !Double.isFinite(normal.x()) || !Double.isFinite(normal.y()) || !Double.isFinite(normal.z())) {
            return false;
        }
        double length = normal.length();
        return length > GaiaSceneValidator.NORMAL_LENGTH_EPSILON && Math.abs(1.0 - length) <= GaiaSceneValidator.NORMAL_LENGTH_EPSILON;
    }

    /* --- Geometry repair (invalid positions, invalid batch IDs) --- */

    private void repairGeometryInNodes(List<GaiaNode> nodes) {
        if (nodes == null) {return;}
        for (GaiaNode node : nodes) {
            if (node == null) {continue;}
            if (node.getMeshes() != null) {
                for (GaiaMesh mesh : node.getMeshes()) {
                    if (mesh == null || mesh.getPrimitives() == null) {continue;}
                    for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                        if (primitive != null) {repairGeometryInPrimitive(primitive);}
                    }
                }
            }
            repairGeometryInNodes(node.getChildren());
        }
    }

    private void repairGeometryInPrimitive(GaiaPrimitive primitive) {
        List<GaiaVertex> vertices = primitive.getVertices();
        if (vertices == null) {return;}

        Set<Integer> invalidIndices = new HashSet<>();
        for (int i = 0; i < vertices.size(); i++) {
            GaiaVertex vertex = vertices.get(i);
            if (vertex == null) {
                invalidIndices.add(i);
                continue;
            }
            // Reset non-finite batch IDs
            if (!Float.isFinite(vertex.getBatchId())) {
                log.debug("[REPAIR] Resetting invalid batchId at vertex[{}]: {}", i, vertex.getBatchId());
                vertex.setBatchId(0.0f);
            }
            // Collect vertices with non-finite positions — cannot fix, will remove referencing faces
            Vector3d pos = vertex.getPosition();
            if (pos == null || !Double.isFinite(pos.x()) || !Double.isFinite(pos.y()) || !Double.isFinite(pos.z())) {
                log.debug("[REPAIR] Invalid position at vertex[{}]: {}", i, pos);
                invalidIndices.add(i);
            }
        }

        if (!invalidIndices.isEmpty()) {
            log.warn("[REPAIR] Removing faces referencing {} invalid-position vertices", invalidIndices.size());
            removeFacesReferencingIndices(primitive, invalidIndices);
        }
    }

    private void removeFacesReferencingIndices(GaiaPrimitive primitive, Set<Integer> invalidIndices) {
        if (primitive.getSurfaces() == null) {return;}
        for (GaiaSurface surface : primitive.getSurfaces()) {
            if (surface == null || surface.getFaces() == null) {continue;}
            surface.getFaces().removeIf(face -> {
                if (face == null || face.getIndices() == null) {return true;}
                for (int idx : face.getIndices()) {
                    if (invalidIndices.contains(idx)) {return true;}
                }
                return false;
            });
        }
        primitive.getSurfaces().removeIf(s -> s == null || s.getFaces() == null || s.getFaces().isEmpty());
    }

    /* --- Degenerate face repair --- */

    private void repairFacesInNodes(List<GaiaNode> nodes) {
        if (nodes == null) {return;}
        for (GaiaNode node : nodes) {
            if (node == null) {continue;}
            if (node.getMeshes() != null) {
                for (GaiaMesh mesh : node.getMeshes()) {
                    if (mesh == null || mesh.getPrimitives() == null) {continue;}
                    for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                        if (primitive != null) {repairFacesInPrimitive(primitive);}
                    }
                }
            }
            repairFacesInNodes(node.getChildren());
        }
    }

    private void repairFacesInPrimitive(GaiaPrimitive primitive) {
        List<GaiaVertex> vertices = primitive.getVertices();
        if (vertices == null || primitive.getSurfaces() == null) {return;}

        int removed = 0;
        for (GaiaSurface surface : primitive.getSurfaces()) {
            if (surface == null || surface.getFaces() == null) {continue;}
            int before = surface.getFaces().size();
            surface.getFaces().removeIf(face -> isInvalidOrDegenerate(face, vertices));
            removed += before - surface.getFaces().size();
        }
        primitive.getSurfaces().removeIf(s -> s == null || s.getFaces() == null || s.getFaces().isEmpty());

        if (removed > 0) {
            log.warn("[REPAIR] Removed {} degenerate/invalid faces from primitive", removed);
        }
    }

    private boolean isInvalidOrDegenerate(GaiaFace face, List<GaiaVertex> vertices) {
        if (face == null || face.getIndices() == null || face.getIndices().length == 0) {return true;}
        int[] idx = face.getIndices();
        if (idx.length != 3) {return false;} // non-triangle: leave as-is (Assimp should have triangulated)

        // repeated indices
        if (idx[0] == idx[1] || idx[1] == idx[2] || idx[2] == idx[0]) {return true;}

        // out-of-range indices
        for (int i : idx) {
            if (i < 0 || i >= vertices.size()) {return true;}
        }

        // zero-area check via cross product
        GaiaVertex v0 = vertices.get(idx[0]);
        GaiaVertex v1 = vertices.get(idx[1]);
        GaiaVertex v2 = vertices.get(idx[2]);
        if (v0 == null || v1 == null || v2 == null) {return true;}

        Vector3d p0 = v0.getPosition();
        Vector3d p1 = v1.getPosition();
        Vector3d p2 = v2.getPosition();
        if (!isFiniteVec(p0) || !isFiniteVec(p1) || !isFiniteVec(p2)) {return true;}

        Vector3d edge0 = new Vector3d(p1).sub(p0);
        Vector3d edge1 = new Vector3d(p2).sub(p0);
        double maxEdgeLenSq = Math.max(edge0.lengthSquared(), edge1.lengthSquared());
        if (maxEdgeLenSq == 0.0) {return true;}
        double crossLenSq = edge0.cross(edge1).lengthSquared();
        double eps = GaiaSceneValidator.DEGENERATE_TRIANGLE_EPSILON;
        return crossLenSq <= eps * eps * maxEdgeLenSq * maxEdgeLenSq;
    }

    private boolean isFiniteVec(Vector3d v) {
        return v != null && Double.isFinite(v.x()) && Double.isFinite(v.y()) && Double.isFinite(v.z());
    }

    private Path sourceParent(File sourceFile) {
        return sourceFile == null ? null : sourceFile.toPath().toAbsolutePath().getParent();
    }
}
