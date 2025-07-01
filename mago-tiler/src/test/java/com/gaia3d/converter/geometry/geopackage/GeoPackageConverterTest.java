package com.gaia3d.converter.geometry.geopackage;

import com.gaia3d.command.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;

import java.util.List;

@Slf4j
class GeoPackageConverterTest {

    @Test
    void getRandomContainsPoints() {
        Configuration.initConsoleLogger();

        Envelope envelope = new Envelope(126.977491, 127.0, 37.659025, 37.7);
        double area = envelope.getArea();

        Coordinate[] coordinates = new Coordinate[5];
        coordinates[0] = new Coordinate(126.977491, 37.659025);
        coordinates[1] = new Coordinate(127.0, 37.659025);
        coordinates[2] = new Coordinate(127.0, 37.7);
        coordinates[3] = new Coordinate(126.977491, 37.7);
        coordinates[4] = new Coordinate(126.977491, 37.659025);

        GeometryFactory geometryFactory = new GeometryFactory();
        Polygon polygon = geometryFactory.createPolygon(coordinates);

        GeoPackageInstanceConverter geoPackageConverter = new GeoPackageInstanceConverter();

        log.info("area: {}", area);
        List<Point> points = geoPackageConverter.getRandomContainsPoints(polygon, geometryFactory, 1000);
        int count = points.size();
        for (Point point : points) {
            log.info("[{}] point: {}", count, point);
        }

        //log.info("points: {}", points);
    }
}