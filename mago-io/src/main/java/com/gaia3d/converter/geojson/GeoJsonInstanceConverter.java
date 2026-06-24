package com.gaia3d.converter.geojson;

import com.gaia3d.converter.AttributeFilter;
import com.gaia3d.converter.Parametric3DOptions;
import com.gaia3d.converter.kml.AttributeReader;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.util.GlobeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * KmlReader is a class that reads kml files.
 * It reads kml files and returns the information of the kml file.
 */
@Slf4j
@RequiredArgsConstructor
public class GeoJsonInstanceConverter implements AttributeReader {

    private final Parametric3DOptions parametricOptions;

    //read kml file
    @Override
    public TileTransformInfo read(File file) {
        log.error("GeojsonPointReader read method is not implemented yet.");
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

        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            FeatureJSON geojson = new FeatureJSON();
            log.info("[Load] Reading GeoJSON file : {}", file.getAbsolutePath());
            SimpleFeatureCollection featureCollection = (SimpleFeatureCollection) geojson.readFeatureCollection(bufferedInputStream);
            log.info("[Load] Reading GeoJSON file : {} done", file.getAbsolutePath());

            var coordinateReferenceSystem = featureCollection.getSchema().getCoordinateReferenceSystem();
            if (isDefaultCrs && coordinateReferenceSystem != null) {
                CoordinateReferenceSystem crs = GlobeUtils.convertProj4jCrsFromGeotoolsCrs(coordinateReferenceSystem);
                log.info(" - Coordinate Reference System : {}", crs.getName());
                parametricOptions.setSourceCrs(crs);
            }

            int totalFeaturesCount = featureCollection.size();
            boolean showProgress = totalFeaturesCount >= 10000;
            int progressInterval = Math.max(totalFeaturesCount / 100, 1);
            int featureIndex = 0;

            try (FeatureIterator<SimpleFeature> iterator = featureCollection.features()) {
                while (iterator.hasNext()) {
                    featureIndex++;
                    if (showProgress && featureIndex % progressInterval == 0) {
                        log.info(" - Processing feature {}/{} ({}%)", featureIndex, totalFeaturesCount, (double) featureIndex / (double) totalFeaturesCount * 100.0d);
                    } else if (!showProgress) {
                        log.info(" - Processing feature {}/{}", featureIndex, totalFeaturesCount);
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
                    emitTileTransformInfos("I3dmFromGeojson", geom, coordinateReferenceSystem, density, scale, altitude, heading, attributes, parametricOptions.getSourceCrs(), consumer);
                }
            }
        } catch (IOException e) {
            log.error("[ERROR] :", e);
            throw new RuntimeException(e);
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
