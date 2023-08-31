package com.gaia3d.converter.citygml;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.*;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.geometry.*;
import com.gaia3d.util.GlobeUtils;
import lombok.extern.slf4j.Slf4j;
import org.citygml4j.core.model.building.Building;
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
import org.joml.Vector4d;
import org.xmlobjects.gml.model.geometry.DirectPositionList;
import org.xmlobjects.gml.model.geometry.primitives.*;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;

@Slf4j
public class CityGmlConverter implements Converter {

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

    private List<GaiaScene> convert(File file) {
        List<GaiaScene> scenes = new ArrayList<>();

        GaiaScene scene = initScene();
        scene.setOriginalPath(file.toPath());
        GaiaMaterial material = scene.getMaterials().get(0);
        GaiaNode rootNode = scene.getNodes().get(0);
        try {
            Tessellator tessellator = new Tessellator();
            Extruder extruder = new Extruder(tessellator);

            CityGMLContext context = CityGMLContext.newInstance();
            CityGMLInputFactory factory = context.createCityGMLInputFactory();
            CityGMLReader reader = factory.createCityGMLReader(file);
            CityModel cityModel = (CityModel) reader.next();
            List<String> names = new ArrayList<>();
            GaiaBoundingBox boundingBox = new GaiaBoundingBox();


            //List<List<Vector3d>> polygons = new ArrayList<>();
            List<GaiaBuilding> gaiaBuildings = new ArrayList<>();

            List<AbstractCityObjectProperty> cityObjectMembers = cityModel.getCityObjectMembers();
            for (AbstractCityObjectProperty cityObjectProperty : cityObjectMembers) {
                AbstractCityObject cityObject = cityObjectProperty.getObject();

                Building building = (Building) cityObject;
                SolidProperty solidProperty= building.getLod1Solid();
                AbstractSolid solid = solidProperty.getObject();

                Shell shell = ((Solid) solid).getExterior().getObject();
                List<SurfaceProperty> surfaceProperties = shell.getSurfaceMembers();

                if (building.getHeights().size() < 1) {
                    continue;
                }

                double height = building.getHeights().get(0).getObject().getValue().getValue();

                GaiaBuilding gaiaBuilding = GaiaBuilding.builder()
                        .id(cityObject.getId())
                        .floorHeight(0)
                        .roofHeight(height)
                        .build();

                for (SurfaceProperty surfaceProperty : surfaceProperties) {
                    List<Vector3d> polygon = new Vector<>();

                    Polygon surface = (Polygon) surfaceProperty.getObject();
                    LinearRing linearRing = (LinearRing)surface.getExterior().getObject();
                    DirectPositionList directPositionList = linearRing.getControlPoints().getPosList();
                    List<Double> values = directPositionList.getValue();

                    double value = 0d;
                    for (int i = 0; i < values.size(); i+=3) {
                        double x = values.get(i + 1);
                        double y = values.get(i);
                        //double z = values.get(i + 2);
                        value += values.get(i + 2);
                        double z = 0.0d;
                        Vector3d position = new Vector3d(x, y, z);
                        polygon.add(position);
                        boundingBox.addPoint(position);
                    }

                    double floorHeight = value / values.size();
                    gaiaBuilding.setPositions(polygon);
                    gaiaBuilding.setFloorHeight(floorHeight);
                    gaiaBuilding.setRoofHeight(floorHeight + height);

                    names.add(cityObject.getId());
                    break;
                }
                gaiaBuildings.add(gaiaBuilding);
            }

            Vector3d center = boundingBox.getCenter();
            for (GaiaBuilding gaiaBuilding : gaiaBuildings) {
                Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
                Matrix4d transformMatrix = GlobeUtils.normalAtCartesianPointWgs84(centerWorldCoordinate);
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
            }

            Matrix4d transformMatrix = new Matrix4d().identity();
            transformMatrix.translate(center, transformMatrix);

            rootNode.setTransformMatrix(transformMatrix);

        } catch (CityGMLContextException e) {
            throw new RuntimeException(e);
        } catch (CityGMLReadException e) {
            throw new RuntimeException(e);
        }

        scenes.add(scene);
        return scenes;
    }

    private GaiaScene initScene() {
        GaiaScene scene = new GaiaScene();
        GaiaMaterial material = new GaiaMaterial();
        material.setId(0);
        material.setName("color");
        material.setDiffuseColor(new Vector4d(0.5, 0.5, 0.5, 1));
        Map<TextureType, List<GaiaTexture>> textureTypeListMap = material.getTextures();
        textureTypeListMap.put(TextureType.DIFFUSE, new ArrayList<>());
        scene.getMaterials().add(material);

        GaiaNode rootNode = new GaiaNode();
        Matrix4d transformMatrix = new Matrix4d();
        transformMatrix.identity();
        rootNode.setTransformMatrix(transformMatrix);
        scene.getNodes().add(rootNode);
        return scene;
    }

    private GaiaNode createNode(GaiaMaterial material, List<Vector3d> positions, List<GaiaTriangle> triangles) {
        GaiaNode node = new GaiaNode();
        node.setTransformMatrix(new Matrix4d().identity());
        GaiaMesh mesh = new GaiaMesh();
        GaiaPrimitive primitive = createPrimitives(material, positions, triangles);
        mesh.getPrimitives().add(primitive);
        node.getMeshes().add(mesh);
        return node;
    }

    private GaiaPrimitive createPrimitives(GaiaMaterial material, List<Vector3d> positions, List<GaiaTriangle> triangles) {
        GaiaPrimitive primitive = new GaiaPrimitive();
        List<GaiaSurface> surfaces = new ArrayList<>();
        List<GaiaVertex> vertices = new ArrayList<>();
        primitive.setMaterial(material);
        primitive.setMaterialIndex(0);
        primitive.setSurfaces(surfaces);
        primitive.setVertices(vertices);

        GaiaSurface surface = new GaiaSurface();
        Vector3d[] normals = new Vector3d[positions.size()];
        for (GaiaTriangle triangle : triangles) {
            GaiaFace face = new GaiaFace();
            Vector3d[] trianglePositions = triangle.getPositions();
            int[] indices = new int[trianglePositions.length];

            indices[0] = indexOf(positions, trianglePositions[0]);
            indices[1] = indexOf(positions, trianglePositions[1]);
            indices[2] = indexOf(positions, trianglePositions[2]);

            normals[indices[0]] = triangle.getNormal();
            normals[indices[1]] = triangle.getNormal();
            normals[indices[2]] = triangle.getNormal();

            face.setIndices(indices);
            surface.getFaces().add(face);
        }

        for (int i = 0; i < positions.size(); i++) {
        //for (Vector3d position : positions) {
            Random random = new Random();
            byte[] colors = new byte[4];
            random.nextBytes(colors);

            Vector3d position = positions.get(i);
            Vector3d normal = normals[i];

            GaiaVertex vertex = new GaiaVertex();
            vertex.setPosition(new Vector3d(position.x, position.y, position.z));
            vertex.setNormal(normal);
            vertex.setColor(colors);
            vertices.add(vertex);
        }

        surfaces.add(surface);
        return primitive;
    }

    private int indexOf(List<Vector3d> positions, Vector3d item) {
        return IntStream.range(0, positions.size())
                //.filter(i -> Objects.equals(positions.get(i), item))
                .filter(i -> positions.get(i) == item)
                .findFirst().orElse(-1);
    };
}
