package com.gaia3d.basic.remesher;

import com.gaia3d.basic.geometry.modifier.topology.GaiaExtractor;
import com.gaia3d.basic.geometry.modifier.topology.GaiaNormalCleaner;
import com.gaia3d.basic.model.*;
import lombok.extern.slf4j.Slf4j;

import org.joml.Vector2d;
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


//    // Delete normals.
//    GaiaNormalCleaner normalCleaner = new GaiaNormalCleaner();
//    normalCleaner.apply(gaiaScene);

    public static void reMeshScene(
            GaiaScene gaiaScene,
            ReMeshParameters reMeshParams,
            WorldVertexClusters worldClusters,
            Vector3i sceneMinCellIndex,
            Vector3i sceneMaxCellIndex) {

        //************************************************************************************
        // Note: the gaiaScene must spend its transform matrix before calling this method
        // Note: the gaiaScene must join all surfaces before calling this method
        //************************************************************************************

        if (gaiaScene == null || reMeshParams == null || worldClusters == null) {
            return;
        }

        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaPrimitive> primitives = extractor.extractAllPrimitives(gaiaScene);

        if (primitives == null || primitives.isEmpty()) {
            return;
        }

        // En tu caso normalmente hay solo 1 primitive.
        GaiaPrimitive primitive = primitives.get(0);
        List<GaiaVertex> vertices = primitive.getVertices();

        if (vertices == null || vertices.isEmpty()) {
            return;
        }

        int originalVertexCount = vertices.size();

        CellGrid3D cellGrid = reMeshParams.getCellGrid();
        if (cellGrid == null) {
            return;
        }

        //************************************************************************************
        // 1) Detectar vertices frontera.
        //************************************************************************************
        List<GaiaFace> allFaces = primitive.extractGaiaAllFaces(null);

        GaiaFrontierFinder frontierFinder = new GaiaFrontierFinder();

        int[] weldedIndices = new int[originalVertexCount];

        boolean[] frontierVertex = frontierFinder.findBoundaryVertices(
                vertices,
                allFaces,
                1e-6,
                weldedIndices
        );

        if (frontierVertex == null || frontierVertex.length < originalVertexCount) {
            return;
        }

        //************************************************************************************
        // 2) Mapas auxiliares.
        //************************************************************************************
        Map<GaiaVertex, List<GaiaFace>> mapVertexToFaces = makeMapVertexToFaces(gaiaScene);
        Map<GaiaVertex, Integer> vertexToIndexMap = new HashMap<>();

        for (int i = 0; i < originalVertexCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            vertexToIndexMap.put(vertex, i);
        }

        //************************************************************************************
        // 3) Limpiar clusters interiores de esta scene.
        //    OJO: no limpio frontierClusters, porque pueden venir del LOD2 / mundo.
        //************************************************************************************
        worldClusters.clearInteriorClusters();

        //************************************************************************************
        // 4) Construir interiorClusters y frontierClusters.
        //
        // Importante:
        // - Recorremos vertices, NO caras.
        // - Así evitamos meter el mismo vertex muchas veces en el cluster.
        // - En tu función original se recorrían las caras, por lo que un vertex podía entrar
        //   repetido varias veces si pertenecía a varias caras.
        //************************************************************************************
        boolean firstVertex = true;

        for (int i = 0; i < originalVertexCount; i++) {
            GaiaVertex vertex = vertices.get(i);

            if (vertex == null || vertex.getPosition() == null) {
                continue;
            }

            Vector3d position = vertex.getPosition();

            // Copia defensiva del Vector3i.
            Vector3i cellIndex = new Vector3i(cellGrid.getCellIndex(position));

            if (frontierVertex[i]) {
                worldClusters.frontierClusters
                        .computeIfAbsent(new Vector3i(cellIndex), k -> new ArrayList<>())
                        .add(vertex);
            } else {
                worldClusters.interiorClusters
                        .computeIfAbsent(new Vector3i(cellIndex), k -> new ArrayList<>())
                        .add(vertex);
            }

            // update scene min and max cell index
            int currCellX = cellIndex.x;
            int currCellY = cellIndex.y;
            int currCellZ = cellIndex.z;

            if (sceneMinCellIndex != null && sceneMaxCellIndex != null) {
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

        //************************************************************************************
        // 5) Calcular averages.
        //
        // Si tus frontierClusters vienen ya preparados desde LOD2, quizá NO quieras
        // recalcular frontierAveragePositions aquí.
        //
        // En esta versión calculo ambos, porque estamos transformando la función original.
        //************************************************************************************
        worldClusters.recalculateAveragePositions();

        //************************************************************************************
        // 6) Crear nuevos vertices para clusters interiores.
        //************************************************************************************
        Map<Vector3i, Integer> cellIndexToNewInteriorVertexIndex = new HashMap<>();
        Map<Vector3i, Integer> cellIndexToNewFrontierVertexIndex = new HashMap<>();

        int createdInteriorVertices = 0;
        int createdFrontierVertices = 0;
        int replacedInteriorVertices = 0;
        int replacedFrontierVertices = 0;

        for (Map.Entry<Vector3i, List<GaiaVertex>> entry : worldClusters.interiorClusters.entrySet()) {
            Vector3i cellIndex = entry.getKey();
            List<GaiaVertex> cluster = entry.getValue();

            if (cellIndex == null || cluster == null || cluster.size() < 2) {
                continue;
            }

            Vector3d averagePosition = worldClusters.interiorAveragePositions.get(cellIndex);

            if (averagePosition == null) {
                averagePosition = calculateAveragePosition(cluster);
                worldClusters.interiorAveragePositions.put(new Vector3i(cellIndex), averagePosition);
            }

            if (averagePosition == null) {
                continue;
            }

            GaiaVertex newVertex = new GaiaVertex();
            newVertex.setPosition(new Vector3d(averagePosition));

            // Copiar atributos desde el primer vertex válido del cluster.
            GaiaVertex srcVertex = findFirstValidVertex(cluster);
            if (srcVertex != null) {
                copyVertexAttributes(srcVertex, newVertex);
            }

            int idxNewVertex = vertices.size();
            vertices.add(newVertex);
            createdInteriorVertices++;

            cellIndexToNewInteriorVertexIndex.put(new Vector3i(cellIndex), idxNewVertex);

            for (GaiaVertex vertex : cluster) {
                Integer oldIndex = vertexToIndexMap.get(vertex);

                if (oldIndex == null) {
                    continue;
                }

                List<GaiaFace> faces = mapVertexToFaces.get(vertex);

                if (faces == null) {
                    continue;
                }

                for (GaiaFace face : faces) {
                    if (face == null || face.getIndices() == null) {
                        continue;
                    }

                    int[] indices = face.getIndices();

                    for (int j = 0; j < indices.length; j++) {
                        if (indices[j] == oldIndex) {
                            indices[j] = idxNewVertex;
                            replacedInteriorVertices++;
                            break;
                        }
                    }
                }
            }
        }

        //************************************************************************************
        // 7) Crear nuevos vertices para clusters frontera.
        //
        // Aquí está la parte nueva importante:
        // los vertices frontera usan frontierAverage, separado del interiorAverage.
        //************************************************************************************
        for (Map.Entry<Vector3i, List<GaiaVertex>> entry : worldClusters.frontierClusters.entrySet()) {
            Vector3i cellIndex = entry.getKey();
            List<GaiaVertex> cluster = entry.getValue();

            if (cellIndex == null || cluster == null || cluster.size() < 2) {
                continue;
            }

            Vector3d averagePosition = worldClusters.frontierAveragePositions.get(cellIndex);

            if (averagePosition == null) {
                averagePosition = calculateAveragePosition(cluster);
                worldClusters.frontierAveragePositions.put(new Vector3i(cellIndex), averagePosition);
            }

            if (averagePosition == null) {
                continue;
            }

            GaiaVertex newVertex = new GaiaVertex();
            newVertex.setPosition(new Vector3d(averagePosition));

            GaiaVertex srcVertex = findFirstValidVertex(cluster);
            if (srcVertex != null) {
                copyVertexAttributes(srcVertex, newVertex);
            }

            int idxNewVertex = vertices.size();
            vertices.add(newVertex);
            createdFrontierVertices++;

            cellIndexToNewFrontierVertexIndex.put(new Vector3i(cellIndex), idxNewVertex);

            for (GaiaVertex vertex : cluster) {
                Integer oldIndex = vertexToIndexMap.get(vertex);

                if (oldIndex == null) {
                    continue;
                }

                List<GaiaFace> faces = mapVertexToFaces.get(vertex);

                if (faces == null) {
                    continue;
                }

                for (GaiaFace face : faces) {
                    if (face == null || face.getIndices() == null) {
                        continue;
                    }

                    int[] indices = face.getIndices();

                    for (int j = 0; j < indices.length; j++) {
                        if (indices[j] == oldIndex) {
                            indices[j] = idxNewVertex;
                            replacedFrontierVertices++;
                            break;
                        }
                    }
                }
            }
        }

        log.debug("reMeshScene_original_withFrontierClusters originalVertexCount = {}", originalVertexCount);
        log.debug("reMeshScene_original_withFrontierClusters interiorClusters = {}", worldClusters.interiorClusters.size());
        log.debug("reMeshScene_original_withFrontierClusters frontierClusters = {}", worldClusters.frontierClusters.size());
        log.debug("reMeshScene_original_withFrontierClusters interiorAveragePositions = {}", worldClusters.interiorAveragePositions.size());
        log.debug("reMeshScene_original_withFrontierClusters frontierAveragePositions = {}", worldClusters.frontierAveragePositions.size());
        log.debug("reMeshScene_original_withFrontierClusters createdInteriorVertices = {}", createdInteriorVertices);
        log.debug("reMeshScene_original_withFrontierClusters createdFrontierVertices = {}", createdFrontierVertices);
        log.debug("reMeshScene_original_withFrontierClusters replacedInteriorVertices = {}", replacedInteriorVertices);
        log.debug("reMeshScene_original_withFrontierClusters replacedFrontierVertices = {}", replacedFrontierVertices);

        //************************************************************************************
        // 8) Limpiar mapas auxiliares.
        //************************************************************************************
        vertexToIndexMap.clear();
        mapVertexToFaces.clear();

        cellIndexToNewInteriorVertexIndex.clear();
        cellIndexToNewFrontierVertexIndex.clear();

        //************************************************************************************
        // 9) Borrar averages internos si quieres mantener la lógica antigua.
        //
        // OJO:
        // Si frontierAveragePositions son anchors globales LOD2 -> LOD3,
        // NO deberías borrarlos aquí.
        //************************************************************************************
        if (sceneMinCellIndex != null && sceneMaxCellIndex != null && !firstVertex) {
            Vector3i minInside = new Vector3i(sceneMinCellIndex);
            Vector3i maxInside = new Vector3i(sceneMaxCellIndex);

            minInside.x += 1;
            minInside.y += 1;
            minInside.z += 1;

            maxInside.x -= 1;
            maxInside.y -= 1;
            maxInside.z -= 1;

            reMeshParams.deleteCellAveragePositionInsideBox(minInside, maxInside);
        }

        //************************************************************************************
        // 10) Borrar caras degeneradas y vertices no usados.
        //************************************************************************************
        primitive.deleteDegeneratedFaces();
    }

    private static Vector3d calculateAveragePosition(List<GaiaVertex> cluster) {
        if (cluster == null || cluster.isEmpty()) {
            return null;
        }

        Vector3d averagePosition = new Vector3d();
        int count = 0;

        for (GaiaVertex vertex : cluster) {
            if (vertex == null || vertex.getPosition() == null) {
                continue;
            }

            averagePosition.add(vertex.getPosition());
            count++;
        }

        if (count == 0) {
            return null;
        }

        averagePosition.div(count);

        return averagePosition;
    }

    private static GaiaVertex findFirstValidVertex(List<GaiaVertex> vertices) {
        if (vertices == null) {
            return null;
        }

        for (GaiaVertex vertex : vertices) {
            if (vertex != null && vertex.getPosition() != null) {
                return vertex;
            }
        }

        return null;
    }

    private static void copyVertexAttributes(GaiaVertex src, GaiaVertex dst) {
        if (src == null || dst == null) {
            return;
        }

        if (src.getNormal() != null) {
            dst.setNormal(new Vector3d(src.getNormal()));
        }

        if (src.getTexcoords() != null) {
            dst.setTexcoords(new Vector2d(src.getTexcoords()));
        }

        if (src.getColor() != null) {
            dst.setColor(src.getColor().clone());
        }

        dst.setBatchId(src.getBatchId());
    }

    private static double getFrontierInfluenceDistance(
            ReMeshParameters reMeshParams,
            CellGrid3D cellGrid) {

        double cellSize = 1.0;

        // Opción A: si ReMeshParameters tiene cellSize.
        // cellSize = reMeshParams.getCellSize();

        // Opción B: si CellGrid3D tiene cellSize.
        // cellSize = cellGrid.getCellSize();

        // De momento, si no tienes getter, puedes cambiar este valor manualmente.
        cellSize = reMeshParams.getCellGrid().getCellSize();

        return cellSize * 0.25;
    }

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
