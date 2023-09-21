package com.gaia3d.converter.geometry.geojson;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.GaiaMaterial;
import com.gaia3d.basic.structure.GaiaNode;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.geometry.*;
import com.gaia3d.process.ProcessOptions;
import com.gaia3d.util.GlobeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.geotools.data.DataStore;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollectionIteration;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.GeoJSONUtil;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.jts.geom.*;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GeoJsonConverter extends AbstractGeometryConverter implements Converter {
    private final CommandLine command;
    private final CoordinateReferenceSystem crs;

    public GeoJsonConverter(CommandLine command, CoordinateReferenceSystem crs) {
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

    @Override
    protected List<GaiaScene> convert(File file) {
        List<GaiaScene> scenes = new ArrayList<>();
        Tessellator tessellator = new Tessellator();
        Extruder extruder = new Extruder(tessellator);
        boolean flipCoordnate = this.command.hasOption(ProcessOptions.Flip_Coordinate.getArgName());

        try {
            FeatureJSON gjson = new FeatureJSON();
            String json = Files.readString(file.toPath());

            SimpleFeatureCollection featureCollection = (SimpleFeatureCollection) gjson.readFeatureCollection(new StringReader(json));
            FeatureIterator<SimpleFeature> iterator = featureCollection.features();

            List<GaiaBuilding> buildings = new ArrayList<>();
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();
                //log.info(geom.getCoordinates());

                GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
                Coordinate[] coordinates = geom.getCoordinates();

                GaiaBoundingBox boundingBox = new GaiaBoundingBox();
                List<Vector3d> positions = new ArrayList<>();

                Vector3d firstPosition = null;
                for (Coordinate coordinate : coordinates) {
                    //log.info(coordinate.toString());
                    Point point = geometryFactory.createPoint(coordinate);
                    double x, y;
                    if (flipCoordnate) {
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

                    positions.add(position);
                    boundingBox.addPoint(position);
                }

                double height = getHeight(feature);
                GaiaBuilding building = GaiaBuilding.builder()
                        .id(feature.getID())
                        .name("test")
                        .boundingBox(boundingBox)
                        .floorHeight(0)
                        .roofHeight(height)
                        .positions(positions)
                        .build();
                buildings.add(building);
            }


            iterator.close();

            for (GaiaBuilding building : buildings) {
                GaiaScene scene = initScene();
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return scenes;
    }

    private double getHeight(SimpleFeature feature) {
        List<Object> attributes = feature.getAttributes();

        double result = 0.0d;
        Object heightLower = feature.getAttribute("height");
        Object heightUpper = feature.getAttribute("HEIGHT");
        Object heightObject = null;
        if (heightLower != null) {
            heightObject = heightLower;
        } else if (heightUpper != null) {
            heightObject = heightUpper;
        }

        if (heightObject instanceof Integer) {
            result = result + (int) heightObject;
        } else if (heightObject instanceof Long) {
            result = result + (long) heightObject;
        } else if (heightObject instanceof Short) {
            result = result + (short) heightObject;
        } else if (heightObject instanceof Double) {
            result = result + (double) heightObject;
        } else if (heightObject instanceof String) {
            String heightString = (String) heightObject;
            if (heightString.contains(".")) {
                result = Double.parseDouble(heightString);
            } else {
                result = (double) Integer.parseInt(heightString);
            }
        }

        if (result < 0.1) {
            result = 1.0d;
        }

        return result;
    }
}
