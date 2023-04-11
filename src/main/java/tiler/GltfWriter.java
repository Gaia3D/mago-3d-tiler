package tiler;

import de.javagl.jgltf.impl.v2.*;
import de.javagl.jgltf.model.GltfConstants;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.GltfModels;
import de.javagl.jgltf.model.impl.DefaultGltfModel;
import de.javagl.jgltf.model.io.GltfModelWriter;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import de.javagl.jgltf.model.v1.GltfModelV1;
import geometry.structure.*;
import org.joml.Matrix4d;
import org.lwjgl.opengl.GL20;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import util.FileUtils;

import javax.swing.text.html.Option;

public class GltfWriter {
    public static void writeGltf(GaiaScene gaiaScene, String outputPath) {
        try {
            GltfModel gltfModel = convert(gaiaScene);
            GltfModelWriter writer = new GltfModelWriter();
            writer.writeEmbedded(gltfModel, new File(outputPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void writeGlb(GaiaScene gaiaScene, String outputPath) {
        try {
            GltfModel gltfModel = convert(gaiaScene);
            GltfModelWriter writer = new GltfModelWriter();
            writer.writeBinary(gltfModel, new File(outputPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static GltfModel convert(GaiaScene gaiaScene) {
        GlTF gltf = new GlTF();
        gltf.setAsset(genAsset());
        gltf.addSamplers(genSampler());

        initScene(gltf);
        GltfBinary binary = new GltfBinary();
        if (binary != null) {
            List<Double> areas = new ArrayList<>();
            List<Double> volumes = new ArrayList<>();

            gaiaScene.getMaterials().stream().forEach(gaiaMaterial -> {
                int materialId = createMaterial(gltf, gaiaMaterial);
                binary.setMaterialId(materialId);
            });
            convertNode(gltf, binary, null, gaiaScene.getNodes());

            areas.sort(Double::compareTo);
            volumes.sort(Double::compareTo);
            binary.fill();
        }
        if (binary.getBody().isPresent()) {
            GltfAssetV2 asset = new GltfAssetV2(gltf, binary.getBody().get());
            GltfModel gltfModel = GltfModels.create(asset);
            return gltfModel;
        }
        return null;
    }

    private static void convertNode(GlTF gltf, GltfBinary binary, Node parentNode, ArrayList<GaiaNode> gaiaNodes) {
        List<GltfNodeBuffer> nodeBuffers = binary.getNodeBuffers();
        gaiaNodes.stream().forEach((gaiaNode) -> {
            Node node = createNode(gltf, parentNode, gaiaNode);
            gltf.addNodes(node);
            int nodeId = gltf.getNodes().size() - 1;
            if (parentNode == null) {
                gltf.getNodes().get(0).addChildren(nodeId);
            } else {
                parentNode.addChildren(nodeId);
            }

            ArrayList<GaiaNode> children = gaiaNode.getChildren();
            if (children.size() > 0) {
                convertNode(gltf, binary, node, children);
            }

            ArrayList<GaiaMesh> gaiaMeshes = gaiaNode.getMeshes();
            gaiaMeshes.stream().forEach((gaiaMesh) -> {
                //GltfNodeBuffer nodeBuffer = createNodeBuffer(gltf, gaiaMesh);
                GltfNodeBuffer nodeBuffer = convertGeometryInfo(gltf, gaiaMesh, node);
                nodeBuffers.add(nodeBuffer);
            });
        });
    }

    private static GltfNodeBuffer convertGeometryInfo(GlTF gltf, GaiaMesh gaiaMesh, Node node) {
        GltfNodeBuffer nodeBuffer = createNodeBuffer(gltf, gaiaMesh);
        createBuffer(gltf, nodeBuffer);

        ArrayList<Short> indices = gaiaMesh.getIndices();
        ArrayList<Float> vertices = gaiaMesh.getPositions();
        ArrayList<Float> normals = gaiaMesh.getNormals();
        ArrayList<Float> colors = gaiaMesh.getColors();

        ArrayList<Float> textureCoordinates = gaiaMesh.getTextureCoordinates();
        Optional<ByteBuffer> indicesBuffer = nodeBuffer.getIndicesBuffer();
        Optional<ByteBuffer> positionsBuffer = nodeBuffer.getPositionsBuffer();
        Optional<ByteBuffer> normalsBuffer = nodeBuffer.getNormalsBuffer();
        Optional<ByteBuffer> colorsBuffer = nodeBuffer.getColorsBuffer();
        Optional<ByteBuffer> textureCoordinatesBuffer = nodeBuffer.getTextureCoordinatesBuffer();

        int indicesBufferViewId = nodeBuffer.getIndicesBufferViewId();
        int positionsBufferViewId = nodeBuffer.getPositionsBufferViewId();
        int normalsBufferViewId = nodeBuffer.getNormalsBufferViewId();
        int colorsBufferViewId = nodeBuffer.getColorsBufferViewId();
        int textureCoordinatesBufferViewId = nodeBuffer.getTextureCoordinatesBufferViewId();

        if (indicesBufferViewId > -1 && indices.size() > 0) {
            int indicesAccessorId = createAccessor(gltf, indicesBufferViewId, 0, indices.size(), GltfConstants.GL_UNSIGNED_SHORT, AccessorType.SCALAR);
            nodeBuffer.setIndicesAccessorId(indicesAccessorId);
        }
        if (positionsBufferViewId > -1 && vertices.size() > 0) {
            int verticesAccessorId = createAccessor(gltf, positionsBufferViewId, 0, vertices.size(), GltfConstants.GL_FLOAT, AccessorType.VEC3);
            nodeBuffer.setPositionsAccessorId(verticesAccessorId);
        }
        if (normalsBufferViewId > -1 && normals.size() > 0) {
            int normalsAccessorId = createAccessor(gltf, normalsBufferViewId, 0, normals.size(), GltfConstants.GL_FLOAT, AccessorType.VEC3);
            nodeBuffer.setNormalsAccessorId(normalsAccessorId);
        }
        if (colorsBufferViewId > -1 && colors.size() > 0) {
            int colorsAccessorId = createAccessor(gltf, colorsBufferViewId, 0, colors.size(), GltfConstants.GL_FLOAT, AccessorType.VEC3);
            nodeBuffer.setColorsAccessorId(colorsAccessorId);
        }
        if (textureCoordinatesBufferViewId > -1 && textureCoordinates.size() > 0) {
            int textureCoordinatesAccessorId = createAccessor(gltf, textureCoordinatesBufferViewId, 0, textureCoordinates.size(), GltfConstants.GL_FLOAT, AccessorType.VEC2);
            nodeBuffer.setTextureCoordinatesAccessorId(textureCoordinatesAccessorId);
        }

        if (indicesBuffer.isPresent()) {
            ByteBuffer indicesByteBuffer = indicesBuffer.get();
            indices.stream().forEach((indice) -> {
                indicesByteBuffer.putShort(indice);
            });
        }
        if (positionsBuffer.isPresent()) {
            ByteBuffer positionByteBuffer = positionsBuffer.get();
            for (Float vertex: vertices) {
                positionByteBuffer.putFloat(vertex);
            }
        }
        if (normalsBuffer.isPresent()) {
            ByteBuffer normalsByteBuffer = normalsBuffer.get();
            for (Float normal: normals) {
                normalsByteBuffer.putFloat(normal);
            }
        }
        if (colorsBuffer.isPresent()) {
            ByteBuffer colorsByteBuffer = colorsBuffer.get();
            for (Float color: colors) {
                colorsByteBuffer.putFloat(color);
            }
        }
        if (textureCoordinatesBuffer.isPresent()) {
            ByteBuffer textureCoordinatesByteBuffer = textureCoordinatesBuffer.get();
            for (Float textureCoordinate: textureCoordinates) {
                textureCoordinatesByteBuffer.putFloat(textureCoordinate);
            }
        }

        MeshPrimitive primitive = createPrimitive(nodeBuffer, 0); // TODO
        int meshId = createMesh(gltf, primitive);
        node.setMesh(meshId);
        return nodeBuffer;
    }

    private static int SHORT_SIZE = 2;
    private static int INTEGER_SIZE = 4;
    private static int VEC2_SIZE = 2;
    private static int VEC3_SIZE = 3;
    private static int VEC4_SIZE = 4;
    private static int FLOAT_SIZE = 4;
    private static GltfNodeBuffer createNodeBuffer(GlTF gltf, GaiaMesh gaiaMesh) {
        GltfNodeBuffer nodeBuffer = new GltfNodeBuffer();

        int indicesByteLength = gaiaMesh.getIndicesCount() * FLOAT_SIZE;
        int positionsByteLength = gaiaMesh.getPositionsCount() * FLOAT_SIZE;
        int normalsByteLength = gaiaMesh.getNormalsCount() * FLOAT_SIZE;
        int colorsByteLength = gaiaMesh.getColorsCount() * FLOAT_SIZE;
        int textureCoordinatesByteLength = gaiaMesh.getTextureCoordinatesCount() * FLOAT_SIZE;

        int bodyLength = 0;
        bodyLength += indicesByteLength;
        bodyLength += positionsByteLength;
        bodyLength += normalsByteLength;
        bodyLength += colorsByteLength;
        bodyLength += textureCoordinatesByteLength;

        nodeBuffer.setTotalByteBufferLength(bodyLength);
        if (indicesByteLength > 0) {
            ByteBuffer indicesBuffer = ByteBuffer.allocate(indicesByteLength);
            indicesBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nodeBuffer.setIndicesBuffer(Optional.of((indicesBuffer)));
        }
        if (positionsByteLength > 0) {
            ByteBuffer positionsBuffer = ByteBuffer.allocate(positionsByteLength);
            positionsBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nodeBuffer.setPositionsBuffer(Optional.of((positionsBuffer)));
        }
        if (normalsByteLength > 0) {
            ByteBuffer normalsBuffer = ByteBuffer.allocate(normalsByteLength);
            normalsBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nodeBuffer.setNormalsBuffer(Optional.of((normalsBuffer)));
        }
        if (colorsByteLength > 0) {
            ByteBuffer colorsBuffer = ByteBuffer.allocate(colorsByteLength);
            colorsBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nodeBuffer.setColorsBuffer(Optional.of((colorsBuffer)));
        }
        if (textureCoordinatesByteLength > 0) {
            ByteBuffer textureCoordinatesBuffer = ByteBuffer.allocate(textureCoordinatesByteLength);
            textureCoordinatesBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nodeBuffer.setTextureCoordinatesBuffer(Optional.of((textureCoordinatesBuffer)));
        }
        return nodeBuffer;
    }

    private static Asset genAsset() {
        Asset asset = new Asset();
        asset.setGenerator("plasma");
        asset.setCopyright("gaia3d");
        asset.setVersion("2.0");
        asset.setMinVersion("2.0");
        return asset;
    }

    private static void initScene(GlTF gltf) {
        List<Scene> scenes = new ArrayList<>();
        Scene scene = new Scene();
        List<Node> nodes = new ArrayList<>();
        Node rootNode = new Node();
        rootNode.setName("root");

        Matrix4d matrix4d = new Matrix4d();
        matrix4d.identity();
        float[] transform = matrix4d.get(new float[16]);
        rootNode.setMatrix(transform);

        nodes.add(rootNode);
        gltf.setNodes(nodes);
        scene.addNodes(gltf.getNodes().size() -1);
        scenes.add(scene);
        gltf.setScenes(scenes);
        gltf.setScene(gltf.getScenes().size() - 1);
    }

    private static int createBuffer(GlTF gltf, GltfNodeBuffer nodeBuffer) {
        Buffer buffer = new Buffer();
        gltf.addBuffers(buffer);
        int bufferId = gltf.getBuffers().size() - 1;

        int bufferOffset = 0;
        if (nodeBuffer.getIndicesBuffer().isPresent()) {
            ByteBuffer indicesBuffer = nodeBuffer.getIndicesBuffer().get();
            nodeBuffer.setIndicesBufferViewId(createBufferView(gltf, bufferId, bufferOffset, indicesBuffer.capacity(), -1, GL20.GL_ELEMENT_ARRAY_BUFFER));
            bufferOffset += indicesBuffer.capacity();
        }
        if (nodeBuffer.getPositionsBuffer().isPresent()) {
            ByteBuffer positionBuffer = nodeBuffer.getPositionsBuffer().get();
            nodeBuffer.setPositionsBufferViewId(createBufferView(gltf, bufferId, bufferOffset, positionBuffer.capacity(), 12, GL20.GL_ARRAY_BUFFER));
            bufferOffset += positionBuffer.capacity();
        }
        if (nodeBuffer.getNormalsBuffer().isPresent()) {
            ByteBuffer normalsBuffer = nodeBuffer.getNormalsBuffer().get();
            nodeBuffer.setNormalsBufferViewId(createBufferView(gltf, bufferId, bufferOffset, normalsBuffer.capacity(), 12, GL20.GL_ARRAY_BUFFER));
            bufferOffset += normalsBuffer.capacity();
        }
        if (nodeBuffer.getColorsBuffer().isPresent()) {
            ByteBuffer colorsBuffer = nodeBuffer.getColorsBuffer().get();
            nodeBuffer.setColorsBufferViewId(createBufferView(gltf, bufferId, bufferOffset, colorsBuffer.capacity(), 16, GL20.GL_ARRAY_BUFFER));
            bufferOffset += colorsBuffer.capacity();
        }
        if (nodeBuffer.getTextureCoordinatesBuffer().isPresent()) {
            ByteBuffer textureCoordinatesBuffer = nodeBuffer.getTextureCoordinatesBuffer().get();
            nodeBuffer.setTextureCoordinatesBufferViewId(createBufferView(gltf, bufferId, bufferOffset, textureCoordinatesBuffer.capacity() * 2, 8, GL20.GL_ARRAY_BUFFER));
            bufferOffset += textureCoordinatesBuffer.capacity();
        }
        buffer.setByteLength(bufferOffset);
        return bufferId;
    }

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

    private static boolean isIdentity(float[] matrix) {
        return matrix[0] == 1 && matrix[1] == 0 && matrix[2] == 0 && matrix[3] == 0 &&
                matrix[4] == 0 && matrix[5] == 1 && matrix[6] == 0 && matrix[7] == 0 &&
                matrix[8] == 0 && matrix[9] == 0 && matrix[10] == 1 && matrix[11] == 0 &&
                matrix[12] == 0 && matrix[13] == 0 && matrix[14] == 0 && matrix[15] == 1;
    }

    private static Node createNode(GlTF gltf, Node parentNode, GaiaNode gaiaNode) {
        if (parentNode != null) {
            parentNode = gltf.getNodes().get(0);
        }
        Node node = new Node();
        //node.setMesh(mesh);
        float[] matrix = gaiaNode.getTransform().get(new float[16]);
        if (matrix != null && !isIdentity(matrix)) {
            node.setMatrix(matrix);
        }
        node.setName(gaiaNode.getName());
        return node;
    }

    private static int addMeshNode(GlTF gltf, int mesh, GaiaNode gaiaNode, String name) {
        Node root = gltf.getNodes().get(0);
        Node node = new Node();
        node.setMesh(mesh);

        float[] matrix = gaiaNode.getTransform().get(new float[16]);
        if (matrix != null && !isIdentity(matrix)) {
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

    private static Sampler genSampler() {
        Sampler sampler = new Sampler();
        sampler.setMagFilter(GL20.GL_LINEAR);
        sampler.setMinFilter(GL20.GL_LINEAR_MIPMAP_LINEAR);
        sampler.setWrapS(GL20.GL_REPEAT);
        sampler.setWrapT(GL20.GL_REPEAT);
        return sampler;
    }

    private static int createMaterial(GlTF gltf, GaiaMaterial gaiaMaterial) {
        GaiaTexture gaiaTexture = gaiaMaterial.getTextures().get(GaiaMaterial.MaterialType.DIFFUSE);
        Material material = new Material();
        material.setName(gaiaMaterial.getName());
        material.setDoubleSided(false);

        int textureId = createTexture(gltf, gaiaTexture);
        TextureInfo textureInfo = new TextureInfo();
        textureInfo.setIndex(textureId);

        MaterialPbrMetallicRoughness pbrMetallicRoughness = new MaterialPbrMetallicRoughness();
        pbrMetallicRoughness.setBaseColorFactor(new float[]{1, 1, 1, 1});
        pbrMetallicRoughness.setBaseColorTexture(textureInfo);
        material.setPbrMetallicRoughness(pbrMetallicRoughness);
        gltf.addMaterials(material);
        return gltf.getMaterials().size() - 1;
    }

    private static int createImage(GlTF gltf, GaiaTexture gaiaTexture) {
        String mimeType = "image/jpeg";
        String uri = FileUtils.writeImage(gaiaTexture.getBufferedImage(), mimeType);
        Image image = new Image();
        image.setUri(uri);
        image.setMimeType(mimeType);

        gltf.addImages(image);
        return gltf.getImages().size() -1;
    }

    private static int createTexture(GlTF gltf, GaiaTexture gaiaTexture) {
        int imageSource = createImage(gltf, gaiaTexture);

        Texture texture = new Texture();
        texture.setSampler(0);
        texture.setSource(imageSource);
        texture.setName(gaiaTexture.getName());

        gltf.addTextures(texture);
        return gltf.getTextures().size() - 1;
    }

    /*private static int createMeshWithPrimitive(GlTF gltf, int mesh, GltfNodeBuffer nodeBuffer) {
        MeshPrimitive primitive = new MeshPrimitive();

        primitive.setMode(GltfConstants.GL_TRIANGLES);
        //primitive.setMaterial(nodeBuffer.getMaterialId());  //TODO
        primitive.setAttributes(new HashMap<>());
        primitive.setIndices(nodeBuffer.getIndicesAccessorId());

        if (nodeBuffer.getPositionsAccessorId() > -1)
            primitive.getAttributes().put("POSITION", nodeBuffer.getPositionsAccessorId());
        if (nodeBuffer.getNormalsAccessorId() > -1)
            primitive.getAttributes().put("NORMAL", nodeBuffer.getNormalsAccessorId());
        if (nodeBuffer.getColorsAccessorId() > -1)
            primitive.getAttributes().put("COLOR_0", nodeBuffer.getColorsAccessorId());
        if (nodeBuffer.getTextureCoordinatesAccessorId() > -1)
            primitive.getAttributes().put("TEXCOORD_0", nodeBuffer.getTextureCoordinatesAccessorId());

        if (mesh == -1)
            mesh = createMesh(gltf);
        gltf.getMeshes().get(mesh).addPrimitives(primitive);
        return mesh;
    }*/

    private static MeshPrimitive createPrimitive(GltfNodeBuffer nodeBuffer, int materialId) {
        MeshPrimitive primitive = new MeshPrimitive();
        primitive.setMode(GltfConstants.GL_TRIANGLES);
        primitive.setMaterial(materialId);
        //primitive.setMaterial(nodeBuffer.getMaterialId());
        primitive.setAttributes(new HashMap<>());
        primitive.setIndices(nodeBuffer.getIndicesAccessorId());

        if (nodeBuffer.getPositionsAccessorId() > -1)
            primitive.getAttributes().put("POSITION", nodeBuffer.getPositionsAccessorId());
        if (nodeBuffer.getNormalsAccessorId() > -1)
            primitive.getAttributes().put("NORMAL", nodeBuffer.getNormalsAccessorId());
        if (nodeBuffer.getColorsAccessorId() > -1)
            primitive.getAttributes().put("COLOR_0", nodeBuffer.getColorsAccessorId());
        if (nodeBuffer.getTextureCoordinatesAccessorId() > -1)
            primitive.getAttributes().put("TEXCOORD_0", nodeBuffer.getTextureCoordinatesAccessorId());

        return primitive;
    }

    private static int createMesh(GlTF gltf, MeshPrimitive primitive) {
        Mesh mesh = new Mesh();
        mesh.addWeights(1.0f);
        mesh.addPrimitives(primitive);
        gltf.addMeshes(mesh);
        return gltf.getMeshes().size() - 1;
    }
}
