package com.gaia3d.converter.shape;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.*;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.geometry.*;
import com.gaia3d.util.GlobeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileHeader;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.util.factory.Hints;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

@Slf4j
public class ShapeConverter implements Converter {
    private final CommandLine command;

    public ShapeConverter(CommandLine command) {
        this.command = command;
    }

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
        Tessellator tessellator = new Tessellator();
        Extruder extruder = new Extruder(tessellator);

        ShpFiles shpFiles = null;
        try {
            shpFiles = new ShpFiles(file);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        try (ShapefileReader reader = new ShapefileReader(shpFiles, true, true, new GeometryFactory())) {
            ShapefileHeader header = reader.getHeader();

            DataStore dataStore = new ShapefileDataStore(file.toURI().toURL());
            String typeName = dataStore.getTypeNames()[0];
            SimpleFeatureSource source = dataStore.getFeatureSource(typeName);
            SimpleFeatureType schema = source.getSchema();

            ShapefileDataStore shapeFileDatastore = (ShapefileDataStore) dataStore;

            source = dataStore.getFeatureSource(typeName);
            schema = source.getSchema();

            var query = new Query(typeName, Filter.INCLUDE);
            query.getHints().add(new Hints(Hints.FEATURE_2D, true)); // for 3d feature
            SimpleFeatureCollection features = source.getFeatures(query);

            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(schema);

            FeatureIterator<SimpleFeature> iterator = features.features();

            List<GaiaBuilding> buildings = new ArrayList<>();
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();

                GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
                Coordinate[] coordinates = geom.getCoordinates();

                GaiaBoundingBox boundingBox = new GaiaBoundingBox();
                List<Vector3d> positions = new ArrayList<>();

                for (Coordinate coordinate : coordinates) {
                    Point point = (Point) geometryFactory.createPoint(coordinate);
                    double x = point.getX();
                    double y = point.getY();

                    Vector3d position = new Vector3d(x, y, 0);
                    positions.add(position);
                    boundingBox.addPoint(position);
                }



                String heightAttribute = (String) feature.getAttribute("height");
                int heightUppercaseAttribute = 0;

                Object heightUppercaseAttributeObject = feature.getAttribute("HEIGHT");
                if (heightUppercaseAttributeObject != null) {
                    heightUppercaseAttribute = (int) feature.getAttribute("HEIGHT");
                }

                double height = 0;
                if (heightAttribute != null && !heightAttribute.equals("")) {
                    height = Double.parseDouble(heightAttribute);
                } else if (heightUppercaseAttribute != 0) {
                    height = heightUppercaseAttribute;
                }
                /*else if (heightUppercaseAttribute != null && !heightUppercaseAttribute.equals("")) {
                    height = Double.parseDouble(heightUppercaseAttribute);
                }*/

                if (height == 0) {
                    height = 1.0d;
                }

                GaiaBuilding building = GaiaBuilding.builder()
                        .id(feature.getID())
                        .name("test")
                        .boundingBox(boundingBox)
                        .floorHeight(0)
                        .roofHeight(height)
                        .positions(positions)
                        .build();
                buildings.add(building);
            }

            for (GaiaBuilding building : buildings) {
                GaiaScene scene = initScene();
                scene.setOriginalPath(file.toPath());

                GaiaMaterial material = scene.getMaterials().get(0);
                GaiaNode rootNode = scene.getNodes().get(0);
                rootNode.setName(building.getName());


                Vector3d center = building.getBoundingBox().getCenter();

                Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
                Matrix4d transformMatrix = GlobeUtils.normalAtCartesianPointWgs84(centerWorldCoordinate);
                Matrix4d transfromMatrixInv = new Matrix4d(transformMatrix).invert();

                List<Vector3d> localPositions = new ArrayList<>();
                for (Vector3d position : building.getPositions()) {
                    Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                    Vector3d localPosition = positionWorldCoordinate.mulPosition(transfromMatrixInv);
                    localPosition.z = 0.0d;
                    localPositions.add(localPosition);
                }

                Extrusion extrusion = extruder.extrude(localPositions, building.getRoofHeight(), building.getFloorHeight());
                GaiaNode node = createNode(material, extrusion.getPositions(), extrusion.getTriangles());
                rootNode.getChildren().add(node);

                Matrix4d rootTransformMatrix = new Matrix4d().identity();
                rootTransformMatrix.translate(center, rootTransformMatrix);
                rootNode.setTransformMatrix(rootTransformMatrix);
                scenes.add(scene);
            }

            dataStore.dispose();
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        shpFiles.dispose();
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
