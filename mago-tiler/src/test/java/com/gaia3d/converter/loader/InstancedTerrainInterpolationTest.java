package com.gaia3d.converter.loader;

import com.gaia3d.util.GlobeUtils;
import com.gaia3d.terrain.GeoTiffTerrainHeightProvider;
import org.eclipse.imagen.Interpolation;
import org.geotools.api.geometry.Bounds;
import org.geotools.api.geometry.Position;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.metadata.spatial.PixelOrientation;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.Interpolator2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.joml.Vector3d;

import java.io.File;
import java.awt.image.Raster;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("default")
class InstancedTerrainInterpolationTest {
    private static final double ELEVATION_TOLERANCE = 1.0e-9;
    private static final double[][] SAMPLE_RATIOS = {
            {0.23, 0.31},
            {0.47, 0.52},
            {0.71, 0.68}
    };
    private static final double[][] MEASURED_POSITIONS = {
            // longitude, latitude, i3dm bottom height, Cesium terrain height
            {127.894604, 37.747068, 329.93, 328.63},
            {127.89467716, 37.74684435, 340.70, 339.5213},
            {127.895213, 37.746152, 348.92, 347.42}
    };

    @Test
    void repeatedBilinearWrappingDoesNotInterpolateElevationTwice() throws Exception {
        File terrainFile = terrainFile();
        Interpolation bilinear = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);

        GeoTiffReader reader = new GeoTiffReader(terrainFile);
        try {
            GridCoverage2D source = reader.read((GeneralParameterValue[]) null);
            GridCoverage2D once = (GridCoverage2D) Operations.DEFAULT.interpolate(source, bilinear);
            GridCoverage2D wrappedAgain = Interpolator2D.create(once, bilinear);

            for (Position2D position : samplePositionsInWgs84(source)) {
                assertEquals(
                        evaluate(once, position),
                        evaluate(wrappedAgain, position),
                        ELEVATION_TOLERANCE,
                        () -> "Repeated bilinear wrapping changed elevation at " + position);
            }
        } finally {
            reader.dispose();
        }
    }

    @Test
    void geotoolsBilinearMatchesInterpolationCalculatedFromFourPixels() throws Exception {
        File terrainFile = terrainFile();
        Interpolation bilinear = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        GeoTiffReader reader = new GeoTiffReader(terrainFile);

        try {
            GridCoverage2D source = reader.read((GeneralParameterValue[]) null);
            GridCoverage2D interpolated = (GridCoverage2D) Operations.DEFAULT.interpolate(source, bilinear);

            for (Position2D position : samplePositionsInWgs84(source)) {
                double expected = calculateBilinearFromSourcePixels(source, position);
                double actual = evaluate(interpolated, position);
                assertEquals(
                        expected,
                        actual,
                        1.0e-5,
                        () -> "GeoTools bilinear differs from the direct 2x2 calculation at " + position);
            }
        } finally {
            reader.dispose();
        }
    }

    @Test
    void bilinearElevationDiffersFromNearestNeighborOnSampleTerrain() throws Exception {
        File terrainFile = terrainFile();
        Interpolation bilinear = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        GeoTiffReader reader = new GeoTiffReader(terrainFile);

        try {
            GridCoverage2D nearest = reader.read((GeneralParameterValue[]) null);
            GridCoverage2D interpolated = (GridCoverage2D) Operations.DEFAULT.interpolate(nearest, bilinear);
            double maximumDifference = 0.0;

            for (Position2D position : samplePositionsInWgs84(nearest)) {
                double nearestElevation = evaluate(nearest, position);
                double bilinearElevation = evaluate(interpolated, position);
                double difference = Math.abs(nearestElevation - bilinearElevation);
                maximumDifference = Math.max(maximumDifference, difference);

                System.out.printf(
                        "terrain interpolation at (%.9f, %.9f): nearest=%.9f, bilinear=%.9f, difference=%.9f m%n",
                        position.getX(),
                        position.getY(),
                        nearestElevation,
                        bilinearElevation,
                        difference);
            }

            assertTrue(
                    maximumDifference > 1.0e-6,
                    "The sample points do not demonstrate a difference between nearest and bilinear interpolation");
        } finally {
            reader.dispose();
        }
    }

    @Test
    void comparePositionPrecisionOfTiles10And11() throws Exception {
        File terrainFile = terrainFile();
        Interpolation bilinear = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        GeoTiffReader reader = new GeoTiffReader(terrainFile);

        try {
            GridCoverage2D source = reader.read((GeneralParameterValue[]) null);
            GridCoverage2D interpolated = (GridCoverage2D) Operations.DEFAULT.interpolate(source, bilinear);
            List<Vector3d> cartographicPositions = new ArrayList<>();

            for (Position2D position : samplePositionsInWgs84(source)) {
                cartographicPositions.add(new Vector3d(position.getX(), position.getY(), evaluate(interpolated, position)));
            }

            Vector3d tileCenter = boundingBoxCenter(cartographicPositions);
            Vector3d centerEcef = GlobeUtils.geographicToCartesianWgs84(tileCenter);
            double maximumTiles10HeightError = 0.0;
            double maximumTiles11HeightError = 0.0;

            for (Vector3d position : cartographicPositions) {
                Vector3d expectedEcef = GlobeUtils.geographicToCartesianWgs84(position);
                Vector3d ecefDelta = new Vector3d(expectedEcef).sub(centerEcef);

                Vector3d reconstructed10 = new Vector3d(
                        centerEcef.x + (float) ecefDelta.x,
                        centerEcef.y + (float) ecefDelta.y,
                        centerEcef.z + (float) ecefDelta.z);

                Vector3d reconstructed11 = new Vector3d(
                        (float) centerEcef.x + (float) ecefDelta.x,
                        -((float) -centerEcef.y + (float) -ecefDelta.y),
                        (float) centerEcef.z + (float) ecefDelta.z);

                double tiles10Height = GlobeUtils.cartesianToGeographicWgs84(reconstructed10).z;
                double tiles11Height = GlobeUtils.cartesianToGeographicWgs84(reconstructed11).z;
                double tiles10Error = Math.abs(tiles10Height - position.z);
                double tiles11Error = Math.abs(tiles11Height - position.z);
                maximumTiles10HeightError = Math.max(maximumTiles10HeightError, tiles10Error);
                maximumTiles11HeightError = Math.max(maximumTiles11HeightError, tiles11Error);

                System.out.printf(
                        "tile position precision at (%.9f, %.9f): source=%.9f, tiles10Error=%.9f m, tiles11Error=%.9f m%n",
                        position.x,
                        position.y,
                        position.z,
                        tiles10Error,
                        tiles11Error);
            }

            assertTrue(maximumTiles10HeightError < 1.0e-3, "3D Tiles 1.0 position encoding lost more than 1 mm");
            assertTrue(maximumTiles11HeightError < 0.5, "3D Tiles 1.1 position encoding lost more than 0.5 m");
            assertTrue(
                    maximumTiles11HeightError > maximumTiles10HeightError,
                    "The test fixture did not expose the float root-translation precision loss in 3D Tiles 1.1");
        } finally {
            reader.dispose();
        }
    }

    @Test
    void compareMeasuredI3dmAndCesiumTerrainHeightsWithGeoTiff() throws Exception {
        File terrainFile = terrainFile();
        Interpolation bilinear = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        GeoTiffReader reader = new GeoTiffReader(terrainFile);

        try {
            GridCoverage2D source = reader.read((GeneralParameterValue[]) null);
            GridCoverage2D interpolated = (GridCoverage2D) Operations.DEFAULT.interpolate(source, bilinear);

            for (double[] measured : MEASURED_POSITIONS) {
                Position2D position = new Position2D(DefaultGeographicCRS.WGS84, measured[0], measured[1]);
                double i3dmHeight = measured[2];
                double cesiumTerrainHeight = measured[3];
                double nearestHeight = evaluate(source, position);
                double bilinearHeight = evaluate(interpolated, position);
                double directBilinearHeight = calculateBilinearFromSourcePixels(source, position);
                double i3dmDifference = Math.abs(bilinearHeight - i3dmHeight);
                double terrainDifference = Math.abs(bilinearHeight - cesiumTerrainHeight);

                assertEquals(directBilinearHeight, bilinearHeight, 1.0e-5);

                System.out.printf(
                        "measured point (%.6f, %.6f): nearest=%.9f, bilinear=%.9f, "
                                + "i3dm=%.2f (diff=%.9f), terrain=%.2f (diff=%.9f), closer=%s%n",
                        measured[0],
                        measured[1],
                        nearestHeight,
                        bilinearHeight,
                        i3dmHeight,
                        i3dmDifference,
                        cesiumTerrainHeight,
                        terrainDifference,
                        i3dmDifference < terrainDifference ? "i3dm" : "terrain");
            }
        } finally {
            reader.dispose();
        }
    }

    @Test
    void instancedLoaderSamplesBilinearFromPixelCenters() throws Exception {
        File terrainFile = terrainFile();
        InstancedFileLoader loader = new InstancedFileLoader(null, null, null);

        List<GridCoverage2D> loadedCoverages = loader.loadGridCoverages(terrainFile, new ArrayList<>());
        assertEquals(1, loadedCoverages.size());
        GeoTiffTerrainHeightProvider provider = new GeoTiffTerrainHeightProvider(loadedCoverages);

        GeoTiffReader reader = new GeoTiffReader(terrainFile);
        try {
            GridCoverage2D source = reader.read((GeneralParameterValue[]) null);

            for (Position2D position : samplePositionsInWgs84(source)) {
                assertEquals(
                        calculateBilinearFromSourcePixels(source, position, PixelOrientation.CENTER),
                        provider.sample(position.getX(), position.getY()).orElseThrow(),
                        1.0e-9,
                        () -> "Terrain provider did not use pixel-center bilinear sampling at " + position);
            }

            assertEquals(
                    339.547453654,
                    provider.sample(127.89467716, 37.74684435).orElseThrow(),
                    1.0e-6);
        } finally {
            reader.dispose();
        }
    }

    private List<Position2D> samplePositionsInWgs84(GridCoverage2D coverage) throws Exception {
        Bounds bounds = coverage.getEnvelope();
        MathTransform toWgs84 = CRS.findMathTransform(
                coverage.getCoordinateReferenceSystem(),
                DefaultGeographicCRS.WGS84,
                true);
        List<Position2D> positions = new ArrayList<>();

        for (double[] ratio : SAMPLE_RATIOS) {
            double x = bounds.getMinimum(0) + bounds.getSpan(0) * ratio[0];
            double y = bounds.getMinimum(1) + bounds.getSpan(1) * ratio[1];
            Position2D sourcePosition = new Position2D(coverage.getCoordinateReferenceSystem(), x, y);
            Position2D wgs84Position = new Position2D(DefaultGeographicCRS.WGS84);
            toWgs84.transform(sourcePosition, wgs84Position);
            positions.add(wgs84Position);
        }
        return positions;
    }

    private double evaluate(GridCoverage2D coverage, Position2D position) {
        double[] elevation = new double[1];
        coverage.evaluate((Position) position, elevation);
        return elevation[0];
    }

    private double calculateBilinearFromSourcePixels(GridCoverage2D source, Position2D wgs84Position) throws Exception {
        return calculateBilinearFromSourcePixels(source, wgs84Position, PixelOrientation.UPPER_LEFT);
    }

    private double calculateBilinearFromSourcePixels(
            GridCoverage2D source,
            Position2D wgs84Position,
            PixelOrientation pixelOrientation) throws Exception {
        MathTransform fromWgs84 = CRS.findMathTransform(
                DefaultGeographicCRS.WGS84,
                source.getCoordinateReferenceSystem(),
                true);
        Position2D coveragePosition = new Position2D(source.getCoordinateReferenceSystem());
        fromWgs84.transform(wgs84Position, coveragePosition);

        MathTransform worldToGrid = source.getGridGeometry()
                .getGridToCRS2D(pixelOrientation)
                .inverse();
        Position gridPosition = worldToGrid.transform(coveragePosition, null);
        double gridX = gridPosition.getOrdinate(0);
        double gridY = gridPosition.getOrdinate(1);
        int x0 = (int) Math.floor(gridX);
        int y0 = (int) Math.floor(gridY);
        double dx = gridX - x0;
        double dy = gridY - y0;

        Raster raster = source.getRenderedImage().getData();
        double topLeft = raster.getSampleDouble(x0, y0, 0);
        double topRight = raster.getSampleDouble(x0 + 1, y0, 0);
        double bottomLeft = raster.getSampleDouble(x0, y0 + 1, 0);
        double bottomRight = raster.getSampleDouble(x0 + 1, y0 + 1, 0);

        double top = topLeft * (1.0 - dx) + topRight * dx;
        double bottom = bottomLeft * (1.0 - dx) + bottomRight * dx;
        return top * (1.0 - dy) + bottom * dy;
    }

    private Vector3d boundingBoxCenter(List<Vector3d> positions) {
        double minLongitude = positions.stream().mapToDouble(position -> position.x).min().orElseThrow();
        double maxLongitude = positions.stream().mapToDouble(position -> position.x).max().orElseThrow();
        double minLatitude = positions.stream().mapToDouble(position -> position.y).min().orElseThrow();
        double maxLatitude = positions.stream().mapToDouble(position -> position.y).max().orElseThrow();
        double minHeight = positions.stream().mapToDouble(position -> position.z).min().orElseThrow();
        double maxHeight = positions.stream().mapToDouble(position -> position.z).max().orElseThrow();
        return new Vector3d(
                (minLongitude + maxLongitude) * 0.5,
                (minLatitude + maxLatitude) * 0.5,
                (minHeight + maxHeight) * 0.5);
    }

    private File terrainFile() throws URISyntaxException {
        URL resource = getClass().getResource("/sample-terrain/garisan-precision.tif");
        assertNotNull(resource, "Terrain test resource is missing");
        return new File(resource.toURI());
    }
}
