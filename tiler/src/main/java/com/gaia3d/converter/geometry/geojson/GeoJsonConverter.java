package com.gaia3d.converter.geometry.geojson;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.tessellator.GaiaExtruder;
import com.gaia3d.basic.geometry.tessellator.GaiaExtrusionSurface;
import com.gaia3d.basic.geometry.tessellator.Vector3dOnlyHashEquals;
import com.gaia3d.basic.model.*;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.EasySceneCreator;
import com.gaia3d.converter.geometry.AbstractGeometryConverter;
import com.gaia3d.converter.geometry.GaiaExtrusionBuilding;
import com.gaia3d.converter.geometry.GaiaSceneTempHolder;
import com.gaia3d.converter.geometry.InnerRingRemover;
import com.gaia3d.util.GlobeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.geojson.feature.FeatureJSON;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.jts.geom.*;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class GeoJsonConverter extends AbstractGeometryConverter implements Converter {

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
    public List<GaiaSceneTempHolder> convertTemp(File input, File output) {
        //String tempName = UUID.randomUUID().toString() + ".scene";
        //File tempFile = new File(input, name);
        //List<GaiaScene> scenes = new ArrayList<>();
        List<GaiaSceneTempHolder> sceneTemps = new ArrayList<>();
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

        try {
            FeatureJSON gjson = new FeatureJSON();
            //String json = Files.readString(input.toPath());
            //SimpleFeatureCollection featureCollection = (SimpleFeatureCollection) gjson.readFeatureCollection(new StringReader(json));

            log.info("Reading GeoJSON file : {}", input.getAbsolutePath());
            BufferedInputStream bufferedInputStream = new BufferedInputStream(Files.newInputStream(input.toPath()));
            SimpleFeatureCollection featureCollection = (SimpleFeatureCollection) gjson.readFeatureCollection(bufferedInputStream);
            FeatureIterator<SimpleFeature> iterator = featureCollection.features();
            log.info("Reading GeoJSON file : {} done", input.getAbsolutePath());

            List<GaiaExtrusionBuilding> buildings = new ArrayList<>();
            while (iterator.hasNext()) {
                SimpleFeatureImpl feature = (SimpleFeatureImpl) iterator.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();

                if (geom == null) {
                    log.debug("Is Null Geometry : {}", feature.getID());
                    continue;
                }

                List<Polygon> polygons = new ArrayList<>();
                if (geom instanceof MultiPolygon) {
                    int count = geom.getNumGeometries();
                    for (int i = 0; i < count; i++) {
                        Polygon polygon = (Polygon) geom.getGeometryN(i);
                        polygons.add(polygon);
                    }
                } else if (geom instanceof Polygon) {
                    polygons.add((Polygon) geom);
                } else {
                    log.debug("Is Not Supported Geometry Type : {}", geom.getGeometryType());
                    continue;
                }

                Map<String, String> attributes = new HashMap<>();
                FeatureType featureType = feature.getFeatureType();
                Collection<PropertyDescriptor> featureDescriptors = featureType.getDescriptors();
                AtomicInteger index = new AtomicInteger(0);
                featureDescriptors.forEach(attributeDescriptor -> {
                    Object attribute = feature.getAttribute(index.getAndIncrement());
                    if (attribute instanceof Geometry) {
                        return;
                    }
                    String attributeString = castStringFromObject(attribute, "Null");
                    //log.debug("{} : {}", attributeDescriptor.getName(), attributeString);
                    attributes.put(attributeDescriptor.getName().getLocalPart(), attributeString);
                });


                for (Polygon polygon : polygons) {
                    if (!polygon.isValid()) {
                        log.debug("Is Invalid Polygon. : {}", feature.getID());
                        continue;
                    }

                    LineString lineString = polygon.getExteriorRing();
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
                        double x, y, z;
                        if (flipCoordinate) {
                            x = coordinate.getY();
                            y = coordinate.getX();
                        } else {
                            x = coordinate.getX();
                            y = coordinate.getY();
                        }
                        z = coordinate.getZ();

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

                    String name = getAttributeValueOfDefault(feature, nameColumnName, "Extrusion-Building");
                    if (positions.size() >= 3) {
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
                                .properties(attributes)
                                .build();
                        buildings.add(building);
                    } else {
                        log.warn("Invalid Geometry : {}, {}", feature.getID(), name);
                    }
                }
            }
            iterator.close();

            EasySceneCreator easySceneCreator = new EasySceneCreator();

            int sceneCount = 1000;
            List<GaiaScene> scenes = new ArrayList<>();
            for (GaiaExtrusionBuilding building : buildings) {
                GaiaScene scene = easySceneCreator.createScene(input);
                GaiaNode rootNode = scene.getNodes().get(0);

                GaiaAttribute gaiaAttribute = scene.getAttribute();
                gaiaAttribute.setAttributes(building.getProperties());
                Map<String, String> attributes = gaiaAttribute.getAttributes();
                gaiaAttribute.setNodeName(rootNode.getName());
                attributes.put("name", building.getName());

                Vector3d center = building.getBoundingBox().getCenter();
                center.z = center.z - skirtHeight;

                Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
                Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerWorldCoordinate);
                Matrix4d transformMatrixInv = new Matrix4d(transformMatrix).invert();

                List<Vector3d> localPositions = new ArrayList<>();
                for (Vector3d position : building.getPositions()) {
                    Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                    Vector3d localPosition = positionWorldCoordinate.mulPosition(transformMatrixInv);
                    localPosition.z = 0.0d;
                    localPositions.add(new Vector3dOnlyHashEquals(localPosition));
                }
                Collections.reverse(localPositions);

                List<GaiaExtrusionSurface> extrusionSurfaces = gaiaExtruder.extrude(localPositions, building.getRoofHeight(), building.getFloorHeight());

                GaiaNode node = new GaiaNode();
                node.setTransformMatrix(new Matrix4d().identity());
                GaiaMesh mesh = new GaiaMesh();
                node.getMeshes().add(mesh);

                GaiaPrimitive primitive = createPrimitiveFromGaiaExtrusionSurfaces(extrusionSurfaces);
                if (primitive.getSurfaces().isEmpty() || primitive.getVertices().size() < 3) {
                    log.debug("Invalid Geometry : {}", building.getId());
                    log.debug("Vertices count : {}", primitive.getVertices().size());
                    log.debug("Surfaces count : {}", primitive.getSurfaces().size());
                    continue;
                }

                primitive.setMaterialIndex(0);
                mesh.getPrimitives().add(primitive);

                rootNode.getChildren().add(node);

                Matrix4d rootTransformMatrix = new Matrix4d().identity();
                rootTransformMatrix.translate(center, rootTransformMatrix);
                rootNode.setTransformMatrix(rootTransformMatrix);

                if (rootNode.getChildren().size() <= 0) {
                    log.debug("Invalid Scene : {}", rootNode.getName());
                    continue;
                }

                scenes.add(scene);
                if (scenes.size() >= sceneCount) {
                    String tempName = UUID.randomUUID().toString() + input.getName();
                    File tempFile = new File(output, tempName);
                    GaiaSceneTempHolder sceneTemp = GaiaSceneTempHolder.builder()
                            .tempScene(scenes)
                            .tempFile(tempFile).build();
                    sceneTemp.minimize(tempFile);
                    sceneTemps.add(sceneTemp);
                    scenes.clear();
                }
            }
            if (!scenes.isEmpty()) {
                String tempName = UUID.randomUUID().toString() + input.getName();
                File tempFile = new File(output, tempName);
                GaiaSceneTempHolder sceneTemp = GaiaSceneTempHolder.builder()
                        .tempScene(scenes)
                        .tempFile(tempFile).build();
                sceneTemp.minimize(tempFile);
                sceneTemps.add(sceneTemp);
                scenes.clear();
            }
        } catch (IOException e) {
            log.error("Failed to read GeoJSON file : {}", input.getAbsolutePath(), e);
            throw new RuntimeException(e);
        }
        return sceneTemps;
    }

    @Override
    protected List<GaiaScene> convert(File file) {
        GaiaSceneTempHolder sceneTemp = GaiaSceneTempHolder.builder()
                .tempFile(file)
                .isMinimized(true)
                .build();
        sceneTemp.maximize();
        List<GaiaScene> scenes = sceneTemp.getTempScene();



        /*GaiaExtruder gaiaExtruder = new GaiaExtruder();
        InnerRingRemover innerRingRemover = new InnerRingRemover();

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        boolean flipCoordinate = globalOptions.isFlipCoordinate();
        String nameColumnName = globalOptions.getNameColumn();
        String heightColumnName = globalOptions.getHeightColumn();
        String altitudeColumnName = globalOptions.getAltitudeColumn();

        double absoluteAltitudeValue = globalOptions.getAbsoluteAltitude();
        double minimumHeightValue = globalOptions.getMinimumHeight();
        double skirtHeight = globalOptions.getSkirtHeight();

        try {
            FeatureJSON gjson = new FeatureJSON();
            String json = Files.readString(file.toPath());

            SimpleFeatureCollection featureCollection = (SimpleFeatureCollection) gjson.readFeatureCollection(new StringReader(json));
            FeatureIterator<SimpleFeature> iterator = featureCollection.features();

            List<GaiaExtrusionBuilding> buildings = new ArrayList<>();
            while (iterator.hasNext()) {
                SimpleFeatureImpl feature = (SimpleFeatureImpl) iterator.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();

                if (geom == null) {
                    log.debug("Is Null Geometry : {}", feature.getID());
                    continue;
                }

                List<Polygon> polygons = new ArrayList<>();
                if (geom instanceof MultiPolygon) {
                    int count = geom.getNumGeometries();
                    for (int i = 0; i < count; i++) {
                        Polygon polygon = (Polygon) geom.getGeometryN(i);
                        polygons.add(polygon);
                    }
                } else if (geom instanceof Polygon) {
                    polygons.add((Polygon) geom);
                } else {
                    log.debug("Is Not Supported Geometry Type : {}", geom.getGeometryType());
                    continue;
                }

                Map<String, String> attributes = new HashMap<>();
                FeatureType featureType = feature.getFeatureType();
                Collection<PropertyDescriptor> featureDescriptors = featureType.getDescriptors();
                AtomicInteger index = new AtomicInteger(0);
                featureDescriptors.forEach(attributeDescriptor -> {
                    Object attribute = feature.getAttribute(index.getAndIncrement());
                    if (attribute instanceof Geometry) {
                        return;
                    }
                    String attributeString = castStringFromObject(attribute, "Null");
                    //log.debug("{} : {}", attributeDescriptor.getName(), attributeString);
                    attributes.put(attributeDescriptor.getName().getLocalPart(), attributeString);
                });


                for (Polygon polygon : polygons) {
                    if (!polygon.isValid()) {
                        log.debug("Is Invalid Polygon. : {}", feature.getID());
                        continue;
                    }

                    LineString lineString = polygon.getExteriorRing();
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
                        double x, y, z;
                        if (flipCoordinate) {
                            x = coordinate.getY();
                            y = coordinate.getX();
                        } else {
                            x = coordinate.getX();
                            y = coordinate.getY();
                        }
                        z = coordinate.getZ();

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

                    String name = getAttributeValueOfDefault(feature, nameColumnName, "Extrusion-Building");
                    if (positions.size() >= 3) {
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
                                .properties(attributes)
                                .build();
                        buildings.add(building);
                    } else {
                        log.warn("Invalid Geometry : {}, {}", feature.getID(), name);
                    }
                }
            }
            iterator.close();

            EasySceneCreator easySceneCreator = new EasySceneCreator();
            for (GaiaExtrusionBuilding building : buildings) {
                GaiaScene scene = easySceneCreator.createScene(file);
                GaiaNode rootNode = scene.getNodes().get(0);

                GaiaAttribute gaiaAttribute = scene.getAttribute();
                gaiaAttribute.setAttributes(building.getProperties());
                Map<String, String> attributes = gaiaAttribute.getAttributes();
                gaiaAttribute.setNodeName(rootNode.getName());
                attributes.put("name", building.getName());

                Vector3d center = building.getBoundingBox().getCenter();
                center.z = center.z - skirtHeight;

                Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
                Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerWorldCoordinate);
                Matrix4d transformMatrixInv = new Matrix4d(transformMatrix).invert();

                List<Vector3d> localPositions = new ArrayList<>();
                for (Vector3d position : building.getPositions()) {
                    Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                    Vector3d localPosition = positionWorldCoordinate.mulPosition(transformMatrixInv);
                    localPosition.z = 0.0d;
                    localPositions.add(new Vector3dOnlyHashEquals(localPosition));
                }
                Collections.reverse(localPositions);

                List<GaiaExtrusionSurface> extrusionSurfaces = gaiaExtruder.extrude(localPositions, building.getRoofHeight(), building.getFloorHeight());

                GaiaNode node = new GaiaNode();
                node.setTransformMatrix(new Matrix4d().identity());
                GaiaMesh mesh = new GaiaMesh();
                node.getMeshes().add(mesh);

                GaiaPrimitive primitive = createPrimitiveFromGaiaExtrusionSurfaces(extrusionSurfaces);
                if (primitive.getSurfaces().isEmpty() || primitive.getVertices().size() < 3) {
                    log.debug("Invalid Geometry : {}", building.getId());
                    continue;
                }

                primitive.setMaterialIndex(0);
                mesh.getPrimitives().add(primitive);

                rootNode.getChildren().add(node);

                Matrix4d rootTransformMatrix = new Matrix4d().identity();
                rootTransformMatrix.translate(center, rootTransformMatrix);
                rootNode.setTransformMatrix(rootTransformMatrix);

                if (rootNode.getChildren().size() <= 0) {
                    log.debug("Invalid Scene : {}", rootNode.getName());
                    continue;
                }
                scenes.add(scene);
            }
        } catch (IOException e) {
            log.error("Failed to read GeoJSON file : {}", file.getAbsolutePath(), e);
            throw new RuntimeException(e);
        }*/
        return scenes;
    }
}
