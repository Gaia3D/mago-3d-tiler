package com.gaia3d.renderer.engine.dataStructure;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.modifier.topology.GaiaExtractor;
import com.gaia3d.basic.halfedge.CameraDirectionType;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.remesher.GaiaWeldedFacesFinder;
import com.gaia3d.renderer.engine.fbo.Fbo;
import org.joml.Matrix4d;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector4d;

import java.nio.ByteBuffer;
import java.util.*;

import static com.gaia3d.basic.halfedge.CameraDirectionType.ZNEG;
import static org.lwjgl.opengl.GL11.GL_RGBA;

public class FaceVisibilityDataManagerV3 {
    private final Map<CameraDirectionType, FaceVisibilityData> faceVisibilityDataMap;
    private final Map<Integer, Set<CameraDirectionType>> mapFaceIdToInnerPointValidCameras;

    public FaceVisibilityDataManagerV3() {
        this.faceVisibilityDataMap = new HashMap<>();
        this.mapFaceIdToInnerPointValidCameras = new HashMap<>();
    }

    public void clear() {
        for (FaceVisibilityData data : faceVisibilityDataMap.values()) {
            if (data != null) {
                data.deleteObjects();
            }
        }

        faceVisibilityDataMap.clear();
        mapFaceIdToInnerPointValidCameras.clear();
    }

    public FaceVisibilityData getFaceVisibilityData(CameraDirectionType cameraDirectionType) {
        return faceVisibilityDataMap.computeIfAbsent(
                cameraDirectionType,
                FaceVisibilityData::new
        );
    }



    public void updateFaceInnerPointsVisibilityData(
            GaiaScene scene,
            CameraDirectionType cameraDirectionType,
            Fbo colorCodeFbo,
            Map<Integer, Map<CameraDirectionType, Matrix4d>> mapClassificationCamDirTypeModelViewMatrix,
            Map<Integer, Map<CameraDirectionType, GaiaBoundingBox>> mapClassificationCamDirTypeBBox
    ) {
        if (scene == null || cameraDirectionType == null || colorCodeFbo == null) {
            return;
        }

        int classifiedId = -1;

        Map<CameraDirectionType, Matrix4d> camDirTypeModelViewMatrix =
                mapClassificationCamDirTypeModelViewMatrix.get(classifiedId);

        if (camDirTypeModelViewMatrix == null) {
            return;
        }

        Matrix4d modelViewMatrix = camDirTypeModelViewMatrix.get(cameraDirectionType);
        if (modelViewMatrix == null) {
            return;
        }

        Map<CameraDirectionType, GaiaBoundingBox> camDirTypeBBox =
                mapClassificationCamDirTypeBBox.get(classifiedId);

        if (camDirTypeBBox == null) {
            return;
        }

        GaiaBoundingBox transformedTargetBbox = camDirTypeBBox.get(cameraDirectionType);
        if (transformedTargetBbox == null) {
            return;
        }

        colorCodeFbo.bind();

        int fboWidth = colorCodeFbo.getFboWidth();
        int fboHeight = colorCodeFbo.getFboHeight();
        ByteBuffer pixels = colorCodeFbo.readPixels(GL_RGBA);

        colorCodeFbo.unbind();

        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaPrimitive> primitives = extractor.extractAllPrimitives(scene);

        if (primitives == null || primitives.isEmpty()) {
            pixels.clear();
            return;
        }

        final int minVisibleInnerPoints = 3;
        final int searchRadius = 2;

        for (GaiaPrimitive primitive : primitives) {
            if (primitive == null || primitive.getVertices() == null) {
                continue;
            }

            List<GaiaVertex> vertices = primitive.getVertices();
            if (vertices == null || vertices.isEmpty()) {
                continue;
            }

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

                    List<Vector3d> innerPoints =
                            getFaceInnerVisibilityPoints(face, vertices);

                    if (innerPoints.isEmpty()) {
                        continue;
                    }

                    int expectedColorCode = face.getId();
                    int visiblePointsCount = 0;

                    for (Vector3d point : innerPoints) {
                        Vector4d transformed =
                                new Vector4d(point, 1.0).mul(modelViewMatrix);

                        Vector2i pixel = transformedPointToPixel(
                                transformed,
                                transformedTargetBbox,
                                fboWidth,
                                fboHeight,
                                true
                        );

                        if (pixel == null) {
                            continue;
                        }

                        boolean pointVisibleInvY = hasExpectedColorNearPixel(
                                pixels,
                                fboWidth,
                                fboHeight,
                                pixel.x,
                                fboHeight - 1 - pixel.y,
                                expectedColorCode,
                                searchRadius
                        );

                        if (pointVisibleInvY) {
                            visiblePointsCount++;
                        }
                    }

                    if (visiblePointsCount >= minVisibleInnerPoints) {
                        addInnerPointValidCamera(face, cameraDirectionType);
                    }
                }
            }
        }

        updateFaceVisibilityData(cameraDirectionType, pixels, fboWidth, fboHeight);
        pixels.clear();
    }

    private void updateFaceVisibilityData(
            CameraDirectionType cameraDirectionType,
            ByteBuffer pixels,
            int fboWidth,
            int fboHeight
    ) {
        if (cameraDirectionType == null || pixels == null) {
            return;
        }

        FaceVisibilityData faceVisibilityData = getFaceVisibilityData(cameraDirectionType);

        for (int y = 0; y < fboHeight; y++) {
            for (int x = 0; x < fboWidth; x++) {
                int colorCode = getRgbaColorCodeAt(
                        pixels,
                        fboWidth,
                        fboHeight,
                        x,
                        y
                );

                if (colorCode != 0xFFFFFFFF) {
                    faceVisibilityData.incrementPixelFaceVisibility(colorCode);
                }
            }
        }
    }

    private void addInnerPointValidCamera(
            GaiaFace face,
            CameraDirectionType cameraDirectionType
    ) {
        if (face == null || cameraDirectionType == null) {
            return;
        }

        mapFaceIdToInnerPointValidCameras
                .computeIfAbsent(face.getId(), k -> new HashSet<>())
                .add(cameraDirectionType);
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

                Set<GaiaFace> previewVisited = new HashSet<>(visited);

                List<GaiaFace> island = growCameraIsland(
                        seedFace,
                        candidateCamera,
                        mapFaceToCamCandidates,
                        mapFaceToNeighbors,
                        mapFaceToNormal,
                        previewVisited,
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
        Set<GaiaFace> componentSet = new HashSet<>(component);
        Map<CameraDirectionType, Integer> countByCamera = new HashMap<>();

        CameraDirectionType ownCamera = null;
        if (!component.isEmpty()) {
            ownCamera = cameraByFace.get(component.get(0));
        }

        for (GaiaFace face : component) {
            List<GaiaFace> neighbors = neighborsMap.get(face);
            if (neighbors == null) {
                continue;
            }

            for (GaiaFace neighbor : neighbors) {
                if (neighbor == null || componentSet.contains(neighbor)) {
                    continue;
                }

                CameraDirectionType neighborCam = cameraByFace.get(neighbor);

                if (neighborCam == null || neighborCam == ownCamera) {
                    continue;
                }

                countByCamera.merge(neighborCam, 1, Integer::sum);
            }
        }

        CameraDirectionType best = null;
        int bestCount = 0;

        for (Map.Entry<CameraDirectionType, Integer> entry : countByCamera.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                best = entry.getKey();
            }
        }

        return best;
    }

    private void absorbFacesByNeighborMajority(
            List<GaiaFace> faces,
            Map<GaiaFace, List<GaiaFace>> neighborsMap,
            Map<GaiaFace, List<CameraDirectionCandidate>> candidatesMap,
            Map<GaiaFace, CameraDirectionType> cameraByFace,
            double minCandidateRatio,
            int iterations
    ) {
        for (int iter = 0; iter < iterations; iter++) {
            Map<GaiaFace, CameraDirectionType> changes = new HashMap<>();

            for (GaiaFace face : faces) {
                CameraDirectionType currentCam = cameraByFace.get(face);

                List<GaiaFace> neighbors = neighborsMap.get(face);
                if (neighbors == null || neighbors.isEmpty()) {
                    continue;
                }

                Map<CameraDirectionType, Integer> countByCam = new HashMap<>();

                for (GaiaFace neighbor : neighbors) {
                    CameraDirectionType neighborCam = cameraByFace.get(neighbor);
                    if (neighborCam == null) {
                        continue;
                    }

                    countByCam.merge(neighborCam, 1, Integer::sum);
                }

                CameraDirectionType dominantCam = null;
                int dominantCount = 0;

                for (Map.Entry<CameraDirectionType, Integer> entry : countByCam.entrySet()) {
                    if (entry.getValue() > dominantCount) {
                        dominantCount = entry.getValue();
                        dominantCam = entry.getKey();
                    }
                }

                if (dominantCam == null || dominantCam == currentCam) {
                    continue;
                }

                // Para triángulos rodeados, exigir mayoría fuerte.
                int neighborCount = neighbors.size();

                boolean stronglySurrounded = dominantCount >= 3;
                boolean mostlySurrounded = neighborCount > 0 && dominantCount >= Math.ceil(neighborCount * 0.66);

                if (!stronglySurrounded && !mostlySurrounded) {
                    continue;
                }

                double ratio = getCameraCandidateRatio(face, dominantCam, candidatesMap);

// Si está muy rodeado, permitir ratio bajo.
                double requiredRatio = stronglySurrounded ? 0.25 : minCandidateRatio;

                if (ratio < requiredRatio) {
                    continue;
                }

                changes.put(face, dominantCam);
            }

            if (changes.isEmpty()) {
                break;
            }

            for (Map.Entry<GaiaFace, CameraDirectionType> entry : changes.entrySet()) {
                cameraByFace.put(entry.getKey(), entry.getValue());
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
            Map<GaiaFace, List<CameraDirectionCandidate>> mapFaceToCamCandidates,
            Map<GaiaFace, List<GaiaFace>> mapFaceToNeighbors,
            Map<GaiaFace, Vector3d> mapFaceToNormal,
            Set<GaiaFace> visited,
            double minCandidateRatio,
            double normalDotThreshold
    ) {
        List<GaiaFace> island = new ArrayList<>();
        ArrayDeque<GaiaFace> queue = new ArrayDeque<>();

        Vector3d seedNormal = mapFaceToNormal.get(seedFace);

        visited.add(seedFace);
        queue.add(seedFace);

        while (!queue.isEmpty()) {
            GaiaFace currentFace = queue.poll();
            island.add(currentFace);

            List<GaiaFace> neighbors = mapFaceToNeighbors.get(currentFace);
            if (neighbors == null || neighbors.isEmpty()) {
                continue;
            }

            for (GaiaFace neighbor : neighbors) {
                if (neighbor == null || visited.contains(neighbor)) {
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

                Vector3d currentNormal = mapFaceToNormal.get(currentFace);
                Vector3d neighborNormal = mapFaceToNormal.get(neighbor);

                if (currentNormal != null && neighborNormal != null) {
                    double dot = currentNormal.dot(neighborNormal);

                    if (dot < normalDotThreshold) {
                        continue;
                    }
                }

                visited.add(neighbor);
                queue.add(neighbor);
            }
        }

        return island;
    }

    private List<Vector3d> getFaceInnerVisibilityPoints(
            GaiaFace face,
            List<GaiaVertex> vertices
    ) {
        List<Vector3d> points = new ArrayList<>();

        if (face == null || vertices == null) {
            return points;
        }

        int[] indices = face.getIndices();
        if (indices == null || indices.length < 3) {
            return points;
        }

        Vector3d p0 = vertices.get(indices[0]).getPosition();
        Vector3d p1 = vertices.get(indices[1]).getPosition();
        Vector3d p2 = vertices.get(indices[2]).getPosition();

        Vector3d centroid = new Vector3d(p0)
                .add(p1)
                .add(p2)
                .mul(1.0 / 3.0);

        double moveRatio = 0.20;

        points.add(new Vector3d(centroid));
        points.add(new Vector3d(p0).lerp(centroid, moveRatio));
        points.add(new Vector3d(p1).lerp(centroid, moveRatio));
        points.add(new Vector3d(p2).lerp(centroid, moveRatio));

        return points;
    }

    private boolean hasExpectedColorNearPixel(
            ByteBuffer pixels,
            int width,
            int height,
            int cx,
            int cy,
            int expectedColorCode,
            int radius
    ) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = cx + dx;
                int y = cy + dy;

                if (x < 0 || y < 0 || x >= width || y >= height) {
                    continue;
                }

                int colorCode = getRgbaColorCodeAt(pixels, width, height, x, y);

                if (colorCode == expectedColorCode) {
                    return true;
                }
            }
        }

        return false;
    }

    private int getRgbaColorCodeAt(
            ByteBuffer pixels,
            int width,
            int height,
            int x,
            int y
    ) {
        if (pixels == null || x < 0 || y < 0 || x >= width || y >= height) {
            return 0xFFFFFFFF;
        }

        int index = (y * width + x) * 4;

        int r = pixels.get(index) & 0xFF;
        int g = pixels.get(index + 1) & 0xFF;
        int b = pixels.get(index + 2) & 0xFF;
        int a = pixels.get(index + 3) & 0xFF;

        return (r << 24) | (g << 16) | (b << 8) | a;
    }

    private Vector2i transformedPointToPixel(
            Vector4d transformed,
            GaiaBoundingBox bbox,
            int width,
            int height,
            boolean invertY
    ) {
        if (transformed == null || bbox == null) {
            return null;
        }

        double sizeX = bbox.getMaxX() - bbox.getMinX();
        double sizeY = bbox.getMaxY() - bbox.getMinY();

        if (sizeX <= 1e-12 || sizeY <= 1e-12) {
            return null;
        }

        double u = (transformed.x - bbox.getMinX()) / sizeX;
        double v = (transformed.y - bbox.getMinY()) / sizeY;

        int x = (int) Math.round(u * (width - 1));
        int y = (int) Math.round(v * (height - 1));

        if (invertY) {
            y = height - 1 - y;
        }

        if (x < 0 || y < 0 || x >= width || y >= height) {
            return null;
        }

        return new Vector2i(x, y);
    }

    public void deleteObjects() {
        for (FaceVisibilityData faceVisibilityData : faceVisibilityDataMap.values()) {
            if (faceVisibilityData != null) {
                faceVisibilityData.deleteObjects();
            }
        }

        faceVisibilityDataMap.clear();
        mapFaceIdToInnerPointValidCameras.clear();
    }
}