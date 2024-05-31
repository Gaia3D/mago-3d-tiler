package com.gaia3d.converter.geometry.geojson;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.tessellator.GaiaExtruder;
import com.gaia3d.basic.geometry.tessellator.GaiaExtrusionSurface;
import com.gaia3d.basic.structure.*;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.geometry.AbstractGeometryConverter;
import com.gaia3d.converter.geometry.GaiaExtrusionBuilding;
import com.gaia3d.converter.geometry.InnerRingRemover;
import com.gaia3d.converter.geometry.Vector3dsOnlyHashEquals;
import com.gaia3d.util.GlobeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.jts.geom.*;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        try {
            FeatureJSON gjson = new FeatureJSON();
            String json = Files.readString(file.toPath());

            SimpleFeatureCollection featureCollection = (SimpleFeatureCollection) gjson.readFeatureCollection(new StringReader(json));
            FeatureIterator<SimpleFeature> iterator = featureCollection.features();

            List<GaiaExtrusionBuilding> buildings = new ArrayList<>();
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
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

                for (Polygon polygon : polygons) {
                    if (!polygon.isValid()) {
                        log.debug("Is Invalid Polygon. : {}", feature.getID());
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
                        String name = getAttributeValue(feature, nameColumnName);
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
                        String name = getAttributeValue(feature, nameColumnName);
                        log.warn("Invalid Geometry : {}, {}", feature.getID(), name);
                    }
                }
            }
            iterator.close();

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
                    localPositions.add(new Vector3dsOnlyHashEquals(localPosition));
                }
                Collections.reverse(localPositions);

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
        } catch (IOException e) {
            log.error("Failed to read GeoJSON file : {}", file.getAbsolutePath(), e);
            throw new RuntimeException(e);
        }
        return scenes;
    }
}
