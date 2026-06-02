package com.gaia3d.renderer.engine.dataStructure;

import com.gaia3d.basic.halfedge.CameraDirectionType;
import com.gaia3d.basic.model.GaiaFace;
import com.gaia3d.basic.model.GaiaPrimitive;
import com.gaia3d.basic.model.GaiaSurface;
import org.joml.Vector3d;

import java.util.*;

public class FaceVisibilityDataManager {
    private final Map<CameraDirectionType, FaceVisibilityData> faceVisibilityDataMap;

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

    public FaceVisibilityDataManager() {
        faceVisibilityDataMap = new java.util.HashMap<>();
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

        // 3rd pass
        absorbFacesByNeighborMajority(
                allFaces,
                mapFaceToNeighbors,
                mapFaceToCamCandidates,
                result,
                0.20,
                1
        );

        absorbSmallCameraComponents(
                allFaces,
                mapFaceToNeighbors,
                mapFaceToCamCandidates,
                result,
                0.20,
                16
        );

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
}
