package com.gaia3d.converter.geometry.shape;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.GaiaMaterial;
import com.gaia3d.basic.structure.GaiaNode;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.geometry.*;
import com.gaia3d.util.GlobeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.util.factory.Hints;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.jts.geom.*;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ShapeConverter extends AbstractGeometryConverter implements Converter {

    @Override
    public List<GaiaScene> load(String path) {
        return convert(new File(path));
    }

    @Override
    public List<GaiaScene> load(File file) {
        return convert(file);
    }

    @Override
    public List<GaiaScene> load(Path path) {
        return convert(path.toFile());
    }

    protected List<GaiaScene> convert(File file) {
        List<GaiaScene> scenes = new ArrayList<>();
        Tessellator tessellator = new Tessellator();
        Extruder extruder = new Extruder(tessellator);
        InnerRingRemover innerRingRemover = new InnerRingRemover();

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        boolean flipCoordinate = globalOptions.isFlipCoordinate();
        String nameColumnName = globalOptions.getNameColumn();
        String heightColumnName = globalOptions.getHeightColumn();
        String altitudeColumnName = globalOptions.getAltitudeColumn();

        double absoluteAltitudeValue = globalOptions.getAbsoluteAltitude();
        double minimumHeightValue = globalOptions.getMinimumHeight();
        double skirtHeight = globalOptions.getSkirtHeight();

        ShpFiles shpFiles = null;
        ShapefileReader reader = null;
        try {
            shpFiles = new ShpFiles(file);
            reader = new ShapefileReader(shpFiles, true, true, new GeometryFactory());
            DataStore dataStore = new ShapefileDataStore(file.toURI().toURL());
            String typeName = dataStore.getTypeNames()[0];
            SimpleFeatureSource source = dataStore.getFeatureSource(typeName);

            source = dataStore.getFeatureSource(typeName);
            var query = new Query(typeName, Filter.INCLUDE);
            query.getHints().add(new Hints(Hints.FEATURE_2D, true)); // for 3d

            SimpleFeatureCollection features = source.getFeatures(query);

            FeatureIterator<SimpleFeature> iterator = features.features();
            List<GaiaExtrusionBuilding> buildings = new ArrayList<>();
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();

                Polygon polygon = null;
                LineString lineString = null;
                if (geom instanceof MultiPolygon) {
                    polygon = (Polygon) geom.getGeometryN(0);
                    lineString = polygon.getExteriorRing();
                } else if (geom instanceof Polygon) {
                    polygon = (Polygon) geom;
                    lineString = polygon.getExteriorRing();
                } else if (geom instanceof MultiLineString) {
                    lineString = (LineString) geom.getGeometryN(0);
                } else if (geom instanceof LineString) {
                    lineString = (LineString) geom;
                } else {
                    log.warn("Is Not Supported Geometry Type : {}", geom.getGeometryType());
                    continue;
                }
                if (!lineString.isValid()) {
                    log.warn("Invalid : {}", feature.getID());
                    //continue;
                }

                GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
                Coordinate[] outerCoordinates = lineString.getCoordinates();

                /*int innerRingCount = polygon.getNumInteriorRing();
                List<Coordinate[]> innerCoordinates = new ArrayList<>();
                for (int i = 0; i < innerRingCount; i++) {
                    LineString innerRing = polygon.getInteriorRingN(i);
                    innerCoordinates.add(innerRing.getCoordinates());
                }*/
                //outerCoordinates = innerRingRemover.removeAll(outerCoordinates, innerCoordinates);

                GaiaBoundingBox boundingBox = new GaiaBoundingBox();
                List<Vector3d> positions = new ArrayList<>();

                for (Coordinate coordinate : outerCoordinates) {
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
                    GaiaExtrusionBuilding building = GaiaExtrusionBuilding.builder()
                            .id(feature.getID())
                            .name(name)
                            .boundingBox(boundingBox)
                            .floorHeight(altitude)
                            .roofHeight(height + skirtHeight)
                            .positions(positions)
                            .build();
                    buildings.add(building);
                } else {
                    String name = getAttribute(feature, nameColumnName);
                    log.warn("Invalid Geometry : {}, {}", feature.getID(), name);
                }
            }

            iterator.close();
            reader.close();
            shpFiles.dispose();
            dataStore.dispose();

            for (GaiaExtrusionBuilding building : buildings) {
                GaiaScene scene = initScene();
                scene.setOriginalPath(file.toPath());

                GaiaMaterial material = scene.getMaterials().get(0);
                GaiaNode rootNode = scene.getNodes().get(0);
                rootNode.setName(building.getName());

                Vector3d center = building.getBoundingBox().getCenter();
                center.z = center.z - skirtHeight;

                Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
                Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerWorldCoordinate);
                Matrix4d transfromMatrixInv = new Matrix4d(transformMatrix).invert();

                List<Vector3d> localPositions = new ArrayList<>();
                for (Vector3d position : building.getPositions()) {
                    Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                    Vector3d localPosition = positionWorldCoordinate.mulPosition(transfromMatrixInv);
                    localPosition.z = 0.0d;
                    localPositions.add(localPosition);
                }

                Extrusion extrusion = extruder.extrude(localPositions, building.getRoofHeight(), building.getFloorHeight());
                GaiaNode node = createNode(material, extrusion.getPositions(), extrusion.getTriangles());
                rootNode.getChildren().add(node);

                Matrix4d rootTransformMatrix = new Matrix4d().identity();
                rootTransformMatrix.translate(center, rootTransformMatrix);
                rootNode.setTransformMatrix(rootTransformMatrix);
                scenes.add(scene);
            }
            dataStore.dispose();
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        shpFiles.dispose();
        return scenes;
    }
}
