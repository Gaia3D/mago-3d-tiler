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

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class CutAndScissorMTV2 {

    private static final int BOUNDARY_FILE_MAGIC = 0x43415332;
    private static final int BOUNDARY_FILE_VERSION = 1;

    private static final int IO_BUFFER_SIZE = 1024 * 1024;

    private static final byte RECORD_FRONTIER = 1;
    private static final byte RECORD_CUTTING = 2;

    private static final double PLANE_TOLERANCE = 1e-4;

    public final GlobalOptions globalOptions = GlobalOptions.getInstance();

    private final int threadCount;

    public CutAndScissorMTV2() {
        this(2);
    }

    public CutAndScissorMTV2(int threadCount) {
        this.threadCount = Math.max(1, threadCount);
    }


    private static void mergeResults(Map<Integer, List<TileInfo>> destination, Map<Integer, List<TileInfo>> source) {
        if (destination == null || source == null || source.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, List<TileInfo>> entry : source.entrySet()) {

            Integer lod = entry.getKey();

            List<TileInfo> sourceTileInfos = entry.getValue();

            if (lod == null || sourceTileInfos == null || sourceTileInfos.isEmpty()) {
                continue;
            }

            destination.computeIfAbsent(lod, ignored -> new ArrayList<>()).addAll(sourceTileInfos);
        }
    }

    private static int countValidTiles(List<TileInfo> tileInfos) {
        int count = 0;

        for (TileInfo tileInfo : tileInfos) {
            if (tileInfo != null && tileInfo.getTempPath() != null) {
                count++;
            }
        }

        return count;
    }

    private static void accumulateFrontierForAllLods(Map<Integer, PartialLodBoundaryData> partialDataByLod, Map<Integer, CellGrid3D> cellGridsByLod, int sourceTileId, double x, double y, double z) {
        if (!isFinitePosition(x, y, z)) {
            return;
        }

        for (Map.Entry<Integer, CellGrid3D> entry : cellGridsByLod.entrySet()) {

            Integer lod = entry.getKey();

            CellGrid3D cellGrid = entry.getValue();

            if (lod == null || cellGrid == null) {
                continue;
            }

            Vector3i cellIndex = cellGrid.getCellIndex(x, y, z);

            if (cellIndex == null) {
                continue;
            }

            PartialLodBoundaryData partialData = partialDataByLod.computeIfAbsent(lod, ignored -> new PartialLodBoundaryData());

            FrontierAccumulator accumulator = partialData.frontierByCell.get(cellIndex);

            if (accumulator == null) {
                accumulator = new FrontierAccumulator();

                partialData.frontierByCell.put(new Vector3i(cellIndex), accumulator);
            }

            accumulator.add(x, y, z, sourceTileId);
        }
    }

    private static void accumulateCuttingPoint(Map<Integer, PartialLodBoundaryData> partialDataByLod, Map<Integer, CellGrid3D> cellGridsByLod, int lod, PlaneType planeType, double x, double y, double z) {
        if (planeType == null || !isFinitePosition(x, y, z)) {
            return;
        }

        CellGrid3D cellGrid = cellGridsByLod.get(lod);

        if (cellGrid == null) {
            return;
        }

        Vector3i cellIndex = cellGrid.getCellIndex(x, y, z);

        if (cellIndex == null) {
            return;
        }

        PartialLodBoundaryData partialData = partialDataByLod.computeIfAbsent(lod, ignored -> new PartialLodBoundaryData());

        CuttingAccumulator accumulator = partialData.cuttingByCell.get(cellIndex);

        if (accumulator == null) {
            accumulator = new CuttingAccumulator();

            partialData.cuttingByCell.put(new Vector3i(cellIndex), accumulator);
        }

        accumulator.add(x, y, z, planeType);
    }

    private static void mergePartialBoundaryDataByLod(Map<Integer, PartialLodBoundaryData> destination, Map<Integer, PartialLodBoundaryData> source) {
        if (destination == null || source == null || source.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, PartialLodBoundaryData> entry : source.entrySet()) {

            Integer lod = entry.getKey();

            PartialLodBoundaryData sourceData = entry.getValue();

            if (lod == null || sourceData == null) {
                continue;
            }

            PartialLodBoundaryData destinationData = destination.computeIfAbsent(lod, ignored -> new PartialLodBoundaryData());

            mergeCuttingAccumulators(destinationData.cuttingByCell, sourceData.cuttingByCell);

            mergeFrontierAccumulators(destinationData.frontierByCell, sourceData.frontierByCell);

            sourceData.clear();
        }

        source.clear();
    }

    private static void mergeCuttingAccumulators(Map<Vector3i, CuttingAccumulator> destination, Map<Vector3i, CuttingAccumulator> source) {
        for (Map.Entry<Vector3i, CuttingAccumulator> entry : source.entrySet()) {

            Vector3i cellIndex = entry.getKey();

            CuttingAccumulator sourceAccumulator = entry.getValue();

            if (cellIndex == null || sourceAccumulator == null) {
                continue;
            }

            CuttingAccumulator destinationAccumulator = destination.get(cellIndex);

            if (destinationAccumulator == null) {
                destination.put(cellIndex, sourceAccumulator);
            } else {
                destinationAccumulator.merge(sourceAccumulator);
            }
        }
    }

    private static void mergeFrontierAccumulators(Map<Vector3i, FrontierAccumulator> destination, Map<Vector3i, FrontierAccumulator> source) {
        for (Map.Entry<Vector3i, FrontierAccumulator> entry : source.entrySet()) {

            Vector3i cellIndex = entry.getKey();

            FrontierAccumulator sourceAccumulator = entry.getValue();

            if (cellIndex == null || sourceAccumulator == null) {
                continue;
            }

            FrontierAccumulator destinationAccumulator = destination.get(cellIndex);

            if (destinationAccumulator == null) {
                destination.put(cellIndex, sourceAccumulator);
            } else {
                destinationAccumulator.merge(sourceAccumulator);
            }
        }
    }

    private static PlaneType planeTypeFromOrdinal(int ordinal) {
        PlaneType[] values = PlaneType.values();

        if (ordinal < 0 || ordinal >= values.length) {
            return null;
        }

        return values[ordinal];
    }

    private static boolean isFinitePosition(double x, double y, double z) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }

    private static void deleteBoundaryFiles(List<BoundaryTaskSource> sources) {
        if (sources == null) {
            return;
        }

        for (BoundaryTaskSource source : sources) {
            if (source != null) {
                deleteFileQuietly(source.boundaryDataPath());
            }
        }
    }

    private static void deleteFileQuietly(Path path) {
        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Could not delete temporary boundary file: {}", path, e);
        }
    }

    private static void deleteDirectoryQuietly(Path path) {
        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.debug("Temporary boundary directory could not be deleted: {}", path);
        }
    }

    private static void writeFrontierCandidates(GaiaScene scene, Vector3d scenePositionRelToCellGrid, BoundaryDataWriter boundaryWriter) throws IOException {
        if (scene == null || scenePositionRelToCellGrid == null || boundaryWriter == null) {
            return;
        }

        GaiaExtractor extractor = new GaiaExtractor();

        List<GaiaPrimitive> primitives = extractor.extractAllPrimitives(scene);

        if (primitives == null || primitives.isEmpty()) {
            return;
        }

        GaiaFrontierFinder frontierFinder = new GaiaFrontierFinder();

        for (GaiaPrimitive primitive : primitives) {
            if (primitive == null || primitive.getVertices() == null || primitive.getVertices().isEmpty()) {
                continue;
            }

            List<GaiaVertex> vertices = primitive.getVertices();

            List<GaiaFace> faces = primitive.extractGaiaAllFaces(null);

            if (faces == null || faces.isEmpty()) {
                continue;
            }

            int vertexCount = vertices.size();

            int[] weldedIndices = new int[vertexCount];

            boolean[] frontierVertices = frontierFinder.findBoundaryVertices(vertices, faces, 1e-6, weldedIndices);

            if (frontierVertices == null || frontierVertices.length < vertexCount) {
                continue;
            }

            for (int i = 0; i < vertexCount; i++) {

                if (!frontierVertices[i]) {
                    continue;
                }

                GaiaVertex vertex = vertices.get(i);

                if (vertex == null || vertex.getPosition() == null) {
                    continue;
                }

                Vector3d position = vertex.getPosition();

                boundaryWriter.writeFrontier(position.x + scenePositionRelToCellGrid.x, position.y + scenePositionRelToCellGrid.y, position.z + scenePositionRelToCellGrid.z);
            }
        }
    }

    /**
     * First phase:
     * <p>
     * - cut and save each source tile;
     * - stream its raw boundary data to a compact temporary binary file;
     * - do not retain PlaneCutPoint or FrontierCandidate objects globally.
     * <p>
     * Second phase:
     * <p>
     * - build the final CellGrid3D instances from the generated TileInfo lists;
     * - read each temporary boundary file in parallel;
     * - build mergeable per-cell accumulators;
     * - merge those accumulators sequentially;
     * - create the final LodBoundaryAnchors.
     */
    public CutAndScissorResult apply(List<TileInfo> tileInfos, Node rootNode, int projectMaxDepthIdx, GaiaBoundingBox rootNodeBBoxLC) {
        Map<Integer, List<TileInfo>> resultsByLod = new HashMap<>();
        Map<Integer, LodBoundaryAnchors> boundaryAnchorsByLod = new HashMap<>();

        if (tileInfos == null || tileInfos.isEmpty() || rootNode == null || rootNodeBBoxLC == null) {
            return new CutAndScissorResult(resultsByLod, boundaryAnchorsByLod);
        }

        int validTileCount = countValidTiles(tileInfos);

        if (validTileCount == 0) {
            return new CutAndScissorResult(resultsByLod, boundaryAnchorsByLod);
        }

        int realThreadCount = Math.min(threadCount, validTileCount);
        Path boundaryDataDirectory = Path.of(globalOptions.getTempPath(), "cutTemp", "boundaryDataV2", UUID.randomUUID().toString());

        try {
            Files.createDirectories(boundaryDataDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not create boundary-data directory: " + boundaryDataDirectory, e);
        }

        log.info("Cutting and Scissor V2 process started. " + "Tiles: {}, threads: {}", validTileCount, realThreadCount);

        List<BoundaryTaskSource> boundaryTaskSources = new ArrayList<>(validTileCount);
        ExecutorService executor = Executors.newFixedThreadPool(realThreadCount);
        CompletionService<CutTaskResult> completionService = new ExecutorCompletionService<>(executor);

        int submittedTasks = 0;
        int nextSourceTileId = 0;

        for (TileInfo tileInfo : tileInfos) {
            if (tileInfo == null || tileInfo.getTempPath() == null) {
                continue;
            }

            int sourceTileId = nextSourceTileId++;
            completionService.submit(() -> processSingleTile(tileInfo, sourceTileId, rootNode, projectMaxDepthIdx, boundaryDataDirectory));
            submittedTasks++;
        }

        executor.shutdown();

        try {
            for (int i = 0; i < submittedTasks; i++) {
                Future<CutTaskResult> future = completionService.take();
                CutTaskResult taskResult = future.get();

                if (taskResult != null) {
                    mergeResults(resultsByLod, taskResult.tileInfosByLod());
                    boundaryTaskSources.add(new BoundaryTaskSource(taskResult.sourcePath(), taskResult.boundaryDataPath()));
                }

                log.info("Cut and Scissor V2 completed: {} / {}", i + 1, submittedTasks);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            deleteBoundaryFiles(boundaryTaskSources);
            deleteDirectoryQuietly(boundaryDataDirectory);
            throw new RuntimeException("Cut and Scissor V2 process interrupted", e);

        } catch (ExecutionException e) {
            executor.shutdownNow();
            deleteBoundaryFiles(boundaryTaskSources);
            deleteDirectoryQuietly(boundaryDataDirectory);
            Throwable cause = e.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw new RuntimeException("Cut and Scissor V2 worker failed", cause);
        }

        /*
         * The current CellGrid3D calculation depends on the generated
         * TileInfo bounding boxes. Therefore it is intentionally done
         * only after all cutting tasks have completed.
         */
        Map<Integer, CellGrid3D> cellGridsByLod = createCellGridsByLod(resultsByLod, projectMaxDepthIdx, rootNodeBBoxLC);
        Map<Integer, PartialLodBoundaryData> mergedBoundaryDataByLod;

        log.info("Processing boundary-anchors data files...");

        try {
            mergedBoundaryDataByLod = processBoundaryDataFiles(boundaryTaskSources, cellGridsByLod, realThreadCount);
        } finally {
            /*
             * Idempotent cleanup. Files already deleted by successful
             * readers simply produce no work here.
             */
            deleteBoundaryFiles(boundaryTaskSources);
            deleteDirectoryQuietly(boundaryDataDirectory);
        }

        boundaryAnchorsByLod = buildBoundaryAnchorsByLod(cellGridsByLod, mergedBoundaryDataByLod);
        return new CutAndScissorResult(resultsByLod, boundaryAnchorsByLod);
    }

    private Map<Integer, CellGrid3D> createCellGridsByLod(Map<Integer, List<TileInfo>> tileInfosByLod, int maxDepth, GaiaBoundingBox rootNodeBBoxLC) {
        Map<Integer, CellGrid3D> result = new HashMap<>();

        if (tileInfosByLod == null || tileInfosByLod.isEmpty() || rootNodeBBoxLC == null) {
            return result;
        }

        for (Map.Entry<Integer, List<TileInfo>> entry : tileInfosByLod.entrySet()) {

            Integer lod = entry.getKey();

            List<TileInfo> lodTileInfos = entry.getValue();

            if (lod == null || lodTileInfos == null || lodTileInfos.isEmpty()) {
                continue;
            }

            CellGrid3D cellGrid = createReMeshCellGridForLod(lod, maxDepth, rootNodeBBoxLC, lodTileInfos);

            if (cellGrid != null) {
                result.put(lod, cellGrid);
            }
        }

        return result;
    }

    private Map<Integer, PartialLodBoundaryData> processBoundaryDataFiles(List<BoundaryTaskSource> sources, Map<Integer, CellGrid3D> cellGridsByLod, int requestedThreadCount) {
        Map<Integer, PartialLodBoundaryData> result = new HashMap<>();

        if (sources == null || sources.isEmpty() || cellGridsByLod == null || cellGridsByLod.isEmpty()) {
            return result;
        }

        int realThreadCount = Math.max(1, Math.min(requestedThreadCount, sources.size()));

        ExecutorService executor = Executors.newFixedThreadPool(realThreadCount);

        CompletionService<BoundaryTaskResult> completionService = new ExecutorCompletionService<>(executor);

        int sourcesCount = sources.size();
        int currSource = 0;
        for (BoundaryTaskSource source : sources) {
            currSource ++;
            log.info("processing boundary-anchors data files: "+ currSource + " / " + sourcesCount);
            completionService.submit(() -> processBoundaryDataFile(source, cellGridsByLod));
        }

        executor.shutdown();

        try {
            for (int i = 0; i < sources.size(); i++) {

                Future<BoundaryTaskResult> future = completionService.take();

                BoundaryTaskResult taskResult = future.get();

                if (taskResult != null) {
                    mergePartialBoundaryDataByLod(result, taskResult.partialDataByLod());
                }

                log.info("Boundary accumulation V2 completed: {} / {}", i + 1, sources.size());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();

            throw new RuntimeException("Boundary accumulation V2 interrupted", e);

        } catch (ExecutionException e) {
            executor.shutdownNow();

            Throwable cause = e.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw new RuntimeException("Boundary accumulation V2 worker failed", cause);
        }

        return result;
    }

    private BoundaryTaskResult processBoundaryDataFile(BoundaryTaskSource source, Map<Integer, CellGrid3D> cellGridsByLod) {
        Map<Integer, PartialLodBoundaryData> partialDataByLod = new HashMap<>();

        Path path = source.boundaryDataPath();

        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(path), IO_BUFFER_SIZE))) {

            int magic = input.readInt();

            if (magic != BOUNDARY_FILE_MAGIC) {
                throw new IOException("Invalid boundary-data magic: " + path);
            }

            int version = input.readInt();

            if (version != BOUNDARY_FILE_VERSION) {
                throw new IOException("Unsupported boundary-data version " + version + ": " + path);
            }

            int sourceTileId = input.readInt();

            while (true) {
                int recordType;

                try {
                    recordType = input.readUnsignedByte();
                } catch (EOFException ignored) {
                    break;
                }

                if (recordType == RECORD_FRONTIER) {
                    double x = input.readDouble();

                    double y = input.readDouble();

                    double z = input.readDouble();

                    accumulateFrontierForAllLods(partialDataByLod, cellGridsByLod, sourceTileId, x, y, z);

                } else if (recordType == RECORD_CUTTING) {
                    int lod = input.readInt();

                    int planeTypeOrdinal = input.readByte();

                    double x = input.readDouble();

                    double y = input.readDouble();

                    double z = input.readDouble();

                    PlaneType planeType = planeTypeFromOrdinal(planeTypeOrdinal);

                    accumulateCuttingPoint(partialDataByLod, cellGridsByLod, lod, planeType, x, y, z);

                } else {
                    throw new IOException("Unknown boundary record type " + recordType + " in " + path);
                }
            }

            return new BoundaryTaskResult(source.sourcePath(), partialDataByLod);

        } catch (IOException e) {
            throw new RuntimeException("Could not read boundary data: " + path, e);

        } finally {
            deleteFileQuietly(path);
        }
    }

    private Map<Integer, LodBoundaryAnchors> buildBoundaryAnchorsByLod(Map<Integer, CellGrid3D> cellGridsByLod, Map<Integer, PartialLodBoundaryData> boundaryDataByLod) {
        Map<Integer, LodBoundaryAnchors> result = new HashMap<>();

        if (cellGridsByLod == null || cellGridsByLod.isEmpty()) {
            return result;
        }

        for (Map.Entry<Integer, CellGrid3D> lodEntry : cellGridsByLod.entrySet()) {

            Integer lod = lodEntry.getKey();

            CellGrid3D cellGrid = lodEntry.getValue();

            if (lod == null || cellGrid == null) {
                continue;
            }

            PartialLodBoundaryData boundaryData = boundaryDataByLod == null ? null : boundaryDataByLod.remove(lod);

            GlobalBoundaryAnchors globalAnchors = new GlobalBoundaryAnchors();

            long cuttingPointCount = 0L;
            long frontierCandidateCount = 0L;

            int cuttingAnchorCount = 0;
            int conflictingCuttingCells = 0;
            int sharedFrontierAnchors = 0;

            if (boundaryData != null) {
                for (Map.Entry<Vector3i, CuttingAccumulator> entry : boundaryData.cuttingByCell.entrySet()) {

                    CuttingAccumulator accumulator = entry.getValue();

                    if (accumulator == null || accumulator.getCount() == 0L) {
                        continue;
                    }

                    cuttingPointCount += accumulator.getCount();

                    if (accumulator.hasPlaneConflict(PLANE_TOLERANCE)) {
                        conflictingCuttingCells++;
                        continue;
                    }

                    Vector3d average = accumulator.calculateLockedAverage();

                    if (average == null) {
                        continue;
                    }

                    if (globalAnchors.putIfAbsent(entry.getKey(), average)) {
                        cuttingAnchorCount++;
                    }
                }

                /*
                 * Cutting anchors have priority.
                 */
                for (Map.Entry<Vector3i, FrontierAccumulator> entry : boundaryData.frontierByCell.entrySet()) {

                    FrontierAccumulator accumulator = entry.getValue();

                    if (accumulator == null) {
                        continue;
                    }

                    frontierCandidateCount += accumulator.getCount();

                    if (!accumulator.isShared() || globalAnchors.hasAverage(entry.getKey())) {
                        continue;
                    }

                    Vector3d average = accumulator.calculateAverage();

                    if (average == null) {
                        continue;
                    }

                    if (globalAnchors.putIfAbsent(entry.getKey(), average)) {
                        sharedFrontierAnchors++;
                    }
                }
            }

            result.put(lod, new LodBoundaryAnchors(cellGrid, globalAnchors));

            log.info("Global boundary anchors V2 built. " + "LOD={}, cuttingPoints={}, " + "frontierCandidates={}, " + "cuttingAnchors={}, " + "conflictingCuttingCells={}, " + "sharedFrontierAnchors={}, anchors={}", lod, cuttingPointCount, frontierCandidateCount, cuttingAnchorCount, conflictingCuttingCells, sharedFrontierAnchors, globalAnchors.size());

            if (boundaryData != null) {
                boundaryData.clear();
            }
        }

        if (boundaryDataByLod != null) {
            boundaryDataByLod.clear();
        }

        return result;
    }

    protected CellGrid3D createReMeshCellGridForLod(int lod, int maxDepth, GaiaBoundingBox rootNodeBBoxLC, List<TileInfo> tileInfos) {

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

    private CutTaskResult processSingleTile(TileInfo tileInfo, int sourceTileId, Node rootNode, int projectMaxDepthIdx, Path boundaryDataDirectory) {
        int initialLod = Math.min(7, projectMaxDepthIdx);
        BoundingVolume rootBoundingVolumeCopy = new BoundingVolume(rootNode.getBoundingVolume());
        Map<Integer, List<TileInfo>> localResults = new HashMap<>();
        Vector3d scenePositionRelToCellGrid = calculateScenePositionRelToCellGrid(tileInfo, rootNode);
        Path boundaryDataPath = boundaryDataDirectory.resolve(UUID.randomUUID() + ".cas2");

        try (BoundaryDataWriter boundaryWriter = new BoundaryDataWriter(boundaryDataPath, sourceTileId)) {
            cutRectangleCakeAllLod(tileInfo, initialLod, rootBoundingVolumeCopy, projectMaxDepthIdx, scenePositionRelToCellGrid, localResults, boundaryWriter);
            return new CutTaskResult(tileInfo.getTempPath().toString(), localResults, boundaryDataPath, boundaryWriter.getFrontierPointCount(), boundaryWriter.getCuttingPointCount());

        } catch (Exception e) {
            deleteFileQuietly(boundaryDataPath);
            throw new RuntimeException("Failed processing source tile: " + tileInfo.getTempPath(), e);
        }
    }

    private Vector3d calculateScenePositionRelToCellGrid(TileInfo tileInfo, Node rootNode) {
        if (tileInfo == null || tileInfo.getTileTransformInfo() == null || tileInfo.getTileTransformInfo().getPosition() == null || rootNode == null) {
            return new Vector3d();
        }

        Vector3d tileGeoCoord = tileInfo.getTileTransformInfo().getPosition();
        Vector3d tilePositionWorld = GlobeUtils.geographicToCartesianWgs84(tileGeoCoord);
        Matrix4d rootTransform = rootNode.getTransformMatrix();

        if (rootTransform == null) {
            Vector3d rootCenterRad = rootNode.getBoundingVolume().calcCenter();
            Vector3d rootCenterDeg = new Vector3d(Math.toDegrees(rootCenterRad.x), Math.toDegrees(rootCenterRad.y), rootCenterRad.z);
            Vector3d rootPositionWorld = GlobeUtils.geographicToCartesianWgs84(rootCenterDeg);

            rootTransform = GlobeUtils.transformMatrixAtCartesianPointWgs84(rootPositionWorld);
        }

        Matrix4d rootTransformInverse = new Matrix4d(rootTransform).invert();
        return rootTransformInverse.transformPosition(tilePositionWorld, new Vector3d());
    }

    private void cutRectangleCakeAllLod(TileInfo tileInfo, int lod, BoundingVolume rootNodeBoundingVolume, int depthIdx, Vector3d scenePositionRelToCellGrid, Map<Integer, List<TileInfo>> localResults, BoundaryDataWriter boundaryWriter) {
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

            /*
             * The cutting phase does not need the original GaiaSet
             * materials after GaiaScene has been created.
             */
            gaiaSet.setMaterials(null);
            gaiaSet.clear();
            scene.deleteNormals();
            GaiaTriangulator triangulator = new GaiaTriangulator();
            triangulator.apply(scene);
            GaiaWeldOptions weldOptions = GaiaWeldOptions.builder().error(1e-6).checkTexCoord(true).checkNormal(false).checkColor(false).checkBatchId(false).build();
            GaiaWelder welder = new GaiaWelder(weldOptions);
            welder.apply(scene);
            writeFrontierCandidates(scene, scenePositionRelToCellGrid, boundaryWriter);
            halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);
            scene.clear();

            boolean scissorTextures = true;
            boolean makeSkirt = GlobalConstants.MAKE_SKIRT;

            Path cutTempPath = Path.of(globalOptions.getTempPath(), "cutTemp");
            Files.createDirectories(cutTempPath);

            cutHalfEdgeSceneByGaiaAAPlanesAndSaveTileInfosAllLodMode(halfEdgeScene, scissorTextures, makeSkirt, tileInfo, lod, rootNodeBoundingVolume, depthIdx, cutTempPath, scenePositionRelToCellGrid, localResults, boundaryWriter);

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

    private List<TileInfo> cutHalfEdgeSceneByGaiaAAPlanesAndSaveTileInfosAllLodMode(HalfEdgeScene halfEdgeScene, boolean scissorTextures, boolean makeSkirt, TileInfo motherTileInfo, int lod, BoundingVolume rootNodeBoundingVolume, int projectMaxDepth, Path cutTempPath, Vector3d scenePositionRelToCellGrid, Map<Integer, List<TileInfo>> localResults, BoundaryDataWriter boundaryWriter) throws IOException {
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

            try {
                if (!planeCutResult.isEmpty()) {
                    boundaryWriter.writeCuttingPoints(currLod, planeCutResult, scenePositionRelToCellGrid);
                }
            } finally {
                /*
                 * Raw PlaneCutPoint objects are no longer needed after
                 * they have been streamed to the temporary file.
                 */
                planeCutResult.deleteObjects();
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

            octreesWithContents.clear();

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
                List<GaiaMaterial> motherMaterials = halfEdgeScene.getMaterials();

                if (scissorTextures && cut) {
                    cuttedScene.scissorTexturesByMotherScene(motherMaterials);
                }

                // clear bufferedImages of motherMaterials to save memory.*******************
                // delete all textures less one.
                boolean skiped = false;
                int skipCount = 0;
                for (GaiaMaterial motherMaterial : motherMaterials) {
                    if (!skiped) {
                        if (motherMaterial.hasTextures()) {
                            skipCount++;
                        }

                        if(skipCount >= 1){
                            skiped = true;
                        }

                        continue;
                    }
                    motherMaterial.deleteTextures();
                }
                // end clear.-----------------------------------------------------------------------

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

                        BufferedImage originalImage = null;
                        BufferedImage resizedImage = null;

                        if (!texture.hasBufferedImage()) {
                            String texPath = texture.getPath();
                            if (texPath == null || texPath.isEmpty()) {
                                log.debug("Texture path is null or empty for texture: " + texture.getFullPath());
                                continue;
                            }
                            String parentPath = texture.getParentPath();
                            if (parentPath == null || parentPath.isEmpty()) {
                                log.debug("Texture path is null or empty for texture: " + texture.getFullPath());
                                continue;
                            }

                            if (currLod > 2) {
                                Path path = Path.of(texture.getFullPath());
                                try {
                                    log.debug("load image SCALED for LOD: " + currLod);
                                    originalImage = texture.readImageScaled(path, 4096);
                                } catch (Exception e) {
                                    log.error("Failed to read and scale image for texture: " + texture.getFullPath(), e);
                                    throw new RuntimeException("Failed to read and scale image for texture: " + texture.getFullPath(), e);
                                }
                            } else {
                                originalImage = texture.getBufferedImage();
                            }

                            //originalImage = texture.getBufferedImage();

                            // check if exist bufferedImage of the texture
                            if (originalImage == null) {
                                log.warn("originalImage is null for texture: " + texture.getFullPath());
                                continue;
                            }
                        } else {
                            log.debug("Texture already has bufferedImage for texture: " + texture.getFullPath());
                        }

                        texture.setParentPath(imagesPath.toString());
                        texture.saveImage(texture.getFullPath());
                        texture.deleteBufferedImage();
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

                localResults.computeIfAbsent(currLod, ignored -> new ArrayList<>()).add(tileInfoCut);

                resultOctree.clearTree();
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


    private static final class BoundaryDataWriter implements AutoCloseable {

        private final DataOutputStream output;

        private long frontierPointCount;
        private long cuttingPointCount;

        private BoundaryDataWriter(Path path, int sourceTileId) throws IOException {
            output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path), IO_BUFFER_SIZE));

            output.writeInt(BOUNDARY_FILE_MAGIC);

            output.writeInt(BOUNDARY_FILE_VERSION);

            output.writeInt(sourceTileId);
        }

        private void writeFrontier(double x, double y, double z) throws IOException {
            if (!isFinitePosition(x, y, z)) {
                return;
            }

            output.writeByte(RECORD_FRONTIER);

            output.writeDouble(x);

            output.writeDouble(y);

            output.writeDouble(z);

            frontierPointCount++;
        }

        private void writeCuttingPoints(int lod, PlaneCutResult planeCutResult, Vector3d translation) throws IOException {
            if (planeCutResult == null || translation == null || planeCutResult.getCuttingPoints() == null || planeCutResult.getCuttingPoints().isEmpty()) {
                return;
            }

            for (PlaneCutPoint point : planeCutResult.getCuttingPoints()) {

                if (point == null || point.getPlaneType() == null) {
                    continue;
                }

                double x = point.getX() + translation.x;

                double y = point.getY() + translation.y;

                double z = point.getZ() + translation.z;

                if (!isFinitePosition(x, y, z)) {
                    continue;
                }

                output.writeByte(RECORD_CUTTING);

                output.writeInt(lod);

                output.writeByte(point.getPlaneType().ordinal());

                output.writeDouble(x);

                output.writeDouble(y);

                output.writeDouble(z);

                cuttingPointCount++;
            }
        }

        private long getFrontierPointCount() {
            return frontierPointCount;
        }

        private long getCuttingPointCount() {
            return cuttingPointCount;
        }

        @Override
        public void close() throws IOException {
            output.close();
        }
    }

    private static final class PartialLodBoundaryData {

        private final Map<Vector3i, CuttingAccumulator> cuttingByCell = new HashMap<>();

        private final Map<Vector3i, FrontierAccumulator> frontierByCell = new HashMap<>();

        private void clear() {
            cuttingByCell.clear();
            frontierByCell.clear();
        }
    }

    /**
     * Mergeable equivalent of the cutting-point accumulation performed
     * by GlobalBoundaryAnchorsBuilder.
     * <p>
     * The average starts with all coordinates. Coordinates constrained
     * by a cutting plane are then replaced by the average coordinate of
     * that plane family:
     * <p>
     * - YZ locks X;
     * - XZ locks Y;
     * - XY locks Z.
     */
    private static final class CuttingAccumulator {

        private double sumX;
        private double sumY;
        private double sumZ;

        private long count;

        private double lockedXSum;
        private long lockedXCount;
        private double lockedXMin = Double.POSITIVE_INFINITY;
        private double lockedXMax = Double.NEGATIVE_INFINITY;

        private double lockedYSum;
        private long lockedYCount;
        private double lockedYMin = Double.POSITIVE_INFINITY;
        private double lockedYMax = Double.NEGATIVE_INFINITY;

        private double lockedZSum;
        private long lockedZCount;
        private double lockedZMin = Double.POSITIVE_INFINITY;
        private double lockedZMax = Double.NEGATIVE_INFINITY;

        private static boolean hasRangeConflict(long valueCount, double min, double max, double tolerance) {
            return valueCount > 1L && Double.isFinite(min) && Double.isFinite(max) && max - min > tolerance;
        }

        private void add(double x, double y, double z, PlaneType planeType) {
            if (planeType == null || !isFinitePosition(x, y, z)) {
                return;
            }

            sumX += x;
            sumY += y;
            sumZ += z;
            count++;

            switch (planeType) {
                case YZ -> {
                    lockedXSum += x;
                    lockedXCount++;

                    lockedXMin = Math.min(lockedXMin, x);

                    lockedXMax = Math.max(lockedXMax, x);
                }

                case XZ -> {
                    lockedYSum += y;
                    lockedYCount++;

                    lockedYMin = Math.min(lockedYMin, y);

                    lockedYMax = Math.max(lockedYMax, y);
                }

                case XY -> {
                    lockedZSum += z;
                    lockedZCount++;

                    lockedZMin = Math.min(lockedZMin, z);

                    lockedZMax = Math.max(lockedZMax, z);
                }

                default -> {
                    /*
                     * Unknown plane families still contribute to the
                     * ordinary average, but do not lock an axis.
                     */
                }
            }
        }

        private void merge(CuttingAccumulator other) {
            if (other == null || other.count == 0L) {
                return;
            }

            sumX += other.sumX;
            sumY += other.sumY;
            sumZ += other.sumZ;
            count += other.count;

            if (other.lockedXCount > 0L) {
                lockedXSum += other.lockedXSum;

                lockedXCount += other.lockedXCount;

                lockedXMin = Math.min(lockedXMin, other.lockedXMin);

                lockedXMax = Math.max(lockedXMax, other.lockedXMax);
            }

            if (other.lockedYCount > 0L) {
                lockedYSum += other.lockedYSum;

                lockedYCount += other.lockedYCount;

                lockedYMin = Math.min(lockedYMin, other.lockedYMin);

                lockedYMax = Math.max(lockedYMax, other.lockedYMax);
            }

            if (other.lockedZCount > 0L) {
                lockedZSum += other.lockedZSum;

                lockedZCount += other.lockedZCount;

                lockedZMin = Math.min(lockedZMin, other.lockedZMin);

                lockedZMax = Math.max(lockedZMax, other.lockedZMax);
            }
        }

        private long getCount() {
            return count;
        }

        private boolean hasPlaneConflict(double tolerance) {
            double safeTolerance = Math.max(0.0, tolerance);

            return hasRangeConflict(lockedXCount, lockedXMin, lockedXMax, safeTolerance) || hasRangeConflict(lockedYCount, lockedYMin, lockedYMax, safeTolerance) || hasRangeConflict(lockedZCount, lockedZMin, lockedZMax, safeTolerance);
        }

        private Vector3d calculateLockedAverage() {
            if (count == 0L) {
                return null;
            }

            double inverseCount = 1.0 / count;

            double x = sumX * inverseCount;

            double y = sumY * inverseCount;

            double z = sumZ * inverseCount;

            if (lockedXCount > 0L) {
                x = lockedXSum / lockedXCount;
            }

            if (lockedYCount > 0L) {
                y = lockedYSum / lockedYCount;
            }

            if (lockedZCount > 0L) {
                z = lockedZSum / lockedZCount;
            }

            return new Vector3d(x, y, z);
        }
    }

    private static final class FrontierAccumulator {

        private static final int NO_SOURCE_TILE = Integer.MIN_VALUE;

        private double sumX;
        private double sumY;
        private double sumZ;

        private long count;

        private int firstSourceTileId = NO_SOURCE_TILE;

        private boolean shared;

        private void add(double x, double y, double z, int sourceTileId) {
            if (!isFinitePosition(x, y, z)) {
                return;
            }

            sumX += x;
            sumY += y;
            sumZ += z;
            count++;

            if (firstSourceTileId == NO_SOURCE_TILE) {

                firstSourceTileId = sourceTileId;

            } else if (sourceTileId != firstSourceTileId) {

                shared = true;
            }
        }

        private void merge(FrontierAccumulator other) {
            if (other == null || other.count == 0L) {
                return;
            }

            sumX += other.sumX;
            sumY += other.sumY;
            sumZ += other.sumZ;
            count += other.count;

            if (firstSourceTileId == NO_SOURCE_TILE) {

                firstSourceTileId = other.firstSourceTileId;

            } else if (other.firstSourceTileId != NO_SOURCE_TILE && other.firstSourceTileId != firstSourceTileId) {

                shared = true;
            }

            if (other.shared) {
                shared = true;
            }
        }

        private long getCount() {
            return count;
        }

        private boolean isShared() {
            return shared;
        }

        private Vector3d calculateAverage() {
            if (count == 0L) {
                return null;
            }

            double inverseCount = 1.0 / count;

            return new Vector3d(sumX * inverseCount, sumY * inverseCount, sumZ * inverseCount);
        }
    }

    private record CutTaskResult(String sourcePath, Map<Integer, List<TileInfo>> tileInfosByLod, Path boundaryDataPath,
                                 long frontierPointCount, long cuttingPointCount) {
    }

    private record BoundaryTaskSource(String sourcePath, Path boundaryDataPath) {
    }

    private record BoundaryTaskResult(String sourcePath, Map<Integer, PartialLodBoundaryData> partialDataByLod) {
    }

    public record LodBoundaryAnchors(CellGrid3D cellGrid, GlobalBoundaryAnchors globalBoundaryAnchors) {
    }

    public record CutAndScissorResult(Map<Integer, List<TileInfo>> tileInfosByLod,
                                      Map<Integer, LodBoundaryAnchors> boundaryAnchorsByLod) {
    }
}
