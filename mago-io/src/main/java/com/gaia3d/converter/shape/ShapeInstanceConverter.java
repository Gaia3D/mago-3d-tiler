package com.gaia3d.converter.shape;

import com.gaia3d.converter.AttributeFilter;
import com.gaia3d.converter.kml.AttributeReader;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.converter.Parametric3DOptions;
import com.gaia3d.util.GlobeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.factory.Hints;
import org.joml.Vector3d;
import org.locationtech.jts.geom.*;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * KmlReader is a class that reads kml files.
 * It reads kml files and returns the information of the kml file.
 */
@Slf4j
@RequiredArgsConstructor
public class ShapeInstanceConverter implements AttributeReader {

    private final Parametric3DOptions parametricOptions;

    //read kml file
    @Override
    public TileTransformInfo read(File file) {
        log.error("[ERROR] ShapePointReader read method is not implemented yet.");
        return null;
    }

    @Override
    public List<TileTransformInfo> readAll(File file) {

        List<AttributeFilter> attributeFilters = parametricOptions.getAttributeFilters();
        boolean isDefaultCrs = parametricOptions.getSourceCrs().equals(new CRSFactory().createFromName("EPSG:3857"));
        String altitudeColumnName = parametricOptions.getAltitudeColumnName();
        String headingColumnName = parametricOptions.getHeadingColumnName();
        String scaleColumnName = parametricOptions.getScaleColumnName();
        String densityColumnName = parametricOptions.getDensityColumnName();

        List<TileTransformInfo> result = new ArrayList<>();
        ShpFiles shpFiles;
        ShapefileReader reader;
        try {
            shpFiles = new ShpFiles(file);
            reader = new ShapefileReader(shpFiles, true, true, new GeometryFactory());
            ShapefileDataStore dataStore = new ShapefileDataStore(file.toURI().toURL());

            ShapeEncodingFix shapeEncodingFix = new ShapeEncodingFix();
            dataStore.setCharset(shapeEncodingFix.detectCharset(file));

            String typeName = dataStore.getTypeNames()[0];
            SimpleFeatureSource source = dataStore.getFeatureSource(typeName);
            var query = new Query(typeName, Filter.INCLUDE);
            query.getHints().add(new Hints(Hints.FEATURE_2D, true));

            SimpleFeatureCollection features = source.getFeatures(query);
            FeatureIterator<SimpleFeature> iterator = features.features();

            var coordinateReferenceSystem = features.getSchema().getCoordinateReferenceSystem();
            if (isDefaultCrs && coordinateReferenceSystem != null) {
                CoordinateReferenceSystem crs = GlobeUtils.convertProj4jCrsFromGeotoolsCrs(coordinateReferenceSystem);
                log.info(" - Coordinate Reference System : {}", crs.getName());
                parametricOptions.setSourceCrs(crs);
            }

            int count = 1;
            int featuresCount = source.getCount(query);
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();

                double heading = getNumberAttribute(feature, headingColumnName, parametricOptions.getDefaultHeading());
                double altitude = getNumberAttribute(feature, altitudeColumnName, parametricOptions.getAbsoluteAltitudeValue());
                double scale = getNumberAttribute(feature, scaleColumnName, parametricOptions.getDefaultScale());
                double density = getNumberAttribute(feature, densityColumnName, parametricOptions.getDefaultDensity());

                if (!attributeFilters.isEmpty()) {
                    boolean filterFlag = false;
                    for (AttributeFilter attributeFilter : attributeFilters) {
                        String columnName = attributeFilter.getAttributeName();
                        String filterValue = attributeFilter.getAttributeValue();
                        String attributeValue = castStringFromObject(feature.getAttribute(columnName), "null");
                        if (filterValue.equals(attributeValue)) {
                            filterFlag = true;
                            break;
                        }
                    }
                    if (!filterFlag) {
                        continue;
                    }
                }

                log.info("[pre][{}/{}] Loading file : {}", count++, featuresCount, file.getName());
                List<Point> points = new ArrayList<>();
                if (geom instanceof MultiPolygon multiPolygon) {
                    int numGeometries = multiPolygon.getNumGeometries();
                    for (int i = 0; i < numGeometries; i++) {
                        Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
                        try {
                            int calculatePointCount = calculatePointCount(polygon, coordinateReferenceSystem, density, scale);
                            points.addAll(getRandomPointsWithDensity(polygon, calculatePointCount));
                        } catch (FactoryException | TransformException e) {
                            log.error("Error transforming geometry:", e);
                            throw new RuntimeException(e);
                        }
                    }
                } else if (geom instanceof Polygon polygon) {
                    try {
                        int calculatePointCount = calculatePointCount(polygon, coordinateReferenceSystem, density, scale);
                        points.addAll(getRandomPointsWithDensity(polygon, calculatePointCount));
                    } catch (FactoryException | TransformException e) {
                        log.error("Error transforming geometry: ", e);
                        throw new RuntimeException(e);
                    }
                } else if (geom instanceof MultiPoint) {
                    GeometryFactory factory = geom.getFactory();
                    Coordinate[] coordinates = geom.getCoordinates();
                    for (Coordinate coordinate : coordinates) {
                        Point point = factory.createPoint(coordinate);
                        points.add(point);
                    }
                } else if (geom instanceof Point point) {
                    points.add(point);
                } else {
                    log.error("[ERROR] Geometry type is not supported.");
                    continue;
                }

                for (Point point : points) {
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
                        attributes.put(attributeDescriptor.getName().getLocalPart(), attributeString);
                    });

                    double x = point.getX();
                    double y = point.getY();

                    Vector3d position;
                    CoordinateReferenceSystem crs = parametricOptions.getSourceCrs();
                    if (crs != null) {
                        ProjCoordinate projCoordinate = new ProjCoordinate(x, y, 0.0d);
                        ProjCoordinate centerWgs84 = GlobeUtils.transform(crs, projCoordinate);
                        position = new Vector3d(centerWgs84.x, centerWgs84.y, altitude);
                    } else {
                        position = new Vector3d(x, y, altitude);
                    }

                    TileTransformInfo tileTransformInfo = TileTransformInfo.builder()
                            .name("I3dmFromShape")
                            .position(position)
                            .heading(heading)
                            .tilt(0.0d)
                            .roll(0.0d)
                            .scaleX(scale)
                            .scaleY(scale)
                            .scaleZ(scale)
                            .properties(attributes)
                            .build();
                    result.add(tileTransformInfo);
                }
            }

            iterator.close();
            reader.close();
            shpFiles.dispose();
            dataStore.dispose();
            reader.close();
        } catch (IOException e) {
            log.error("[ERROR] :", e);
            throw new RuntimeException(e);
        }
        shpFiles.dispose();
        return result;
    }

    private double getNumberAttribute(SimpleFeature feature, String column, double defaultValue) {
        double result = defaultValue;
        Object attributeLower = feature.getAttribute(column);
        Object attributeUpper = feature.getAttribute(column.toUpperCase());
        Object attributeObject = null;
        if (attributeLower != null) {
            attributeObject = attributeLower;
        } else if (attributeUpper != null) {
            attributeObject = attributeUpper;
        }

        if (attributeObject instanceof Short) {
            result = result + (short) attributeObject;
        } else if (attributeObject instanceof Integer) {
            result = result + (int) attributeObject;
        } else if (attributeObject instanceof Long) {
            result = result + (Long) attributeObject;
        } else if (attributeObject instanceof Double) {
            result = result + (double) attributeObject;
        } else if (attributeObject instanceof String) {
            result = Double.parseDouble((String) attributeObject);
        }
        return result;
    }

    private String castStringFromObject(Object object, String defaultValue) {
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
