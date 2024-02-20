package com.gaia3d.converter.geometry;

import com.gaia3d.basic.structure.*;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.command.Configurator;
import com.gaia3d.converter.jgltf.GltfWriter;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

class TessellatorTest {

    @Test
    void tessellate() {
        Configurator.initConsoleLogger();
        double[] target = errorTest();

        List<Vector3d> positions = convert(target);
        Tessellator tessellator = new Tessellator();
        List<GaiaTriangle> triangles = tessellator.tessellate(positions);

        GaiaScene scene = new GaiaScene();
        GaiaMaterial material = new GaiaMaterial();
        material.setId(0);
        material.setName("color");
        material.setDiffuseColor(new Vector4d(1, 0, 0, 1));
        Map<TextureType, List<GaiaTexture>> textureTypeListMap = material.getTextures();
        textureTypeListMap.put(TextureType.DIFFUSE, new ArrayList<>());
        scene.getMaterials().add(material);

        GaiaNode rootNode = new GaiaNode();
        Matrix4d transformMatrix = new Matrix4d();
        transformMatrix.identity();
        rootNode.setTransformMatrix(transformMatrix);

        GaiaNode node = new GaiaNode();
        node.setTransformMatrix(transformMatrix);

        GaiaMesh mesh = new GaiaMesh();
        GaiaPrimitive primitive = new GaiaPrimitive();
        primitive.setMaterialIndex(0);

        List<GaiaSurface> surfaces = new ArrayList<>();
        List<GaiaVertex> vertices = new ArrayList<>();
        primitive.setSurfaces(surfaces);
        primitive.setVertices(vertices);

        int index = 0;
        GaiaSurface surface = new GaiaSurface();
        for (GaiaTriangle triangle : triangles) {
            GaiaFace face = new GaiaFace();
            face.setIndices(new int[]{index++, index++, index++});
            surface.getFaces().add(face);
        }
        surfaces.add(surface);

        for (GaiaTriangle triangle : triangles) {
            Random random = new Random();
            byte[] colors = new byte[4];
            random.nextBytes(colors);
            for (Vector3d position : triangle.getPositions()) {
                GaiaVertex vertex = new GaiaVertex();
                vertex.setPosition(position);
                vertex.setColor(colors);
                vertex.setNormal(triangle.getNormal());
                vertices.add(vertex);
            }
        }

        mesh.getPrimitives().add(primitive);
        node.getMeshes().add(mesh);
        rootNode.getChildren().add(node);
        scene.getNodes().add(rootNode);

        GltfWriter gltfWriter = new GltfWriter();
        gltfWriter.writeGltf(scene, "D:\\tessellate.gltf");
    }

    private List<Vector3d> convert(double[] target) {
        List<Vector3d> positions = new ArrayList<>();
        for (int i = 0; i < target.length; i += 2) {
            Vector3d position = new Vector3d(target[i], target[i + 1], 0);
            positions.add(position);
        }
        return positions;
    }

    private double[] getTriangle() {
        return new double[]{0, 0, 1, 0, 0, 1, 0, 0};
    }

    private double[] getSquare() {
        return new double[]{0, 0, 1, 0, 1, 1, 0, 1, 0, 0};
    }

    private double[] getPentagon() {
        return new double[]{-1, -3, 1, -3, 3, 0, 0, 3, -3, 0, -1, -3};
    }

    private double[] getHexagon() {
        return new double[]{-1, -1, 1, -1, 2, 0, 1, 1, -1, 1, -2, 0, -1, -1};
    }

    private double[] getConcave() {
        return new double[]{0, 0, 0, 1, -1, 1, -1, -1, 1, -1, 1, 0, 0, 0};
    }

    private double[] getStar() {
        return new double[]{0, 4, -1, 1, -4, 1, -1, -1, -3, -4, 0, -1, 3, -4, 1, -1, 4, 1, 1, 1, 0, 4};
    }

    private double[] getSnail() {
        return new double[]{0, 0, -1.765186228930574, 11.24182504658529, -19.732671505408902, 21.051451218700095, -37.36971728240659, 15.760167102111154, -40.787293093195366, 3.4144991880093585, 1.7598394633913546, -38.58239532323205, -30.09401836494934, -44.42282345685089, -27.00883772339016, -20.394402730416914, -64.59510531034394, -23.479665189202933, -86.86150281633002, 30.971524629276246, -68.89514797298777, 35.049412476611906, -55.11684509963631, -5.9549387739753, -50.37665627747588, -4.852271279385604, -48.060170697455355, 25.351185015919327, -26.456148736352965, 37.365455475672206, 6.390778024162477, 37.25459073688398, 18.075009362353043, 18.95723795129743, 17.082957411108108, -10.252751749350864, -1.9845011547008347, -13.006814993372245, -19.400292621316808, -5.952403797338775, -19.622516488065926, 9.147150198135932, -8.854772838370444, 9.554194507596549, -7.419846425453158, -1.9078145103994757, 0, 0};
    }

    private double[] errorTest1() {
        return new double[]{-0.3993259317809361, -1.172630094856686,

                -0.4341592866552446, -1.2016642955359202,

                -0.39537062277616997, -1.2861416291798378,

                -0.3466033561565496, -1.2454935492201002,

                -0.37677294152871355, -1.179789576838175,

                -0.3907059259233847, -1.1914035131326273};
    }


    private double[] errorTest2() {
        return new double[]{-0.0993, -0.0726,

                -0.1341, -0.1016,

                -0.0953, -0.1861,

                -0.0466, -0.1454,

                -0.0767, -0.0797,

                -0.0907, -0.0914};
    }

    private double[] errorTest() {
        return new double[]{
                11, 0,

                16, 4,

                10, 18,

                3, 13,

                7, 5,

                9, 7
        };
    }
}