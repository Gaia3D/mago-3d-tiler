package com.gaia3d.converter.geometry.geojson;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.tessellator.GaiaExtruder;
import com.gaia3d.basic.geometry.tessellator.GaiaExtrusionSurface;
import com.gaia3d.basic.geometry.tessellator.Vector3dOnlyHashEquals;
import com.gaia3d.basic.model.*;
import com.gaia3d.command.mago.AttributeFilter;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.DefaultSceneFactory;
import com.gaia3d.converter.geometry.AbstractGeometryConverter;
import com.gaia3d.converter.geometry.GaiaExtrusionModel;
import com.gaia3d.converter.geometry.GaiaSceneTempGroup;
import com.gaia3d.converter.geometry.InnerRingRemover;
import com.gaia3d.converter.geometry.pipe.GaiaPipeLineString;
import com.gaia3d.converter.geometry.pipe.PipeType;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class GeoJsonConverter extends AbstractGeometryConverter implements Converter {

    private final GlobalOptions globalOptions = GlobalOptions.getInstance();

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
    public List<GaiaSceneTempGroup> convertTemp(File input, File output) {
        List<GaiaSceneTempGroup> sceneTemps = new ArrayList<>();
        GaiaExtruder gaiaExtruder = new GaiaExtruder();
        InnerRingRemover innerRingRemover = new InnerRingRemover();

        List<AttributeFilter> attributeFilters = globalOptions.getAttributeFilters();
        boolean isDefaultCrs = globalOptions.getCrs().equals(GlobalOptions.DEFAULT_CRS);
        boolean flipCoordinate = globalOptions.isFlipCoordinate();
        String heightColumnName = globalOptions.getHeightColumn();
        String altitudeColumnName = globalOptions.getAltitudeColumn();
        String diameterColumnName = globalOptions.getDiameterColumn();
        String scaleColumnName = globalOptions.getScaleColumn();

        double absoluteAltitudeValue = globalOptions.getAbsoluteAltitude();
        double minimumHeightValue = globalOptions.getMinimumHeight();
        double skirtHeight = globalOptions.getSkirtHeight();

        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(Files.newInputStream(input.toPath()))) {
            FeatureJSON geojson = new FeatureJSON();
            log.info("[Load] Reading GeoJSON file : {}", input.getAbsolutePath());
            SimpleFeatureCollection featureCollection = (SimpleFeatureCollection) geojson.readFeatureCollection(bufferedInputStream);
            FeatureIterator<SimpleFeature> iterator = featureCollection.features();
            log.info("[Load] Reading GeoJSON file : {} done", input.getAbsolutePath());

            var coordinateReferenceSystem = featureCollection.getSchema().getCoordinateReferenceSystem();
            if (isDefaultCrs && coordinateReferenceSystem != null) {
                CoordinateReferenceSystem crs = GlobeUtils.convertProj4jCrsFromGeotoolsCrs(coordinateReferenceSystem);
                log.info(" - Coordinate Reference System : {}", crs.getName());
                globalOptions.setCrs(crs);
            }

            List<GaiaExtrusionModel> buildings = new ArrayList<>();
            List<GaiaPipeLineString> pipeLineStrings = new ArrayList<>();
            while (iterator.hasNext()) {
                SimpleFeatureImpl feature = (SimpleFeatureImpl) iterator.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();

                if (geom == null) {
                    log.debug("Is Null Geometry : {}", feature.getID());
                    continue;
                }

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

                List<Polygon> polygons = new ArrayList<>();
                List<LineString> lineStrings = new ArrayList<>();
                if (geom instanceof MultiPolygon) {
                    int count = geom.getNumGeometries();
                    for (int i = 0; i < count; i++) {
                        Polygon polygon = (Polygon) geom.getGeometryN(i);
                        polygons.add(polygon);
                    }
                } else if (geom instanceof Polygon) {
                    polygons.add((Polygon) geom);
                } else if (geom instanceof LineString) {
                    lineStrings.add((LineString) geom);
                } else if (geom instanceof MultiLineString) {
                    int count = geom.getNumGeometries();
                    for (int i = 0; i < count; i++) {
                        LineString lineString = (LineString) geom.getGeometryN(i);
                        lineStrings.add(lineString);
                    }
                }else {
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

                for (LineString lineString : lineStrings) {
                    Coordinate[] coordinates = lineString.getCoordinates();
                    List<Vector3d> positions = new ArrayList<>();
                    if (coordinates.length < 2) {
                        log.warn("[WARN] Invalid LineString : {}", feature.getID());
                        continue;
                    }
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
                        if (Double.isNaN(z) || Double.isInfinite(z)) {
                            z = 0.0d;
                        }

                        Vector3d position = new Vector3d(x, y, z); // usually crs 3857
                        positions.add(position);
                    }
                    double diameter = getDiameter(feature, diameterColumnName);

                    GaiaPipeLineString pipeLineString = GaiaPipeLineString.builder().id(feature.getID()).profileType(PipeType.CIRCULAR).diameter(diameter).properties(attributes).positions(positions).build();
                    pipeLineString.setOriginalFilePath(input.getPath());
                    pipeLineStrings.add(pipeLineString);
                }

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

                    if (positions.size() >= 3) {
                        double height = getHeight(feature, heightColumnName, minimumHeightValue);
                        double altitude = absoluteAltitudeValue;
                        if (altitudeColumnName != null) {
                            altitude = getAltitude(feature, altitudeColumnName);
                        }

                        /* If the height is less than the altitude, swap the values. */
                        if (height < altitude) {
                            double temp = height;
                            height = altitude;
                            altitude = temp;
                        }

                        GaiaExtrusionModel building = GaiaExtrusionModel.builder()
                                .id(feature.getID())
                                .boundingBox(boundingBox)
                                .floorHeight(altitude)
                                .roofHeight(height + skirtHeight)
                                .positions(positions)
                                .properties(attributes)
                                .build();
                        buildings.add(building);
                    } else {
                        log.warn("[WARN] Invalid Geometry : {}", feature.getID());
                    }
                }
            }
            iterator.close();
            DefaultSceneFactory defaultSceneFactory = new DefaultSceneFactory();

            int sceneCount = 10000;
            List<GaiaScene> scenes = new ArrayList<>();
            for (GaiaExtrusionModel building : buildings) {
                GaiaScene scene = defaultSceneFactory.createScene(input);
                GaiaNode rootNode = scene.getNodes().get(0);

                GaiaAttribute gaiaAttribute = scene.getAttribute();
                gaiaAttribute.setAttributes(building.getProperties());
                gaiaAttribute.setNodeName(rootNode.getName());

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
                    String tempName = UUID.randomUUID() + "_" + input.getName();
                    File tempFile = new File(output, tempName);

                    scenes.forEach((gaiaScene) -> {
                        gaiaScene.setOriginalPath(tempFile.toPath());
                    });
                    log.info("[{}] write temp : {}", tempName, scenes.size());
                    GaiaSceneTempGroup sceneTemp = GaiaSceneTempGroup.builder()
                            .tempScene(scenes)
                            .tempFile(tempFile).build();
                    sceneTemp.minimize(tempFile);
                    sceneTemps.add(sceneTemp);
                    scenes.clear();
                }
            }
            convertPipeLineStrings(pipeLineStrings, sceneTemps, input, output);
            if (!scenes.isEmpty()) {
                String tempName = UUID.randomUUID() + "_" + input.getName();
                File tempFile = new File(output, tempName);

                scenes.forEach((gaiaScene) -> {
                    gaiaScene.setOriginalPath(tempFile.toPath());
                });
                log.info("[{}] write temp : {}", tempName, scenes.size());
                GaiaSceneTempGroup sceneTemp = GaiaSceneTempGroup.builder()
                        .tempScene(scenes)
                        .tempFile(tempFile).build();
                sceneTemp.minimize(tempFile);
                sceneTemps.add(sceneTemp);
            }
        } catch (IOException e) {
             log.error("[ERROR] Failed to read GeoJSON file : {}", input.getAbsolutePath(), e);
            throw new RuntimeException(e);
        }
        return sceneTemps;
    }

    @Override
    protected List<GaiaScene> convert(File file) {
        GaiaSceneTempGroup sceneTemp = GaiaSceneTempGroup.builder()
                .tempFile(file)
                .isMinimized(true)
                .build();
        sceneTemp.maximize();
        return sceneTemp.getTempScene();
    }


    private void convertPipeLineStrings(List<GaiaPipeLineString> pipeLineStrings, List<GaiaSceneTempGroup> sceneTemps, File input, File output) {
        if (pipeLineStrings.isEmpty()) {
            return;
        }

        GlobalOptions globalOptions = GlobalOptions.getInstance();
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

                    double defaultHeight = 2.0;
                    double heightOffset = 0.0;

                    if (pipeLineString.getProfileType() == PipeType.CIRCULAR) {
                        heightOffset = pipeLineString.getDiameter() / 1000 / 2;
                    } else if (pipeLineString.getProfileType() == PipeType.RECTANGULAR) {
                        heightOffset = pipeLineString.getRectangularSize()[1] / 1000 / 2;
                    }

                    point.set(centerWgs84.x, centerWgs84.y, point.z - heightOffset - defaultHeight);
                    bbox.addPoint(point);
                }
            }
        }

        int sceneCount = 1000;
        List<GaiaScene> scenes = new ArrayList<>();

        DefaultSceneFactory defaultSceneFactory = new DefaultSceneFactory();
        for (GaiaPipeLineString pipeLineString : pipeLineStrings) {
            int pointsCount = pipeLineString.getPositions().size();
            if (pointsCount < 2) {
                log.warn("[WARN] Invalid PipeLineString : {}", pipeLineString.getId());
                continue;
            }

            GaiaScene scene = defaultSceneFactory.createScene(input);
            GaiaNode rootNode = scene.getNodes().get(0);
            rootNode.setName("PipeLineStrings");

            GaiaAttribute gaiaAttribute = scene.getAttribute();
            gaiaAttribute.setAttributes(pipeLineString.getProperties());
            Map<String, String> attributes = gaiaAttribute.getAttributes();
            gaiaAttribute.setNodeName(rootNode.getName());
            attributes.put("name", pipeLineString.getName());

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

            // set the positions in the pipeLineString
            pipeLineString.setPositions(localPositions);

            //pipeLineString.TEST_Check();
            if (localPositions.size() > 2) {
                pipeLineString.deleteDuplicatedPoints();
            }

            // once deleted duplicatedPoints, check pointsCount again
            pointsCount = pipeLineString.getPositions().size();
            if (pointsCount < 2) {
                log.warn("[WARN] Invalid PipeLineString POINTS COUNT LESS THAN 2: {}", pipeLineString.getId());
                continue;
            }

            GaiaNode node = createPrimitiveFromPipeLineString(pipeLineString);
            if (node == null) {
                log.warn("[WARN] Invalid PipeLineString NULL NODE: {}", pipeLineString.getId());
                continue;
            }
            node.setName(pipeLineString.getName());
            node.setTransformMatrix(new Matrix4d().identity());

            // for all primitives set the material index
            for (GaiaMesh mesh : node.getMeshes()) {
                for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                    primitive.setMaterialIndex(0);
                }
            }

            rootNode.getChildren().add(node);
            Matrix4d rootTransformMatrix = new Matrix4d().identity();
            rootTransformMatrix.translate(bboxCenter, rootTransformMatrix);
            rootNode.setTransformMatrix(rootTransformMatrix);

            if (rootNode.getChildren().size() <= 0) {
                log.debug("Invalid Scene : {}", rootNode.getName());
                continue;
            }
            //resultScenes.add(scene);

            //resultScenes.add(scene);
            scenes.add(scene);
            if (scenes.size() >= sceneCount) {
                String tempName = UUID.randomUUID() + input.getName();
                File tempFile = new File(output, tempName);
                GaiaSceneTempGroup sceneTemp = GaiaSceneTempGroup.builder()
                        .tempScene(scenes)
                        .tempFile(tempFile).build();
                sceneTemp.minimize(tempFile);
                sceneTemps.add(sceneTemp);
                scenes.clear();
            }
        }
        if (!scenes.isEmpty()) {
            String tempName = UUID.randomUUID() + input.getName();
            File tempFile = new File(output, tempName);
            GaiaSceneTempGroup sceneTemp = GaiaSceneTempGroup.builder()
                    .tempScene(scenes)
                    .tempFile(tempFile).build();
            sceneTemp.minimize(tempFile);
            sceneTemps.add(sceneTemp);
        }
    }
}
