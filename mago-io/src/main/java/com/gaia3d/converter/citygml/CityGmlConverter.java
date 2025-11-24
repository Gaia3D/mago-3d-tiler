package com.gaia3d.converter.citygml;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.temp.GaiaSceneTempGroup;
import com.gaia3d.basic.types.Classification;
import com.gaia3d.converter.Converter;
import com.gaia3d.basic.geometry.modifier.DefaultSceneFactory;
import com.gaia3d.converter.AbstractGeometryConverter;
import com.gaia3d.basic.geometry.parametric.GaiaSurfaceModel;
import com.gaia3d.converter.Parametric3DOptions;
import com.gaia3d.util.GlobeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.citygml4j.core.model.bridge.Bridge;
import org.citygml4j.core.model.bridge.BridgeFurniture;
import org.citygml4j.core.model.bridge.BridgeInstallation;
import org.citygml4j.core.model.building.*;
import org.citygml4j.core.model.cityfurniture.CityFurniture;
import org.citygml4j.core.model.cityobjectgroup.CityObjectGroup;
import org.citygml4j.core.model.construction.*;
import org.citygml4j.core.model.core.*;
import org.citygml4j.core.model.deprecated.bridge.DeprecatedPropertiesOfAbstractBridge;
import org.citygml4j.core.model.deprecated.transportation.DeprecatedPropertiesOfAbstractTransportationSpace;
import org.citygml4j.core.model.deprecated.vegetation.DeprecatedPropertiesOfPlantCover;
import org.citygml4j.core.model.deprecated.waterbody.DeprecatedPropertiesOfWaterBody;
import org.citygml4j.core.model.generics.GenericOccupiedSpace;
import org.citygml4j.core.model.relief.AbstractReliefComponent;
import org.citygml4j.core.model.relief.AbstractReliefComponentProperty;
import org.citygml4j.core.model.relief.ReliefFeature;
import org.citygml4j.core.model.relief.TINRelief;
import org.citygml4j.core.model.transportation.Railway;
import org.citygml4j.core.model.transportation.Road;
import org.citygml4j.core.model.tunnel.Tunnel;
import org.citygml4j.core.model.tunnel.TunnelFurniture;
import org.citygml4j.core.model.tunnel.TunnelInstallation;
import org.citygml4j.core.model.vegetation.PlantCover;
import org.citygml4j.core.model.vegetation.SolitaryVegetationObject;
import org.citygml4j.core.model.waterbody.WaterBody;
import org.citygml4j.core.model.waterbody.WaterGroundSurface;
import org.citygml4j.core.model.waterbody.WaterSurface;
import org.citygml4j.xml.CityGMLContext;
import org.citygml4j.xml.CityGMLContextException;
import org.citygml4j.xml.reader.CityGMLInputFactory;
import org.citygml4j.xml.reader.CityGMLReadException;
import org.citygml4j.xml.reader.CityGMLReader;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import org.xmlobjects.builder.ObjectBuildException;
import org.xmlobjects.gml.adapter.geometry.primitives.AbstractRingPropertyAdapter;
import org.xmlobjects.gml.model.geometry.*;
import org.xmlobjects.gml.model.geometry.aggregates.MultiSurface;
import org.xmlobjects.gml.model.geometry.aggregates.MultiSurfaceProperty;
import org.xmlobjects.gml.model.geometry.complexes.CompositeSolid;
import org.xmlobjects.gml.model.geometry.complexes.CompositeSurface;
import org.xmlobjects.gml.model.geometry.primitives.*;
import org.xmlobjects.model.Child;

import javax.xml.namespace.QName;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class CityGmlConverter extends AbstractGeometryConverter implements Converter {

    private final Parametric3DOptions options;

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
        return null;
    }

    protected List<GaiaScene> convert(File file) {
        List<GaiaScene> scenes = new ArrayList<>();
        try {
            CityGMLContext context = CityGMLContext.newInstance();
            CityGMLInputFactory factory = context.createCityGMLInputFactory();
            CityGMLReader reader = factory.createCityGMLReader(file);

            while (reader.hasNext()) {
                CityModel cityModel = (CityModel) reader.next();
                List<List<GaiaSurfaceModel>> buildingSurfacesList = new ArrayList<>();
                List<AbstractCityObjectProperty> cityObjectMembers = cityModel.getCityObjectMembers();
                for (AbstractCityObjectProperty cityObjectProperty : cityObjectMembers) {
                    AbstractCityObject cityObject = cityObjectProperty.getObject();

                    List<SolidProperty> solidProperties = extractSolid(cityObject);
                    for (SolidProperty solidProperty : solidProperties) {
                        AbstractSolid solid = solidProperty.getObject();
                        if (solid == null) {
                            log.error("[ERROR] No solid found for city object: {}", cityObject.getId());
                        } else {
                            buildingSurfacesList.add(convertSolidSurfaceProperty(cityObject, solid));
                        }
                    }

                    List<MultiSurfaceProperty> multiSurfaceProperties = extractMultiSurfaceProperty(cityObject);
                    for (MultiSurfaceProperty multiSurfaceProperty : multiSurfaceProperties) {
                        buildingSurfacesList.add(convertMultiSurfaceProperty(cityObject, multiSurfaceProperty));
                    }

                }

                DefaultSceneFactory defaultSceneFactory = new DefaultSceneFactory();

                for (List<GaiaSurfaceModel> surfaces : buildingSurfacesList) {
                    if (surfaces.isEmpty()) {
                        continue;
                    }

                    GaiaScene scene = defaultSceneFactory.createScene(file);
                    GaiaNode rootNode = scene.getNodes().get(0);

                    GaiaAttribute attribute = scene.getAttribute();
                    //attribute.setAttributes(surfaces.getProperties());

                    GaiaBoundingBox globalBoundingBox = new GaiaBoundingBox();
                    for (GaiaSurfaceModel buildingSurface : surfaces) {
                        GaiaBoundingBox localBoundingBox = buildingSurface.getBoundingBox();
                        globalBoundingBox.addBoundingBox(localBoundingBox);
                    }

                    Vector3d center = globalBoundingBox.getCenter();
                    Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
                    Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerWorldCoordinate);
                    Matrix4d transformMatrixInv = new Matrix4d(transformMatrix).invert();

                    CoordinateReferenceSystem crs = options.getSourceCrs();

                    for (GaiaSurfaceModel buildingSurface : surfaces) {
                        GaiaMaterial material = getMaterialByClassification(scene.getMaterials(), buildingSurface.getClassification());

                        // Check if buildingSurface has holes
                        List<List<Vector3d>> interiorPolygons = buildingSurface.getInteriorPositions();
                        boolean hasHoles = interiorPolygons != null && !interiorPolygons.isEmpty();

                        GaiaNode node = new GaiaNode();
                        node.setTransformMatrix(new Matrix4d().identity());
                        GaiaMesh mesh = new GaiaMesh();
                        node.getMeshes().add(mesh);

                        if (!hasHoles) {
                            List<List<Vector3d>> polygons = new ArrayList<>();
                            List<Vector3d> polygon = new ArrayList<>();

                            if (buildingSurface.getExteriorPositions().size() < 3) {
                                log.debug("Invalid Geometry : {}", buildingSurface.getId());
                                continue;
                            }
                            for (Vector3d position : buildingSurface.getExteriorPositions()) {
                                if (crs.getName().equals("EPSG:4978")) {
                                    polygon.add(position);
                                } else {
                                    Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                                    Vector3d localPosition = positionWorldCoordinate.mulPosition(transformMatrixInv);
                                    polygon.add(localPosition);
                                }
                            }
                            polygons.add(polygon);

                            GaiaPrimitive primitive = createPrimitiveFromPolygons(polygons);

                            primitive.setMaterialIndex(material.getId());
                            if (primitive.getSurfaces().isEmpty() || primitive.getVertices().size() < 3) {
                                log.debug("Invalid Geometry : {}", buildingSurface.getId());
                                log.debug("Vertices count : {}", primitive.getVertices().size());
                                log.debug("Surfaces count : {}", primitive.getSurfaces().size());
                                continue;
                            }
                            mesh.getPrimitives().add(primitive);
                            rootNode.getChildren().add(node);
                        } else {
                            // Has holes
                            List<Vector3d> ExteriorPolygon = buildingSurface.getExteriorPositions();

                            // convert points to local coordinates
                            List<Vector3d> exteriorPolygonLocal = new ArrayList<>();
                            for (Vector3d position : ExteriorPolygon) {
                                if (crs.getName().equals("EPSG:4978")) {
                                    exteriorPolygonLocal.add(position);
                                } else {
                                    Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                                    Vector3d localPosition = positionWorldCoordinate.mulPosition(transformMatrixInv);
                                    exteriorPolygonLocal.add(localPosition);
                                }
                            }

                            // interior points
                            List<List<Vector3d>> interiorPolygonsLocal = new ArrayList<>();
                            for (List<Vector3d> interiorPolygon : interiorPolygons) {
                                List<Vector3d> interiorPolygonLocal = new ArrayList<>();
                                for (Vector3d position : interiorPolygon) {
                                    if (crs.getName().equals("EPSG:4978")) {
                                        interiorPolygonLocal.add(position);
                                    } else {
                                        Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                                        Vector3d localPosition = positionWorldCoordinate.mulPosition(transformMatrixInv);
                                        interiorPolygonLocal.add(localPosition);
                                    }
                                }
                                interiorPolygonsLocal.add(interiorPolygonLocal);
                            }
                            GaiaPrimitive primitive = createSurfaceFromExteriorAndInteriorPolygons(exteriorPolygonLocal, interiorPolygonsLocal);
                            if (primitive.getSurfaces().isEmpty() || primitive.getVertices().size() < 3) {
                                log.debug("Invalid Geometry : {}", buildingSurface.getId());
                                log.debug("Vertices count : {}", primitive.getVertices().size());
                                log.debug("Surfaces count : {}", primitive.getSurfaces().size());
                                continue;
                            }

                            primitive.setMaterialIndex(material.getId());
                            mesh.getPrimitives().add(primitive);
                            rootNode.getChildren().add(node);
                        }
                    }

                    Matrix4d rootTransformMatrix = new Matrix4d().identity();
                    rootNode.setTransformMatrix(rootTransformMatrix);

                    Vector3d degreeTranslation = scene.getTranslation();
                    degreeTranslation.set(center);

                    if (rootNode.getChildren().size() <= 0) {
                        log.debug("Invalid Scene : {}", rootNode.getName());
                        continue;
                    }
                    scenes.add(scene);
                }
            }
            reader.close();
        } catch (CityGMLContextException | CityGMLReadException e) {
            log.error("[ERROR] Failed to read citygml file: {}", file.getName());
            throw new RuntimeException(e);
        }

        return scenes;
    }

    private List<GaiaSurfaceModel> convertSolidSurfaceProperty(AbstractCityObject cityObject, AbstractSolid abstractSolid) {
        List<GaiaSurfaceModel> buildingSurfaces = new ArrayList<>();

        List<Solid> solids = new ArrayList<>();
        if (abstractSolid == null) {
            log.error("[ERROR] No solid found for city object: {}", cityObject.getId());
            return buildingSurfaces;
        } else if (abstractSolid instanceof CompositeSolid object) {
            object.getSolidMembers().forEach(solidProperty -> {
                Solid solid = (Solid) solidProperty.getObject();
                if (solid != null) {
                    solids.add(solid);
                }
            });
        } else if (abstractSolid instanceof Solid object) {
            solids.add(object);
        }

        for (Solid solid : solids) {
            ShellProperty exterior = solid.getExterior();
            if (exterior == null) {
                continue;
            }
            Shell shell = exterior.getObject();
            if (shell == null) {
                continue;
            }
            Classification classification = getClassification(cityObject);
            List<SurfaceProperty> surfaceProperties = shell.getSurfaceMembers();
            List<GaiaSurfaceModel> compositeSurfaces = convertSurfaceProperty(cityObject, classification, surfaceProperties);
            buildingSurfaces.addAll(compositeSurfaces);
        }
        return buildingSurfaces;
    }

    private List<GaiaSurfaceModel> convertMultiSurfaceProperty(AbstractCityObject cityObject, MultiSurfaceProperty multiSurfaceProperty) {
        List<GaiaSurfaceModel> buildingSurfaces = new ArrayList<>();

        MultiSurface multiSurface = multiSurfaceProperty.getObject();
        if (multiSurface == null) {
            log.error("[ERROR] No multi surface found for city object: {}", cityObject.getId());
            return buildingSurfaces;
        }

        SurfaceArrayProperty surfaceArrayProperty = multiSurface.getSurfaceMembers();
        List<SurfaceProperty> surfaceProperties = multiSurface.getSurfaceMember();
        if (surfaceProperties == null && surfaceArrayProperty == null) {
            log.error("[ERROR] No surface properties found for city object: {}", cityObject.getId());
            return buildingSurfaces;
        }

        if (surfaceProperties == null && surfaceArrayProperty != null) {
            List<AbstractSurface> surfaces = surfaceArrayProperty.getObjects();
            if (surfaces == null) {
                log.error("[ERROR] No surfaces found for city object: {}", cityObject.getId());
                return buildingSurfaces;
            }
            List<SurfaceProperty> newSurfaceProperties = new ArrayList<>();
            for (AbstractSurface surface : surfaces) {
                newSurfaceProperties.add(new SurfaceProperty(surface));
            }
            surfaceProperties = newSurfaceProperties;
        }

        Classification classification = getClassification(cityObject);
        Child parent = multiSurfaceProperty.getParent();
        if (parent instanceof AbstractSpaceBoundary) {
            classification = getClassification((AbstractSpaceBoundary) parent);
        } else if (parent instanceof AbstractCityObject) {
            classification = getClassification((AbstractCityObject) parent);
        } else {
            //classification = Classification.WALL;
            log.info("Parent is not AbstractSpaceBoundary or AbstractCityObject:");
        }
        buildingSurfaces = convertSurfaceProperty(cityObject, classification, surfaceProperties);
        return buildingSurfaces;
    }

    private List<Vector3d> convertLinearRingToPolygon(LinearRing linearRing, GaiaBoundingBox resultBBox) {
        List<Vector3d> polygon = new ArrayList<>();

        boolean flipCoordinate = options.isFlipCoordinate();

        GeometricPositionList geometricPositionList = linearRing.getControlPoints();
        DirectPositionList directPositions = geometricPositionList.getPosList();
        if (directPositions != null) {
            List<Double> positions = directPositions.getValue();
            for (int i = 0; i < positions.size(); i += 3) {
                double x, y, z = 0.0d;

                if (flipCoordinate) {
                    x = positions.get(i + 1);
                    y = positions.get(i);
                } else {
                    x = positions.get(i);
                    y = positions.get(i + 1);
                }
                z = positions.get(i + 2);
                Vector3d position = new Vector3d(x, y, z);
                CoordinateReferenceSystem crs = options.getSourceCrs();
                if (crs != null) {
                    ProjCoordinate projCoordinate = new ProjCoordinate(x, y, resultBBox.getMinZ());
                    ProjCoordinate centerWgs84 = GlobeUtils.transform(crs, projCoordinate);
                    position = new Vector3d(centerWgs84.x, centerWgs84.y, z);
                }
                polygon.add(position);
                resultBBox.addPoint(position);
            }
        } else {
            List<GeometricPosition> geometricPositions = geometricPositionList.getGeometricPositions();
            geometricPositions.forEach((geometricPosition) -> {
                DirectPosition directPosition = geometricPosition.getPos();
                List<Double> positions = directPosition.getValue();
                for (int i = 0; i < positions.size(); i += 3) {
                    double x, y, z = 0.0d;
                    x = positions.get(i);
                    y = positions.get(i + 1);
                    z = positions.get(i + 2);
                    Vector3d position = new Vector3d(x, y, z);
                    CoordinateReferenceSystem crs = options.getSourceCrs();
                    if (crs != null) {
                        ProjCoordinate projCoordinate = new ProjCoordinate(x, y, resultBBox.getMinZ());
                        ProjCoordinate centerWgs84 = GlobeUtils.transform(crs, projCoordinate);
                        position = new Vector3d(centerWgs84.x, centerWgs84.y, z);
                    }
                    polygon.add(position);
                    resultBBox.addPoint(position);
                }
            });
        }

        return polygon;
    }

    private List<GaiaSurfaceModel> convertSurfaceProperty(AbstractCityObject cityObject, Classification classification, List<SurfaceProperty> surfaceProperties) {
        List<GaiaSurfaceModel> buildingSurfaces = new ArrayList<>();
        int surfaceCount = surfaceProperties.size();
        for (SurfaceProperty surfaceProperty : surfaceProperties) {
            AbstractSurface abstractSurface = surfaceProperty.getObject();
            if (abstractSurface instanceof CompositeSurface compositeSurface) {
                List<GaiaSurfaceModel> compositeSurfaces = convertSurfaceProperty(cityObject, classification, compositeSurface.getSurfaceMembers());
                buildingSurfaces.addAll(compositeSurfaces);
                continue;
            }

            List<LinearRing> interiorRings = new ArrayList<>();
            LinearRing exteriorLinearRing = null;
            if (abstractSurface == null) {
                log.error("[ERROR] No surface found for city object: {}", cityObject.getId());
                continue;
            } else if (abstractSurface instanceof Polygon polygon) {
                exteriorLinearRing = (LinearRing) polygon.getExterior().getObject();
                polygon.getInterior().forEach(interior -> {
                    interiorRings.add((LinearRing) interior.getObject());
                });
            } else {
                log.error("Unsupported surface type: {}", abstractSurface.getClass().getSimpleName());
                continue;
            }

            GaiaBoundingBox boundingBox = new GaiaBoundingBox();
            List<Vector3d> vec3Polygon = convertLinearRingToPolygon(exteriorLinearRing, boundingBox);
            if (vec3Polygon == null) {
                continue;
            }

            List<List<Vector3d>> vec3InteriorPolygons = null;
            if (!interiorRings.isEmpty()) {
                vec3InteriorPolygons = new ArrayList<>();
                for (LinearRing interiorRing : interiorRings) {
                    List<Vector3d> vec3InteriorPolygon = convertLinearRingToPolygon(interiorRing, boundingBox);
                    if (vec3InteriorPolygon == null) {
                        continue;
                    }
                    vec3InteriorPolygons.add(vec3InteriorPolygon);
                }
            }
            Map<String, String> properties = new HashMap<>();
            properties.put("name", cityObject.getId());
            GaiaSurfaceModel gaiaBuildingSurface = GaiaSurfaceModel.builder().id(cityObject.getId()).name(cityObject.getId()).exteriorPositions(vec3Polygon).interiorPositions(vec3InteriorPolygons).boundingBox(boundingBox).classification(classification).properties(properties).build();
            buildingSurfaces.add(gaiaBuildingSurface);
        }
        return buildingSurfaces;
    }

    protected double getHeight(Building building) {
        if (building.getHeights().size() > 1) {
            return building.getHeights().get(0).getObject().getValue().getValue();
        } else {
            return 0.0d;
        }
    }

    protected double getHeight(BuildingRoom buildingRoom) {
        if (!buildingRoom.getRoomHeights().isEmpty()) {
            return buildingRoom.getRoomHeights().get(0).getObject().getValue().getValue();
        } else {
            return 0.0d;
        }
    }

    private List<SolidProperty> extractSolid(AbstractCityObject cityObject) {
        List<SolidProperty> solids = new ArrayList<>();
        SolidProperty lod1Solid = null;
        SolidProperty lod2Solid = null;
        SolidProperty lod3Solid = null;
        List<BuildingRoomProperty> buildingRoomProperties = null;
        List<BuildingPartProperty> buildingPartProperties = null;

        if (cityObject instanceof Building object) {
            lod1Solid = object.getLod1Solid();
            lod2Solid = object.getLod2Solid();
            lod3Solid = object.getLod3Solid();
            buildingRoomProperties = object.getBuildingRooms();
            buildingPartProperties = object.getBuildingParts();
        } else if (cityObject instanceof BuildingPart object) {
            lod1Solid = object.getLod1Solid();
            lod2Solid = object.getLod2Solid();
            lod3Solid = object.getLod3Solid();
            buildingRoomProperties = object.getBuildingRooms();
        } else if (cityObject instanceof BuildingRoom object) {
            lod1Solid = object.getLod1Solid();
            lod2Solid = object.getLod2Solid();
            lod3Solid = object.getLod3Solid();
        } else if (cityObject instanceof Bridge object) {
            lod1Solid = object.getLod1Solid();
            lod2Solid = object.getLod2Solid();
            lod3Solid = object.getLod3Solid();
        } else if (cityObject instanceof SolitaryVegetationObject object) {
            lod1Solid = object.getLod1Solid();
            lod2Solid = object.getLod2Solid();
            lod3Solid = object.getLod3Solid();
        } else if (cityObject instanceof CityObjectGroup object) {
            lod1Solid = object.getLod1Solid();
            lod2Solid = object.getLod2Solid();
            lod3Solid = object.getLod3Solid();
        } else if (cityObject instanceof Tunnel object) {
            lod1Solid = object.getLod1Solid();
            lod2Solid = object.getLod2Solid();
            lod3Solid = object.getLod3Solid();
        } else if (cityObject instanceof TunnelInstallation object) {
            lod1Solid = object.getLod1Solid();
            lod2Solid = object.getLod2Solid();
            lod3Solid = object.getLod3Solid();
        } else if (cityObject instanceof WaterBody object) {
            lod1Solid = object.getLod1Solid();
            lod2Solid = object.getLod2Solid();
            lod3Solid = object.getLod3Solid();
        } else if (cityObject instanceof Railway object) {
            lod1Solid = object.getLod1Solid();
            lod2Solid = object.getLod2Solid();
            lod3Solid = object.getLod3Solid();
        } else if (cityObject instanceof CityFurniture object) {
            lod1Solid = object.getLod1Solid();
            lod2Solid = object.getLod2Solid();
            lod3Solid = object.getLod3Solid();
        } else if (cityObject instanceof GenericOccupiedSpace object) {
            lod1Solid = object.getLod1Solid();
            lod2Solid = object.getLod2Solid();
            lod3Solid = object.getLod3Solid();
        } else if (cityObject instanceof Road object) {
            lod1Solid = object.getLod1Solid();
            lod2Solid = object.getLod2Solid();
            lod3Solid = object.getLod3Solid();
        } else if (cityObject instanceof PlantCover object) {
            lod1Solid = object.getLod1Solid();
            lod2Solid = object.getLod2Solid();
            lod3Solid = object.getLod3Solid();
        } else if (cityObject instanceof ReliefFeature object) {
            List<AbstractReliefComponentProperty> abstractReliefComponents = object.getReliefComponents();
            log.debug("ReliefFeature: {}", object.getId());
        } else {
            log.debug("Unsupported city object type: {}", cityObject.getClass().getSimpleName());
        }
        int solidCount = 0;

        if (lod1Solid != null) {
            solids.add(lod1Solid);
            solidCount++;
        }
        if (lod2Solid != null) {
            solids.add(lod2Solid);
            solidCount++;
        }
        if (lod3Solid != null) {
            solids.add(lod3Solid);
            solidCount++;
        }

        if (solidCount > 1) {
            log.warn("[WARN] Multiple solids found for city object: {}", cityObject.getId());
        }

        if (buildingRoomProperties != null && !buildingRoomProperties.isEmpty()) {
            for (BuildingRoomProperty buildingRoomProperty : buildingRoomProperties) {
                BuildingRoom buildingRoom = buildingRoomProperty.getObject();
                List<SolidProperty> solidProperties = extractSolid(buildingRoom);
                solids.addAll(solidProperties);
            }
        }
        if (buildingPartProperties != null && !buildingPartProperties.isEmpty()) {
            for (BuildingPartProperty buildingPartProperty : buildingPartProperties) {
                BuildingPart buildingPart = buildingPartProperty.getObject();
                List<SolidProperty> solidProperties = extractSolid(buildingPart);
                solids.addAll(solidProperties);
            }
        }

        return solids;
    }

    private List<MultiSurfaceProperty> extractMultiSurfaceProperty(AbstractCityObject cityObject) {
        List<MultiSurfaceProperty> multiSurfaces = new ArrayList<>();
        MultiSurfaceProperty lod0MultiSurface = null;
        MultiSurfaceProperty lod1MultiSurface = null;
        MultiSurfaceProperty lod2MultiSurface = null;
        MultiSurfaceProperty lod3MultiSurface = null;

        List<AbstractSpaceBoundary> boundaries = new ArrayList<>();
        List<AbstractCityObject> childCityObjects = new ArrayList<>();
        List<ImplicitGeometryProperty> implicitGeometryProperties = new ArrayList<>();

        if (cityObject instanceof Building object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            object.getBoundaries().forEach(childProperty -> {
                boundaries.add(childProperty.getObject());
            });
            object.getBuildingRooms().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
            object.getBuildingParts().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
            object.getBuildingInstallations().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
            object.getBuildingFurniture().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
            object.getBuildingConstructiveElements().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
        } else if (cityObject instanceof BuildingPart object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            object.getBoundaries().forEach(childProperty -> {
                boundaries.add(childProperty.getObject());
            });
            object.getBuildingRooms().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
            object.getBoundaries().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
            object.getBuildingInstallations().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
            object.getBuildingFurniture().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
            object.getBuildingConstructiveElements().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
        } else if (cityObject instanceof BuildingRoom object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            object.getBoundaries().forEach(childProperty -> {
                boundaries.add(childProperty.getObject());
            });
            object.getBuildingFurniture().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
            object.getBuildingInstallations().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
            object.getBoundaries().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
        } else if (cityObject instanceof BuildingInstallation object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            object.getBoundaries().forEach(childProperty -> {
                boundaries.add(childProperty.getObject());
            });
        } else if (cityObject instanceof BuildingFurniture object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            object.getBoundaries().forEach(childProperty -> {
                boundaries.add(childProperty.getObject());
            });
        } else if (cityObject instanceof DoorSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
        } else if (cityObject instanceof WindowSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
        } else if (cityObject instanceof ClosureSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
        } else if (cityObject instanceof CeilingSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
        } else if (cityObject instanceof InteriorWallSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
        } else if (cityObject instanceof FloorSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
        } else if (cityObject instanceof GroundSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
        } else if (cityObject instanceof RoofSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
        } else if (cityObject instanceof WallSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
        } else if (cityObject instanceof ReliefFeature object) {
            List<MultiSurfaceProperty> multiSurfaceProperties = extractMultiSurfaceProperty(object);
            multiSurfaces.addAll(multiSurfaceProperties);
        } else if (cityObject instanceof AbstractSpaceBoundary object) {
            List<MultiSurfaceProperty> multiSurfaceProperties = extractMultiSurfaceProperty(object);
            multiSurfaces.addAll(multiSurfaceProperties);
        } else if (cityObject instanceof SolitaryVegetationObject object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            object.getBoundaries().forEach(childProperty -> {
                boundaries.add(childProperty.getObject());
            });
            if (object.getLod1ImplicitRepresentation() != null) {
                implicitGeometryProperties.add(object.getLod1ImplicitRepresentation());
            }
            if (object.getLod2ImplicitRepresentation() != null) {
                implicitGeometryProperties.add(object.getLod2ImplicitRepresentation());
            }
            if (object.getLod3ImplicitRepresentation() != null) {
                implicitGeometryProperties.add(object.getLod3ImplicitRepresentation());
            }
        } else if (cityObject instanceof Bridge object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            object.getBridgeParts().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
            object.getBridgeInstallations().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
            object.getBridgeFurniture().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
            object.getBridgeConstructiveElements().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
            object.getBoundaries().forEach(childProperty -> {
                boundaries.add(childProperty.getObject());
            });
            if (object.getDeprecatedProperties() != null) {
                DeprecatedPropertiesOfAbstractBridge deprecatedProperties = object.getDeprecatedProperties();
                if (deprecatedProperties.getLod1MultiSurface() != null) {
                    lod1MultiSurface = deprecatedProperties.getLod1MultiSurface();
                }
                if (deprecatedProperties.getLod4MultiSurface() != null) {
                    lod3MultiSurface = deprecatedProperties.getLod4MultiSurface();
                }
            }
        } else if (cityObject instanceof Tunnel object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            object.getTunnelFurniture().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
            object.getTunnelInstallations().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
            object.getTunnelConstructiveElements().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
            object.getTunnelParts().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
            object.getBoundaries().forEach(childProperty -> {
                boundaries.add(childProperty.getObject());
            });
        } else if (cityObject instanceof TunnelInstallation object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            object.getBoundaries().forEach(childProperty -> {
                boundaries.add(childProperty.getObject());
            });
        } else if (cityObject instanceof Railway object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            object.getBoundaries().forEach(childProperty -> {
                boundaries.add(childProperty.getObject());
            });
        } else if (cityObject instanceof CityObjectGroup object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            object.getBoundaries().forEach(childProperty -> {
                boundaries.add(childProperty.getObject());
            });
        } else if (cityObject instanceof GenericOccupiedSpace object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            object.getBoundaries().forEach(childProperty -> {
                boundaries.add(childProperty.getObject());
            });
        } else if (cityObject instanceof CityFurniture object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            object.getBoundaries().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
        } else if (cityObject instanceof OtherConstruction object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            object.getBoundaries().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
        } else if (cityObject instanceof WaterBody object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            if (object.getDeprecatedProperties() != null) {
                DeprecatedPropertiesOfWaterBody deprecatedProperties = object.getDeprecatedProperties();
                if (deprecatedProperties.getLod1MultiSurface() != null) {
                    lod1MultiSurface = deprecatedProperties.getLod1MultiSurface();
                }
            }
            object.getBoundaries().forEach(childProperty -> {
                boundaries.add(childProperty.getObject());
            });
        } else if (cityObject instanceof PlantCover object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            if (object.getDeprecatedProperties() != null) {
                DeprecatedPropertiesOfPlantCover deprecatedProperties = object.getDeprecatedProperties();
                if (deprecatedProperties.getLod1MultiSurface() != null) {
                    lod1MultiSurface = deprecatedProperties.getLod1MultiSurface();
                }
                if (deprecatedProperties.getLod4MultiSurface() != null) {
                    lod3MultiSurface = deprecatedProperties.getLod4MultiSurface();
                }
            }
            object.getBoundaries().forEach(childProperty -> {
                boundaries.add(childProperty.getObject());
            });
        } else if (cityObject instanceof Road object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();

            if (object.getDeprecatedProperties() != null) {
                DeprecatedPropertiesOfAbstractTransportationSpace deprecatedProperties = object.getDeprecatedProperties();
                if (deprecatedProperties.getLod1MultiSurface() != null) {
                    lod1MultiSurface = deprecatedProperties.getLod1MultiSurface();
                }
                if (deprecatedProperties.getLod4MultiSurface() != null) {
                    lod3MultiSurface = deprecatedProperties.getLod4MultiSurface();
                }
            }

            object.getBoundaries().forEach(childProperty -> {
                boundaries.add(childProperty.getObject());
            });
        } else if (cityObject instanceof AbstractSpace object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            object.getBoundaries().forEach(childProperty -> {
                boundaries.add(childProperty.getObject());
            });
        } else {
            log.info("Unsupported city object type: {}", cityObject.getClass().getSimpleName());
        }

        if (lod0MultiSurface != null) {
            multiSurfaces.add(lod0MultiSurface);
        }
        if (lod1MultiSurface != null) {
            multiSurfaces.add(lod1MultiSurface);
        }
        if (lod2MultiSurface != null) {
            multiSurfaces.add(lod2MultiSurface);
        }
        if (lod3MultiSurface != null) {
            multiSurfaces.add(lod3MultiSurface);
        }

        for (AbstractSpaceBoundary spaceBoundary : boundaries) {
            List<MultiSurfaceProperty> childMultiSurfaces = extractMultiSurfaceProperty(spaceBoundary);
            multiSurfaces.addAll(childMultiSurfaces);
        }

        for (AbstractCityObject childCityObject : childCityObjects) {
            List<MultiSurfaceProperty> childMultiSurfaces = extractMultiSurfaceProperty(childCityObject);
            multiSurfaces.addAll(childMultiSurfaces);
        }

        // Implicit Geometry, It is like instanced model
        // TODO : Implicit Geometry
        /*for (ImplicitGeometryProperty implicitGeometryProperty : implicitGeometryProperties) {
            ImplicitGeometry implicitGeometry = implicitGeometryProperty.getObject();
            log.info("Implicit Geometry: {}", implicitGeometry.getId());
            GeometryProperty geometryProperty = implicitGeometry.getRelativeGeometry();
            MultiSurface multiSurface = (MultiSurface) geometryProperty.getObject();
            MultiSurfaceProperty multiSurfaceProperty = new MultiSurfaceProperty(multiSurface);
            multiSurfaces.add(multiSurfaceProperty);
        }*/

        return multiSurfaces;
    }

    private List<MultiSurfaceProperty> extractMultiSurfaceProperty(AbstractReliefComponent abstractReliefComponent) {
        List<MultiSurfaceProperty> multiSurfaceProperties = new ArrayList<>();

        //SurfaceArrayProperty surfaceArrayProperty = new SurfaceArrayProperty();
        List<SurfaceProperty> surfaceProperties = new ArrayList<>();
        if (abstractReliefComponent instanceof TINRelief object) {
            TriangulatedSurface triangulatedSurface = object.getTin().getObject();
            TriangleArrayProperty triangleArrayProperty = triangulatedSurface.getPatches();
            List<Triangle> triangles = triangleArrayProperty.getObjects();
            triangles.forEach(triangle -> {
                AbstractRingProperty ringProperty = triangle.getExterior();
                AbstractRing ring = ringProperty.getObject();
                LinearRing linearRing = (LinearRing) ring;
                DirectPositionList directPositions = linearRing.getControlPoints().getPosList();
                if (directPositions == null) {
                    List<GeometricPosition> geometricPositions = linearRing.getControlPoints().getGeometricPositions();
                    DirectPositionList newDirectPositions = new DirectPositionList();

                    List<Double> coords = new ArrayList<>(geometricPositions.size() * 3);
                    geometricPositions.forEach(geometricPosition -> {
                        DirectPosition directPosition = geometricPosition.getPos();
                        List<Double> list = directPosition.getValue();
                        for (int i = 0; i < list.size(); i += 3) {
                            double x = list.get(i);
                            double y = list.get(i + 1);
                            double z = list.get(i + 2);
                            coords.add(x);
                            coords.add(y);
                            coords.add(z);
                        }
                        directPosition.setValue(coords);
                    });
                    newDirectPositions.setValue(coords);
                    GeometricPositionList geometricPositionList = new GeometricPositionList();
                    geometricPositionList.setPosList(newDirectPositions);
                    linearRing.setControlPoints(geometricPositionList);
                    SurfaceProperty surfaceProperty = createSurfaceProperty(linearRing);
                    surfaceProperties.add(surfaceProperty);
                } else {
                    List<Double> list = directPositions.getValue();
                    List<Double> coords = new ArrayList<>(list.size());
                    for (int i = 0; i < list.size(); i += 3) {
                        double x = list.get(i);
                        double y = list.get(i + 1);
                        double z = list.get(i + 2);
                        coords.add(x);
                        coords.add(y);
                        coords.add(z);
                    }
                    directPositions.setValue(coords);

                    GeometricPositionList geometricPositionList = new GeometricPositionList();
                    geometricPositionList.setPosList(directPositions);
                    linearRing.setControlPoints(geometricPositionList);
                    SurfaceProperty surfaceProperty = createSurfaceProperty(linearRing);
                    surfaceProperties.add(surfaceProperty);
                }
            });
        }
        //surfaceArrayProperty.setObjects(surfaceProperties);
        MultiSurfaceProperty multiSurfaceProperty = createMultiSurfaceProperties(surfaceProperties);
        multiSurfaceProperties.add(multiSurfaceProperty);
        return multiSurfaceProperties;
    }

    private List<MultiSurfaceProperty> extractMultiSurfaceProperty(AbstractSpaceBoundary spaceBoundary) {
        List<MultiSurfaceProperty> multiSurfaces = new ArrayList<>();
        MultiSurfaceProperty lod0MultiSurface = null;
        MultiSurfaceProperty lod1MultiSurface = null;
        MultiSurfaceProperty lod2MultiSurface = null;
        MultiSurfaceProperty lod3MultiSurface = null;
        MultiSurfaceProperty lod4MultiSurface = null;

        List<AbstractFillingSurfaceProperty> fillingSurfaceProperties = null;
        List<AbstractReliefComponentProperty> reliefComponentProperties = null;
        List<AbstractCityObject> childCityObjects = new ArrayList<>();
        if (spaceBoundary instanceof CeilingSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            lod4MultiSurface = object.getDeprecatedProperties().getLod4MultiSurface();
            fillingSurfaceProperties = object.getFillingSurfaces();
        } else if (spaceBoundary instanceof InteriorWallSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            lod4MultiSurface = object.getDeprecatedProperties().getLod4MultiSurface();
            fillingSurfaceProperties = object.getFillingSurfaces();
        } else if (spaceBoundary instanceof FloorSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            lod4MultiSurface = object.getDeprecatedProperties().getLod4MultiSurface();
            fillingSurfaceProperties = object.getFillingSurfaces();
        } else if (spaceBoundary instanceof GroundSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            lod4MultiSurface = object.getDeprecatedProperties().getLod4MultiSurface();
            fillingSurfaceProperties = object.getFillingSurfaces();
        } else if (spaceBoundary instanceof RoofSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            lod4MultiSurface = object.getDeprecatedProperties().getLod4MultiSurface();
            fillingSurfaceProperties = object.getFillingSurfaces();
        } else if (spaceBoundary instanceof WallSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            lod4MultiSurface = object.getDeprecatedProperties().getLod4MultiSurface();
            fillingSurfaceProperties = object.getFillingSurfaces();
        } else if (spaceBoundary instanceof ClosureSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            lod4MultiSurface = object.getDeprecatedProperties().getLod4MultiSurface();
        } else if (spaceBoundary instanceof DoorSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            lod4MultiSurface = object.getDeprecatedProperties().getLod4MultiSurface();
        } else if (spaceBoundary instanceof WindowSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            lod4MultiSurface = object.getDeprecatedProperties().getLod4MultiSurface();
        } else if (spaceBoundary instanceof WaterSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            lod4MultiSurface = object.getDeprecatedProperties().getLod4MultiSurface();
        } else if (spaceBoundary instanceof OuterCeilingSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            lod4MultiSurface = object.getDeprecatedProperties().getLod4MultiSurface();
        } else if (spaceBoundary instanceof WaterGroundSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            lod4MultiSurface = object.getDeprecatedProperties().getLod4MultiSurface();
        } else if (spaceBoundary instanceof ReliefFeature object) {
            reliefComponentProperties = object.getReliefComponents();
        } else {
            log.info("Unsupported space boundary type: {}", spaceBoundary.getClass().getSimpleName());
        }

        if (lod0MultiSurface != null) {
            multiSurfaces.add(lod0MultiSurface);
        }
        if (lod1MultiSurface != null) {
            multiSurfaces.add(lod1MultiSurface);
        }
        if (lod2MultiSurface != null) {
            multiSurfaces.add(lod2MultiSurface);
        }
        if (lod3MultiSurface != null) {
            multiSurfaces.add(lod3MultiSurface);
        }
        if (lod4MultiSurface != null) {
            multiSurfaces.add(lod4MultiSurface);
        }

        if (fillingSurfaceProperties != null && !fillingSurfaceProperties.isEmpty()) {
            for (AbstractFillingSurfaceProperty fillingSurfaceProperty : fillingSurfaceProperties) {
                AbstractFillingSurface fillingSurface = fillingSurfaceProperty.getObject();
                List<MultiSurfaceProperty> multiSurfaceProperties = extractMultiSurfaceProperty(fillingSurface);
                multiSurfaces.addAll(multiSurfaceProperties);
            }
        }

        if (reliefComponentProperties != null && !reliefComponentProperties.isEmpty()) {
            for (AbstractReliefComponentProperty reliefComponentProperty : reliefComponentProperties) {
                AbstractReliefComponent reliefComponent = reliefComponentProperty.getObject();
                List<MultiSurfaceProperty> multiSurfaceProperties = extractMultiSurfaceProperty(reliefComponent);
                multiSurfaces.addAll(multiSurfaceProperties);
            }
        }

        return multiSurfaces;
    }

    private MultiSurfaceProperty createMultiSurfaceProperties(List<SurfaceProperty> surfaceProperties) {
        MultiSurface multiSurface = new MultiSurface();
        multiSurface.setSurfaceMember(surfaceProperties);
        MultiSurfaceProperty multiSurfaceProperty = new MultiSurfaceProperty();
        multiSurfaceProperty.setInlineObject(multiSurface);
        return multiSurfaceProperty;
    }

    private SurfaceProperty createSurfaceProperty(LinearRing linearRing) {
        LinearRingProperty linearRingProperty = new LinearRingProperty();
        linearRingProperty.setObject(linearRing);

        Polygon polygon = new Polygon();
        AbstractRingPropertyAdapter ringPropertyAdapter = new AbstractRingPropertyAdapter();
        try {
            AbstractRingProperty ringProperty = ringPropertyAdapter.createObject(new QName("handmade-polygon"), polygon);
            ringProperty.setObject(linearRing);
            polygon.setExterior(ringProperty);
        } catch (ObjectBuildException e) {
            throw new RuntimeException(e);
        }
        SurfaceProperty surfaceProperty = new SurfaceProperty();
        surfaceProperty.setInlineObject(polygon);

        return surfaceProperty;
    }

    private Classification getClassification(AbstractSpaceBoundary abstractSpaceBoundary) {
        if (abstractSpaceBoundary instanceof CeilingSurface) {
            return Classification.CEILING;
        } else if (abstractSpaceBoundary instanceof InteriorWallSurface) {
            return Classification.WALL;
        } else if (abstractSpaceBoundary instanceof FloorSurface) {
            return Classification.FLOOR;
        } else if (abstractSpaceBoundary instanceof GroundSurface) {
            return Classification.FLOOR;
        } else if (abstractSpaceBoundary instanceof RoofSurface) {
            return Classification.ROOF;
        } else if (abstractSpaceBoundary instanceof WallSurface) {
            return Classification.WALL;
        } else if (abstractSpaceBoundary instanceof ClosureSurface) {
            return Classification.WINDOW;
        } else if (abstractSpaceBoundary instanceof DoorSurface) {
            return Classification.DOOR;
        } else if (abstractSpaceBoundary instanceof WindowSurface) {
            return Classification.WINDOW;
        } else if (abstractSpaceBoundary instanceof WaterSurface) {
            return Classification.WATER;
        } else if (abstractSpaceBoundary instanceof WaterGroundSurface) {
            return Classification.GROUND;
        } else if (abstractSpaceBoundary instanceof ReliefFeature) {
            return Classification.GROUND;
        } else {
            return Classification.WALL;
        }
    }

    private Classification getClassification(AbstractCityObject cityObject) {
        if (cityObject instanceof Building) {
            return Classification.WALL;
        } else if (cityObject instanceof BuildingFurniture) {
            return Classification.FURNITURE;
        } else if (cityObject instanceof BuildingInstallation) {
            return Classification.INSTALLATION;
        } else if (cityObject instanceof BuildingPart) {
            return Classification.WALL;
        } else if (cityObject instanceof BuildingRoom) {
            return Classification.WALL;
        } else if (cityObject instanceof CityFurniture) {
            return Classification.FURNITURE;
        } else if (cityObject instanceof DoorSurface) {
            return Classification.DOOR;
        } else if (cityObject instanceof WindowSurface) {
            return Classification.WINDOW;
        } else if (cityObject instanceof ClosureSurface) {
            return Classification.WALL;
        } else if (cityObject instanceof CeilingSurface) {
            return Classification.CEILING;
        } else if (cityObject instanceof InteriorWallSurface) {
            return Classification.WALL;
        } else if (cityObject instanceof FloorSurface) {
            return Classification.FLOOR;
        } else if (cityObject instanceof GroundSurface) {
            return Classification.FLOOR;
        } else if (cityObject instanceof RoofSurface) {
            return Classification.ROOF;
        } else if (cityObject instanceof WallSurface) {
            return Classification.WALL;
        } else if (cityObject instanceof Bridge) {
            return Classification.INFRASTRUCTURE;
        } else if (cityObject instanceof BridgeFurniture) {
            return Classification.FURNITURE;
        } else if (cityObject instanceof BridgeInstallation) {
            return Classification.INSTALLATION;
        } else if (cityObject instanceof Tunnel) {
            return Classification.INFRASTRUCTURE;
        } else if (cityObject instanceof TunnelFurniture) {
            return Classification.FURNITURE;
        } else if (cityObject instanceof TunnelInstallation) {
            return Classification.INSTALLATION;
        } else if (cityObject instanceof AbstractSpaceBoundary abstractSpaceBoundary) {
            return getClassification(abstractSpaceBoundary);
        } else if (cityObject instanceof WaterBody) {
            return Classification.WATER;
        } else if (cityObject instanceof Railway) {
            return Classification.WALL;
        } else if (cityObject instanceof GenericOccupiedSpace) {
            return Classification.FURNITURE;
        } else if (cityObject instanceof SolitaryVegetationObject) {
            return Classification.GROUND;
        } else if (cityObject instanceof Road) {
            return Classification.FLOOR;
        } else if (cityObject instanceof PlantCover) {
            return Classification.INSTALLATION;
        }
        log.info("Unsupported city object type: {}", cityObject.getClass().getSimpleName());
        return Classification.WALL;
    }
}