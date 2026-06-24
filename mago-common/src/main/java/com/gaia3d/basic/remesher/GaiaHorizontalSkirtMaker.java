package com.gaia3d.basic.remesher;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.*;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GaiaHorizontalSkirtMaker {

    private static final Logger log = LoggerFactory.getLogger(GaiaHorizontalSkirtMaker.class);

    public int addHorizontalSkirtsToScene(
            GaiaScene scene,
            GaiaBoundingBox nodeBBox,
            double tolerance,
            double pushDistance
    ) {
        if (scene == null || nodeBBox == null) {
            return 0;
        }

        if (tolerance <= 0.0 || pushDistance <= 0.0) {
            return 0;
        }

        List<GaiaNode> nodes = scene.getNodes();
        if (nodes == null || nodes.isEmpty()) {
            return 0;
        }

        int movedVerticesCount = 0;

        for (GaiaNode node : nodes) {
            movedVerticesCount += addHorizontalSkirtsToNode(
                    node,
                    nodeBBox,
                    tolerance,
                    pushDistance
            );
        }

        log.debug("GaiaHorizontalSkirtMaker moved frontier vertices = {}", movedVerticesCount);

        return movedVerticesCount;
    }

    private int addHorizontalSkirtsToNode(
            GaiaNode node,
            GaiaBoundingBox nodeBBox,
            double tolerance,
            double pushDistance
    ) {
        if (node == null) {
            return 0;
        }

        int movedVerticesCount = 0;

        List<GaiaMesh> meshes = node.getMeshes();
        if (meshes != null && !meshes.isEmpty()) {
            for (GaiaMesh mesh : meshes) {
                movedVerticesCount += addHorizontalSkirtsToMesh(
                        mesh,
                        nodeBBox,
                        tolerance,
                        pushDistance
                );
            }
        }

        List<GaiaNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (GaiaNode child : children) {
                movedVerticesCount += addHorizontalSkirtsToNode(
                        child,
                        nodeBBox,
                        tolerance,
                        pushDistance
                );
            }
        }

        return movedVerticesCount;
    }

    private int addHorizontalSkirtsToMesh(
            GaiaMesh mesh,
            GaiaBoundingBox nodeBBox,
            double tolerance,
            double pushDistance
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
            movedVerticesCount += addHorizontalSkirtsToPrimitive(
                    primitive,
                    nodeBBox,
                    tolerance,
                    pushDistance
            );
        }

        return movedVerticesCount;
    }

    private int addHorizontalSkirtsToPrimitive(
            GaiaPrimitive primitive,
            GaiaBoundingBox nodeBBox,
            double tolerance,
            double pushDistance
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
            Vector3d outwardDir = getOutwardDirectionXY(
                    position,
                    nodeBBox,
                    tolerance
            );

            if (outwardDir == null) {
                continue;
            }

            position.x += outwardDir.x * pushDistance;
            position.y += outwardDir.y * pushDistance;

            movedVerticesCount++;
        }

        return movedVerticesCount;
    }

    private Vector3d getOutwardDirectionXY(
            Vector3d position,
            GaiaBoundingBox nodeBBox,
            double tolerance
    ) {
        if (position == null || nodeBBox == null) {
            return null;
        }

        Vector3d dir = new Vector3d();

        if (Math.abs(position.x - nodeBBox.getMinX()) <= tolerance) {
            dir.x -= 1.0;
        }

        if (Math.abs(position.x - nodeBBox.getMaxX()) <= tolerance) {
            dir.x += 1.0;
        }

        if (Math.abs(position.y - nodeBBox.getMinY()) <= tolerance) {
            dir.y -= 1.0;
        }

        if (Math.abs(position.y - nodeBBox.getMaxY()) <= tolerance) {
            dir.y += 1.0;
        }

        if (dir.lengthSquared() <= 1e-12) {
            return null;
        }

        dir.normalize();
        return dir;
    }
}
