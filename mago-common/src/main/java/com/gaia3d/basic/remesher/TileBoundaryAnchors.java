package com.gaia3d.basic.remesher;

import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.*;

public class TileBoundaryAnchors {
    public final Map<Vector3i, List<Vector3d>> frontierPositionClusters = new HashMap<>();
    public final Map<Vector3i, Vector3d> frontierAveragePositions = new HashMap<>();

    // Opcional: sirve para detectar celdas donde contribuyen varios meshes.
    // Muy útil para diagnosticar esquinas tipo A/B/C/D.
    public final Map<Vector3i, Set<Integer>> cellToMeshIds = new HashMap<>();

    public void clear() {
        frontierPositionClusters.clear();
        frontierAveragePositions.clear();
        cellToMeshIds.clear();
    }

    public void addFrontierPosition(Vector3i cellIndex, Vector3d position, int meshId) {
        if (cellIndex == null || position == null) {
            return;
        }

        Vector3i key = new Vector3i(cellIndex);

        frontierPositionClusters
                .computeIfAbsent(new Vector3i(key), k -> new ArrayList<>())
                .add(new Vector3d(position));

        cellToMeshIds
                .computeIfAbsent(new Vector3i(key), k -> new HashSet<>())
                .add(meshId);
    }

    public void calculateAveragePositions() {
        frontierAveragePositions.clear();

        for (Map.Entry<Vector3i, List<Vector3d>> entry : frontierPositionClusters.entrySet()) {
            Vector3i cellIndex = entry.getKey();
            List<Vector3d> positions = entry.getValue();

            if (cellIndex == null || positions == null || positions.isEmpty()) {
                continue;
            }

            Vector3d avg = new Vector3d();
            int count = 0;

            for (Vector3d p : positions) {
                if (p == null) {
                    continue;
                }

                avg.add(p);
                count++;
            }

            if (count == 0) {
                continue;
            }

            avg.div(count);

            frontierAveragePositions.put(new Vector3i(cellIndex), avg);
        }
    }

    public Vector3d getFrontierAverage(Vector3i cellIndex) {
        if (cellIndex == null) {
            return null;
        }

        return frontierAveragePositions.get(cellIndex);
    }

    public int getContributingMeshCount(Vector3i cellIndex) {
        if (cellIndex == null) {
            return 0;
        }

        Set<Integer> meshIds = cellToMeshIds.get(cellIndex);
        return meshIds == null ? 0 : meshIds.size();
    }

    public int countCornerLikeCells() {
        int count = 0;

        for (Set<Integer> meshIds : cellToMeshIds.values()) {
            if (meshIds != null && meshIds.size() >= 3) {
                count++;
            }
        }

        return count;
    }
}
