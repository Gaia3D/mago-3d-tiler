package com.gaia3d.converter.kml;

import com.gaia3d.util.GlobeUtils;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.joml.Vector3d;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public interface AttributeReader {
    TileTransformInfo read(File file);

    List<TileTransformInfo> readAll(File file);

    default void readEach(File file, Consumer<TileTransformInfo> consumer) {
        List<TileTransformInfo> tileTransformInfos = readAll(file);
        if (tileTransformInfos == null) {
            return;
        }
        tileTransformInfos.forEach(consumer);
    }

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

    default Map<String, String> extractAttributes(SimpleFeature feature) {
        Map<String, String> attributes = new HashMap<>();
        FeatureType featureType = feature.getFeatureType();
        Collection<PropertyDescriptor> featureDescriptors = featureType.getDescriptors();
        AtomicInteger index = new AtomicInteger(0);
        featureDescriptors.forEach(attributeDescriptor -> {
            Object attribute = feature.getAttribute(index.getAndIncrement());
            if (attribute instanceof Geometry) {
                return;
            }
            String attributeString = castStringFromObject(attribute, "null");
            String attributeName = attributeDescriptor.getName().getLocalPart();
            if (!attributeName.isEmpty() && Character.isDigit(attributeName.charAt(0))) {
                attributeName = "_" + attributeName;
            }
            attributes.put(attributeName, attributeString);
        });
        return attributes;
    }

    default void emitTileTransformInfos(String name,
                                        Geometry geometry,
                                        CoordinateReferenceSystem sourceCrs,
                                        double density,
                                        double scale,
                                        double altitude,
                                        double heading,
                                        Map<String, String> attributes,
                                        org.locationtech.proj4j.CoordinateReferenceSystem targetCrs,
                                        Consumer<TileTransformInfo> consumer) {
        if (geometry instanceof MultiPolygon multiPolygon) {
            for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
                emitPolygonPoints(name, polygon, sourceCrs, density, scale, altitude, heading, attributes, targetCrs, consumer);
            }
        } else if (geometry instanceof Polygon polygon) {
            emitPolygonPoints(name, polygon, sourceCrs, density, scale, altitude, heading, attributes, targetCrs, consumer);
        } else if (geometry instanceof MultiPoint multiPoint) {
            GeometryFactory factory = multiPoint.getFactory();
            for (Coordinate coordinate : multiPoint.getCoordinates()) {
                emitPoint(name, factory.createPoint(coordinate), altitude, heading, scale, attributes, targetCrs, consumer);
            }
        } else if (geometry instanceof Point point) {
            emitPoint(name, point, altitude, heading, scale, attributes, targetCrs, consumer);
        } else {
            throw new IllegalArgumentException("Geometry type is not supported.");
        }
    }

    default void emitPolygonPoints(String name,
                                   Polygon polygon,
                                   CoordinateReferenceSystem sourceCrs,
                                   double density,
                                   double scale,
                                   double altitude,
                                   double heading,
                                   Map<String, String> attributes,
                                   org.locationtech.proj4j.CoordinateReferenceSystem targetCrs,
                                   Consumer<TileTransformInfo> consumer) {
        try {
            int pointCount = calculatePointCount(polygon, sourceCrs, density, scale);
            List<Point> points = getRandomPointsWithDensity(polygon, pointCount);
            for (Point point : points) {
                emitPoint(name, point, altitude, heading, scale, attributes, targetCrs, consumer);
            }
        } catch (FactoryException | TransformException e) {
            throw new RuntimeException("Error transforming geometry.", e);
        }
    }

    default void emitPoint(String name,
                           Point point,
                           double altitude,
                           double heading,
                           double scale,
                           Map<String, String> attributes,
                           org.locationtech.proj4j.CoordinateReferenceSystem targetCrs,
                           Consumer<TileTransformInfo> consumer) {
        double x = point.getX();
        double y = point.getY();

        Vector3d position;
        if (targetCrs != null) {
            ProjCoordinate projCoordinate = new ProjCoordinate(x, y, 0.0d);
            ProjCoordinate centerWgs84 = GlobeUtils.transform(targetCrs, projCoordinate);
            position = new Vector3d(centerWgs84.x, centerWgs84.y, altitude);
        } else {
            position = new Vector3d(x, y, altitude);
        }

        TileTransformInfo tileTransformInfo = TileTransformInfo.builder()
                .name(name)
                .position(position)
                .heading(heading)
                .tilt(0.0d)
                .roll(0.0d)
                .scaleX(scale)
                .scaleY(scale)
                .scaleZ(scale)
                .properties(attributes)
                .build();
        consumer.accept(tileTransformInfo);
    }

    default String castStringFromObject(Object object, String defaultValue) {
        String result;
        if (object == null) {
            result = defaultValue;
        } else if (object instanceof String) {
            result = (String) object;
        } else if (object instanceof Integer) {
            result = String.valueOf((int) object);
        } else if (object instanceof Long) {
            result = String.valueOf(object);
        } else if (object instanceof Double) {
            result = String.valueOf((double) object);
        } else if (object instanceof Short) {
            result = String.valueOf((short) object);
        } else {
            result = object.toString();
        }
        return result;
    }
}
