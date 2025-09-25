package com.gaia3d.converter.jgltf.tiles;

import com.gaia3d.basic.model.GaiaMesh;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaPrimitive;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.types.AccessorType;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.converter.jgltf.GltfBinary;
import com.gaia3d.converter.jgltf.GltfNodeBuffer;
import com.gaia3d.converter.jgltf.GltfWriter;
import com.gaia3d.converter.jgltf.Quantization;
import com.gaia3d.converter.jgltf.extension.ExtensionConstant;
import com.gaia3d.converter.jgltf.extension.ExtensionMeshFeatures;
import com.gaia3d.converter.jgltf.extension.ExtensionStructuralMetadata;
import com.gaia3d.process.postprocess.batch.GaiaBatchTableMap;
import com.gaia3d.process.postprocess.instance.GaiaFeatureTable;
import de.javagl.jgltf.impl.v2.*;
import de.javagl.jgltf.model.GltfConstants;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.GltfModels;
import de.javagl.jgltf.model.io.GltfModelWriter;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.lwjgl.opengl.GL20;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GltfWriter is a class that writes the glTF file.
 * It contains the method to write the glTF file from the GaiaScene object.
 * The glTF file is written in the glTF 2.0 format.
 * for 3D Tiles 1.1 Batched Model
 */
@Slf4j
@NoArgsConstructor
public class BatchedModelGltfWriter extends GltfWriter {

    /**
     * Write the glTF file from the GaiaScene object.
     * @param gaiaScene The GaiaScene object to be written.
     * @param outputPath The output path of the glTF file.
     */
    public void writeGlb(GaiaScene gaiaScene, File outputPath, GaiaFeatureTable featureTable, GaiaBatchTableMap<String, List<String>> batchTableMap) {
        try {
            GltfModel gltfModel = convert(gaiaScene, featureTable, batchTableMap);
            GltfModelWriter writer = new GltfModelWriter();
            writer.writeBinary(gltfModel, outputPath);
        } catch (IOException e) {
            log.error("[ERROR] :", e);
            log.error("[ERROR] Failed to write glb file.");
        }
    }

    /**
     * Write the glTF file from the GaiaScene object.
     * @param gaiaScene The GaiaScene object to be written.
     * @param outputStream The output stream of the glTF file.
     */
    public void writeGlb(GaiaScene gaiaScene, OutputStream outputStream, GaiaFeatureTable featureTable, GaiaBatchTableMap<String, List<String>> batchTableMap) {
        try {
            GltfModel gltfModel = convert(gaiaScene, featureTable, batchTableMap);
            GltfModelWriter writer = new GltfModelWriter();
            writer.writeBinary(gltfModel, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            log.error("[ERROR] :", e);
            log.error("[ERROR] Failed to write glb file.");
        }
    }

    protected GltfModel convert(GaiaScene gaiaScene, GaiaFeatureTable featureTable, GaiaBatchTableMap<String, List<String>> batchTableMap) {
        GltfBinary binary = new GltfBinary();
        GlTF gltf = new GlTF();
        gltf.setAsset(genAsset());
        gltf.addSamplers(genSampler());

        Node rootNode = initNode();
        initScene(gltf, rootNode);

        double[] rtcCenterOrigin = featureTable.getRtcCenter();
        double rctCenterX = rtcCenterOrigin[0];
        double rctCenterY = rtcCenterOrigin[1];
        double rctCenterZ = rtcCenterOrigin[2];

        float rtcCenterXBig = (float) Math.floor(rctCenterX);
        float rtcCenterYBig = (float) Math.floor(rctCenterY);
        float rtcCenterZBig = (float) Math.floor(rctCenterZ);
        float[] rtcCenterBigArray = new float[]{rtcCenterXBig, rtcCenterZBig, -rtcCenterYBig,};

        float rtcCenterXSmall = (float) (rctCenterX - rtcCenterXBig);
        float rtcCenterYSmall = (float) (rctCenterY - rtcCenterYBig);
        float rtcCenterZSmall = (float) (rctCenterZ - rtcCenterZBig);
        float[] rtcCenterSmallArray = new float[]{rtcCenterXSmall, rtcCenterZSmall, -rtcCenterYSmall,};

        rootNode.setTranslation(rtcCenterBigArray);

        if (globalOptions.isUseQuantization()) {
            gltf.addExtensionsUsed(ExtensionConstant.MESH_QUANTIZATION.getExtensionName());
            gltf.addExtensionsRequired(ExtensionConstant.MESH_QUANTIZATION.getExtensionName());
        }

        // Batch table
        gltf.addExtensionsUsed(ExtensionConstant.MESH_FEATURES.getExtensionName());
        gltf.addExtensionsUsed(ExtensionConstant.STRUCTURAL_METADATA.getExtensionName());

        ExtensionStructuralMetadata extensionStructuralMetadata = ExtensionStructuralMetadata.fromBatchTableMap(batchTableMap);
        Map<String, Object> extensions = new HashMap<>();
        extensions.put(ExtensionConstant.STRUCTURAL_METADATA.getExtensionName(), extensionStructuralMetadata);
        gltf.setExtensions(extensions);

        convertNode(gltf, binary, rootNode, gaiaScene.getNodes(), batchTableMap, rtcCenterSmallArray);
        gaiaScene.getMaterials().forEach(gaiaMaterial -> createMaterial(gltf, binary, gaiaMaterial));
        applyPropertiesBinary(gltf, binary, extensionStructuralMetadata);

        binary.fill();
        if (binary.getBody() != null) {
            GltfAssetV2 asset = new GltfAssetV2(gltf, binary.getBody());
            return GltfModels.create(asset);
        }
        return null;
    }

    private void applyPropertiesBinary(GlTF gltf, GltfBinary binary, ExtensionStructuralMetadata extensionStructuralMetadata) {
        int totalByteBufferLength = binary.calcTotalByteBufferLength() + binary.calcTotalImageByteBufferLength();
        AtomicInteger bufferOffset = new AtomicInteger(totalByteBufferLength);

        List<ByteBuffer> buffers = binary.getPropertyBuffers();

        extensionStructuralMetadata.getPropertyTables().forEach(propertyTable -> {
            propertyTable.getProperties().forEach((name, property) -> {
                List<String> values = property.getPrimaryValues();
                ByteBuffer[] stringBuffers = createStringBuffers(values);

                ByteBuffer stringBuffer = stringBuffers[0];
                ByteBuffer offsetBuffer = stringBuffers[1];

                int stringBufferViewId = createBufferView(gltf, 0, bufferOffset.get(), stringBuffer.capacity(), -1, GL20.GL_ARRAY_BUFFER);
                property.setValues(stringBufferViewId);

                BufferView stringBufferView = gltf.getBufferViews().get(stringBufferViewId);
                stringBufferView.setName(name + "_values");

                int offsetBufferViewId = createBufferView(gltf, 0, bufferOffset.get() + stringBuffer.capacity(), offsetBuffer.capacity(), -1, GL20.GL_ARRAY_BUFFER);
                property.setStringOffsets(offsetBufferViewId);

                BufferView offsetBufferView = gltf.getBufferViews().get(offsetBufferViewId);
                offsetBufferView.setName(name + "_offsets");

                bufferOffset.addAndGet(stringBuffer.capacity() + offsetBuffer.capacity());

                buffers.add(stringBuffer);
                buffers.add(offsetBuffer);
            });
        });
    }

    private ByteBuffer[] createStringBuffers(List<String> strings) {
        int totalStringLength = 0;
        byte[][] encodedFeatures = new byte[strings.size()][];
        for (int i = 0; i < strings.size(); i++) {
            String feature = strings.get(i);
            if (feature == null || feature.isEmpty()) {
                feature = " ";
            }
            encodedFeatures[i] = feature.getBytes(StandardCharsets.UTF_8);
            totalStringLength += encodedFeatures[i].length;
        }

        totalStringLength = padMultiple4(totalStringLength);
        int encodedFeaturesCount = (encodedFeatures.length + 1) * 4; // Each offset is an int (4 bytes)

        ByteBuffer stringBuffer = ByteBuffer.allocate(totalStringLength).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer offsetBuffer = ByteBuffer.allocate(encodedFeaturesCount).order(ByteOrder.LITTLE_ENDIAN);

        int currentOffset = 0;
        offsetBuffer.putInt(currentOffset); // offset[0] = 0
        for (byte[] encoded : encodedFeatures) {
            stringBuffer.put(encoded);
            currentOffset += encoded.length;
            offsetBuffer.putInt(currentOffset); // offset[i+1]
        }

        // position을 다시 0으로 되돌려서 읽기 준비
        stringBuffer.flip();
        offsetBuffer.flip();

        return new ByteBuffer[]{stringBuffer, offsetBuffer};
    }


    private void convertNode(GlTF gltf, GltfBinary binary, Node parentNode, List<GaiaNode> gaiaNodes, GaiaBatchTableMap<String, List<String>> batchTableMap, float[] translation) {
        List<GltfNodeBuffer> nodeBuffers = binary.getNodeBuffers();
        gaiaNodes.forEach((gaiaNode) -> {
            Node node = createNode(gltf, parentNode, gaiaNode, translation);

            List<Node> nodes = gltf.getNodes();
            int nodeId = nodes.size() - 1;
            if (parentNode != null) {
                parentNode.addChildren(nodeId);
            }

            List<GaiaNode> children = gaiaNode.getChildren();
            if (!children.isEmpty()) {
                convertNode(gltf, binary, node, children, batchTableMap, new float[3]);
            }

            List<GaiaMesh> gaiaMeshes = gaiaNode.getMeshes();
            gaiaMeshes.forEach((gaiaMesh) -> {
                GltfNodeBuffer nodeBuffer = convertGeometryInfo(gltf, gaiaMesh, node, batchTableMap);
                nodeBuffers.add(nodeBuffer);
            });
        });
    }

    protected Node createNode(GlTF gltf, Node parentNode, GaiaNode gaiaNode, float[] translation) {
        Node node;
        if (parentNode == null) {
            List<Node> nodes = gltf.getNodes();
            node = nodes.get(0);
        } else {
            node = new Node();
            gltf.addNodes(node);
        }

        Matrix4d rotationMatrix = gaiaNode.getTransformMatrix();

        Quaterniond rotationQuaternion = rotationMatrix.getNormalizedRotation(new Quaterniond());
        node.setRotation(new float[]{(float) rotationQuaternion.x, (float) rotationQuaternion.y, (float) rotationQuaternion.z, (float) rotationQuaternion.w});

        node.setTranslation(translation);

        node.setName(gaiaNode.getName());
        return node;
    }

    private GltfNodeBuffer convertGeometryInfo(GlTF gltf, GaiaMesh gaiaMesh, Node node, GaiaBatchTableMap<String, List<String>> batchTableMap) {
        int[] indices = gaiaMesh.getIndices();
        float[] positions = gaiaMesh.getFloatPositions();

        short[] unsignedShortsPositions = null;
        if (globalOptions.isUseQuantization()) {
            float[] otm = node.getMatrix();
            Matrix4d originalTransformMatrix;
            if (otm == null) {
                otm = new float[16];
                Matrix4d identityMatrix = new Matrix4d();
                identityMatrix.identity();
                originalTransformMatrix = identityMatrix;
            } else {
                originalTransformMatrix = new Matrix4d(otm[0], otm[1], otm[2], otm[3], otm[4], otm[5], otm[6], otm[7], otm[8], otm[9], otm[10], otm[11], otm[12], otm[13], otm[14], otm[15]);
            }

            Matrix4d quantizationMatrix = Quantization.computeQuantizationMatrix(originalTransformMatrix, positions);
            unsignedShortsPositions = Quantization.quantizeUnsignedShorts(positions, originalTransformMatrix, quantizationMatrix);
            node.setMatrix(quantizationMatrix.get(new float[16]));
        }

        float[] normals = gaiaMesh.getNormals();
        byte[] colors = gaiaMesh.getColors();
        float[] texcoords = gaiaMesh.getTexcoords();
        float[] batchIds = gaiaMesh.getBatchIds();

        int vertexCount = gaiaMesh.getPositionsCount() / 3;
        boolean isOverShortVertices = vertexCount >= 65535;
        if (isOverShortVertices) {
            log.debug("[WARN] The number of vertices count than 65535 ({})", vertexCount);
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
            for (int indicesValue : indices) {
                if (isOverShortVertices) {
                    indicesBuffer.putInt(indicesValue);
                } else {
                    short indicesValueShort = (short) indicesValue;
                    indicesBuffer.putShort(indicesValueShort);
                }
            }
        }
        if (positionsBuffer != null) {
            if (globalOptions.isUseQuantization() && unsignedShortsPositions != null) {
                for (short position : unsignedShortsPositions) {
                    positionsBuffer.putShort(position);
                }
            } else {
                for (Float position : positions) {
                    positionsBuffer.putFloat(position);
                }
            }
        }
        if (normalsBuffer != null) {
            for (Float normal : normals) {
                normalsBuffer.putFloat(normal);
            }
        }
        if (colorsBuffer != null) {
            for (Byte color : colors) {
                colorsBuffer.put(color);
            }
        }
        if (texcoordsBuffer != null) {
            for (Float textureCoordinate : texcoords) {
                texcoordsBuffer.putFloat(textureCoordinate);
            }
        }
        if (batchIdBuffer != null) {
            for (Float batchId : batchIds) {
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
            if (globalOptions.isUseQuantization()) {
                int verticesAccessorId = createAccessor(gltf, positionsBufferViewId, 0, positions.length / 3, GltfConstants.GL_UNSIGNED_SHORT, AccessorType.VEC3, true);
                nodeBuffer.setPositionsAccessorId(verticesAccessorId);
            } else {
                int verticesAccessorId = createAccessor(gltf, positionsBufferViewId, 0, positions.length / 3, GltfConstants.GL_FLOAT, AccessorType.VEC3, false);
                nodeBuffer.setPositionsAccessorId(verticesAccessorId);
            }
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
        MeshPrimitive primitive = createPrimitive(nodeBuffer, gaiaPrimitive, materials, batchTableMap);
        int meshId = createMesh(gltf, primitive);
        node.setMesh(meshId);
        return nodeBuffer;
    }

    private MeshPrimitive createPrimitive(GltfNodeBuffer nodeBuffer, GaiaPrimitive gaiaPrimitive, List<Material> materials, GaiaBatchTableMap<String, List<String>> batchTableMap) {
        MeshPrimitive primitive = new MeshPrimitive();
        primitive.setMode(GltfConstants.GL_TRIANGLES);
        primitive.setMaterial(gaiaPrimitive.getMaterialIndex());
        primitive.setAttributes(new HashMap<>());
        primitive.setIndices(nodeBuffer.getIndicesAccessorId());

        ExtensionMeshFeatures extensionMeshFeatures = ExtensionMeshFeatures.fromBatchTable(batchTableMap);
        Map<String, Object> extensions = new HashMap<>();
        extensions.put(ExtensionConstant.MESH_FEATURES.getExtensionName(), extensionMeshFeatures);
        primitive.setExtensions(extensions);

        Map<String, Integer> attributes = primitive.getAttributes();
        if (nodeBuffer.getPositionsAccessorId() > -1) {
            attributes.put(AttributeType.POSITION.getAccessor(), nodeBuffer.getPositionsAccessorId());
        }
        if (nodeBuffer.getNormalsAccessorId() > -1) {
            attributes.put(AttributeType.NORMAL.getAccessor(), nodeBuffer.getNormalsAccessorId());
        }
        if (nodeBuffer.getColorsAccessorId() > -1) {
            attributes.put(AttributeType.COLOR.getAccessor(), nodeBuffer.getColorsAccessorId());
        }
        if (nodeBuffer.getTexcoordsAccessorId() > -1) {
            attributes.put(AttributeType.TEXCOORD.getAccessor(), nodeBuffer.getTexcoordsAccessorId());
        }
        if (nodeBuffer.getBatchIdAccessorId() > -1) {
            attributes.put(AttributeType.FEATURE_ID_0.getAccessor(), nodeBuffer.getBatchIdAccessorId());
        }

        return primitive;
    }
}
