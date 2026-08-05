package com.gaia3d.process.tileprocess.multithread;

import org.joml.Vector3d;

public class PositionAccumulator {
    private final Vector3d sum =
            new Vector3d();

    private int count;

    void add(
            Vector3d position
    ) {
        if (position == null) {
            return;
        }

        sum.add(position);
        count++;
    }

    Vector3d calculateAverage() {
        if (count == 0) {
            return null;
        }

        return new Vector3d(sum)
                .div(count);
    }
}
