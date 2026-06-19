package com.gaia3d.render;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.modifier.topology.GaiaExtractor;
import com.gaia3d.basic.halfedge.CameraDirectionType;
import com.gaia3d.basic.magogl.MagoFbo;
import com.gaia3d.basic.model.GaiaFace;
import com.gaia3d.basic.model.GaiaPrimitive;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.GaiaSurface;
import com.gaia3d.basic.model.GaiaVertex;
import com.gaia3d.basic.remesher.GaiaWeldedFacesFinder;
import com.gaia3d.renderer.engine.dataStructure.CameraDirectionCandidate;
import com.gaia3d.renderer.engine.dataStructure.FaceVisibilityData;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector4d;

import java.util.*;

public final class FaceVisibilityManager {

    public static final int BACKGROUND_FACE_CODE =
            0xFFFFFFFF;

    private static final int DEFAULT_CLASSIFIED_ID =
            -1;

    private static final int MIN_VISIBLE_INNER_POINTS =
            3;

    private static final int INNER_POINT_SEARCH_RADIUS =
            2;

    private static final double BBOX_EPSILON =
            1e-12;

    private final Map<CameraDirectionType, FaceVisibilityData>
            faceVisibilityDataMap =
            new EnumMap<>(CameraDirectionType.class);

    private final Map<Integer, Set<CameraDirectionType>>
            mapFaceIdToInnerPointValidCameras =
            new HashMap<>();

    public FaceVisibilityData getFaceVisibilityData(
            CameraDirectionType direction
    ) {
        return faceVisibilityDataMap.computeIfAbsent(
                direction,
                FaceVisibilityData::new
        );
    }

    public void updateFaceVisibilityData(
            GaiaScene scene,
            CameraDirectionType direction,
            MagoFbo faceCodeFbo,
            Map<CameraDirectionType, Matrix4d> mapCameraDirectionTypeModelViewMatrix,
            Map<CameraDirectionType, GaiaBoundingBox> mapCameraDirectionTypeBBox
    ) {
        if (scene == null
                || direction == null
                || faceCodeFbo == null
                || mapCameraDirectionTypeModelViewMatrix == null
                || mapCameraDirectionTypeBBox == null) {

            return;
        }

        Matrix4d modelViewMatrix = mapCameraDirectionTypeModelViewMatrix.get(direction);
        if (modelViewMatrix == null) {
            return;
        }

        GaiaBoundingBox transformedTargetBBox = mapCameraDirectionTypeBBox.get(direction);
        if (transformedTargetBBox == null) {
            return;
        }

        double bboxSizeX =
                transformedTargetBBox.getMaxX()
                        - transformedTargetBBox.getMinX();

        double bboxSizeY =
                transformedTargetBBox.getMaxY()
                        - transformedTargetBBox.getMinY();

        if (bboxSizeX <= BBOX_EPSILON
                || bboxSizeY <= BBOX_EPSILON) {

            return;
        }

        int width =
                faceCodeFbo.getWidth();

        int height =
                faceCodeFbo.getHeight();

        int[] faceCodeBuffer =
                faceCodeFbo.getColorBuffer();

        if (width <= 0
                || height <= 0
                || faceCodeBuffer == null) {

            return;
        }

        int expectedPixelCount;

        try {
            expectedPixelCount =
                    Math.multiplyExact(width, height);
        } catch (ArithmeticException exception) {
            return;
        }

        if (faceCodeBuffer.length != expectedPixelCount) {
            throw new IllegalStateException(
                    "Unexpected MagoFbo color-buffer size. Expected "
                            + expectedPixelCount
                            + " values, but found "
                            + faceCodeBuffer.length
            );
        }

        GaiaExtractor extractor =
                new GaiaExtractor();

        List<GaiaPrimitive> primitives =
                extractor.extractAllPrimitives(scene);

        if (primitives == null || primitives.isEmpty()) {
            return;
        }

        /*
         * Reused by every tested point.
         */
        Vector4d transformedScratch =
                new Vector4d();

        for (GaiaPrimitive primitive : primitives) {
            if (primitive == null) {
                continue;
            }

            List<GaiaVertex> vertices =
                    primitive.getVertices();

            if (vertices == null || vertices.isEmpty()) {
                continue;
            }

            List<GaiaSurface> surfaces =
                    primitive.getSurfaces();

            if (surfaces == null || surfaces.isEmpty()) {
                continue;
            }

            for (GaiaSurface surface : surfaces) {
                if (surface == null) {
                    continue;
                }

                List<GaiaFace> faces =
                        surface.getFaces();

                if (faces == null || faces.isEmpty()) {
                    continue;
                }

                for (GaiaFace face : faces) {
                    if (face == null) {
                        continue;
                    }

                    int faceCode =
                            face.getId();

                    if (faceCode == BACKGROUND_FACE_CODE) {
                        throw new IllegalStateException(
                                "GaiaFace id collides with "
                                        + "BACKGROUND_FACE_CODE: "
                                        + faceCode
                        );
                    }

                    if (faceCode < 0) {
                        throw new IllegalStateException(
                                "GaiaFace id must be non-negative, but was "
                                        + faceCode
                        );
                    }

                    int visibleInnerPoints =
                            countVisibleInnerPoints(
                                    face,
                                    vertices,
                                    modelViewMatrix,
                                    transformedTargetBBox,
                                    bboxSizeX,
                                    bboxSizeY,
                                    faceCodeBuffer,
                                    width,
                                    height,
                                    faceCode,
                                    INNER_POINT_SEARCH_RADIUS,
                                    MIN_VISIBLE_INNER_POINTS,
                                    transformedScratch
                            );

                    if (visibleInnerPoints
                            >= MIN_VISIBLE_INNER_POINTS) {

                        addInnerPointValidCamera(
                                face,
                                direction
                        );
                    }
                }
            }
        }

        accumulatePixelVisibility(
                direction,
                faceCodeBuffer
        );
    }

    private static int countVisibleInnerPoints(
            GaiaFace face,
            List<GaiaVertex> vertices,
            Matrix4d modelViewMatrix,
            GaiaBoundingBox bbox,
            double bboxSizeX,
            double bboxSizeY,
            int[] faceCodeBuffer,
            int width,
            int height,
            int expectedFaceCode,
            int searchRadius,
            int requiredVisiblePoints,
            Vector4d transformedScratch
    ) {
        int[] indices =
                face.getIndices();

        if (indices == null || indices.length < 3) {
            return 0;
        }

        int index0 = indices[0];
        int index1 = indices[1];
        int index2 = indices[2];

        if (index0 < 0 || index0 >= vertices.size()
                || index1 < 0 || index1 >= vertices.size()
                || index2 < 0 || index2 >= vertices.size()) {

            return 0;
        }

        GaiaVertex vertex0 =
                vertices.get(index0);

        GaiaVertex vertex1 =
                vertices.get(index1);

        GaiaVertex vertex2 =
                vertices.get(index2);

        if (vertex0 == null
                || vertex1 == null
                || vertex2 == null) {

            return 0;
        }

        Vector3d p0 =
                vertex0.getPosition();

        Vector3d p1 =
                vertex1.getPosition();

        Vector3d p2 =
                vertex2.getPosition();

        if (p0 == null || p1 == null || p2 == null) {
            return 0;
        }

        double centroidX =
                (p0.x + p1.x + p2.x) / 3.0;

        double centroidY =
                (p0.y + p1.y + p2.y) / 3.0;

        double centroidZ =
                (p0.z + p1.z + p2.z) / 3.0;

        int visibleCount = 0;

        if (isInnerPointVisible(
                centroidX,
                centroidY,
                centroidZ,
                modelViewMatrix,
                bbox,
                bboxSizeX,
                bboxSizeY,
                faceCodeBuffer,
                width,
                height,
                expectedFaceCode,
                searchRadius,
                transformedScratch
        )) {
            visibleCount++;

            if (visibleCount >= requiredVisiblePoints) {
                return visibleCount;
            }
        }

        final double moveRatio =
                0.20;

        double pointX =
                p0.x + (centroidX - p0.x) * moveRatio;

        double pointY =
                p0.y + (centroidY - p0.y) * moveRatio;

        double pointZ =
                p0.z + (centroidZ - p0.z) * moveRatio;

        if (isInnerPointVisible(
                pointX,
                pointY,
                pointZ,
                modelViewMatrix,
                bbox,
                bboxSizeX,
                bboxSizeY,
                faceCodeBuffer,
                width,
                height,
                expectedFaceCode,
                searchRadius,
                transformedScratch
        )) {
            visibleCount++;

            if (visibleCount >= requiredVisiblePoints) {
                return visibleCount;
            }
        }

        pointX =
                p1.x + (centroidX - p1.x) * moveRatio;

        pointY =
                p1.y + (centroidY - p1.y) * moveRatio;

        pointZ =
                p1.z + (centroidZ - p1.z) * moveRatio;

        if (isInnerPointVisible(
                pointX,
                pointY,
                pointZ,
                modelViewMatrix,
                bbox,
                bboxSizeX,
                bboxSizeY,
                faceCodeBuffer,
                width,
                height,
                expectedFaceCode,
                searchRadius,
                transformedScratch
        )) {
            visibleCount++;

            if (visibleCount >= requiredVisiblePoints) {
                return visibleCount;
            }
        }

        pointX =
                p2.x + (centroidX - p2.x) * moveRatio;

        pointY =
                p2.y + (centroidY - p2.y) * moveRatio;

        pointZ =
                p2.z + (centroidZ - p2.z) * moveRatio;

        if (isInnerPointVisible(
                pointX,
                pointY,
                pointZ,
                modelViewMatrix,
                bbox,
                bboxSizeX,
                bboxSizeY,
                faceCodeBuffer,
                width,
                height,
                expectedFaceCode,
                searchRadius,
                transformedScratch
        )) {
            visibleCount++;
        }

        return visibleCount;
    }

    private static boolean isInnerPointVisible(
            double pointX,
            double pointY,
            double pointZ,
            Matrix4d modelViewMatrix,
            GaiaBoundingBox bbox,
            double bboxSizeX,
            double bboxSizeY,
            int[] faceCodeBuffer,
            int width,
            int height,
            int expectedFaceCode,
            int searchRadius,
            Vector4d transformedScratch
    ) {
        transformedScratch
                .set(
                        pointX,
                        pointY,
                        pointZ,
                        1.0
                )
                .mul(modelViewMatrix);

        double u =
                (transformedScratch.x - bbox.getMinX())
                        / bboxSizeX;

        double v =
                (transformedScratch.y - bbox.getMinY())
                        / bboxSizeY;

        if (!Double.isFinite(u)
                || !Double.isFinite(v)
                || u < 0.0
                || u > 1.0
                || v < 0.0
                || v > 1.0) {

            return false;
        }

        int pixelX =
                (int) (u * (width - 1) + 0.5);

        int pixelY =
                (int) (v * (height - 1) + 0.5);

        if (pixelX < 0 || pixelX >= width
                || pixelY < 0 || pixelY >= height) {

            return false;
        }

        /*
         * No Y inversion:
         * MagoFbo uses the same bottom-left origin as MagoRenderer.
         */
        return hasExpectedFaceCodeNearPixel(
                faceCodeBuffer,
                width,
                height,
                pixelX,
                pixelY,
                expectedFaceCode,
                searchRadius
        );
    }

    public void printInnerPointDiagnostics(
            GaiaScene scene
    ) {
        if (scene == null) {
            return;
        }

        GaiaExtractor extractor =
                new GaiaExtractor();

        List<GaiaPrimitive> primitives =
                extractor.extractAllPrimitives(scene);

        if (primitives == null || primitives.isEmpty()) {
            return;
        }

        int totalFaces = 0;

        int facesWithVisiblePixels = 0;
        int facesWithInnerCandidate = 0;
        int facesVisibleButWithoutInnerCandidate = 0;
        int facesWithoutVisiblePixels = 0;

        int visibleFaceCameraPairs = 0;
        int acceptedFaceCameraPairs = 0;
        int rejectedFaceCameraPairs = 0;

        for (GaiaPrimitive primitive : primitives) {
            if (primitive == null
                    || primitive.getSurfaces() == null) {

                continue;
            }

            for (GaiaSurface surface
                    : primitive.getSurfaces()) {

                if (surface == null
                        || surface.getFaces() == null) {

                    continue;
                }

                for (GaiaFace face : surface.getFaces()) {
                    if (face == null) {
                        continue;
                    }

                    totalFaces++;

                    int faceId =
                            face.getId();

                    boolean hasVisiblePixels =
                            false;

                    boolean hasInnerCandidate =
                            false;

                    for (Map.Entry<
                            CameraDirectionType,
                            FaceVisibilityData> entry
                            : faceVisibilityDataMap.entrySet()) {

                        CameraDirectionType direction =
                                entry.getKey();

                        FaceVisibilityData visibilityData =
                                entry.getValue();

                        int pixelCount =
                                visibilityData.getPixelFaceVisibility(
                                        faceId
                                );

                        if (pixelCount <= 0) {
                            continue;
                        }

                        hasVisiblePixels = true;
                        visibleFaceCameraPairs++;

                        if (isInnerPointCameraValid(
                                face,
                                direction
                        )) {
                            hasInnerCandidate = true;
                            acceptedFaceCameraPairs++;
                        } else {
                            rejectedFaceCameraPairs++;
                        }
                    }

                    if (!hasVisiblePixels) {
                        facesWithoutVisiblePixels++;
                        continue;
                    }

                    facesWithVisiblePixels++;

                    if (hasInnerCandidate) {
                        facesWithInnerCandidate++;
                    } else {
                        facesVisibleButWithoutInnerCandidate++;
                    }
                }
            }
        }

        System.out.println(
                "========== Inner point diagnostics =========="
        );

        System.out.println(
                "Total faces: "
                        + totalFaces
        );

        System.out.println(
                "Faces with visible pixels: "
                        + facesWithVisiblePixels
        );

        System.out.println(
                "Faces with inner candidate: "
                        + facesWithInnerCandidate
        );

        System.out.println(
                "Faces visible but without inner candidate: "
                        + facesVisibleButWithoutInnerCandidate
        );

        System.out.println(
                "Faces without visible pixels: "
                        + facesWithoutVisiblePixels
        );

        System.out.println(
                "Visible face-camera pairs: "
                        + visibleFaceCameraPairs
        );

        System.out.println(
                "Accepted face-camera pairs: "
                        + acceptedFaceCameraPairs
        );

        System.out.println(
                "Rejected face-camera pairs: "
                        + rejectedFaceCameraPairs
        );
    }

    private static boolean hasExpectedFaceCodeNearPixel(
            int[] faceCodeBuffer,
            int width,
            int height,
            int centerX,
            int centerY,
            int expectedFaceCode,
            int radius
    ) {
        int minX =
                Math.max(0, centerX - radius);

        int maxX =
                Math.min(width - 1, centerX + radius);

        int minY =
                Math.max(0, centerY - radius);

        int maxY =
                Math.min(height - 1, centerY + radius);

        for (int y = minY; y <= maxY; y++) {
            int rowOffset =
                    y * width;

            for (int x = minX; x <= maxX; x++) {
                if (faceCodeBuffer[rowOffset + x]
                        == expectedFaceCode) {

                    return true;
                }
            }
        }

        return false;
    }

    private void accumulatePixelVisibility(
            CameraDirectionType direction,
            int[] faceCodeBuffer
    ) {
        FaceVisibilityData visibilityData =
                getFaceVisibilityData(direction);

        for (int faceCode : faceCodeBuffer) {
            if (faceCode == BACKGROUND_FACE_CODE) {
                continue;
            }

            visibilityData.incrementPixelFaceVisibility(
                    faceCode
            );
        }
    }

    private void addInnerPointValidCamera(
            GaiaFace face,
            CameraDirectionType direction
    ) {
        mapFaceIdToInnerPointValidCameras
                .computeIfAbsent(
                        face.getId(),
                        ignored -> new HashSet<>()
                )
                .add(direction);
    }

    public void clear() {
        for (FaceVisibilityData data
                : faceVisibilityDataMap.values()) {

            if (data != null) {
                data.deleteObjects();
            }
        }

        faceVisibilityDataMap.clear();
        mapFaceIdToInnerPointValidCameras.clear();
    }

    public void deleteObjects() {
        clear();
    }

    public Map<GaiaFace, CameraDirectionType> solveCameraDirectionTypeToFaces(
            List<GaiaPrimitive> gaiaPrimitives
    ) {
        Map<GaiaFace, List<CameraDirectionCandidate>> mapFaceToCamCandidates = new HashMap<>();
        Map<GaiaFace, Vector3d> mapFaceToNormal = new HashMap<>();
        Map<GaiaFace, List<GaiaFace>> mapFaceToNeighbors = new HashMap<>();

        if (gaiaPrimitives == null || gaiaPrimitives.isEmpty()) {
            return new HashMap<>();
        }

        for (GaiaPrimitive primitive : gaiaPrimitives) {
            if (primitive == null || primitive.getVertices() == null) {
                continue;
            }

            List<GaiaVertex> vertices = primitive.getVertices();
            List<GaiaFace> primitiveFaces = new ArrayList<>();

            List<GaiaSurface> surfaces = primitive.getSurfaces();
            if (surfaces == null || surfaces.isEmpty()) {
                continue;
            }

            for (GaiaSurface surface : surfaces) {
                if (surface == null || surface.getFaces() == null) {
                    continue;
                }

                for (GaiaFace face : surface.getFaces()) {
                    if (face == null) {
                        continue;
                    }

                    face.calculateFaceNormal(vertices);
                    Vector3d normal = face.getFaceNormal();

                    if (normal != null && normal.lengthSquared() > 1e-12) {
                        mapFaceToNormal.put(face, new Vector3d(normal).normalize());
                    }

                    List<CameraDirectionCandidate> candidates =
                            getCameraDirectionCandidatesOfFace(face);

                    mapFaceToCamCandidates.put(face, candidates);
                    primitiveFaces.add(face);
                }
            }

            Map<GaiaFace, List<GaiaFace>> primitiveNeighbors =
                    buildGaiaFaceNeighbors(primitiveFaces);

            for (Map.Entry<GaiaFace, List<GaiaFace>> entry : primitiveNeighbors.entrySet()) {
                mapFaceToNeighbors
                        .computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                        .addAll(entry.getValue());
            }
        }

        return buildCameraIslandsByPropagation(
                new ArrayList<>(mapFaceToCamCandidates.keySet()),
                mapFaceToCamCandidates,
                mapFaceToNeighbors,
                mapFaceToNormal,
                0.70,
                Math.cos(Math.toRadians(30.0)),
                4
        );
    }

    private Map<GaiaFace, CameraDirectionType> buildCameraIslandsByPropagation(
            List<GaiaFace> allFaces,
            Map<GaiaFace, List<CameraDirectionCandidate>> mapFaceToCamCandidates,
            Map<GaiaFace, List<GaiaFace>> mapFaceToNeighbors,
            Map<GaiaFace, Vector3d> mapFaceToNormal,
            double minCandidateRatio,
            double normalDotThreshold,
            int maxSeedCandidatesToTry
    ) {
        Map<GaiaFace, CameraDirectionType> result = new HashMap<>();
        Set<GaiaFace> visited = new HashSet<>();

        for (GaiaFace seedFace : allFaces) {
            if (seedFace == null || visited.contains(seedFace)) {
                continue;
            }

            List<CameraDirectionCandidate> seedCandidates = mapFaceToCamCandidates.get(seedFace);
            if (seedCandidates == null || seedCandidates.isEmpty()) {
                Vector3d normal = mapFaceToNormal.get(seedFace);

                CameraDirectionType fallbackCamera =
                        CameraDirectionType.getBest9CameraDirectionTypeByNormal(normal);

                result.put(seedFace, fallbackCamera);
                visited.add(seedFace);
                continue;
            }

            List<GaiaFace> bestIsland = null;
            CameraDirectionType bestCameraDirectionType = null;
            double bestIslandScore = -1.0;

            int candidatesToTry = Math.min(maxSeedCandidatesToTry, seedCandidates.size());

            for (int i = 0; i < candidatesToTry; i++) {
                CameraDirectionType candidateCamera = seedCandidates.get(i).cameraDirectionType;

                if (candidateCamera == null) {
                    continue;
                }

                Set<GaiaFace> candidateVisited = new HashSet<>();

                List<GaiaFace> island = growCameraIsland(
                        seedFace,
                        candidateCamera,
                        mapFaceToCamCandidates,
                        mapFaceToNeighbors,
                        mapFaceToNormal,
                        visited,
                        candidateVisited,
                        minCandidateRatio,
                        normalDotThreshold
                );

                double islandScore = calculateIslandCompatibilityScore(
                        island,
                        candidateCamera,
                        mapFaceToCamCandidates
                );

                if (islandScore > bestIslandScore) {
                    bestIslandScore = islandScore;
                    bestIsland = island;
                    bestCameraDirectionType = candidateCamera;
                }
            }

            if (bestIsland == null || bestIsland.isEmpty() || bestCameraDirectionType == null) {
                bestIsland = new ArrayList<>();
                bestIsland.add(seedFace);
                bestCameraDirectionType = seedCandidates.get(0).cameraDirectionType;
            }

            for (GaiaFace face : bestIsland) {
                visited.add(face);
                result.put(face, bestCameraDirectionType);
            }
        }

        for (int cleanIter = 0; cleanIter < 2; cleanIter++) {
            absorbFacesByNeighborMajority(
                    allFaces,
                    mapFaceToNeighbors,
                    mapFaceToCamCandidates,
                    result,
                    0.55,
                    2
            );

            absorbSmallCameraComponents(
                    allFaces,
                    mapFaceToNeighbors,
                    mapFaceToCamCandidates,
                    result,
                    0.50,
                    4
            );

            absorbFacesByNeighborMajority(
                    allFaces,
                    mapFaceToNeighbors,
                    mapFaceToCamCandidates,
                    result,
                    0.35,
                    3
            );

            absorbSmallCameraComponents(
                    allFaces,
                    mapFaceToNeighbors,
                    mapFaceToCamCandidates,
                    result,
                    0.35,
                    8
            );
        }

        return result;
    }

    private void absorbSmallCameraComponents(
            List<GaiaFace> allFaces,
            Map<GaiaFace, List<GaiaFace>> neighborsMap,
            Map<GaiaFace, List<CameraDirectionCandidate>> candidatesMap,
            Map<GaiaFace, CameraDirectionType> cameraByFace,
            double minCandidateRatio,
            int maxSmallComponentFaces
    ) {
        Set<GaiaFace> visited = new HashSet<>();

        for (GaiaFace seed : allFaces) {
            if (seed == null || visited.contains(seed)) {
                continue;
            }

            CameraDirectionType cam = cameraByFace.get(seed);
            if (cam == null) {
                continue;
            }

            List<GaiaFace> component = collectSameCameraComponent(
                    seed,
                    cam,
                    neighborsMap,
                    cameraByFace,
                    visited
            );

            if (component.size() > maxSmallComponentFaces) {
                continue;
            }

            CameraDirectionType neighborDominantCam = findDominantNeighborCamera(
                    component,
                    neighborsMap,
                    cameraByFace
            );

            if (neighborDominantCam == null || neighborDominantCam == cam) {
                continue;
            }

            boolean allCompatible = true;

            for (GaiaFace face : component) {
                if (!isCameraCompatible(face, neighborDominantCam, candidatesMap, minCandidateRatio)) {
                    allCompatible = false;
                    break;
                }
            }

            if (!allCompatible) {
                continue;
            }

            for (GaiaFace face : component) {
                cameraByFace.put(face, neighborDominantCam);
            }
        }
    }

    private List<GaiaFace> collectSameCameraComponent(
            GaiaFace seed,
            CameraDirectionType camera,
            Map<GaiaFace, List<GaiaFace>> neighborsMap,
            Map<GaiaFace, CameraDirectionType> cameraByFace,
            Set<GaiaFace> globalVisited
    ) {
        List<GaiaFace> component = new ArrayList<>();
        ArrayDeque<GaiaFace> queue = new ArrayDeque<>();

        globalVisited.add(seed);
        queue.add(seed);

        while (!queue.isEmpty()) {
            GaiaFace current = queue.poll();
            component.add(current);

            List<GaiaFace> neighbors = neighborsMap.get(current);
            if (neighbors == null) {
                continue;
            }

            for (GaiaFace neighbor : neighbors) {
                if (neighbor == null || globalVisited.contains(neighbor)) {
                    continue;
                }

                if (cameraByFace.get(neighbor) != camera) {
                    continue;
                }

                globalVisited.add(neighbor);
                queue.add(neighbor);
            }
        }

        return component;
    }

    private CameraDirectionType findDominantNeighborCamera(
            List<GaiaFace> component,
            Map<GaiaFace, List<GaiaFace>> neighborsMap,
            Map<GaiaFace, CameraDirectionType> cameraByFace
    ) {
        if (component == null || component.isEmpty()) {
            return null;
        }

        CameraDirectionType ownCamera = cameraByFace.get(component.get(0));
        CameraDirectionType[] cameraTypes = CameraDirectionType.values();
        int[] countByCamera = new int[cameraTypes.length];

        for (GaiaFace face : component) {
            List<GaiaFace> neighbors = neighborsMap.get(face);

            if (neighbors == null) {
                continue;
            }

            for (GaiaFace neighbor : neighbors) {
                if (neighbor == null) {
                    continue;
                }

                CameraDirectionType neighborCamera = cameraByFace.get(neighbor);

                if (neighborCamera == null || neighborCamera == ownCamera) {
                    continue;
                }

                countByCamera[neighborCamera.ordinal()]++;
            }
        }

        CameraDirectionType bestCamera = null;
        int bestCount = 0;

        for (CameraDirectionType cameraType : cameraTypes) {
            int count = countByCamera[cameraType.ordinal()];

            if (count > bestCount) {
                bestCount = count;
                bestCamera = cameraType;
            }
        }

        return bestCamera;
    }

    private boolean isCameraCompatible(
            GaiaFace face,
            CameraDirectionType cameraDirectionType,
            Map<GaiaFace, List<CameraDirectionCandidate>> mapFaceToCamCandidates,
            double minCandidateRatio
    ) {
        List<CameraDirectionCandidate> candidates = mapFaceToCamCandidates.get(face);

        if (candidates == null || candidates.isEmpty()) {
            return false;
        }

        int bestPixelCount = candidates.get(0).pixelCount;
        if (bestPixelCount <= 0) {
            return false;
        }

        for (CameraDirectionCandidate candidate : candidates) {
            if (candidate.cameraDirectionType != cameraDirectionType) {
                continue;
            }

            double ratio = (double) candidate.pixelCount / (double) bestPixelCount;
            return ratio >= minCandidateRatio;
        }

        return false;
    }

    private void absorbFacesByNeighborMajority(
            List<GaiaFace> faces,
            Map<GaiaFace, List<GaiaFace>> neighborsMap,
            Map<GaiaFace, List<CameraDirectionCandidate>> candidatesMap,
            Map<GaiaFace, CameraDirectionType> cameraByFace,
            double minCandidateRatio,
            int iterations
    ) {
        CameraDirectionType[] cameraTypes = CameraDirectionType.values();
        int[] countByCamera = new int[cameraTypes.length];

        for (int iter = 0; iter < iterations; iter++) {
            List<GaiaFace> changedFaces = new ArrayList<>();
            List<CameraDirectionType> changedCameras = new ArrayList<>();

            for (GaiaFace face : faces) {
                if (face == null) {
                    continue;
                }

                CameraDirectionType currentCamera = cameraByFace.get(face);
                List<GaiaFace> neighbors = neighborsMap.get(face);

                if (neighbors == null || neighbors.isEmpty()) {
                    continue;
                }

                Arrays.fill(countByCamera, 0);

                for (GaiaFace neighbor : neighbors) {
                    CameraDirectionType neighborCamera = cameraByFace.get(neighbor);

                    if (neighborCamera != null) {
                        countByCamera[neighborCamera.ordinal()]++;
                    }
                }

                CameraDirectionType dominantCamera = null;
                int dominantCount = 0;

                for (CameraDirectionType cameraType : cameraTypes) {
                    int count = countByCamera[cameraType.ordinal()];

                    if (count > dominantCount) {
                        dominantCount = count;
                        dominantCamera = cameraType;
                    }
                }

                if (dominantCamera == null || dominantCamera == currentCamera) {
                    continue;
                }

                int neighborCount = neighbors.size();

                boolean stronglySurrounded = dominantCount >= 3;
                boolean mostlySurrounded =
                        dominantCount * 100 >= neighborCount * 66;

                if (!stronglySurrounded && !mostlySurrounded) {
                    continue;
                }

                double ratio = getCameraCandidateRatio(
                        face,
                        dominantCamera,
                        candidatesMap
                );

                double requiredRatio =
                        stronglySurrounded ? 0.25 : minCandidateRatio;

                if (ratio < requiredRatio) {
                    continue;
                }

                changedFaces.add(face);
                changedCameras.add(dominantCamera);
            }

            if (changedFaces.isEmpty()) {
                break;
            }

            for (int i = 0; i < changedFaces.size(); i++) {
                cameraByFace.put(
                        changedFaces.get(i),
                        changedCameras.get(i)
                );
            }
        }
    }

    private double getCameraCandidateRatio(
            GaiaFace face,
            CameraDirectionType cameraDirectionType,
            Map<GaiaFace, List<CameraDirectionCandidate>> candidatesMap
    ) {
        List<CameraDirectionCandidate> candidates = candidatesMap.get(face);

        if (candidates == null || candidates.isEmpty()) {
            return 0.0;
        }

        int bestPixelCount = candidates.get(0).pixelCount;
        if (bestPixelCount <= 0) {
            return 0.0;
        }

        for (CameraDirectionCandidate candidate : candidates) {
            if (candidate.cameraDirectionType == cameraDirectionType) {
                return (double) candidate.pixelCount / (double) bestPixelCount;
            }
        }

        return 0.0;
    }

    private double calculateIslandCompatibilityScore(
            List<GaiaFace> island,
            CameraDirectionType cameraDirectionType,
            Map<GaiaFace, List<CameraDirectionCandidate>> mapFaceToCamCandidates
    ) {
        if (island == null || island.isEmpty() || cameraDirectionType == null) {
            return 0.0;
        }

        double score = 0.0;

        for (GaiaFace face : island) {
            List<CameraDirectionCandidate> candidates = mapFaceToCamCandidates.get(face);

            if (candidates == null || candidates.isEmpty()) {
                continue;
            }

            int bestPixelCount = candidates.get(0).pixelCount;
            if (bestPixelCount <= 0) {
                continue;
            }

            for (CameraDirectionCandidate candidate : candidates) {
                if (candidate.cameraDirectionType == cameraDirectionType) {
                    double ratio = (double) candidate.pixelCount / (double) bestPixelCount;
                    score += candidate.pixelCount * ratio;
                    break;
                }
            }
        }

        return score;
    }

    private List<GaiaFace> growCameraIsland(
            GaiaFace seedFace,
            CameraDirectionType islandCameraDirectionType,
            Map<GaiaFace, List<CameraDirectionCandidate>>
                    mapFaceToCamCandidates,
            Map<GaiaFace, List<GaiaFace>>
                    mapFaceToNeighbors,
            Map<GaiaFace, Vector3d>
                    mapFaceToNormal,
            Set<GaiaFace> globallyVisited,
            Set<GaiaFace> candidateVisited,
            double minCandidateRatio,
            double normalDotThreshold
    ) {
        List<GaiaFace> island = new ArrayList<>();
        ArrayDeque<GaiaFace> queue = new ArrayDeque<>();

        if (seedFace == null || globallyVisited.contains(seedFace)) {
            return island;
        }

        candidateVisited.add(seedFace);
        queue.add(seedFace);

        while (!queue.isEmpty()) {
            GaiaFace currentFace = queue.poll();
            island.add(currentFace);

            List<GaiaFace> neighbors = mapFaceToNeighbors.get(currentFace);

            if (neighbors == null || neighbors.isEmpty()) {
                continue;
            }

            Vector3d currentNormal = mapFaceToNormal.get(currentFace);

            for (GaiaFace neighbor : neighbors) {
                if (neighbor == null) {
                    continue;
                }

                if (globallyVisited.contains(neighbor)
                        || candidateVisited.contains(neighbor)) {
                    continue;
                }

                if (!isCameraCompatible(
                        neighbor,
                        islandCameraDirectionType,
                        mapFaceToCamCandidates,
                        minCandidateRatio
                )) {
                    continue;
                }

                Vector3d neighborNormal = mapFaceToNormal.get(neighbor);

                if (currentNormal != null
                        && neighborNormal != null
                        && currentNormal.dot(neighborNormal)
                        < normalDotThreshold) {

                    continue;
                }

                candidateVisited.add(neighbor);
                queue.add(neighbor);
            }
        }

        return island;
    }

    private Map<GaiaFace, List<GaiaFace>> buildGaiaFaceNeighbors(List<GaiaFace> faces) {
        Map<GaiaWeldedFacesFinder.EdgeKey, List<GaiaFace>> edgeToFaces = new HashMap<>();

        if (faces == null || faces.isEmpty()) {
            return new HashMap<>();
        }

        for (GaiaFace face : faces) {
            if (face == null || face.getIndices() == null || face.getIndices().length < 3) {
                continue;
            }

            int[] indices = face.getIndices();

            for (int i = 0; i < indices.length; i++) {
                int a = indices[i];
                int b = indices[(i + 1) % indices.length];

                GaiaWeldedFacesFinder.EdgeKey edgeKey = new GaiaWeldedFacesFinder.EdgeKey(a, b);
                edgeToFaces.computeIfAbsent(edgeKey, k -> new ArrayList<>()).add(face);
            }
        }

        Map<GaiaFace, List<GaiaFace>> faceToNeighbors = new HashMap<>();

        for (List<GaiaFace> edgeFaces : edgeToFaces.values()) {
            if (edgeFaces == null || edgeFaces.size() < 2) {
                continue;
            }

            for (GaiaFace a : edgeFaces) {
                for (GaiaFace b : edgeFaces) {
                    if (a == b) {
                        continue;
                    }

                    faceToNeighbors.computeIfAbsent(a, k -> new ArrayList<>()).add(b);
                }
            }
        }

        return faceToNeighbors;
    }

    public List<CameraDirectionCandidate> getCameraDirectionCandidatesOfFace(
            GaiaFace face
    ) {
        List<CameraDirectionCandidate> candidates = new ArrayList<>();

        if (face == null) {
            return candidates;
        }

        int faceId = face.getId();

        for (Map.Entry<CameraDirectionType, FaceVisibilityData> entry : faceVisibilityDataMap.entrySet()) {
            CameraDirectionType cameraDirectionType = entry.getKey();

            if (!isInnerPointCameraValid(face, cameraDirectionType)) {
                continue;
            }

            int pixelCount = entry.getValue().getPixelFaceVisibility(faceId);
            if (pixelCount <= 0) {
                continue;
            }

            candidates.add(new CameraDirectionCandidate(cameraDirectionType, pixelCount));
        }

        candidates.sort((a, b) -> Integer.compare(b.pixelCount, a.pixelCount));
        return candidates;
    }

    private boolean isInnerPointCameraValid(
            GaiaFace face,
            CameraDirectionType cameraDirectionType
    ) {
        if (face == null || cameraDirectionType == null) {
            return false;
        }

        Set<CameraDirectionType> validCameras =
                mapFaceIdToInnerPointValidCameras.get(face.getId());

        if (validCameras == null || validCameras.isEmpty()) {
            return false;
        }

        return validCameras.contains(cameraDirectionType);
    }
}
