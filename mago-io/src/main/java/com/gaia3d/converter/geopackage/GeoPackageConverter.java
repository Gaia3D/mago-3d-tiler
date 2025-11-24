package com.gaia3d.converter.geopackage;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.modifier.DefaultSceneFactory;
import com.gaia3d.basic.geometry.parametric.GaiaExtrusionModel;
import com.gaia3d.basic.geometry.tessellator.GaiaExtruder;
import com.gaia3d.basic.geometry.tessellator.GaiaExtrusionSurface;
import com.gaia3d.basic.geometry.tessellator.Vector3dOnlyHashEquals;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.pipe.GaiaPipeLineString;
import com.gaia3d.basic.pipe.PipeType;
import com.gaia3d.basic.temp.GaiaSceneTempGroup;
import com.gaia3d.converter.*;
import com.gaia3d.util.GlobeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.data.SimpleFeatureReader;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.geometry.jts.Geometries;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.jts.geom.*;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GeoPackage Converter
 */
@Slf4j
@RequiredArgsConstructor
public class GeoPackageConverter extends AbstractGeometryConverter implements Converter {

    private final Parametric3DOptions parametricOptions;

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
        InnerRingRemover innerRingRemover = new InnerRingRemover();

        List<AttributeFilter> attributeFilters = parametricOptions.getAttributeFilters();
        boolean isDefaultCrs = parametricOptions.getSourceCrs().equals(new CRSFactory().createFromName("EPSG:3857"));
        boolean flipCoordinate = parametricOptions.isFlipCoordinate();
        String heightColumnName = parametricOptions.getHeightColumnName();
        String altitudeColumnName = parametricOptions.getAltitudeColumnName();
        String diameterColumnName = parametricOptions.getDiameterColumnName();

        double absoluteAltitudeValue = parametricOptions.getAbsoluteAltitudeValue();
        double minimumHeightValue = parametricOptions.getMinimumHeightValue();
        double skirtHeight = parametricOptions.getSkirtHeight();

        GeoPackage geoPackage = null;
        try {
            geoPackage = new GeoPackage(input);
            List<FeatureEntry> features = geoPackage.features();

            for (FeatureEntry featureEntry : features) {
                Geometries geometryType = featureEntry.getGeometryType();
                log.info("FeatureTableName: {}", featureEntry.getTableName());
                log.info("GeometryType: {}", geometryType.getName());
                log.info("GeometryColumn: {}", featureEntry.getGeometryColumn());
                log.info("SRID: {}", featureEntry.getSrid());
                log.info("TableName: {}", featureEntry.getTableName());
            }

            List<GaiaExtrusionModel> buildings = new ArrayList<>();
            List<GaiaPipeLineString> pipeLineStrings = new ArrayList<>();
            for (FeatureEntry featureEntry : features) {

                var coordinateReferenceSystem = featureEntry.getBounds().getCoordinateReferenceSystem();
                if (isDefaultCrs && coordinateReferenceSystem != null) {
                    CoordinateReferenceSystem crs = GlobeUtils.convertProj4jCrsFromGeotoolsCrs(coordinateReferenceSystem);
                    log.info(" - Coordinate Reference System : {}", crs.getName());
                    parametricOptions.setSourceCrs(crs);
                }

                Filter filter = Filter.INCLUDE;
                Transaction transaction = Transaction.AUTO_COMMIT;
                SimpleFeatureReader simpleFeatureReader = geoPackage.reader(featureEntry, filter, transaction);
                while (simpleFeatureReader.hasNext()) {
                    SimpleFeature feature = simpleFeatureReader.next();
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
                        String attributeString = castStringFromObject(attribute, "null");
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
                            log.warn("[WARN] {} Is Invalid Polygon.", feature.getID());
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
                        if (innerRingCount > 0) {
                            outerCoordinates = innerRingRemover.removeAll(outerCoordinates, innerCoordinates);
                        }

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
                            CoordinateReferenceSystem crs = parametricOptions.getSourceCrs();
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

                            GaiaExtrusionModel building = GaiaExtrusionModel.builder().id(feature.getID()).boundingBox(boundingBox).floorHeight(altitude).roofHeight(height + skirtHeight).positions(positions).originalFilePath(input.getPath()).properties(attributes).build();
                            buildings.add(building);
                        } else {
                            log.warn("[WARN] Invalid Geometry : {}", feature.getID());
                        }
                    }
                }
            }
            convertPipeLineStrings(pipeLineStrings, sceneTemps, input, output);
            convertExtrusionBuildings(buildings, sceneTemps, input, output);

            geoPackage.close();
        } catch (IOException e) {
            if (geoPackage != null) {geoPackage.close();}
            throw new RuntimeException(e);
        }
        return sceneTemps;
    }

    @Override
    protected List<GaiaScene> convert(File file) {
        GaiaSceneTempGroup sceneTemp = GaiaSceneTempGroup.builder().tempFile(file).isMinimized(true).build();
        sceneTemp.maximize();
        return sceneTemp.getTempScene();
    }

    private void convertExtrusionBuildings(List<GaiaExtrusionModel> buildings, List<GaiaSceneTempGroup> sceneTemps, File input, File output) {
        double skirtHeight = parametricOptions.getSkirtHeight();
        GaiaExtruder gaiaExtruder = new GaiaExtruder();

        int sceneCount = 10000;
        List<GaiaScene> scenes = new ArrayList<>();

        DefaultSceneFactory defaultSceneFactory = new DefaultSceneFactory();
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
            localPositions.remove(localPositions.size() - 1);

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
            rootNode.setTransformMatrix(rootTransformMatrix);

            Vector3d degreeTranslation = scene.getTranslation();
            degreeTranslation.set(center);

            scenes.add(scene);
            if (scenes.size() >= sceneCount) {
                String tempName = UUID.randomUUID() + "_" + input.getName();
                File tempFile = new File(output, tempName);

                scenes.forEach((gaiaScene) -> {
                    gaiaScene.setOriginalPath(tempFile.toPath());
                });
                log.info("[{}] write temp : {}", tempName, scenes.size());
                GaiaSceneTempGroup sceneTemp = GaiaSceneTempGroup.builder().tempScene(scenes).tempFile(tempFile).build();
                sceneTemp.minimize(tempFile);
                sceneTemps.add(sceneTemp);
                scenes.clear();
            }
        }
        if (!scenes.isEmpty()) {
            String tempName = UUID.randomUUID() + "_" + input.getName();
            File tempFile = new File(output, tempName);

            scenes.forEach((gaiaScene) -> {
                gaiaScene.setOriginalPath(tempFile.toPath());
            });
            log.info("[{}] write temp : {}", tempName, scenes.size());
            GaiaSceneTempGroup sceneTemp = GaiaSceneTempGroup.builder().tempScene(scenes).tempFile(tempFile).build();
            sceneTemp.minimize(tempFile);
            sceneTemps.add(sceneTemp);
        }
    }

    private void convertPipeLineStrings(List<GaiaPipeLineString> pipeLineStrings, List<GaiaSceneTempGroup> sceneTemps, File input, File output) {
        if (pipeLineStrings.isEmpty()) {
            return;
        }

        for (GaiaPipeLineString pipeLineString : pipeLineStrings) {
            int pointsCount = pipeLineString.getPositions().size();
            pipeLineString.setBoundingBox(new GaiaBoundingBox());
            GaiaBoundingBox bbox = pipeLineString.getBoundingBox();
            bbox.setInit(false);
            for (int j = 0; j < pointsCount; j++) {
                Vector3d point = pipeLineString.getPositions().get(j);
                //Vector3d position = new Vector3d(x, y, z);
                CoordinateReferenceSystem crs = parametricOptions.getSourceCrs();
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
            rootNode.setTransformMatrix(rootTransformMatrix);

            Vector3d degreeTranslation = scene.getTranslation();
            degreeTranslation.set(bboxCenter);

            if (rootNode.getChildren().size() <= 0) {
                log.debug("Invalid Scene : {}", rootNode.getName());
                continue;
            }

            scenes.add(scene);
            if (scenes.size() >= sceneCount) {
                String tempName = UUID.randomUUID() + input.getName();
                File tempFile = new File(output, tempName);
                GaiaSceneTempGroup sceneTemp = GaiaSceneTempGroup.builder().tempScene(scenes).tempFile(tempFile).build();
                sceneTemp.minimize(tempFile);
                sceneTemps.add(sceneTemp);
                scenes.clear();
            }
        }
        if (!scenes.isEmpty()) {
            String tempName = UUID.randomUUID() + input.getName();
            File tempFile = new File(output, tempName);
            GaiaSceneTempGroup sceneTemp = GaiaSceneTempGroup.builder().tempScene(scenes).tempFile(tempFile).build();
            sceneTemp.minimize(tempFile);
            sceneTemps.add(sceneTemp);
        }
    }
}
