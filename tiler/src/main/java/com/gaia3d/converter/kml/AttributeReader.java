package com.gaia3d.converter.kml;

import com.gaia3d.command.mago.GlobalOptions;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public interface AttributeReader {
    KmlInfo read(File file);
    List<KmlInfo> readAll(File file);

    default List<Point> getRandomContainsPoints(Geometry polygon, GeometryFactory geometryFactory, int count) {
        PreparedGeometry preparedGeometry = PreparedGeometryFactory.prepare(polygon);
        Envelope envelope = polygon.getEnvelopeInternal();

        if (count <= -1) {
            double area = polygon.getArea();
            area *= 0.025;
            count = (int) area;
            if (count < 1) {
                count = 1;
            }
        }
        Random random = new Random(/*GlobalOptions.RANDOM_SEED*/);

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
        Random random = new Random(GlobalOptions.RANDOM_SEED);

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
