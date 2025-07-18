package com.gaia3d.converter.geometry.geopackage;

import com.gaia3d.command.mago.AttributeFilter;
import com.gaia3d.command.mago.GlobalConstants;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.AttributeReader;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.util.GlobeUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.joml.Vector3d;
import org.locationtech.jts.geom.*;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * KmlReader is a class that reads kml files.
 * It reads kml files and returns the information of the kml file.
 */
@Slf4j
@NoArgsConstructor
public class GeoPackageInstanceConverter implements AttributeReader {

    //read kml file
    @Override
    public TileTransformInfo read(File file) {
        log.error("ShapePointReader read method is not implemented yet.");
        return null;
    }

    @Override
    public List<TileTransformInfo> readAll(File file) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();

        List<AttributeFilter> attributeFilters = globalOptions.getAttributeFilters();
        List<TileTransformInfo> result = new ArrayList<>();
        boolean isDefaultCrs = globalOptions.getCrs().equals(GlobalConstants.DEFAULT_CRS);
        String altitudeColumnName = globalOptions.getAltitudeColumn();
        String headingColumnName = globalOptions.getHeadingColumn();
        String scaleColumnName = globalOptions.getScaleColumn();
        String densityColumnName = globalOptions.getDensityColumn();

        GeoPackage geoPackage = null;
        try {
            geoPackage = new GeoPackage(file);
            List<FeatureEntry> features = geoPackage.features();
            for (FeatureEntry featureEntry : features) {

                var coordinateReferenceSystem = featureEntry.getBounds().getCoordinateReferenceSystem();
                if (isDefaultCrs && coordinateReferenceSystem != null) {
                    CoordinateReferenceSystem crs = GlobeUtils.convertProj4jCrsFromGeotoolsCrs(coordinateReferenceSystem);
                    log.info(" - Coordinate Reference System : {}", crs.getName());
                    globalOptions.setCrs(crs);
                }

                Filter filter = Filter.INCLUDE;
                Transaction transaction = Transaction.AUTO_COMMIT;
                SimpleFeatureReader simpleFeatureReader = geoPackage.reader(featureEntry, filter, transaction);
                while (simpleFeatureReader.hasNext()) {
                    SimpleFeature feature = simpleFeatureReader.next();
                    Geometry geom = (Geometry) feature.getDefaultGeometry();

                    double heading = getNumberAttribute(feature, headingColumnName, GlobalConstants.DEFAULT_HEIGHT);
                    double altitude = getNumberAttribute(feature, altitudeColumnName, GlobalConstants.DEFAULT_ALTITUDE);
                    double scale = getNumberAttribute(feature, scaleColumnName, GlobalConstants.DEFAULT_SCALE);
                    double density = getNumberAttribute(feature, densityColumnName, GlobalConstants.DEFAULT_DENSITY);

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
                    List<Point> points = new ArrayList<>();
                    if (geom instanceof MultiPolygon multiPolygon) {
                        for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                            Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
                            try {
                                int calculatePointCount = calculatePointCount(polygon, coordinateReferenceSystem, density, scale);
                                points.addAll(getRandomPointsWithDensity(polygon, calculatePointCount));
                            } catch (FactoryException | TransformException e) {
                                log.error("Error transforming geometry: {}", e.getMessage());
                                throw new RuntimeException(e);
                            }
                        }
                    } else if (geom instanceof Polygon polygon) {
                        try {
                            int calculatePointCount = calculatePointCount(polygon, coordinateReferenceSystem, density, scale);
                            points.addAll(getRandomPointsWithDensity(polygon, calculatePointCount));
                        } catch (FactoryException | TransformException e) {
                            log.error("Error transforming geometry: {}", e.getMessage());
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
                        log.error("Geometry type is not supported.");
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
                        CoordinateReferenceSystem crs = globalOptions.getCrs();
                        if (crs != null) {
                            ProjCoordinate projCoordinate = new ProjCoordinate(x, y, 0.0d);
                            ProjCoordinate centerWgs84 = GlobeUtils.transform(crs, projCoordinate);
                            position = new Vector3d(centerWgs84.x, centerWgs84.y, altitude);
                        } else {
                            position = new Vector3d(x, y, altitude);
                        }

                        TileTransformInfo tileTransformInfo = TileTransformInfo.builder()
                                .name("I3dmFromGeoPackage")
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
            }
            geoPackage.close();
        } catch (IOException e) {
            if (geoPackage != null) {
                geoPackage.close();
            }
            log.error("[ERROR] :", e);
            throw new RuntimeException(e);
        }
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
