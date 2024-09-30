package com.gaia3d.converter.geometry.geojson;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.tessellator.GaiaExtruder;
import com.gaia3d.basic.geometry.tessellator.GaiaExtrusionSurface;
import com.gaia3d.basic.structure.*;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.EasySceneCreator;
import com.gaia3d.converter.geometry.*;
import com.gaia3d.util.GlobeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.jts.geom.*;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class GeoJsonSurfaceConverter extends AbstractGeometryConverter implements Converter {

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
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        boolean flipCoordinate = globalOptions.isFlipCoordinate();
        String nameColumnName = globalOptions.getNameColumn();
        List<List<GaiaBuildingSurface>> buildingSurfacesList = new ArrayList<>();
        try {
            FeatureJSON gjson = new FeatureJSON();
            String json = Files.readString(file.toPath());

            SimpleFeatureCollection featureCollection = (SimpleFeatureCollection) gjson.readFeatureCollection(new StringReader(json));
            FeatureIterator<SimpleFeature> iterator = featureCollection.features();

            while (iterator.hasNext()) {
                List<GaiaBuildingSurface> buildingSurfaces = new ArrayList<>();
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
                    attributes.put(attributeDescriptor.getName().getLocalPart(), attributeString);
                });


                for (Polygon polygon : polygons) {
                    log.debug("Polygon : {}", polygon);
                    LineString lineString = polygon.getExteriorRing();
                    Coordinate[] outerCoordinates = lineString.getCoordinates();

                    int interiorRingLength = polygon.getNumInteriorRing();
                    List<List<Vector3d>> vec3InteriorPolygons = new ArrayList<>();
                    for (int i = 0; i < interiorRingLength; i++) {
                        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
                        List<Vector3d> positions = new ArrayList<>();
                        LineString interiorRingLineString= polygon.getInteriorRingN(i);
                        Coordinate[] interiorCoordinates = interiorRingLineString.getCoordinates();

                        for (Coordinate coordinate : interiorCoordinates) {
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
                                position = new Vector3d(centerWgs84.x, centerWgs84.y, z);
                            } else {
                                position = new Vector3d(x, y, z);
                            }
                            positions.add(position);
                            boundingBox.addPoint(position);
                        }
                        vec3InteriorPolygons.add(positions);
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
                        CoordinateReferenceSystem crs = globalOptions.getCrs();
                        if (crs != null && !crs.getName().equals("EPSG:4326")) {
                            ProjCoordinate projCoordinate = new ProjCoordinate(x, y, boundingBox.getMinZ());
                            ProjCoordinate centerWgs84 = GlobeUtils.transform(crs, projCoordinate);
                            position = new Vector3d(centerWgs84.x, centerWgs84.y, z);
                        } else {
                            position = new Vector3d(x, y, z);
                        }
                        positions.add(position);
                        boundingBox.addPoint(position);
                    }

                    String name = getAttributeValueOfDefault(feature, nameColumnName, "Building-Surface");
                    if (positions.size() >= 3) {
                        GaiaBuildingSurface buildingSurface = GaiaBuildingSurface.builder()
                                .id(feature.getID())
                                .name(name)
                                .boundingBox(boundingBox)
                                .exteriorPositions(positions)
                                .interiorPositions(vec3InteriorPolygons)
                                .properties(attributes)
                                .build();
                        buildingSurfaces.add(buildingSurface);
                    } else {
                        log.warn("Invalid Geometry : {}, {}", feature.getID(), name);
                    }
                }
                buildingSurfacesList.add(buildingSurfaces);
            }
            iterator.close();
        } catch (IOException e) {
            log.error("Failed to read GeoJSON file : {}", file.getAbsolutePath(), e);
            throw new RuntimeException(e);
        }


        EasySceneCreator easySceneCreator = new EasySceneCreator();
        for (List<GaiaBuildingSurface> surfaces : buildingSurfacesList) {
            if (surfaces.isEmpty()) {
                continue;
            }

            GaiaScene scene = easySceneCreator.createScene(file);
            GaiaNode rootNode = scene.getNodes().get(0);

            GaiaBuildingSurface firstSurface = surfaces.get(0);
            GaiaAttribute gaiaAttribute = scene.getAttribute();
            gaiaAttribute.setAttributes(firstSurface.getProperties());
            Map<String, String> attributes = gaiaAttribute.getAttributes();
            gaiaAttribute.setNodeName(rootNode.getName());
            attributes.put("name", firstSurface.getName());

            GaiaBoundingBox globalBoundingBox = new GaiaBoundingBox();
            for (GaiaBuildingSurface buildingSurface : surfaces) {
                GaiaBoundingBox localBoundingBox = buildingSurface.getBoundingBox();
                globalBoundingBox.addBoundingBox(localBoundingBox);
            }

            Vector3d center = globalBoundingBox.getCenter();
            Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
            Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerWorldCoordinate);
            Matrix4d transformMatrixInv = new Matrix4d(transformMatrix).invert();

            for (GaiaBuildingSurface buildingSurface : surfaces) {
                GaiaMaterial material = scene.getMaterials().get(0);

                // Has holes.***
                List<Vector3d> ExteriorPolygon = buildingSurface.getExteriorPositions();
                Collections.reverse(ExteriorPolygon);

                List<List<Vector3d>> interiorPolygons = buildingSurface.getInteriorPositions();

                // convert points to local coordinates.***
                List<Vector3d> ExteriorPolygonLocal = new ArrayList<>();
                for (Vector3d position : ExteriorPolygon) {
                    Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                    Vector3d localPosition = positionWorldCoordinate.mulPosition(transformMatrixInv);
                    ExteriorPolygonLocal.add(localPosition);
                }

                // interior points.***
                List<List<Vector3d>> interiorPolygonsLocal = new ArrayList<>();
                for(List<Vector3d> interiorPolygon : interiorPolygons)
                {
                    List<Vector3d> interiorPolygonLocal = new ArrayList<>();
                    for (Vector3d position : interiorPolygon) {
                        Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                        Vector3d localPosition = positionWorldCoordinate.mulPosition(transformMatrixInv);
                        interiorPolygonLocal.add(localPosition);
                    }
                    interiorPolygonsLocal.add(interiorPolygonLocal);
                }
                GaiaPrimitive primitive = createSurfaceFromExteriorAndInteriorPolygons(ExteriorPolygonLocal, interiorPolygonsLocal);

                GaiaNode node = new GaiaNode();
                node.setTransformMatrix(new Matrix4d().identity());
                GaiaMesh mesh = new GaiaMesh();
                node.getMeshes().add(mesh);

                primitive.setMaterialIndex(material.getId());
                mesh.getPrimitives().add(primitive);
                rootNode.getChildren().add(node);
            }

            Matrix4d rootTransformMatrix = new Matrix4d().identity();
            rootTransformMatrix.translate(center, rootTransformMatrix);
            rootNode.setTransformMatrix(rootTransformMatrix);
            scenes.add(scene);
        }

        return scenes;
    }
}
