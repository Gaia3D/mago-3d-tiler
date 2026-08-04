package com.gaia3d.terrain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.geometry.Position;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import java.util.List;
import java.util.OptionalDouble;

@Slf4j
@RequiredArgsConstructor
public class GeoTiffTerrainHeightProvider implements TerrainHeightProvider {
    private final List<GridCoverage2D> coverages;

    @Override
    public OptionalDouble sample(double longitude, double latitude) {
        if (coverages == null || coverages.isEmpty()) {
            return OptionalDouble.empty();
        }

        Position position = new Position2D(DefaultGeographicCRS.WGS84, longitude, latitude);
        OptionalDouble result = OptionalDouble.empty();
        for (GridCoverage2D coverage : coverages) {
            double[] height = new double[1];
            try {
                coverage.evaluate(position, height);
                if (Double.isFinite(height[0])) {
                    result = OptionalDouble.of(height[0]);
                }
            } catch (RuntimeException e) {
                log.debug("Failed to sample GeoTIFF terrain at {}, {}", longitude, latitude);
            }
        }
        return result;
    }
}
