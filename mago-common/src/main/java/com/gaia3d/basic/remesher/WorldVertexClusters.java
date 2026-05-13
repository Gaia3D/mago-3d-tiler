package com.gaia3d.basic.remesher;

import com.gaia3d.basic.model.GaiaVertex;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldVertexClusters {

    public final Map<Vector3i, List<GaiaVertex>> interiorClusters = new HashMap<>();
    public final Map<Vector3i, List<GaiaVertex>> frontierClusters = new HashMap<>();

    public final Map<Vector3i, Vector3d> interiorAveragePositions = new HashMap<>();
    public final Map<Vector3i, Vector3d> frontierAveragePositions = new HashMap<>();

    public Vector3d getInteriorAverage(Vector3i cellIndex) {
        if (cellIndex == null) {
            return null;
        }

        return interiorAveragePositions.get(cellIndex);
    }

    public Vector3d getFrontierAverage(Vector3i cellIndex) {
        if (cellIndex == null) {
            return null;
        }

        return frontierAveragePositions.get(cellIndex);
    }

    public void clearInteriorClusters() {
        interiorClusters.clear();
        interiorAveragePositions.clear();
    }

    public void clearFrontierClusters() {
        frontierClusters.clear();
        frontierAveragePositions.clear();
    }

    public void clearAll() {
        clearInteriorClusters();
        clearFrontierClusters();
    }

    public void recalculateAveragePositions() {
        interiorAveragePositions.clear();
        frontierAveragePositions.clear();

        calculateAveragePositions(interiorClusters, interiorAveragePositions);
        calculateAveragePositions(frontierClusters, frontierAveragePositions);
    }

    private static void calculateAveragePositions(
            Map<Vector3i, List<GaiaVertex>> clusters,
            Map<Vector3i, Vector3d> result) {

        if (clusters == null || result == null) {
            return;
        }

        for (Map.Entry<Vector3i, List<GaiaVertex>> entry : clusters.entrySet()) {
            Vector3i cellIndex = entry.getKey();
            List<GaiaVertex> cluster = entry.getValue();

            if (cellIndex == null || cluster == null || cluster.isEmpty()) {
                continue;
            }

            Vector3d average = new Vector3d();
            int count = 0;

            for (GaiaVertex vertex : cluster) {
                if (vertex == null || vertex.getPosition() == null) {
                    continue;
                }

                average.add(vertex.getPosition());
                count++;
            }

            if (count == 0) {
                continue;
            }

            average.div(count);

            result.put(new Vector3i(cellIndex), average);
        }
    }
}
