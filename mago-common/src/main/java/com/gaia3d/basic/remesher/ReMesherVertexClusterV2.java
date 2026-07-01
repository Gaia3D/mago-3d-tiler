package com.gaia3d.basic.remesher;

import com.gaia3d.basic.geometry.modifier.topology.GaiaExtractor;
import com.gaia3d.basic.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ReMesherVertexClusterV2 {
    /**
     * PASADA 1:
     * <p>
     * Acumula anchors de frontera del tile.
     * <p>
     * Llamar una vez por cada mesh/scene del tile.
     * No remeshea nada.
     * Solo guarda posiciones de frontera en TileBoundaryAnchors.
     */
    public static void accumulateTileBoundaryAnchorsFromScene(
            GaiaScene scene,
            ReMeshParameters reMeshParams,
            TileBoundaryAnchors tileBoundaryAnchors,
            GlobalBoundaryAnchors globalBoundaryAnchors,
            int meshId) {

        if (scene == null || reMeshParams == null || tileBoundaryAnchors == null) {
            return;
        }

        CellGrid3D cellGrid = reMeshParams.getCellGrid();

        if (cellGrid == null) {
            return;
        }

        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaPrimitive> primitives = extractor.extractAllPrimitives(scene);

        if (primitives == null || primitives.isEmpty()) {
            return;
        }

        GaiaFrontierFinder frontierFinder = new GaiaFrontierFinder();

        int totalFrontierVertices = 0;
        int addedLocalFrontierVertices = 0;
        int reusedGlobalAnchors = 0;

        for (GaiaPrimitive primitive : primitives) {
            if (primitive == null) {
                continue;
            }

            List<GaiaVertex> vertices = primitive.getVertices();

            if (vertices == null || vertices.isEmpty()) {
                continue;
            }

            int vertexCount = vertices.size();

            List<GaiaFace> faces = primitive.extractGaiaAllFaces(null);

            if (faces == null || faces.isEmpty()) {
                continue;
            }

            int[] weldedIndices = new int[vertexCount];

            boolean[] frontierVertex = frontierFinder.findBoundaryVertices(
                    vertices,
                    faces,
                    1e-6,
                    weldedIndices
            );

            if (frontierVertex == null || frontierVertex.length < vertexCount) {
                continue;
            }

            for (int i = 0; i < vertexCount; i++) {
                if (!frontierVertex[i]) {
                    continue;
                }

                GaiaVertex vertex = vertices.get(i);

                if (vertex == null || vertex.getPosition() == null) {
                    continue;
                }

                Vector3i cellIndex = new Vector3i(
                        cellGrid.getCellIndex(vertex.getPosition())
                );

                totalFrontierVertices++;

                // Si ya existe un anchor global bloqueado para esta celda,
                // este tile debe usarlo, no modificarlo.
                if (globalBoundaryAnchors != null &&
                        globalBoundaryAnchors.hasAverage(cellIndex)) {
                    reusedGlobalAnchors++;
                    continue;
                }

                // Si no existe global, este tile contribuye a crear el anchor local.
                tileBoundaryAnchors.addFrontierPosition(
                        cellIndex,
                        vertex.getPosition(),
                        meshId
                );

                addedLocalFrontierVertices++;
            }
        }

        log.debug("V2 accumulateTileBoundaryAnchorsFromScene meshId = {}", meshId);
        log.debug("V2 total frontier vertices = {}", totalFrontierVertices);
        log.debug("V2 added local frontier vertices = {}", addedLocalFrontierVertices);
        log.debug("V2 reused global anchors = {}", reusedGlobalAnchors);
        log.debug("V2 local frontierPositionClusters size = {}", tileBoundaryAnchors.frontierPositionClusters.size());
        if (globalBoundaryAnchors != null) {
            log.debug("V2 global lockedAveragePositions size = {}", globalBoundaryAnchors.lockedAveragePositions.size());
        }
    }

    /**
     * Después de llamar a accumulateTileBoundaryAnchorsFromScene(...) para todos los meshes
     * del tile, llamar a esta función una sola vez.
     */
    public static void finishTileBoundaryAnchors(TileBoundaryAnchors tileBoundaryAnchors) {
        if (tileBoundaryAnchors == null) {
            return;
        }

        tileBoundaryAnchors.calculateAveragePositions();

        log.debug("V2 tile frontierPositionClusters = {}", tileBoundaryAnchors.frontierPositionClusters.size());
        log.debug("V2 tile frontierAveragePositions = {}", tileBoundaryAnchors.frontierAveragePositions.size());
        log.debug("V2 tile cornerLikeCells = {}", tileBoundaryAnchors.countCornerLikeCells());
    }

    /**
     * PASADA 2:
     * <p>
     * Remeshea una scene/mesh usando:
     * <p>
     * - frontera global del tile: TileBoundaryAnchors
     * - interiores calculados al vuelo dentro de esta función
     */
    public static void reMeshScene_original(
            GaiaScene scene,
            ReMeshParameters reMeshParams,
            TileBoundaryAnchors tileBoundaryAnchors,
            GlobalBoundaryAnchors globalBoundaryAnchors,
            Vector3i sceneMinCellIndex,
            Vector3i sceneMaxCellIndex) {

        if (scene == null || reMeshParams == null || tileBoundaryAnchors == null) {
            return;
        }

        CellGrid3D cellGrid = reMeshParams.getCellGrid();

        if (cellGrid == null) {
            return;
        }

        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaPrimitive> primitives = extractor.extractAllPrimitives(scene);

        if (primitives == null || primitives.isEmpty()) {
            return;
        }

        // Si tienes varias primitives por scene, puedes adaptar esto a un for.
        // Mantengo tu caso habitual: 1 primitive por scene.
        GaiaPrimitive primitive = primitives.get(0);

        if (primitive == null) {
            return;
        }

        List<GaiaVertex> vertices = primitive.getVertices();

        if (vertices == null || vertices.isEmpty()) {
            return;
        }

        int originalVertexCount = vertices.size();

        List<GaiaFace> faces = primitive.extractGaiaAllFaces(null);

        if (faces == null || faces.isEmpty()) {
            return;
        }

        GaiaFrontierFinder frontierFinder = new GaiaFrontierFinder();

        int[] weldedIndices = new int[originalVertexCount];

        boolean[] frontierVertex = frontierFinder.findBoundaryVertices(
                vertices,
                faces,
                1e-6,
                weldedIndices
        );

        if (frontierVertex == null || frontierVertex.length < originalVertexCount) {
            return;
        }

        updateSceneCellBounds(
                vertices,
                originalVertexCount,
                cellGrid,
                sceneMinCellIndex,
                sceneMaxCellIndex
        );

        //**************************************************************************
        // 1) Construir clusters interiores locales al vuelo.
        //    Guardamos índices viejos, no GaiaVertex, para ahorrar memoria y facilitar
        //    oldIndex -> newIndex.
        //**************************************************************************
        Map<Vector3i, List<Integer>> interiorCellToOldIndices = new HashMap<>();

        int frontierOriginalVertices = 0;
        int interiorOriginalVertices = 0;

        for (int oldIndex = 0; oldIndex < originalVertexCount; oldIndex++) {
            GaiaVertex vertex = vertices.get(oldIndex);

            if (vertex == null || vertex.getPosition() == null) {
                continue;
            }

            if (frontierVertex[oldIndex]) {
                frontierOriginalVertices++;
                continue;
            }

            interiorOriginalVertices++;

            Vector3i cellIndex = new Vector3i(
                    cellGrid.getCellIndex(vertex.getPosition())
            );

            interiorCellToOldIndices
                    .computeIfAbsent(new Vector3i(cellIndex), k -> new ArrayList<>())
                    .add(oldIndex);
        }

        //**************************************************************************
        // 2) Calcular averages interiores locales.
        //**************************************************************************
        Map<Vector3i, Vector3d> interiorAveragePositions =
                calculateInteriorAveragePositions(vertices, interiorCellToOldIndices);

        //**************************************************************************
        // 3) Crear oldIndex -> newIndex.
        //**************************************************************************
        Map<Integer, Integer> oldIndexToNewIndex = new HashMap<>();

        Map<Vector3i, Integer> cellToNewInteriorIndex = new HashMap<>();
        Map<Vector3i, Integer> cellToNewFrontierIndex = new HashMap<>();

        int createdInteriorVertices = 0;
        int createdFrontierVertices = 0;
        int mappedInteriorVertices = 0;
        int mappedFrontierVertices = 0;
        int skippedNoAverage = 0;
        int skippedSingleInteriorCluster = 0;

        for (int oldIndex = 0; oldIndex < originalVertexCount; oldIndex++) {
            GaiaVertex oldVertex = vertices.get(oldIndex);

            if (oldVertex == null || oldVertex.getPosition() == null) {
                continue;
            }

            Vector3i cellIndex = new Vector3i(
                    cellGrid.getCellIndex(oldVertex.getPosition())
            );

            boolean isFrontier = frontierVertex[oldIndex];

            Vector3d targetAverage;
            Map<Vector3i, Integer> cellToNewIndex;

            if (isFrontier) {
                targetAverage = null;

                if (globalBoundaryAnchors != null) {
                    targetAverage = globalBoundaryAnchors.getAverage(cellIndex);
                }

                if (targetAverage == null) {
                    targetAverage = tileBoundaryAnchors.getFrontierAverage(cellIndex);
                }

                cellToNewIndex = cellToNewFrontierIndex;

                if (targetAverage == null) {
                    skippedNoAverage++;
                    continue;
                }
            } else {
                List<Integer> cluster = interiorCellToOldIndices.get(cellIndex);

                if (cluster == null || cluster.size() < 2) {
                    skippedSingleInteriorCluster++;
                    continue;
                }

                targetAverage = interiorAveragePositions.get(cellIndex);
                cellToNewIndex = cellToNewInteriorIndex;

                if (targetAverage == null) {
                    skippedNoAverage++;
                    continue;
                }
            }

            Integer newIndex = cellToNewIndex.get(cellIndex);

            if (newIndex == null) {
                GaiaVertex newVertex = new GaiaVertex();
                newVertex.setPosition(new Vector3d(targetAverage));

                copyVertexAttributes(oldVertex, newVertex);

                newIndex = vertices.size();
                vertices.add(newVertex);

                cellToNewIndex.put(new Vector3i(cellIndex), newIndex);

                if (isFrontier) {
                    createdFrontierVertices++;
                } else {
                    createdInteriorVertices++;
                }
            }

            oldIndexToNewIndex.put(oldIndex, newIndex);

            if (isFrontier) {
                mappedFrontierVertices++;
            } else {
                mappedInteriorVertices++;
            }
        }

        //**************************************************************************
        // 4) Sustituir índices en las caras.
        //**************************************************************************
        replaceFaceIndices(primitive, oldIndexToNewIndex);

        log.debug("V2 reMeshScene originalVertexCount = {}", originalVertexCount);
        log.debug("V2 reMeshScene frontierOriginalVertices = {}", frontierOriginalVertices);
        log.debug("V2 reMeshScene interiorOriginalVertices = {}", interiorOriginalVertices);
        log.debug("V2 reMeshScene interiorCells = {}", interiorCellToOldIndices.size());
        log.debug("V2 reMeshScene interiorAveragePositions = {}", interiorAveragePositions.size());
        log.debug("V2 reMeshScene tile frontierAveragePositions = {}", tileBoundaryAnchors.frontierAveragePositions.size());
        log.debug("V2 reMeshScene createdInteriorVertices = {}", createdInteriorVertices);
        log.debug("V2 reMeshScene createdFrontierVertices = {}", createdFrontierVertices);
        log.debug("V2 reMeshScene mappedInteriorVertices = {}", mappedInteriorVertices);
        log.debug("V2 reMeshScene mappedFrontierVertices = {}", mappedFrontierVertices);
        log.debug("V2 reMeshScene skippedNoAverage = {}", skippedNoAverage);
        log.debug("V2 reMeshScene skippedSingleInteriorCluster = {}", skippedSingleInteriorCluster);
        log.debug("V2 reMeshScene oldIndexToNewIndex size = {}", oldIndexToNewIndex.size());

        //**************************************************************************
        // 5) Borrar caras degeneradas y vértices no usados.
        //**************************************************************************
        primitive.deleteDegeneratedFaces();

        //**************************************************************************
        // 6) Limpiar temporales.
        //**************************************************************************
        oldIndexToNewIndex.clear();
        cellToNewInteriorIndex.clear();
        cellToNewFrontierIndex.clear();
        interiorCellToOldIndices.clear();
        interiorAveragePositions.clear();
    }

    public static void reMeshScene(
            GaiaScene scene,
            ReMeshParameters reMeshParams,
            Vector3i sceneMinCellIndex,
            Vector3i sceneMaxCellIndex) {

        if (scene == null || reMeshParams == null) {
            return;
        }

        CellGrid3D cellGrid = reMeshParams.getCellGrid();

        if (cellGrid == null) {
            return;
        }

        GlobalBoundaryAnchors globalBoundaryAnchors =
                reMeshParams.getGlobalBoundaryAnchors();

        if(globalBoundaryAnchors == null) {
            // In the last LODs, is possible that there are no GlobalBoundaryAnchors because there are no cut meshes.
            globalBoundaryAnchors = new GlobalBoundaryAnchors();
        }

        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaPrimitive> primitives = extractor.extractAllPrimitives(scene);

        if (primitives == null || primitives.isEmpty()) {
            return;
        }

        // Si tienes varias primitives por scene, puedes adaptar esto a un for.
        // Mantengo tu caso habitual: 1 primitive por scene.
        GaiaPrimitive primitive = primitives.get(0);

        if (primitive == null) {
            return;
        }

        List<GaiaVertex> vertices = primitive.getVertices();

        if (vertices == null || vertices.isEmpty()) {
            return;
        }

        int originalVertexCount = vertices.size();

        List<GaiaFace> faces = primitive.extractGaiaAllFaces(null);

        if (faces == null || faces.isEmpty()) {
            return;
        }

        GaiaFrontierFinder frontierFinder = new GaiaFrontierFinder();

        int[] weldedIndices = new int[originalVertexCount];

        boolean[] frontierVertex = frontierFinder.findBoundaryVertices(
                vertices,
                faces,
                1e-6,
                weldedIndices
        );

        if (frontierVertex == null || frontierVertex.length < originalVertexCount) {
            return;
        }

        updateSceneCellBounds(
                vertices,
                originalVertexCount,
                cellGrid,
                sceneMinCellIndex,
                sceneMaxCellIndex
        );

        //**************************************************************************
        // 1) Construir clusters interiores locales al vuelo.
        //    Guardamos índices viejos, no GaiaVertex, para ahorrar memoria y facilitar
        //    oldIndex -> newIndex.
        //**************************************************************************
        Map<Vector3i, List<Integer>> interiorCellToOldIndices = new HashMap<>();

        int frontierOriginalVertices = 0;
        int interiorOriginalVertices = 0;

        for (int oldIndex = 0; oldIndex < originalVertexCount; oldIndex++) {
            GaiaVertex vertex = vertices.get(oldIndex);

            if (vertex == null || vertex.getPosition() == null) {
                continue;
            }

            if (frontierVertex[oldIndex]) {
                frontierOriginalVertices++;
                continue;
            }

            interiorOriginalVertices++;

            Vector3i cellIndex = new Vector3i(
                    cellGrid.getCellIndex(vertex.getPosition())
            );

            interiorCellToOldIndices
                    .computeIfAbsent(new Vector3i(cellIndex), k -> new ArrayList<>())
                    .add(oldIndex);
        }

        //**************************************************************************
        // 2) Calcular averages interiores locales.
        //**************************************************************************
        Map<Vector3i, Vector3d> interiorAveragePositions =
                calculateInteriorAveragePositions(vertices, interiorCellToOldIndices);

        //**************************************************************************
        // 3) Crear oldIndex -> newIndex.
        //**************************************************************************
        Map<Integer, Integer> oldIndexToNewIndex = new HashMap<>();

        Map<Vector3i, Integer> cellToNewInteriorIndex = new HashMap<>();
        Map<Vector3i, Integer> cellToNewFrontierIndex = new HashMap<>();

        int createdInteriorVertices = 0;
        int createdFrontierVertices = 0;
        int mappedInteriorVertices = 0;
        int mappedFrontierVertices = 0;
        int skippedNoAverage = 0;
        int skippedSingleInteriorCluster = 0;

        for (int oldIndex = 0; oldIndex < originalVertexCount; oldIndex++) {
            GaiaVertex oldVertex = vertices.get(oldIndex);

            if (oldVertex == null || oldVertex.getPosition() == null) {
                continue;
            }

            Vector3i cellIndex = new Vector3i(
                    cellGrid.getCellIndex(oldVertex.getPosition())
            );

            boolean isFrontier = frontierVertex[oldIndex];

            Vector3d targetAverage;
            Map<Vector3i, Integer> cellToNewIndex;

            if (isFrontier) {
                targetAverage =
                        globalBoundaryAnchors.getAverage(
                                cellIndex
                        );

                cellToNewIndex =
                        cellToNewFrontierIndex;

                if (targetAverage == null) {
                    skippedNoAverage++;
                    continue;
                }
            } else {
                List<Integer> cluster = interiorCellToOldIndices.get(cellIndex);

                if (cluster == null || cluster.size() < 2) {
                    skippedSingleInteriorCluster++;
                    continue;
                }

                targetAverage = interiorAveragePositions.get(cellIndex);
                cellToNewIndex = cellToNewInteriorIndex;

                if (targetAverage == null) {
                    skippedNoAverage++;
                    continue;
                }
            }

            Integer newIndex = cellToNewIndex.get(cellIndex);

            if (newIndex == null) {
                GaiaVertex newVertex = new GaiaVertex();
                newVertex.setPosition(new Vector3d(targetAverage));

                copyVertexAttributes(oldVertex, newVertex);

                newIndex = vertices.size();
                vertices.add(newVertex);

                cellToNewIndex.put(new Vector3i(cellIndex), newIndex);

                if (isFrontier) {
                    createdFrontierVertices++;
                } else {
                    createdInteriorVertices++;
                }
            }

            oldIndexToNewIndex.put(oldIndex, newIndex);

            if (isFrontier) {
                mappedFrontierVertices++;
            } else {
                mappedInteriorVertices++;
            }
        }

        //**************************************************************************
        // 4) Sustituir índices en las caras.
        //**************************************************************************
        replaceFaceIndices(primitive, oldIndexToNewIndex);

        log.debug("V2 reMeshScene originalVertexCount = {}", originalVertexCount);
        log.debug("V2 reMeshScene frontierOriginalVertices = {}", frontierOriginalVertices);
        log.debug("V2 reMeshScene interiorOriginalVertices = {}", interiorOriginalVertices);
        log.debug("V2 reMeshScene interiorCells = {}", interiorCellToOldIndices.size());
        log.debug("V2 reMeshScene interiorAveragePositions = {}", interiorAveragePositions.size());
        log.debug("V2 reMeshScene createdInteriorVertices = {}", createdInteriorVertices);
        log.debug("V2 reMeshScene createdFrontierVertices = {}", createdFrontierVertices);
        log.debug("V2 reMeshScene mappedInteriorVertices = {}", mappedInteriorVertices);
        log.debug("V2 reMeshScene mappedFrontierVertices = {}", mappedFrontierVertices);
        log.debug("V2 reMeshScene skippedNoAverage = {}", skippedNoAverage);
        log.debug("V2 reMeshScene skippedSingleInteriorCluster = {}", skippedSingleInteriorCluster);
        log.debug("V2 reMeshScene oldIndexToNewIndex size = {}", oldIndexToNewIndex.size());

        //**************************************************************************
        // 5) Borrar caras degeneradas y vértices no usados.
        //**************************************************************************
        primitive.deleteDegeneratedFaces();

        //**************************************************************************
        // 6) Limpiar temporales.
        //**************************************************************************
        oldIndexToNewIndex.clear();
        cellToNewInteriorIndex.clear();
        cellToNewFrontierIndex.clear();
        interiorCellToOldIndices.clear();
        interiorAveragePositions.clear();
    }

    private static Map<Vector3i, Vector3d> calculateInteriorAveragePositions(
            List<GaiaVertex> vertices,
            Map<Vector3i, List<Integer>> interiorCellToOldIndices) {

        Map<Vector3i, Vector3d> result = new HashMap<>();

        if (vertices == null || interiorCellToOldIndices == null || interiorCellToOldIndices.isEmpty()) {
            return result;
        }

        for (Map.Entry<Vector3i, List<Integer>> entry : interiorCellToOldIndices.entrySet()) {
            Vector3i cellIndex = entry.getKey();
            List<Integer> oldIndices = entry.getValue();

            if (cellIndex == null || oldIndices == null || oldIndices.size() < 2) {
                continue;
            }

            Vector3d avg = new Vector3d();
            int count = 0;

            for (Integer oldIndex : oldIndices) {
                if (oldIndex == null || oldIndex < 0 || oldIndex >= vertices.size()) {
                    continue;
                }

                GaiaVertex vertex = vertices.get(oldIndex);

                if (vertex == null || vertex.getPosition() == null) {
                    continue;
                }

                avg.add(vertex.getPosition());
                count++;
            }

            if (count == 0) {
                continue;
            }

            avg.div(count);

            result.put(new Vector3i(cellIndex), avg);
        }

        return result;
    }

    private static void replaceFaceIndices(
            GaiaPrimitive primitive,
            Map<Integer, Integer> oldIndexToNewIndex) {

        if (primitive == null || oldIndexToNewIndex == null || oldIndexToNewIndex.isEmpty()) {
            return;
        }

        List<GaiaSurface> surfaces = primitive.getSurfaces();

        if (surfaces == null) {
            return;
        }

        for (GaiaSurface surface : surfaces) {
            if (surface == null || surface.getFaces() == null) {
                continue;
            }

            for (GaiaFace face : surface.getFaces()) {
                if (face == null || face.getIndices() == null) {
                    continue;
                }

                int[] indices = face.getIndices();

                for (int i = 0; i < indices.length; i++) {
                    Integer newIndex = oldIndexToNewIndex.get(indices[i]);

                    if (newIndex != null) {
                        indices[i] = newIndex;
                    }
                }
            }
        }
    }

    private static void updateSceneCellBounds(
            List<GaiaVertex> vertices,
            int originalVertexCount,
            CellGrid3D cellGrid,
            Vector3i sceneMinCellIndex,
            Vector3i sceneMaxCellIndex) {

        if (vertices == null || cellGrid == null || sceneMinCellIndex == null || sceneMaxCellIndex == null) {
            return;
        }

        boolean first = true;

        for (int i = 0; i < originalVertexCount; i++) {
            GaiaVertex vertex = vertices.get(i);

            if (vertex == null || vertex.getPosition() == null) {
                continue;
            }

            Vector3i cellIndex = new Vector3i(
                    cellGrid.getCellIndex(vertex.getPosition())
            );

            if (first) {
                sceneMinCellIndex.set(cellIndex);
                sceneMaxCellIndex.set(cellIndex);
                first = false;
            } else {
                sceneMinCellIndex.x = Math.min(sceneMinCellIndex.x, cellIndex.x);
                sceneMinCellIndex.y = Math.min(sceneMinCellIndex.y, cellIndex.y);
                sceneMinCellIndex.z = Math.min(sceneMinCellIndex.z, cellIndex.z);

                sceneMaxCellIndex.x = Math.max(sceneMaxCellIndex.x, cellIndex.x);
                sceneMaxCellIndex.y = Math.max(sceneMaxCellIndex.y, cellIndex.y);
                sceneMaxCellIndex.z = Math.max(sceneMaxCellIndex.z, cellIndex.z);
            }
        }
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
}