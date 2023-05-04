package gltf;

import de.javagl.jgltf.impl.v2.*;
import de.javagl.jgltf.model.GltfConstants;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.GltfModels;
import de.javagl.jgltf.model.io.GltfModelWriter;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import geometry.structure.*;
import geometry.types.AccessorType;
import geometry.types.AttributeType;
import geometry.types.TextureType;
import org.joml.Matrix4d;
import org.joml.Vector4d;
import org.lwjgl.opengl.GL20;
import util.FileUtils;
import util.GeometryUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.List;

public class GltfWriter {

    private static int FLOAT_SIZE = 4;

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
        GltfBinary binary = new GltfBinary();
        GlTF gltf = new GlTF();
        gltf.setAsset(genAsset());
        gltf.addSamplers(genSampler());
        initScene(gltf);
        if (binary != null) {
            gaiaScene.getMaterials().stream().forEach(gaiaMaterial -> {
                int materialId = createMaterial(gltf, gaiaMaterial);
            });
            convertNode(gltf, binary, null, gaiaScene.getNodes());
            binary.fill();
        }
        if (binary.getBody().isPresent()) {
            GltfAssetV2 asset = new GltfAssetV2(gltf, binary.getBody().get());
            GltfModel gltfModel = GltfModels.create(asset);
            return gltfModel;
        }
        return null;
    }

    private static void convertNode(GlTF gltf, GltfBinary binary, Node parentNode, List<GaiaNode> gaiaNodes) {
        List<GltfNodeBuffer> nodeBuffers = binary.getNodeBuffers();
        gaiaNodes.stream().forEach((gaiaNode) -> {
            Node node = createNode(gltf, parentNode, gaiaNode);
            int nodeId = gltf.getNodes().size() - 1;
            if (parentNode != null) {
                parentNode.addChildren(nodeId);
            }

            List<GaiaNode> children = gaiaNode.getChildren();
            if (children.size() > 0) {
                convertNode(gltf, binary, node, children);
            }

            List<GaiaMesh> gaiaMeshes = gaiaNode.getMeshes();
            gaiaMeshes.stream().forEach((gaiaMesh) -> {
                GltfNodeBuffer nodeBuffer = convertGeometryInfo(gltf, gaiaMesh, node);
                nodeBuffers.add(nodeBuffer);
            });
        });
    }

    private static GltfNodeBuffer convertGeometryInfo(GlTF gltf, GaiaMesh gaiaMesh, Node node) {
        GltfNodeBuffer nodeBuffer = initNodeBuffer(gaiaMesh);
        createBuffer(gltf, nodeBuffer);

        List<Short> indices = gaiaMesh.getIndices();
        List<Float> positions = gaiaMesh.getPositions();
        List<Float> normals = gaiaMesh.getNormals();
        List<Float> colors = gaiaMesh.getColors();
        List<Float> textureCoordinates = gaiaMesh.getTextureCoordinates();

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

        if (indicesBuffer.isPresent()) {
            ByteBuffer indicesByteBuffer = indicesBuffer.get();
            indices.stream().forEach((indice) -> {
                indicesByteBuffer.putShort(indice);
            });
        }
        if (positionsBuffer.isPresent()) {
            ByteBuffer positionByteBuffer = positionsBuffer.get();
            for (Float position: positions) {
                positionByteBuffer.putFloat(position);
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

        if (indicesBufferViewId > -1 && indices.size() > 0) {
            int indicesAccessorId = createAccessor(gltf, indicesBufferViewId, 0, indices.size(), GltfConstants.GL_UNSIGNED_SHORT, AccessorType.SCALAR/*, min, max*/);
            nodeBuffer.setIndicesAccessorId(indicesAccessorId);
        }
        if (positionsBufferViewId > -1 && positions.size() > 0) {
            int verticesAccessorId = createAccessor(gltf, positionsBufferViewId, 0, positions.size() / 3, GltfConstants.GL_FLOAT, AccessorType.VEC3/*, min, max*/);
            nodeBuffer.setPositionsAccessorId(verticesAccessorId);
        }
        if (normalsBufferViewId > -1 && normals.size() > 0) {
            int normalsAccessorId = createAccessor(gltf, normalsBufferViewId, 0, normals.size() / 3, GltfConstants.GL_FLOAT, AccessorType.VEC3/*, min, max*/);
            nodeBuffer.setNormalsAccessorId(normalsAccessorId);
        }
        if (colorsBufferViewId > -1 && colors.size() > 0) {
            int colorsAccessorId = createAccessor(gltf, colorsBufferViewId, 0, colors.size() / 4, GltfConstants.GL_FLOAT, AccessorType.VEC4/*, min, max*/);
            nodeBuffer.setColorsAccessorId(colorsAccessorId);
        }
        if (textureCoordinatesBufferViewId > -1 && textureCoordinates.size() > 0) {
            int textureCoordinatesAccessorId = createAccessor(gltf, textureCoordinatesBufferViewId, 0, textureCoordinates.size() / 2, GltfConstants.GL_FLOAT, AccessorType.VEC2/*, min, max*/);
            nodeBuffer.setTextureCoordinatesAccessorId(textureCoordinatesAccessorId);
        }

        List<Material> materials = gltf.getMaterials();
        GaiaPrimitive gaiaPrimitive = gaiaMesh.getPrimitives().get(0);
        MeshPrimitive primitive = createPrimitive(nodeBuffer, gaiaPrimitive, materials); // TODO
        int meshId = createMesh(gltf, primitive);
        node.setMesh(meshId);
        return nodeBuffer;
    }

    private static GltfNodeBuffer initNodeBuffer(GaiaMesh gaiaMesh) {
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
        rootNode.setName("default root node");

        Matrix4d matrix4d = new Matrix4d();
        matrix4d.identity();
        rootNode.setMatrix(matrix4d.get(new float[16]));

        nodes.add(rootNode);
        gltf.setNodes(nodes);
        scene.addNodes(gltf.getNodes().size() -1);
        scenes.add(scene);
        gltf.setScenes(scenes);
        gltf.setScene(gltf.getScenes().size() - 1);
    }

    private static Buffer initBuffer(GlTF gltf) {
        if (gltf.getBuffers() == null) {
            Buffer buffer = new Buffer();
            gltf.addBuffers(buffer);
        }
        return gltf.getBuffers().get(0);
    }

    private static int createBuffer(GlTF gltf, GltfNodeBuffer nodeBuffer) {


        /*int calcBufferOffset = 0;
        if (gltf.getBuffers() != null) {
            calcBufferOffset = gltf.getBuffers().stream().mapToInt(preNodeBuffer -> {
                Integer byteLength = preNodeBuffer.getByteLength();
                return byteLength == null ? 0 : byteLength;
            }).sum();
        }*/
        //int buffId = gltf.getBuffers().size() - 1;
        //System.out.println("calcBufferOffset : " + calcBufferOffset);
        //gltf.addBuffers(buffer);
        Buffer buffer = initBuffer(gltf);
        int bufferLength = buffer.getByteLength() == null ? 0 : buffer.getByteLength();
        int bufferId = 0;
        int bufferOffset = 0;
        if (nodeBuffer.getIndicesBuffer().isPresent()) {
            ByteBuffer indicesBuffer = nodeBuffer.getIndicesBuffer().get();
            int bufferViewId = createBufferView(gltf, bufferId, bufferLength + bufferOffset, indicesBuffer.capacity(), -1, GL20.GL_ELEMENT_ARRAY_BUFFER);
            nodeBuffer.setIndicesBufferViewId(bufferViewId);
            BufferView bufferView = gltf.getBufferViews().get(bufferViewId);
            bufferView.setName("indices");
            bufferOffset += indicesBuffer.capacity();
        }
        if (nodeBuffer.getPositionsBuffer().isPresent()) {
            ByteBuffer positionBuffer = nodeBuffer.getPositionsBuffer().get();
            int bufferViewId = createBufferView(gltf, bufferId, bufferLength + bufferOffset, positionBuffer.capacity(), 12, GL20.GL_ARRAY_BUFFER);
            nodeBuffer.setPositionsBufferViewId(bufferViewId);
            BufferView bufferView = gltf.getBufferViews().get(bufferViewId);
            bufferView.setName("positions");
            bufferOffset += positionBuffer.capacity();
        }
        if (nodeBuffer.getNormalsBuffer().isPresent()) {
            ByteBuffer normalsBuffer = nodeBuffer.getNormalsBuffer().get();
            int bufferViewId = createBufferView(gltf, bufferId, bufferLength + bufferOffset, normalsBuffer.capacity(), 12, GL20.GL_ARRAY_BUFFER);
            nodeBuffer.setNormalsBufferViewId(bufferViewId);
            BufferView bufferView = gltf.getBufferViews().get(bufferViewId);
            bufferView.setName("normals");
            bufferOffset += normalsBuffer.capacity();
        }
        if (nodeBuffer.getColorsBuffer().isPresent()) {
            ByteBuffer colorsBuffer = nodeBuffer.getColorsBuffer().get();
            int bufferViewId = createBufferView(gltf, bufferId, bufferLength + bufferOffset, colorsBuffer.capacity(), 16, GL20.GL_ARRAY_BUFFER);
            nodeBuffer.setColorsBufferViewId(bufferViewId);
            BufferView bufferView = gltf.getBufferViews().get(bufferViewId);
            bufferView.setName("colors");
            bufferOffset += colorsBuffer.capacity();
        }
        if (nodeBuffer.getTextureCoordinatesBuffer().isPresent()) {
            ByteBuffer textureCoordinatesBuffer = nodeBuffer.getTextureCoordinatesBuffer().get();
            int bufferViewId = createBufferView(gltf, bufferId, bufferLength + bufferOffset, textureCoordinatesBuffer.capacity(), 8, GL20.GL_ARRAY_BUFFER);
            nodeBuffer.setTextureCoordinatesBufferViewId(bufferViewId);
            BufferView bufferView = gltf.getBufferViews().get(bufferViewId);
            bufferView.setName("textureCoordinates");
            bufferOffset += textureCoordinatesBuffer.capacity();
        }
        buffer.setByteLength(bufferLength + bufferOffset);
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

    private static Node createNode(GlTF gltf, Node parentNode, GaiaNode gaiaNode) {
        Node node;
        if (parentNode == null) {
            node = gltf.getNodes().get(0); // root node
        } else {
            node = new Node();
            gltf.addNodes(node);
        }
        float[] matrix = gaiaNode.getTransformMatrix().get(new float[16]);
        if (matrix != null && !GeometryUtils.isIdentity(matrix)) {
            node.setMatrix(matrix);
        }
        node.setName(gaiaNode.getName());
        return node;
    }

    private static int createMaterial(GlTF gltf, GaiaMaterial gaiaMaterial) {
        List<GaiaTexture> diffuseTextures = gaiaMaterial.getTextures().get(TextureType.DIFFUSE);

        Material material = new Material();
        material.setName(gaiaMaterial.getName());
        material.setDoubleSided(false);

        MaterialPbrMetallicRoughness pbrMetallicRoughness = new MaterialPbrMetallicRoughness();
        Vector4d diffuseColor = gaiaMaterial.getDiffuseColor();
        pbrMetallicRoughness.setBaseColorFactor(new float[]{(float) diffuseColor.x, (float) diffuseColor.y, (float) diffuseColor.z, (float) diffuseColor.w});

        if (diffuseTextures.size() > 0) {
            GaiaTexture gaiaTexture = diffuseTextures.get(0);
            int textureId = createTexture(gltf, gaiaTexture);
            TextureInfo textureInfo = new TextureInfo();
            textureInfo.setIndex(textureId);
            pbrMetallicRoughness.setBaseColorTexture(textureInfo);
        }

        material.setPbrMetallicRoughness(pbrMetallicRoughness);
        gltf.addMaterials(material);
        return gltf.getMaterials().size() - 1;
    }

    private static int createImage(GlTF gltf, GaiaTexture gaiaTexture) {
        String extension = FileUtils.getExtension(gaiaTexture.getPath());
        String mimeType = FileUtils.getMimeTypeByExtension(extension);
        String uri = FileUtils.writeImage(gaiaTexture.getBufferedImage(), mimeType);
        Image image = new Image();
        image.setUri(uri);
        image.setMimeType(mimeType);
        gltf.addImages(image);
        return gltf.getImages().size() -1;
    }

    private static int createTexture(GlTF gltf, GaiaTexture gaiaTexture) {
        if (gaiaTexture.getBufferedImage() == null) {
            gaiaTexture.readImage();
        }
        int imageSource = createImage(gltf, gaiaTexture);

        Texture texture = new Texture();
        texture.setSampler(0);
        texture.setSource(imageSource);
        texture.setName(gaiaTexture.getName());

        gltf.addTextures(texture);
        return gltf.getTextures().size() - 1;
    }

    private static int createAccessor(GlTF gltf, int bufferView, int byteOffset, int count, int componentType, AccessorType accessorType/*, Number[] min, Number[] max*/) {
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

    private static MeshPrimitive createPrimitive(GltfNodeBuffer nodeBuffer, GaiaPrimitive gaiaPrimitive, List<Material> materials) {
        MeshPrimitive primitive = new MeshPrimitive();
        primitive.setMode(GltfConstants.GL_TRIANGLES);
        if (materials != null && materials.size() > 0) {
            primitive.setMaterial(gaiaPrimitive.getMaterialIndex());
        }
        primitive.setAttributes(new HashMap<>());
        primitive.setIndices(nodeBuffer.getIndicesAccessorId());

        if (nodeBuffer.getPositionsAccessorId() > -1)
            primitive.getAttributes().put(AttributeType.POSITION.getAccessor(), nodeBuffer.getPositionsAccessorId());
        if (nodeBuffer.getNormalsAccessorId() > -1)
            primitive.getAttributes().put(AttributeType.NORMAL.getAccessor(), nodeBuffer.getNormalsAccessorId());
        if (nodeBuffer.getColorsAccessorId() > -1)
            primitive.getAttributes().put(AttributeType.COLOR.getAccessor(), nodeBuffer.getColorsAccessorId());
        if (nodeBuffer.getTextureCoordinatesAccessorId() > -1)
            primitive.getAttributes().put(AttributeType.TEXCOORD.getAccessor(), nodeBuffer.getTextureCoordinatesAccessorId());

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
