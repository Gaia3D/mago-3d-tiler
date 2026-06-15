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
import java.util.function.Consumer;

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
        List<TileTransformInfo> result = new ArrayList<>();
        readEach(file, result::add);
        return result;
    }

    @Override
    public void readEach(File file, Consumer<TileTransformInfo> consumer) {
        List<AttributeFilter> attributeFilters = parametricOptions.getAttributeFilters();
        boolean isDefaultCrs = Objects.equals(parametricOptions.getSourceCrs(), new CRSFactory().createFromName("EPSG:3857"));
        String altitudeColumnName = parametricOptions.getAltitudeColumnName();
        String headingColumnName = parametricOptions.getHeadingColumnName();
        String scaleColumnName = parametricOptions.getScaleColumnName();
        String densityColumnName = parametricOptions.getDensityColumnName();

        ShpFiles shpFiles = null;
        ShapefileReader reader = null;
        ShapefileDataStore dataStore = null;
        try {
            shpFiles = new ShpFiles(file);
            reader = new ShapefileReader(shpFiles, true, true, new GeometryFactory());
            dataStore = new ShapefileDataStore(file.toURI().toURL());

            ShapeEncodingFix shapeEncodingFix = new ShapeEncodingFix();
            dataStore.setCharset(shapeEncodingFix.detectCharset(file));

            String typeName = dataStore.getTypeNames()[0];
            SimpleFeatureSource source = dataStore.getFeatureSource(typeName);
            var query = new Query(typeName, Filter.INCLUDE);
            query.getHints().add(new Hints(Hints.FEATURE_2D, true));

            SimpleFeatureCollection features = source.getFeatures(query);
            var coordinateReferenceSystem = features.getSchema().getCoordinateReferenceSystem();
            if (isDefaultCrs && coordinateReferenceSystem != null) {
                CoordinateReferenceSystem crs = GlobeUtils.convertProj4jCrsFromGeotoolsCrs(coordinateReferenceSystem);
                log.info(" - Coordinate Reference System : {}", crs.getName());
                parametricOptions.setSourceCrs(crs);
            }

            int featureIndex = 0;
            int featuresCount = source.getCount(query);
            boolean showProgress = featuresCount >= 10000;
            int progressInterval = Math.max(featuresCount / 100, 1);
            try (FeatureIterator<SimpleFeature> iterator = features.features()) {
                while (iterator.hasNext()) {
                    featureIndex++;
                    if (showProgress && featureIndex % progressInterval == 0) {
                        log.info(" - Processing feature {}/{} ({}%)", featureIndex, featuresCount, (double) featureIndex / (double) featuresCount * 100.0d);
                    } else if (!showProgress) {
                        log.info(" - Processing feature {}/{}", featureIndex, featuresCount);
                    }

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

                    if (geom == null) {
                        continue;
                    }

                    Map<String, String> attributes = extractAttributes(feature);
                    emitTileTransformInfos("I3dmFromShape", geom, coordinateReferenceSystem, density, scale, altitude, heading, attributes, parametricOptions.getSourceCrs(), consumer);
                }
            }
        } catch (IOException e) {
            log.error("[ERROR] :", e);
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.debug("Failed to close shapefile reader.", e);
                }
            }
            if (shpFiles != null) {
                shpFiles.dispose();
            }
            if (dataStore != null) {
                dataStore.dispose();
            }
        }
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

}
