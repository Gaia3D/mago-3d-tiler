package com.gaia3d.util;

import com.gaia3d.basic.model.*;
import com.gaia3d.basic.model.structure.GaiaFaceExplicit;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class GaiaSceneUtils {
    public static GaiaScene getSceneRectangularNet(int numCols, int numRows, double width, double height, boolean calculateTexCoords) {
        GaiaScene scene = new GaiaScene();
        GaiaNode rootNode = new GaiaNode();
        scene.getNodes().add(rootNode);

        GaiaNode node = new GaiaNode();
        rootNode.getChildren().add(node);

        GaiaMesh mesh = new GaiaMesh();
        node.getMeshes().add(mesh);

        GaiaPrimitive primitive = GaiaPrimitiveUtils.getRectangularNet(numCols, numRows, width, height, calculateTexCoords);
        mesh.getPrimitives().add(primitive);
        return scene;
    }

    public static boolean checkSceneMaterials(GaiaScene scene) {
        for (GaiaNode node : scene.getNodes()) {
            for (GaiaMesh mesh : node.getMeshes()) {
                for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                    int matId = primitive.getMaterialIndex();

                    if (matId < 0 || matId >= scene.getMaterials().size()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static Map<GaiaVertex, List<GaiaFaceExplicit>> getMapVertexToFaceExplicits(List<GaiaFaceExplicit> faces, Map<GaiaVertex, List<GaiaFaceExplicit>> resultMapVertexToFace) {
        if (resultMapVertexToFace == null)
            resultMapVertexToFace = new HashMap<>();

        for (GaiaFaceExplicit face : faces) {
            GaiaVertex vertex1 = face.getVertex1();
            GaiaVertex vertex2 = face.getVertex2();
            GaiaVertex vertex3 = face.getVertex3();
            List<GaiaFaceExplicit> listFaces = resultMapVertexToFace.computeIfAbsent(vertex1, k -> new ArrayList<>());
            listFaces.add(face);
            listFaces = resultMapVertexToFace.computeIfAbsent(vertex2, k -> new ArrayList<>());
            listFaces.add(face);
            listFaces = resultMapVertexToFace.computeIfAbsent(vertex3, k -> new ArrayList<>());
            listFaces.add(face);
        }

        return resultMapVertexToFace;
    }

    public static List<GaiaFaceExplicit> getGaiaFacesExplicit(GaiaSurface surface, List<GaiaVertex> vertices, List<GaiaFaceExplicit> resultGaiaFaceExplicits) {
        if (resultGaiaFaceExplicits == null)
            resultGaiaFaceExplicits = new ArrayList<>();
        List<GaiaFace> faces = surface.getFaces();
        for (GaiaFace face : faces) {
            GaiaFaceExplicit gaiaFaceExplicit = new GaiaFaceExplicit();
            int[] indices = face.getIndices();
            GaiaVertex vertex1 = vertices.get(indices[0]);
            GaiaVertex vertex2 = vertices.get(indices[1]);
            GaiaVertex vertex3 = vertices.get(indices[2]);
            gaiaFaceExplicit.setVertices(vertex1, vertex2, vertex3);
            resultGaiaFaceExplicits.add(gaiaFaceExplicit);
        }
        return resultGaiaFaceExplicits;
    }

    public static int getMostHorizontalVector(Vector3d vector1, Vector3d vector2, Vector3d vector3) {
        int result = -1;
        Vector3d vectorZ = new Vector3d(0.0, 0.0, 1.0);
        double dot1 = Math.abs(vector1.dot(vectorZ));
        double dot2 = Math.abs(vector2.dot(vectorZ));
        double dot3 = Math.abs(vector3.dot(vectorZ));

        if (dot1 < dot2 && dot1 < dot3) {
            result = 1;
        } else if (dot2 < dot1 && dot2 < dot3) {
            result = 2;
        } else {
            result = 3;
        }
        return result;
    }

    public static boolean isVerticalVector(Vector3d vector) {
        return vector.z > 0.95;
    }

    public static boolean modifyVerticalNormalsOneIteration(List<GaiaFaceExplicit> faces) {
        boolean modified = false;
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            GaiaFaceExplicit face = faces.get(i);
            Vector3d planeNormal = face.getPlaneNormal();
            if (isVerticalVector(planeNormal)) {
                // check if the normals are vertical.
                GaiaVertex vertex1 = face.getVertex1();
                GaiaVertex vertex2 = face.getVertex2();
                GaiaVertex vertex3 = face.getVertex3();
                Vector3d normal1 = vertex1.getNormal();
                Vector3d normal2 = vertex2.getNormal();
                Vector3d normal3 = vertex3.getNormal();

                int mostHorizontalIdx = getMostHorizontalVector(normal1, normal2, normal3);

                Vector3d mostHorizontalVector = null;
                GaiaVertex mostHorizontalVertex = null;
                if (mostHorizontalIdx == 1) {
                    mostHorizontalVector = normal1;
                    mostHorizontalVertex = vertex1;
                } else if (mostHorizontalIdx == 2) {
                    mostHorizontalVector = normal2;
                    mostHorizontalVertex = vertex2;
                } else {
                    mostHorizontalVector = normal3;
                    mostHorizontalVertex = vertex3;
                }
                if (!isVerticalVector(mostHorizontalVector)) {
                    // modify the vertical normal.
                    if (isVerticalVector(normal1)) {
                        // add to normal1 a fractional part of the normal of the vertex.
                        //double dist = mostHorizontalVertex.getPosition().distance(vertex1.getPosition());
                        double factor = 0.9;
                        normal1.add(mostHorizontalVector.x * factor, mostHorizontalVector.y * factor, mostHorizontalVector.z * factor);
                        normal1.normalize();
                        modified = true;
                    }

                    if (isVerticalVector(normal2)) {
                        // add to normal2 a fractional part of the normal of the vertex.
                        //double dist = mostHorizontalVertex.getPosition().distance(vertex2.getPosition());
                        double factor = 0.9;
                        normal2.add(mostHorizontalVector.x * factor, mostHorizontalVector.y * factor, mostHorizontalVector.z * factor);
                        normal2.normalize();
                        modified = true;
                    }

                    if (isVerticalVector(normal3)) {
                        // add to normal3 a fractional part of the normal of the vertex.
                        //double dist = mostHorizontalVertex.getPosition().distance(vertex3.getPosition());
                        double factor = 0.9;
                        normal3.add(mostHorizontalVector.x * factor, mostHorizontalVector.y * factor, mostHorizontalVector.z * factor);
                        normal3.normalize();
                        modified = true;
                    }
                }
            }
        }

        return modified;
    }
}
