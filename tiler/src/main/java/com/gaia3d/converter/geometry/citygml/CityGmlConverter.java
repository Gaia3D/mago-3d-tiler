package com.gaia3d.converter.geometry.citygml;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.*;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.geometry.*;
import com.gaia3d.converter.geometry.tessellator.GaiaTessellator;
import com.gaia3d.util.GlobeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.citygml4j.core.model.building.Building;
import org.citygml4j.core.model.building.BuildingRoom;
import org.citygml4j.core.model.construction.DoorSurface;
import org.citygml4j.core.model.construction.WindowSurface;
import org.citygml4j.core.model.core.AbstractCityObject;
import org.citygml4j.core.model.core.AbstractCityObjectProperty;
import org.citygml4j.core.model.core.CityModel;
import org.citygml4j.xml.CityGMLContext;
import org.citygml4j.xml.CityGMLContextException;
import org.citygml4j.xml.reader.CityGMLInputFactory;
import org.citygml4j.xml.reader.CityGMLReadException;
import org.citygml4j.xml.reader.CityGMLReader;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import org.xmlobjects.gml.model.geometry.DirectPositionList;
import org.xmlobjects.gml.model.geometry.aggregates.MultiSurface;
import org.xmlobjects.gml.model.geometry.aggregates.MultiSurfaceProperty;
import org.xmlobjects.gml.model.geometry.primitives.*;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

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

                // LOD1 Solid
                GaiaExtrusionBuilding building = convertLod1Solid(cityObject);
                if (building != null) {
                    buildingList.add(building);
                }

                // LOD1 MultiSurface
                List<GaiaBuildingSurface> lod1Surfaces = convertLod1(cityObject);
                if (!lod1Surfaces.isEmpty()) {
                    buildingSurfacesList.add(lod1Surfaces);
                }

                // LOD2 MultiSurface
                List<GaiaBuildingSurface> lod2Surfaces = convertLod2(cityObject);
                if (!lod2Surfaces.isEmpty()) {
                    buildingSurfacesList.add(lod2Surfaces);
                }

                // LOD3 MultiSurface
                List<GaiaBuildingSurface> lod3Surfaces = convertLod3(cityObject);
                if (!lod3Surfaces.isEmpty()) {
                    buildingSurfacesList.add(lod3Surfaces);
                }
            }

            for (GaiaExtrusionBuilding gaiaBuilding : buildingList) {
                GaiaScene scene = initScene();
                scene.setOriginalPath(file.toPath());
                GaiaMaterial material = scene.getMaterials().get(0);
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
                //log.info("Building Surface Size: {}", surfaces.size());

                GaiaScene scene = initScene();
                scene.setOriginalPath(file.toPath());
                GaiaMaterial material = scene.getMaterials().get(0);
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

                List<List<Vector3d>> polygons = new ArrayList<>();
                for (GaiaBuildingSurface buildingSurface : surfaces) {
                    List<Vector3d> polygon = new ArrayList<>();

                    List<Vector3d> localPositions = new ArrayList<>();
                    for (Vector3d position : buildingSurface.getPositions()) {
                        Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                        Vector3d localPosition = positionWorldCoordinate.mulPosition(transfromMatrixInv);
                        localPosition.z = position.z;
                        localPositions.add(localPosition);
                        polygon.add(localPosition);
                    }
                    polygons.add(polygon);
                }

                GaiaNode node = new GaiaNode();
                node.setTransformMatrix(new Matrix4d().identity());
                GaiaMesh mesh = new GaiaMesh();
                node.getMeshes().add(mesh);
                GaiaPrimitive primitive = createPrimitiveFromPolygons(polygons);
                primitive.setMaterialIndex(0);
                mesh.getPrimitives().add(primitive);
                rootNode.getChildren().add(node);

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

    private GaiaExtrusionBuilding convertLod1Solid(AbstractCityObject cityObject) {
        boolean flipCoordinate = globalOptions.isFlipCoordinate();
        double skirtHeight = globalOptions.getSkirtHeight();

        // Building DoorSurface WindowSurface BuildingRoom

        double height = 1.0d;
        SolidProperty lod1SolidProperty = null;
        if (cityObject instanceof Building) {
            Building building = (Building) cityObject;
            lod1SolidProperty = building.getLod1Solid();
            height = getHeight(building);
        } else if (cityObject instanceof BuildingRoom) {
            BuildingRoom buildingRoom = (BuildingRoom) cityObject;
            lod1SolidProperty = buildingRoom.getLod1Solid();
            height = getHeight(buildingRoom);
        } else if (cityObject instanceof DoorSurface) {

        } else if (cityObject instanceof WindowSurface) {

        } else {
            log.error("Unsupported city object: {}", cityObject.getClass().getName());
        }

        if (lod1SolidProperty == null) {
            return null;
        }

        AbstractSolid solid = lod1SolidProperty.getObject();

        Shell shell = ((Solid) solid).getExterior().getObject();
        List<SurfaceProperty> surfaceProperties = shell.getSurfaceMembers();

        GaiaExtrusionBuilding gaiaBuilding = GaiaExtrusionBuilding.builder()
                .id(cityObject.getId())
                .name(cityObject.getId())
                .floorHeight(0)
                .roofHeight(height)
                .build();

        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        for (SurfaceProperty surfaceProperty : surfaceProperties) {
            List<Vector3d> polygon = new Vector<>();

            Polygon surface = (Polygon) surfaceProperty.getObject();
            LinearRing linearRing = (LinearRing)surface.getExterior().getObject();
            DirectPositionList directPositions = linearRing.getControlPoints().getPosList();
            List<Double> positions = directPositions.getValue();

            double heightSum = 0d;
            for (int i = 0; i < positions.size(); i+=3) {
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
        }
        gaiaBuilding.setBoundingBox(boundingBox);
        return gaiaBuilding;
    }

    private List<GaiaBuildingSurface> convertLod1(AbstractCityObject cityObject) {
        //MultiSurfaceProperty lod2MultiSurface = building.getLod2MultiSurface();
        List<GaiaBuildingSurface> buildingSurfaces = new ArrayList<>();

        MultiSurfaceProperty multiSurfaceProperty = null;
        if (cityObject instanceof Building) {
            return buildingSurfaces;
        } else if (cityObject instanceof DoorSurface) {
            DoorSurface doorSurface = (DoorSurface) cityObject;
            multiSurfaceProperty = doorSurface.getLod1MultiSurface();
        } else if (cityObject instanceof WindowSurface) {
            WindowSurface windowSurface = (WindowSurface) cityObject;
            multiSurfaceProperty = windowSurface.getLod1MultiSurface();
        } else if (cityObject instanceof Building) {

        } else if (cityObject instanceof BuildingRoom) {

        } else {
            log.error("Unsupported city lod1 object: {}", cityObject.getClass().getName());
        }

        if (multiSurfaceProperty == null) {
            return buildingSurfaces;
        }

        MultiSurface multiSurface = multiSurfaceProperty.getObject();
        //SurfaceArrayProperty surfaceArrayProperty = multiSurface.getSurfaceMembers();
        List<SurfaceProperty> surfaceProperties = multiSurface.getSurfaceMember();

        if (surfaceProperties.size() < 1) {
            log.error("No surface properties found for city object: {}", cityObject.getId());
        }

        for (SurfaceProperty surfaceProperty : surfaceProperties) {
            Polygon polygon = (Polygon) surfaceProperty.getObject();
            LinearRing linearRing = (LinearRing) polygon.getExterior().getObject();

            List<Vector3d> vec3Polygon = new ArrayList<>();
            GaiaBoundingBox boundingBox = new GaiaBoundingBox();

            DirectPositionList directPositionList = linearRing.getControlPoints().getPosList();
            List<Double> positions = directPositionList.getValue();
            for (int i = 0; i < positions.size(); i+=3) {
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
                    .build();
            buildingSurfaces.add(gaiaBuildingSurface);
        }
        return buildingSurfaces;
    }

    private List<GaiaBuildingSurface> convertLod2(AbstractCityObject cityObject) {
        //MultiSurfaceProperty lod2MultiSurface = building.getLod2MultiSurface();
        List<GaiaBuildingSurface> buildingSurfaces = new ArrayList<>();

        MultiSurfaceProperty multiSurfaceProperty = null;
        if (cityObject instanceof Building) {
            Building building = (Building) cityObject;
            multiSurfaceProperty = building.getLod2MultiSurface();
        } else if (cityObject instanceof DoorSurface) {
            DoorSurface doorSurface = (DoorSurface) cityObject;
            multiSurfaceProperty = doorSurface.getLod2MultiSurface();
        } else if (cityObject instanceof WindowSurface) {
            WindowSurface windowSurface = (WindowSurface) cityObject;
            multiSurfaceProperty = windowSurface.getLod2MultiSurface();
        } else if (cityObject instanceof BuildingRoom) {
            BuildingRoom buildingRoom = (BuildingRoom) cityObject;
            multiSurfaceProperty = buildingRoom.getLod2MultiSurface();
        } else {
            log.error("Unsupported city lod2 object: {}", cityObject.getClass().getName());
        }

        if (multiSurfaceProperty == null) {
            return buildingSurfaces;
        }

        MultiSurface multiSurface = multiSurfaceProperty.getObject();
        //SurfaceArrayProperty surfaceArrayProperty = multiSurface.getSurfaceMembers();
        List<SurfaceProperty> surfaceProperties = multiSurface.getSurfaceMember();

        if (surfaceProperties.size() < 1) {
            log.error("No surface properties found for city object: {}", cityObject.getId());
        }

        for (SurfaceProperty surfaceProperty : surfaceProperties) {
            Polygon polygon = (Polygon) surfaceProperty.getObject();
            LinearRing linearRing = (LinearRing) polygon.getExterior().getObject();

            List<Vector3d> vec3Polygon = new ArrayList<>();
            GaiaBoundingBox boundingBox = new GaiaBoundingBox();

            DirectPositionList directPositionList = linearRing.getControlPoints().getPosList();
            List<Double> positions = directPositionList.getValue();
            for (int i = 0; i < positions.size(); i+=3) {
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
                    .build();
            buildingSurfaces.add(gaiaBuildingSurface);
        }
        return buildingSurfaces;
    }

    private List<GaiaBuildingSurface> convertLod3(AbstractCityObject cityObject) {
        //MultiSurfaceProperty lod3MultiSurface = cityObject.getLod3MultiSurface();

        MultiSurfaceProperty multiSurfaceProperty = null;
        if (cityObject instanceof Building) {
            Building building = (Building) cityObject;
            multiSurfaceProperty = building.getLod3MultiSurface();
        } else if (cityObject instanceof DoorSurface) {
            DoorSurface doorSurface = (DoorSurface) cityObject;
            multiSurfaceProperty = doorSurface.getLod3MultiSurface();
        } else if (cityObject instanceof WindowSurface) {
            WindowSurface windowSurface = (WindowSurface) cityObject;
            multiSurfaceProperty = windowSurface.getLod3MultiSurface();
        } else if (cityObject instanceof BuildingRoom) {
            BuildingRoom buildingRoom = (BuildingRoom) cityObject;
            multiSurfaceProperty = buildingRoom.getLod3MultiSurface();
        } else {
            log.error("Unsupported city object: {}", cityObject.getClass().getName());
        }

        List<GaiaBuildingSurface> buildingSurfaces = new ArrayList<>();
        if (multiSurfaceProperty == null) {
            return buildingSurfaces;
        }

        MultiSurface multiSurface = multiSurfaceProperty.getObject();
        //SurfaceArrayProperty surfaceArrayProperty = multiSurface.getSurfaceMembers();
        List<SurfaceProperty> surfaceProperties = multiSurface.getSurfaceMember();

        if (surfaceProperties.size() < 1) {
            log.error("No surface properties found for city object: {}", cityObject.getId());
        }

        for (SurfaceProperty surfaceProperty : surfaceProperties) {
            Polygon polygon = (Polygon) surfaceProperty.getObject();
            LinearRing linearRing = (LinearRing) polygon.getExterior().getObject();

            List<Vector3d> vec3Polygon = new ArrayList<>();
            GaiaBoundingBox boundingBox = new GaiaBoundingBox();

            DirectPositionList directPositionList = linearRing.getControlPoints().getPosList();
            List<Double> positions = directPositionList.getValue();
            for (int i = 0; i < positions.size(); i+=3) {
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
                    .build();
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
        if (buildingRoom.getRoomHeights().size() > 0) {
            return buildingRoom.getRoomHeights().get(0).getObject().getValue().getValue();
        } else {
            return 0.0d;
        }
    }

    protected GaiaPrimitive createPrimitiveFromPolygons(List<List<Vector3d>> polygons) {
        GaiaTessellator tessellator = new GaiaTessellator();

        GaiaPrimitive primitive = new GaiaPrimitive();
        List<GaiaVertex> vertexList = new ArrayList<>();
        //Map<GaiaVertex, Integer> vertexMap = new HashMap<>();
        Map<Vector3d, Integer> pointsMap = new HashMap<>();

        int polygonCount = polygons.size();
        for (List<Vector3d> polygon : polygons) {

            Vector3d normal = new Vector3d();
            tessellator.calculateNormal3D(polygon, normal);

            for (Vector3d vector3d : polygon) {
                GaiaVertex vertex = new GaiaVertex();
                vertex.setPosition(vector3d);
                vertex.setNormal(new Vector3d(normal));
                vertexList.add(vertex);
            }
        }

        int vertexCount = vertexList.size();
        for (int m = 0; m < vertexCount; m++) {
            GaiaVertex vertex = vertexList.get(m);
            //vertexMap.put(vertex, m);
            pointsMap.put(vertex.getPosition(), m);
        }

        primitive.setVertices(vertexList); // total vertex list.***

        List<Integer> resultTrianglesIndices = new ArrayList<>();

        for (int m = 0; m < polygonCount; m++) {
            GaiaSurface surface = new GaiaSurface();
            primitive.getSurfaces().add(surface);

            int idx1Local = -1;
            int idx2Local = -1;
            int idx3Local = -1;

            List<Vector3d> polygon = polygons.get(m);
            resultTrianglesIndices.clear();
            tessellator.tessellate3D(polygon, resultTrianglesIndices);

            int indicesCount = resultTrianglesIndices.size();
            int trianglesCount = indicesCount / 3;
            for (int n = 0; n < trianglesCount; n++) {
                idx1Local = resultTrianglesIndices.get(n * 3);
                idx2Local = resultTrianglesIndices.get(n * 3 + 1);
                idx3Local = resultTrianglesIndices.get(n * 3 + 2);

                Vector3d point1 = polygon.get(idx1Local);
                Vector3d point2 = polygon.get(idx2Local);
                Vector3d point3 = polygon.get(idx3Local);

                int idx1 = pointsMap.get(point1);
                int idx2 = pointsMap.get(point2);
                int idx3 = pointsMap.get(point3);

                GaiaFace face = new GaiaFace();
                int[] indicesArray = new int[3];
                indicesArray[0] = idx1;
                indicesArray[1] = idx2;
                indicesArray[2] = idx3;
                face.setIndices(indicesArray);
                surface.getFaces().add(face);
            }
        }

        return primitive;
    }
}