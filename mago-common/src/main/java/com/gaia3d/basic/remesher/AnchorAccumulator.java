package com.gaia3d.basic.remesher;

import org.joml.Vector3d;

public class AnchorAccumulator {

    private double sumX;
    private double sumY;
    private double sumZ;
    private int count;

    public void add(Vector3d point) {
        sumX += point.x;
        sumY += point.y;
        sumZ += point.z;
        count++;
    }

    public int getCount() {
        return count;
    }

    public Vector3d getAverage() {
        if (count == 0) {
            return null;
        }

        double inverseCount =
                1.0 / count;

        return new Vector3d(
                sumX * inverseCount,
                sumY * inverseCount,
                sumZ * inverseCount
        );
    }
}
