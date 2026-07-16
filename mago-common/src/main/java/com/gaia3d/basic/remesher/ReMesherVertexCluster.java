package com.gaia3d.basic.remesher;

import com.gaia3d.basic.geometry.modifier.topology.GaiaExtractor;
import com.gaia3d.basic.model.GaiaFace;
import com.gaia3d.basic.model.GaiaPrimitive;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.GaiaSurface;
import com.gaia3d.basic.model.GaiaVertex;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ReMesherVertexCluster {

    private ReMesherVertexCluster() {
    }

    /**
     * Remeshes all primitives in the scene using vertex clustering.
     *
     * <p>Frontier vertices are handled as follows:</p>
     *
     * <ul>
     *     <li>
     *         A frontier vertex with a global boundary anchor uses
     *         the globally locked position.
     *     </li>
     *     <li>
     *         A frontier vertex without a global boundary anchor is
     *          clustered only with other local frontier vertices.
     *     </li>
     * </ul>
     *
     * <p>The CellGrid3D and GlobalBoundaryAnchors must have been
     * calculated using the same grid definition.</p>
     */
    public static void reMeshScene(
            GaiaScene scene,
            ReMeshParameters reMeshParams,
            Vector3i sceneMinCellIndex,
            Vector3i sceneMaxCellIndex
    ) {
        if (scene == null || reMeshParams == null) {
            return;
        }

        CellGrid3D cellGrid =
                reMeshParams.getCellGrid();

        if (cellGrid == null) {
            log.warn(
                    "Could not remesh scene because CellGrid3D is null"
            );

            return;
        }

        /*
         * A null GlobalBoundaryAnchors is valid.
         * It means that this LOD has no globally locked boundaries.
         */
        GlobalBoundaryAnchors globalBoundaryAnchors =
                reMeshParams.getGlobalBoundaryAnchors();

        GaiaExtractor extractor =
                new GaiaExtractor();

        List<GaiaPrimitive> primitives =
                extractor.extractAllPrimitives(
                        scene
                );

        if (primitives == null || primitives.isEmpty()) {
            return;
        }

        /*
         * Calculate the complete scene bounds before modifying
         * any primitive or adding clustered vertices.
         */
        updateSceneCellBounds(
                primitives,
                cellGrid,
                sceneMinCellIndex,
                sceneMaxCellIndex
        );

        RemeshStats totalStats =
                new RemeshStats();

        int processedPrimitives = 0;

        for (GaiaPrimitive primitive : primitives) {
            if (primitive == null) {
                continue;
            }

            RemeshStats primitiveStats =
                    reMeshPrimitive(
                            primitive,
                            cellGrid,
                            globalBoundaryAnchors
                    );

            totalStats.add(
                    primitiveStats
            );

            processedPrimitives++;
        }

        log.debug(
                "V2 reMeshScene processedPrimitives = {}",
                processedPrimitives
        );

        log.debug(
                "V2 reMeshScene originalVertices = {}",
                totalStats.originalVertices
        );

        log.debug(
                "V2 reMeshScene frontierVertices = {}",
                totalStats.frontierVertices
        );

        log.debug(
                "V2 reMeshScene anchoredFrontierVertices = {}",
                totalStats.anchoredFrontierVertices
        );

        log.debug(
                "V2 reMeshScene localFrontierVertices = {}",
                totalStats.localFrontierVertices
        );

        log.debug(
                "V2 reMeshScene localInteriorVertices = {}",
                totalStats.localInteriorVertices
        );
        

        log.debug(
                "V2 reMeshScene createdAnchoredVertices = {}",
                totalStats.createdAnchoredVertices
        );
        

        log.debug(
                "V2 reMeshScene mappedAnchoredVertices = {}",
                totalStats.mappedAnchoredVertices
        );
        

        log.debug(
                "V2 reMeshScene missingGlobalAverage = {}",
                totalStats.missingGlobalAverage
        );

        log.debug(
                "V2 reMeshScene verticesAfterCompaction = {}",
                totalStats.verticesAfterCompaction
        );
    }

    /**
     * Remeshes one primitive.
     */
    private static RemeshStats reMeshPrimitive_new(
            GaiaPrimitive primitive,
            CellGrid3D cellGrid,
            GlobalBoundaryAnchors globalBoundaryAnchors
    ) {
        RemeshStats stats =
                new RemeshStats();

        List<GaiaVertex> vertices =
                primitive.getVertices();

        if (vertices == null || vertices.isEmpty()) {
            return stats;
        }

        int originalVertexCount =
                vertices.size();

        stats.originalVertices =
                originalVertexCount;

        List<GaiaFace> faces =
                primitive.extractGaiaAllFaces(
                        null
                );

        if (faces == null || faces.isEmpty()) {
            return stats;
        }

        GaiaFrontierFinder frontierFinder =
                new GaiaFrontierFinder();

        int[] weldedIndices =
                new int[originalVertexCount];

        boolean[] frontierVertex =
                frontierFinder.findBoundaryVertices(
                        vertices,
                        faces,
                        1e-6,
                        weldedIndices
                );

        if (frontierVertex == null
                || frontierVertex.length < originalVertexCount) {

            return stats;
        }

        /*
         * Interior and unanchored frontier vertices must use
         * separate local clusters.
         */
        Map<Vector3i, CellAccumulator> interiorCellAccumulators =
                new HashMap<>();

        Map<Vector3i, CellAccumulator> frontierCellAccumulators =
                new HashMap<>();

        for (int oldIndex = 0;
             oldIndex < originalVertexCount;
             oldIndex++) {

            GaiaVertex vertex =
                    vertices.get(oldIndex);

            if (vertex == null
                    || vertex.getPosition() == null) {
                continue;
            }

            Vector3i cellIndex =
                    new Vector3i(
                            cellGrid.getCellIndex(
                                    vertex.getPosition()
                            )
                    );

            boolean isFrontier =
                    frontierVertex[oldIndex];

            Vector3d globalAnchor =
                    null;

            if (isFrontier
                    && globalBoundaryAnchors != null) {

                globalAnchor =
                        globalBoundaryAnchors.getAverage(
                                cellIndex
                        );
            }

            if (isFrontier) {
                stats.frontierVertices++;

                if (globalAnchor != null) {
                    stats.anchoredFrontierVertices++;
                    continue;
                }

                frontierCellAccumulators
                        .computeIfAbsent(
                                cellIndex,
                                ignored -> new CellAccumulator()
                        )
                        .add(
                                vertex.getPosition()
                        );

                stats.localFrontierVertices++;

            } else {
                interiorCellAccumulators
                        .computeIfAbsent(
                                cellIndex,
                                ignored -> new CellAccumulator()
                        )
                        .add(
                                vertex.getPosition()
                        );

                stats.localInteriorVertices++;
            }
        }

        /*
         * This is the total number of local clusters, not necessarily
         * the number of unique spatial cells, because one cell may
         * contain both an interior and a frontier cluster.
         */
        stats.localCells =
                interiorCellAccumulators.size()
                        + frontierCellAccumulators.size();

        int[] oldIndexToNewIndex =
                new int[originalVertexCount];

        Arrays.fill(
                oldIndexToNewIndex,
                -1
        );

        /*
         * Keep separate resulting vertices for:
         *
         * - globally anchored frontiers;
         * - locally clustered frontiers;
         * - locally clustered interiors.
         */
        Map<Vector3i, Integer> cellToNewAnchoredIndex =
                new HashMap<>();

        Map<Vector3i, Integer> cellToNewFrontierIndex =
                new HashMap<>();

        Map<Vector3i, Integer> cellToNewInteriorIndex =
                new HashMap<>();

        for (int oldIndex = 0;
             oldIndex < originalVertexCount;
             oldIndex++) {

            GaiaVertex oldVertex =
                    vertices.get(oldIndex);

            if (oldVertex == null
                    || oldVertex.getPosition() == null) {
                continue;
            }

            Vector3i cellIndex =
                    new Vector3i(
                            cellGrid.getCellIndex(
                                    oldVertex.getPosition()
                            )
                    );

            boolean isFrontier =
                    frontierVertex[oldIndex];

            Vector3d globalAnchor =
                    null;

            if (isFrontier
                    && globalBoundaryAnchors != null) {

                globalAnchor =
                        globalBoundaryAnchors.getAverage(
                                cellIndex
                        );
            }

            boolean hasGlobalAnchor =
                    globalAnchor != null;

            Vector3d targetPosition;
            Map<Vector3i, Integer> cellToNewIndex;

            if (hasGlobalAnchor) {
                targetPosition =
                        globalAnchor;

                cellToNewIndex =
                        cellToNewAnchoredIndex;

            } else if (isFrontier) {
                /*
                 * No global anchor means this frontier vertex must remain untouched.
                 * oldIndexToNewIndex stays -1, so replaceFaceIndices() will keep
                 * the original vertex index.
                 */
                //stats.preservedUnanchoredFrontierVertices++;
                continue;

            } else {
                CellAccumulator accumulator =
                        interiorCellAccumulators.get(
                                cellIndex
                        );

                if (accumulator == null
                        || accumulator.getCount() < 2) {

                    /*
                     * Leave the original vertex index unchanged.
                     */
                    stats.skippedSingleInteriorCluster++;
                    continue;
                }

                targetPosition =
                        accumulator.calculateAverage();

                cellToNewIndex =
                        cellToNewInteriorIndex;
            }

            if (targetPosition == null) {
                continue;
            }

            Integer newIndex =
                    cellToNewIndex.get(
                            cellIndex
                    );

            if (newIndex == null) {
                GaiaVertex newVertex =
                        new GaiaVertex();

                newVertex.setPosition(
                        new Vector3d(
                                targetPosition
                        )
                );

                copyVertexAttributes(
                        oldVertex,
                        newVertex
                );

                newIndex =
                        vertices.size();

                vertices.add(
                        newVertex
                );

                cellToNewIndex.put(
                        cellIndex,
                        newIndex
                );

                if (hasGlobalAnchor) {
                    stats.createdAnchoredVertices++;
                } else if (isFrontier) {
                    stats.createdFrontierVertices++;
                } else {
                    stats.createdInteriorVertices++;
                }
            }

            oldIndexToNewIndex[oldIndex] =
                    newIndex;

            if (hasGlobalAnchor) {
                stats.mappedAnchoredVertices++;
            } else if (isFrontier) {
                stats.mappedFrontierVertices++;
            } else {
                stats.mappedInteriorVertices++;
            }
        }

        replaceFaceIndices(
                primitive,
                oldIndexToNewIndex
        );

        primitive.deleteDegeneratedFaces();

        removeUnusedVerticesAndReindex(
                primitive
        );

        stats.verticesAfterCompaction =
                vertices.size();

        return stats;
    }

    private static RemeshStats reMeshPrimitive(
            GaiaPrimitive primitive,
            CellGrid3D cellGrid,
            GlobalBoundaryAnchors globalBoundaryAnchors
    ) {
        RemeshStats stats =
                new RemeshStats();

        List<GaiaVertex> vertices =
                primitive.getVertices();

        if (vertices == null || vertices.isEmpty()) {
            return stats;
        }

        int originalVertexCount =
                vertices.size();

        stats.originalVertices =
                originalVertexCount;

        List<GaiaFace> faces =
                primitive.extractGaiaAllFaces(
                        null
                );

        if (faces == null || faces.isEmpty()) {
            return stats;
        }

        GaiaFrontierFinder frontierFinder =
                new GaiaFrontierFinder();

        int[] weldedIndices =
                new int[originalVertexCount];

        boolean[] frontierVertex =
                frontierFinder.findBoundaryVertices(
                        vertices,
                        faces,
                        1e-6,
                        weldedIndices
                );

        if (frontierVertex == null
                || frontierVertex.length < originalVertexCount) {

            return stats;
        }

        /*
         * Interior and unanchored frontier vertices must use
         * separate local clusters.
         */
        Map<Vector3i, CellAccumulator> interiorCellAccumulators =
                new HashMap<>();

        Map<Vector3i, CellAccumulator> frontierCellAccumulators =
                new HashMap<>();

        for (int oldIndex = 0;
             oldIndex < originalVertexCount;
             oldIndex++) {

            GaiaVertex vertex =
                    vertices.get(oldIndex);

            if (vertex == null
                    || vertex.getPosition() == null) {
                continue;
            }

            Vector3i cellIndex =
                    new Vector3i(
                            cellGrid.getCellIndex(
                                    vertex.getPosition()
                            )
                    );

            boolean isFrontier =
                    frontierVertex[oldIndex];

            Vector3d globalAnchor =
                    null;

            if (isFrontier
                    && globalBoundaryAnchors != null) {

                globalAnchor =
                        globalBoundaryAnchors.getAverage(
                                cellIndex
                        );
            }

            if (isFrontier) {
                stats.frontierVertices++;

                if (globalAnchor != null) {
                    stats.anchoredFrontierVertices++;
                    continue;
                }

                /*
                 * Unanchored frontier vertices are clustered only
                 * with other unanchored frontier vertices.
                 */
                frontierCellAccumulators
                        .computeIfAbsent(
                                cellIndex,
                                ignored -> new CellAccumulator()
                        )
                        .add(
                                vertex.getPosition()
                        );

                stats.localFrontierVertices++;

            } else {
                /*
                 * Interior vertices are clustered only with other
                 * interior vertices.
                 */
                interiorCellAccumulators
                        .computeIfAbsent(
                                cellIndex,
                                ignored -> new CellAccumulator()
                        )
                        .add(
                                vertex.getPosition()
                        );

                stats.localInteriorVertices++;
            }
        }

        /*
         * This is the total number of local clusters, not necessarily
         * the number of unique spatial cells, because one cell may
         * contain both an interior and a frontier cluster.
         */
        stats.localCells =
                interiorCellAccumulators.size()
                        + frontierCellAccumulators.size();

        int[] oldIndexToNewIndex =
                new int[originalVertexCount];

        Arrays.fill(
                oldIndexToNewIndex,
                -1
        );

        /*
         * Keep separate resulting vertices for:
         *
         * - globally anchored frontiers;
         * - locally clustered frontiers;
         * - locally clustered interiors.
         */
        Map<Vector3i, Integer> cellToNewAnchoredIndex =
                new HashMap<>();

        Map<Vector3i, Integer> cellToNewFrontierIndex =
                new HashMap<>();

        Map<Vector3i, Integer> cellToNewInteriorIndex =
                new HashMap<>();

        for (int oldIndex = 0;
             oldIndex < originalVertexCount;
             oldIndex++) {

            GaiaVertex oldVertex =
                    vertices.get(oldIndex);

            if (oldVertex == null
                    || oldVertex.getPosition() == null) {
                continue;
            }

            Vector3i cellIndex =
                    new Vector3i(
                            cellGrid.getCellIndex(
                                    oldVertex.getPosition()
                            )
                    );

            boolean isFrontier =
                    frontierVertex[oldIndex];

            Vector3d globalAnchor =
                    null;

            if (isFrontier
                    && globalBoundaryAnchors != null) {

                globalAnchor =
                        globalBoundaryAnchors.getAverage(
                                cellIndex
                        );
            }

            boolean hasGlobalAnchor =
                    globalAnchor != null;

            Vector3d targetPosition;
            Map<Vector3i, Integer> cellToNewIndex;

            if (hasGlobalAnchor) {
                targetPosition =
                        globalAnchor;

                cellToNewIndex =
                        cellToNewAnchoredIndex;

            } else if (isFrontier) {
                CellAccumulator accumulator =
                        frontierCellAccumulators.get(
                                cellIndex
                        );

                if (accumulator == null
                        || accumulator.getCount() < 2) {

                    /*
                     * Leave the original vertex index unchanged.
                     */
                    stats.skippedSingleFrontierCluster++;
                    continue;
                }

                targetPosition =
                        accumulator.calculateAverage();

                cellToNewIndex =
                        cellToNewFrontierIndex;

            } else {
                CellAccumulator accumulator =
                        interiorCellAccumulators.get(
                                cellIndex
                        );

                if (accumulator == null
                        || accumulator.getCount() < 2) {

                    /*
                     * Leave the original vertex index unchanged.
                     */
                    stats.skippedSingleInteriorCluster++;
                    continue;
                }

                targetPosition =
                        accumulator.calculateAverage();

                cellToNewIndex =
                        cellToNewInteriorIndex;
            }

            if (targetPosition == null) {
                continue;
            }

            Integer newIndex =
                    cellToNewIndex.get(
                            cellIndex
                    );

            if (newIndex == null) {
                GaiaVertex newVertex =
                        new GaiaVertex();

                newVertex.setPosition(
                        new Vector3d(
                                targetPosition
                        )
                );

                copyVertexAttributes(
                        oldVertex,
                        newVertex
                );

                newIndex =
                        vertices.size();

                vertices.add(
                        newVertex
                );

                cellToNewIndex.put(
                        cellIndex,
                        newIndex
                );

                if (hasGlobalAnchor) {
                    stats.createdAnchoredVertices++;
                } else if (isFrontier) {
                    stats.createdFrontierVertices++;
                } else {
                    stats.createdInteriorVertices++;
                }
            }

            oldIndexToNewIndex[oldIndex] =
                    newIndex;

            if (hasGlobalAnchor) {
                stats.mappedAnchoredVertices++;
            } else if (isFrontier) {
                stats.mappedFrontierVertices++;
            } else {
                stats.mappedInteriorVertices++;
            }
        }

        replaceFaceIndices(
                primitive,
                oldIndexToNewIndex
        );

        primitive.deleteDegeneratedFaces();

        removeUnusedVerticesAndReindex(
                primitive
        );

        stats.verticesAfterCompaction =
                vertices.size();

        return stats;
    }

    /**
     * Replaces every mapped original vertex index with its
     * corresponding clustered vertex index.
     */
    private static void replaceFaceIndices(
            GaiaPrimitive primitive,
            int[] oldIndexToNewIndex
    ) {
        if (primitive == null
                || oldIndexToNewIndex == null
                || oldIndexToNewIndex.length == 0) {
            return;
        }

        List<GaiaSurface> surfaces =
                primitive.getSurfaces();

        if (surfaces == null || surfaces.isEmpty()) {
            return;
        }

        for (GaiaSurface surface : surfaces) {
            if (surface == null
                    || surface.getFaces() == null) {
                continue;
            }

            for (GaiaFace face : surface.getFaces()) {
                if (face == null
                        || face.getIndices() == null) {
                    continue;
                }

                int[] indices =
                        face.getIndices();

                for (int i = 0;
                     i < indices.length;
                     i++) {

                    int oldIndex =
                            indices[i];

                    if (oldIndex < 0
                            || oldIndex >= oldIndexToNewIndex.length) {
                        continue;
                    }

                    int newIndex =
                            oldIndexToNewIndex[oldIndex];

                    if (newIndex >= 0) {
                        indices[i] =
                                newIndex;
                    }
                }
            }
        }
    }

    /**
     * Removes vertices that are not referenced by any face and
     * rewrites all face indices to use the compacted vertex list.
     */
    private static void removeUnusedVerticesAndReindex(
            GaiaPrimitive primitive
    ) {
        if (primitive == null) {
            return;
        }

        List<GaiaVertex> vertices =
                primitive.getVertices();

        if (vertices == null || vertices.isEmpty()) {
            return;
        }

        List<GaiaFace> faces =
                primitive.extractGaiaAllFaces(
                        null
                );

        if (faces == null || faces.isEmpty()) {
            vertices.clear();
            return;
        }

        int vertexCount =
                vertices.size();

        boolean[] usedVertices =
                new boolean[vertexCount];

        int usedVertexCount = 0;

        for (GaiaFace face : faces) {
            if (face == null
                    || face.getIndices() == null) {
                continue;
            }

            int[] indices =
                    face.getIndices();

            for (int index : indices) {
                if (index < 0 || index >= vertexCount) {
                    continue;
                }

                if (!usedVertices[index]) {
                    usedVertices[index] =
                            true;

                    usedVertexCount++;
                }
            }
        }

        if (usedVertexCount == vertexCount) {
            return;
        }

        int[] oldIndexToCompactedIndex =
                new int[vertexCount];

        Arrays.fill(
                oldIndexToCompactedIndex,
                -1
        );

        List<GaiaVertex> compactedVertices =
                new ArrayList<>(
                        usedVertexCount
                );

        for (int oldIndex = 0;
             oldIndex < vertexCount;
             oldIndex++) {

            if (!usedVertices[oldIndex]) {
                continue;
            }

            int compactedIndex =
                    compactedVertices.size();

            oldIndexToCompactedIndex[oldIndex] =
                    compactedIndex;

            compactedVertices.add(
                    vertices.get(oldIndex)
            );
        }

        for (GaiaFace face : faces) {
            if (face == null
                    || face.getIndices() == null) {
                continue;
            }

            int[] indices =
                    face.getIndices();

            for (int i = 0;
                 i < indices.length;
                 i++) {

                int oldIndex =
                        indices[i];

                if (oldIndex < 0
                        || oldIndex >= oldIndexToCompactedIndex.length) {
                    continue;
                }

                int compactedIndex =
                        oldIndexToCompactedIndex[oldIndex];

                if (compactedIndex >= 0) {
                    indices[i] =
                            compactedIndex;
                }
            }
        }

        vertices.clear();

        vertices.addAll(
                compactedVertices
        );
    }

    /**
     * Calculates the minimum and maximum CellGrid3D indices
     * occupied by all primitives in the scene.
     */
    private static void updateSceneCellBounds(
            List<GaiaPrimitive> primitives,
            CellGrid3D cellGrid,
            Vector3i sceneMinCellIndex,
            Vector3i sceneMaxCellIndex
    ) {
        if (primitives == null
                || cellGrid == null
                || sceneMinCellIndex == null
                || sceneMaxCellIndex == null) {
            return;
        }

        int minX =
                Integer.MAX_VALUE;

        int minY =
                Integer.MAX_VALUE;

        int minZ =
                Integer.MAX_VALUE;

        int maxX =
                Integer.MIN_VALUE;

        int maxY =
                Integer.MIN_VALUE;

        int maxZ =
                Integer.MIN_VALUE;

        boolean foundPosition =
                false;

        for (GaiaPrimitive primitive : primitives) {
            if (primitive == null
                    || primitive.getVertices() == null) {
                continue;
            }

            List<GaiaVertex> vertices =
                    primitive.getVertices();

            for (GaiaVertex vertex : vertices) {
                if (vertex == null
                        || vertex.getPosition() == null) {
                    continue;
                }

                Vector3i cellIndex =
                        cellGrid.getCellIndex(
                                vertex.getPosition()
                        );

                minX =
                        Math.min(
                                minX,
                                cellIndex.x
                        );

                minY =
                        Math.min(
                                minY,
                                cellIndex.y
                        );

                minZ =
                        Math.min(
                                minZ,
                                cellIndex.z
                        );

                maxX =
                        Math.max(
                                maxX,
                                cellIndex.x
                        );

                maxY =
                        Math.max(
                                maxY,
                                cellIndex.y
                        );

                maxZ =
                        Math.max(
                                maxZ,
                                cellIndex.z
                        );

                foundPosition =
                        true;
            }
        }

        if (!foundPosition) {
            return;
        }

        sceneMinCellIndex.set(
                minX,
                minY,
                minZ
        );

        sceneMaxCellIndex.set(
                maxX,
                maxY,
                maxZ
        );
    }

    /**
     * Copies the non-positional attributes of a source vertex.
     */
    private static void copyVertexAttributes(
            GaiaVertex source,
            GaiaVertex destination
    ) {
        if (source == null || destination == null) {
            return;
        }

        if (source.getNormal() != null) {
            destination.setNormal(
                    new Vector3d(
                            source.getNormal()
                    )
            );
        }

        if (source.getTexcoords() != null) {
            destination.setTexcoords(
                    new Vector2d(
                            source.getTexcoords()
                    )
            );
        }

        if (source.getColor() != null) {
            destination.setColor(
                    source.getColor().clone()
            );
        }

        destination.setBatchId(
                source.getBatchId()
        );
    }

    /**
     * Accumulates local vertex positions for one grid cell.
     */
    private static final class CellAccumulator {

        private final Vector3d positionSum =
                new Vector3d();

        private int count;

        private void add(
                Vector3d position
        ) {
            if (position == null) {
                return;
            }

            positionSum.add(
                    position
            );

            count++;
        }

        private int getCount() {
            return count;
        }

        private Vector3d calculateAverage() {
            if (count <= 0) {
                return null;
            }

            return new Vector3d(
                    positionSum
            ).div(
                    count
            );
        }
    }

    /**
     * Aggregated remeshing statistics.
     */
    private static final class RemeshStats {

        public int localCells;
        private long originalVertices;

        private long frontierVertices;
        private long anchoredFrontierVertices;
        private long localFrontierVertices;
        private long localInteriorVertices;

        private long frontierCells;
        private long interiorCells;

        private long createdAnchoredVertices;
        private long createdFrontierVertices;
        private long createdInteriorVertices;

        private long mappedAnchoredVertices;
        private long mappedFrontierVertices;
        private long mappedInteriorVertices;

        private long skippedSingleFrontierCluster;
        private long skippedSingleInteriorCluster;

        private long missingGlobalAverage;

        private long verticesAfterCompaction;

        private void add(
                RemeshStats other
        ) {
            if (other == null) {
                return;
            }

            originalVertices +=
                    other.originalVertices;

            frontierVertices +=
                    other.frontierVertices;

            anchoredFrontierVertices +=
                    other.anchoredFrontierVertices;

            localFrontierVertices +=
                    other.localFrontierVertices;

            localInteriorVertices +=
                    other.localInteriorVertices;

            frontierCells +=
                    other.frontierCells;

            interiorCells +=
                    other.interiorCells;

            createdAnchoredVertices +=
                    other.createdAnchoredVertices;

            createdFrontierVertices +=
                    other.createdFrontierVertices;

            createdInteriorVertices +=
                    other.createdInteriorVertices;

            mappedAnchoredVertices +=
                    other.mappedAnchoredVertices;

            mappedFrontierVertices +=
                    other.mappedFrontierVertices;

            mappedInteriorVertices +=
                    other.mappedInteriorVertices;

            skippedSingleFrontierCluster +=
                    other.skippedSingleFrontierCluster;

            skippedSingleInteriorCluster +=
                    other.skippedSingleInteriorCluster;

            missingGlobalAverage +=
                    other.missingGlobalAverage;

            verticesAfterCompaction +=
                    other.verticesAfterCompaction;
        }
    }
}