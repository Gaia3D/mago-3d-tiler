package com.gaia3d.basic.remesher;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaFace;
import com.gaia3d.basic.model.GaiaMesh;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaPrimitive;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.GaiaVertex;

import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class GaiaFrontierExpander {

    public int expandFrontiersToScene(
            GaiaScene scene,
            GaiaBoundingBox nodeBBox,
            double tolerance,
            double expandDistance
    ) {
        if (scene == null || nodeBBox == null) {
            return 0;
        }

        if (tolerance <= 0.0 || expandDistance <= 0.0) {
            return 0;
        }

        List<GaiaNode> nodes = scene.getNodes();
        if (nodes == null || nodes.isEmpty()) {
            return 0;
        }

        int movedVerticesCount = 0;

        for (GaiaNode node : nodes) {
            movedVerticesCount += expandFrontiersToNode(
                    node,
                    nodeBBox,
                    tolerance,
                    expandDistance
            );
        }

        log.debug("[GaiaFrontierExpander] moved frontier vertices = {}", movedVerticesCount);

        return movedVerticesCount;
    }

    private int expandFrontiersToNode(
            GaiaNode node,
            GaiaBoundingBox nodeBBox,
            double tolerance,
            double expandDistance
    ) {
        if (node == null) {
            return 0;
        }

        int movedVerticesCount = 0;

        List<GaiaMesh> meshes = node.getMeshes();
        if (meshes != null && !meshes.isEmpty()) {
            for (GaiaMesh mesh : meshes) {
                movedVerticesCount += expandFrontiersToMesh(
                        mesh,
                        nodeBBox,
                        tolerance,
                        expandDistance
                );
            }
        }

        List<GaiaNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (GaiaNode child : children) {
                movedVerticesCount += expandFrontiersToNode(
                        child,
                        nodeBBox,
                        tolerance,
                        expandDistance
                );
            }
        }

        return movedVerticesCount;
    }

    private int expandFrontiersToMesh(
            GaiaMesh mesh,
            GaiaBoundingBox nodeBBox,
            double tolerance,
            double expandDistance
    ) {
        if (mesh == null) {
            return 0;
        }

        List<GaiaPrimitive> primitives = mesh.getPrimitives();
        if (primitives == null || primitives.isEmpty()) {
            return 0;
        }

        int movedVerticesCount = 0;

        for (GaiaPrimitive primitive : primitives) {
            movedVerticesCount += expandFrontiersToPrimitive(
                    primitive,
                    nodeBBox,
                    tolerance,
                    expandDistance
            );
        }

        return movedVerticesCount;
    }

    private int expandFrontiersToPrimitive(
            GaiaPrimitive primitive,
            GaiaBoundingBox nodeBBox,
            double tolerance,
            double expandDistance
    ) {
        if (primitive == null || nodeBBox == null) {
            return 0;
        }

        List<GaiaVertex> vertices = primitive.getVertices();
        if (vertices == null || vertices.isEmpty()) {
            return 0;
        }

        List<GaiaFace> faces = new ArrayList<>();
        primitive.extractGaiaAllFaces(faces);

        if (faces.isEmpty()) {
            return 0;
        }

        int[] weldedIndices = new int[vertices.size()];

        GaiaFrontierFinder frontierFinder = new GaiaFrontierFinder();
        boolean[] frontierVertices = frontierFinder.findBoundaryVertices(
                vertices,
                faces,
                1e-6,
                weldedIndices
        );

        if (frontierVertices == null || frontierVertices.length < vertices.size()) {
            return 0;
        }

        Map<Integer, List<GaiaFace>> vertexToFaces = buildVertexToFaces(faces);

        int movedVerticesCount = 0;

        for (int i = 0; i < vertices.size(); i++) {
            if (!frontierVertices[i]) {
                continue;
            }

            GaiaVertex vertex = vertices.get(i);
            if (vertex == null || vertex.getPosition() == null) {
                continue;
            }

            Vector3d position = vertex.getPosition();

            // Direction outward from the nodeBBox.
            Vector3d bboxDir = getExpandDirectionXYZ(
                    position,
                    nodeBBox,
                    tolerance
            );

            if (bboxDir == null || bboxDir.lengthSquared() <= 1e-12) {
                continue;
            }

            // Average normal of faces that use this vertex.
            Vector3d averageNormal = calculateAverageFaceNormalForVertex(
                    i,
                    vertexToFaces,
                    vertices
            );

            Vector3d expandDir = null;

            if (averageNormal != null && averageNormal.lengthSquared() > 1e-12) {
                // Move only inside the face plane.
                expandDir = projectDirectionOnPlane(bboxDir, averageNormal);
            }

            // Fallback: if projection fails, use bboxDir.
            // This can happen when bboxDir is almost equal to the face normal.
            if (expandDir == null || expandDir.lengthSquared() <= 1e-12) {
                expandDir = new Vector3d(bboxDir);

                if (expandDir.lengthSquared() > 1e-12) {
                    expandDir.normalize();
                }
            }

            position.x += expandDir.x * expandDistance;
            position.y += expandDir.y * expandDistance;
            position.z += expandDir.z * expandDistance;

            movedVerticesCount++;
        }

        return movedVerticesCount;
    }

    private Vector3d calculateAverageFaceNormalForVertex(
            int vertexIndex,
            Map<Integer, List<GaiaFace>> vertexToFaces,
            List<GaiaVertex> vertices
    ) {
        List<GaiaFace> faces = vertexToFaces.get(vertexIndex);
        if (faces == null || faces.isEmpty()) {
            return null;
        }

        Vector3d normalSum = new Vector3d();

        for (GaiaFace face : faces) {
            Vector3d normal = calculateFaceNormal(face, vertices);
            if (normal == null || normal.lengthSquared() <= 1e-12) {
                continue;
            }

            double area = calculateFaceArea(face, vertices);
            if (area <= 1e-12) {
                continue;
            }

            normalSum.add(normal.mul(area));
        }

        if (normalSum.lengthSquared() <= 1e-12) {
            return null;
        }

        normalSum.normalize();
        return normalSum;
    }

    private Vector3d getExpandDirectionXYZ(
            Vector3d position,
            GaiaBoundingBox nodeBBox,
            double tolerance
    ) {
        if (position == null || nodeBBox == null) {
            return null;
        }

        Vector3d direction = new Vector3d();

        if (Math.abs(position.x - nodeBBox.getMinX()) <= tolerance) {
            direction.x -= 1.0;
        }

        if (Math.abs(position.x - nodeBBox.getMaxX()) <= tolerance) {
            direction.x += 1.0;
        }

        if (Math.abs(position.y - nodeBBox.getMinY()) <= tolerance) {
            direction.y -= 1.0;
        }

        if (Math.abs(position.y - nodeBBox.getMaxY()) <= tolerance) {
            direction.y += 1.0;
        }

        if (Math.abs(position.z - nodeBBox.getMinZ()) <= tolerance) {
            direction.z -= 1.0;
        }

        if (Math.abs(position.z - nodeBBox.getMaxZ()) <= tolerance) {
            direction.z += 1.0;
        }

        if (direction.lengthSquared() <= 1e-12) {
            return null;
        }

        return direction;
    }

    private double calculateFaceArea(
            GaiaFace face,
            List<GaiaVertex> vertices
    ) {
        if (face == null || face.getIndices() == null || face.getIndices().length < 3) {
            return 0.0;
        }

        int[] indices = face.getIndices();

        int i0 = indices[0];
        int i1 = indices[1];
        int i2 = indices[2];

        if (i0 < 0 || i0 >= vertices.size()
                || i1 < 0 || i1 >= vertices.size()
                || i2 < 0 || i2 >= vertices.size()) {
            return 0.0;
        }

        Vector3d p0 = vertices.get(i0).getPosition();
        Vector3d p1 = vertices.get(i1).getPosition();
        Vector3d p2 = vertices.get(i2).getPosition();

        if (p0 == null || p1 == null || p2 == null) {
            return 0.0;
        }

        Vector3d e1 = new Vector3d(p1).sub(p0);
        Vector3d e2 = new Vector3d(p2).sub(p0);

        return e1.cross(e2, new Vector3d()).length() * 0.5;
    }

    private Vector3d calculateBoundaryNormalForVertex(
            int vertexIndex,
            Vector3d bboxDir,
            Map<Integer, List<GaiaFace>> vertexToFaces,
            List<GaiaVertex> vertices
    ) {
        if (bboxDir == null || bboxDir.lengthSquared() <= 1e-12) {
            return null;
        }

        List<GaiaFace> faces = vertexToFaces.get(vertexIndex);
        if (faces == null || faces.isEmpty()) {
            return null;
        }

        Vector3d bboxDirNorm = new Vector3d(bboxDir);
        bboxDirNorm.normalize();

        Vector3d normalSum = new Vector3d();

        for (GaiaFace face : faces) {
            Vector3d faceNormal = calculateFaceNormal(face, vertices);
            if (faceNormal == null || faceNormal.lengthSquared() <= 1e-12) {
                continue;
            }

            // Orient the face normal toward the bbox expansion direction.
            if (faceNormal.dot(bboxDirNorm) < 0.0) {
                faceNormal.negate();
            }

            double alignment = faceNormal.dot(bboxDirNorm);

            // Ignore faces whose normal does not contribute toward the touched boundary.
            // You can lower this to 0.05 if it rejects too much.
            if (alignment < 0.15) {
                continue;
            }

            double area = calculateFaceArea(face, vertices);
            if (area <= 1e-12) {
                continue;
            }

            normalSum.add(faceNormal.mul(area));
        }

        if (normalSum.lengthSquared() <= 1e-12) {
            return null;
        }

        normalSum.normalize();
        return normalSum;
    }

    private Vector3d projectDirectionOnPlane(
            Vector3d direction,
            Vector3d planeNormal
    ) {
        if (direction == null || planeNormal == null) {
            return null;
        }

        if (direction.lengthSquared() <= 1e-12 || planeNormal.lengthSquared() <= 1e-12) {
            return null;
        }

        Vector3d n = new Vector3d(planeNormal).normalize();

        // projected = direction - normal * dot(direction, normal)
        Vector3d projected = new Vector3d(direction).sub(
                new Vector3d(n).mul(direction.dot(n))
        );

        if (projected.lengthSquared() <= 1e-12) {
            return null;
        }

        projected.normalize();
        return projected;
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

        if (i0 < 0 || i0 >= vertices.size()
                || i1 < 0 || i1 >= vertices.size()
                || i2 < 0 || i2 >= vertices.size()) {
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

        Vector3d normal = e1.cross(e2, new Vector3d());

        if (normal.lengthSquared() <= 1e-12) {
            return null;
        }

        normal.normalize();
        return normal;
    }

    private Map<Integer, List<GaiaFace>> buildVertexToFaces(List<GaiaFace> faces) {
        Map<Integer, List<GaiaFace>> result = new HashMap<>();

        for (GaiaFace face : faces) {
            if (face == null || face.getIndices() == null) {
                continue;
            }

            int[] indices = face.getIndices();

            for (int index : indices) {
                result.computeIfAbsent(index, k -> new ArrayList<>()).add(face);
            }
        }

        return result;
    }
}