package com.gaia3d.basic.remesher;

import com.gaia3d.basic.geometry.modifier.topology.GaiaExtractor;
import com.gaia3d.basic.geometry.modifier.topology.GaiaNormalCleaner;
import com.gaia3d.basic.model.*;
import lombok.extern.slf4j.Slf4j;

import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.*;

@Slf4j
public class ReMesherVertexCluster {
    private static Map<GaiaVertex, List<GaiaFace>> makeMapVertexToFaces(GaiaScene gaiaScene) {
        Map<GaiaVertex, List<GaiaFace>> mapVertexToFaces = new HashMap<>();
        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaPrimitive> primitives = extractor.extractAllPrimitives(gaiaScene);
        for (GaiaPrimitive primitive : primitives) {
            List<GaiaVertex> vertices = primitive.getVertices();
            List<GaiaSurface> surfaces = primitive.getSurfaces();
            for (GaiaSurface surface : surfaces) {
                List<GaiaFace> faces = surface.getFaces();
                for (GaiaFace face : faces) {
                    int[] indices = face.getIndices();
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

    public static void getVertexClusterBoundingBox(Map<Vector3i, List<GaiaVertex>> vertexClusters, Vector3i resultMinCellIndex, Vector3i resultMaxCellIndex) {
        List<Vector3i> cellIndices = vertexClusters.keySet().stream().toList();
        int cellIndicesCount = cellIndices.size();
        for (int i = 0; i < cellIndicesCount; i++) {
            Vector3i cellIndex = cellIndices.get(i);
            int currCellX = cellIndex.x;
            int currCellY = cellIndex.y;
            int currCellZ = cellIndex.z;

            if (i == 0) {
                resultMinCellIndex.x = currCellX;
                resultMinCellIndex.y = currCellY;
                resultMinCellIndex.z = currCellZ;

                resultMaxCellIndex.x = currCellX;
                resultMaxCellIndex.y = currCellY;
                resultMaxCellIndex.z = currCellZ;
                continue;
            }

            if (resultMinCellIndex.x > currCellX) resultMinCellIndex.x = currCellX;
            if (resultMinCellIndex.y > currCellY) resultMinCellIndex.y = currCellY;
            if (resultMinCellIndex.z > currCellZ) resultMinCellIndex.z = currCellZ;

            if (resultMaxCellIndex.x < currCellX) resultMaxCellIndex.x = currCellX;
            if (resultMaxCellIndex.y < currCellY) resultMaxCellIndex.y = currCellY;
            if (resultMaxCellIndex.z < currCellZ) resultMaxCellIndex.z = currCellZ;
        }
    }

    private static boolean isDegenerated(int[] idx) {
        for (int i = 0; i < idx.length; i++) {
            for (int j = i + 1; j < idx.length; j++) {
                if (idx[i] == idx[j]) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void reMeshScene(
            GaiaScene gaiaScene,
            ReMeshParameters params,
            Vector3i sceneMinCellIndex,
            Vector3i sceneMaxCellIndex
    ) {
        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaPrimitive> primitives = extractor.extractAllPrimitives(gaiaScene);

        if (primitives == null || primitives.isEmpty()) return;

        GaiaPrimitive primitive = primitives.get(0);
        List<GaiaVertex> vertices = primitive.getVertices();
        List<GaiaSurface> surfaces = primitive.getSurfaces();

        if (vertices == null || vertices.isEmpty()) return;

        CellGrid3D grid = params.getCellGrid();
        int vertexCount = vertices.size();

        // =========================================================
        // 1. Agrupar vertices por celda
        // =========================================================
        Map<Vector3i, List<Integer>> cellVertices = new HashMap<>();

        boolean first = true;

        for (int i = 0; i < vertexCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            if (vertex == null || vertex.getPosition() == null) continue;

            Vector3i cell = grid.getCellIndex(vertex.getPosition());

            // IMPORTANTE:
            // Si getCellIndex reutiliza el mismo Vector3i internamente,
            // haz copia aquí.
            Vector3i cellKey = new Vector3i(cell);

            cellVertices.computeIfAbsent(cellKey, k -> new ArrayList<>()).add(i);

            if (sceneMinCellIndex != null && sceneMaxCellIndex != null) {
                if (first) {
                    sceneMinCellIndex.set(cellKey);
                    sceneMaxCellIndex.set(cellKey);
                    first = false;
                } else {
                    if (cellKey.x < sceneMinCellIndex.x) sceneMinCellIndex.x = cellKey.x;
                    if (cellKey.y < sceneMinCellIndex.y) sceneMinCellIndex.y = cellKey.y;
                    if (cellKey.z < sceneMinCellIndex.z) sceneMinCellIndex.z = cellKey.z;

                    if (cellKey.x > sceneMaxCellIndex.x) sceneMaxCellIndex.x = cellKey.x;
                    if (cellKey.y > sceneMaxCellIndex.y) sceneMaxCellIndex.y = cellKey.y;
                    if (cellKey.z > sceneMaxCellIndex.z) sceneMaxCellIndex.z = cellKey.z;
                }
            }
        }

        // =========================================================
        // 2. Crear 1 nuevo vertice por celda
        // =========================================================
        List<GaiaVertex> newVertices = new ArrayList<>(cellVertices.size());
        int[] vertexToNew = new int[vertexCount];
        Arrays.fill(vertexToNew, -1);

        for (Map.Entry<Vector3i, List<Integer>> entry : cellVertices.entrySet()) {
            List<Integer> cluster = entry.getValue();
            if (cluster == null || cluster.isEmpty()) continue;

            Vector3d avgPos = new Vector3d();
            Vector3d avgNormal = new Vector3d();

            int validCount = 0;

            for (int vi : cluster) {
                GaiaVertex oldVertex = vertices.get(vi);
                if (oldVertex == null || oldVertex.getPosition() == null) continue;

                avgPos.add(oldVertex.getPosition());

                if (oldVertex.getNormal() != null) {
                    avgNormal.add(oldVertex.getNormal());
                }

                validCount++;
            }

            if (validCount == 0) continue;

            avgPos.div(validCount);

            if (avgNormal.lengthSquared() > 1e-12) {
                avgNormal.normalize();
            }

            GaiaVertex newVertex = new GaiaVertex();
            newVertex.setPosition(avgPos);
            newVertex.setNormal(avgNormal);

            int newIndex = newVertices.size();
            newVertices.add(newVertex);

            for (int vi : cluster) {
                vertexToNew[vi] = newIndex;
            }
        }

        // =========================================================
        // 3. Remapear faces y eliminar degeneradas
        // =========================================================
        for (GaiaSurface surface : surfaces) {
            List<GaiaFace> newFaces = new ArrayList<>();

            for (GaiaFace face : surface.getFaces()) {
                int[] oldIdx = face.getIndices();
                if (oldIdx == null || oldIdx.length < 3) continue;

                int[] newIdx = new int[oldIdx.length];

                boolean invalid = false;
                for (int i = 0; i < oldIdx.length; i++) {
                    int oldIndex = oldIdx[i];

                    if (oldIndex < 0 || oldIndex >= vertexToNew.length) {
                        invalid = true;
                        break;
                    }

                    int newIndex = vertexToNew[oldIndex];

                    if (newIndex < 0) {
                        invalid = true;
                        break;
                    }

                    newIdx[i] = newIndex;
                }

                if (invalid) continue;

                if (isDegenerated(newIdx)) {
                    continue;
                }

                face.setIndices(newIdx);
                newFaces.add(face);
            }

            surface.setFaces(newFaces);
        }

        // =========================================================
        // 4. Reemplazar vertices y limpieza final
        // =========================================================
        primitive.setVertices(newVertices);

        if (sceneMinCellIndex != null && sceneMaxCellIndex != null) {
            sceneMinCellIndex.x += 1;
            sceneMinCellIndex.y += 1;
            sceneMinCellIndex.z += 1;

            sceneMaxCellIndex.x -= 1;
            sceneMaxCellIndex.y -= 1;
            sceneMaxCellIndex.z -= 1;

            params.deleteCellAveragePositionInsideBox(sceneMinCellIndex, sceneMaxCellIndex);
        }

        primitive.deleteDegeneratedFaces();

            // Delete normals.
        GaiaNormalCleaner normalCleaner = new GaiaNormalCleaner();
        normalCleaner.apply(gaiaScene);
    }


//    // Delete normals.
//    GaiaNormalCleaner normalCleaner = new GaiaNormalCleaner();
//    normalCleaner.apply(gaiaScene);

    public static void reMeshScene_original(GaiaScene gaiaScene,
                                   ReMeshParameters reMeshParams,
                                   Map<Vector3i, List<GaiaVertex>> vertexClusters,
                                   Vector3i sceneMinCellIndex,
                                   Vector3i sceneMaxCellIndex) {
        //************************************************************************************
        // Note: the gaiaScene must spend its transform matrix before calling this method*
        // Note: the gaiaScene must join all surfaces before calling this method**********
        //************************************************************************************
        Map<GaiaVertex, List<GaiaFace>> mapVertexToFaces = makeMapVertexToFaces(gaiaScene);
        Map<GaiaVertex, Integer> vertexToIndexMap = new HashMap<>();

        //GaiaBoundingBox originalBBox = gaiaScene.updateBoundingBox();

        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaPrimitive> primitives = extractor.extractAllPrimitives(gaiaScene);
        // There are only 1 primitive in the gaiaScene, so we can use it directly.
        List<GaiaVertex> vertices = primitives.get(0).getVertices();

        CellGrid3D cellGrid = reMeshParams.getCellGrid();
        Map<Vector3i, Vector3d> cellAveragePositions = reMeshParams.getCellAveragePositions();

        // 1rs, make map of vertex to index
        for (int i = 0; i < vertices.size(); i++) {
            GaiaVertex vertex = vertices.get(i);
            vertexToIndexMap.put(vertex, i);
        }

        boolean firstVertex = true;
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

                        if (firstVertex) {
                            sceneMinCellIndex.x = currCellX;
                            sceneMinCellIndex.y = currCellY;
                            sceneMinCellIndex.z = currCellZ;
                            sceneMaxCellIndex.x = currCellX;
                            sceneMaxCellIndex.y = currCellY;
                            sceneMaxCellIndex.z = currCellZ;
                            firstVertex = false;
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
            Vector3d averagePosition = cellAveragePositions.get(cellIndex); // original
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

        // now delete degenerate faces
        GaiaPrimitive primitive = primitives.get(0);
        primitive.deleteDegeneratedFaces(); // here deletes no used vertices either.
    }

    public static void reMeshSceneByFaces(GaiaScene gaiaScene,
                                            ReMeshParameters reMeshParams,
                                            Map<Vector3i, List<GaiaVertex>> vertexClusters,
                                            Vector3i sceneMinCellIndex,
                                            Vector3i sceneMaxCellIndex) {
        //************************************************************************************
        // Note: the gaiaScene must spend its transform matrix before calling this method*
        // Note: the gaiaScene must join all surfaces before calling this method**********
        //************************************************************************************
        Map<GaiaVertex, List<GaiaFace>> mapVertexToFaces = makeMapVertexToFaces(gaiaScene);
        Map<GaiaVertex, Integer> vertexToIndexMap = new HashMap<>();

        //GaiaBoundingBox originalBBox = gaiaScene.updateBoundingBox();

        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaPrimitive> primitives = extractor.extractAllPrimitives(gaiaScene);
        // There are only 1 primitive in the gaiaScene, so we can use it directly.
        List<GaiaVertex> vertices = primitives.get(0).getVertices();

        CellGrid3D cellGrid = reMeshParams.getCellGrid();
        Map<Vector3i, Vector3d> cellAveragePositions = reMeshParams.getCellAveragePositions();

        // 1rs, make map of vertex to index
        for (int i = 0; i < vertices.size(); i++) {
            GaiaVertex vertex = vertices.get(i);
            vertexToIndexMap.put(vertex, i);
        }

        boolean firstVertex = true;
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

                        if (firstVertex) {
                            sceneMinCellIndex.x = currCellX;
                            sceneMinCellIndex.y = currCellY;
                            sceneMinCellIndex.z = currCellZ;
                            sceneMaxCellIndex.x = currCellX;
                            sceneMaxCellIndex.y = currCellY;
                            sceneMaxCellIndex.z = currCellZ;
                            firstVertex = false;
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
            Vector3d averagePosition = cellAveragePositions.get(cellIndex); // original
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

        // now delete degenerate faces
        GaiaPrimitive primitive = primitives.get(0);
        primitive.deleteDegeneratedFaces(); // here deletes no used vertices either.
    }
}
