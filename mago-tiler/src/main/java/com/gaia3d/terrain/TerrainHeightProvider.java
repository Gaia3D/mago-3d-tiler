package com.gaia3d.terrain;

import java.util.OptionalDouble;

@FunctionalInterface
public interface TerrainHeightProvider {
    OptionalDouble sample(double longitude, double latitude);

    static TerrainHeightProvider empty() {
        return (longitude, latitude) -> OptionalDouble.empty();
    }
}
