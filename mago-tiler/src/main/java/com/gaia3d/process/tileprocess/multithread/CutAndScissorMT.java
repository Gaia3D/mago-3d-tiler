package com.gaia3d.process.tileprocess.multithread;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.entities.GaiaAAPlane;
import com.gaia3d.basic.geometry.modifier.topology.GaiaExtractor;
import com.gaia3d.basic.geometry.modifier.topology.GaiaTriangulator;
import com.gaia3d.basic.geometry.modifier.topology.GaiaWeldOptions;
import com.gaia3d.basic.geometry.modifier.topology.GaiaWelder;
import com.gaia3d.basic.geometry.octree.GaiaOctree;
import com.gaia3d.basic.geometry.octree.HalfEdgeOctreeFaces;
import com.gaia3d.basic.halfedge.*;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.remesher.CellGrid3D;
import com.gaia3d.basic.remesher.GaiaFrontierFinder;
import com.gaia3d.basic.remesher.GlobalBoundaryAnchors;
import com.gaia3d.basic.remesher.GlobalBoundaryAnchorsBuilder;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.command.mago.GlobalConstants;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.process.tileprocess.tile.tileset.node.BoundingVolume;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import com.gaia3d.util.GlobeUtils;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;



@Slf4j
public class CutAndScissorMT {
    public final GlobalOptions globalOptions = GlobalOptions.getInstance();

    private final int threadCount;

    public CutAndScissorMT() {
        this(2);
    }

    public CutAndScissorMT(int threadCount) {
        this.threadCount = Math.max(1, threadCount);
    }

    private static void mergeResults(
            Map<Integer, List<TileInfo>> destination,
            Map<Integer, List<TileInfo>> source
    ) {
        if (destination == null
                || source == null
                || source.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, List<TileInfo>> entry : source.entrySet()) {
            if (entry == null
                    || entry.getKey() == null
                    || entry.getValue() == null
                    || entry.getValue().isEmpty()) {
                continue;
            }

            destination
                    .computeIfAbsent(
                            entry.getKey(),
                            ignored -> new ArrayList<>()
                    )
                    .addAll(
                            entry.getValue()
                    );
        }
    }

    private static void mergePlaneCutResults(
            Map<Integer, PlaneCutResult> destination,
            Map<Integer, PlaneCutResult> source
    ) {
        if (destination == null
                || source == null
                || source.isEmpty()) {
            return;
        }

        source.forEach((lod, sourceResult) -> {
            if (lod == null
                    || sourceResult == null
                    || sourceResult.isEmpty()) {
                return;
            }

            PlaneCutResult destinationResult =
                    destination.computeIfAbsent(
                            lod,
                            ignored -> new PlaneCutResult()
                    );

            destinationResult.add(sourceResult);
        });
    }

    public CutAndScissorResult apply(
            List<TileInfo> tileInfos,
            Node rootNode,
            int projectMaxDepthIdx,
            GaiaBoundingBox rootNodeBBoxLC
    ) {
        Map<Integer, List<TileInfo>> resultsByLod =
                new HashMap<>();

        Map<Integer, PlaneCutResult> planeCutResultsByLod =
                new HashMap<>();

        Map<Integer, List<FrontierCandidate>>
                frontierCandidatesByLod =
                new HashMap<>();

        Map<Integer, LodBoundaryAnchors>
                boundaryAnchorsByLod =
                new HashMap<>();

        if (tileInfos == null
                || tileInfos.isEmpty()
                || rootNode == null
                || rootNodeBBoxLC == null) {

            return new CutAndScissorResult(
                    resultsByLod,
                    boundaryAnchorsByLod
            );
        }

        int validTileCount = 0;

        for (TileInfo tileInfo : tileInfos) {
            if (tileInfo != null
                    && tileInfo.getTempPath() != null) {

                validTileCount++;
            }
        }

        if (validTileCount == 0) {
            return new CutAndScissorResult(
                    resultsByLod,
                    boundaryAnchorsByLod
            );
        }

        int realThreadCount =
                Math.min(
                        threadCount,
                        validTileCount
                );

        log.info(
                "Cutting and Scissor process started. "
                        + "Tiles: {}, threads: {}",
                validTileCount,
                realThreadCount
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        realThreadCount
                );

        CompletionService<CutTaskResult> completionService =
                new ExecutorCompletionService<>(
                        executor
                );

        int submittedTasks = 0;
        int nextSourceTileId = 0;

        for (TileInfo tileInfo : tileInfos) {
            if (tileInfo == null
                    || tileInfo.getTempPath() == null) {
                continue;
            }

            int sourceTileId =
                    nextSourceTileId++;

            completionService.submit(
                    () -> processSingleTile(
                            tileInfo,
                            sourceTileId,
                            rootNode,
                            projectMaxDepthIdx
                    )
            );

            submittedTasks++;
        }

        executor.shutdown();

        try {
            for (int i = 0; i < submittedTasks; i++) {
                Future<CutTaskResult> future =
                        completionService.take();

                CutTaskResult taskResult =
                        future.get();

                if (taskResult != null) {
                    mergeResults(
                            resultsByLod,
                            taskResult.tileInfosByLod()
                    );

                    mergePlaneCutResults(
                            planeCutResultsByLod,
                            taskResult.planeCutResultsByLod()
                    );

                    mergeFrontierCandidates(
                            frontierCandidatesByLod,
                            taskResult.frontierCandidatesByLod()
                    );
                }

                log.info(
                        "Cut and Scissor completed: {} / {}",
                        i + 1,
                        submittedTasks
                );
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();

            throw new RuntimeException(
                    "Cut and Scissor process interrupted",
                    e
            );

        } catch (ExecutionException e) {
            executor.shutdownNow();

            Throwable cause =
                    e.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw new RuntimeException(
                    "Cut and Scissor worker failed",
                    cause
            );
        }

        /*
         * At this point, all workers have finished.
         * Anchor calculation is entirely sequential.
         */
        boundaryAnchorsByLod =
                buildGlobalBoundaryAnchorsByLod(
                        planeCutResultsByLod,
                        frontierCandidatesByLod,
                        resultsByLod,
                        projectMaxDepthIdx,
                        rootNodeBBoxLC
                );

        return new CutAndScissorResult(
                resultsByLod,
                boundaryAnchorsByLod
        );
    }

    private static final class FrontierAccumulator {

        private final Vector3d positionSum =
                new Vector3d();

        private final Set<Integer> tileIds =
                new HashSet<>();

        private int pointCount;

        public void add(
                Vector3d position,
                int tileId
        ) {
            if (position == null) {
                return;
            }

            positionSum.add(position);
            pointCount++;

            tileIds.add(tileId);
        }

        public boolean isShared() {
            return tileIds.size() >= 2;
        }

        public Vector3d calculateAverage() {
            if (pointCount <= 0) {
                return null;
            }

            return new Vector3d(positionSum)
                    .div(pointCount);
        }
    }

    private static void mergeFrontierCandidates(
            Map<Integer, List<FrontierCandidate>> destination,
            Map<Integer, List<FrontierCandidate>> source
    ) {
        if (destination == null
                || source == null
                || source.isEmpty()) {
            return;
        }

        source.forEach((lod, candidates) -> {
            if (lod == null
                    || candidates == null
                    || candidates.isEmpty()) {
                return;
            }

            destination
                    .computeIfAbsent(
                            lod,
                            ignored -> new ArrayList<>()
                    )
                    .addAll(candidates);
        });
    }

    private static final boolean DEBUG_ADD_SHARED_FRONTIER_ANCHORS =
            false;

//    private Map<Integer, LodBoundaryAnchors>
//    buildGlobalBoundaryAnchorsByLod(
//            Map<Integer, PlaneCutResult> planeCutResultsByLod,
//            Map<Integer, List<FrontierCandidate>>
//                    frontierCandidatesByLod,
//            Map<Integer, List<TileInfo>> tileInfosByLod,
//            int maxDepth,
//            GaiaBoundingBox rootNodeBBoxLC
//    ) {
//        Map<Integer, LodBoundaryAnchors> result =
//                new HashMap<>();
//
//        if (tileInfosByLod == null
//                || tileInfosByLod.isEmpty()
//                || rootNodeBBoxLC == null) {
//            return result;
//        }
//
//        for (Map.Entry<Integer, List<TileInfo>> lodEntry
//                : tileInfosByLod.entrySet()) {
//
//            Integer lod =
//                    lodEntry.getKey();
//
//            List<TileInfo> lodTileInfos =
//                    lodEntry.getValue();
//
//            if (lod == null
//                    || lodTileInfos == null
//                    || lodTileInfos.isEmpty()) {
//                continue;
//            }
//
//            CellGrid3D cellGrid =
//                    createReMeshCellGridForLod(
//                            lod,
//                            maxDepth,
//                            rootNodeBBoxLC,
//                            lodTileInfos
//                    );
//
//            GlobalBoundaryAnchorsBuilder builder =
//                    new GlobalBoundaryAnchorsBuilder(
//                            cellGrid,
//                            1e-4
//                    );
//
//            PlaneCutResult planeCutResult =
//                    planeCutResultsByLod == null
//                            ? null
//                            : planeCutResultsByLod.get(lod);
//
//            int cuttingPointCount =
//                    0;
//
//            if (planeCutResult != null
//                    && !planeCutResult.isEmpty()
//                    && planeCutResult.getCuttingPoints() != null) {
//
//                builder.addPoints(
//                        planeCutResult.getCuttingPoints()
//                );
//
//                cuttingPointCount =
//                        planeCutResult.getCuttingPoints()
//                                .size();
//            }
//
//            /*
//             * Build the initial anchors using only cutting points.
//             */
//            GlobalBoundaryAnchors globalAnchors =
//                    builder.build();
//
//            List<FrontierCandidate> frontierCandidates =
//                    frontierCandidatesByLod == null
//                            ? null
//                            : frontierCandidatesByLod.get(lod);
//
//            int frontierCandidateCount =
//                    frontierCandidates == null
//                            ? 0
//                            : frontierCandidates.size();
//
//            int sharedFrontierAnchors =
//                    0;
//
//            /*
//             * Diagnostic switch:
//             *
//             * false:
//             *     GlobalBoundaryAnchors contains only cutting anchors.
//             *
//             * true:
//             *     Shared frontiers are also converted into global
//             *     anchors.
//             */
//            if (DEBUG_ADD_SHARED_FRONTIER_ANCHORS
//                    && frontierCandidates != null
//                    && !frontierCandidates.isEmpty()) {
//
//                Map<Vector3i, FrontierAccumulator>
//                        frontierAccumulators =
//                        new HashMap<>();
//
//                for (FrontierCandidate candidate
//                        : frontierCandidates) {
//
//                    if (candidate == null
//                            || candidate.position() == null) {
//                        continue;
//                    }
//
//                    Vector3i candidateCellIndex =
//                            cellGrid.getCellIndex(
//                                    candidate.position()
//                            );
//
//                    if (candidateCellIndex == null) {
//                        continue;
//                    }
//
//                    Vector3i cellIndex =
//                            new Vector3i(
//                                    candidateCellIndex
//                            );
//
//                    /*
//                     * Cutting anchors have priority.
//                     */
//                    if (globalAnchors.hasAverage(
//                            cellIndex
//                    )) {
//                        continue;
//                    }
//
//                    frontierAccumulators
//                            .computeIfAbsent(
//                                    cellIndex,
//                                    ignored ->
//                                            new FrontierAccumulator()
//                            )
//                            .add(
//                                    candidate.position(),
//                                    candidate.sourceTileId()
//                            );
//                }
//
//                for (Map.Entry<Vector3i, FrontierAccumulator> entry
//                        : frontierAccumulators.entrySet()) {
//
//                    FrontierAccumulator accumulator =
//                            entry.getValue();
//
//                    if (accumulator == null
//                            || !accumulator.isShared()) {
//                        continue;
//                    }
//
//                    Vector3d average =
//                            accumulator.calculateAverage();
//
//                    if (average == null) {
//                        continue;
//                    }
//
//                    boolean inserted =
//                            globalAnchors.putIfAbsent(
//                                    entry.getKey(),
//                                    average
//                            );
//
//                    if (inserted) {
//                        sharedFrontierAnchors++;
//                    }
//                }
//            }
//
//            result.put(
//                    lod,
//                    new LodBoundaryAnchors(
//                            cellGrid,
//                            globalAnchors
//                    )
//            );
//
//            log.info(
//                    "Global boundary anchors built. "
//                            + "LOD={}, cuttingPoints={}, "
//                            + "frontierCandidates={}, "
//                            + "addSharedFrontiers={}, "
//                            + "sharedFrontierAnchors={}, "
//                            + "anchors={}",
//                    lod,
//                    cuttingPointCount,
//                    frontierCandidateCount,
//                    DEBUG_ADD_SHARED_FRONTIER_ANCHORS,
//                    sharedFrontierAnchors,
//                    globalAnchors.size()
//            );
//        }
//
//        return result;
//    }

    private Map<Integer, LodBoundaryAnchors>
    buildGlobalBoundaryAnchorsByLod(
            Map<Integer, PlaneCutResult> planeCutResultsByLod,
            Map<Integer, List<FrontierCandidate>>
                    frontierCandidatesByLod,
            Map<Integer, List<TileInfo>> tileInfosByLod,
            int maxDepth,
            GaiaBoundingBox rootNodeBBoxLC
    ) {
        Map<Integer, LodBoundaryAnchors> result =
                new HashMap<>();

        if (tileInfosByLod == null
                || tileInfosByLod.isEmpty()
                || rootNodeBBoxLC == null) {
            return result;
        }

        for (Map.Entry<Integer, List<TileInfo>> lodEntry
                : tileInfosByLod.entrySet()) {

            Integer lod =
                    lodEntry.getKey();

            List<TileInfo> lodTileInfos =
                    lodEntry.getValue();

            if (lod == null
                    || lodTileInfos == null
                    || lodTileInfos.isEmpty()) {
                continue;
            }

            CellGrid3D cellGrid =
                    createReMeshCellGridForLod(
                            lod,
                            maxDepth,
                            rootNodeBBoxLC,
                            lodTileInfos
                    );

            GlobalBoundaryAnchorsBuilder builder =
                    new GlobalBoundaryAnchorsBuilder(
                            cellGrid,
                            1e-4
                    );

            PlaneCutResult planeCutResult =
                    planeCutResultsByLod == null
                            ? null
                            : planeCutResultsByLod.get(lod);

            int cuttingPointCount = 0;

            if (planeCutResult != null
                    && !planeCutResult.isEmpty()
                    && planeCutResult.getCuttingPoints() != null) {

                builder.addPoints(
                        planeCutResult.getCuttingPoints()
                );

                cuttingPointCount =
                        planeCutResult.getCuttingPoints()
                                .size();
            }

            /*
             * Cutting anchors have priority.
             */
            GlobalBoundaryAnchors globalAnchors =
                    builder.build();

            List<FrontierCandidate> frontierCandidates =
                    frontierCandidatesByLod == null
                            ? null
                            : frontierCandidatesByLod.get(lod);

            Map<Vector3i, FrontierAccumulator>
                    frontierAccumulators =
                    new HashMap<>();

            if (frontierCandidates != null) {
                for (FrontierCandidate candidate
                        : frontierCandidates) {

                    if (candidate == null
                            || candidate.position() == null) {
                        continue;
                    }

                    Vector3i cellIndex =
                            cellGrid.getCellIndex(
                                    candidate.position()
                            );

                    if (cellIndex == null) {
                        continue;
                    }

                    /*
                     * Never replace an anchor generated by a cut.
                     */
                    if (globalAnchors.hasAverage(
                            cellIndex
                    )) {
                        continue;
                    }

                    frontierAccumulators
                            .computeIfAbsent(
                                    new Vector3i(cellIndex),
                                    ignored ->
                                            new FrontierAccumulator()
                            )
                            .add(
                                    candidate.position(),
                                    candidate.sourceTileId()
                            );
                }
            }

            int sharedFrontierAnchors = 0;

            for (Map.Entry<Vector3i, FrontierAccumulator> entry
                    : frontierAccumulators.entrySet()) {

                FrontierAccumulator accumulator =
                        entry.getValue();

                if (accumulator == null
                        || !accumulator.isShared()) {
                    continue;
                }

                Vector3d average =
                        accumulator.calculateAverage();

                if (average == null) {
                    continue;
                }

                boolean inserted =
                        globalAnchors.putIfAbsent(
                                entry.getKey(),
                                average
                        );

                if (inserted) {
                    sharedFrontierAnchors++;
                }
            }

            result.put(
                    lod,
                    new LodBoundaryAnchors(
                            cellGrid,
                            globalAnchors
                    )
            );

            log.info(
                    "Global boundary anchors built. "
                            + "LOD={}, cuttingPoints={}, "
                            + "frontierCandidates={}, "
                            + "sharedFrontierAnchors={}, anchors={}",
                    lod,
                    cuttingPointCount,
                    frontierCandidates == null
                            ? 0
                            : frontierCandidates.size(),
                    sharedFrontierAnchors,
                    globalAnchors.size()
            );
        }

        return result;
    }

    protected CellGrid3D createReMeshCellGridForLod(
            int lod,
            int maxDepth,
            GaiaBoundingBox rootNodeBBoxLC,
            List<TileInfo> tileInfos) {

        double maxSize = rootNodeBBoxLC.getMaxSize();

        if (lod > 0) {
            maxSize = maxSize / Math.pow(2, maxDepth - lod);
        }

        double cellSize = maxSize / 26.0;

        double averageBBoxMinSize = calculateAverageBBoxMinSize(tileInfos);

        double factor = 0.9;
        if (cellSize > averageBBoxMinSize * factor) {
            cellSize = averageBBoxMinSize * factor;
        }

        Vector3d cellGridOrigin = new Vector3d(0.0, 0.0, 0.0);
        return new CellGrid3D(cellGridOrigin, cellSize);
    }

    protected double calculateAverageBBoxMinSize(List<TileInfo> tileInfos) {
        double bboxMinSize = Double.MAX_VALUE;
        double averageBBoxMinSize = 0.0;
        int tileInfosCount = tileInfos.size();
        for (int i = 0; i < tileInfosCount; i++) {
            TileInfo tileInfo = tileInfos.get(i);
            GaiaBoundingBox bbox = tileInfo.getBoundingBox();
            double tileMinSizeX = bbox.getSizeX();
            double tileMinSizeY = bbox.getSizeY();

            if (tileMinSizeX < tileMinSizeY) {
                bboxMinSize = tileMinSizeX;
            } else {
                bboxMinSize = tileMinSizeY;
            }

            averageBBoxMinSize += bboxMinSize;
        }

        return averageBBoxMinSize / (double) tileInfosCount;
    }

    /**
     * Converts PlaneCutResult cutting points from source tile-local
     * coordinates to the root-local coordinate system used by CellGrid3D.
     *
     * This method intentionally mutates the PlaneCutResult in place because
     * PlaneCutResult is only used afterwards for global anchor accumulation.
     */
    private static PlaneCutResult translatedPlaneCutResultToCellGridCoordinates(
            PlaneCutResult source,
            Vector3d scenePositionRelToCellGrid
    ) {
        if (source == null) {
            return new PlaneCutResult();
        }

        return source.translated(
                scenePositionRelToCellGrid
        );
    }

    private CutTaskResult processSingleTile(
            TileInfo tileInfo,
            int sourceTileId,
            Node rootNode,
            int projectMaxDepthIdx
    ) {
        int initialLod =
                Math.min(
                        7,
                        projectMaxDepthIdx
                );

        BoundingVolume rootBoundingVolumeCopy =
                new BoundingVolume(
                        rootNode.getBoundingVolume()
                );

        Map<Integer, List<TileInfo>> localResults =
                new HashMap<>();

        Map<Integer, PlaneCutResult>
                localPlaneCutResultsByLod =
                new HashMap<>();

        Map<Integer, List<FrontierCandidate>>
                localFrontierCandidatesByLod =
                new HashMap<>();

        Vector3d scenePositionRelToCellGrid =
                calculateScenePositionRelToCellGrid(
                        tileInfo,
                        rootNode
                );

        cutRectangleCakeAllLod(
                tileInfo,
                initialLod,
                rootBoundingVolumeCopy,
                projectMaxDepthIdx,
                sourceTileId,
                scenePositionRelToCellGrid,
                localResults,
                localPlaneCutResultsByLod,
                localFrontierCandidatesByLod
        );

        return new CutTaskResult(
                tileInfo.getTempPath().toString(),
                localResults,
                localPlaneCutResultsByLod,
                localFrontierCandidatesByLod
        );
    }

    private Vector3d calculateScenePositionRelToCellGrid(
            TileInfo tileInfo,
            Node rootNode
    ) {
        if (tileInfo == null
                || tileInfo.getTileTransformInfo() == null
                || tileInfo.getTileTransformInfo().getPosition() == null
                || rootNode == null) {

            return new Vector3d();
        }

        Vector3d tileGeoCoord =
                tileInfo.getTileTransformInfo()
                        .getPosition();

        Vector3d tilePositionWorld =
                GlobeUtils.geographicToCartesianWgs84(
                        tileGeoCoord
                );

        Matrix4d rootTransform =
                rootNode.getTransformMatrix();

        if (rootTransform == null) {
            Vector3d rootCenterRad =
                    rootNode.getBoundingVolume()
                            .calcCenter();

            Vector3d rootCenterDeg =
                    new Vector3d(
                            Math.toDegrees(rootCenterRad.x),
                            Math.toDegrees(rootCenterRad.y),
                            rootCenterRad.z
                    );

            Vector3d rootPositionWorld =
                    GlobeUtils.geographicToCartesianWgs84(
                            rootCenterDeg
                    );

            rootTransform =
                    GlobeUtils.transformMatrixAtCartesianPointWgs84(
                            rootPositionWorld
                    );
        }

        Matrix4d rootTransformInverse =
                new Matrix4d(rootTransform)
                        .invert();

        return rootTransformInverse.transformPosition(
                tilePositionWorld,
                new Vector3d()
        );
    }

    private void cutRectangleCakeAllLod(
            TileInfo tileInfo,
            int lod,
            BoundingVolume rootNodeBoundingVolume,
            int depthIdx,
            int sourceTileId,
            Vector3d scenePositionRelToCellGrid,
            Map<Integer, List<TileInfo>> localResults,
            Map<Integer, PlaneCutResult> localPlaneCutResultsByLod,
            Map<Integer, List<FrontierCandidate>>
                    localFrontierCandidatesByLod
    ) {
        Path path = tileInfo.getTempPath();

        GaiaSet gaiaSet = null;
        GaiaScene scene = null;
        HalfEdgeScene halfEdgeScene = null;

        try {
            gaiaSet = GaiaSet.readFile(path);

            if (gaiaSet == null) {
                return;
            }

            scene = new GaiaScene(gaiaSet);
            gaiaSet.setMaterials(null); // avoid to keep materials in memory, because we will not use them for cutting, and they can consume a lot of memory.
            gaiaSet.clear();
            scene.deleteNormals();

            GaiaTriangulator triangulator = new GaiaTriangulator();

            triangulator.apply(scene);

            GaiaWeldOptions weldOptions = GaiaWeldOptions.builder().error(1e-6).checkTexCoord(true).checkNormal(false).checkColor(false).checkBatchId(false).build();

            GaiaWelder welder = new GaiaWelder(weldOptions);

            welder.apply(scene);

            List<FrontierCandidate> sourceFrontierCandidates =
                    collectFrontierCandidates(
                            scene,
                            sourceTileId,
                            scenePositionRelToCellGrid
                    );

            /*
             * Original source frontiers are valid candidates for every
             * generated LOD.
             */
            for (int currLod = lod;
                 currLod >= 0;
                 currLod--) {

                localFrontierCandidatesByLod
                        .computeIfAbsent(
                                currLod,
                                ignored -> new ArrayList<>()
                        )
                        .addAll(
                                sourceFrontierCandidates
                        );
            }

            halfEdgeScene =
                    HalfEdgeUtils.halfEdgeSceneFromGaiaScene(
                            scene
                    );

            scene.clear();

            boolean scissorTextures = true;
            boolean makeSkirt = GlobalConstants.MAKE_SKIRT;

            Path cutTempPath = Path.of(globalOptions.getTempPath(), "cutTemp");

            /*
             * Thread-safe e idempotente.
             * Mucho mejor que exists() + mkdirs().
             */
            Files.createDirectories(cutTempPath);

            cutHalfEdgeSceneByGaiaAAPlanesAndSaveTileInfosAllLodMode(
                    halfEdgeScene,
                    scissorTextures,
                    makeSkirt,
                    tileInfo,
                    lod,
                    rootNodeBoundingVolume,
                    depthIdx,
                    cutTempPath,
                    scenePositionRelToCellGrid,
                    localResults,
                    localPlaneCutResultsByLod
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed cutting tile: " + path, e);

        } finally {
            if (halfEdgeScene != null) {
                halfEdgeScene.deleteObjects();
            }

            if (scene != null) {
                scene.clear();
            }

            if (gaiaSet != null) {
                gaiaSet.clear();
            }
        }
    }

    public List<TileInfo>
    cutHalfEdgeSceneByGaiaAAPlanesAndSaveTileInfosAllLodMode(
            HalfEdgeScene halfEdgeScene,
            boolean scissorTextures,
            boolean makeSkirt,
            TileInfo motherTileInfo,
            int lod,
            BoundingVolume rootNodeBoundingVolume,
            int projectMaxDepth,
            Path cutTempPath,
            Vector3d scenePositionRelToCellGrid,
            Map<Integer, List<TileInfo>> localResults,
            Map<Integer, PlaneCutResult> localPlaneCutResultsByLod
    ) {
        TileTransformInfo tileTransformInfo = motherTileInfo.getTileTransformInfo();
        Vector3d geoCoordPosition = tileTransformInfo.getPosition();

        List<TileInfo> cutTileInfos = new ArrayList<>();
        List<GaiaAAPlane> planes = new ArrayList<>();

        for (int currLod = lod; currLod >= 0; currLod--) {
            // calculate the AAPlanes to cut.***
            Matrix4d transformMatrix = new Matrix4d();
            GaiaBoundingBox boundingBox = null;
            planes.clear();
            try {
                boundingBox = this.getCuttingPlanesAndLocalBoundingBox(motherTileInfo, currLod, rootNodeBoundingVolume, projectMaxDepth, planes, transformMatrix);
            } catch (Exception e) {
                log.error("[ERROR] calculating cutting planes for LOD " + currLod, e);
                continue;
            }

            HalfEdgeOctreeFaces resultOctree = new HalfEdgeOctreeFaces(null, boundingBox);
            resultOctree.setLimitDepth(projectMaxDepth - currLod);

            double error = 1e-4;
            int planesCount = planes.size();
            boolean cut = false;
            PlaneCutResult planeCutResult = new PlaneCutResult();
            for (int i = 0; i < planesCount; i++) {
                GaiaAAPlane plane = planes.get(i);
                if (halfEdgeScene.cutByPlane(plane.getPlaneType(), plane.getPoint(), error, planeCutResult)) {
                    cut = true;
                }
            }

            if (!planeCutResult.isEmpty()) {
                PlaneCutResult planeCutResultInCellGrid =
                        translatedPlaneCutResultToCellGridCoordinates(
                                planeCutResult,
                                scenePositionRelToCellGrid
                        );

                PlaneCutResult accumulatedResult =
                        localPlaneCutResultsByLod.computeIfAbsent(
                                currLod,
                                ignored -> new PlaneCutResult()
                        );

                accumulatedResult.add(
                        planeCutResultInCellGrid
                );
            }

            halfEdgeScene.deleteDegeneratedFaces();

            // now, distribute faces into octree
            List<HalfEdgeSurface> surfaces = halfEdgeScene.extractSurfaces(null);
            for (HalfEdgeSurface surface : surfaces) {
                List<HalfEdgeFace> faces = surface.getFaces();
                for (HalfEdgeFace face : faces) {
                    if (face.getStatus() == ObjectStatus.DELETED) {
                        continue;
                    }
                    resultOctree.addContent(face);
                }
            }

            resultOctree.distributeFacesToTargetDepth(resultOctree.getLimitDepth());
            List<GaiaOctree<HalfEdgeFace>> octreesWithContents = resultOctree.extractOctreesWithContents();

            // set the classifyId for each face
            int octreesCount = octreesWithContents.size();
            for (int j = 0; j < octreesCount; j++) {
                HalfEdgeOctreeFaces octree = (HalfEdgeOctreeFaces) octreesWithContents.get(j);
                List<HalfEdgeFace> faces = octree.getContents();
                for (HalfEdgeFace face : faces) {
                    face.setClassifyId(j);
                }
            }

            for (int j = 0; j < octreesCount; j++) {
                int classifyId = j;

                // create a new HalfEdgeScene
                HalfEdgeScene cuttedScene = halfEdgeScene.cloneByClassifyId(classifyId);
                if (cuttedScene == null) {
                    log.info("cuttedScene is null");
                    continue;
                }
                cuttedScene.deleteDegeneratedFaces();
                cuttedScene.deleteNoUsedMaterials();
                cuttedScene.removeDeletedObjects();

                if (scissorTextures && cut) {
                    cuttedScene.scissorTexturesByMotherScene(halfEdgeScene.getMaterials());
                }

                GaiaScene gaiaSceneCut = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(cuttedScene);

                List<GaiaMaterial> materials = cuttedScene.getMaterials(); // keep material.
                List<GaiaMaterial> voidMaterials = new ArrayList<>();
                cuttedScene.setMaterials(voidMaterials);
                cuttedScene.deleteObjects();// delete to save memory.

                GaiaBoundingBox boundingBoxCutLC = new GaiaBoundingBox();
                GaiaBoundingBox cartographicBoundingBox = this.calculateCartographicBoundingBox(gaiaSceneCut, transformMatrix, boundingBoxCutLC);

                // create an originalPath for the cut scene
                Path cutScenePath = Paths.get("");
                gaiaSceneCut.setOriginalPath(cutScenePath);

                GaiaSet gaiaSetCut = GaiaSet.fromGaiaScene(gaiaSceneCut);

                // delete the contents of the gaiaSceneCut before create tileInfo**********************
                gaiaSceneCut.getNodes().forEach(GaiaNode::clear); // new 20260420.

                // Save.*****************************************************************************************
                Path cutTempLodPath = cutTempPath.resolve("lod" + currLod);
                if (!cutTempLodPath.toFile().exists() && cutTempLodPath.toFile().mkdirs()) {
                    log.debug("cutTempLod folder created.");
                }

                UUID identifier = UUID.randomUUID();
                Path gaiaSetCutFolderPath = cutTempLodPath.resolve(identifier.toString());
                if (!gaiaSetCutFolderPath.toFile().exists() && gaiaSetCutFolderPath.toFile().mkdirs()) {
                    log.debug("gaiaSetCut folder created.");
                }

                // save the materials
                // save material atlas textures/////////////////////////////////////////////////////
                //Path parentPath = path.getParent();
                Path imagesPath = gaiaSetCutFolderPath.resolve("images");
                // make directories if not exists
                File imagesFolder = imagesPath.toFile();
                if (!imagesFolder.exists() && imagesFolder.mkdirs()) {
                    log.debug("images folder created.");
                }
                for (GaiaMaterial material : materials) {
                    List<GaiaTexture> textures = material.getTextures().get(TextureType.DIFFUSE);
                    for (GaiaTexture texture : textures) {
                        if (texture == null) {
                            continue;
                        }
                        // check if exist bufferedImage of the texture
                        if (texture.getBufferedImage() == null) {
                            // load the image
                            texture.loadImage();
                        }

                        if (texture.getBufferedImage() == null) {
                            continue;
                        }

                        if (currLod > 4) {
                            // resize the texture to half size
                            BufferedImage originalImage = texture.getBufferedImage();
                            if (originalImage == null) {
                                log.error("originalImage is null.");
                                continue;
                            }
                            if (originalImage.getWidth() > 4096 || originalImage.getHeight() > 4096) {
                                int newWidth = Math.min(originalImage.getWidth(), 4096);
                                int newHeight = Math.min(originalImage.getHeight(), 4096);
                                BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
                                Graphics2D g = resizedImage.createGraphics();
                                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                                g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
                                g.dispose();
                                texture.setBufferedImage(resizedImage);
                            }

                        }

                        texture.setParentPath(imagesPath.toString());
                        texture.saveImage(texture.getFullPath());
                    }
                }

                boolean copyTexturesToNewPath = false;
                Path tempPathLod = gaiaSetCut.writeFileForPR(gaiaSetCutFolderPath, copyTexturesToNewPath);

                /////////////////////////////////////////////////////////////////////////////////////////

                // create a new tileInfo for the cut scene
                TileInfo tileInfoCut = TileInfo.builder().scene(gaiaSceneCut).outputPath(motherTileInfo.getOutputPath()).build();
                tileInfoCut.setTempPath(tempPathLod);
                Matrix4d transformMatrixCut = new Matrix4d(motherTileInfo.getTransformMatrix());
                tileInfoCut.setTransformMatrix(transformMatrixCut);
                tileInfoCut.setBoundingBox(boundingBoxCutLC);
                tileInfoCut.setCartographicBBox(cartographicBoundingBox);

                // make a kmlInfo for the cut scene
                // In reality, we must recalculate the position of the cut scene. Provisionally, we use the same position
                TileTransformInfo tileTransformInfoCut = TileTransformInfo.builder().position(geoCoordPosition).build();
                tileInfoCut.setTileTransformInfo(tileTransformInfoCut);
                //cutTileInfos.add(tileInfoCut);

                localResults.computeIfAbsent(currLod, ignored -> new ArrayList<>()).add(tileInfoCut);

                cuttedScene.deleteObjects();
                gaiaSetCut.clear();
                gaiaSceneCut.clear();
            }
        }

        return cutTileInfos;
    }

    private GaiaBoundingBox getCuttingPlanesAndLocalBoundingBox(TileInfo tileInfo, int lod, BoundingVolume rootNodeBoundingVolume, int depthIdx, List<GaiaAAPlane> resultPlanes, Matrix4d resultTransformMatrix) throws FileNotFoundException {
        // Note : tileInfos must contain only one tileInfo
        // calculate the divisions of the rectangle cake
        // int maxDepth = rootNode.findMaxDepth();
        int depthCount = depthIdx + 1;
        int currDepth = depthIdx - lod;

        // the maxDepth corresponds to lod0
        //List<Node> nodes = new ArrayList<>();
        //rootNode.getNodesByDepth(currDepth, nodes);
        BoundingVolume boundingVolume = rootNodeBoundingVolume;
        double minLonDeg = Math.toDegrees(boundingVolume.getRegion()[0]);
        double minLatDeg = Math.toDegrees(boundingVolume.getRegion()[1]);
        double maxLonDeg = Math.toDegrees(boundingVolume.getRegion()[2]);
        double maxLatDeg = Math.toDegrees(boundingVolume.getRegion()[3]);
        double minAlt = boundingVolume.getRegion()[4];
        double maxAlt = boundingVolume.getRegion()[5];

        double divisionsCount = Math.pow(2, (currDepth));

        List<Double> lonDivisions = new ArrayList<>();
        List<Double> latDivisions = new ArrayList<>();
        List<Double> altDivisions = new ArrayList<>();

        double lonStep = (maxLonDeg - minLonDeg) / divisionsCount;
        double latStep = (maxLatDeg - minLatDeg) / divisionsCount;
        double altStep = (maxAlt - minAlt) / divisionsCount;

        // exclude the first and last divisions, so i = 1 and i < divisionsCount
        // but we must include the 1rst and last divisions to calculate the localBBox
        for (int i = 0; i <= divisionsCount; i++) {
            lonDivisions.add(minLonDeg + i * lonStep);
            latDivisions.add(minLatDeg + i * latStep);
            altDivisions.add(minAlt + i * altStep);
        }

        // now, cut the scene by the divisions
        boolean someSceneCut = false;

        GaiaBoundingBox setBBox = tileInfo.getBoundingBox();
        if (setBBox == null) {
            log.error("[ERROR] setBBox is null.");
        }
        TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
        Vector3d geoCoordPosition = tileTransformInfo.getPosition();
        Vector3d posWC = GlobeUtils.geographicToCartesianWgs84(geoCoordPosition);
        Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(posWC);
        resultTransformMatrix.set(transformMatrix);
        Matrix4d transformMatrixInv = new Matrix4d(transformMatrix);
        transformMatrixInv.invert();

        Vector3d samplePointLC = new Vector3d();

        // make GaiaAAPlanes
        List<GaiaAAPlane> planesYZ = new ArrayList<>();
        List<GaiaAAPlane> planesXZ = new ArrayList<>();
        List<GaiaAAPlane> planesXY = new ArrayList<>();

        Vector3d samplePointGeoCoord;
        Vector3d samplePointWC;

        double localMinX = Double.MAX_VALUE;
        double localMinY = Double.MAX_VALUE;
        double localMinZ = Double.MAX_VALUE;
        double localMaxX = -Double.MAX_VALUE;
        double localMaxY = -Double.MAX_VALUE;
        double localMaxZ = -Double.MAX_VALUE;

        for (int i = 0; i < lonDivisions.size(); i++) {
            double lonDeg = lonDivisions.get(i);
            double latDeg = latDivisions.get(i);
            double altitude = altDivisions.get(i);

            // Longitude plane: create a point with lonDeg, geoCoordPosition.y, 0.0
            samplePointGeoCoord = new Vector3d(lonDeg, geoCoordPosition.y, 0.0);
            samplePointWC = GlobeUtils.geographicToCartesianWgs84(samplePointGeoCoord);
            transformMatrixInv.transformPosition(samplePointWC, samplePointLC);

            // check local min max X
            if (samplePointLC.x < localMinX) {
                localMinX = samplePointLC.x;
            }
            if (samplePointLC.x > localMaxX) {
                localMaxX = samplePointLC.x;
            }

            // check if the planeLC cuts the setBBox
            if (samplePointLC.x > setBBox.getMinX() && samplePointLC.x < setBBox.getMaxX()) {
                if (i > 0 && i < lonDivisions.size() - 1) {
                    GaiaAAPlane planeYZ = new GaiaAAPlane();
                    planeYZ.setPlaneType(PlaneType.YZ);
                    planeYZ.setPoint(new Vector3d(samplePointLC));
                    planesYZ.add(planeYZ);
                }
            }

            // Latitude plane: create a point with geoCoordPosition.x, latDeg, 0.0
            samplePointGeoCoord = new Vector3d(geoCoordPosition.x, latDeg, 0.0);
            samplePointWC = GlobeUtils.geographicToCartesianWgs84(samplePointGeoCoord);
            transformMatrixInv.transformPosition(samplePointWC, samplePointLC);

            // check local min max Y
            if (samplePointLC.y < localMinY) {
                localMinY = samplePointLC.y;
            }
            if (samplePointLC.y > localMaxY) {
                localMaxY = samplePointLC.y;
            }

            // check if the planeLC cuts the setBBox
            if (samplePointLC.y > setBBox.getMinY() && samplePointLC.y < setBBox.getMaxY()) {
                if (i > 0 && i < latDivisions.size() - 1) {
                    GaiaAAPlane planeXZ = new GaiaAAPlane();
                    planeXZ.setPlaneType(PlaneType.XZ);
                    planeXZ.setPoint(new Vector3d(samplePointLC));
                    planesXZ.add(planeXZ);
                }
            }

            // Altitude plane: create a point with geoCoordPosition.x, geoCoordPosition.y, 0.0
            samplePointGeoCoord = new Vector3d(geoCoordPosition.x, geoCoordPosition.y, altitude);
            samplePointWC = GlobeUtils.geographicToCartesianWgs84(samplePointGeoCoord);
            transformMatrixInv.transformPosition(samplePointWC, samplePointLC);

            // check local min max Z
            if (samplePointLC.z < localMinZ) {
                localMinZ = samplePointLC.z;
            }
            if (samplePointLC.z > localMaxZ) {
                localMaxZ = samplePointLC.z;
            }

            // check if the planeLC cuts the setBBox
            if (samplePointLC.z > setBBox.getMinZ() && samplePointLC.z < setBBox.getMaxZ()) {
                if (i > 0 && i < altDivisions.size() - 1) {
                    GaiaAAPlane planeXY = new GaiaAAPlane();
                    planeXY.setPlaneType(PlaneType.XY);
                    planeXY.setPoint(new Vector3d(samplePointLC));
                    planesXY.add(planeXY);
                }
            }
        }

        resultPlanes.addAll(planesXY);
        resultPlanes.addAll(planesXZ);
        resultPlanes.addAll(planesYZ);

        GaiaBoundingBox resultBBox = new GaiaBoundingBox(localMinX, localMinY, localMinZ, localMaxX, localMaxY, localMaxZ, true);
        return resultBBox;
    }

    private GaiaBoundingBox calculateCartographicBoundingBox(GaiaScene gaiaScene, Matrix4d transformMatrix, GaiaBoundingBox resultBoundingBoxLC) {
//        GaiaScene gaiaSceneCut = HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);
        GaiaBoundingBox boundingBoxCutLC = gaiaScene.updateBoundingBox();
        resultBoundingBoxLC.set(boundingBoxCutLC);

        // Calculate cartographicBoundingBox
        double minPosLCX = boundingBoxCutLC.getMinX();
        double minPosLCY = boundingBoxCutLC.getMinY();
        double minPosLCZ = boundingBoxCutLC.getMinZ();

        double maxPosLCX = boundingBoxCutLC.getMaxX();
        double maxPosLCY = boundingBoxCutLC.getMaxY();
        double maxPosLCZ = boundingBoxCutLC.getMaxZ();

        Vector3d leftDownBottomLC = new Vector3d(minPosLCX, minPosLCY, minPosLCZ);
        Vector3d rightDownBottomLC = new Vector3d(maxPosLCX, minPosLCY, minPosLCZ);
        Vector3d rightUpBottomLC = new Vector3d(maxPosLCX, maxPosLCY, minPosLCZ);

        Vector3d leftDownUpLC = new Vector3d(minPosLCX, minPosLCY, maxPosLCZ);

        Vector3d leftDownBottomWC = transformMatrix.transformPosition(leftDownBottomLC);
        Vector3d geoCoordLeftDownBottom = GlobeUtils.cartesianToGeographicWgs84(leftDownBottomWC);

        Vector3d rightDownBottomWC = transformMatrix.transformPosition(rightDownBottomLC);
        Vector3d geoCoordRightDownBottom = GlobeUtils.cartesianToGeographicWgs84(rightDownBottomWC);

        Vector3d rightUpBottomWC = transformMatrix.transformPosition(rightUpBottomLC);
        Vector3d geoCoordRightUpBottom = GlobeUtils.cartesianToGeographicWgs84(rightUpBottomWC);

        Vector3d leftDownUpWC = transformMatrix.transformPosition(leftDownUpLC);
        Vector3d geoCoordLeftDownUp = GlobeUtils.cartesianToGeographicWgs84(leftDownUpWC);

        double minLonDegCut = geoCoordLeftDownBottom.x;
        double minLatDegCut = geoCoordLeftDownBottom.y;
        double maxLonDegCut = geoCoordRightDownBottom.x;
        double maxLatDegCut = geoCoordRightUpBottom.y;

        return new GaiaBoundingBox(minLonDegCut, minLatDegCut, geoCoordLeftDownBottom.z, maxLonDegCut, maxLatDegCut, geoCoordLeftDownUp.z, false);
    }

    private static List<FrontierCandidate>
    collectFrontierCandidates(
            GaiaScene scene,
            int sourceTileId,
            Vector3d scenePositionRelToCellGrid
    ) {
        List<FrontierCandidate> result =
                new ArrayList<>();

        if (scene == null
                || scenePositionRelToCellGrid == null) {
            return result;
        }

        GaiaExtractor extractor =
                new GaiaExtractor();

        List<GaiaPrimitive> primitives =
                extractor.extractAllPrimitives(
                        scene
                );

        if (primitives == null
                || primitives.isEmpty()) {
            return result;
        }

        GaiaFrontierFinder frontierFinder =
                new GaiaFrontierFinder();

        for (GaiaPrimitive primitive : primitives) {
            if (primitive == null
                    || primitive.getVertices() == null
                    || primitive.getVertices().isEmpty()) {
                continue;
            }

            List<GaiaVertex> vertices =
                    primitive.getVertices();

            List<GaiaFace> faces =
                    primitive.extractGaiaAllFaces(
                            null
                    );

            if (faces == null
                    || faces.isEmpty()) {
                continue;
            }

            int vertexCount =
                    vertices.size();

            int[] weldedIndices =
                    new int[vertexCount];

            boolean[] frontierVertices =
                    frontierFinder.findBoundaryVertices(
                            vertices,
                            faces,
                            1e-6,
                            weldedIndices
                    );

            if (frontierVertices == null
                    || frontierVertices.length < vertexCount) {
                continue;
            }

            for (int i = 0; i < vertexCount; i++) {
                if (!frontierVertices[i]) {
                    continue;
                }

                GaiaVertex vertex =
                        vertices.get(i);

                if (vertex == null
                        || vertex.getPosition() == null) {
                    continue;
                }

                Vector3d positionInCellGrid =
                        new Vector3d(
                                vertex.getPosition()
                        ).add(
                                scenePositionRelToCellGrid
                        );

                result.add(
                        new FrontierCandidate(
                                sourceTileId,
                                positionInCellGrid
                        )
                );
            }
        }

        return result;
    }

    private record CutTaskResult(
            String sourcePath,
            Map<Integer, List<TileInfo>> tileInfosByLod,
            Map<Integer, PlaneCutResult> planeCutResultsByLod,
            Map<Integer, List<FrontierCandidate>>
            frontierCandidatesByLod
    ) {
    }

    public record LodBoundaryAnchors(
            CellGrid3D cellGrid,
            GlobalBoundaryAnchors globalBoundaryAnchors
    ) {
    }

    public record CutAndScissorResult(
            Map<Integer, List<TileInfo>> tileInfosByLod,
            Map<Integer, LodBoundaryAnchors> boundaryAnchorsByLod
    ) {
    }

    private record FrontierCandidate(
            int sourceTileId,
            Vector3d position
    ) {
        private FrontierCandidate {
            position =
                    position == null
                            ? null
                            : new Vector3d(position);
        }
    }


}