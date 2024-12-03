package com.gaia3d.converter.geometry.geojson;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.AttributeReader;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.util.GlobeUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.geojson.GeoJSONDataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.util.factory.Hints;
import org.joml.Vector3d;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * KmlReader is a class that reads kml files.
 * It reads kml files and returns the information of the kml file.
 */
@Slf4j
@NoArgsConstructor
public class GeojsonPointReader implements AttributeReader {

    //read kml file
    @Override
    public KmlInfo read(File file) {
        log.error("GeojsonPointReader read method is not implemented yet.");
        return null;
    }

    @Override
    public List<KmlInfo> readAll(File file) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();

        List<KmlInfo> result = new ArrayList<>();
        String altitudeColumnName = globalOptions.getAltitudeColumn();
        String headingColumnName = globalOptions.getHeadingColumn();

        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(Files.newInputStream(file.toPath()))){
            FeatureJSON geojson = new FeatureJSON();
            log.info("Reading GeoJSON file : {}", file.getAbsolutePath());
            SimpleFeatureCollection featureCollection = (SimpleFeatureCollection) geojson.readFeatureCollection(bufferedInputStream);
            FeatureIterator<SimpleFeature> iterator = featureCollection.features();
            log.info("Reading GeoJSON file : {} done", file.getAbsolutePath());
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();

                Point point = null;
                if (geom instanceof MultiPoint) {
                    GeometryFactory factory = geom.getFactory();
                    point = factory.createPoint(geom.getCoordinate());
                } else if (geom instanceof Point) {
                    point = (Point) geom;
                } else {
                    log.error("Geometry type is not supported.");
                    continue;
                }

                double x = point.getX();
                double y = point.getY();
                double heading = getNumberAttribute(feature, headingColumnName, 0.0d);
                double altitude = getNumberAttribute(feature, altitudeColumnName, 0.0d);

                Vector3d position;
                CoordinateReferenceSystem crs = globalOptions.getCrs();
                if (crs != null) {
                    ProjCoordinate projCoordinate = new ProjCoordinate(x, y, 0.0d);
                    ProjCoordinate centerWgs84 = GlobeUtils.transform(crs, projCoordinate);
                    position = new Vector3d(centerWgs84.x, centerWgs84.y, altitude);
                } else {
                    position = new Vector3d(x, y, altitude);
                }

                KmlInfo kmlInfo = KmlInfo.builder()
                        .name("I3dmFromGeojson")
                        .position(position)
                        .heading(heading)
                        .tilt(0.0d)
                        .roll(0.0d)
                        .scaleX(1.0d)
                        .scaleY(1.0d)
                        .scaleZ(1.0d)
                        .build();
                result.add(kmlInfo);
            }
            iterator.close();
        } catch (IOException e) {
            log.error("Error : {}", e.getMessage());
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
}
