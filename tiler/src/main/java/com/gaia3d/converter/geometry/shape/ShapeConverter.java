package com.gaia3d.converter.geometry.shape;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.GaiaPipeLineString;
import com.gaia3d.basic.geometry.networkStructure.modeler.Modeler3D;
import com.gaia3d.basic.geometry.tessellator.GaiaExtruder;
import com.gaia3d.basic.geometry.tessellator.GaiaExtrusionSurface;
import com.gaia3d.basic.geometry.tessellator.Vector3dOnlyHashEquals;
import com.gaia3d.basic.structure.*;
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
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTSFactoryFinder;

import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.locationtech.jts.geom.*;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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
        GaiaExtruder gaiaExtruder = new GaiaExtruder();
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
            ContentFeatureSource source = (ContentFeatureSource) dataStore.getFeatureSource(typeName);
            var query = new Query(typeName, Filter.INCLUDE);
            //query.getHints().add(new Hints(Hints.FEATURE_2D, true));

            SimpleFeatureCollection features = source.getFeatures(query);
            FeatureIterator<SimpleFeature> iterator = features.features();
            List<GaiaExtrusionBuilding> buildings = new ArrayList<>();
            List<GaiaPipeLineString> pipeLineStrings = new ArrayList<>();
            int counterAux = 0;
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();

                if (geom == null) {
                    log.warn("Is Null Geometry : {}", feature.getID());
                    continue;
                }

                List<Polygon> polygons = new ArrayList<>();
                List<LineString> LineStrings = new ArrayList<>();
                if (geom instanceof MultiPolygon) {
                    int count = geom.getNumGeometries();
                    for (int i = 0; i < count; i++) {
                        Polygon polygon = (Polygon) geom.getGeometryN(i);
                        polygons.add(polygon);
                    }
                } else if (geom instanceof Polygon) {
                    polygons.add((Polygon) geom);
                } else if (geom instanceof LineString) {
                    LineStrings.add((LineString) geom);
                } else if (geom instanceof MultiLineString) {
                    int count = geom.getNumGeometries();
                    for (int i = 0; i < count; i++) {
                        LineString lineString = (LineString) geom.getGeometryN(i);
                        LineStrings.add(lineString);
                    }
                } else {
                    log.warn("Is Not Supported Geometry Type : {}", geom.getGeometryType());
                    continue;
                }

                // Check lineStrings. Convert to pipes.***
                int lineStringsCount = LineStrings.size();
                for (LineString lineString : LineStrings) {
                    Coordinate[] coordinates = lineString.getCoordinates();
                    List<Vector3d> positions = new ArrayList<>();
                    for (Coordinate coordinate : coordinates) {
                        Point point = new GeometryFactory().createPoint(coordinate);
                        double x, y, z;
                        if (flipCoordinate) {
                            x = point.getY();
                            y = point.getX();
                        } else {
                            x = point.getX();
                            y = point.getY();
                        }

                        z = point.getCoordinate().getZ();

                        Vector3d position = new Vector3d(x, y, z); // usually crs 3857.***
                        positions.add(position);
                    }

                    if (positions.size() >= 2) {
                        String columnName = "LOW_DEP";
                        String lowerDepthText = getAttribute(feature, columnName);
                        //Double lowerDepth = Double.parseDouble(lowerDepthText);

                        columnName = "HGH_DEP";
                        String higherDepthText = getAttribute(feature, columnName);
                        //Double higherDepth = Double.parseDouble(higherDepthText);

                        columnName = "PIP_LBL";
                        String pipeLabel = getAttribute(feature, columnName);
                        String separator = "/";
                        String[] pipeLabelTokens = pipeLabel.split(separator);
                        String diameterCmString = pipeLabelTokens[2];
                        int pipeProfileType = 0; // 1 = circular, 2 = rectangular.***
                        float rectangleWidth = 0.0f;
                        float rectangleHeight = 0.0f;

                        if (diameterCmString.isEmpty()) {
                            continue;
                        }

                        // check the 1rst character of the string.***
                        char firstChar = diameterCmString.charAt(0);
                        // '⌀' = 248.***
                        // '?' = 63.***
                        if (firstChar == 248 || firstChar == 63) {
                            // delete the 1rst character of the string.***
                            diameterCmString = diameterCmString.substring(1);
                            if (diameterCmString.isEmpty()) {
                                continue;
                            }

                            // finally check if the diameterCmString is "number X number".***
                            separator = "X|x";
                            String[] diameterCmStringTokens = diameterCmString.split(separator);
                            if (diameterCmStringTokens.length == 2) {
                                diameterCmString = diameterCmStringTokens[0];
                            }

                            pipeProfileType = 1;  // 1 = circular.***
                        } else {
                            // is possible that the diameterCmString is a rectangle "width X height" or "2@ width X height".***
                            // check if the 2nd char is "@".***
                            char secondChar = diameterCmString.charAt(1);
                            if (secondChar == 64) // @ = 64.***
                            {
                                // 2@ width X height.***
                                separator = "[@Xx]";
                                String[] diameterCmStringTokens = diameterCmString.split(separator);
                                String widthString = diameterCmStringTokens[1];
                                String heightString = diameterCmStringTokens[2];
                                if (widthString.isEmpty() || heightString.isEmpty()) {
                                    continue;
                                }
                                rectangleWidth = Float.parseFloat(widthString);
                                rectangleHeight = Float.parseFloat(heightString);
                                pipeProfileType = 2; // 2 = rectangular.***
                            } else {
                                separator = "X|x";
                                String[] diameterCmStringTokens = diameterCmString.split(separator);
                                if (diameterCmStringTokens.length == 2) {
                                    String widthString = diameterCmStringTokens[0];
                                    String heightString = diameterCmStringTokens[1];
                                    if (widthString.isEmpty() || heightString.isEmpty()) {
                                        continue;
                                    }
                                    rectangleWidth = Float.parseFloat(widthString);
                                    rectangleHeight = Float.parseFloat(heightString);
                                    pipeProfileType = 2; // 2 = rectangular.***
                                } else {
                                    continue;
                                }
                            }

                            //continue;
                        }
                        // delete the 1rst character of the string.***
                        //diameterCmString = diameterCmString.substring(1, diameterCmString.length());

                        if (pipeProfileType == 2) {
                            // is a rectangular pipe.***
                            // create a pipe with a rectangular profile.***
                            float[] pipeRectangularSize = new float[2];
                            pipeRectangularSize[0] = rectangleWidth;
                            pipeRectangularSize[1] = rectangleHeight;
                            GaiaPipeLineString pipeLineString = GaiaPipeLineString.builder().id(feature.getID()).name(pipeLabel).pipeProfileType(pipeProfileType).pipeRectangularSize(pipeRectangularSize).positions(positions).build();
                            pipeLineString.setOriginalFilePath(file.getPath());
                            pipeLineStrings.add(pipeLineString);
                        } else {
                            // is a circular pipe.***
                            double diameterCm = Double.parseDouble(diameterCmString);
                            // create a pipe with a circular profile.***
                            GaiaPipeLineString pipeLineString = GaiaPipeLineString.builder().id(feature.getID()).name(pipeLabel).pipeProfileType(pipeProfileType).diameterCm(diameterCm).positions(positions).build();
                            pipeLineString.setOriginalFilePath(file.getPath());
                            pipeLineStrings.add(pipeLineString);
                            continue;
                        }
                    } else {
                        log.warn("Invalid Geometry : has no points. : {}", feature.getID());
                    }
                }

                for (Polygon polygon : polygons) {
                    if (!polygon.isValid()) {
                        log.warn("Is Invalid Polygon. : {}", feature.getID());
                        continue;
                    }

                    LineString lineString = polygon.getExteriorRing();
                    GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
                    Coordinate[] outerCoordinates = lineString.getCoordinates();

                    int innerRingCount = polygon.getNumInteriorRing();
                    List<Coordinate[]> innerCoordinates = new ArrayList<>();
                    for (int i = 0; i < innerRingCount; i++) {
                        LineString innerRing = polygon.getInteriorRingN(i);
                        Coordinate[] innerCoordinatesArray = innerRing.getCoordinates();
                        innerCoordinates.add(innerCoordinatesArray);
                    }

                    outerCoordinates = innerRingRemover.removeAll(outerCoordinates, innerCoordinates);
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
                        if (crs != null && !crs.getName().equals("EPSG:4326")) {
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
                        GaiaExtrusionBuilding building = GaiaExtrusionBuilding.builder().id(feature.getID()).name(name).boundingBox(boundingBox).floorHeight(altitude).roofHeight(height + skirtHeight).positions(positions).build();
                        buildings.add(building);
                    } else {
                        String name = getAttribute(feature, nameColumnName);
                        log.warn("Invalid Geometry : {}, {}", feature.getID(), name);
                    }
                }
                counterAux++;
            }
            iterator.close();
            reader.close();
            shpFiles.dispose();
            dataStore.dispose();


            this.convertPipeLineStrings(pipeLineStrings, scenes);


            // check if there are buildings.***
            for (GaiaExtrusionBuilding building : buildings) {
                GaiaScene scene = initScene();
                scene.setOriginalPath(file.toPath());

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
                    localPositions.add(new Vector3dOnlyHashEquals(localPosition));
                }
                Collections.reverse(localPositions);
                localPositions.remove(localPositions.size() - 1);

                List<GaiaExtrusionSurface> extrusionSurfaces = gaiaExtruder.extrude(localPositions, building.getRoofHeight(), building.getFloorHeight());

                GaiaNode node = new GaiaNode();
                node.setTransformMatrix(new Matrix4d().identity());
                GaiaMesh mesh = new GaiaMesh();
                node.getMeshes().add(mesh);

                GaiaPrimitive primitive = createPrimitiveFromGaiaExtrusionSurfaces(extrusionSurfaces);

                primitive.setMaterialIndex(0);
                mesh.getPrimitives().add(primitive);

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

    private void convertPipeLineStrings(List<GaiaPipeLineString> pipeLineStrings, List<GaiaScene> resultScenes) {
        if (pipeLineStrings.isEmpty()) {
            return;
        }

        Modeler3D modeler3d = new Modeler3D();
        modeler3d.concatenateGaiaPipeLines(pipeLineStrings);

        GlobalOptions globalOptions = GlobalOptions.getInstance();

        int pipeLineStringsCount = pipeLineStrings.size();
        for (GaiaPipeLineString pipeLineString : pipeLineStrings) {
            int pointsCount = pipeLineString.getPositions().size();
            pipeLineString.setBoundingBox(new GaiaBoundingBox());
            GaiaBoundingBox bbox = pipeLineString.getBoundingBox();
            bbox.setInit(false);
            for (int j = 0; j < pointsCount; j++) {
                Vector3d point = pipeLineString.getPositions().get(j);
                //Vector3d position = new Vector3d(x, y, z);
                CoordinateReferenceSystem crs = globalOptions.getCrs();
                if (crs != null && !crs.getName().equals("EPSG:4326")) {
                    ProjCoordinate projCoordinate = new ProjCoordinate(point.x, point.y, point.z);
                    ProjCoordinate centerWgs84 = GlobeUtils.transform(crs, projCoordinate);
                    point.set(centerWgs84.x, centerWgs84.y, point.z);
                    bbox.addPoint(point);
                    //position = new Vector3d(centerWgs84.x, centerWgs84.y, point.z);
                }
            }
        }


        for (GaiaPipeLineString pipeLineString : pipeLineStrings) {
//          if (i >= 4000)
//          {
//               break;
//          }
            GaiaScene scene = initScene(); // here creates materials.***
            GaiaMaterial mat = scene.getMaterials().get(0);
            // create random color4.***
            Vector4d randomColor = new Vector4d(Math.random(), Math.random(), Math.random(), 1.0);
//            Vector4d waterColor = new Vector4d(0.25, 0.5, 1.0, 1.0);
//            Vector4d dirtyColor = new Vector4d(0.9, 0.5, 0.25, 1.0);
//            Vector4d yellow = new Vector4d(0.8, 0.8, 0.0, 1.0);
//            Vector4d red = new Vector4d(0.8, 0.01, 0.0, 1.0);
//            Vector4d green = new Vector4d(0.01, 0.8, 0.0, 1.0);
            mat.setDiffuseColor(randomColor);
            Path path = new File(pipeLineString.getOriginalFilePath()).toPath();
            scene.setOriginalPath(path);

            GaiaNode rootNode = scene.getNodes().get(0);
            rootNode.setName("PipeLineStrings");

            GaiaBoundingBox boundingBox = pipeLineString.getBoundingBox();
            Vector3d bboxCenter = boundingBox.getCenter();

            Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(bboxCenter);
            Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerWorldCoordinate);
            Matrix4d transformMatrixInv = new Matrix4d(transformMatrix).invert();

            List<Vector3d> localPositions = new ArrayList<>();
            for (Vector3d position : pipeLineString.getPositions()) {
                Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                Vector3d localPosition = positionWorldCoordinate.mulPosition(transformMatrixInv);
                localPositions.add(new Vector3dOnlyHashEquals(localPosition));
            }

            // set the positions in the pipeLineString.***
            pipeLineString.setPositions(localPositions);

            //pipeLineString.TEST_Check();
            pipeLineString.deleteDuplicatedPoints();

            GaiaNode node = createPrimitiveFromPipeLineString(pipeLineString);
            node.setName(pipeLineString.getName());
            node.setTransformMatrix(new Matrix4d().identity());

            // for all primitives set the material index.***
            for (GaiaMesh mesh : node.getMeshes()) {
                for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                    primitive.setMaterialIndex(0);
                }
            }

            rootNode.getChildren().add(node);
            Matrix4d rootTransformMatrix = new Matrix4d().identity();
            rootTransformMatrix.translate(bboxCenter, rootTransformMatrix);
            rootNode.setTransformMatrix(rootTransformMatrix);
            resultScenes.add(scene);
        }

    }

}
