package com.gaia3d.basic.remesher;

import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;

public class GlobalBoundaryAnchors {
    public final Map<Vector3i, Vector3d> lockedAveragePositions = new HashMap<>();

    public Vector3d getAverage(Vector3i cellIndex) {
        if (cellIndex == null) {return null;}
        return lockedAveragePositions.get(cellIndex);
    }

    public boolean hasAverage(Vector3i cellIndex) {
        if (cellIndex == null) {return false;}
        return lockedAveragePositions.containsKey(cellIndex);
    }

    public boolean putIfAbsent(Vector3i cellIndex, Vector3d average) {
        if (cellIndex == null || average == null) {
            return false;
        }

        lockedAveragePositions.putIfAbsent(
                new Vector3i(cellIndex),
                new Vector3d(average)
        );
        return false;
    }

    public int size() {
        return lockedAveragePositions.size();
    }

    public boolean isEmpty() {
        return lockedAveragePositions.isEmpty();
    }

    public void clear() {
        lockedAveragePositions.clear();
    }

    /*
     * Solo debe llamarse durante la construcción.
     * Después, GlobalBoundaryAnchors será de solo lectura.
     */
    void putLockedAverage(
            Vector3i cellIndex,
            Vector3d average
    ) {
        if (cellIndex == null || average == null) {
            return;
        }

        lockedAveragePositions.put(
                new Vector3i(cellIndex),
                new Vector3d(average)
        );
    }
}
