package com.gaia3d.basic.remesher;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
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
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ReMesherVertexClusterV3 {

    private static final double FRONTIER_WELD_ERROR =
            1e-6;

    /*
     * Vertices whose locked coordinates differ by less than this
     * value may still belong to the same cluster.
     */
    private static final double LOCK_COORDINATE_TOLERANCE =
            1e-6;

    /*
     * A scene axis is considered thin when its size is smaller
     * than one CellGrid3D cell.
     */
    private static final double THIN_AXIS_FACTOR =
            3.0;

    private static final double DEBUG_ANCHOR_AXIS_TOLERANCE =
            1e-3;

    private static final double THIN_EXPANSION_FACTOR =
            1.10;

    private ReMesherVertexClusterV3() {
    }

    public static void reMeshScene(
            GaiaScene scene,
            ReMeshParameters reMeshParams,
            Vector3i sceneMinCellIndex,
            Vector3i sceneMaxCellIndex,
            GaiaBoundingBox nodeBBox
    ) {
        if (scene == null
                || reMeshParams == null) {
            return;
        }

        CellGrid3D cellGrid =
                reMeshParams.getCellGrid();

        if (cellGrid == null) {
            return;
        }

        double cellSize =
                cellGrid.getCellSize();

        if (!Double.isFinite(cellSize)
                || cellSize <= 0.0) {
            return;
        }

        GlobalBoundaryAnchors globalBoundaryAnchors =
                reMeshParams.getGlobalBoundaryAnchors();

        GaiaBoundingBox sceneBBox =
                scene.updateBoundingBox();

        if (sceneBBox == null) {
            return;
        }

        double sceneSizeX =
                sceneBBox.getSizeX();

        double sceneSizeY =
                sceneBBox.getSizeY();

        double thinLimit =
                cellSize * THIN_AXIS_FACTOR;

        /*
         * These flags describe the original fragment.
         *
         * They must remain active after expansion because the remesher
         * must continue protecting the originally thin axis.
         */
        boolean lockX =
                Double.isFinite(sceneSizeX)
                        && sceneSizeX > 0.0
                        && sceneSizeX < thinLimit;

        boolean lockY =
                Double.isFinite(sceneSizeY)
                        && sceneSizeY > 0.0
                        && sceneSizeY < thinLimit;

        GaiaExtractor extractor =
                new GaiaExtractor();

        List<GaiaPrimitive> primitives =
                extractor.extractAllPrimitives(
                        scene
                );

        if (primitives == null
                || primitives.isEmpty()) {
            return;
        }

        ThinScenesScaler.ThinExpansionPlan expansionPlan =
                ThinScenesScaler.createThinExpansionPlan(
                        sceneBBox,
                        nodeBBox,
                        cellSize,
                        lockX,
                        lockY,
                        THIN_EXPANSION_FACTOR
                );

        int modifiedVertices =
                0;

        if (expansionPlan.isEnabled()) {
            List<GaiaVertex> sceneVertices =
                    new ArrayList<>();

            for (GaiaPrimitive primitive : primitives) {
                if (primitive == null
                        || primitive.getVertices() == null
                        || primitive.getVertices().isEmpty()) {
                    continue;
                }

                sceneVertices.addAll(
                        primitive.getVertices()
                );
            }

            modifiedVertices =
                    ThinScenesScaler.applyThinExpansion(
                            sceneVertices,
                            expansionPlan
                    );

            if (modifiedVertices > 0) {
                sceneBBox =
                        scene.updateBoundingBox();
            }

            log.info(
                    "Thin scene expanded. "
                            + "originalSizeX={}, originalSizeY={}, "
                            + "newSizeX={}, newSizeY={}, "
                            + "thinLimit={}, lockX={}, lockY={}, "
                            + "expandX={}, fixedX={}, "
                            + "expandY={}, fixedY={}, "
                            + "factor={}, modifiedVertices={}",
                    sceneSizeX,
                    sceneSizeY,
                    sceneBBox == null
                            ? Double.NaN
                            : sceneBBox.getSizeX(),
                    sceneBBox == null
                            ? Double.NaN
                            : sceneBBox.getSizeY(),
                    thinLimit,
                    lockX,
                    lockY,
                    expansionPlan.expandX(),
                    expansionPlan.fixedX(),
                    expansionPlan.expandY(),
                    expansionPlan.fixedY(),
                    expansionPlan.scaleFactor(),
                    modifiedVertices
            );
        }

        /*
         * Must be calculated after the expansion because some vertices
         * may now occupy different CellGrid3D cells.
         */
        updateSceneCellBounds(
                primitives,
                cellGrid,
                sceneMinCellIndex,
                sceneMaxCellIndex
        );

        RemeshStats totalStats =
                new RemeshStats();

        for (GaiaPrimitive primitive : primitives) {
            if (primitive == null) {
                continue;
            }

            RemeshStats primitiveStats =
                    reMeshPrimitive(
                            primitive,
                            cellGrid,
                            globalBoundaryAnchors,
                            lockX,
                            lockY
                    );

            totalStats.add(
                    primitiveStats
            );
        }

        scene.updateBoundingBox();

        log.debug(
                "V3 reMeshScene sceneSizeX={}, sceneSizeY={}, "
                        + "cellSize={}, lockX={}, lockY={}",
                sceneSizeX,
                sceneSizeY,
                cellSize,
                lockX,
                lockY
        );

        log.debug(
                "V3 reMeshScene originalVertices={}, "
                        + "verticesAfterCompaction={}",
                totalStats.originalVertices,
                totalStats.verticesAfterCompaction
        );

        log.debug(
                "V3 reMeshScene frontierVertices={}, "
                        + "anchoredFrontierVertices={}, "
                        + "localFrontierVertices={}, "
                        + "localInteriorVertices={}",
                totalStats.frontierVertices,
                totalStats.anchoredFrontierVertices,
                totalStats.localFrontierVertices,
                totalStats.localInteriorVertices
        );

        log.debug(
                "V3 reMeshScene frontierCells={}, interiorCells={}",
                totalStats.frontierCells,
                totalStats.interiorCells
        );

        log.debug(
                "V3 reMeshScene createdAnchoredVertices={}, "
                        + "createdFrontierVertices={}, "
                        + "createdInteriorVertices={}",
                totalStats.createdAnchoredVertices,
                totalStats.createdFrontierVertices,
                totalStats.createdInteriorVertices
        );

        log.debug(
                "V3 reMeshScene mappedAnchoredVertices={}, "
                        + "mappedFrontierVertices={}, "
                        + "mappedInteriorVertices={}",
                totalStats.mappedAnchoredVertices,
                totalStats.mappedFrontierVertices,
                totalStats.mappedInteriorVertices
        );

        log.debug(
                "V3 reMeshScene skippedSingleFrontierCluster={}, "
                        + "skippedSingleInteriorCluster={}",
                totalStats.skippedSingleFrontierCluster,
                totalStats.skippedSingleInteriorCluster
        );
    }

    private static final AtomicInteger
            DEBUG_THIN_FRONTIER_LOG_COUNT =
            new AtomicInteger();

    private static RemeshStats reMeshPrimitive(
            GaiaPrimitive primitive,
            CellGrid3D cellGrid,
            GlobalBoundaryAnchors globalBoundaryAnchors,
            boolean lockX,
            boolean lockY
    ) {
        RemeshStats stats =
                new RemeshStats();

        if (primitive == null
                || cellGrid == null) {
            return stats;
        }

        List<GaiaVertex> vertices =
                primitive.getVertices();

        if (vertices == null
                || vertices.isEmpty()) {
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

        if (faces == null
                || faces.isEmpty()) {
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
                        FRONTIER_WELD_ERROR,
                        weldedIndices
                );

        if (frontierVertex == null
                || frontierVertex.length
                < originalVertexCount) {

            return stats;
        }

        /*
         * Frontier and interior vertices use separate local clusters.
         *
         * For thin scenes, the protected coordinate becomes part
         * of LocalClusterKey, preventing both sides of a thin strip
         * from collapsing into the same local vertex.
         */
        Map<LocalClusterKey, CellAccumulator>
                frontierCellAccumulators =
                new HashMap<>();

        Map<LocalClusterKey, CellAccumulator>
                interiorCellAccumulators =
                new HashMap<>();

        /*
         * First pass:
         *
         * - identify global-anchor vertices;
         * - accumulate unanchored frontier vertices;
         * - accumulate interior vertices.
         */
        for (int oldIndex = 0;
             oldIndex < originalVertexCount;
             oldIndex++) {

            GaiaVertex vertex =
                    vertices.get(oldIndex);

            if (vertex == null
                    || vertex.getPosition() == null) {
                continue;
            }

            Vector3d position =
                    vertex.getPosition();

            Vector3i cellIndex =
                    new Vector3i(
                            cellGrid.getCellIndex(
                                    position
                            )
                    );

            boolean isFrontier =
                    frontierVertex[oldIndex];

            /*
             * Raw anchor obtained only from the grid cell.
             *
             * This value is useful for debugging because it tells us
             * whether the cell contains an anchor before applying the
             * thin-axis acceptance rules.
             */
            Vector3d rawGlobalAnchor =
                    globalBoundaryAnchors == null
                            ? null
                            : globalBoundaryAnchors.getAverage(
                            cellIndex
                    );

            /*
             * Accepted anchor after validating the protected axes.
             */
            Vector3d globalAnchor =
                    getGlobalAnchor(
                            globalBoundaryAnchors,
                            cellIndex,
                            position,
                            isFrontier,
                            lockX,
                            lockY
                    );

            /*
             * Debug must happen before any continue.
             *
             * Otherwise anchored frontier vertices would never be
             * displayed because they leave the loop immediately.
             */
            if (isFrontier
                    && (lockX || lockY)
                    && DEBUG_THIN_FRONTIER_LOG_COUNT
                    .getAndIncrement() < 200) {

                double rawAnchorDistance =
                        rawGlobalAnchor == null
                                ? Double.NaN
                                : position.distance(
                                rawGlobalAnchor
                        );

                double differenceX =
                        rawGlobalAnchor == null
                                ? Double.NaN
                                : Math.abs(
                                position.x
                                - rawGlobalAnchor.x
                        );

                double differenceY =
                        rawGlobalAnchor == null
                                ? Double.NaN
                                : Math.abs(
                                position.y
                                - rawGlobalAnchor.y
                        );

                double differenceZ =
                        rawGlobalAnchor == null
                                ? Double.NaN
                                : Math.abs(
                                position.z
                                - rawGlobalAnchor.z
                        );

                log.warn(
                        "THIN FRONTIER: "
                                + "position={}, cell={}, "
                                + "rawAnchor={}, acceptedAnchor={}, "
                                + "rawDistance={}, "
                                + "diffX={}, diffY={}, diffZ={}, "
                                + "lockX={}, lockY={}",
                        position,
                        cellIndex,
                        rawGlobalAnchor,
                        globalAnchor != null,
                        rawAnchorDistance,
                        differenceX,
                        differenceY,
                        differenceZ,
                        lockX,
                        lockY
                );
            }

            if (isFrontier) {
                stats.frontierVertices++;

                /*
                 * A frontier vertex that belongs to a valid global
                 * anchor must not enter the local accumulator.
                 */
                if (globalAnchor != null) {
                    stats.anchoredFrontierVertices++;
                    continue;
                }

                LocalClusterKey clusterKey =
                        createLocalClusterKey(
                                cellIndex,
                                position,
                                lockX,
                                lockY
                        );

                frontierCellAccumulators
                        .computeIfAbsent(
                                clusterKey,
                                ignored ->
                                        new CellAccumulator()
                        )
                        .add(
                                position
                        );

                stats.localFrontierVertices++;

            } else {
                LocalClusterKey clusterKey =
                        createLocalClusterKey(
                                cellIndex,
                                position,
                                lockX,
                                lockY
                        );

                interiorCellAccumulators
                        .computeIfAbsent(
                                clusterKey,
                                ignored ->
                                        new CellAccumulator()
                        )
                        .add(
                                position
                        );

                stats.localInteriorVertices++;
            }
        }

        stats.frontierCells =
                frontierCellAccumulators.size();

        stats.interiorCells =
                interiorCellAccumulators.size();

        /*
         * -1 means that the original vertex remains unchanged.
         */
        int[] oldIndexToNewIndex =
                new int[originalVertexCount];

        Arrays.fill(
                oldIndexToNewIndex,
                -1
        );

        /*
         * Global anchors currently use one resulting vertex per
         * CellGrid3D cell.
         */
        Map<Vector3i, Integer>
                cellToNewAnchoredIndex =
                new HashMap<>();

        /*
         * Local frontier and interior clusters remain separated.
         */
        Map<LocalClusterKey, Integer>
                clusterToNewFrontierIndex =
                new HashMap<>();

        Map<LocalClusterKey, Integer>
                clusterToNewInteriorIndex =
                new HashMap<>();

        /*
         * Second pass:
         *
         * - create anchored vertices;
         * - create local clustered vertices;
         * - create the old-index to new-index mapping.
         */
        for (int oldIndex = 0;
             oldIndex < originalVertexCount;
             oldIndex++) {

            GaiaVertex oldVertex =
                    vertices.get(oldIndex);

            if (oldVertex == null
                    || oldVertex.getPosition() == null) {
                continue;
            }

            Vector3d oldPosition =
                    oldVertex.getPosition();

            Vector3i cellIndex =
                    new Vector3i(
                            cellGrid.getCellIndex(
                                    oldPosition
                            )
                    );

            boolean isFrontier =
                    frontierVertex[oldIndex];

            Vector3d globalAnchor =
                    getGlobalAnchor(
                            globalBoundaryAnchors,
                            cellIndex,
                            oldPosition,
                            isFrontier,
                            lockX,
                            lockY
                    );

            /*
             * Global anchors always have priority over local
             * clustering.
             */
            if (globalAnchor != null) {
                Integer newIndex =
                        cellToNewAnchoredIndex.get(
                                cellIndex
                        );

                if (newIndex == null) {
                    GaiaVertex newVertex =
                            new GaiaVertex();

                    newVertex.setPosition(
                            new Vector3d(
                                    globalAnchor
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

                    cellToNewAnchoredIndex.put(
                            new Vector3i(
                                    cellIndex
                            ),
                            newIndex
                    );

                    stats.createdAnchoredVertices++;
                }

                oldIndexToNewIndex[oldIndex] =
                        newIndex;

                stats.mappedAnchoredVertices++;

                continue;
            }

            LocalClusterKey clusterKey =
                    createLocalClusterKey(
                            cellIndex,
                            oldPosition,
                            lockX,
                            lockY
                    );

            CellAccumulator accumulator;

            Map<LocalClusterKey, Integer>
                    clusterToNewIndex;

            if (isFrontier) {
                accumulator =
                        frontierCellAccumulators.get(
                                clusterKey
                        );

                clusterToNewIndex =
                        clusterToNewFrontierIndex;

                if (accumulator == null
                        || accumulator.getCount() < 2) {

                    /*
                     * Keep the original frontier vertex unchanged.
                     */
                    stats.skippedSingleFrontierCluster++;
                    continue;
                }

            } else {
                accumulator =
                        interiorCellAccumulators.get(
                                clusterKey
                        );

                clusterToNewIndex =
                        clusterToNewInteriorIndex;

                if (accumulator == null
                        || accumulator.getCount() < 2) {

                    /*
                     * Keep the original interior vertex unchanged.
                     */
                    stats.skippedSingleInteriorCluster++;
                    continue;
                }
            }

            Vector3d targetPosition =
                    accumulator.calculateTargetPosition(
                            lockX,
                            lockY
                    );

            if (targetPosition == null) {
                continue;
            }

            Integer newIndex =
                    clusterToNewIndex.get(
                            clusterKey
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

                clusterToNewIndex.put(
                        clusterKey,
                        newIndex
                );

                if (isFrontier) {
                    stats.createdFrontierVertices++;
                } else {
                    stats.createdInteriorVertices++;
                }
            }

            oldIndexToNewIndex[oldIndex] =
                    newIndex;

            if (isFrontier) {
                stats.mappedFrontierVertices++;
            } else {
                stats.mappedInteriorVertices++;
            }
        }

        /*
         * Replace only vertices that received a new index.
         *
         * Original vertices whose mapping remains -1 are preserved.
         */
        replaceFaceIndices(
                primitive,
                oldIndexToNewIndex
        );

        primitive.deleteDegeneratedFaces();

        removeUnusedVerticesAndReindex(
                primitive
        );

        stats.verticesAfterCompaction =
                primitive.getVertices() == null
                        ? 0
                        : primitive.getVertices().size();

        return stats;
    }

    private static Vector3d getGlobalAnchor(
            GlobalBoundaryAnchors globalBoundaryAnchors,
            Vector3i cellIndex,
            Vector3d vertexPosition,
            boolean isFrontier,
            boolean lockX,
            boolean lockY
    ) {
        if (!isFrontier
                || globalBoundaryAnchors == null
                || cellIndex == null
                || vertexPosition == null) {
            return null;
        }

        Vector3d globalAnchor =
                globalBoundaryAnchors.getAverage(
                        cellIndex
                );

        if (globalAnchor == null) {
            return null;
        }

        /*
         * In a thin scene, sharing the same grid cell is not enough
         * to establish that a vertex belongs to the anchor.
         *
         * The vertex must also coincide with the anchor along every
         * protected axis.
         */
        if (lockX
                && Math.abs(
                vertexPosition.x
                        - globalAnchor.x
        ) > DEBUG_ANCHOR_AXIS_TOLERANCE) {

            return null;
        }

        if (lockY
                && Math.abs(
                vertexPosition.y
                        - globalAnchor.y
        ) > DEBUG_ANCHOR_AXIS_TOLERANCE) {

            return null;
        }

        return globalAnchor;
    }

    private static Vector3d getGlobalAnchor_original(
            GlobalBoundaryAnchors globalBoundaryAnchors,
            Vector3i cellIndex,
            boolean isFrontier
    ) {
        if (!isFrontier
                || globalBoundaryAnchors == null
                || cellIndex == null) {
            return null;
        }

        return globalBoundaryAnchors.getAverage(
                cellIndex
        );
    }

    private static LocalClusterKey createLocalClusterKey(
            Vector3i cellIndex,
            Vector3d position,
            boolean lockX,
            boolean lockY
    ) {
        long lockedX =
                lockX
                        ? quantizeLockedCoordinate(
                        position.x
                )
                        : 0L;

        long lockedY =
                lockY
                        ? quantizeLockedCoordinate(
                        position.y
                )
                        : 0L;

        return new LocalClusterKey(
                cellIndex.x,
                cellIndex.y,
                cellIndex.z,
                lockedX,
                lockedY
        );
    }

    private static long quantizeLockedCoordinate(
            double coordinate
    ) {
        if (!Double.isFinite(coordinate)) {
            return 0L;
        }

        return Math.round(
                coordinate
                        / LOCK_COORDINATE_TOLERANCE
        );
    }

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

        if (surfaces == null) {
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
                            || oldIndex
                            >= oldIndexToNewIndex.length) {
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

    private static void removeUnusedVerticesAndReindex(
            GaiaPrimitive primitive
    ) {
        if (primitive == null
                || primitive.getVertices() == null
                || primitive.getVertices().isEmpty()) {
            return;
        }

        List<GaiaVertex> vertices =
                primitive.getVertices();

        int vertexCount =
                vertices.size();

        boolean[] usedVertices =
                new boolean[vertexCount];

        List<GaiaSurface> surfaces =
                primitive.getSurfaces();

        if (surfaces == null) {
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

                for (int index : face.getIndices()) {
                    if (index >= 0
                            && index < vertexCount) {
                        usedVertices[index] =
                                true;
                    }
                }
            }
        }

        int[] oldToCompactIndex =
                new int[vertexCount];

        Arrays.fill(
                oldToCompactIndex,
                -1
        );

        List<GaiaVertex> compactedVertices =
                new ArrayList<>();

        for (int oldIndex = 0;
             oldIndex < vertexCount;
             oldIndex++) {

            if (!usedVertices[oldIndex]) {
                continue;
            }

            oldToCompactIndex[oldIndex] =
                    compactedVertices.size();

            compactedVertices.add(
                    vertices.get(oldIndex)
            );
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
                            || oldIndex >= vertexCount) {
                        continue;
                    }

                    int compactIndex =
                            oldToCompactIndex[oldIndex];

                    if (compactIndex >= 0) {
                        indices[i] =
                                compactIndex;
                    }
                }
            }
        }

        vertices.clear();

        vertices.addAll(
                compactedVertices
        );
    }

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

        boolean firstVertex =
                true;

        for (GaiaPrimitive primitive : primitives) {
            if (primitive == null
                    || primitive.getVertices() == null) {
                continue;
            }

            for (GaiaVertex vertex
                    : primitive.getVertices()) {

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

                if (firstVertex) {
                    sceneMinCellIndex.set(
                            cellIndex
                    );

                    sceneMaxCellIndex.set(
                            cellIndex
                    );

                    firstVertex =
                            false;

                    continue;
                }

                sceneMinCellIndex.x =
                        Math.min(
                                sceneMinCellIndex.x,
                                cellIndex.x
                        );

                sceneMinCellIndex.y =
                        Math.min(
                                sceneMinCellIndex.y,
                                cellIndex.y
                        );

                sceneMinCellIndex.z =
                        Math.min(
                                sceneMinCellIndex.z,
                                cellIndex.z
                        );

                sceneMaxCellIndex.x =
                        Math.max(
                                sceneMaxCellIndex.x,
                                cellIndex.x
                        );

                sceneMaxCellIndex.y =
                        Math.max(
                                sceneMaxCellIndex.y,
                                cellIndex.y
                        );

                sceneMaxCellIndex.z =
                        Math.max(
                                sceneMaxCellIndex.z,
                                cellIndex.z
                        );
            }
        }
    }

    private static void copyVertexAttributes(
            GaiaVertex source,
            GaiaVertex destination
    ) {
        if (source == null
                || destination == null) {
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

    private record LocalClusterKey(
            int cellX,
            int cellY,
            int cellZ,
            long lockedX,
            long lockedY
    ) {
    }

    private static final class CellAccumulator {

        private final Vector3d positionSum =
                new Vector3d();

        private final Vector3d representativePosition =
                new Vector3d();

        private int count;

        private void add(
                Vector3d position
        ) {
            if (position == null) {
                return;
            }

            if (count == 0) {
                representativePosition.set(
                        position
                );
            }

            positionSum.add(
                    position
            );

            count++;
        }

        private int getCount() {
            return count;
        }

        private Vector3d calculateTargetPosition(
                boolean lockX,
                boolean lockY
        ) {
            if (count <= 0) {
                return null;
            }

            Vector3d result =
                    new Vector3d(
                            positionSum
                    ).div(count);

            /*
             * Preserve the representative coordinate exactly on
             * thin axes. No cluster movement is allowed there.
             */
            if (lockX) {
                result.x =
                        representativePosition.x;
            }

            if (lockY) {
                result.y =
                        representativePosition.y;
            }

            return result;
        }
    }

    private static final class RemeshStats {

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

            verticesAfterCompaction +=
                    other.verticesAfterCompaction;
        }
    }
}