package com.gaia3d.converter.geopackage;

import com.gaia3d.converter.AttributeFilter;
import com.gaia3d.converter.Parametric3DOptions;
import com.gaia3d.converter.kml.AttributeReader;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.util.GlobeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.data.*;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.referencing.CRS;
import org.joml.Vector3d;
import org.locationtech.jts.geom.*;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;

/**
 * KmlReader is a class that reads kml files.
 * It reads kml files and returns the information of the kml file.
 */
@Slf4j
@RequiredArgsConstructor
public class GeoPackageInstanceConverter implements AttributeReader {

    private final Parametric3DOptions parametricOptions;

    @Override
    public TileTransformInfo read(File file) {
        log.error("ShapePointReader read method is not implemented yet.");
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
        String heightColumnName = parametricOptions.getHeightColumnName();
        String scaleColumnName = parametricOptions.getScaleColumnName();
        String densityColumnName = parametricOptions.getDensityColumnName();

        try (GeoPackage geoPackage = new GeoPackage(file)) {
            List<FeatureEntry> features = geoPackage.features();
            for (FeatureEntry featureEntry : features) {
                org.geotools.api.referencing.crs.CoordinateReferenceSystem coordinateReferenceSystem = featureEntry.getBounds() != null
                        ? featureEntry.getBounds().getCoordinateReferenceSystem()
                        : null;
                if (isDefaultCrs && coordinateReferenceSystem != null) {
                    CoordinateReferenceSystem crs = GlobeUtils.convertProj4jCrsFromGeotoolsCrs(coordinateReferenceSystem);
                    log.info(" - Coordinate Reference System : {}", crs.getName());
                    parametricOptions.setSourceCrs(crs);
                }

                Filter filter;
                Transaction transaction = Transaction.AUTO_COMMIT;

                Map<String, Object> params = new HashMap<>();
                params.put("dbtype", "geopkg");
                params.put("database", file.getAbsolutePath());
                DataStore dataStore = DataStoreFinder.getDataStore(params);
                try {
                    SimpleFeatureSource featureSource = dataStore.getFeatureSource(featureEntry.getTableName());

                    long totalFeaturesCount;
                    boolean hasEnvelope = false;
                    if (hasEnvelope) {
                        double minX = 128.4546;
                        double minY = 37.3259;
                        double maxX = 128.5076;
                        double maxY = 37.3792;
                        org.geotools.api.referencing.crs.CoordinateReferenceSystem queryCrs = null;
                        org.geotools.api.referencing.crs.CoordinateReferenceSystem dataCrs = featureEntry.getBounds().getCoordinateReferenceSystem();
                        try {
                            queryCrs = CRS.decode("EPSG:4326", true);
                        } catch (FactoryException e) {
                            throw new RuntimeException(e);
                        }
                        ReferencedEnvelope queryEnv = new ReferencedEnvelope(minX, maxX, minY, maxY, queryCrs);

                        ReferencedEnvelope dataEnv;
                        try {
                            dataEnv = queryEnv.transform(dataCrs, true);
                        } catch (TransformException | FactoryException e) {
                            log.error("Error transforming envelope:", e);
                            throw new RuntimeException(e);
                        }
                        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
                        String geomField = featureSource.getFeatures().getSchema().getGeometryDescriptor().getLocalName();
                        filter = ff.bbox(ff.property(geomField), dataEnv);
                    } else {
                        filter = Filter.INCLUDE;
                    }
                    totalFeaturesCount = featureSource.getCount(new Query(featureEntry.getTableName(), filter));
                    log.info(" - Total Features Count: {}", totalFeaturesCount);
                    boolean showProgress = totalFeaturesCount >= 10000;
                    int progressInterval = (int) (totalFeaturesCount / 100);
                    if (progressInterval == 0) {
                        progressInterval = 1;
                    }

                    long featureIndex = 0;
                    try (SimpleFeatureReader simpleFeatureReader = geoPackage.reader(featureEntry, filter, transaction)) {
                        while (simpleFeatureReader.hasNext()) {
                            featureIndex++;
                            if (showProgress && featureIndex % progressInterval == 0) {
                                log.info(" - Processing feature {}/{} ({}%)", featureIndex, totalFeaturesCount, (double) featureIndex / (double) totalFeaturesCount * 100.0d);
                            }

                            SimpleFeature feature = simpleFeatureReader.next();
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
                            try {
                                emitTileTransformInfos("i3dm", geom, coordinateReferenceSystem, density, scale, altitude, heading, attributes, parametricOptions.getSourceCrs(), consumer);
                            } catch (RuntimeException e) {
                                log.error("Error processing geometry:", e);
                                throw e;
                            }
                        }
                    }
                } finally {
                    if (dataStore != null) {
                        dataStore.dispose();
                    }
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
