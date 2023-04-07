package tiler;

import de.javagl.jgltf.impl.v2.*;
import de.javagl.jgltf.model.GltfConstants;
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
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * GltfWriter
 */
public class GltfWriter {
    public static void writeGltf(GaiaScene scene, String path) {
        GltfModel gltfModel = convert(scene);
        GltfModelWriter writer = new GltfModelWriter();
        try {
            //writer.write(gltfModel, new File(path));
            writer.writeEmbedded(gltfModel, new File(path));
            //writer.writeBinary(gltfModel, new File(path));
            System.out.println("Done");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeGlb(GaiaScene scene, String path) {
        GltfModel gltfModel = convert(scene);
        GltfModelWriter writer = new GltfModelWriter();
        try {
            writer.writeBinary(gltfModel, new File(path));
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
        if (binary != null) {
            List<Double> areas = new ArrayList<>();
            List<Double> volumes = new ArrayList<>();

            convertGeometryInfo(gltf, binary, gaiaScene);

            areas.sort(Double::compareTo);
            volumes.sort(Double::compareTo);
            binary.fill();
        }
        GltfAssetV2 asset = new GltfAssetV2(gltf, binary.getBody());
        return GltfModels.create(asset);
    }

    //convertGeometryInfo
    private static void convertGeometryInfo(GlTF gltf, GltfBinary binary, GaiaScene gaiaScene) {
        ArrayList<Short> indices = gaiaScene.getTotalIndices();
        ArrayList<Float> vertices = gaiaScene.getTotalVertices();
        ArrayList<Float> normals = gaiaScene.getTotalNormals();
        ArrayList<Float> colors = gaiaScene.getTotalColors();
        ArrayList<Float> textureCoordinates = gaiaScene.getTotalTextureCoordinates();

        ByteBuffer indicesBuffer = binary.getIndicesBuffer();
        ByteBuffer verticesBuffer = binary.getVerticesBuffer();
        ByteBuffer normalsBuffer = binary.getNormalsBuffer();
        ByteBuffer colorsBuffer = binary.getColorsBuffer();
        ByteBuffer textureCoordinatesBuffer = binary.getTextureCoordinatesBuffer();
        ByteBuffer textureBuffer = binary.getTextureBuffer();

        int indicesBufferViewId = binary.getIndicesBufferViewId();
        int verticesBufferViewId = binary.getVerticesBufferViewId();
        int normalsBufferViewId = binary.getNormalsBufferViewId();
        int colorsBufferViewId = binary.getColorsBufferViewId();
        int textureCoordinatesBufferViewId = binary.getTextureCoordinatesBufferViewId();
        int textureBufferViewId = binary.getTextureBufferViewId();

        if (indicesBufferViewId > -1)
            binary.setIndicesAccessorId(createAccessor(gltf, indicesBufferViewId, 0, indices.size(), GltfConstants.GL_UNSIGNED_SHORT, AccessorType.SCALAR));
        if (verticesBufferViewId > -1)
            binary.setVerticesAccessorId(createAccessor(gltf, verticesBufferViewId, 0, vertices.size() / 3, GltfConstants.GL_FLOAT, AccessorType.VEC3));
        if (normalsBufferViewId > -1)
            binary.setNormalsAccessorId(createAccessor(gltf, normalsBufferViewId, 0, normals.size() / 3, GltfConstants.GL_FLOAT, AccessorType.VEC3));
        if (colorsBufferViewId > -1)
            binary.setColorsAccessorId(createAccessor(gltf, colorsBufferViewId, 0, colors.size() / 4, GltfConstants.GL_FLOAT, AccessorType.VEC4));
        if (textureCoordinatesBufferViewId > -1)
            binary.setTextureCoordinatesAccessorId(createAccessor(gltf, textureCoordinatesBufferViewId, 0, textureCoordinates.size() / 2, GltfConstants.GL_FLOAT, AccessorType.VEC2));
        if (textureBufferViewId > -1)
            binary.setTextureAccessorId(createAccessor(gltf, textureBufferViewId, 0, textureBuffer.capacity(), GltfConstants.GL_UNSIGNED_BYTE, AccessorType.SCALAR));

        int meshId = createMeshWithPrimitive(gltf, -1, binary);

//        int indicesAccessorId = createAccessor(gltf, indicesBufferViewId, 0, indices.size(), GltfConstants.GL_UNSIGNED_SHORT, AccessorType.VEC3);
//        int verticesAccessorId = createAccessor(gltf, verticesBufferViewId, 0, vertices.size() / 3, GltfConstants.GL_FLOAT, AccessorType.VEC3);
//        int normalsAccessorId = createAccessor(gltf, normalsBufferViewId, 0, normals.size() / 3, GltfConstants.GL_FLOAT, AccessorType.VEC3);
//        int colorsAccessorId = createAccessor(gltf, colorsBufferViewId, 0, colors.size() / 4, GltfConstants.GL_FLOAT, AccessorType.VEC4);
//        int textureCoordinatesAccessorId = createAccessor(gltf, textureCoordinatesBufferViewId, 0, textureCoordinates.size() / 2, GltfConstants.GL_FLOAT, AccessorType.VEC2);

        for (Short indice: indices) {
            indicesBuffer.putShort(indice);
        }
        for (Float vertex: vertices) {
            verticesBuffer.putFloat(vertex);
        }
        for (Float normal: normals) {
            normalsBuffer.putFloat(normal);
        }
        for (Float color: colors) {
            colorsBuffer.putFloat(color);
        }
        for (Float textureCoordinate: textureCoordinates) {
            textureCoordinatesBuffer.putFloat(textureCoordinate);
        }

        addMeshNode(gltf, meshId, null, "znkim test");
//        System.out.println("good");
    }

    private static GltfBinary createBinaryBuffer(GlTF gltf, GaiaScene gaiaScene) {

        int totalIndicesByteLength = gaiaScene.getTotalIndicesCount() * 4;
        int totalVerticesByteLength = gaiaScene.getTotalVerticesCount() * 4 * 3;
        int totalNormalsByteLength = gaiaScene.getTotalNormalsCount() * 4 * 3;
        int totalColorsByteLength = gaiaScene.getTotalColorsCount() * 4 * 4;
        int totalTextureCoordinatesByteLength = gaiaScene.getTotalTextureCoordinatesCount() * 4 * 2;

//        System.out.println("totalIndicesByteLength: " + totalIndicesByteLength);
//        System.out.println("totalVerticesByteLength: " + totalVerticesByteLength);
//        System.out.println("totalNormalsByteLength: " + totalNormalsByteLength);
//        System.out.println("totalColorsByteLength: " + totalColorsByteLength);
//        System.out.println("totalTextureCoordinatesByteLength: " + totalTextureCoordinatesByteLength);


        int totalBodyByteLength = totalIndicesByteLength + totalVerticesByteLength + totalNormalsByteLength + totalColorsByteLength + totalTextureCoordinatesByteLength;
        //int totalBodyByteLength = 10;
//        System.out.println("totalBodyByteLength: " + totalBodyByteLength);

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
            binary.setIndicesBufferViewId(createBufferView(gltf, 0, bodyOffset, capacity, -1, GL20.GL_ELEMENT_ARRAY_BUFFER));
            bodyOffset += capacity;
        }
        if (binary.getVerticesBuffer() != null) {
            int capacity = binary.getVerticesBuffer().capacity();
            binary.setVerticesBufferViewId(createBufferView(gltf, 0, bodyOffset, capacity, 12, GL20.GL_ARRAY_BUFFER));
            bodyOffset += capacity;
        }
        if (binary.getNormalsBuffer() != null) {
            int capacity = binary.getNormalsBuffer().capacity();
            binary.setNormalsBufferViewId(createBufferView(gltf, 0, bodyOffset, capacity, 12, GL20.GL_ARRAY_BUFFER));
            bodyOffset += capacity;
        }
        if (binary.getColorsBuffer() != null) {
            int capacity = binary.getColorsBuffer().capacity();
            binary.setColorsBufferViewId(createBufferView(gltf, 0, bodyOffset, capacity, 16, GL20.GL_ARRAY_BUFFER));
            bodyOffset += capacity;
        }
        if (binary.getTextureCoordinatesBuffer() != null) {
            int capacity = binary.getTextureCoordinatesBuffer().capacity();
            binary.setTextureCoordinatesBufferViewId(createBufferView(gltf, 0, bodyOffset, capacity, 8, GL20.GL_ARRAY_BUFFER));
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
        if (target > -1)
            bufferView.setTarget(target);
        if (stride > -1)
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
    private static int addMeshNode(GlTF gltf, int mesh, float[] matrix, String name) {
        Node root = gltf.getNodes().get(0);
        Node node = new Node();
        node.setMesh(mesh);
        if (matrix != null && !isDefaultMatrix(matrix)) {
            node.setMatrix(matrix);
        }
        if (name != null) {
            node.setName(name);
        }
        gltf.addNodes(node);
        int nodeId = gltf.getNodes().size() - 1;
        root.addChildren(nodeId);
        return nodeId;
    }

    private static final String ACCESSOR_TYPE_SCALE = "SCALAR";
    private static final String ACCESSOR_TYPE_VEC3 = "VEC3";
    private static final String ACCESSOR_TYPE_VEC4 = "VEC4";

    private static int createAccessor(GlTF gltf, int bufferView, int byteOffset, int count, int componentType, AccessorType accessorType) {
        Accessor accessor = new Accessor();
        accessor.setBufferView(bufferView);
        accessor.setByteOffset(byteOffset);
        accessor.setCount(count);
        accessor.setComponentType(componentType);
        accessor.setType(accessorType.name());
        gltf.addAccessors(accessor);
        return gltf.getAccessors().size() - 1;
    }

    //createPrimitive
    private static int createMeshWithPrimitive(GlTF gltf, int mesh, GltfBinary binary) {
        MeshPrimitive primitive = new MeshPrimitive();

        primitive.setMode(GltfConstants.GL_TRIANGLES);
        primitive.setMaterial(binary.getTextureAccessorId());
        primitive.setAttributes(new HashMap<>());
        primitive.setIndices(binary.getIndicesAccessorId());

        if (binary.getVerticesAccessorId() > -1)
        primitive.getAttributes().put("POSITION", binary.getVerticesAccessorId());
        if (binary.getNormalsAccessorId() > -1)
            primitive.getAttributes().put("NORMAL", binary.getNormalsAccessorId());
        if (binary.getColorsAccessorId() > -1)
            primitive.getAttributes().put("COLOR_0", binary.getColorsAccessorId());
        if (binary.getTextureCoordinatesAccessorId() > -1)
            primitive.getAttributes().put("TEXCOORD_0", binary.getTextureCoordinatesAccessorId());
        if (mesh == -1)
            mesh = createMesh(gltf);
        gltf.getMeshes().get(mesh).addPrimitives(primitive);
        return mesh;
    }
    private static int createMesh(GlTF gltf) {
        Mesh mesh = new Mesh();
        gltf.addMeshes(mesh);
        return gltf.getMeshes().size() - 1;
    }
}
