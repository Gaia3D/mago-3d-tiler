package com.gaia3d.util;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.octree.GaiaOctreeVertices;
import com.gaia3d.basic.halfedge.HalfEdgeScene;
import com.gaia3d.basic.halfedge.HalfEdgeUtils;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.model.structure.GaiaFaceExplicit;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
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
        if(resultMapVertexToFace == null)
            resultMapVertexToFace = new HashMap<>();

        for(GaiaFaceExplicit face : faces)
        {
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

    public static List<GaiaFaceExplicit> getGaiaFacesExplicit(GaiaSurface surface, List<GaiaVertex> vertices, List<GaiaFaceExplicit> resultGaiaFaceExplicits)
    {
        if(resultGaiaFaceExplicits == null)
            resultGaiaFaceExplicits = new ArrayList<>();
        List<GaiaFace> faces = surface.getFaces();
        for(GaiaFace face : faces)
        {
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

    public static void deformSceneByVerticesConvexity(GaiaScene scene, double dist, double minHeight, double maxHeight)
    {
        List<GaiaVertex> totalVertices = new ArrayList<>();
        List<GaiaFaceExplicit> totalFacesExplicit = new ArrayList<>();
        List<GaiaFaceExplicit> currFacesExplicit = new ArrayList<>();
        List<GaiaPrimitive> primitives = scene.extractPrimitives(null);
        for (GaiaPrimitive primitive : primitives) {
            totalVertices.addAll(primitive.getVertices());
            List<GaiaSurface> surfaces = primitive.getSurfaces();
            for (GaiaSurface surface : surfaces) {
                currFacesExplicit.clear();
                GaiaSceneUtils.getGaiaFacesExplicit(surface, primitive.getVertices(), currFacesExplicit);
                totalFacesExplicit.addAll(currFacesExplicit);
            }
        }

        // 1rst, find the coincident vertices.***
        //List<GaiaVertex> primitiveVertices = primitive.getVertices();
        GaiaOctreeVertices octreeVertices = new GaiaOctreeVertices(null);
        octreeVertices.getVertices().addAll(totalVertices);
        octreeVertices.calculateSize();
        octreeVertices.setAsCube();
        octreeVertices.setMaxDepth(10);
        octreeVertices.setMinBoxSize(1.0); // 1m.***

        octreeVertices.makeTreeByMinVertexCount(50);

        List<GaiaOctreeVertices> octreesWithContents = new ArrayList<>();
        octreeVertices.extractOctreesWithContents(octreesWithContents);

        Map<GaiaVertex, GaiaVertex> mapVertexToVertexMaster = new HashMap<>();
        double error = 0.001;
        boolean checkTexCoord = false;
        boolean checkNormal = false;
        boolean checkColor = false;
        boolean checkBatchId = false;

        for (GaiaOctreeVertices octree : octreesWithContents) {
            List<GaiaVertex> vertices = octree.getVertices();
            GaiaPrimitiveUtils.getWeldableVertexMap(mapVertexToVertexMaster, vertices, error, checkTexCoord, checkNormal, checkColor, checkBatchId);
        }

        // make a map<vertexMaster, List<GaiaVertex>>
        Map<GaiaVertex, List<GaiaVertex>> mapVertexMasterToListVertices = new HashMap<>();
        for (GaiaVertex vertex : mapVertexToVertexMaster.keySet()) {
            GaiaVertex vertexMaster = mapVertexToVertexMaster.get(vertex);
            List<GaiaVertex> listVertices = mapVertexMasterToListVertices.computeIfAbsent(vertexMaster, k -> new ArrayList<>());
            listVertices.add(vertex);
        }

        // now, make map<GaiaVertex, GaiaFace> for each vertex.***
        Map<GaiaVertex, List<GaiaFaceExplicit>> mapVertexToFaceExplicits = GaiaSceneUtils.getMapVertexToFaceExplicits(totalFacesExplicit, null);

        Map<GaiaFaceExplicit, GaiaFaceExplicit> mapVisitedFaces = new HashMap<>();
        Map<GaiaVertex, GaiaVertex> mapVisitedVertices = new HashMap<>();

        int vertexCount = totalVertices.size();
        for (int i = 0; i < vertexCount; i++) {
            GaiaVertex vertex = totalVertices.get(i);
            if(mapVisitedVertices.containsKey(vertex)) {
                continue;
            }

            GaiaVertex vertexMaster = mapVertexToVertexMaster.get(vertex);
            List<GaiaVertex> currListVertices = mapVertexMasterToListVertices.get(vertexMaster);
            List<GaiaFaceExplicit> currListFaces = new ArrayList<>();
            if (currListVertices == null) {
                continue;
            }

            int currListVerticesCount = currListVertices.size();
            for (int j = 0; j < currListVerticesCount; j++) {
                GaiaVertex vertex2 = currListVertices.get(j);
                List<GaiaFaceExplicit> faces = mapVertexToFaceExplicits.get(vertex2);
                if (faces != null) {
                    currListFaces.addAll(faces);
                }
            }

            int currListFacesCount = currListFaces.size();
            Vector3d normalFinal = new Vector3d(0.0, 0.0, 0.0);
            mapVisitedFaces.clear(); // reset the visited faces.***

            for (int j = 0; j < currListFacesCount; j++) {
                GaiaFaceExplicit face = currListFaces.get(j);
                if (mapVisitedFaces.containsKey(face)) {
                    continue;
                }

                mapVisitedFaces.put(face, face);

                GaiaVertex vertex1 = face.getVertex1();
                GaiaVertex vertex2 = face.getVertex2();
                GaiaVertex vertex3 = face.getVertex3();
                Vector3d normal = GeometryUtils.calcNormal3D(vertex1.getPosition(), vertex2.getPosition(), vertex3.getPosition());
                face.setPlaneNormal(normal);
                normalFinal.add(normal);
            }

            normalFinal.normalize();

            // finally set the normals.***
            for (int j = 0; j < currListVerticesCount; j++) {
                GaiaVertex vertex2 = currListVertices.get(j);
                if(mapVisitedVertices.containsKey(vertex2)) {
                    continue;
                }
                mapVisitedVertices.put(vertex2, vertex2);

                vertex2.setNormal(new Vector3d(normalFinal));
            }
        }

        boolean finished = false;
        int iteration = 0;
//        while(!finished && iteration < 100) {
//            finished = !modifyVerticalNormalsOneIteration(totalFacesExplicit);
//            iteration++;
//        }

        if(iteration > 95) {
            log.info("The iteration is 100.***");
        }


        for (int i = 0; i < vertexCount; i++) {
            GaiaVertex vertex = totalVertices.get(i);
            Vector3d position = vertex.getPosition();
            double posZ = position.z;
            Vector3d normal = vertex.getNormal();

            double relPosZ = (posZ - minHeight) / (maxHeight - minHeight);
            double finalFactor = dist * relPosZ;

            Vector3d normalXY = new Vector3d(normal.x, normal.y, 0.0);
            normalXY.normalize();

            position.add(-normalXY.x * finalFactor, -normalXY.y * finalFactor, 0.0);
            //position.add(-normal.x * finalFactor, -normal.y * finalFactor, -normal.z * finalFactor);
        }
    }

    public static boolean modifyVerticalNormalsOneIteration(List<GaiaFaceExplicit> faces)
    {
        boolean modified = false;
        int facesCount = faces.size();
        for(int i=0; i<facesCount; i++)
        {
            GaiaFaceExplicit face = faces.get(i);
            Vector3d planeNormal = face.getPlaneNormal();
            if(isVerticalVector(planeNormal)) {
                // check if the normals are vertical.***
                GaiaVertex vertex1 = face.getVertex1();
                GaiaVertex vertex2 = face.getVertex2();
                GaiaVertex vertex3 = face.getVertex3();
                Vector3d normal1 = vertex1.getNormal();
                Vector3d normal2 = vertex2.getNormal();
                Vector3d normal3 = vertex3.getNormal();

                int mostHorizontalIdx = getMostHorizontalVector(normal1, normal2, normal3);

                Vector3d mostHorizontalVector = null;
                GaiaVertex mostHorizontalVertex = null;
                if(mostHorizontalIdx == 1) {
                    mostHorizontalVector = normal1;
                    mostHorizontalVertex = vertex1;
                } else if(mostHorizontalIdx == 2) {
                    mostHorizontalVector = normal2;
                    mostHorizontalVertex = vertex2;
                } else {
                    mostHorizontalVector = normal3;
                    mostHorizontalVertex = vertex3;
                }
                if(!isVerticalVector(mostHorizontalVector)) {
                    // modify the vertical normal.***
                    if (isVerticalVector(normal1)) {
                        // add to normal1 a fractional part of the normal of the vertex.***
                        //double dist = mostHorizontalVertex.getPosition().distance(vertex1.getPosition());
                        double factor = 0.9;
                        normal1.add(mostHorizontalVector.x * factor, mostHorizontalVector.y * factor, mostHorizontalVector.z * factor);
                        normal1.normalize();
                        modified = true;
                    }

                    if (isVerticalVector(normal2)) {
                        // add to normal2 a fractional part of the normal of the vertex.***
                        //double dist = mostHorizontalVertex.getPosition().distance(vertex2.getPosition());
                        double factor = 0.9;
                        normal2.add(mostHorizontalVector.x * factor, mostHorizontalVector.y * factor, mostHorizontalVector.z * factor);
                        normal2.normalize();
                        modified = true;
                    }

                    if (isVerticalVector(normal3)) {
                        // add to normal3 a fractional part of the normal of the vertex.***
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
