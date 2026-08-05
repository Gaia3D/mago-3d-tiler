package com.gaia3d.process.tileprocess.multithread;

import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;

public class FrontierAccumulator {
    private final Map<Integer, PositionAccumulator>
            positionsByTile =
            new HashMap<>();

    private void add(
            Vector3d position,
            int tileId
    ) {
        if (position == null) {
            return;
        }

        positionsByTile
                .computeIfAbsent(
                        tileId,
                        ignored -> new PositionAccumulator()
                )
                .add(position);
    }

    private boolean isShared() {
        return positionsByTile.size() >= 2;
    }

    private int getTileCount() {
        return positionsByTile.size();
    }

    private Vector3d calculateAverage() {
        if (!isShared()) {
            return null;
        }

        Vector3d result =
                new Vector3d();

        int validTileCount = 0;

        for (PositionAccumulator accumulator
                : positionsByTile.values()) {

            Vector3d tileAverage =
                    accumulator.calculateAverage();

            if (tileAverage == null) {
                continue;
            }

            result.add(tileAverage);
            validTileCount++;
        }

        if (validTileCount == 0) {
            return null;
        }

        return result.div(validTileCount);
    }
}
