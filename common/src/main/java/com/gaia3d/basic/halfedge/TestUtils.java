package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.model.*;
import org.joml.Vector3d;

import java.util.List;

public class TestUtils {
    public static double getMaxHalfEdgeLength(HalfEdgeScene scene) {
        double maxHalfEdgeLength = -1;
        List<HalfEdgeSurface> surfaces = scene.extractSurfaces(null);
        for (HalfEdgeSurface halfEdgeSurface : surfaces) {
            List<HalfEdge> halfEdges = halfEdgeSurface.getHalfEdges();
            for (HalfEdge halfEdge : halfEdges) {
                double edgeLength = halfEdge.getLength();
                if (edgeLength > maxHalfEdgeLength) {
                    maxHalfEdgeLength = edgeLength;
                }
            }
        }
        return maxHalfEdgeLength;
    }

    public static double getMaxEdgeLength(GaiaScene scene) {
        double maxEdgeLength = -1;
        List<GaiaPrimitive> primitives = scene.extractPrimitives(null);
        for (GaiaPrimitive primitive : primitives) {
            List<GaiaSurface> surfaces = primitive.getSurfaces();
            for (GaiaSurface surface : surfaces) {
                int[] indices = surface.getIndices();
                List<GaiaVertex> vertices = primitive.getVertices();
                for (int i = 0; i < indices.length; i += 3) {
                    GaiaVertex v0 = vertices.get(indices[i]);
                    GaiaVertex v1 = vertices.get(indices[i + 1]);
                    GaiaVertex v2 = vertices.get(indices[i + 2]);
                    double edgeLength = v0.getPosition().distance(v1.getPosition());
                    if (edgeLength > maxEdgeLength) {
                        maxEdgeLength = edgeLength;
                    }
                    edgeLength = v1.getPosition().distance(v2.getPosition());
                    if (edgeLength > maxEdgeLength) {
                        maxEdgeLength = edgeLength;
                    }
                    edgeLength = v2.getPosition().distance(v0.getPosition());
                    if (edgeLength > maxEdgeLength) {
                        maxEdgeLength = edgeLength;
                    }
                }
            }
        }
        return maxEdgeLength;
    }

    public static void translateGaiaScene(GaiaScene scene, double x, double y, double z) {
        List<GaiaPrimitive> primitives = scene.extractPrimitives(null);
        for (GaiaPrimitive primitive : primitives) {
            List<GaiaVertex> vertices = primitive.getVertices();
            for (GaiaVertex vertex : vertices) {
                vertex.getPosition().add(x, y, z);
            }
        }
    }

    public static void translateHalfEdgeScene(HalfEdgeScene scene, double x, double y, double z) {
        List<HalfEdgeSurface> surfaces = scene.extractSurfaces(null);
        for (HalfEdgeSurface halfEdgeSurface : surfaces) {
            List<HalfEdgeVertex> vertices = halfEdgeSurface.getVertices();
            for (HalfEdgeVertex vertex : vertices) {
                vertex.getPosition().add(x, y, z);
            }
        }
    }

    public static void rotateXAxisHalfEdgeScene(HalfEdgeScene scene, double angle) {
        List<HalfEdgeSurface> surfaces = scene.extractSurfaces(null);
        for (HalfEdgeSurface halfEdgeSurface : surfaces) {
            List<HalfEdgeVertex> vertices = halfEdgeSurface.getVertices();
            for (HalfEdgeVertex vertex : vertices) {
                Vector3d position = vertex.getPosition();
                double y = position.y;
                double z = position.z;
                position.y = y * Math.cos(angle) - z * Math.sin(angle);
                position.z = y * Math.sin(angle) + z * Math.cos(angle);
            }
        }
    }
}
