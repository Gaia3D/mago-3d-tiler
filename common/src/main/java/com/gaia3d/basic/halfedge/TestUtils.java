package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.model.*;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static boolean checkHalfEdgeScene(HalfEdgeScene scene)
    {
        // 1rst, check vertices.***
        List<HalfEdgeSurface> surfaces = scene.extractSurfaces(null);
        for (HalfEdgeSurface halfEdgeSurface : surfaces) {
            TestUtils.checkHalfEdgeSurface(halfEdgeSurface);
        }

        return true;
    }

    public static boolean checkHalfEdgeSurface(HalfEdgeSurface halfEdgeSurface)
    {
        List<HalfEdgeVertex> vertices = halfEdgeSurface.getVertices();
        int verticesCount = vertices.size();
        Map<HalfEdgeVertex, HalfEdgeVertex> verticesMap = new HashMap<>();
        if(verticesCount == 0)
        {
            int hola = 0;
        }
        for(HalfEdgeFace face : halfEdgeSurface.getFaces()) {
            List<HalfEdge> faceHalfEdges = face.getHalfEdgesLoop(null);
            for(HalfEdge halfEdge : faceHalfEdges) {
                if(halfEdge == null) {
                    int hola = 0;
                }
                HalfEdgeVertex startVertex = halfEdge.getStartVertex();
                if(startVertex == null) {
                    int hola = 0;
                }
                HalfEdgeVertex endVertex = halfEdge.getEndVertex();
                if(endVertex == null) {
                    int hola = 0;
                }
                if(startVertex == endVertex) {
                    int hola = 0;
                }

                double length = halfEdge.getLength();
                if(length > 18.0) {
                    int hola = 0;
                }
            }
            List<HalfEdgeVertex> faceVertices = face.getVertices(null);
            for(HalfEdgeVertex vertex : faceVertices) {
                if(vertex == null) {
                    int hola = 0;
                }
                verticesMap.put(vertex, vertex);
                if(!vertices.contains(vertex)) {
                    int hola = 0;
                }

                Vector3d vertexPosition = vertex.getPosition();
                if(vertexPosition == null) {
                    int hola = 0;
                }

                if(vertexPosition.x == 0 && vertexPosition.y == 0 && vertexPosition.z == 0) {
                    int hola = 0;
                }

                Vector2d vertexTexCoord = vertex.getTexcoords();
                if(vertexTexCoord == null) {
                    int hola = 0;
                }

                Vector3d vertexNormal = vertex.getNormal();
                if(vertexNormal == null) {
                    int hola = 0;
                }

                byte[] vertexColor = vertex.getColor();
                if(vertexColor == null) {
                    int hola = 0;
                }
            }
        }

        if(vertices.size() != verticesMap.size()) {
            int hola = 0;
        }
        return true;
    }

    public static boolean checkGaiaScene(GaiaScene scene)
    {
        // 1rst, check vertices.***
        List<GaiaPrimitive> primitives = scene.extractPrimitives(null);
        for (GaiaPrimitive primitive : primitives) {
            TestUtils.checkGaiaPrimitive(primitive);
        }

        return true;
    }

    public static boolean checkGaiaPrimitive(GaiaPrimitive primitive) {
        List<GaiaVertex> vertices = primitive.getVertices();
        int verticesCount = vertices.size();
        Map<GaiaVertex, GaiaVertex> verticesMap = new HashMap<>();
        if (verticesCount == 0) {
            int hola = 0;
        }
        for (GaiaSurface surface : primitive.getSurfaces()) {
            int[] surfaceIndices = surface.getIndices();
            for (GaiaFace face : surface.getFaces()) {
                int[] faceIndices = face.getIndices();
                int faceIndicesCount = faceIndices.length;

                for (int i = 0; i < faceIndicesCount; i++) {
                    GaiaVertex vertex = vertices.get(faceIndices[i]);
                    if (vertex == null) {
                        int hola = 0;
                    }
                    verticesMap.put(vertex, vertex);
                    if (!vertices.contains(vertex)) {
                        int hola = 0;
                    }

                    Vector3d vertexPosition = vertex.getPosition();
                    if (vertexPosition == null) {
                        int hola = 0;
                    }

                    if (vertexPosition.x == 0 && vertexPosition.y == 0 && vertexPosition.z == 0) {
                        int hola = 0;
                    }

                    Vector2d vertexTexCoord = vertex.getTexcoords();
                    if (vertexTexCoord == null) {
                        int hola = 0;
                    }

                    Vector3d vertexNormal = vertex.getNormal();
                    if (vertexNormal == null) {
                        int hola = 0;
                    }

                    byte[] vertexColor = vertex.getColor();
                    if (vertexColor == null) {
                        int hola = 0;
                    }
                }
            }
        }

        if (vertices.size() != verticesMap.size()) {
            int hola = 0;
            GaiaVertex lastVertex = vertices.get(vertices.size() - 1);
            for(GaiaVertex vertex : vertices) {
                if(vertex == null) {
                    int hola2 = 0;
                }
            }
        }
        return true;
    }

    public static boolean checkHalfEdgeSurfacesHalfEdgeVertices(List<HalfEdgeSurface> halfEdgeSurfaces, List<HalfEdgeVertex> halfEdgeVertices) {
        Map<HalfEdgeVertex, HalfEdgeVertex> verticesMap = new HashMap<>();
        for (HalfEdgeSurface halfEdgeSurface : halfEdgeSurfaces) {
            for (HalfEdgeFace face : halfEdgeSurface.getFaces()) {
                List<HalfEdgeVertex> faceVertices = face.getVertices(null);
                for (HalfEdgeVertex vertex : faceVertices) {
                    verticesMap.put(vertex, vertex);
                }
            }
        }
        if (halfEdgeVertices.size() != verticesMap.size()) {
            int hola = 0;
        }
        return true;
    }
}
