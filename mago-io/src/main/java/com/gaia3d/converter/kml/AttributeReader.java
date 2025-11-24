package com.gaia3d.converter.kml;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public interface AttributeReader {
    TileTransformInfo read(File file);

    List<TileTransformInfo> readAll(File file);

    default Geometry transformGeometry(Geometry polygon, CoordinateReferenceSystem sourceCRS) throws FactoryException, TransformException {
        // 3857 is the default CRS for GeoJSON, which is WGS 84
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:3857", true);
        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
        return JTS.transform(polygon, transform);
    }

    default int calculatePointCount(Geometry polygon, CoordinateReferenceSystem sourceCRS, double proportion, double diameter) throws FactoryException, TransformException {
        Geometry transformedPolygon = transformGeometry(polygon, sourceCRS);
        double area = transformedPolygon.getArea();
        // convert proportion to a fraction of the area
        double forestArea = area * proportion;
        double treeDensity = diameter * diameter;

        double count = forestArea / treeDensity;
        return (int) count;
    }

    default List<Point> getRandomPointsWithDensity(Geometry polygon, int count) {
        return getRandomContainsPoints(polygon, polygon.getFactory(), count);
    }

    default List<Point> getRandomPointsWithDensity(Geometry polygon, double proportion, double diameter) {
        if (proportion <= 0) {
            return new ArrayList<>();
        }
        double area = polygon.getArea();
        // convert proportion to a fraction of the area

        double forestArea = area * proportion;
        double treeDensity = diameter * diameter;

        double count = forestArea / treeDensity;

        int castCount = (int) count;
        return getRandomContainsPoints(polygon, polygon.getFactory(), castCount);
    }

    default List<Point> getRandomContainsPoints(Geometry polygon, GeometryFactory geometryFactory, int count) {
        PreparedGeometry preparedGeometry = PreparedGeometryFactory.prepare(polygon);
        Envelope envelope = polygon.getEnvelopeInternal();

        if (count < 0) {
            double area = polygon.getArea();
            area *= 0.025;
            count = (int) area;
            if (count < 1) {
                count = 1;
            }
        }
        Random random = new Random(2620);

        List<Point> randomPoints = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Point randomPoint;
            do {
                double x = envelope.getMinX() + (envelope.getWidth() * random.nextDouble());
                double y = envelope.getMinY() + (envelope.getHeight() * random.nextDouble());
                randomPoint = geometryFactory.createPoint(new Coordinate(x, y));
            } while (!preparedGeometry.contains(randomPoint));

            randomPoints.add(randomPoint);
        }
        return randomPoints;
    }

    default List<Point> getRandomContainsPoints2(Geometry polygon, GeometryFactory geometryFactory, int count) {
        // contains more fast
        PreparedGeometry preparedGeometry = PreparedGeometryFactory.prepare(polygon);
        Envelope envelope = polygon.getEnvelopeInternal();

        if (count <= -1) {
            double area = envelope.getArea();
            area *= 0.05;
            count = (int) area;
            if (count < 1) {
                count = 1;
            }
            //count *= 10;
        }
        Random random = new Random(2620);

        List<Point> randomPoints = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double x = envelope.getMinX() + (envelope.getWidth() * random.nextDouble());
            double y = envelope.getMinY() + (envelope.getHeight() * random.nextDouble());
            Point randomPoint = geometryFactory.createPoint(new Coordinate(x, y));
            randomPoints.add(randomPoint);
        }

        randomPoints = randomPoints.stream()
                .filter(preparedGeometry::contains)
                .toList();

        return randomPoints;
    }
}
