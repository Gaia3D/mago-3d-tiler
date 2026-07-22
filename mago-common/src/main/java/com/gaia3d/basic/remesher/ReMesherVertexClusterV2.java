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

@Slf4j
public final class ReMesherVertexClusterV2 {

    /*
     * Tolerance used to weld coincident positions while detecting
     * topological frontier vertices.
     */
    private static final double FRONTIER_WELD_TOLERANCE =
            1e-6;

    /*
     * Tolerance used to decide whether a frontier position belongs
     * to one side of nodeBBox or meshBBox.
     *
     * Coordinates are assumed to be expressed in metres.
     */
    private static final double BBOX_SIDE_TOLERANCE =
            1e-4;

    /*
     * Tolerance used to verify that a global anchor really belongs
     * to the node boundary plane requested by the current vertex.
     *
     * This prevents an anchor from another cut plane in the same
     * CellGrid3D cell from being applied accidentally.
     */
    private static final double ANCHOR_PLANE_TOLERANCE =
            1e-3;

    private ReMesherVertexClusterV2() {
    }

    /**
     * Remeshes all primitives of one cut mesh/scene.
     *
     * <p>The two bounding boxes have different meanings:</p>
     *
     * <ul>
     *     <li>
     *         nodeBBox is the bbox of the current LOD node.
     *         For example, 200 x 200 metres at LOD3.
     *     </li>
     *     <li>
     *         meshBBox is the bbox of the current cut mesh fragment.
     *         For example, a remaining strip only 5 metres wide.
     *     </li>
     * </ul>
     *
     * <p>Only frontier vertices touching nodeBBox are allowed to
     * consume a GlobalBoundaryAnchor.</p>
     */
    public static void reMeshScene(
            GaiaScene scene,
            ReMeshParameters reMeshParams,
            GaiaBoundingBox nodeBBox,
            GaiaBoundingBox meshBBox
    ) {
        if (scene == null
                || reMeshParams == null
                || nodeBBox == null
                || meshBBox == null) {
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
         * A null GlobalBoundaryAnchors is allowed.
         *
         * In that case, real node-boundary vertices will remain
         * protected and unchanged.
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

        RemeshStats totalStats =
                new RemeshStats();

        int processedPrimitives =
                0;

        for (GaiaPrimitive primitive : primitives) {
            if (primitive == null) {
                continue;
            }

            RemeshStats primitiveStats =
                    reMeshPrimitive(
                            primitive,
                            cellGrid,
                            globalBoundaryAnchors,
                            nodeBBox,
                            meshBBox
                    );

            totalStats.add(
                    primitiveStats
            );

            processedPrimitives++;
        }

        log.debug(
                "VertexClusterV2: processedPrimitives={}, "
                        + "originalVertices={}, verticesAfterCompaction={}",
                processedPrimitives,
                totalStats.originalVertices,
                totalStats.verticesAfterCompaction
        );

        log.debug(
                "VertexClusterV2 frontiers: total={}, "
                        + "nodeBoundary={}, anchored={}, "
                        + "protectedNodeBoundary={}, "
                        + "protectedNodeCorners={}",
                totalStats.frontierVertices,
                totalStats.nodeBoundaryFrontierVertices,
                totalStats.anchoredFrontierVertices,
                totalStats.protectedNodeBoundaryVertices,
                totalStats.protectedNodeCornerVertices
        );

        log.debug(
                "VertexClusterV2 internal frontiers: total={}, "
                        + "localClustered={}, treatedAsInterior={}, "
                        + "protectedMeshCorners={}",
                totalStats.internalFrontierVertices,
                totalStats.localFrontierVertices,
                totalStats.noMeshClubFrontierVertices,
                totalStats.protectedMeshCornerVertices
        );

        log.debug(
                "VertexClusterV2 anchors: missing={}, "
                        + "rejectedIncompatible={}",
                totalStats.missingNodeBoundaryAnchors,
                totalStats.rejectedIncompatibleAnchors
        );

        log.debug(
                "VertexClusterV2 clusters: interiorCells={}, "
                        + "frontierCells={}, createdAnchored={}, "
                        + "createdFrontier={}, createdInterior={}",
                totalStats.interiorCells,
                totalStats.frontierCells,
                totalStats.createdAnchoredVertices,
                totalStats.createdFrontierVertices,
                totalStats.createdInteriorVertices
        );

        log.debug(
                "VertexClusterV2 result: mappedAnchored={}, "
                        + "mappedFrontier={}, mappedInterior={}, "
                        + "deletedDegeneratedFaces={}",
                totalStats.mappedAnchoredVertices,
                totalStats.mappedFrontierVertices,
                totalStats.mappedInteriorVertices,
                totalStats.deletedDegeneratedFaces
        );
    }

    private static RemeshStats reMeshPrimitive(
            GaiaPrimitive primitive,
            CellGrid3D cellGrid,
            GlobalBoundaryAnchors globalBoundaryAnchors,
            GaiaBoundingBox nodeBBox,
            GaiaBoundingBox meshBBox
    ) {
        RemeshStats stats =
                new RemeshStats();

        if (primitive == null
                || cellGrid == null
                || nodeBBox == null
                || meshBBox == null) {
            return stats;
        }

        List<GaiaVertex> vertices =
                primitive.getVertices();

        if (vertices == null || vertices.isEmpty()) {
            return stats;
        }

        /*
         * Only these original vertices are processed.
         *
         * Clustered vertices appended later must not participate
         * in either pass.
         */
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

        boolean[] frontierVertices =
                frontierFinder.findBoundaryVertices(
                        vertices,
                        faces,
                        FRONTIER_WELD_TOLERANCE,
                        weldedIndices
                );

        if (frontierVertices == null
                || frontierVertices.length < originalVertexCount) {
            return stats;
        }

        /*
         * Ordinary interior clusters.
         *
         * This map is indexed only by the real CellGrid3D cell.
         */
        Map<CellKey, CellAccumulator> interiorAccumulators =
                new HashMap<>();

        /*
         * Internal frontier clusters.
         *
         * The key contains both the real cell and the side of the
         * mesh fragment.
         *
         * Therefore, if the two sides of a narrow strip occupy the
         * same cell, they remain independent:
         *
         *     Cell(-78, y, z) + WEST
         *     Cell(-78, y, z) + EAST
         */
        Map<FrontierClusterKey, CellAccumulator>
                frontierAccumulators =
                new HashMap<>();

        /*
         * Decisions calculated during the first pass.
         *
         * The second pass must reuse these exact decisions rather
         * than classify vertices again.
         */
        VertexMode[] modeByOldIndex =
                new VertexMode[originalVertexCount];

        Arrays.fill(
                modeByOldIndex,
                VertexMode.PROTECTED
        );

        CellKey[] realCellByOldIndex =
                new CellKey[originalVertexCount];

        FrontierClusterKey[] frontierKeyByOldIndex =
                new FrontierClusterKey[originalVertexCount];

        AnchoredClusterKey[] anchoredKeyByOldIndex =
                new AnchoredClusterKey[originalVertexCount];

        Vector3d[] globalAnchorByOldIndex =
                new Vector3d[originalVertexCount];

        /*
         * =========================================================
         * FIRST PASS
         * =========================================================
         *
         * 1. Detect the real grid cell.
         * 2. Determine whether the frontier touches the node bbox.
         * 3. Only real node-boundary frontiers may use anchors.
         * 4. Internal frontiers are classified with meshBBox.
         * 5. Accumulate local clusters.
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

            Vector3i realCellIndex =
                    cellGrid.getCellIndex(
                            position
                    );

            if (realCellIndex == null) {
                continue;
            }

            CellKey realCellKey =
                    CellKey.from(
                            realCellIndex
                    );

            realCellByOldIndex[oldIndex] =
                    realCellKey;

            boolean isFrontier =
                    frontierVertices[oldIndex];

            /*
             * Ordinary interior vertex.
             */
            if (!isFrontier) {
                modeByOldIndex[oldIndex] =
                        VertexMode.INTERIOR;

                interiorAccumulators
                        .computeIfAbsent(
                                realCellKey,
                                ignored -> new CellAccumulator()
                        )
                        .add(
                                position
                        );

                stats.localInteriorVertices++;
                continue;
            }

            stats.frontierVertices++;

            /*
             * nodeClub answers:
             *
             *     Does this frontier belong to a real node cut?
             *
             * meshClub answers:
             *
             *     Which side of the current cut mesh fragment
             *     does this frontier belong to?
             */
            FrontierClub nodeClub =
                    FrontierClub.classify(
                            position,
                            nodeBBox,
                            BBOX_SIDE_TOLERANCE
                    );

            FrontierClub meshClub =
                    FrontierClub.classify(
                            position,
                            meshBBox,
                            BBOX_SIDE_TOLERANCE
                    );

            /*
             * A vertex touching two node sides belongs to a node
             * corner.
             *
             * GlobalBoundaryAnchors is currently indexed only by
             * CellGrid3D cell, so using an anchor at this location
             * could mix two perpendicular cut planes.
             *
             * Protect the original vertex.
             */
            if (nodeClub.isMultiple()) {
                stats.nodeBoundaryFrontierVertices++;
                stats.protectedNodeCornerVertices++;
                continue;
            }

            /*
             * A frontier touching exactly one node side is a real
             * shared boundary between LOD nodes.
             */
            if (nodeClub.isSingle()) {
                stats.nodeBoundaryFrontierVertices++;

                FrontierClub.Side nodeSide =
                        nodeClub.getSingleSide();

                Vector3d globalAnchor =
                        null;

                if (globalBoundaryAnchors != null) {
                    /*
                     * Anchors remain indexed using the real geometric
                     * cell.
                     */
                    globalAnchor =
                            globalBoundaryAnchors.getAverage(
                                    realCellIndex
                            );
                }

                if (globalAnchor == null) {
                    /*
                     * Never cluster a real node boundary independently.
                     *
                     * If the neighbour does something different, an
                     * immediate crack would appear.
                     *
                     * Keeping the original cut vertex is safer.
                     */
                    stats.missingNodeBoundaryAnchors++;
                    stats.protectedNodeBoundaryVertices++;
                    continue;
                }

                if (!isAnchorCompatibleWithNodeSide(
                        globalAnchor,
                        nodeSide,
                        nodeBBox
                )) {
                    /*
                     * An anchor was found in the same cell, but it
                     * does not lie on the expected node plane.
                     *
                     * It may belong to another cut contained in the
                     * same coarse LOD3 cell.
                     */
                    stats.rejectedIncompatibleAnchors++;
                    stats.protectedNodeBoundaryVertices++;
                    continue;
                }

                AnchoredClusterKey anchoredKey =
                        new AnchoredClusterKey(
                                realCellKey,
                                nodeSide
                        );

                modeByOldIndex[oldIndex] =
                        VertexMode.ANCHORED_FRONTIER;

                anchoredKeyByOldIndex[oldIndex] =
                        anchoredKey;

                globalAnchorByOldIndex[oldIndex] =
                        new Vector3d(
                                globalAnchor
                        );

                stats.anchoredFrontierVertices++;
                continue;
            }

            /*
             * From this point, the vertex is a topological frontier
             * located fully inside the node.
             *
             * This is precisely where the internal side of a narrow
             * cut strip should arrive.
             */
            stats.internalFrontierVertices++;

            /*
             * A frontier touching several sides of meshBBox is a
             * corner of the cut fragment.
             *
             * According to the current rule, fragment corners remain
             * unchanged.
             */
            if (meshClub.isMultiple()) {
                stats.protectedMeshCornerVertices++;
                continue;
            }

            /*
             * The internal frontier belongs to exactly one side of
             * the cut fragment.
             *
             * Cluster it using:
             *
             *     real cell + mesh side
             */
            if (meshClub.isSingle()) {
                FrontierClusterKey frontierKey =
                        new FrontierClusterKey(
                                realCellKey,
                                meshClub.getSingleSide()
                        );

                modeByOldIndex[oldIndex] =
                        VertexMode.LOCAL_FRONTIER;

                frontierKeyByOldIndex[oldIndex] =
                        frontierKey;

                frontierAccumulators
                        .computeIfAbsent(
                                frontierKey,
                                ignored -> new CellAccumulator()
                        )
                        .add(
                                position
                        );

                stats.localFrontierVertices++;
                continue;
            }

            /*
             * This is a topological frontier, but it does not belong
             * to any XY side of meshBBox.
             *
             * Treat it like an ordinary interior vertex.
             */
            modeByOldIndex[oldIndex] =
                    VertexMode.INTERIOR;

            interiorAccumulators
                    .computeIfAbsent(
                            realCellKey,
                            ignored -> new CellAccumulator()
                    )
                    .add(
                            position
                    );

            stats.noMeshClubFrontierVertices++;
            stats.localInteriorVertices++;
        }

        stats.frontierCells =
                frontierAccumulators.size();

        stats.interiorCells =
                interiorAccumulators.size();

        /*
         * =========================================================
         * SECOND PASS
         * =========================================================
         *
         * Create one resulting vertex per selected cluster and map
         * original indices to the resulting indices.
         */
        int[] oldIndexToNewIndex =
                new int[originalVertexCount];

        Arrays.fill(
                oldIndexToNewIndex,
                -1
        );

        Map<AnchoredClusterKey, Integer>
                anchoredKeyToNewIndex =
                new HashMap<>();

        Map<FrontierClusterKey, Integer>
                frontierKeyToNewIndex =
                new HashMap<>();

        Map<CellKey, Integer>
                interiorCellToNewIndex =
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

            VertexMode mode =
                    modeByOldIndex[oldIndex];

            if (mode == null
                    || mode == VertexMode.PROTECTED) {
                /*
                 * Keep the original index.
                 */
                continue;
            }

            switch (mode) {
                case ANCHORED_FRONTIER -> {
                    AnchoredClusterKey anchoredKey =
                            anchoredKeyByOldIndex[oldIndex];

                    Vector3d targetPosition =
                            globalAnchorByOldIndex[oldIndex];

                    if (anchoredKey == null
                            || targetPosition == null) {
                        continue;
                    }

                    Integer newIndex =
                            anchoredKeyToNewIndex.get(
                                    anchoredKey
                            );

                    if (newIndex == null) {
                        newIndex =
                                appendClusteredVertex(
                                        vertices,
                                        oldVertex,
                                        targetPosition
                                );

                        anchoredKeyToNewIndex.put(
                                anchoredKey,
                                newIndex
                        );

                        stats.createdAnchoredVertices++;
                    }

                    oldIndexToNewIndex[oldIndex] =
                            newIndex;

                    stats.mappedAnchoredVertices++;
                }

                case LOCAL_FRONTIER -> {
                    FrontierClusterKey frontierKey =
                            frontierKeyByOldIndex[oldIndex];

                    if (frontierKey == null) {
                        continue;
                    }

                    CellAccumulator accumulator =
                            frontierAccumulators.get(
                                    frontierKey
                            );

                    if (accumulator == null
                            || accumulator.getCount() < 2) {
                        /*
                         * A one-vertex cluster is not worth replacing.
                         * Keep the original vertex.
                         */
                        stats.skippedSingleFrontierCluster++;
                        continue;
                    }

                    Vector3d targetPosition =
                            accumulator.calculateAverage();

                    if (targetPosition == null) {
                        continue;
                    }

                    Integer newIndex =
                            frontierKeyToNewIndex.get(
                                    frontierKey
                            );

                    if (newIndex == null) {
                        newIndex =
                                appendClusteredVertex(
                                        vertices,
                                        oldVertex,
                                        targetPosition
                                );

                        frontierKeyToNewIndex.put(
                                frontierKey,
                                newIndex
                        );

                        stats.createdFrontierVertices++;
                    }

                    oldIndexToNewIndex[oldIndex] =
                            newIndex;

                    stats.mappedFrontierVertices++;
                }

                case INTERIOR -> {
                    CellKey realCellKey =
                            realCellByOldIndex[oldIndex];

                    if (realCellKey == null) {
                        continue;
                    }

                    CellAccumulator accumulator =
                            interiorAccumulators.get(
                                    realCellKey
                            );

                    if (accumulator == null
                            || accumulator.getCount() < 2) {
                        /*
                         * Leave single interior vertices unchanged.
                         */
                        stats.skippedSingleInteriorCluster++;
                        continue;
                    }

                    Vector3d targetPosition =
                            accumulator.calculateAverage();

                    if (targetPosition == null) {
                        continue;
                    }

                    Integer newIndex =
                            interiorCellToNewIndex.get(
                                    realCellKey
                            );

                    if (newIndex == null) {
                        newIndex =
                                appendClusteredVertex(
                                        vertices,
                                        oldVertex,
                                        targetPosition
                                );

                        interiorCellToNewIndex.put(
                                realCellKey,
                                newIndex
                        );

                        stats.createdInteriorVertices++;
                    }

                    oldIndexToNewIndex[oldIndex] =
                            newIndex;

                    stats.mappedInteriorVertices++;
                }

                default -> {
                    /*
                     * Defensive case.
                     */
                }
            }
        }

        replaceFaceIndices(
                primitive,
                oldIndexToNewIndex
        );

        int facesBeforeDeletingDegenerated =
                countFaces(
                        primitive
                );

        primitive.deleteDegeneratedFaces();

        int facesAfterDeletingDegenerated =
                countFaces(
                        primitive
                );

        if (facesAfterDeletingDegenerated
                < facesBeforeDeletingDegenerated) {

            stats.deletedDegeneratedFaces =
                    facesBeforeDeletingDegenerated
                            - facesAfterDeletingDegenerated;
        }

        removeUnusedVerticesAndReindex(
                primitive
        );

        List<GaiaVertex> resultingVertices =
                primitive.getVertices();

        stats.verticesAfterCompaction =
                resultingVertices == null
                        ? 0
                        : resultingVertices.size();

        return stats;
    }

    /**
     * Verifies that the anchor found by cell index lies on the same
     * node boundary plane as the current frontier.
     */
    private static boolean isAnchorCompatibleWithNodeSide(
            Vector3d anchor,
            FrontierClub.Side nodeSide,
            GaiaBoundingBox nodeBBox
    ) {
        if (anchor == null
                || nodeSide == null
                || nodeBBox == null) {
            return false;
        }

        double actualCoordinate;
        double expectedCoordinate;

        switch (nodeSide) {
            case WEST -> {
                actualCoordinate =
                        anchor.x;

                expectedCoordinate =
                        nodeBBox.getMinX();
            }

            case EAST -> {
                actualCoordinate =
                        anchor.x;

                expectedCoordinate =
                        nodeBBox.getMaxX();
            }

            case SOUTH -> {
                actualCoordinate =
                        anchor.y;

                expectedCoordinate =
                        nodeBBox.getMinY();
            }

            case NORTH -> {
                actualCoordinate =
                        anchor.y;

                expectedCoordinate =
                        nodeBBox.getMaxY();
            }

            default -> {
                return false;
            }
        }

        if (!Double.isFinite(actualCoordinate)
                || !Double.isFinite(expectedCoordinate)) {
            return false;
        }

        return Math.abs(
                actualCoordinate
                        - expectedCoordinate
        ) <= ANCHOR_PLANE_TOLERANCE;
    }

    /**
     * Adds a clustered vertex to the primitive vertex list.
     */
    private static int appendClusteredVertex(
            List<GaiaVertex> vertices,
            GaiaVertex sourceVertex,
            Vector3d targetPosition
    ) {
        GaiaVertex newVertex =
                new GaiaVertex();

        newVertex.setPosition(
                new Vector3d(
                        targetPosition
                )
        );

        copyVertexAttributes(
                sourceVertex,
                newVertex
        );

        int newIndex =
                vertices.size();

        vertices.add(
                newVertex
        );

        return newIndex;
    }

    /**
     * Returns the current number of faces in a primitive.
     */
    private static int countFaces(
            GaiaPrimitive primitive
    ) {
        if (primitive == null) {
            return 0;
        }

        List<GaiaFace> faces =
                primitive.extractGaiaAllFaces(
                        null
                );

        return faces == null
                ? 0
                : faces.size();
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
     * Removes vertices not referenced by any face and rewrites all
     * indices to use the compacted vertex list.
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

        int usedVertexCount =
                0;

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
     * Copies non-positional attributes from the source vertex.
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
     * Processing decision for one original vertex.
     */
    private enum VertexMode {

        /*
         * Keep the original position and index.
         */
        PROTECTED,

        /*
         * Cluster using the real CellGrid3D cell.
         */
        INTERIOR,

        /*
         * Cluster using:
         *
         *     real cell + mesh frontier side.
         */
        LOCAL_FRONTIER,

        /*
         * Move to a globally locked anchor.
         */
        ANCHORED_FRONTIER
    }

    /**
     * Immutable representation of a CellGrid3D index.
     *
     * This avoids accidentally modifying a Vector3i after inserting
     * it into a HashMap.
     */
    private record CellKey(
            int x,
            int y,
            int z
    ) {

        private static CellKey from(
                Vector3i cellIndex
        ) {
            if (cellIndex == null) {
                return null;
            }

            return new CellKey(
                    cellIndex.x,
                    cellIndex.y,
                    cellIndex.z
            );
        }
    }

    /**
     * Identity of an internal frontier cluster.
     *
     * Opposite sides of a narrow strip remain separate even when
     * both belong to the same real CellGrid3D cell.
     */
    private record FrontierClusterKey(
            CellKey cell,
            FrontierClub.Side meshSide
    ) {
    }

    /**
     * Identity of an anchored node-boundary cluster.
     *
     * The side is included so two perpendicular node sides are never
     * merged merely because they occupy the same grid cell.
     */
    private record AnchoredClusterKey(
            CellKey cell,
            FrontierClub.Side nodeSide
    ) {
    }

    /**
     * Accumulates vertex positions for one local cluster.
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
        private long verticesAfterCompaction;

        private long frontierVertices;

        private long nodeBoundaryFrontierVertices;
        private long anchoredFrontierVertices;
        private long protectedNodeBoundaryVertices;
        private long protectedNodeCornerVertices;
        private long missingNodeBoundaryAnchors;
        private long rejectedIncompatibleAnchors;

        private long internalFrontierVertices;
        private long localFrontierVertices;
        private long noMeshClubFrontierVertices;
        private long protectedMeshCornerVertices;

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

        private long deletedDegeneratedFaces;

        private void add(
                RemeshStats other
        ) {
            if (other == null) {
                return;
            }

            originalVertices +=
                    other.originalVertices;

            verticesAfterCompaction +=
                    other.verticesAfterCompaction;

            frontierVertices +=
                    other.frontierVertices;

            nodeBoundaryFrontierVertices +=
                    other.nodeBoundaryFrontierVertices;

            anchoredFrontierVertices +=
                    other.anchoredFrontierVertices;

            protectedNodeBoundaryVertices +=
                    other.protectedNodeBoundaryVertices;

            protectedNodeCornerVertices +=
                    other.protectedNodeCornerVertices;

            missingNodeBoundaryAnchors +=
                    other.missingNodeBoundaryAnchors;

            rejectedIncompatibleAnchors +=
                    other.rejectedIncompatibleAnchors;

            internalFrontierVertices +=
                    other.internalFrontierVertices;

            localFrontierVertices +=
                    other.localFrontierVertices;

            noMeshClubFrontierVertices +=
                    other.noMeshClubFrontierVertices;

            protectedMeshCornerVertices +=
                    other.protectedMeshCornerVertices;

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

            deletedDegeneratedFaces +=
                    other.deletedDegeneratedFaces;
        }
    }
}