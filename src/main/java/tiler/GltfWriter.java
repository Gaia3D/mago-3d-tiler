package tiler;

import de.javagl.jgltf.impl.v2.*;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.GltfModels;
import de.javagl.jgltf.model.io.GltfModelWriter;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import geometry.GaiaMesh;
import geometry.GaiaScene;
import org.joml.Matrix4d;
import org.lwjgl.opengl.GL20;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GltfWriter {
    public static void write(GaiaScene scene, String path) {
        GltfModel gltfModel = convert(scene);
        GltfModelWriter writer = new GltfModelWriter();
        try {
            writer.write(gltfModel, new File(path));
            System.out.println("Done");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static GltfModel convert(GaiaScene gaiaScene) {
        GlTF gltf = new GlTF();
        gltf = generateAsset(gltf);
        initScene(gltf, gaiaScene);
        GltfBinary binary = createBinaryBuffer(gltf, gaiaScene);
        binary.fill();
        GltfAssetV2 asset = new GltfAssetV2(gltf, binary.getBody());
        return GltfModels.create(asset);
    }

    private static GltfBinary createBinaryBuffer(GlTF gltf, GaiaScene gaiaScene) {

        int totalIndicesByteLength = gaiaScene.getTotalIndices() * 4;
        int totalVerticesByteLength = gaiaScene.getTotalVertices() * 4;
        int totalNormalsByteLength = gaiaScene.getTotalNormals() * 4;
        int totalColorsByteLength = gaiaScene.getTotalColors() * 4;
        int totalTextureCoordinatesByteLength = gaiaScene.getTotalTexCoords();

        System.out.println("totalIndicesByteLength: " + totalIndicesByteLength);
        System.out.println("totalVerticesByteLength: " + totalVerticesByteLength);
        System.out.println("totalNormalsByteLength: " + totalNormalsByteLength);
        System.out.println("totalColorsByteLength: " + totalColorsByteLength);
        System.out.println("totalTextureCoordinatesByteLength: " + totalTextureCoordinatesByteLength);


        int totalBodyByteLength = totalIndicesByteLength + totalVerticesByteLength + totalNormalsByteLength + totalColorsByteLength + totalTextureCoordinatesByteLength;
        //int totalBodyByteLength = 10;
        System.out.println("totalBodyByteLength: " + totalBodyByteLength);

        GltfBinary binary = new GltfBinary();
        binary.setBody(ByteBuffer.allocate(totalBodyByteLength));
        ByteBuffer body = binary.getBody();
        body.order(ByteOrder.LITTLE_ENDIAN);
        if (totalIndicesByteLength > 0) {
            ByteBuffer indicesBuffer = ByteBuffer.allocate(totalIndicesByteLength);
            indicesBuffer.order(ByteOrder.LITTLE_ENDIAN);
            binary.setIndicesBuffer(indicesBuffer);
        }
        if (totalVerticesByteLength > 0) {
            ByteBuffer verticesBuffer = ByteBuffer.allocate(totalVerticesByteLength);
            verticesBuffer.order(ByteOrder.LITTLE_ENDIAN);
            binary.setVerticesBuffer(verticesBuffer);
        }
        if (totalNormalsByteLength > 0) {
            ByteBuffer normalsBuffer = ByteBuffer.allocate(totalNormalsByteLength);
            normalsBuffer.order(ByteOrder.LITTLE_ENDIAN);
            binary.setNormalsBuffer(normalsBuffer);
        }
        if (totalColorsByteLength > 0) {
            ByteBuffer colorsBuffer = ByteBuffer.allocate(totalColorsByteLength);
            colorsBuffer.order(ByteOrder.LITTLE_ENDIAN);
            binary.setColorsBuffer(colorsBuffer);
        }
        if (totalTextureCoordinatesByteLength > 0) {
            ByteBuffer textureCoordinatesBuffer = ByteBuffer.allocate(totalTextureCoordinatesByteLength);
            textureCoordinatesBuffer.order(ByteOrder.LITTLE_ENDIAN);
            binary.setTextureCoordinatesBuffer(textureCoordinatesBuffer);
        }

        readyBinary(gltf, binary);
        return binary;
    }

    private static GlTF generateAsset(GlTF gltf) {
        Asset asset = new Asset();
        asset.setVersion("2.0");
        asset.setMinVersion("2.0");
        //asset.setGenerator(Metas.properties.getProperty("project.name") + "-" + Metas.properties.getProperty("project.version") + " for " + Metas.properties.getProperty("platform.os.name") + "-" + Metas.properties.getProperty("platform.os.arch"));
        gltf.setAsset(asset);
        return gltf;
    }

    private static void initScene(GlTF gltf, GaiaScene gaiaScene) {
        List<Scene> scenes = new ArrayList<>();
        Scene scene = new Scene();
        List<Node> nodes = new ArrayList<>();
        Node node = new Node();
        node.setName("root");

        Matrix4d matrix4d = new Matrix4d();
        float[] mat = matrix4d.get(new float[16]);
        /*double[] trueNorth = model.getTrueNorth();
        double r = Math.sqrt(trueNorth[0] * trueNorth[0] + trueNorth[1] * trueNorth[1]);
        float[] matTn = new float[] {
                (float) (trueNorth[1] / r), (float) (-trueNorth[0] / r),    0.f, 0.f,
                (float) (trueNorth[0] / r), (float) (trueNorth[1] / r),     0.f, 0.f,
                0.f,                        0.f,                            1.f, 0.f,
                0.f,                        0.f,                            0.f, 1.f
        };

        float[] mat = new float[16];
        Matrix.multiplyMM(mat, 0, ROT_Z_TO_Y, 0, matTn, 0);*/
        node.setMatrix(mat);
        nodes.add(node);
        gltf.setNodes(nodes);
        scene.addNodes(0);
        scenes.add(scene);
        gltf.setScenes(scenes);
        gltf.setScene(0);
    }

    //readBinary
    private static void readyBinary(GlTF gltf, GltfBinary binary) {
        ByteBuffer body = binary.getBody();

        List<Buffer> buffers = new ArrayList<>(1);
        Buffer buffer = new Buffer();
        buffer.setByteLength(body.capacity());
        buffers.add(buffer);

        int bodyOffset = 0;
        if (binary.getIndicesBuffer() != null) {
            int capacity = binary.getIndicesBuffer().capacity();
            binary.setIndicesBufferId(createBufferView(gltf, 0, bodyOffset, capacity, -1, GL20.GL_ELEMENT_ARRAY_BUFFER));
            bodyOffset += capacity;
        }
        if (binary.getVerticesBuffer() != null) {
            int capacity = binary.getVerticesBuffer().capacity();
            binary.setVerticesBufferId(createBufferView(gltf, 0, bodyOffset, capacity, 12, GL20.GL_ARRAY_BUFFER));
            bodyOffset += capacity;
        }
        if (binary.getNormalsBuffer() != null) {
            int capacity = binary.getNormalsBuffer().capacity();
            binary.setNormalsBufferId(createBufferView(gltf, 0, bodyOffset, capacity, 12, GL20.GL_ARRAY_BUFFER));
            bodyOffset += capacity;
        }
        if (binary.getColorsBuffer() != null) {
            int capacity = binary.getColorsBuffer().capacity();
            binary.setColorsBufferId(createBufferView(gltf, 0, bodyOffset, capacity, 16, GL20.GL_ARRAY_BUFFER));
            bodyOffset += capacity;
        }
        if (binary.getTextureCoordinatesBuffer() != null) {
            int capacity = binary.getTextureCoordinatesBuffer().capacity();
            binary.setTextureCoordinatesBufferId(createBufferView(gltf, 0, bodyOffset, capacity, 8, GL20.GL_ARRAY_BUFFER));
            bodyOffset += capacity;
        }
        buffers.get(0).setByteLength(bodyOffset);
        gltf.setBuffers(buffers);
    }

    //createBufferView
    private static int createBufferView(GlTF gltf, int buffer, int offset, int length, int stride, int target) {
        BufferView bufferView = new BufferView();
        bufferView.setBuffer(buffer);
        bufferView.setByteOffset(offset);
        bufferView.setByteLength(length);
        if (target > 0)
            bufferView.setTarget(target);
        if (stride > 0)
            bufferView.setByteStride(stride);
        gltf.addBufferViews(bufferView);
        return gltf.getBufferViews().size() - 1;
    }

    //isDefaultMatrix
    private static boolean isDefaultMatrix(float[] matrix) {
        return matrix[0] == 1 && matrix[1] == 0 && matrix[2] == 0 && matrix[3] == 0 &&
                matrix[4] == 0 && matrix[5] == 1 && matrix[6] == 0 && matrix[7] == 0 &&
                matrix[8] == 0 && matrix[9] == 0 && matrix[10] == 1 && matrix[11] == 0 &&
                matrix[12] == 0 && matrix[13] == 0 && matrix[14] == 0 && matrix[15] == 1;
    }

    //addMeshNode
    private static void addMeshNode(GlTF gltf, int mesh, float[] matrix, String name) {
        Node root = gltf.getNodes().get(0);
        Node node = new Node();
        if (matrix != null && !isDefaultMatrix(matrix)) {
            node.setMatrix(matrix);
        }
        if (name != null) {
            node.setName(name);
        }
        node.setMesh(mesh);
        gltf.addNodes(node);
        int nodeId = gltf.getNodes().size() - 1;
        root.addChildren(mesh);
    }

    //createPrimitive
    private static int createPrimitive(GlTF gltf, int mesh, GltfBinary binary, GaiaMesh gaiaMesh, int materialId) {
        MeshPrimitive primitive = new MeshPrimitive();

        //??
        primitive.setMode(GL20.GL_TRIANGLES);
        primitive.setMaterial(materialId);
        primitive.setAttributes(new HashMap<>());
        primitive.getAttributes().put("POSITION", binary.getVerticesBufferId());
        if (binary.getNormalsBuffer() != null) {
            primitive.getAttributes().put("NORMAL", binary.getNormalsBufferId());
        }
        if (binary.getColorsBuffer() != null) {
            primitive.getAttributes().put("COLOR_0", binary.getColorsBufferId());
        }
        if (binary.getTextureCoordinatesBuffer() != null) {
            primitive.getAttributes().put("TEXCOORD_0", binary.getTextureCoordinatesBufferId());
        }
        primitive.setIndices(binary.getIndicesBufferId());

        if (mesh == -1) {
            mesh = createMesh(gltf);
        }
        gltf.getMeshes().get(mesh).addPrimitives(primitive);
        return mesh;
    }
    private static int createMesh(GlTF gltf) {
        Mesh mesh = new Mesh();
        gltf.addMeshes(mesh);
        return gltf.getMeshes().size() - 1;
    }
}
