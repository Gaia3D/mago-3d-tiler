package com.gaia3d.terrain;

import lombok.extern.slf4j.Slf4j;
import org.geotools.api.geometry.Position;
import org.geotools.api.metadata.spatial.PixelOrientation;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

@Slf4j
public class GeoTiffTerrainHeightProvider implements TerrainHeightProvider {
    private static final double MAXIMUM_ABSOLUTE_TERRAIN_HEIGHT = 100_000.0;
    private final List<CoverageSampler> samplers;

    public GeoTiffTerrainHeightProvider(List<GridCoverage2D> coverages) {
        this.samplers = new ArrayList<>();
        if (coverages == null) {
            return;
        }
        for (GridCoverage2D coverage : coverages) {
            try {
                MathTransform fromWgs84 = CRS.findMathTransform(
                        DefaultGeographicCRS.WGS84,
                        coverage.getCoordinateReferenceSystem(),
                        true);
                MathTransform worldToGridCenter = coverage.getGridGeometry()
                        .getGridToCRS2D(PixelOrientation.CENTER)
                        .inverse();
                samplers.add(new CoverageSampler(
                        coverage,
                        fromWgs84,
                        worldToGridCenter,
                        getNoDataValues(coverage)));
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to initialize GeoTIFF terrain sampler", e);
            }
        }
    }

    private double[] getNoDataValues(GridCoverage2D coverage) {
        try {
            double[] noDataValues = coverage.getSampleDimension(0).getNoDataValues();
            return noDataValues == null ? new double[0] : noDataValues.clone();
        } catch (IllegalStateException e) {
            log.debug("GeoTIFF terrain does not provide readable NoData metadata");
            return new double[0];
        }
    }

    static boolean isValidTerrainHeight(double height, double[] noDataValues) {
        if (!Double.isFinite(height) || Math.abs(height) > MAXIMUM_ABSOLUTE_TERRAIN_HEIGHT) {
            return false;
        }
        for (double noDataValue : noDataValues) {
            if ((Double.isNaN(noDataValue) && Double.isNaN(height))
                    || Double.compare(height, noDataValue) == 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public OptionalDouble sample(double longitude, double latitude) {
        if (samplers.isEmpty()) {
            return OptionalDouble.empty();
        }

        Position2D wgs84Position = new Position2D(DefaultGeographicCRS.WGS84, longitude, latitude);
        OptionalDouble result = OptionalDouble.empty();
        for (CoverageSampler sampler : samplers) {
            try {
                OptionalDouble height = sampler.sample(wgs84Position);
                if (height.isPresent()) {
                    result = height;
                }
            } catch (Exception e) {
                log.debug("Failed to sample GeoTIFF terrain at {}, {}", longitude, latitude);
            }
        }
        return result;
    }

    private record CoverageSampler(
            GridCoverage2D coverage,
            MathTransform fromWgs84,
            MathTransform worldToGridCenter,
            double[] noDataValues) {

        private static int clamp(int value, int minimum, int maximum) {
            return Math.max(minimum, Math.min(maximum, value));
        }

        OptionalDouble sample(Position2D wgs84Position) throws Exception {
            Position2D coveragePosition = new Position2D(coverage.getCoordinateReferenceSystem());
            fromWgs84.transform(wgs84Position, coveragePosition);
            Position gridPosition = worldToGridCenter.transform(coveragePosition, null);
            double gridX = gridPosition.getOrdinate(0);
            double gridY = gridPosition.getOrdinate(1);

            RenderedImage image = coverage.getRenderedImage();
            int minimumX = image.getMinX();
            int minimumY = image.getMinY();
            int maximumX = minimumX + image.getWidth() - 1;
            int maximumY = minimumY + image.getHeight() - 1;
            if (gridX < minimumX - 0.5 || gridX > maximumX + 0.5
                    || gridY < minimumY - 0.5 || gridY > maximumY + 0.5) {
                return OptionalDouble.empty();
            }

            int rawX0 = (int) Math.floor(gridX);
            int rawY0 = (int) Math.floor(gridY);
            int x0 = clamp(rawX0, minimumX, maximumX);
            int y0 = clamp(rawY0, minimumY, maximumY);
            int x1 = Math.min(x0 + 1, maximumX);
            int y1 = Math.min(y0 + 1, maximumY);
            double fractionX = rawX0 < minimumX ? 0.0 : gridX - rawX0;
            double fractionY = rawY0 < minimumY ? 0.0 : gridY - rawY0;

            Raster raster = image.getData(new Rectangle(x0, y0, x1 - x0 + 1, y1 - y0 + 1));
            double topLeft = raster.getSampleDouble(x0, y0, 0);
            double topRight = raster.getSampleDouble(x1, y0, 0);
            double bottomLeft = raster.getSampleDouble(x0, y1, 0);
            double bottomRight = raster.getSampleDouble(x1, y1, 0);
            if (!isValidTerrainHeight(topLeft, noDataValues)
                    || !isValidTerrainHeight(topRight, noDataValues)
                    || !isValidTerrainHeight(bottomLeft, noDataValues)
                    || !isValidTerrainHeight(bottomRight, noDataValues)) {
                return OptionalDouble.empty();
            }

            double top = topLeft * (1.0 - fractionX) + topRight * fractionX;
            double bottom = bottomLeft * (1.0 - fractionX) + bottomRight * fractionX;
            double height = top * (1.0 - fractionY) + bottom * fractionY;
            return isValidTerrainHeight(height, noDataValues)
                    ? OptionalDouble.of(height)
                    : OptionalDouble.empty();
        }
    }
}
