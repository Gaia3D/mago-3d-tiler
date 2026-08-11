package com.gaia3d.terrain;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("default")
class GeoTiffTerrainHeightProviderTest {

    @Test
    void rejectsDeclaredNoDataAndNonFiniteValues() {
        double[] noDataValues = {-9999.0};

        assertFalse(GeoTiffTerrainHeightProvider.isValidTerrainHeight(-9999.0, noDataValues));
        assertFalse(GeoTiffTerrainHeightProvider.isValidTerrainHeight(Double.NaN, noDataValues));
        assertFalse(GeoTiffTerrainHeightProvider.isValidTerrainHeight(Double.POSITIVE_INFINITY, noDataValues));
    }

    @Test
    void rejectsExtremeSentinelValuesWithoutNoDataMetadata() {
        assertFalse(GeoTiffTerrainHeightProvider.isValidTerrainHeight(-3.017897893241e38, new double[0]));
        assertFalse(GeoTiffTerrainHeightProvider.isValidTerrainHeight(3.017897893241e38, new double[0]));
    }

    @Test
    void acceptsRealisticTerrainHeights() {
        assertTrue(GeoTiffTerrainHeightProvider.isValidTerrainHeight(-430.5, new double[0]));
        assertTrue(GeoTiffTerrainHeightProvider.isValidTerrainHeight(8848.86, new double[0]));
    }
}
