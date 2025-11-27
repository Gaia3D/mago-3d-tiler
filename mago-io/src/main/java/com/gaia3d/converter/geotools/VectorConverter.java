package com.gaia3d.converter.geotools;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@NoArgsConstructor
public class VectorConverter {
    private boolean isInstance = false;

    public void convert(List<Geometry> geometries) {
        VectorCollection collection = filter(geometries);
    }

    private VectorCollection filter(List<Geometry> geometries) {
        List<Point> points = new ArrayList<>();
        List<MultiPoint> multiPoints = new ArrayList<>();
        List<Polygon> polygons = new ArrayList<>();
        List<MultiPolygon> multiPolygons = new ArrayList<>();
        List<LineString> lineStrings = new ArrayList<>();
        List<MultiLineString> multiLineStrings = new ArrayList<>();
        List<GeometryCollection> geometryCollections = new ArrayList<>();

        for (Geometry geometry : geometries) {
            if (geometry instanceof Point) {
                points.add((Point) geometry);
            } else if (geometry instanceof MultiPoint) {
                multiPoints.add((MultiPoint) geometry);
            } else if (geometry instanceof Polygon) {
                polygons.add((Polygon) geometry);
            } else if (geometry instanceof MultiPolygon) {
                multiPolygons.add((MultiPolygon) geometry);
            } else if (geometry instanceof LineString) {
                lineStrings.add((LineString) geometry);
            } else if (geometry instanceof MultiLineString) {
                multiLineStrings.add((MultiLineString) geometry);
            } else {
                throw new IllegalArgumentException("Unsupported geometry type: " + geometry.getGeometryType());
            }
        }

        log.info("================== Geometry Filter Result =================");
        log.info("Points: {}", points.size());
        log.info("MultiPoints: {}", multiPoints.size());
        log.info("Polygons: {}", polygons.size());
        log.info("MultiPolygons: {}", multiPolygons.size());
        log.info("LineStrings: {}", lineStrings.size());
        log.info("MultiLineStrings: {}", multiLineStrings.size());
        log.info("==========================================================");

        VectorCollection vectorCollection = new VectorCollection();
        List<Point> pointList = vectorCollection.getPoints();
        pointList.addAll(points);
        pointList.addAll(extractPoints(multiPoints));

        List<LineString> lineStringList = vectorCollection.getLineStrings();
        lineStringList.addAll(lineStrings);
        lineStringList.addAll(extractLineStrings(multiLineStrings));

        List<Polygon> polygonList = vectorCollection.getPolygons();
        polygonList.addAll(polygons);
        polygonList.addAll(extractPolygons(multiPolygons));

        return vectorCollection;
    }

    private List<Polygon> extractPolygons(List<MultiPolygon> multiPolygons) {
        List<Polygon> polygons = new ArrayList<>();
        for (MultiPolygon multiPolygon : multiPolygons) {
            for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                Geometry geometry = multiPolygon.getGeometryN(i);
                if (geometry instanceof Polygon) {
                    polygons.add((Polygon) geometry);
                }
            }
        }
        return polygons;
    }

    private List<LineString> extractLineStrings(List<MultiLineString> multiLineStrings) {
        List<LineString> lineStrings = new ArrayList<>();
        for (MultiLineString multiLineString : multiLineStrings) {
            for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
                Geometry geometry = multiLineString.getGeometryN(i);
                if (geometry instanceof LineString) {
                    lineStrings.add((LineString) geometry);
                }
            }
        }
        return lineStrings;
    }

    private List<Point> extractPoints(List<MultiPoint> multiPoints) {
        List<Point> points = new ArrayList<>();
        for (MultiPoint multiPoint : multiPoints) {
            for (int i = 0; i < multiPoint.getNumGeometries(); i++) {
                Geometry geometry = multiPoint.getGeometryN(i);
                if (geometry instanceof Point) {
                    points.add((Point) geometry);
                }
            }
        }
        return points;
    }

    private List<Point> fillPoints(Polygon polygon) {
        // TODO
        List<Point> points = new ArrayList<>();
        Coordinate[] coordinates = polygon.getCoordinates();
        for (Coordinate coordinate : coordinates) {
            Point point = polygon.getFactory().createPoint(coordinate);
            points.add(point);
        }
        return points;
    }

    private List<Point> fillPoints(LineString lineString) {
        // TODO
        List<Point> points = new ArrayList<>();
        Coordinate[] coordinates = lineString.getCoordinates();
        for (Coordinate coordinate : coordinates) {
            Point point = lineString.getFactory().createPoint(coordinate);
            points.add(point);
        }
        return points;
    }
}
