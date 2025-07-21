package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.model.*;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.ArrayList;
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

    public static boolean checkHalfEdgeScene(HalfEdgeScene scene) {
        // 1rst, check vertices
        List<HalfEdgeSurface> surfaces = scene.extractSurfaces(null);
        for (HalfEdgeSurface halfEdgeSurface : surfaces) {
            TestUtils.checkHalfEdgeSurface(halfEdgeSurface);
        }

        return true;
    }

    public static boolean checkHalfEdgeSurface(HalfEdgeSurface halfEdgeSurface) {
        List<HalfEdgeVertex> vertices = halfEdgeSurface.getVertices();
        int verticesCount = vertices.size();
        Map<HalfEdgeVertex, HalfEdgeVertex> verticesMap = new HashMap<>();
        if (verticesCount == 0) {

        }
        for (HalfEdgeFace face : halfEdgeSurface.getFaces()) {
            List<HalfEdge> faceHalfEdges = face.getHalfEdgesLoop(null);
            for (HalfEdge halfEdge : faceHalfEdges) {
                if (halfEdge == null) {

                }
                HalfEdgeVertex startVertex = halfEdge.getStartVertex();
                if (startVertex == null) {

                }
                HalfEdgeVertex endVertex = halfEdge.getEndVertex();
                if (endVertex == null) {

                }
                if (startVertex == endVertex) {

                }

                double length = halfEdge.getLength();
                if (length > 18.0) {

                }
            }
            List<HalfEdgeVertex> faceVertices = face.getVertices(null);
            for (HalfEdgeVertex vertex : faceVertices) {
                if (vertex == null) {

                }
                verticesMap.put(vertex, vertex);
                if (!vertices.contains(vertex)) {

                }

                Vector3d vertexPosition = vertex.getPosition();
                if (vertexPosition == null) {

                }

                if (vertexPosition.x == 0 && vertexPosition.y == 0 && vertexPosition.z == 0) {

                }

                Vector2d vertexTexCoord = vertex.getTexcoords();
                if (vertexTexCoord == null) {

                }

                Vector3d vertexNormal = vertex.getNormal();
                if (vertexNormal == null) {

                }

                byte[] vertexColor = vertex.getColor();
                if (vertexColor == null) {

                }
            }
        }

        if (vertices.size() != verticesMap.size()) {

        }
        return true;
    }

    public static boolean checkGaiaScene(GaiaScene scene) {
        // 1rst, check vertices
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

        }
        for (GaiaSurface surface : primitive.getSurfaces()) {
            int[] surfaceIndices = surface.getIndices();
            for (GaiaFace face : surface.getFaces()) {
                int[] faceIndices = face.getIndices();
                int faceIndicesCount = faceIndices.length;

                for (int i = 0; i < faceIndicesCount; i++) {
                    GaiaVertex vertex = vertices.get(faceIndices[i]);
                    if (vertex == null) {

                    }
                    verticesMap.put(vertex, vertex);
                    if (!vertices.contains(vertex)) {

                    }

                    Vector3d vertexPosition = vertex.getPosition();
                    if (vertexPosition == null) {

                    }

                    if (vertexPosition.x == 0 && vertexPosition.y == 0 && vertexPosition.z == 0) {

                    }

                    Vector2d vertexTexCoord = vertex.getTexcoords();
                    if (vertexTexCoord == null) {

                    }

                    Vector3d vertexNormal = vertex.getNormal();
                    if (vertexNormal == null) {

                    }

                    byte[] vertexColor = vertex.getColor();
                    if (vertexColor == null) {

                    }
                }
            }
        }

        if (vertices.size() != verticesMap.size()) {

            GaiaVertex lastVertex = vertices.get(vertices.size() - 1);
            for (GaiaVertex vertex : vertices) {
                if (vertex == null) {

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

        }
        return true;
    }

    public static int checkTexCoordsOfHalfEdgeScene(HalfEdgeScene scene) {
        int badFacesCount = 0;
        List<HalfEdgeSurface> surfaces = scene.extractSurfaces(null);
        for (HalfEdgeSurface halfEdgeSurface : surfaces) {
            int localBadFacesCount = checkTexCoordsOfHalfEdgeSurface(halfEdgeSurface);
            badFacesCount += localBadFacesCount;
        }

        return badFacesCount;
    }

    public static int checkTexCoordsOfHalfEdgeSurface(HalfEdgeSurface halfEdgeSurface) {
        int badFacesCount = 0;
        for (HalfEdgeFace face : halfEdgeSurface.getFaces()) {
            if (!checkTexCoordsOfHalfEdgeFace(face)) {
                badFacesCount++;
            }
        }
        return badFacesCount;
    }

    public static int checkTexCoordsOfHalfEdgeFaces(List<HalfEdgeFace> faces) {
        int badFacesCount = 0;
        for (HalfEdgeFace face : faces) {
            if (!checkTexCoordsOfHalfEdgeFace(face)) {
                badFacesCount++;
            }
        }
        return badFacesCount;
    }

    public static boolean checkTexCoordsOfHalfEdgeFace(HalfEdgeFace face) {
        List<HalfEdgeVertex> faceVertices = face.getVertices(null);
        Vector3d pos0 = faceVertices.get(0).getPosition();
        Vector3d pos1 = faceVertices.get(1).getPosition();
        Vector3d pos2 = faceVertices.get(2).getPosition();

        Vector2d texCoord0 = faceVertices.get(0).getTexcoords();
        Vector2d texCoord1 = faceVertices.get(1).getTexcoords();
        Vector2d texCoord2 = faceVertices.get(2).getTexcoords();

        GaiaRectangle texBRect = face.getTexCoordBoundingRectangle(null, false);

        double width = texBRect.getWidth();
        double height = texBRect.getHeight();

        if (width > 0.5) {

        }

        if (height > 0.5) {

        }

        if (texCoord0.equals(texCoord1)) {
            return false;
        }

        if (texCoord0.equals(texCoord2)) {
            return false;
        }

        return !texCoord1.equals(texCoord2);
    }

    public static boolean checkClassifyIdAndCamDirOfHalfEdgeFace(HalfEdgeFace face) {
        List<HalfEdgeFace> adjacentFaces = face.getAdjacentFaces(null);
        for (HalfEdgeFace adjacentFace : adjacentFaces) {
            if (face.getClassifyId() != adjacentFace.getClassifyId()) {
                return false;
            }

            if (face.getCameraDirectionType() != adjacentFace.getCameraDirectionType()) {
                return false;
            }
        }

        return true;
    }

    public static boolean checkWeldedGroups(HalfEdgeScene scene) {
        List<HalfEdgeSurface> surfaces = scene.extractSurfaces(null);
        for (HalfEdgeSurface halfEdgeSurface : surfaces) {
            List<List<HalfEdgeFace>> resultWeldedFacesGroups = new ArrayList<>();
            halfEdgeSurface.getWeldedFacesGroups(resultWeldedFacesGroups);

        }
        return true;
    }

    public static boolean checkWeldedFacesGroups(List<List<HalfEdgeFace>> resultWeldedFacesGroups) {
        int groupsCount = resultWeldedFacesGroups.size();
        for (int i = 0; i < groupsCount; i++) {
            List<HalfEdgeFace> group = resultWeldedFacesGroups.get(i);
            int facesCount = group.size();
            int classifyId = group.get(0).getClassifyId();
            CameraDirectionType cameraDirectionType = group.get(0).getCameraDirectionType();
            for (int j = 0; j < facesCount; j++) {
                HalfEdgeFace face = group.get(j);
                if (face == null) {

                }

                if (face.getClassifyId() != classifyId) {
                    return false;
                }

                if (face.getCameraDirectionType() != cameraDirectionType) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean checkIfExistCoincidentTexCoords(List<HalfEdgeVertex> vertexList, List<HalfEdgeVertex> resultEqualVertices, Map<HalfEdgeVertex, HalfEdgeVertex> visitedVertices) {
        int vertexCount = vertexList.size();
        for (int i = 0; i < vertexCount; i++) {
            HalfEdgeVertex vertex = vertexList.get(i);
            if (visitedVertices.containsKey(vertex)) {
                continue;
            }

            Vector2d texCoord = vertex.getTexcoords();
            for (int j = i + 1; j < vertexCount; j++) {
                HalfEdgeVertex otherVertex = vertexList.get(j);
                Vector2d otherTexCoord = otherVertex.getTexcoords();
                if (texCoord.equals(otherTexCoord)) {
                    resultEqualVertices.add(vertex);
                    resultEqualVertices.add(otherVertex);
                    visitedVertices.put(vertex, vertex);
                    return true;
                }
            }
        }
        return false;
    }

}
