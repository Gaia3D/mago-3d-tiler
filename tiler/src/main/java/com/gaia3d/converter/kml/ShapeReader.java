package com.gaia3d.converter.kml;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.geometry.GaiaExtrusionBuilding;
import com.gaia3d.util.GlobeUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureIterator;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * KmlReader is a class that reads kml files.
 * It reads kml files and returns the information of the kml file.
 */
@Slf4j
@NoArgsConstructor
public class ShapeReader implements AttributeReader {

    //read kml file
    @Override
    public KmlInfo read(File file) {
        log.warn("not yet");
        return null;
    }

    @Override
    public List<KmlInfo> readAll(File file) {
        List<KmlInfo> result = new ArrayList<>();

        GlobalOptions globalOptions = GlobalOptions.getInstance();

        ShpFiles shpFiles = null;
        ShapefileReader reader = null;
        try {
            shpFiles = new ShpFiles(file);
            reader = new ShapefileReader(shpFiles, true, true, new GeometryFactory());
            DataStore dataStore = new ShapefileDataStore(file.toURI().toURL());
            String typeName = dataStore.getTypeNames()[0];
            SimpleFeatureSource source = dataStore.getFeatureSource(typeName);
            var query = new Query(typeName, Filter.INCLUDE);
            query.getHints().add(new Hints(Hints.FEATURE_2D, true)); // for 3d

            SimpleFeatureCollection features = source.getFeatures(query);

            FeatureIterator<SimpleFeature> iterator = features.features();
            List<GaiaExtrusionBuilding> buildings = new ArrayList<>();
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();

                Point point = null;
                if (geom instanceof MultiPoint) {
                    //(Point) geom.getCoordinates();
                } else if (geom instanceof Point) {
                    point = (Point) geom;
                }

                double x = point.getX();
                double y = point.getY();

                Vector3d position;
                CoordinateReferenceSystem crs = globalOptions.getCrs();
                if (crs != null) {
                    ProjCoordinate projCoordinate = new ProjCoordinate(x, y, 0.0d);
                    ProjCoordinate centerWgs84 = GlobeUtils.transform(crs, projCoordinate);
                    position = new Vector3d(centerWgs84.x, centerWgs84.y, 0.0d);
                } else {
                    position = new Vector3d(x, y, 0.0d);
                }

                KmlInfo kmlInfo = KmlInfo.builder().position(position).name("fromShape").heading(0.0d).tilt(0.0d).roll(0.0d).scaleX(1.0d).scaleY(1.0d).scaleZ(1.0d).build();

                result.add(kmlInfo);

                /*if (!lineString.isValid()) {
                    log.warn("Invalid : {}", feature.getID());
                    continue;
                }*/

                /*GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
                Coordinate[] coordinates = lineString.getCoordinates();

                GaiaBoundingBox boundingBox = new GaiaBoundingBox();
                List<Vector3d> positions = new ArrayList<>();

                Vector3d firstPosition = null;
                for (Coordinate coordinate : coordinates) {
                    Point point = geometryFactory.createPoint(coordinate);

                    double x, y;
                    if (flipCoordinate) {
                        x = point.getY();
                        y = point.getX();
                    } else {
                        x = point.getX();
                        y = point.getY();
                    }

                    Vector3d position;
                    CoordinateReferenceSystem crs = globalOptions.getCrs();
                    if (crs != null) {
                        ProjCoordinate projCoordinate = new ProjCoordinate(x, y, boundingBox.getMinZ());
                        ProjCoordinate centerWgs84 = GlobeUtils.transform(crs, projCoordinate);
                        position = new Vector3d(centerWgs84.x, centerWgs84.y, 0.0d);
                    } else {
                        position = new Vector3d(x, y, 0.0d);
                    }

                    if (firstPosition == null) {
                        firstPosition = position;
                    } else if (firstPosition.equals(position)) {
                        break;
                    }
                    positions.add(position);
                    boundingBox.addPoint(position);
                }

                if (positions.size() >= 3) {
                    String name = getAttribute(feature, nameColumnName);
                    double height = getHeight(feature, heightColumnName, minimumHeightValue);
                    double altitude = absoluteAltitudeValue;
                    if (altitudeColumnName != null) {
                        altitude = getAltitude(feature, altitudeColumnName);
                    }
                    GaiaBuilding building = GaiaBuilding.builder()
                            .id(feature.getID())
                            .name(name)
                            .boundingBox(boundingBox)
                            .floorHeight(altitude)
                            .roofHeight(height)
                            .positions(positions)
                            .build();
                    buildings.add(building);
                } else {
                    String name = getAttribute(feature, nameColumnName);
                    log.warn("Invalid Geometry : {}, {}", feature.getID(), name);
                }*/
            }

            iterator.close();
            reader.close();
            shpFiles.dispose();
            dataStore.dispose();
            reader.close();
        } catch (IOException e) {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        }
        shpFiles.dispose();
        return result;
    }
}
