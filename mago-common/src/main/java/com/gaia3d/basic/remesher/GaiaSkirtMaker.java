package com.gaia3d.basic.remesher;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaFace;
import com.gaia3d.basic.model.GaiaMesh;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaPrimitive;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.GaiaSurface;
import com.gaia3d.basic.model.GaiaVertex;

import lombok.extern.slf4j.Slf4j;

import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class GaiaSkirtMaker {

    private enum BoundarySide {
        NONE,
        MIN_X,
        MAX_X,
        MIN_Y,
        MAX_Y
    }

    public int addSkirtsToScene(
            GaiaScene scene,
            GaiaBoundingBox nodeBBox,
            double tolerance,
            double skirtDepth,
            double maxSegmentLength
    ) {
        if (scene == null || nodeBBox == null) {
            return 0;
        }

        if (tolerance <= 0.0 || skirtDepth <= 0.0 || maxSegmentLength <= 0.0) {
            return 0;
        }

        List<GaiaNode> nodes = scene.getNodes();
        if (nodes == null || nodes.isEmpty()) {
            return 0;
        }

        int totalCreatedFaces = 0;

        for (GaiaNode node : nodes) {
            totalCreatedFaces += addSkirtsToNode(
                    node,
                    nodeBBox,
                    tolerance,
                    skirtDepth,
                    maxSegmentLength
            );
        }

        log.debug("GaiaSkirtMakerV2 created skirt triangles = {}", totalCreatedFaces);

        return totalCreatedFaces;
    }

    private int addSkirtsToNode(
            GaiaNode node,
            GaiaBoundingBox nodeBBox,
            double tolerance,
            double skirtDepth,
            double maxSegmentLength
    ) {
        if (node == null) {
            return 0;
        }

        int createdFaces = 0;

        List<GaiaMesh> meshes = node.getMeshes();
        if (meshes != null) {
            for (GaiaMesh mesh : meshes) {
                createdFaces += addSkirtsToMesh(
                        mesh,
                        nodeBBox,
                        tolerance,
                        skirtDepth,
                        maxSegmentLength
                );
            }
        }

        List<GaiaNode> children = node.getChildren();
        if (children != null) {
            for (GaiaNode child : children) {
                createdFaces += addSkirtsToNode(
                        child,
                        nodeBBox,
                        tolerance,
                        skirtDepth,
                        maxSegmentLength
                );
            }
        }

        return createdFaces;
    }

    private int addSkirtsToMesh(
            GaiaMesh mesh,
            GaiaBoundingBox nodeBBox,
            double tolerance,
            double skirtDepth,
            double maxSegmentLength
    ) {
        if (mesh == null) {
            return 0;
        }

        List<GaiaPrimitive> primitives = mesh.getPrimitives();
        if (primitives == null || primitives.isEmpty()) {
            return 0;
        }

        int createdFaces = 0;

        for (GaiaPrimitive primitive : primitives) {
            createdFaces += addSkirtsToPrimitive(
                    primitive,
                    nodeBBox,
                    tolerance,
                    skirtDepth,
                    maxSegmentLength
            );
        }

        return createdFaces;
    }

    public int addSkirtsToPrimitive(
            GaiaPrimitive primitive,
            GaiaBoundingBox nodeBBox,
            double tolerance,
            double skirtDepth,
            double maxSegmentLength
    ) {
        return addSkirtsToPrimitiveByFrontierVertices(
                primitive,
                nodeBBox,
                tolerance,
                skirtDepth,
                maxSegmentLength
        );
    }

    private Vector3d calculateFaceNormal(
            GaiaFace face,
            List<GaiaVertex> vertices
    ) {
        if (face == null || face.getIndices() == null || face.getIndices().length < 3) {
            return null;
        }

        int[] indices = face.getIndices();

        int i0 = indices[0];
        int i1 = indices[1];
        int i2 = indices[2];

        if (i0 < 0 || i1 < 0 || i2 < 0 ||
                i0 >= vertices.size() ||
                i1 >= vertices.size() ||
                i2 >= vertices.size()) {
            return null;
        }

        Vector3d p0 = vertices.get(i0).getPosition();
        Vector3d p1 = vertices.get(i1).getPosition();
        Vector3d p2 = vertices.get(i2).getPosition();

        if (p0 == null || p1 == null || p2 == null) {
            return null;
        }

        Vector3d e1 = new Vector3d(p1).sub(p0);
        Vector3d e2 = new Vector3d(p2).sub(p0);
        Vector3d n = e1.cross(e2, new Vector3d());

        if (n.lengthSquared() < 1e-12) {
            return null;
        }

        return n.normalize();
    }

    private int addSkirtsToPrimitiveByFrontierVertices(
            GaiaPrimitive primitive,
            GaiaBoundingBox nodeBBox,
            double tolerance,
            double skirtDepth,
            double maxSegmentLength
    ) {
        if (primitive == null || nodeBBox == null) {
            return 0;
        }

        List<GaiaVertex> vertices = primitive.getVertices();
        List<GaiaSurface> surfaces = primitive.getSurfaces();

        if (vertices == null || vertices.size() < 2 ||
                surfaces == null || surfaces.isEmpty()) {
            return 0;
        }

        List<GaiaFace> allFaces = new ArrayList<>();
        primitive.extractGaiaAllFaces(allFaces);

        if (allFaces.isEmpty()) {
            return 0;
        }

        int[] weldedIndices = new int[vertices.size()];

        GaiaFrontierFinder finder = new GaiaFrontierFinder();

        boolean[] frontierVertices = finder.findBoundaryVertices(
                vertices,
                allFaces,
                1e-6,
                weldedIndices
        );

        if (frontierVertices == null || frontierVertices.length == 0) {
            return 0;
        }

        GaiaSurface skirtSurface = new GaiaSurface();

        Map<Integer, Integer> originalToSkirt = new HashMap<>();
        Set<Long> createdEdges = new HashSet<>();

        int frontierEdgeCandidates = 0;
        int bboxEdgeCandidates = 0;
        int createdFaces = 0;

        // Copia defensiva. No queremos que, si se añade una surface nueva,
        // el recorrido la procese también.
        List<GaiaSurface> originalSurfaces = new ArrayList<>(surfaces);

        for (GaiaSurface surface : originalSurfaces) {
            if (surface == null || surface.getFaces() == null) {
                continue;
            }

            List<GaiaFace> faces = surface.getFaces();

            for (GaiaFace face : faces) {
                if (face == null || face.getIndices() == null) {
                    continue;
                }

                int[] indices = face.getIndices();

                if (indices.length < 3) {
                    continue;
                }

                Vector3d faceNormal = null;
                boolean skipFaceByNormal = false;
                for (int i = 0; i < indices.length; i++) {
                    int v0 = indices[i];
                    int v1 = indices[(i + 1) % indices.length];

                    if (!isValidVertexIndex(v0, vertices.size()) ||
                            !isValidVertexIndex(v1, vertices.size()) ||
                            v0 == v1) {
                        continue;
                    }

                    if (v0 >= frontierVertices.length || v1 >= frontierVertices.length) {
                        continue;
                    }

                    if (!frontierVertices[v0] || !frontierVertices[v1]) {
                        continue;
                    }

                    // filter by face normal.
                    if(faceNormal == null) {
                        faceNormal = calculateFaceNormal(face, vertices);
                    }
                    if (faceNormal != null && faceNormal.z < 0.0) {
                        skipFaceByNormal = true;
                        break;
                    }

                    frontierEdgeCandidates++;

                    int added = tryAddSkirtForFrontierEdge(
                            vertices,
                            skirtSurface,
                            originalToSkirt,
                            createdEdges,
                            v0,
                            v1,
                            nodeBBox,
                            tolerance,
                            skirtDepth,
                            maxSegmentLength
                    );

                    if (added > 0) {
                        bboxEdgeCandidates++;
                        createdFaces += added;
                    }
                }

                if (skipFaceByNormal) {
                    continue;
                }
            }
        }

        if (createdFaces > 0) {
            primitive.getSurfaces().add(skirtSurface);
        }

        log.debug(
                "GaiaSkirtMakerV2 primitive: frontierEdgeCandidates={}, bboxEdgeCandidates={}, createdFaces={}, tolerance={}, skirtDepth={}, maxSegmentLength={}",
                frontierEdgeCandidates,
                bboxEdgeCandidates,
                createdFaces,
                tolerance,
                skirtDepth,
                maxSegmentLength
        );

        return createdFaces;
    }

    private int tryAddSkirtForFrontierEdge(
            List<GaiaVertex> vertices,
            GaiaSurface skirtSurface,
            Map<Integer, Integer> originalToSkirt,
            Set<Long> createdEdges,
            int v0,
            int v1,
            GaiaBoundingBox nodeBBox,
            double tolerance,
            double skirtDepth,
            double maxSegmentLength
    ) {
        if (vertices == null || skirtSurface == null ||
                originalToSkirt == null || createdEdges == null ||
                nodeBBox == null) {
            return 0;
        }

        if (!isValidVertexIndex(v0, vertices.size()) ||
                !isValidVertexIndex(v1, vertices.size()) ||
                v0 == v1) {
            return 0;
        }

        GaiaVertex vertex0 = vertices.get(v0);
        GaiaVertex vertex1 = vertices.get(v1);

        if (vertex0 == null || vertex1 == null ||
                vertex0.getPosition() == null ||
                vertex1.getPosition() == null) {
            return 0;
        }

        if (!areVerticesCloseEnough(vertex0, vertex1, maxSegmentLength)) {
            return 0;
        }

        BoundarySide side = getBoundarySideOfEdge(
                vertex0,
                vertex1,
                nodeBBox,
                tolerance
        );

        if (side == BoundarySide.NONE) {
            return 0;
        }

        long edgeKey = makeUndirectedEdgeKey(v0, v1);

        if (!createdEdges.add(edgeKey)) {
            return 0;
        }

        int skirtVertex0 = getOrCreateSkirtVertex(
                vertices,
                originalToSkirt,
                v0,
                skirtDepth
        );

        int skirtVertex1 = getOrCreateSkirtVertex(
                vertices,
                originalToSkirt,
                v1,
                skirtDepth
        );

        if (skirtVertex0 < 0 || skirtVertex1 < 0) {
            return 0;
        }

        // Winding fijo usando el orden v0 -> v1 de la face original:
        // tri 1 = (v1, v0, skirtVertex1)
        // tri 2 = (v0, skirtVertex1, skirtVertex0)
        addTriangleFace(skirtSurface, v1, v0, skirtVertex0);
        addTriangleFace(skirtSurface, v1, skirtVertex0, skirtVertex1);

        return 2;
    }

    private BoundarySide getBoundarySideOfEdge(
            GaiaVertex v0,
            GaiaVertex v1,
            GaiaBoundingBox nodeBBox,
            double tolerance
    ) {
        if (v0 == null || v1 == null ||
                v0.getPosition() == null ||
                v1.getPosition() == null ||
                nodeBBox == null) {
            return BoundarySide.NONE;
        }

        Vector3d p0 = v0.getPosition();
        Vector3d p1 = v1.getPosition();

        // Para que una arista pertenezca a un lado,
        // ambos extremos deben estar cerca de ese mismo lado.
        double minXDist = Math.max(
                Math.abs(p0.x - nodeBBox.getMinX()),
                Math.abs(p1.x - nodeBBox.getMinX())
        );

        double maxXDist = Math.max(
                Math.abs(p0.x - nodeBBox.getMaxX()),
                Math.abs(p1.x - nodeBBox.getMaxX())
        );

        double minYDist = Math.max(
                Math.abs(p0.y - nodeBBox.getMinY()),
                Math.abs(p1.y - nodeBBox.getMinY())
        );

        double maxYDist = Math.max(
                Math.abs(p0.y - nodeBBox.getMaxY()),
                Math.abs(p1.y - nodeBBox.getMaxY())
        );

        double best = tolerance;
        BoundarySide side = BoundarySide.NONE;

        if (minXDist <= best) {
            best = minXDist;
            side = BoundarySide.MIN_X;
        }

        if (maxXDist <= best) {
            best = maxXDist;
            side = BoundarySide.MAX_X;
        }

        if (minYDist <= best) {
            best = minYDist;
            side = BoundarySide.MIN_Y;
        }

        if (maxYDist <= best) {
            side = BoundarySide.MAX_Y;
        }

        return side;
    }

    private int getOrCreateSkirtVertex(
            List<GaiaVertex> vertices,
            Map<Integer, Integer> originalToSkirt,
            int originalIndex,
            double skirtDepth
    ) {
        if (vertices == null || originalToSkirt == null) {
            return -1;
        }

        Integer existing = originalToSkirt.get(originalIndex);

        if (existing != null) {
            return existing;
        }

        if (!isValidVertexIndex(originalIndex, vertices.size())) {
            return -1;
        }

        GaiaVertex original = vertices.get(originalIndex);

        if (original == null || original.getPosition() == null) {
            return -1;
        }

        GaiaVertex skirtVertex = original.clone();
        skirtVertex.getPosition().z -= skirtDepth;

        int skirtIndex = vertices.size();
        vertices.add(skirtVertex);

        originalToSkirt.put(originalIndex, skirtIndex);

        return skirtIndex;
    }

    private boolean isValidVertexIndex(
            int index,
            int verticesCount
    ) {
        return index >= 0 && index < verticesCount;
    }

    private long makeUndirectedEdgeKey(
            int a,
            int b
    ) {
        int min = Math.min(a, b);
        int max = Math.max(a, b);

        return (((long) min) << 32) ^ (max & 0xffffffffL);
    }

    private void addTriangleFace(
            GaiaSurface surface,
            int i0,
            int i1,
            int i2
    ) {
        if (surface == null) {
            return;
        }

        GaiaFace face = new GaiaFace();
        face.setIndices(new int[] { i0, i1, i2 });

        surface.getFaces().add(face);
    }

    private boolean areVerticesCloseEnough(
            GaiaVertex a,
            GaiaVertex b,
            double maxDistance
    ) {
        if (a == null || b == null ||
                a.getPosition() == null ||
                b.getPosition() == null) {
            return false;
        }

        return a.getPosition().distance(b.getPosition()) <= maxDistance;
    }
}