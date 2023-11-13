package com.gaia3d.converter.geometry.shape;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.*;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.geometry.*;
import com.gaia3d.process.ProcessOptions;
import com.gaia3d.util.GlobeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
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
public class ShapeConverter extends AbstractGeometryConverter implements Converter {
    private final CommandLine command;
    private final CoordinateReferenceSystem crs;

    public ShapeConverter(CommandLine command, CoordinateReferenceSystem crs) {
        this.command = command;
        this.crs = crs;
    }

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
        boolean flipCoordinate = this.command.hasOption(ProcessOptions.FLIP_COORDINATE.getArgName());
        boolean hasNameColumn = this.command.hasOption(ProcessOptions.NAME_COLUMN.getArgName());
        boolean hasHeightColumn = this.command.hasOption(ProcessOptions.HEIGHT_COLUMN.getArgName());
        boolean hasAltitudeColumn = this.command.hasOption(ProcessOptions.ALTITUDE_COLUMN.getArgName());
        boolean hasAbsoluteAltitude = this.command.hasOption(ProcessOptions.ABSOLUTE_ALTITUDE.getArgName());
        boolean hasMinimumHeight = this.command.hasOption(ProcessOptions.MINIMUM_HEIGHT.getArgName());

        String nameColumnName;
        if (hasNameColumn) {
            nameColumnName = this.command.getOptionValue(ProcessOptions.NAME_COLUMN.getArgName());
        } else {
            nameColumnName = "ExtrusionBuilding";
        }
        String heightColumnName;
        if (hasHeightColumn) {
            heightColumnName = this.command.getOptionValue(ProcessOptions.HEIGHT_COLUMN.getArgName());
        } else {
            heightColumnName = "height";
        }

        String altitudeColumnName;
        if (hasAltitudeColumn) {
            altitudeColumnName = this.command.getOptionValue(ProcessOptions.ALTITUDE_COLUMN.getArgName());
        } else {
            altitudeColumnName = "altitude";
        }

        double absoluteAltitudeValue;
        if (hasAbsoluteAltitude) {
            String absoluteAltitude = this.command.getOptionValue(ProcessOptions.ABSOLUTE_ALTITUDE.getArgName());
            absoluteAltitudeValue = Double.parseDouble(absoluteAltitude);
        } else {
            absoluteAltitudeValue = 0.0d;
        }

        double minimumHeightValue;
        if (hasMinimumHeight) {
            String minimumHeight = this.command.getOptionValue(ProcessOptions.MINIMUM_HEIGHT.getArgName());
            minimumHeightValue = Double.parseDouble(minimumHeight);
        } else {
            minimumHeightValue = 1.0d;
        }

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
            List<GaiaBuilding> buildings = new ArrayList<>();
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();

                Polygon polygon = null;
                LineString lineString = null;
                if (geom instanceof MultiPolygon multiPolygon) {
                    polygon = (Polygon) multiPolygon.getGeometryN(0);
                    lineString = polygon.getExteriorRing();
                } else if (geom instanceof Polygon) {
                    polygon = (Polygon) geom;
                    lineString = polygon.getExteriorRing();
                } else if (geom instanceof MultiLineString multiLineString) {
                    lineString = (LineString) multiLineString.getGeometryN(0);
                } else if (geom instanceof LineString) {
                    lineString = (LineString) geom;
                } else {
                    log.warn("Is Not Supported Geometry Type : {}", geom.getGeometryType());
                    continue;
                }
                if (!lineString.isValid()) {
                    log.warn("Invalid : {}", feature.getID());
                    continue;
                }

                GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
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
                    double altitude = 0.0d;
                    if (hasAbsoluteAltitude) {
                        altitude = absoluteAltitudeValue;
                    } else if (hasAltitudeColumn) {
                        getAltitude(feature, altitudeColumnName, absoluteAltitudeValue);
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
                }
            }
            iterator.close();

            for (GaiaBuilding building : buildings) {
                GaiaScene scene = initScene(this.command);
                scene.setOriginalPath(file.toPath());

                GaiaMaterial material = scene.getMaterials().get(0);
                GaiaNode rootNode = scene.getNodes().get(0);
                rootNode.setName(building.getName());

                Vector3d center = building.getBoundingBox().getCenter();

                Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
                Matrix4d transformMatrix = GlobeUtils.normalAtCartesianPointWgs84(centerWorldCoordinate);
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
