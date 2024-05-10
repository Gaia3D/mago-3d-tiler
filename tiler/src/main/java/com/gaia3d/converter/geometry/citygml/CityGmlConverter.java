package com.gaia3d.converter.geometry.citygml;

import com.gaia3d.basic.geometry.Classification;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.*;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.geometry.*;
import com.gaia3d.util.GlobeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.citygml4j.core.model.building.*;
import org.citygml4j.core.model.construction.*;
import org.citygml4j.core.model.core.*;
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

import java.awt.*;
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
                GaiaScene scene = initScene();
                scene.setOriginalPath(file.toPath());
                GaiaMaterial material = getMaterialByClassification(scene.getMaterials(), gaiaBuilding.getClassification());
                GaiaNode rootNode = scene.getNodes().get(0);

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

                GaiaScene scene = initScene();
                scene.setOriginalPath(file.toPath());
                GaiaNode rootNode = scene.getNodes().get(0);

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
                    List<List<Vector3d>> polygons = new ArrayList<>();
                    List<Vector3d> polygon = new ArrayList<>();

                    List<Vector3d> localPositions = new ArrayList<>();
                    for (Vector3d position : buildingSurface.getPositions()) {
                        Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                        Vector3d localPosition = positionWorldCoordinate.mulPosition(transfromMatrixInv);
                        localPositions.add(localPosition);
                        polygon.add(new Vector3dOnlyHashEquals(localPosition));
                    }
                    polygons.add(polygon);

                    GaiaNode node = new GaiaNode();
                    node.setTransformMatrix(new Matrix4d().identity());
                    GaiaMesh mesh = new GaiaMesh();
                    node.getMeshes().add(mesh);
                    GaiaPrimitive primitive = createPrimitiveFromPolygons(polygons);
                    primitive.setMaterialIndex(material.getId());
                    mesh.getPrimitives().add(primitive);
                    rootNode.getChildren().add(node);
                }
                /*GaiaNode node = new GaiaNode();
                node.setTransformMatrix(new Matrix4d().identity());
                GaiaMesh mesh = new GaiaMesh();
                node.getMeshes().add(mesh);
                GaiaPrimitive primitive = createPrimitiveFromPolygons(polygons);
                primitive.setMaterialIndex(material.getId());
                mesh.getPrimitives().add(primitive);
                rootNode.getChildren().add(node);*/

                Matrix4d rootTransformMatrix = new Matrix4d().identity();
                rootTransformMatrix.translate(center, rootTransformMatrix);
                rootNode.setTransformMatrix(rootTransformMatrix);
                scenes.add(scene);
            }

        } catch (CityGMLContextException | CityGMLReadException e) {
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
            GaiaExtrusionBuilding gaiaBuilding = GaiaExtrusionBuilding.builder()
                    .id(cityObject.getId())
                    .name(cityObject.getId())
                    .floorHeight(0)
                    .roofHeight(height)
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
        if (surfaceProperties.size() < 1) {
            log.error("No surface properties found for city object: {}", cityObject.getId());
            return buildingSurfaces;
        }

        Classification classification = getClassification((AbstractSpaceBoundary) multiSurfaceProperty.getParent());
        buildingSurfaces = convertSurfaceProperty(cityObject, classification, surfaceProperties);
        return buildingSurfaces;
    }

    private List<GaiaBuildingSurface> convertSurfaceProperty(AbstractCityObject cityObject, Classification classification, List<SurfaceProperty> surfaceProperties) {
        List<GaiaBuildingSurface> buildingSurfaces = new ArrayList<>();
        for (SurfaceProperty surfaceProperty : surfaceProperties) {
            AbstractSurface abstractSurface = surfaceProperty.getObject();
            if (abstractSurface instanceof CompositeSurface compositeSurface) {
                List<GaiaBuildingSurface> compositeSurfaces = convertSurfaceProperty(cityObject, classification, compositeSurface.getSurfaceMembers());
                buildingSurfaces.addAll(compositeSurfaces);
                continue;
            }

            LinearRing linearRing = null;
            if (abstractSurface instanceof Polygon polygon) {
                linearRing = (LinearRing) polygon.getExterior().getObject();
            } else {
                log.error("Unsupported surface type: {}", abstractSurface.getClass().getSimpleName());
                continue;
            }

            List<GeometricPosition> geometricPositionList = linearRing.getControlPoints().getGeometricPositions();
            if (geometricPositionList != null && !geometricPositionList.isEmpty()) {
                List<Vector3d> vec3Polygon = new ArrayList<>();
                GaiaBoundingBox boundingBox = new GaiaBoundingBox();
                for (GeometricPosition geometricPosition : geometricPositionList) {
                    DirectPosition directPosition = geometricPosition.getPos();
                    List<Double> positions  = directPosition.getValue();
                    for (int i = 0; i < positions.size(); i += 3) {
                        double x, y, z;
                        x = positions.get(i);
                        y = positions.get(i + 1);
                        z = positions.get(i + 2);
                        Vector3d position = new Vector3d(x, y, z);
                        CoordinateReferenceSystem crs = globalOptions.getCrs();
                        if (crs != null) {
                            ProjCoordinate projCoordinate = new ProjCoordinate(x, y, z);
                            ProjCoordinate centerWgs84 = GlobeUtils.transform(crs, projCoordinate);
                            position = new Vector3d(centerWgs84.x, centerWgs84.y, z);
                        }
                        vec3Polygon.add(position);
                        boundingBox.addPoint(position);
                    }
                    GaiaBuildingSurface gaiaBuildingSurface = GaiaBuildingSurface.builder()
                            .id(cityObject.getId())
                            .name(cityObject.getId())
                            .positions(vec3Polygon)
                            .boundingBox(boundingBox)
                            .classification(classification)
                            .build();
                    buildingSurfaces.add(gaiaBuildingSurface);
                }
            }

            DirectPositionList directPositionList = linearRing.getControlPoints().getPosList();
            if (directPositionList != null && directPositionList.getValue() != null) {
                List<Vector3d> vec3Polygon = new ArrayList<>();
                GaiaBoundingBox boundingBox = new GaiaBoundingBox();
                List<Double> positions = directPositionList.getValue();
                for (int i = 0; i < positions.size(); i += 3) {
                    double x, y, z;
                    x = positions.get(i);
                    y = positions.get(i + 1);
                    z = positions.get(i + 2);
                    Vector3d position = new Vector3d(x, y, z);
                    CoordinateReferenceSystem crs = globalOptions.getCrs();
                    if (crs != null) {
                        ProjCoordinate projCoordinate = new ProjCoordinate(x, y, boundingBox.getMinZ());
                        ProjCoordinate centerWgs84 = GlobeUtils.transform(crs, projCoordinate);
                        position = new Vector3d(centerWgs84.x, centerWgs84.y, z);
                    }
                    vec3Polygon.add(position);
                    boundingBox.addPoint(position);
                }
                GaiaBuildingSurface gaiaBuildingSurface = GaiaBuildingSurface.builder()
                        .id(cityObject.getId())
                        .name(cityObject.getId())
                        .positions(vec3Polygon)
                        .boundingBox(boundingBox)
                        .classification(getClassification(cityObject))
                        .build();
                buildingSurfaces.add(gaiaBuildingSurface);
            }
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
        if (buildingRoom.getRoomHeights().size() > 0) {
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

        if (cityObject instanceof Building building) {
            lod1Solid = building.getLod1Solid();
            lod2Solid = building.getLod2Solid();
            lod3Solid = building.getLod3Solid();
            buildingRoomProperties = building.getBuildingRooms();
            buildingPartProperties = building.getBuildingParts();
        } else if (cityObject instanceof BuildingPart buildingPart) {
            lod1Solid = buildingPart.getLod1Solid();
            lod2Solid = buildingPart.getLod2Solid();
            lod3Solid = buildingPart.getLod3Solid();
            buildingRoomProperties = buildingPart.getBuildingRooms();
        } else if (cityObject instanceof BuildingRoom buildingRoom) {
            lod1Solid = buildingRoom.getLod1Solid();
            lod2Solid = buildingRoom.getLod2Solid();
            lod3Solid = buildingRoom.getLod3Solid();
        }

        if (lod1Solid != null) {
            solids.add(lod1Solid);
        }
        if (lod2Solid != null) {
            solids.add(lod2Solid);
        }
        if (lod3Solid != null) {
            solids.add(lod3Solid);
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
        List<AbstractSpaceBoundaryProperty> boundaries = null;
        List<BuildingRoomProperty> buildingRoomProperties = null;
        List<BuildingPartProperty> buildingPartProperties = null;

        if (cityObject instanceof Building building) {
            lod0MultiSurface = building.getLod0MultiSurface();
            lod2MultiSurface = building.getLod2MultiSurface();
            lod3MultiSurface = building.getLod3MultiSurface();
            boundaries = building.getBoundaries();
            buildingRoomProperties = building.getBuildingRooms();
            buildingPartProperties = building.getBuildingParts();
        } else if (cityObject instanceof BuildingPart buildingPart) {
            lod0MultiSurface = buildingPart.getLod0MultiSurface();
            lod2MultiSurface = buildingPart.getLod2MultiSurface();
            lod3MultiSurface = buildingPart.getLod3MultiSurface();
            boundaries = buildingPart.getBoundaries();
            buildingRoomProperties = buildingPart.getBuildingRooms();
        } else if (cityObject instanceof BuildingRoom buildingRoom) {
            lod0MultiSurface = buildingRoom.getLod0MultiSurface();
            lod2MultiSurface = buildingRoom.getLod2MultiSurface();
            lod3MultiSurface = buildingRoom.getLod3MultiSurface();
            boundaries = buildingRoom.getBoundaries();
        } else if (cityObject instanceof DoorSurface doorSurface) {
            lod0MultiSurface = doorSurface.getLod0MultiSurface();
            lod1MultiSurface = doorSurface.getLod1MultiSurface();
            lod2MultiSurface = doorSurface.getLod2MultiSurface();
            lod3MultiSurface = doorSurface.getLod3MultiSurface();
        } else if (cityObject instanceof WindowSurface windowSurface) {
            lod0MultiSurface = windowSurface.getLod0MultiSurface();
            lod1MultiSurface = windowSurface.getLod1MultiSurface();
            lod2MultiSurface = windowSurface.getLod2MultiSurface();
            lod3MultiSurface = windowSurface.getLod3MultiSurface();
        } else if (cityObject instanceof ClosureSurface closureSurface) {
            lod0MultiSurface = closureSurface.getLod0MultiSurface();
            lod1MultiSurface = closureSurface.getLod1MultiSurface();
            lod2MultiSurface = closureSurface.getLod2MultiSurface();
            lod3MultiSurface = closureSurface.getLod3MultiSurface();
        } else if (cityObject instanceof CeilingSurface ceilingSurface) {
            lod0MultiSurface = ceilingSurface.getLod0MultiSurface();
            lod1MultiSurface = ceilingSurface.getLod1MultiSurface();
            lod2MultiSurface = ceilingSurface.getLod2MultiSurface();
            lod3MultiSurface = ceilingSurface.getLod3MultiSurface();
        } else if (cityObject instanceof InteriorWallSurface interiorWallSurface) {
            lod0MultiSurface = interiorWallSurface.getLod0MultiSurface();
            lod1MultiSurface = interiorWallSurface.getLod1MultiSurface();
            lod2MultiSurface = interiorWallSurface.getLod2MultiSurface();
            lod3MultiSurface = interiorWallSurface.getLod3MultiSurface();
        } else if (cityObject instanceof FloorSurface floorSurface) {
            lod0MultiSurface = floorSurface.getLod0MultiSurface();
            lod1MultiSurface = floorSurface.getLod1MultiSurface();
            lod2MultiSurface = floorSurface.getLod2MultiSurface();
            lod3MultiSurface = floorSurface.getLod3MultiSurface();
        } else if (cityObject instanceof GroundSurface groundSurface) {
            lod0MultiSurface = groundSurface.getLod0MultiSurface();
            lod1MultiSurface = groundSurface.getLod1MultiSurface();
            lod2MultiSurface = groundSurface.getLod2MultiSurface();
            lod3MultiSurface = groundSurface.getLod3MultiSurface();
        } else if (cityObject instanceof RoofSurface roofSurface) {
            lod0MultiSurface = roofSurface.getLod0MultiSurface();
            lod1MultiSurface = roofSurface.getLod1MultiSurface();
            lod2MultiSurface = roofSurface.getLod2MultiSurface();
            lod3MultiSurface = roofSurface.getLod3MultiSurface();
        } else if (cityObject instanceof WallSurface wallSurface) {
            lod0MultiSurface = wallSurface.getLod0MultiSurface();
            lod1MultiSurface = wallSurface.getLod1MultiSurface();
            lod2MultiSurface = wallSurface.getLod2MultiSurface();
            lod3MultiSurface = wallSurface.getLod3MultiSurface();
        } else if (cityObject instanceof AbstractSpaceBoundary abstractSpaceBoundary) {
            List<MultiSurfaceProperty> multiSurfaceProperties = extractMultiSurfaceProperty(abstractSpaceBoundary);
            multiSurfaces.addAll(multiSurfaceProperties);
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

        if (boundaries != null && !boundaries.isEmpty()) {
            for (AbstractSpaceBoundaryProperty boundary : boundaries) {
                AbstractSpaceBoundary spaceBoundary = boundary.getObject();
                List<MultiSurfaceProperty> multiSurfaceProperties = extractMultiSurfaceProperty(spaceBoundary);
                multiSurfaces.addAll(multiSurfaceProperties);
            }
        }

        if (buildingRoomProperties != null && !buildingRoomProperties.isEmpty()) {
            for (BuildingRoomProperty buildingRoomProperty : buildingRoomProperties) {
                BuildingRoom buildingRoom = buildingRoomProperty.getObject();
                List<MultiSurfaceProperty> multiSurfaceProperties = extractMultiSurfaceProperty(buildingRoom);
                multiSurfaces.addAll(multiSurfaceProperties);
            }
        }

        if (buildingPartProperties != null && !buildingPartProperties.isEmpty()) {
            for (BuildingPartProperty buildingPartProperty : buildingPartProperties) {
                BuildingPart buildingPart = buildingPartProperty.getObject();
                List<MultiSurfaceProperty> multiSurfaceProperties = extractMultiSurfaceProperty(buildingPart);
                multiSurfaces.addAll(multiSurfaceProperties);
            }
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

        if (spaceBoundary instanceof CeilingSurface ceilingSurface) {
            lod0MultiSurface = ceilingSurface.getLod0MultiSurface();
            lod1MultiSurface = ceilingSurface.getLod1MultiSurface();
            lod2MultiSurface = ceilingSurface.getLod2MultiSurface();
            lod3MultiSurface = ceilingSurface.getLod3MultiSurface();
            lod4MultiSurface = ceilingSurface.getDeprecatedProperties().getLod4MultiSurface();
            fillingSurfaceProperties = ceilingSurface.getFillingSurfaces();
        } else if (spaceBoundary instanceof InteriorWallSurface interiorWallSurface) {
            lod0MultiSurface = interiorWallSurface.getLod0MultiSurface();
            lod1MultiSurface = interiorWallSurface.getLod1MultiSurface();
            lod2MultiSurface = interiorWallSurface.getLod2MultiSurface();
            lod3MultiSurface = interiorWallSurface.getLod3MultiSurface();
            lod4MultiSurface = interiorWallSurface.getDeprecatedProperties().getLod4MultiSurface();
            fillingSurfaceProperties = interiorWallSurface.getFillingSurfaces();
        } else if (spaceBoundary instanceof FloorSurface floorSurface) {
            lod0MultiSurface = floorSurface.getLod0MultiSurface();
            lod1MultiSurface = floorSurface.getLod1MultiSurface();
            lod2MultiSurface = floorSurface.getLod2MultiSurface();
            lod3MultiSurface = floorSurface.getLod3MultiSurface();
            lod4MultiSurface = floorSurface.getDeprecatedProperties().getLod4MultiSurface();
            fillingSurfaceProperties = floorSurface.getFillingSurfaces();
        } else if (spaceBoundary instanceof GroundSurface groundSurface) {
            lod0MultiSurface = groundSurface.getLod0MultiSurface();
            lod1MultiSurface = groundSurface.getLod1MultiSurface();
            lod2MultiSurface = groundSurface.getLod2MultiSurface();
            lod3MultiSurface = groundSurface.getLod3MultiSurface();
            lod4MultiSurface = groundSurface.getDeprecatedProperties().getLod4MultiSurface();
            fillingSurfaceProperties = groundSurface.getFillingSurfaces();
        } else if (spaceBoundary instanceof RoofSurface roofSurface) {
            lod0MultiSurface = roofSurface.getLod0MultiSurface();
            lod1MultiSurface = roofSurface.getLod1MultiSurface();
            lod2MultiSurface = roofSurface.getLod2MultiSurface();
            lod3MultiSurface = roofSurface.getLod3MultiSurface();
            lod4MultiSurface = roofSurface.getDeprecatedProperties().getLod4MultiSurface();
            fillingSurfaceProperties = roofSurface.getFillingSurfaces();
        } else if (spaceBoundary instanceof WallSurface wallSurface) {
            lod0MultiSurface = wallSurface.getLod0MultiSurface();
            lod1MultiSurface = wallSurface.getLod1MultiSurface();
            lod2MultiSurface = wallSurface.getLod2MultiSurface();
            lod3MultiSurface = wallSurface.getLod3MultiSurface();
            lod4MultiSurface = wallSurface.getDeprecatedProperties().getLod4MultiSurface();
            fillingSurfaceProperties = wallSurface.getFillingSurfaces();
        } else if (spaceBoundary instanceof ClosureSurface closureSurface) {
            lod0MultiSurface = closureSurface.getLod0MultiSurface();
            lod1MultiSurface = closureSurface.getLod1MultiSurface();
            lod2MultiSurface = closureSurface.getLod2MultiSurface();
            lod3MultiSurface = closureSurface.getLod3MultiSurface();
            lod4MultiSurface = closureSurface.getDeprecatedProperties().getLod4MultiSurface();
        } else if (spaceBoundary instanceof DoorSurface doorSurface) {
            lod0MultiSurface = doorSurface.getLod0MultiSurface();
            lod1MultiSurface = doorSurface.getLod1MultiSurface();
            lod2MultiSurface = doorSurface.getLod2MultiSurface();
            lod3MultiSurface = doorSurface.getLod3MultiSurface();
            lod4MultiSurface = doorSurface.getDeprecatedProperties().getLod4MultiSurface();
        } else if (spaceBoundary instanceof WindowSurface windowSurface) {
            lod0MultiSurface = windowSurface.getLod0MultiSurface();
            lod1MultiSurface = windowSurface.getLod1MultiSurface();
            lod2MultiSurface = windowSurface.getLod2MultiSurface();
            lod3MultiSurface = windowSurface.getLod3MultiSurface();
            lod4MultiSurface = windowSurface.getDeprecatedProperties().getLod4MultiSurface();
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
            return Classification.FLOOR;
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
        } else if (cityObject instanceof AbstractSpaceBoundary) {
            return Classification.WALL;
        }
        return Classification.UNKNOWN;
    }
}