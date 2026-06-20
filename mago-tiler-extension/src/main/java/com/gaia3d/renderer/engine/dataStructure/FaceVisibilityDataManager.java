package com.gaia3d.renderer.engine.dataStructure;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.modifier.topology.GaiaExtractor;
import com.gaia3d.basic.halfedge.CameraDirectionType;
import com.gaia3d.basic.model.*;
import com.gaia3d.renderer.engine.fbo.Fbo;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector4d;

import java.nio.ByteBuffer;
import java.util.*;

import static org.lwjgl.opengl.GL11.GL_RGBA;

@Getter
@Setter
public class FaceVisibilityDataManager {
    private final Map<GaiaFace, List<CameraDirectionCandidate>> mapFaceToCamCandidates;
    private final Map<CameraDirectionType, FaceVisibilityData> faceVisibilityDataMap; // old

    public FaceVisibilityDataManager() {
        this.mapFaceToCamCandidates = new HashMap<>();
        this.faceVisibilityDataMap = new HashMap<>();
    }

    private static class EdgeKey {
        private final int a;
        private final int b;

        EdgeKey(int i0, int i1) {
            if (i0 <= i1) {
                this.a = i0;
                this.b = i1;
            } else {
                this.a = i1;
                this.b = i0;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof EdgeKey)) {
                return false;
            }

            EdgeKey other = (EdgeKey) obj;
            return this.a == other.a && this.b == other.b;
        }

        @Override
        public int hashCode() {
            return 31 * a + b;
        }
    }

    public FaceVisibilityData getFaceVisibilityData(CameraDirectionType cameraDirectionType) {
        // if no existing data, create new one
        if (!faceVisibilityDataMap.containsKey(cameraDirectionType)) {
            faceVisibilityDataMap.put(cameraDirectionType, new FaceVisibilityData(cameraDirectionType));
        }
        return faceVisibilityDataMap.get(cameraDirectionType);
    }

    public CameraDirectionType getBestCameraDirectionTypeOfFace(int faceId) {
        CameraDirectionType bestCameraDirectionType = null;
        int maxPixelCount = 0;
        for (Map.Entry<CameraDirectionType, FaceVisibilityData> entry : faceVisibilityDataMap.entrySet()) {
            int pixelCount = entry.getValue().getPixelFaceVisibility(faceId);
            if (pixelCount > maxPixelCount) {
                maxPixelCount = pixelCount;
                bestCameraDirectionType = entry.getKey();
            }
        }
        return bestCameraDirectionType;
    }

    public List<CameraDirectionCandidate> getCameraDirectionCandidatesOfFace(int faceId) {
        List<CameraDirectionCandidate> candidates = new ArrayList<>();

        for (Map.Entry<CameraDirectionType, FaceVisibilityData> entry : faceVisibilityDataMap.entrySet()) {
            CameraDirectionType cameraDirectionType = entry.getKey();
            int pixelCount = entry.getValue().getPixelFaceVisibility(faceId);
            if (pixelCount <= 0) {
                continue;
            }

            candidates.add(new CameraDirectionCandidate(cameraDirectionType, pixelCount));
        }

        candidates.sort((a, b) -> Integer.compare(b.pixelCount, a.pixelCount));
        return candidates;
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

    public Map<GaiaFace, CameraDirectionType> solveCameraDirectionTypeToFaces(
            List<GaiaPrimitive> gaiaPrimitives
    ) {
        Map<GaiaFace, CameraDirectionType> result = new HashMap<>();
        Map<GaiaFace, Vector3d> mapFaceToNormal = new HashMap<>();
        Map<GaiaFace, List<GaiaFace>> mapFaceToNeighbors = new HashMap<>();
        List<GaiaFace> allFaces = new ArrayList<>();

        if (gaiaPrimitives == null || gaiaPrimitives.isEmpty()) {
            return result;
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
                            mapFaceToCamCandidates.get(face);

                    if (candidates != null && !candidates.isEmpty()) {
                        candidates.sort((a, b) -> Integer.compare(b.pixelCount, a.pixelCount));
                    }

                    primitiveFaces.add(face);
                    allFaces.add(face);
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

        return buildCameraIslandsFromCandidates(
                allFaces,
                mapFaceToCamCandidates,
                mapFaceToNeighbors,
                mapFaceToNormal,
                Math.cos(Math.toRadians(30.0))
        );
    }

    private Map<GaiaFace, CameraDirectionType> buildCameraIslandsFromCandidates(
            List<GaiaFace> allFaces,
            Map<GaiaFace, List<CameraDirectionCandidate>> candidatesMap,
            Map<GaiaFace, List<GaiaFace>> neighborsMap,
            Map<GaiaFace, Vector3d> normalMap,
            double normalDotThreshold
    ) {
        Map<GaiaFace, CameraDirectionType> result = new HashMap<>();
        Set<GaiaFace> visited = new HashSet<>();

        for (GaiaFace seedFace : allFaces) {
            if (seedFace == null || visited.contains(seedFace)) {
                continue;
            }

            List<CameraDirectionCandidate> seedCandidates = candidatesMap.get(seedFace);

            if (seedCandidates == null || seedCandidates.isEmpty()) {
                result.put(seedFace, CameraDirectionType.ZNEG);
                visited.add(seedFace);
                continue;
            }

            CameraDirectionType bestCamera = null;
            List<GaiaFace> bestIsland = null;
            int bestScore = -1;

            for (CameraDirectionCandidate seedCandidate : seedCandidates) {
                if (seedCandidate == null || seedCandidate.cameraDirectionType == null) {
                    continue;
                }

                CameraDirectionType camera = seedCandidate.cameraDirectionType;

                Set<GaiaFace> previewVisited = new HashSet<>(visited);

                List<GaiaFace> island = growIslandForCamera(
                        seedFace,
                        camera,
                        candidatesMap,
                        neighborsMap,
                        normalMap,
                        previewVisited,
                        normalDotThreshold
                );

                int score = calculateIslandScoreByCandidates(
                        island,
                        camera,
                        candidatesMap
                );

                if (score > bestScore) {
                    bestScore = score;
                    bestCamera = camera;
                    bestIsland = island;
                }
            }

            if (bestIsland == null || bestIsland.isEmpty() || bestCamera == null) {
                result.put(seedFace, CameraDirectionType.ZNEG);
                visited.add(seedFace);
                continue;
            }

            for (GaiaFace face : bestIsland) {
                visited.add(face);
                result.put(face, bestCamera);
            }
        }

        return result;
    }

    private List<GaiaFace> growIslandForCamera(
            GaiaFace seedFace,
            CameraDirectionType camera,
            Map<GaiaFace, List<CameraDirectionCandidate>> candidatesMap,
            Map<GaiaFace, List<GaiaFace>> neighborsMap,
            Map<GaiaFace, Vector3d> normalMap,
            Set<GaiaFace> visited,
            double normalDotThreshold
    ) {
        List<GaiaFace> island = new ArrayList<>();
        ArrayDeque<GaiaFace> queue = new ArrayDeque<>();

        visited.add(seedFace);
        queue.add(seedFace);

        while (!queue.isEmpty()) {
            GaiaFace current = queue.poll();
            island.add(current);

            List<GaiaFace> neighbors = neighborsMap.get(current);
            if (neighbors == null || neighbors.isEmpty()) {
                continue;
            }

            Vector3d currentNormal = normalMap.get(current);

            for (GaiaFace neighbor : neighbors) {
                if (neighbor == null || visited.contains(neighbor)) {
                    continue;
                }

                if (!hasCameraCandidate(neighbor, camera, candidatesMap)) {
                    continue;
                }

                Vector3d neighborNormal = normalMap.get(neighbor);

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

    private boolean hasCameraCandidate(
            GaiaFace face,
            CameraDirectionType camera,
            Map<GaiaFace, List<CameraDirectionCandidate>> candidatesMap
    ) {
        List<CameraDirectionCandidate> candidates = candidatesMap.get(face);

        if (candidates == null || candidates.isEmpty()) {
            return false;
        }

        for (CameraDirectionCandidate candidate : candidates) {
            if (candidate.cameraDirectionType == camera) {
                return true;
            }
        }

        return false;
    }

    private int calculateIslandScoreByCandidates(
            List<GaiaFace> island,
            CameraDirectionType camera,
            Map<GaiaFace, List<CameraDirectionCandidate>> candidatesMap
    ) {
        if (island == null || island.isEmpty() || camera == null) {
            return 0;
        }

        int score = 0;

        for (GaiaFace face : island) {
            List<CameraDirectionCandidate> candidates = candidatesMap.get(face);

            if (candidates == null || candidates.isEmpty()) {
                continue;
            }

            for (CameraDirectionCandidate candidate : candidates) {
                if (candidate.cameraDirectionType == camera) {
                    score += candidate.pixelCount;
                    break;
                }
            }
        }

        return score;
    }

    public Map<GaiaFace, CameraDirectionType> solveCameraDirectionTypeToFaces_old(
            List<GaiaPrimitive> gaiaPrimitives
    ) {
        Map<GaiaFace, List<CameraDirectionCandidate>> mapFaceToCamCandidates = new HashMap<>();
        Map<GaiaFace, Vector3d> mapFaceToNormal = new HashMap<>();
        Map<GaiaFace, List<GaiaFace>> mapFaceToNeighbors = new HashMap<>();

        if (gaiaPrimitives == null || gaiaPrimitives.isEmpty()) {
            return new HashMap<>();
        }

        for (GaiaPrimitive gaiaPrimitive : gaiaPrimitives) {
            if (gaiaPrimitive == null || gaiaPrimitive.getVertices() == null) {
                continue;
            }

            List<GaiaFace> primitiveFaces = new ArrayList<>();

            List<GaiaSurface> gaiaSurfaces = gaiaPrimitive.getSurfaces();
            if (gaiaSurfaces == null || gaiaSurfaces.isEmpty()) {
                continue;
            }

            for (GaiaSurface surface : gaiaSurfaces) {
                if (surface == null || surface.getFaces() == null) {
                    continue;
                }

                for (GaiaFace face : surface.getFaces()) {
                    if (face == null) {
                        continue;
                    }

                    int faceId = face.getId();

                    face.calculateFaceNormal(gaiaPrimitive.getVertices());
                    Vector3d normal = face.getFaceNormal();

                    if (normal != null && normal.lengthSquared() > 1e-12) {
                        mapFaceToNormal.put(face, new Vector3d(normal).normalize());
                    }

                    List<CameraDirectionCandidate> candidates = getCameraDirectionCandidatesOfFace(faceId);
                    if (candidates == null) {
                        candidates = new ArrayList<>();
                    }

                    mapFaceToCamCandidates.put(face, candidates);
                    primitiveFaces.add(face);
                }
            }

            // Importante: los índices de GaiaFace suelen ser locales al primitive.
            // Por eso los vecinos se calculan por primitive, no globalmente.
            Map<GaiaFace, List<GaiaFace>> primitiveNeighbors = buildGaiaFaceNeighbors(primitiveFaces);

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
                0.70, // minCandidateRatio
                Math.cos(Math.toRadians(30.0)), // normalDotThreshold
                4 // maxSeedCandidatesToTry
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
                result.put(seedFace, CameraDirectionType.ZNEG);
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

        // Limpieza morfológica sobre el grafo de faces.
        for (int cleanIter = 0; cleanIter < 2; cleanIter++) {
            // Limpieza conservadora
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

            // 2nd pass
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

//        // 3rd pass
//        absorbFacesByNeighborMajority(
//                allFaces,
//                mapFaceToNeighbors,
//                mapFaceToCamCandidates,
//                result,
//                0.20,
//                1
//        );
//
//        absorbSmallCameraComponents(
//                allFaces,
//                mapFaceToNeighbors,
//                mapFaceToCamCandidates,
//                result,
//                0.20,
//                16
//        );

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

    private Map<GaiaFace, List<GaiaFace>> buildGaiaFaceNeighbors(List<GaiaFace> faces) {
        Map<EdgeKey, List<GaiaFace>> edgeToFaces = new HashMap<>();

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

                EdgeKey edgeKey = new EdgeKey(a, b);
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

    public void deleteObjects() {
        for (FaceVisibilityData faceVisibilityData : faceVisibilityDataMap.values()) {
            faceVisibilityData.deleteObjects();
        }
        faceVisibilityDataMap.clear();
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

        points.add(new Vector3d(centroid));

        double moveRatio = 0.20;

        points.add(movePointTowardCenter(p0, centroid, moveRatio));
        points.add(movePointTowardCenter(p1, centroid, moveRatio));
        points.add(movePointTowardCenter(p2, centroid, moveRatio));

        return points;
    }

    private Vector3d movePointTowardCenter(
            Vector3d point,
            Vector3d center,
            double ratio
    ) {
        return new Vector3d(point).lerp(center, ratio);
    }

    public void updateFaceVisibilityData(GaiaScene scene,
                                                                                  CameraDirectionType cameraDirectionType,
                                                                                  Fbo colorCodeFbo,
                                                                                  Map<Integer, Map<CameraDirectionType, Matrix4d>> mapClassificationCamDirTypeModelViewMatrix,
                                                                                  Map<Integer, Map<CameraDirectionType, GaiaBoundingBox>> mapClassificationCamDirTypeBBox) {

        colorCodeFbo.bind();

        // read pixels from fbo
        int fboWidth = colorCodeFbo.getFboWidth();
        int fboHeight = colorCodeFbo.getFboHeight();
        ByteBuffer pixels = colorCodeFbo.readPixels(GL_RGBA);

        // unbind the fbo
        colorCodeFbo.unbind();

        int classifiedId = -1; // here always is -1.
        Map<CameraDirectionType, Matrix4d> camDirTypeModelViewMatrix = mapClassificationCamDirTypeModelViewMatrix.get(classifiedId);
        if (camDirTypeModelViewMatrix == null) {
            return;
        }
        Matrix4d modelViewMatrix = camDirTypeModelViewMatrix.get(cameraDirectionType);
        if (modelViewMatrix == null) {
            return;
        }

        Map<CameraDirectionType, GaiaBoundingBox> camDirTypeBBox = mapClassificationCamDirTypeBBox.get(classifiedId);
        if (camDirTypeBBox == null) {
            return;
        }
        GaiaBoundingBox transformedTargetBbox = camDirTypeBBox.get(cameraDirectionType);
        if (transformedTargetBbox == null) {
            return;
        }

        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaPrimitive> primitives = extractor.extractAllPrimitives(scene);
        for(GaiaPrimitive primitive : primitives){
            List<GaiaVertex> vertices = primitive.getVertices();
            if(vertices == null || vertices.isEmpty()) {
                continue;
            }
            List<GaiaSurface> surfaces = primitive.getSurfaces();
            if(surfaces == null || surfaces.isEmpty()) {
                continue;
            }
            for(GaiaSurface surface : surfaces){
                List<GaiaFace> faces = surface.getFaces();
                if(faces == null || faces.isEmpty()) {
                    continue;
                }
                for (GaiaFace face : faces) {
                    List<Vector3d> faceInnerPoints = getFaceInnerVisibilityPoints(face, vertices);

                    int expectedColorCode = face.getId(); // o encodeFaceIdToColorCode(face.getId())
                    int visiblePointsCount = 0;

                    for (Vector3d point : faceInnerPoints) {
                        Vector4d transformed = new Vector4d(point, 1.0).mul(modelViewMatrix);

                        Vector2i pixel = transformedPointToPixel(
                                transformed,
                                transformedTargetBbox,
                                fboWidth,
                                fboHeight
                        );

                        if (pixel == null) {
                            continue;
                        }

                        boolean pointVisible = hasExpectedColorNearPixel(
                                pixels,
                                fboWidth,
                                fboHeight,
                                pixel.x,
                                pixel.y,
                                expectedColorCode,
                                1
                        );

                        if (pointVisible) {
                            visiblePointsCount++;
                        }
                    }

                    if (visiblePointsCount >= 3) {
                        // Esta cámara es válida para esta face.
                        List<CameraDirectionCandidate> candidates = mapFaceToCamCandidates.computeIfAbsent(face, k -> new ArrayList<>());
                        candidates.add(new CameraDirectionCandidate(cameraDirectionType, visiblePointsCount));
                    }
                }
            }
        }

        // delete pixels
        pixels.clear();
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

    private int getRgbaColorCodeAt(ByteBuffer pixels, int width, int height, int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
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
            int height
    ) {
        double minX = bbox.getMinX();
        double maxX = bbox.getMaxX();
        double minY = bbox.getMinY();
        double maxY = bbox.getMaxY();

        double sizeX = maxX - minX;
        double sizeY = maxY - minY;

        if (sizeX <= 1e-12 || sizeY <= 1e-12) {
            return null;
        }

        double u = (transformed.x - minX) / sizeX;
        double v = (transformed.y - minY) / sizeY;

        int x = (int) Math.round(u * (width - 1));

        // Normalmente hay que invertir Y para pasar de coordenada tipo imagen a buffer OpenGL.
        int y = (int) Math.round((1.0 - v) * (height - 1));

        if (x < 0 || y < 0 || x >= width || y >= height) {
            return null;
        }

        return new Vector2i(x, y);
    }
}
