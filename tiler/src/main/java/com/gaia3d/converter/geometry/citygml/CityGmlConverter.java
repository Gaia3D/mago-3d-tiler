package com.gaia3d.converter.geometry.citygml;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.*;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.geometry.*;
import com.gaia3d.converter.geometry.extrusion.Extruder;
import com.gaia3d.util.GlobeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.citygml4j.core.model.bridge.Bridge;
import org.citygml4j.core.model.building.*;
import org.citygml4j.core.model.cityfurniture.CityFurniture;
import org.citygml4j.core.model.cityobjectgroup.CityObjectGroup;
import org.citygml4j.core.model.construction.*;
import org.citygml4j.core.model.core.*;
import org.citygml4j.core.model.generics.GenericOccupiedSpace;
import org.citygml4j.core.model.transportation.Railway;
import org.citygml4j.core.model.tunnel.Tunnel;
import org.citygml4j.core.model.vegetation.SolitaryVegetationObject;
import org.citygml4j.core.model.waterbody.WaterBody;
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
import org.xmlobjects.gml.model.geometry.DirectPosition;
import org.xmlobjects.gml.model.geometry.DirectPositionList;
import org.xmlobjects.gml.model.geometry.GeometricPosition;
import org.xmlobjects.gml.model.geometry.aggregates.MultiSurface;
import org.xmlobjects.gml.model.geometry.aggregates.MultiSurfaceProperty;
import org.xmlobjects.gml.model.geometry.complexes.CompositeSurface;
import org.xmlobjects.gml.model.geometry.primitives.*;
import org.xmlobjects.gml.model.geometry.primitives.Polygon;
import org.xmlobjects.model.Child;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class CityGmlConverter extends AbstractGeometryConverter implements Converter {
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

    protected List<GaiaScene> convert(File file) {
        List<GaiaScene> scenes = new ArrayList<>();

        try {
            Tessellator tessellator = new Tessellator();
            Extruder extruder = new Extruder(tessellator);

            CityGMLContext context = CityGMLContext.newInstance();
            CityGMLInputFactory factory = context.createCityGMLInputFactory();
            CityGMLReader reader = factory.createCityGMLReader(file);
            CityModel cityModel = (CityModel) reader.next();

            double skirtHeight = globalOptions.getSkirtHeight();

            List<GaiaExtrusionBuilding> buildingList = new ArrayList<>();
            List<List<GaiaBuildingSurface>> buildingSurfacesList = new ArrayList<>();

            List<AbstractCityObjectProperty> cityObjectMembers = cityModel.getCityObjectMembers();
            for (AbstractCityObjectProperty cityObjectProperty : cityObjectMembers) {
                AbstractCityObject cityObject = cityObjectProperty.getObject();

                List<SolidProperty> solidProperties = extractSolid(cityObject);
                for (SolidProperty solidPropertie : solidProperties) {
                    AbstractSolid solid = solidPropertie.getObject();
                    buildingList.addAll(convertSolidProperty(cityObject, solid));
                }

                List<MultiSurfaceProperty> multiSurfaceProperties = extractMultiSurfaceProperty(cityObject);
                for (MultiSurfaceProperty multiSurfaceProperty : multiSurfaceProperties) {
                    buildingSurfacesList.add(convertMultiSurfaceProperty(cityObject, multiSurfaceProperty));
                }
            }

            for (GaiaExtrusionBuilding gaiaBuilding : buildingList) {
                GaiaScene scene = initScene(file);
                scene.setOriginalPath(file.toPath());
                GaiaMaterial material = getMaterialByClassification(scene.getMaterials(), gaiaBuilding.getClassification());
                GaiaNode rootNode = scene.getNodes().get(0);

                GaiaAttribute attribute = scene.getAttribute();
                attribute.setAttributes(gaiaBuilding.getProperties());

                GaiaBoundingBox boundingBox = gaiaBuilding.getBoundingBox();
                Vector3d center = boundingBox.getCenter();
                center.z = center.z - skirtHeight;

                Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
                Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerWorldCoordinate);
                Matrix4d transfromMatrixInv = new Matrix4d(transformMatrix).invert();

                List<Vector3d> localPositions = new ArrayList<>();
                for (Vector3d position : gaiaBuilding.getPositions()) {
                    Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                    Vector3d localPosition = positionWorldCoordinate.mulPosition(transfromMatrixInv);
                    localPosition.z = 0.0d;
                    localPositions.add(localPosition);
                }

                Extrusion extrusion = extruder.extrude(localPositions, gaiaBuilding.getRoofHeight(), gaiaBuilding.getFloorHeight());
                GaiaNode node = createNode(material, extrusion.getPositions(), extrusion.getTriangles());
                rootNode.getChildren().add(node);

                Matrix4d rootTransformMatrix = new Matrix4d().identity();
                rootTransformMatrix.translate(center, rootTransformMatrix);
                rootNode.setTransformMatrix(rootTransformMatrix);
                scenes.add(scene);
            }

            for (List<GaiaBuildingSurface> surfaces : buildingSurfacesList) {
                if (surfaces.isEmpty()) {
                    continue;
                }

                GaiaScene scene = initScene(file);
                scene.setOriginalPath(file.toPath());
                GaiaNode rootNode = scene.getNodes().get(0);

                GaiaAttribute attribute = scene.getAttribute();
                //attribute.setAttributes(surfaces.getProperties());

                GaiaBoundingBox globalBoundingBox = new GaiaBoundingBox();
                for (GaiaBuildingSurface buildingSurface : surfaces) {
                    GaiaBoundingBox localBoundingBox = buildingSurface.getBoundingBox();
                    globalBoundingBox.addBoundingBox(localBoundingBox);
                }

                Vector3d center = globalBoundingBox.getCenter();
                Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
                Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerWorldCoordinate);
                Matrix4d transfromMatrixInv = new Matrix4d(transformMatrix).invert();

                for (GaiaBuildingSurface buildingSurface : surfaces) {

                    GaiaMaterial material = getMaterialByClassification(scene.getMaterials(), buildingSurface.getClassification());

                    // Check if buildingSurface has holes.***
                    List<List<Vector3d>> interiorPolygons = buildingSurface.getInteriorPositions();
                    boolean hasHoles = false;
                    if(interiorPolygons != null && !interiorPolygons.isEmpty())
                    {
                        hasHoles = true;
                    }

                    GaiaNode node = new GaiaNode();
                    node.setTransformMatrix(new Matrix4d().identity());
                    GaiaMesh mesh = new GaiaMesh();
                    node.getMeshes().add(mesh);

                    if(!hasHoles) {
                        List<List<Vector3d>> polygons = new ArrayList<>();
                        List<Vector3d> polygon = new ArrayList<>();

                        List<Vector3d> localPositions = new ArrayList<>();
                        for (Vector3d position : buildingSurface.getExteriorPositions()) {
                            Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                            Vector3d localPosition = positionWorldCoordinate.mulPosition(transfromMatrixInv);
                            localPositions.add(localPosition);
                            polygon.add(new Vector3dsOnlyHashEquals(localPosition));
                        }
                        polygons.add(polygon);

                        GaiaPrimitive primitive = createPrimitiveFromPolygons(polygons);

                        primitive.setMaterialIndex(material.getId());
                        mesh.getPrimitives().add(primitive);
                        rootNode.getChildren().add(node);
                    }
                    else
                    {
                        // Has holes.***
                        List<Vector3d> ExteriorPolygon = buildingSurface.getExteriorPositions();

                        // convert points to local coordinates.***
                        List<Vector3d> ExteriorPolygonLocal = new ArrayList<>();
                        for (Vector3d position : ExteriorPolygon) {
                            Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                            Vector3d localPosition = positionWorldCoordinate.mulPosition(transfromMatrixInv);
                            ExteriorPolygonLocal.add(localPosition);
                        }

                        // interior points.***
                        List<List<Vector3d>> interiorPolygonsLocal = new ArrayList<>();
                        for(List<Vector3d> interiorPolygon : interiorPolygons)
                        {
                            List<Vector3d> interiorPolygonLocal = new ArrayList<>();
                            for (Vector3d position : interiorPolygon) {
                                Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                                Vector3d localPosition = positionWorldCoordinate.mulPosition(transfromMatrixInv);
                                interiorPolygonLocal.add(localPosition);
                            }
                            interiorPolygonsLocal.add(interiorPolygonLocal);
                        }
                        GaiaPrimitive primitive = createSurfaceFromExteriorAndInteriorPolygons(ExteriorPolygonLocal, interiorPolygonsLocal);

                        primitive.setMaterialIndex(material.getId());
                        mesh.getPrimitives().add(primitive);
                        rootNode.getChildren().add(node);
                    }
                }

                Matrix4d rootTransformMatrix = new Matrix4d().identity();
                rootTransformMatrix.translate(center, rootTransformMatrix);
                rootNode.setTransformMatrix(rootTransformMatrix);
                scenes.add(scene);
            }
        } catch (CityGMLContextException | CityGMLReadException e) {
            log.error("Failed to read citygml file: {}", file.getName());
            throw new RuntimeException(e);
        }

        return scenes;
    }

    private List<GaiaExtrusionBuilding> convertSolidProperty(AbstractCityObject cityObject, AbstractSolid solid) {
        List<GaiaExtrusionBuilding> buildingList = new ArrayList<>();
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        boolean flipCoordinate = globalOptions.isFlipCoordinate();
        double skirtHeight = globalOptions.getSkirtHeight();
        double height = 1.0d;

        Shell shell = ((Solid) solid).getExterior().getObject();
        List<SurfaceProperty> surfaceProperties = shell.getSurfaceMembers();

        for (SurfaceProperty surfaceProperty : surfaceProperties) {
            GaiaBoundingBox boundingBox = new GaiaBoundingBox();

            Map<String, String> properties = new HashMap<>();
            properties.put("name", cityObject.getId());
            GaiaExtrusionBuilding gaiaBuilding = GaiaExtrusionBuilding.builder()
                    .id(cityObject.getId())
                    .name(cityObject.getId())
                    .floorHeight(0)
                    .roofHeight(height)
                    .properties(properties)
                    .build();

            List<Vector3d> polygon = new Vector<>();

            Polygon surface = (Polygon) surfaceProperty.getObject();
            if (surface == null) {
                log.error("No surface found for city object: {}", cityObject.getId());
                continue;
            }

            LinearRing linearRing = (LinearRing) surface.getExterior().getObject();
            DirectPositionList directPositions = linearRing.getControlPoints().getPosList();
            List<Double> positions = directPositions.getValue();

            double heightSum = 0d;
            for (int i = 0; i < positions.size(); i += 3) {
                double x, y, z = 0.0d;
                if (flipCoordinate) {
                    x = positions.get(i + 1);
                    y = positions.get(i);
                } else {
                    x = positions.get(i);
                    y = positions.get(i + 1);
                }
                heightSum += positions.get(i + 2);
                Vector3d position = new Vector3d(x, y, z);
                CoordinateReferenceSystem crs = globalOptions.getCrs();
                if (crs != null) {
                    ProjCoordinate projCoordinate = new ProjCoordinate(x, y, boundingBox.getMinZ());
                    ProjCoordinate centerWgs84 = GlobeUtils.transform(crs, projCoordinate);
                    position = new Vector3d(centerWgs84.x, centerWgs84.y, z);
                }
                polygon.add(position);
                boundingBox.addPoint(position);
            }

            double floorHeight = heightSum / positions.size();
            gaiaBuilding.setPositions(polygon);
            gaiaBuilding.setFloorHeight(floorHeight);
            gaiaBuilding.setRoofHeight(floorHeight + height + skirtHeight);
            gaiaBuilding.setClassification(getClassification(cityObject));
            gaiaBuilding.setBoundingBox(boundingBox);
            buildingList.add(gaiaBuilding);
        }
        return buildingList;
    }

    private List<GaiaBuildingSurface> convertMultiSurfaceProperty(AbstractCityObject cityObject, MultiSurfaceProperty multiSurfaceProperty) {
        List<GaiaBuildingSurface> buildingSurfaces = new ArrayList<>();

        MultiSurface multiSurface = multiSurfaceProperty.getObject();
        List<SurfaceProperty> surfaceProperties = multiSurface.getSurfaceMember();
        if (surfaceProperties.isEmpty()) {
            log.error("No surface properties found for city object: {}", cityObject.getId());
            return buildingSurfaces;
        }

        Classification classification = getClassification(cityObject);
        Child parent = multiSurfaceProperty.getParent();
        if (parent instanceof AbstractSpaceBoundary) {
            classification = getClassification((AbstractSpaceBoundary) parent);
        } else if (parent instanceof AbstractCityObject) {
            classification = getClassification((AbstractCityObject) parent);
        }
        //Classification classification = getClassification((AbstractSpaceBoundary) multiSurfaceProperty.getParent());
        buildingSurfaces = convertSurfaceProperty(cityObject, classification, surfaceProperties);
        return buildingSurfaces;
    }

    private List<Vector3d> convertLinearRingToPolygon(LinearRing linearRing, GaiaBoundingBox resultBBox)
    {
        List<Vector3d> polygon = new ArrayList<>();
        DirectPositionList directPositions = linearRing.getControlPoints().getPosList();
        List<Double> positions = directPositions.getValue();
        for (int i = 0; i < positions.size(); i += 3) {
            double x, y, z = 0.0d;
            x = positions.get(i);
            y = positions.get(i + 1);
            z = positions.get(i + 2);
            Vector3d position = new Vector3d(x, y, z);
            CoordinateReferenceSystem crs = globalOptions.getCrs();
            if (crs != null) {
                ProjCoordinate projCoordinate = new ProjCoordinate(x, y, resultBBox.getMinZ());
                ProjCoordinate centerWgs84 = GlobeUtils.transform(crs, projCoordinate);
                position = new Vector3d(centerWgs84.x, centerWgs84.y, z);
            }
            polygon.add(position);
            resultBBox.addPoint(position);
        }
        return polygon;
    }

    private List<GaiaBuildingSurface> convertSurfaceProperty(AbstractCityObject cityObject, Classification classification, List<SurfaceProperty> surfaceProperties) {
        List<GaiaBuildingSurface> buildingSurfaces = new ArrayList<>();
        int surfaceCount = surfaceProperties.size();
        for(int aaa =0; aaa<surfaceCount; aaa++)
        {
            if(aaa == 34)
            {
                int hola = 0;
            }
        //for (SurfaceProperty surfaceProperty : surfaceProperties) {
            SurfaceProperty surfaceProperty = surfaceProperties.get(aaa);
            AbstractSurface abstractSurface = surfaceProperty.getObject();
            if (abstractSurface instanceof CompositeSurface compositeSurface) {
                List<GaiaBuildingSurface> compositeSurfaces = convertSurfaceProperty(cityObject, classification, compositeSurface.getSurfaceMembers());
                buildingSurfaces.addAll(compositeSurfaces);
                continue;
            }

            List<LinearRing> interiorRings = new ArrayList<>();
            LinearRing exteriorLinearRing = null;
            if (abstractSurface instanceof Polygon polygon) {
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
            List<List<Vector3d>> vec3InteriorPolygons = null;
            if(interiorRings.size() > 0)
            {
                vec3InteriorPolygons = new ArrayList<>();
                for(LinearRing interiorRing : interiorRings)
                {
                    List<Vector3d> vec3InteriorPolygon = convertLinearRingToPolygon(interiorRing, boundingBox);
                    vec3InteriorPolygons.add(vec3InteriorPolygon);
                }
            }
            Map<String, String> properties = new HashMap<>();
            properties.put("name", cityObject.getId());
            GaiaBuildingSurface gaiaBuildingSurface = GaiaBuildingSurface.builder()
                    .id(cityObject.getId())
                    .name(cityObject.getId())
                    .exteriorPositions(vec3Polygon)
                    .interiorPositions(vec3InteriorPolygons)
                    .boundingBox(boundingBox)
                    .classification(classification)
                    .properties(properties)
                    .build();
            buildingSurfaces.add(gaiaBuildingSurface);

//            List<GeometricPosition> geometricPositionList = exteriorLinearRing.getControlPoints().getGeometricPositions();
//            if (geometricPositionList != null && !geometricPositionList.isEmpty()) {
//                List<Vector3d> vec3Polygon = new ArrayList<>();
//                GaiaBoundingBox boundingBox = new GaiaBoundingBox();
//                for (GeometricPosition geometricPosition : geometricPositionList) {
//                    DirectPosition directPosition = geometricPosition.getPos();
//                    List<Double> positions  = directPosition.getValue();
//                    for (int i = 0; i < positions.size(); i += 3) {
//                        double x, y, z;
//                        x = positions.get(i);
//                        y = positions.get(i + 1);
//                        z = positions.get(i + 2);
//                        Vector3d position = new Vector3d(x, y, z);
//                        CoordinateReferenceSystem crs = globalOptions.getCrs();
//                        if (crs != null) {
//                            ProjCoordinate projCoordinate = new ProjCoordinate(x, y, z);
//                            ProjCoordinate centerWgs84 = GlobeUtils.transform(crs, projCoordinate);
//                            position = new Vector3d(centerWgs84.x, centerWgs84.y, z);
//                        }
//                        vec3Polygon.add(position);
//                        boundingBox.addPoint(position);
//                    }
//
//                    Map<String, String> properties = new HashMap<>();
//                    properties.put("name", cityObject.getId());
//                    GaiaBuildingSurface gaiaBuildingSurface = GaiaBuildingSurface.builder()
//                            .id(cityObject.getId())
//                            .name(cityObject.getId())
//                            //.interiorPositions()
//                            .exteriorPositions(vec3Polygon)
//                            .boundingBox(boundingBox)
//                            .classification(classification)
//                            .properties(properties)
//                            .build();
//                    buildingSurfaces.add(gaiaBuildingSurface);
//                }
//            }

//            DirectPositionList directPositionList = exteriorLinearRing.getControlPoints().getPosList();
//            if (directPositionList != null && directPositionList.getValue() != null) {
//                List<Vector3d> vec3Polygon = new ArrayList<>();
//                GaiaBoundingBox boundingBox = new GaiaBoundingBox();
//                List<Double> positions = directPositionList.getValue();
//                for (int i = 0; i < positions.size(); i += 3) {
//                    double x, y, z;
//                    x = positions.get(i);
//                    y = positions.get(i + 1);
//                    z = positions.get(i + 2);
//                    Vector3d position = new Vector3d(x, y, z);
//                    CoordinateReferenceSystem crs = globalOptions.getCrs();
//                    if (crs != null) {
//                        ProjCoordinate projCoordinate = new ProjCoordinate(x, y, boundingBox.getMinZ());
//                        ProjCoordinate centerWgs84 = GlobeUtils.transform(crs, projCoordinate);
//                        position = new Vector3d(centerWgs84.x, centerWgs84.y, z);
//                    }
//                    vec3Polygon.add(position);
//                    boundingBox.addPoint(position);
//                }
//                Map<String, String> properties = new HashMap<>();
//                properties.put("name", cityObject.getId());
//                GaiaBuildingSurface gaiaBuildingSurface = GaiaBuildingSurface.builder()
//                        .id(cityObject.getId())
//                        .name(cityObject.getId())
//                        .exteriorPositions(vec3Polygon)
//                        .boundingBox(boundingBox)
//                        .classification(classification)
//                        .properties(properties)
//                        .build();
//                buildingSurfaces.add(gaiaBuildingSurface);
//            }
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
        } else {
            log.debug("Unsupported city object type: {}", cityObject.getClass().getSimpleName());
        }

        int TEST_solidCount = 0;

        if (lod1Solid != null) {
            solids.add(lod1Solid);
            TEST_solidCount++;
        }
        if (lod2Solid != null) {
            solids.add(lod2Solid);
            TEST_solidCount++;
        }
        if (lod3Solid != null) {
            solids.add(lod3Solid);
            TEST_solidCount++;
        }

        if(TEST_solidCount > 1)
        {
            log.warn("Multiple solids found for city object: {}", cityObject.getId());
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
        } else if (cityObject instanceof Bridge object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            object.getBridgeParts().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
            object.getBoundaries().forEach(childProperty -> {
                boundaries.add(childProperty.getObject());
            });
        } else if (cityObject instanceof Tunnel object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
            object.getTunnelParts().forEach(childProperty -> {
                childCityObjects.add(childProperty.getObject());
            });
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
            log.debug("Unsupported city object type: {}", cityObject.getClass().getSimpleName());
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

        return multiSurfaces;
    }

    private List<MultiSurfaceProperty> extractMultiSurfaceProperty(AbstractSpaceBoundary spaceBoundary) {
        List<MultiSurfaceProperty> multiSurfaces = new ArrayList<>();
        MultiSurfaceProperty lod0MultiSurface = null;
        MultiSurfaceProperty lod1MultiSurface = null;
        MultiSurfaceProperty lod2MultiSurface = null;
        MultiSurfaceProperty lod3MultiSurface = null;
        MultiSurfaceProperty lod4MultiSurface = null;

        List<AbstractFillingSurfaceProperty> fillingSurfaceProperties = null;

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
        } else if (spaceBoundary instanceof OuterCeilingSurface object) {
            lod0MultiSurface = object.getLod0MultiSurface();
            lod1MultiSurface = object.getLod1MultiSurface();
            lod2MultiSurface = object.getLod2MultiSurface();
            lod3MultiSurface = object.getLod3MultiSurface();
        } else {
            log.debug("Unsupported space boundary type: {}", spaceBoundary.getClass().getSimpleName());
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

        return multiSurfaces;
    }

    private Classification getClassification(AbstractSpaceBoundary abstractSpaceBoundary) {
        if (abstractSpaceBoundary instanceof CeilingSurface) {
            return Classification.CEILING;
        } else if (abstractSpaceBoundary instanceof InteriorWallSurface) {
            return Classification.WALL;
        } else if (abstractSpaceBoundary instanceof FloorSurface) {
            return Classification.FLOOR;
        } else if (abstractSpaceBoundary instanceof GroundSurface) {
            return Classification.GROUND;
        } else if (abstractSpaceBoundary instanceof RoofSurface) {
            return Classification.ROOF;
        } else if (abstractSpaceBoundary instanceof WallSurface) {
            return Classification.WALL;
        } else if (abstractSpaceBoundary instanceof ClosureSurface) {
            return Classification.WALL;
        } else if (abstractSpaceBoundary instanceof DoorSurface) {
            return Classification.DOOR;
        } else if (abstractSpaceBoundary instanceof WindowSurface) {
            return Classification.WINDOW;
        } else if (abstractSpaceBoundary instanceof WaterSurface) {
            return Classification.WATER;
        } else {
            return Classification.UNKNOWN;
        }
    }

    private Classification getClassification(AbstractCityObject cityObject) {
        if (cityObject instanceof Building) {
            return Classification.WALL;
        } else if (cityObject instanceof BuildingPart) {
            return Classification.WALL;
        } else if (cityObject instanceof BuildingRoom) {
            return Classification.WALL;
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
        } else if (cityObject instanceof AbstractSpaceBoundary abstractSpaceBoundary) {
            return getClassification(abstractSpaceBoundary);
        }
        return Classification.UNKNOWN;
    }
}