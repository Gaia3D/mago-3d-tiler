package com.gaia3d.basic.remesher;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.exchangable.SceneInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ReMesherVertexCluster {
    private static Map<GaiaVertex, List<GaiaFace>> makeMapVertexToFaces(GaiaScene gaiaScene) {
        Map<GaiaVertex, List<GaiaFace>> mapVertexToFaces = new HashMap<>();
        List<GaiaPrimitive> primitives = gaiaScene.extractPrimitives(null);
        for (GaiaPrimitive primitive : primitives) {
            List<GaiaVertex> vertices = primitive.getVertices();
            List<GaiaSurface> surfaces = primitive.getSurfaces();
            for (GaiaSurface surface : surfaces) {
                List<GaiaFace> faces = surface.getFaces();
                for (GaiaFace face : faces) {
                    int indices[] = face.getIndices();
                    for (int index : indices) {
                        GaiaVertex vertex = vertices.get(index);
                        List<GaiaFace> faceList = mapVertexToFaces.computeIfAbsent(vertex, k -> new java.util.ArrayList<>());
                        faceList.add(face);
                    }
                }
            }
        }
        return mapVertexToFaces;
    }

    public static void reMeshScene(GaiaScene gaiaScene, ReMeshParameters reMeshParams, Map<Vector3i, List<GaiaVertex>> vertexClusters) {
        //************************************************************************************
        // Note: the gaiaScene must spend its transform matrix before calling this method.****
        // Note: the gaiaScene must join all surfaces before calling this method.*************
        //************************************************************************************
        Map<GaiaVertex, List<GaiaFace>> mapVertexToFaces = makeMapVertexToFaces(gaiaScene);
        Map<GaiaVertex, Integer> vertexToIndexMap = new HashMap<>();

        List<GaiaPrimitive> primitives = gaiaScene.extractPrimitives(null);
        // There are only 1 primitive in the gaiaScene, so we can use it directly.
        List<GaiaVertex> vertices = primitives.get(0).getVertices();

        CellGrid3D cellGrid = reMeshParams.getCellGrid();
        Map<Vector3i, Vector3d> cellAveragePositions = reMeshParams.getCellAveragePositions();

        // 1rs, make map of vertex to index
        for (int i = 0; i < vertices.size(); i++) {
            GaiaVertex vertex = vertices.get(i);
            vertexToIndexMap.put(vertex, i);
        }

        Vector3i sceneMinCellIndex = null;
        Vector3i sceneMaxCellIndex = null;

        for (GaiaPrimitive primitive : primitives) {
            List<GaiaSurface> surfaces = primitive.getSurfaces();
            for (GaiaSurface surface : surfaces) {
                List<GaiaFace> faces = surface.getFaces();
                for (GaiaFace face : faces) {
                    int[] indices = face.getIndices();
                    for (int index : indices) {
                        GaiaVertex vertex = vertices.get(index);
                        Vector3d position = vertex.getPosition();
                        Vector3i cellIndex = cellGrid.getCellIndex(position);
                        List<GaiaVertex> cluster = vertexClusters.computeIfAbsent(cellIndex, k -> new java.util.ArrayList<>());
                        cluster.add(vertex);

                        // update scene min and max cell index
                        int currCellX = cellIndex.x;
                        int currCellY = cellIndex.y;
                        int currCellZ = cellIndex.z;

                        if (sceneMinCellIndex == null) {
                            sceneMinCellIndex = new Vector3i(currCellX, currCellY, currCellZ);
                            sceneMaxCellIndex = new Vector3i(currCellX, currCellY, currCellZ);
                        } else {
                            if (currCellX < sceneMinCellIndex.x) sceneMinCellIndex.x = currCellX;
                            if (currCellY < sceneMinCellIndex.y) sceneMinCellIndex.y = currCellY;
                            if (currCellZ < sceneMinCellIndex.z) sceneMinCellIndex.z = currCellZ;

                            if (currCellX > sceneMaxCellIndex.x) sceneMaxCellIndex.x = currCellX;
                            if (currCellY > sceneMaxCellIndex.y) sceneMaxCellIndex.y = currCellY;
                            if (currCellZ > sceneMaxCellIndex.z) sceneMaxCellIndex.z = currCellZ;
                        }
                    }
                }
            }
        }

        // Now we have the clusters of vertices in the vertexClusters map
        for (Map.Entry<Vector3i, List<GaiaVertex>> entry : vertexClusters.entrySet()) {
            Vector3i cellIndex = entry.getKey();
            List<GaiaVertex> cluster = entry.getValue();

            if (cluster.size() < 2) {
                continue; // Skip clusters with less than 2 vertices
            }

            // check if exists the average position for the cell
            Vector3d averagePosition = cellAveragePositions.get(cellIndex);
            if (averagePosition == null) {
                // Calculate the average position of the cluster
                averagePosition = new Vector3d();
                for (GaiaVertex vertex : cluster) {
                    if (vertex == null || vertex.getPosition() == null) {
                        log.error("ReMesh process: vertex or position is null");
                        continue;
                    }
                    averagePosition.add(vertex.getPosition());
                }
                averagePosition.div(cluster.size());
                cellAveragePositions.put(cellIndex, averagePosition);
            } else {
                // If the average position already exists, use it
                log.debug("Using existing average position for cell index: {}", cellIndex);
            }

            // Create a new vertex at the average position
            GaiaVertex newVertex = new GaiaVertex();
            Vector3d averagePositionCopy = new Vector3d(averagePosition);
            newVertex.setPosition(averagePositionCopy);
            int idxNewVertex = vertices.size();
            vertices.add(newVertex);

            for (GaiaVertex vertex : cluster) {
                List<GaiaFace> faces = mapVertexToFaces.get(vertex);
                if (faces != null) {
                    for (GaiaFace face : faces) {
                        // Replace the vertex in the face with the new vertex
                        int[] indices = face.getIndices();
                        for (int j = 0; j < indices.length; j++) {
                            if (indices[j] == vertexToIndexMap.get(vertex)) {
                                indices[j] = idxNewVertex; // Replace it with new vertex index
                                break;
                            }
                        }
                    }
                }
            }
        }

        vertexToIndexMap.clear();
        mapVertexToFaces.clear();

        if (sceneMinCellIndex != null && sceneMaxCellIndex != null) {
            sceneMinCellIndex.x += 1; // to avoid boundary problems, do not delete the vertices in the boundary cells.
            sceneMinCellIndex.y += 1;
            sceneMinCellIndex.z += 1;
            sceneMaxCellIndex.x -= 1;
            sceneMaxCellIndex.y -= 1;
            sceneMaxCellIndex.z -= 1;
            reMeshParams.deleteCellAveragePositionInsideBox(sceneMinCellIndex, sceneMaxCellIndex);
        }

        // now delete degenerate faces.***
        primitives.get(0).deleteDegeneratedFaces(); // here deletes no used vertices either.
    }

    public static void reMesh(List<SceneInfo> sceneInfos, ReMeshParameters reMeshParameters, GaiaBoundingBox nodeBBox, Matrix4d nodeTMatrix, List<GaiaScene> resultGaiaScenes) {

        // Take FboManager from engine
        Matrix4d nodeMatrixInv = new Matrix4d(nodeTMatrix);
        nodeMatrixInv.invert();

        // render the scenes
        int scenesCount = sceneInfos.size();
        int counter = 0;
        Map<Vector3i, List<GaiaVertex>> vertexClusters = new HashMap<>();

        for (int i = 0; i < scenesCount; i++) {
            // load and render, one by one
            SceneInfo sceneInfo = sceneInfos.get(i);
            String scenePath = sceneInfo.getScenePath();
            Matrix4d sceneTMat = sceneInfo.getTransformMatrix();

            // must find the local position of the scene rel to node
            Vector3d scenePosWC = new Vector3d(sceneTMat.m30(), sceneTMat.m31(), sceneTMat.m32());
            Vector3d scenePosLC = nodeMatrixInv.transformPosition(scenePosWC, new Vector3d());

            // calculate the local sceneTMat
            Matrix4d sceneTMatLC = new Matrix4d();
            sceneTMatLC.identity();
            sceneTMatLC.m30(scenePosLC.x);
            sceneTMatLC.m31(scenePosLC.y);
            sceneTMatLC.m32(scenePosLC.z);

            // load the set file
            GaiaSet gaiaSet = null;
            GaiaScene gaiaScene = null;
            Path path = Paths.get(scenePath);
            try {
                gaiaSet = GaiaSet.readFile(path);
            } catch (Exception e) {
                log.error("[ERROR] reading the file: ", e);
            }

            if (gaiaSet == null) {
                log.error("[ERROR] GaiaSet is null for path: {}", scenePath);
                continue;
            }

            //**************************************************************************************************************************
            // Note: to reMesh or decimate the scene, 1- it must spend its transform matrix, 2- join all surfaces, 3- and weld vertices.
            //**************************************************************************************************************************

            gaiaScene = new GaiaScene(gaiaSet);
            gaiaScene.makeTriangularFaces();
            GaiaNode gaiaNode = gaiaScene.getNodes().get(0);
            gaiaNode.setTransformMatrix(new Matrix4d(sceneTMatLC));
            gaiaNode.setPreMultipliedTransformMatrix(new Matrix4d(sceneTMatLC));
            gaiaScene.spendTranformMatrix();
            gaiaScene.joinAllSurfaces();
            double weldError = 1e-6; // 1e-6 is a good value for remeshing
            gaiaScene.weldVertices(weldError, false, false, false, false);

            vertexClusters.clear();
            reMeshScene(gaiaScene, reMeshParameters, vertexClusters);


            gaiaSet.clear();

            resultGaiaScenes.add(gaiaScene);

            counter++;
            if (counter > 20) {
                //System.gc();
                counter = 0;
            }
        } // for each scene
    }
}
