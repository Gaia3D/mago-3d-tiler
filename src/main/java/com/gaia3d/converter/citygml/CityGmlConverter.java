package com.gaia3d.converter.citygml;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.*;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.command.Configurator;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.geometry.GaiaTriangle;
import com.gaia3d.converter.geometry.Tessellator;
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
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import org.xmlobjects.gml.model.geometry.DirectPositionList;
import org.xmlobjects.gml.model.geometry.primitives.*;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

@Slf4j
public class CityGmlConverter implements Converter {

    @Override
    public GaiaScene load(String path) {
        return convert(new File(path));
    }

    @Override
    public GaiaScene load(File file) {
        return convert(file);
    }

    @Override
    public GaiaScene load(Path path) {
        return convert(path.toFile());
    }

    private GaiaScene convert(File file) {
        GaiaScene scene = initScene();
        scene.setOriginalPath(file.toPath());
        GaiaMaterial material = scene.getMaterials().get(0);
        GaiaNode rootNode = scene.getNodes().get(0);

        int sample = 0;

        Configurator.initConsoleLogger();
        try {
            Tessellator tessellator = new Tessellator();

            CityGMLContext context = CityGMLContext.newInstance();
            CityGMLInputFactory factory = context.createCityGMLInputFactory();
            CityGMLReader reader = factory.createCityGMLReader(file);
            CityModel cityModel = (CityModel) reader.next();

            //AbstractCityObject cityObject = cityModel.getCityObjectMembers().get(0).getObject();

            List<List<Vector3d>> polygons = new ArrayList<>();
            List<String> names = new ArrayList<>();
            GaiaBoundingBox boundingBox = new GaiaBoundingBox();

            //GaiaNode node = createNode();
            //GaiaMesh mesh = createMesh();

            List<AbstractCityObjectProperty> cityObjectMembers = cityModel.getCityObjectMembers();
            for (AbstractCityObjectProperty cityObjectProperty : cityObjectMembers) {
                AbstractCityObject cityObject = cityObjectProperty.getObject();

                Building building = (Building) cityObject;
                SolidProperty solidProperty= building.getLod1Solid();
                AbstractSolid solid = solidProperty.getObject();

                Shell shell = ((Solid) solid).getExterior().getObject();
                List<SurfaceProperty> surfaceProperties = shell.getSurfaceMembers();

                //String srsName = cityModel.getBoundedBy().getEnvelope().getSrsName();

                for (SurfaceProperty surfaceProperty : surfaceProperties) {
                    List<Vector3d> polygon = new Vector<>();

                    Polygon surface = (Polygon) surfaceProperty.getObject();
                    LinearRing linearRing = (LinearRing)surface.getExterior().getObject();
                    DirectPositionList directPositionList = linearRing.getControlPoints().getPosList();
                    List<Double> values = directPositionList.getValue();

                    for (int i = 0; i < values.size(); i+=3) {
                        double x = values.get(i+1);
                        double y = values.get(i);
                        double z = 0;
                        Vector3d position = new Vector3d(x, y, z);
                        polygon.add(position);
                        boundingBox.addPoint(position);
                    }
                    polygons.add(polygon);
                    names.add(cityObject.getId());

                    break;
                }
            }

            Vector3d center = boundingBox.getCenter();
            for (List<Vector3d> polygon : polygons) {
                GaiaBoundingBox worldBoundingBox = new GaiaBoundingBox();
                for (Vector3d position : polygon) {
                    worldBoundingBox.addPoint(position);
                }
                Vector3d worldCenter = center;
                Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(worldCenter);
                Matrix4d transformMatrix = GlobeUtils.normalAtCartesianPointWgs84(centerWorldCoordinate);
                Matrix4d transfromMatrixInv = new Matrix4d(transformMatrix).invert();

                List<Vector3d> localPositions = new ArrayList<>();

                for (Vector3d position : polygon) {
                    Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                    Vector3d localPosition = positionWorldCoordinate.mulPosition(transfromMatrixInv);
                    localPositions.add(localPosition);
                }

                List<GaiaTriangle> triangles = tessellator.tessellate(localPositions);
                GaiaNode node = createNode(material, localPositions, triangles);
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
        return scene;
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
        for (GaiaTriangle triangle : triangles) {
            GaiaFace face = new GaiaFace();
            Vector3d[] trianglePositions = triangle.getPositions();
            int[] indices = new int[trianglePositions.length];
            indices[0] = positions.indexOf(trianglePositions[0]);
            indices[1] = positions.indexOf(trianglePositions[1]);
            indices[2] = positions.indexOf(trianglePositions[2]);
            face.setIndices(indices);
            surface.getFaces().add(face);
        }

        for (Vector3d position : positions) {
            Random random = new Random();
            byte[] colors = new byte[4];
            random.nextBytes(colors);

            GaiaVertex vertex = new GaiaVertex();
            vertex.setPosition(new Vector3d(position.x, position.y, 30));
            vertex.setColor(colors);
            vertices.add(vertex);
        }

        surfaces.add(surface);
        return primitive;
    }
}
