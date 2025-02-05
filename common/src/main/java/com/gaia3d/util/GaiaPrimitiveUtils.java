package com.gaia3d.util;

import com.gaia3d.basic.model.GaiaFace;
import com.gaia3d.basic.model.GaiaPrimitive;
import com.gaia3d.basic.model.GaiaSurface;
import com.gaia3d.basic.model.GaiaVertex;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GaiaPrimitiveUtils {

    public static void mergePrimitives(GaiaPrimitive primitiveMaster, GaiaPrimitive primitive) {
        int vertexCountMaster = primitiveMaster.getVertices().size();

        primitiveMaster.getVertices().addAll(primitive.getVertices());
        int surfacesCount = primitive.getSurfaces().size();
        for (int i = 0; i < surfacesCount; i++) {
            GaiaSurface surface = primitive.getSurfaces().get(i);
            GaiaSurface surfaceNew = new GaiaSurface();
            int facesCount = surface.getFaces().size();
            for (int j = 0; j < facesCount; j++) {
                GaiaFace face = surface.getFaces().get(j);
                GaiaFace faceNew = new GaiaFace();
                int[] indices = face.getIndices();
                int indicesCount = indices.length;
                int[] indicesNew = new int[indicesCount];
                for (int k = 0; k < indicesCount; k++) {
                    indicesNew[k] = indices[k] + vertexCountMaster;
                }
                faceNew.setIndices(indicesNew);
                surfaceNew.getFaces().add(faceNew);
            }

            primitiveMaster.getSurfaces().add(surfaceNew);
        }
    }

    public static GaiaPrimitive getRectangularNet(int numCols, int numRows, double width, double height, boolean calculateTexCoords) {
        GaiaPrimitive primitive = new GaiaPrimitive();
        double xStep = width / (numCols - 1);
        double yStep = height / (numRows - 1);
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                double x = j * xStep;
                double y = i * yStep;
                double z = 0;
                GaiaVertex vertex = new GaiaVertex();
                vertex.setPosition(new Vector3d(x, y, z));
                primitive.getVertices().add(vertex);

                if (calculateTexCoords) {
                    double s = (double) j / (double) (numCols - 1);
                    double t = (double) i / (double) (numRows - 1);
                    vertex.setTexcoords(new Vector2d(s, t));
                }
            }
        }

        GaiaSurface surface = new GaiaSurface();
        primitive.getSurfaces().add(surface);

        for (int i = 0; i < numRows - 1; i++) {
            for (int j = 0; j < numCols - 1; j++) {
                GaiaFace faceA = new GaiaFace();
                GaiaFace faceB = new GaiaFace();
                int index1 = i * numCols + j;
                int index2 = i * numCols + j + 1;
                int index3 = (i + 1) * numCols + j + 1;
                int index4 = (i + 1) * numCols + j;
                faceA.setIndices(new int[]{index1, index2, index3});
                faceB.setIndices(new int[]{index1, index3, index4});

                surface.getFaces().add(faceA);
                surface.getFaces().add(faceB);
            }
        }

        return primitive;
    }

    public static void getWeldableVertexMap(Map<GaiaVertex, GaiaVertex> mapVertexToVertexMaster, List<GaiaVertex> vertices, double error, boolean checkTexCoord, boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        Map<GaiaVertex, GaiaVertex> visitedMap = new HashMap<>();
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            if (visitedMap.containsKey(vertex)) {
                continue;
            }

            mapVertexToVertexMaster.put(vertex, vertex);

            for (int j = i + 1; j < verticesCount; j++) {
                GaiaVertex vertex2 = vertices.get(j);
                if (visitedMap.containsKey(vertex2)) {
                    continue;
                }
                if (vertex.isWeldable(vertex2, error, checkTexCoord, checkNormal, checkColor, checkBatchId)) {
                    mapVertexToVertexMaster.put(vertex2, vertex);

                    visitedMap.put(vertex, vertex);
                    visitedMap.put(vertex2, vertex2);
                }
            }
        }
    }


    public static int calculateConvexity(GaiaVertex vertex, Vector3d normal, List<GaiaVertex> neighborVertices, double error) {
        int convexity = 1;
        Vector3d vertexPosition = vertex.getPosition();
        Vector3d v = new Vector3d();
        boolean isPlane = true;
        for (GaiaVertex neighborVertex : neighborVertices) {
            if (neighborVertex == vertex) {
                continue;
            }
            Vector3d position = neighborVertex.getPosition();
            v.set(position).sub(vertexPosition);
            v.normalize();

            if (Double.isNaN(v.x) || Double.isNaN(v.y) || Double.isNaN(v.z)) {
                continue;
            }

            double dot = v.dot(normal);
            if (Math.abs(dot) < error) {
                continue;
            }

            if (dot > 0.0) {
                return -1;
            } else if (dot < 0.0) {
                isPlane = false;
            }
        }

        if (isPlane) {
            return 0;
        }

        return convexity;
    }


}
