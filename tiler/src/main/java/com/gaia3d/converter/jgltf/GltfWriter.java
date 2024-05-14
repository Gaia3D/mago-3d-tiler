package com.gaia3d.converter.jgltf;

import com.gaia3d.basic.structure.*;
import com.gaia3d.basic.types.AccessorType;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.util.GeometryUtils;
import com.gaia3d.util.ImageResizer;
import com.gaia3d.util.ImageUtils;
import de.javagl.jgltf.impl.v2.*;
import de.javagl.jgltf.model.GltfConstants;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.GltfModels;
import de.javagl.jgltf.model.io.GltfModelWriter;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL20;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * GltfWriter is a class that writes the glTF file.
 * It contains the method to write the glTF file from the GaiaScene object.
 * The glTF file is written in the glTF 2.0 format.
 * author znkim
 * @since 1.0.0
 * @see GaiaScene , GltfBinary
 */
@Slf4j
@NoArgsConstructor
public class GltfWriter {
    public void writeGltf(GaiaScene gaiaScene, File outputPath) {
        try {
            GltfModel gltfModel = convert(gaiaScene);
            GltfModelWriter writer = new GltfModelWriter();
            writer.writeEmbedded(gltfModel, outputPath);
        } catch (IOException e) {
            log.error(e.getMessage());
            log.error("Failed to write glTF file.");
        }
    }
    public void writeGltf(GaiaScene gaiaScene, String outputPath) {
        writeGltf(gaiaScene, new File(outputPath));
    }
    public void writeGlb(GaiaScene gaiaScene, File outputPath) {
        try {
            GltfModel gltfModel = convert(gaiaScene);
            GltfModelWriter writer = new GltfModelWriter();
            writer.writeBinary(gltfModel, outputPath);
        } catch (IOException e) {
            log.error(e.getMessage());
            log.error("Failed to write glb file.");
        }
    }
    public void writeGlb(GaiaScene gaiaScene, OutputStream outputStream) {
        try {
            GltfModel gltfModel = convert(gaiaScene);
            GltfModelWriter writer = new GltfModelWriter();
            writer.writeBinary(gltfModel, outputStream);
            outputStream.close();
        } catch (IOException e) {
            log.error(e.getMessage());
            log.error("Failed to write glb file.");
        }
    }
    public void writeGlb(GaiaScene gaiaScene, String outputPath) {
        writeGlb(gaiaScene, new File(outputPath));
    }

    private GltfModel convert(GaiaScene gaiaScene) {
        GltfBinary binary = new GltfBinary();
        GlTF gltf = new GlTF();
        gltf.setAsset(genAsset());
        gltf.addSamplers(genSampler());
        initScene(gltf);

        gaiaScene.getMaterials().forEach(gaiaMaterial -> createMaterial(gltf, gaiaMaterial));
        convertNode(gltf, binary, null, gaiaScene.getNodes());
        
        binary.fill();
        if (binary.getBody() != null) {
            GltfAssetV2 asset = new GltfAssetV2(gltf, binary.getBody());
            return GltfModels.create(asset);
        }
        return null;
    }

    private void convertNode(GlTF gltf, GltfBinary binary, Node parentNode, List<GaiaNode> gaiaNodes) {
        List<GltfNodeBuffer> nodeBuffers = binary.getNodeBuffers();
        gaiaNodes.forEach((gaiaNode) -> {
            Node node = createNode(gltf, parentNode, gaiaNode);
            int nodeId = gltf.getNodes().size() - 1;
            if (parentNode != null) {
                parentNode.addChildren(nodeId);
            }

            List<GaiaNode> children = gaiaNode.getChildren();
            if (!children.isEmpty()) {
                convertNode(gltf, binary, node, children);
            }

            List<GaiaMesh> gaiaMeshes = gaiaNode.getMeshes();
            gaiaMeshes.forEach((gaiaMesh) -> {
                GltfNodeBuffer nodeBuffer = convertGeometryInfo(gltf, gaiaMesh, node);
                nodeBuffers.add(nodeBuffer);
            });
        });
    }

    private Byte convertNormal(Float normalValue) {
        byte normalByte = (byte) (normalValue * 127);
        return normalByte;
    }

    private byte[] convertNormals(float[] normalValues) {
        int length = (normalValues.length / 3) * 4;
        int index = 0;
        byte[] normalBytes = new byte[length];
        for (int i = 0; i < length; i += 4) {
            float x = normalValues[index++];
            float y = normalValues[index++];
            float z = normalValues[index++];

            // Normalize the normal vector
            Vector3d vector3d = new Vector3d(x, y, z);
            vector3d.normalize();

            normalBytes[i] = convertNormal((float) vector3d.x);
            normalBytes[i + 1] = convertNormal((float) vector3d.y);
            normalBytes[i + 2] = convertNormal((float) vector3d.z);
            normalBytes[i + 3] = (byte) 1;
        }
        return normalBytes;
    }

    private GltfNodeBuffer convertGeometryInfo(GlTF gltf, GaiaMesh gaiaMesh, Node node) {
        int[] indices = gaiaMesh.getIndices();
        float[] positions = gaiaMesh.getPositions();
        float[] normals = gaiaMesh.getNormals();
        //byte[] normalBytes = convertNormals(normals);
        byte[] colors = gaiaMesh.getColors();
        float[] texcoords = gaiaMesh.getTexcoords();
        float[] batchIds = gaiaMesh.getBatchIds();

        /*boolean isIntegerIndices = gaiaMesh.getIndicesCount() >= 65535;
        if (isIntegerIndices) {
            log.warn("Integer indices are used. The number of indices is greater than {}/65535", gaiaMesh.getIndicesCount());
        }*/


        int vertexCount = gaiaMesh.getPositionsCount() / 3;
        boolean isOverShortVertices = vertexCount / 3 >= 65535;
        if (isOverShortVertices) {
            log.warn("[Warning] The number of vertices count than 65535 ({})", vertexCount);
        }


        GltfNodeBuffer nodeBuffer = initNodeBuffer(gaiaMesh, isOverShortVertices);
        createBuffer(gltf, nodeBuffer);

        ByteBuffer indicesBuffer = nodeBuffer.getIndicesBuffer();
        ByteBuffer positionsBuffer = nodeBuffer.getPositionsBuffer();
        ByteBuffer normalsBuffer = nodeBuffer.getNormalsBuffer();
        ByteBuffer colorsBuffer = nodeBuffer.getColorsBuffer();
        ByteBuffer texcoordsBuffer = nodeBuffer.getTexcoordsBuffer();
        ByteBuffer batchIdBuffer = nodeBuffer.getBatchIdBuffer();

        int indicesBufferViewId = nodeBuffer.getIndicesBufferViewId();
        int positionsBufferViewId = nodeBuffer.getPositionsBufferViewId();
        int normalsBufferViewId = nodeBuffer.getNormalsBufferViewId();
        int colorsBufferViewId = nodeBuffer.getColorsBufferViewId();
        int texcoordsBufferViewId = nodeBuffer.getTexcoordsBufferViewId();
        int batchIdBufferViewId = nodeBuffer.getBatchIdBufferViewId();

        if (indicesBuffer != null) {
            for (int indicesValue: indices) {
                if (isOverShortVertices) {
                    indicesBuffer.putInt(indicesValue);
                } else {
                    short indicesValueShort = (short) indicesValue;
                    indicesBuffer.putShort(indicesValueShort);
                }
            }
        }
        if (positionsBuffer != null) {
            for (Float position: positions) {
                positionsBuffer.putFloat(position);
            }
        }
        if (normalsBuffer != null) {
            for (Float normal: normals) {
                normalsBuffer.putFloat(normal);
            }
        }
        if (colorsBuffer != null) {
            for (Byte color: colors) {
                colorsBuffer.put(color);
            }
        }
        if (texcoordsBuffer != null) {
            for (Float textureCoordinate: texcoords) {
                texcoordsBuffer.putFloat(textureCoordinate);
            }
        }
        if (batchIdBuffer != null) {
            for (Float batchId: batchIds) {
                batchIdBuffer.putFloat(batchId);
            }
        }

        if (indicesBufferViewId > -1 && indices.length > 0) {
            if (isOverShortVertices) {
                int indicesAccessorId = createAccessor(gltf, indicesBufferViewId, 0, indices.length, GltfConstants.GL_UNSIGNED_INT, AccessorType.SCALAR, false);
                nodeBuffer.setIndicesAccessorId(indicesAccessorId);
            } else {
                int indicesAccessorId = createAccessor(gltf, indicesBufferViewId, 0, indices.length, GltfConstants.GL_UNSIGNED_SHORT, AccessorType.SCALAR, false);
                nodeBuffer.setIndicesAccessorId(indicesAccessorId);
            }
        }
        if (positionsBufferViewId > -1 && positions.length > 0) {
            int verticesAccessorId = createAccessor(gltf, positionsBufferViewId, 0, positions.length / 3, GltfConstants.GL_FLOAT, AccessorType.VEC3, false);
            nodeBuffer.setPositionsAccessorId(verticesAccessorId);
        }
        if (normalsBufferViewId > -1 && normals.length > 0) {
            int normalsAccessorId = createAccessor(gltf, normalsBufferViewId, 0, normals.length / 3, GltfConstants.GL_FLOAT, AccessorType.VEC3, false);
            nodeBuffer.setNormalsAccessorId(normalsAccessorId);
        }
        if (colorsBufferViewId > -1 && colors.length > 0) {
            int colorsAccessorId = createAccessor(gltf, colorsBufferViewId, 0, colors.length / 4, GltfConstants.GL_UNSIGNED_BYTE, AccessorType.VEC4, true);
            nodeBuffer.setColorsAccessorId(colorsAccessorId);
        }
        if (texcoordsBufferViewId > -1 && texcoords.length > 0) {
            int texcoordsAccessorId = createAccessor(gltf, texcoordsBufferViewId, 0, texcoords.length / 2, GltfConstants.GL_FLOAT, AccessorType.VEC2, false);
            nodeBuffer.setTexcoordsAccessorId(texcoordsAccessorId);
        }
        if (batchIdBufferViewId > -1 && batchIds.length > 0) {
            int batchIdAccessorId = createAccessor(gltf, batchIdBufferViewId, 0, batchIds.length, GltfConstants.GL_FLOAT, AccessorType.SCALAR, false);
            nodeBuffer.setBatchIdAccessorId(batchIdAccessorId);
        }

        List<Material> materials = gltf.getMaterials();
        GaiaPrimitive gaiaPrimitive = gaiaMesh.getPrimitives().get(0);
        MeshPrimitive primitive = createPrimitive(nodeBuffer, gaiaPrimitive, materials);
        int meshId = createMesh(gltf, primitive);
        node.setMesh(meshId);
        return nodeBuffer;
    }

    private int padMultiple4(int value) {
        int remainder = value % 4;
        if (remainder == 0) {
            return value;
        }
        return value + (4 - remainder);
    }

    private GltfNodeBuffer initNodeBuffer(GaiaMesh gaiaMesh, boolean isIntegerIndices) {
        GltfNodeBuffer nodeBuffer = new GltfNodeBuffer();
        int SHORT_SIZE = 2;
        int INT_SIZE = 4;
        int FLOAT_SIZE = 4;

        int indicesCapacity = gaiaMesh.getIndicesCount() * (isIntegerIndices ? INT_SIZE : SHORT_SIZE);
        int positionsCapacity = gaiaMesh.getPositionsCount() * FLOAT_SIZE;
        int normalsCapacity = gaiaMesh.getPositionsCount() * FLOAT_SIZE;
        int colorsCapacity = gaiaMesh.getColorsCount();
        int texcoordCapacity = gaiaMesh.getTexcoordsCount() * FLOAT_SIZE;
        int batchIdCapacity = gaiaMesh.getBatchIdsCount() * FLOAT_SIZE;

        indicesCapacity = padMultiple4(indicesCapacity);
        positionsCapacity = padMultiple4(positionsCapacity);
        normalsCapacity = padMultiple4(normalsCapacity);
        colorsCapacity = padMultiple4(colorsCapacity);
        texcoordCapacity = padMultiple4(texcoordCapacity);
        batchIdCapacity = padMultiple4(batchIdCapacity);

        int bodyLength = 0;
        bodyLength += indicesCapacity;
        bodyLength += positionsCapacity;
        bodyLength += normalsCapacity;
        bodyLength += colorsCapacity;
        bodyLength += texcoordCapacity;
        bodyLength += batchIdCapacity;

        nodeBuffer.setTotalByteBufferLength(bodyLength);
        if (indicesCapacity > 0) {
            ByteBuffer indicesBuffer = ByteBuffer.allocate(indicesCapacity);
            indicesBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nodeBuffer.setIndicesBuffer(indicesBuffer);
        }
        if (positionsCapacity > 0) {
            ByteBuffer positionsBuffer = ByteBuffer.allocate(positionsCapacity);
            positionsBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nodeBuffer.setPositionsBuffer(positionsBuffer);
        }
        if (normalsCapacity > 0) {
            ByteBuffer normalsBuffer = ByteBuffer.allocate(normalsCapacity);
            normalsBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nodeBuffer.setNormalsBuffer(normalsBuffer);
        }
        if (colorsCapacity > 0) {
            ByteBuffer colorsBuffer = ByteBuffer.allocate(colorsCapacity);
            colorsBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nodeBuffer.setColorsBuffer(colorsBuffer);
        }
        if (texcoordCapacity > 0) {
            ByteBuffer texcoordsBuffer = ByteBuffer.allocate(texcoordCapacity);
            texcoordsBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nodeBuffer.setTexcoordsBuffer(texcoordsBuffer);
        }
        if (batchIdCapacity > 0) {
            ByteBuffer batchIdBuffer = ByteBuffer.allocate(batchIdCapacity);
            batchIdBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nodeBuffer.setBatchIdBuffer(batchIdBuffer);
        }
        return nodeBuffer;
    }

    private Asset genAsset() {
        Asset asset = new Asset();
        asset.setGenerator("plasma");
        asset.setCopyright("gaia3d");
        asset.setVersion("2.0");
        asset.setMinVersion("2.0");
        return asset;
    }

    private void initScene(GlTF gltf) {
        List<Scene> scenes = new ArrayList<>();
        Scene scene = new Scene();
        List<Node> nodes = new ArrayList<>();
        Node rootNode = new Node();
        rootNode.setName("RootNode");

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

    private Buffer initBuffer(GlTF gltf) {
        if (gltf.getBuffers() == null) {
            Buffer buffer = new Buffer();
            gltf.addBuffers(buffer);
        }
        return gltf.getBuffers().get(0);
    }

    private void createBuffer(GlTF gltf, GltfNodeBuffer nodeBuffer) {
        Buffer buffer = initBuffer(gltf);
        int bufferLength = buffer.getByteLength() == null ? 0 : buffer.getByteLength();
        int bufferId = 0;
        int bufferOffset = 0;
        if (nodeBuffer.getIndicesBuffer() != null) {
            ByteBuffer indicesBuffer = nodeBuffer.getIndicesBuffer();
            int bufferViewId = createBufferView(gltf, bufferId, bufferLength + bufferOffset, indicesBuffer.capacity(), -1, GL20.GL_ELEMENT_ARRAY_BUFFER);
            nodeBuffer.setIndicesBufferViewId(bufferViewId);
            BufferView bufferView = gltf.getBufferViews().get(bufferViewId);
            bufferView.setName("indices");
            bufferOffset += indicesBuffer.capacity();
        }
        if (nodeBuffer.getPositionsBuffer() != null) {
            ByteBuffer positionBuffer = nodeBuffer.getPositionsBuffer();
            int bufferViewId = createBufferView(gltf, bufferId, bufferLength + bufferOffset, positionBuffer.capacity(), 12, GL20.GL_ARRAY_BUFFER);
            nodeBuffer.setPositionsBufferViewId(bufferViewId);
            BufferView bufferView = gltf.getBufferViews().get(bufferViewId);
            bufferView.setName("positions");
            bufferOffset += positionBuffer.capacity();
        }
        if (nodeBuffer.getNormalsBuffer() != null) {
            ByteBuffer normalsBuffer = nodeBuffer.getNormalsBuffer();
            int bufferViewId = createBufferView(gltf, bufferId, bufferLength + bufferOffset, normalsBuffer.capacity(), 12, GL20.GL_ARRAY_BUFFER);
            nodeBuffer.setNormalsBufferViewId(bufferViewId);
            BufferView bufferView = gltf.getBufferViews().get(bufferViewId);
            bufferView.setName("normals");
            bufferOffset += normalsBuffer.capacity();
        }
        if (nodeBuffer.getColorsBuffer() != null) {
            ByteBuffer colorsBuffer = nodeBuffer.getColorsBuffer();
            int bufferViewId = createBufferView(gltf, bufferId, bufferLength + bufferOffset, colorsBuffer.capacity(), 4, GL20.GL_ARRAY_BUFFER);
            nodeBuffer.setColorsBufferViewId(bufferViewId);
            BufferView bufferView = gltf.getBufferViews().get(bufferViewId);
            bufferView.setName("colors");
            bufferOffset += colorsBuffer.capacity();
        }
        if (nodeBuffer.getTexcoordsBuffer() != null) {
            ByteBuffer texcoordsBuffer = nodeBuffer.getTexcoordsBuffer();
            int bufferViewId = createBufferView(gltf, bufferId, bufferLength + bufferOffset, texcoordsBuffer.capacity(), 8, GL20.GL_ARRAY_BUFFER);
            nodeBuffer.setTexcoordsBufferViewId(bufferViewId);
            BufferView bufferView = gltf.getBufferViews().get(bufferViewId);
            bufferView.setName("texcoords");
            bufferOffset += texcoordsBuffer.capacity();
        }
        if (nodeBuffer.getBatchIdBuffer() != null) {
            ByteBuffer batchIdBuffer = nodeBuffer.getBatchIdBuffer();
            int bufferViewId = createBufferView(gltf, bufferId, bufferLength + bufferOffset, batchIdBuffer.capacity(), 4, GL20.GL_ARRAY_BUFFER);
            nodeBuffer.setBatchIdBufferViewId(bufferViewId);
            BufferView bufferView = gltf.getBufferViews().get(bufferViewId);
            bufferView.setName("batchIds");
            bufferOffset += batchIdBuffer.capacity();
        }
        buffer.setByteLength(bufferLength + bufferOffset);
    }

    private int createBufferView(GlTF gltf, int buffer, int offset, int length, int stride, int target) {
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

    private Node createNode(GlTF gltf, Node parentNode, GaiaNode gaiaNode) {
        Node node;
        if (parentNode == null) {
            node = gltf.getNodes().get(0);
        } else {
            node = new Node();
            gltf.addNodes(node);
        }
        float[] matrix = gaiaNode.getTransformMatrix().get(new float[16]);
        if (!GeometryUtils.isIdentity(matrix)) {
            node.setMatrix(matrix);
        }
        node.setName(gaiaNode.getName());
        return node;
    }

    private void createMaterial(GlTF gltf, GaiaMaterial gaiaMaterial) {
        List<GaiaTexture> diffuseTextures = gaiaMaterial.getTextures().get(TextureType.DIFFUSE);

        Material material = new Material();
        material.setName(gaiaMaterial.getName());
        material.setDoubleSided(false);

        // Set the alpha mode.***
        boolean isOpaque = gaiaMaterial.isOpaqueMaterial();
        if (!isOpaque) {
            material.setAlphaMode("MASK"); // "OPAQUE", "MASK", "BLEND"
            material.setAlphaCutoff(0.0f);
        } else {
            material.setAlphaMode("OPAQUE"); // "OPAQUE", "MASK", "BLEND"
        }

        MaterialPbrMetallicRoughness pbrMetallicRoughness = new MaterialPbrMetallicRoughness();
        if (!diffuseTextures.isEmpty()) {
            GaiaTexture gaiaTexture = diffuseTextures.get(0);
            int textureId = createTexture(gltf, gaiaTexture);
            TextureInfo textureInfo = new TextureInfo();
            textureInfo.setIndex(textureId);
            pbrMetallicRoughness.setBaseColorTexture(textureInfo);
            pbrMetallicRoughness.setBaseColorFactor(new float[]{1.0f, 1.0f, 1.0f, 1.0f});
            pbrMetallicRoughness.setMetallicFactor(0.0f);
            pbrMetallicRoughness.setRoughnessFactor(0.5f);
        } else {
            pbrMetallicRoughness.setBaseColorFactor(new float[]{1.0f, 1.0f, 1.0f, 1.0f});
            pbrMetallicRoughness.setMetallicFactor(0.0f);
            pbrMetallicRoughness.setRoughnessFactor(0.5f);
        }

        material.setPbrMetallicRoughness(pbrMetallicRoughness);
        gltf.addMaterials(material);
    }

    private int createImage(GlTF gltf, GaiaTexture gaiaTexture) {
        String extension = FilenameUtils.getExtension(gaiaTexture.getPath());
        String mimeType = ImageUtils.getMimeTypeByExtension(extension);
        String uri = writeImage(gaiaTexture.getBufferedImage(), mimeType);
        Image image = new Image();
        image.setUri(uri);
        image.setMimeType(mimeType);
        gltf.addImages(image);
        return gltf.getImages().size() -1;
    }

    private int createTexture(GlTF gltf, GaiaTexture gaiaTexture) {
        gaiaTexture.getBufferedImage();
        int imageSource = createImage(gltf, gaiaTexture);

        Texture texture = new Texture();
        texture.setSampler(0);
        texture.setSource(imageSource);
        texture.setName(gaiaTexture.getName());

        gltf.addTextures(texture);
        return gltf.getTextures().size() - 1;
    }

    private int createAccessor(GlTF gltf, int bufferView, int byteOffset, int count, int componentType, AccessorType accessorType, boolean normalized) {
        Accessor accessor = new Accessor();
        accessor.setBufferView(bufferView);
        accessor.setByteOffset(byteOffset);
        accessor.setCount(count);
        accessor.setComponentType(componentType);
        accessor.setType(accessorType.name());
        accessor.setNormalized(normalized);
        gltf.addAccessors(accessor);
        return gltf.getAccessors().size() - 1;
    }

    private Sampler genSampler() {
        Sampler sampler = new Sampler();
        sampler.setMagFilter(GL20.GL_LINEAR);
        sampler.setMinFilter(GL20.GL_LINEAR_MIPMAP_LINEAR);
        sampler.setWrapS(GL20.GL_REPEAT);
        sampler.setWrapT(GL20.GL_REPEAT);
        return sampler;
    }

    private MeshPrimitive createPrimitive(GltfNodeBuffer nodeBuffer, GaiaPrimitive gaiaPrimitive, List<Material> materials) {
        MeshPrimitive primitive = new MeshPrimitive();
        primitive.setMode(GltfConstants.GL_TRIANGLES);
        if (materials != null && !materials.isEmpty()) {
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
        if (nodeBuffer.getTexcoordsAccessorId() > -1)
            primitive.getAttributes().put(AttributeType.TEXCOORD.getAccessor(), nodeBuffer.getTexcoordsAccessorId());
        if (nodeBuffer.getBatchIdAccessorId() > -1)
            primitive.getAttributes().put(AttributeType.BATCHID.getAccessor(), nodeBuffer.getBatchIdAccessorId());

        return primitive;
    }

    private int createMesh(GlTF gltf, MeshPrimitive primitive) {
        Mesh mesh = new Mesh();
        mesh.addWeights(1.0f);
        mesh.addPrimitives(primitive);
        gltf.addMeshes(mesh);
        return gltf.getMeshes().size() - 1;
    }


    private String writeImage(BufferedImage bufferedImage, String mimeType) {
        ImageResizer imageResizer = new ImageResizer();
        String formatName = ImageUtils.getFormatNameByMimeType(mimeType);
        String imageString = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            int powerOfTwoWidth = ImageUtils.getNearestPowerOfTwo(width);
            int powerOfTwoHeight = ImageUtils.getNearestPowerOfTwo(height);

            if (width != powerOfTwoWidth || height != powerOfTwoHeight) {
                bufferedImage = imageResizer.resizeImageGraphic2D(bufferedImage, powerOfTwoWidth, powerOfTwoHeight);
            }
            ImageIO.write(bufferedImage, formatName, baos);
            byte[] bytes = baos.toByteArray();
            imageString = "data:" + mimeType +";base64," + Base64.getEncoder().encodeToString(bytes);
            bufferedImage.flush();
            bytes = null;
        } catch (IOException e) {
            log.error(e.getMessage());
            log.error("Error writing image");
        }
        return imageString;
    }
}
