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
public class ReMesherVertexClusterV2 {

    private ReMesherVertexClusterV2() {
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
     *         treated as a regular local vertex.
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
                "V2 reMeshScene localCells = {}",
                totalStats.localCells
        );

        log.debug(
                "V2 reMeshScene createdAnchoredVertices = {}",
                totalStats.createdAnchoredVertices
        );

        log.debug(
                "V2 reMeshScene createdLocalVertices = {}",
                totalStats.createdLocalVertices
        );

        log.debug(
                "V2 reMeshScene mappedAnchoredVertices = {}",
                totalStats.mappedAnchoredVertices
        );

        log.debug(
                "V2 reMeshScene mappedLocalVertices = {}",
                totalStats.mappedLocalVertices
        );

        log.debug(
                "V2 reMeshScene skippedSingleLocalCluster = {}",
                totalStats.skippedSingleLocalCluster
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
         * Local clusters contain:
         *
         * - all interior vertices;
         * - frontier vertices without a global anchor.
         *
         * Frontier vertices with a global anchor are excluded
         * because they must use the globally locked position.
         */
        Map<Vector3i, CellAccumulator> localCellAccumulators =
                new HashMap<>();

        for (int oldIndex = 0;
             oldIndex < originalVertexCount;
             oldIndex++) {

            GaiaVertex vertex =
                    vertices.get(
                            oldIndex
                    );

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

            if (isFrontier) {
                stats.frontierVertices++;
            }

            boolean hasGlobalAnchor =
                    isFrontier
                            && globalBoundaryAnchors != null
                            && globalBoundaryAnchors.hasAverage(
                            cellIndex
                    );

            if (hasGlobalAnchor) {
                stats.anchoredFrontierVertices++;
                continue;
            }

            localCellAccumulators
                    .computeIfAbsent(
                            cellIndex,
                            ignored -> new CellAccumulator()
                    )
                    .add(
                            vertex.getPosition()
                    );

            if (isFrontier) {
                stats.localFrontierVertices++;
            } else {
                stats.localInteriorVertices++;
            }
        }

        stats.localCells =
                localCellAccumulators.size();

        /*
         * An array is cheaper than Map<Integer, Integer> and
         * provides direct old-index to new-index lookup.
         */
        int[] oldIndexToNewIndex =
                new int[originalVertexCount];

        Arrays.fill(
                oldIndexToNewIndex,
                -1
        );

        /*
         * Anchored and local vertices use different maps because
         * both types may exist in the same grid cell but must not
         * necessarily share the same resulting vertex.
         */
        Map<Vector3i, Integer> cellToNewLocalIndex =
                new HashMap<>();

        Map<Vector3i, Integer> cellToNewAnchoredIndex =
                new HashMap<>();

        for (int oldIndex = 0;
             oldIndex < originalVertexCount;
             oldIndex++) {

            GaiaVertex oldVertex =
                    vertices.get(
                            oldIndex
                    );

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

            boolean hasGlobalAnchor =
                    isFrontier
                            && globalBoundaryAnchors != null
                            && globalBoundaryAnchors.hasAverage(
                            cellIndex
                    );

            Vector3d targetPosition;
            Map<Vector3i, Integer> cellToNewIndex;

            if (hasGlobalAnchor) {
                /*
                 * This vertex belongs to a globally locked
                 * boundary cell.
                 */
                targetPosition =
                        globalBoundaryAnchors.getAverage(
                                cellIndex
                        );

                if (targetPosition == null) {
                    /*
                     * This should not normally happen because
                     * hasAverage() returned true.
                     */
                    stats.missingGlobalAverage++;
                    continue;
                }

                cellToNewIndex =
                        cellToNewAnchoredIndex;

            } else {
                /*
                 * Interior vertices and unanchored frontier
                 * vertices are treated identically.
                 */
                CellAccumulator accumulator =
                        localCellAccumulators.get(
                                cellIndex
                        );

                if (accumulator == null
                        || accumulator.getCount() < 2) {

                    /*
                     * A cell containing only one local vertex
                     * cannot be simplified. Its original index
                     * remains unchanged.
                     */
                    stats.skippedSingleLocalCluster++;
                    continue;
                }

                targetPosition =
                        accumulator.calculateAverage();

                if (targetPosition == null) {
                    continue;
                }

                cellToNewIndex =
                        cellToNewLocalIndex;
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

                /*
                 * cellIndex is a newly created object and is not
                 * modified after being inserted into the map.
                 */
                cellToNewIndex.put(
                        cellIndex,
                        newIndex
                );

                if (hasGlobalAnchor) {
                    stats.createdAnchoredVertices++;
                } else {
                    stats.createdLocalVertices++;
                }
            }

            oldIndexToNewIndex[oldIndex] =
                    newIndex;

            if (hasGlobalAnchor) {
                stats.mappedAnchoredVertices++;
            } else {
                stats.mappedLocalVertices++;
            }
        }

        /*
         * Replace original indices with clustered indices.
         */
        replaceFaceIndices(
                primitive,
                oldIndexToNewIndex
        );

        /*
         * Several original vertices may now reference the same
         * clustered vertex, producing degenerated faces.
         */
        primitive.deleteDegeneratedFaces();

        /*
         * Remove original vertices that are no longer referenced
         * by any face and compact all remaining indices.
         *
         * Without this step, clustered vertices are appended to
         * the original list and the resulting primitive may become
         * larger instead of smaller.
         */
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

        private long originalVertices;
        private long frontierVertices;
        private long anchoredFrontierVertices;
        private long localFrontierVertices;
        private long localInteriorVertices;
        private long localCells;

        private long createdAnchoredVertices;
        private long createdLocalVertices;

        private long mappedAnchoredVertices;
        private long mappedLocalVertices;

        private long skippedSingleLocalCluster;
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

            localCells +=
                    other.localCells;

            createdAnchoredVertices +=
                    other.createdAnchoredVertices;

            createdLocalVertices +=
                    other.createdLocalVertices;

            mappedAnchoredVertices +=
                    other.mappedAnchoredVertices;

            mappedLocalVertices +=
                    other.mappedLocalVertices;

            skippedSingleLocalCluster +=
                    other.skippedSingleLocalCluster;

            missingGlobalAverage +=
                    other.missingGlobalAverage;

            verticesAfterCompaction +=
                    other.verticesAfterCompaction;
        }
    }
}