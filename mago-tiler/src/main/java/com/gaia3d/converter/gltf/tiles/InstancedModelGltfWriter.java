package com.gaia3d.converter.gltf.tiles;

import com.gaia3d.basic.model.GaiaMesh;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaPrimitive;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.types.AccessorType;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.gltf.*;
import com.gaia3d.converter.gltf.extension.ExtensionInstanceFeatures;
import com.gaia3d.converter.gltf.extension.ExtensionMeshGpuInstancing;
import com.gaia3d.converter.gltf.extension.ExtensionStructuralMetadata;
import com.gaia3d.process.postprocess.batch.GaiaBatchTableMap;
import com.gaia3d.process.postprocess.instance.GaiaFeatureTable;
import com.gaia3d.process.postprocess.instance.Instanced3DModelBinary;
import de.javagl.jgltf.impl.v2.BufferView;
import de.javagl.jgltf.impl.v2.GlTF;
import de.javagl.jgltf.impl.v2.MeshPrimitive;
import de.javagl.jgltf.impl.v2.Node;
import de.javagl.jgltf.model.GltfConstants;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.GltfModels;
import de.javagl.jgltf.model.io.GltfModelWriter;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import lombok.AllArgsConstructor;
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
 * for 3D Tiles 1.1 Instance Model.
 */
@Slf4j
public class InstancedModelGltfWriter extends GltfWriter {

    public InstancedModelGltfWriter() {
        super();
    }

    public InstancedModelGltfWriter(GltfWriterOptions gltfOptions) {
        super(gltfOptions);
    }

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

        Double[] rtcCenterOrigin = featureTable.getRtcCenter();
        rootNode.setTranslation(new float[]{(float) rtcCenterOrigin[0].doubleValue(), (float) rtcCenterOrigin[1].doubleValue(), (float) rtcCenterOrigin[2].doubleValue()});

        if (gltfOptions.isUseQuantization()) {
            gltf.addExtensionsUsed(ExtensionConstant.MESH_QUANTIZATION.getExtensionName());
            gltf.addExtensionsRequired(ExtensionConstant.MESH_QUANTIZATION.getExtensionName());
        }

        // Batch table (3D Tiles 1.1)
        gltf.addExtensionsUsed(ExtensionConstant.INSTANCE_FEATURES.getExtensionName());
        gltf.addExtensionsUsed(ExtensionConstant.STRUCTURAL_METADATA.getExtensionName());

        // Instance table (GPU Instancing)(Required for 3D Tiles 1.1)
        gltf.addExtensionsUsed(ExtensionConstant.MESH_GPU_INSTANCING.getExtensionName());
        //gltf.addExtensionsRequired(ExtensionConstant.MESH_GPU_INSTANCING.getExtensionName());

        ExtensionStructuralMetadata extensionStructuralMetadata = ExtensionStructuralMetadata.fromBatchTableMap(batchTableMap);
        Map<String, Object> extensions = new HashMap<>();
        extensions.put(ExtensionConstant.STRUCTURAL_METADATA.getExtensionName(), extensionStructuralMetadata);
        gltf.setExtensions(extensions);

        convertNode(gltf, binary, rootNode, gaiaScene.getNodes(), featureTable, batchTableMap);
        gaiaScene.getMaterials().forEach(gaiaMaterial -> createMaterial(gltf, binary, gaiaMaterial));
        applyPropertiesBinary(gltf, binary, extensionStructuralMetadata);
        applyInstanceFeaturesBinary(gltf, binary, featureTable);

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

    private void applyInstanceFeaturesBinary(GlTF gltf, GltfBinary binary, GaiaFeatureTable featureTable) {
        int totalByteBufferLength = binary.calcTotalByteBufferLength() + binary.calcTotalImageByteBufferLength() + binary.calcTotalPropertyByteBufferLength();
        List<ByteBuffer> instancingBuffers = binary.getInstancingBuffers();

        int featureCount = featureTable.getInstancesLength();

        Instanced3DModelBinary instancedBuffer = featureTable.getInstancedBuffer();
        byte[] translationBuffer = instancedBuffer.getPositionBytes();
        byte[] rotationBuffer = instancedBuffer.getRotationBytes();
        byte[] scaleBuffer = instancedBuffer.getScaleBytes();
        byte[] featureIdBuffer = instancedBuffer.getFeatureIdBytes();

        ByteBuffer translationByteBuffer = ByteBuffer.allocate(translationBuffer.length).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer rotationByteBuffer = ByteBuffer.allocate(rotationBuffer.length).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer scaleByteBuffer = ByteBuffer.allocate(scaleBuffer.length).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer featureIdByteBuffer = ByteBuffer.allocate(featureIdBuffer.length).order(ByteOrder.LITTLE_ENDIAN);

        translationByteBuffer.put(translationBuffer);
        rotationByteBuffer.put(rotationBuffer);
        scaleByteBuffer.put(scaleBuffer);
        featureIdByteBuffer.put(featureIdBuffer);

        translationByteBuffer.flip();
        rotationByteBuffer.flip();
        scaleByteBuffer.flip();
        featureIdByteBuffer.flip();

        instancingBuffers.add(translationByteBuffer);
        instancingBuffers.add(rotationByteBuffer);
        instancingBuffers.add(scaleByteBuffer);
        instancingBuffers.add(featureIdByteBuffer);

        int translationByteBufferLength = translationByteBuffer.capacity();
        int rotationByteBufferLength = rotationByteBuffer.capacity();
        int scaleByteBufferLength = scaleByteBuffer.capacity();
        int featureIdByteBufferLength = featureIdByteBuffer.capacity();

        int translationBufferViewId = createBufferView(gltf, 0, totalByteBufferLength, translationByteBufferLength, 12, GL20.GL_ARRAY_BUFFER);
        int rotationBufferViewId = createBufferView(gltf, 0, totalByteBufferLength + translationByteBufferLength, rotationByteBufferLength, 16, GL20.GL_ARRAY_BUFFER);
        int scaleBufferViewId = createBufferView(gltf, 0, totalByteBufferLength + translationByteBufferLength + rotationByteBufferLength, scaleByteBufferLength, 12, GL20.GL_ARRAY_BUFFER);
        int featureIdBufferViewId = createBufferView(gltf, 0, totalByteBufferLength + translationByteBufferLength + rotationByteBufferLength + scaleByteBufferLength, featureIdByteBufferLength, -1, GL20.GL_ARRAY_BUFFER);

        BufferView translationBufferView = gltf.getBufferViews().get(translationBufferViewId);
        translationBufferView.setName("translation");
        BufferView rotationBufferView = gltf.getBufferViews().get(rotationBufferViewId);
        rotationBufferView.setName("rotation");
        BufferView scaleBufferView = gltf.getBufferViews().get(scaleBufferViewId);
        scaleBufferView.setName("scale");
        BufferView featureIdBufferView = gltf.getBufferViews().get(featureIdBufferViewId);
        featureIdBufferView.setName("featureId");

        int translationAccessorId = createAccessor(gltf, translationBufferViewId, 0, featureCount, GltfConstants.GL_FLOAT, AccessorType.VEC3, false);
        int rotationAccessorId = createAccessor(gltf, rotationBufferViewId, 0, featureCount, GltfConstants.GL_FLOAT, AccessorType.VEC4, false);
        int scaleAccessorId = createAccessor(gltf, scaleBufferViewId, 0, featureCount, GltfConstants.GL_FLOAT, AccessorType.VEC3, false);
        int featureIdAccessorId = createAccessor(gltf, featureIdBufferViewId, 0, featureCount, GltfConstants.GL_FLOAT, AccessorType.SCALAR, false);

        List<Node> nodes = gltf.getNodes();
        nodes.forEach(node -> {
            if (node.getMesh() != null && node.getMesh() >= 0) {
                Map<String, Object> extensions = new HashMap<>();
                ExtensionInstanceFeatures extensionInstanceFeatures = ExtensionInstanceFeatures.fromBatchTable(featureTable);
                extensions.put(ExtensionConstant.INSTANCE_FEATURES.getExtensionName(), extensionInstanceFeatures);

                ExtensionMeshGpuInstancing extensionMeshGpuInstancing = new ExtensionMeshGpuInstancing();
                Map<String, Integer> attributes = extensionMeshGpuInstancing.getAttributes();
                attributes.put("TRANSLATION", translationAccessorId);
                attributes.put("ROTATION", rotationAccessorId);
                attributes.put("SCALE", scaleAccessorId);
                attributes.put(AttributeType.FEATURE_ID_0.getAccessor(), featureIdAccessorId);
                extensions.put(ExtensionConstant.MESH_GPU_INSTANCING.getExtensionName(), extensionMeshGpuInstancing);

                node.setExtensions(extensions);
            }
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

        stringBuffer.flip();
        offsetBuffer.flip();
        return new ByteBuffer[]{stringBuffer, offsetBuffer};
    }


    protected void convertNode(GlTF gltf, GltfBinary binary, Node parentNode, List<GaiaNode> gaiaNodes, GaiaFeatureTable featureTable, GaiaBatchTableMap<String, List<String>> batchTableMap) {
        List<GltfNodeBuffer> nodeBuffers = binary.getNodeBuffers();
        gaiaNodes.forEach((gaiaNode) -> {
            Node node = createNode(gltf, parentNode, gaiaNode, binary, featureTable);

            int nodeId = gltf.getNodes().size() - 1;
            if (parentNode != null) {
                parentNode.addChildren(nodeId);
            }

            List<GaiaNode> children = gaiaNode.getChildren();
            if (!children.isEmpty()) {
                convertNode(gltf, binary, node, children, featureTable, batchTableMap);
            }

            List<GaiaMesh> gaiaMeshes = gaiaNode.getMeshes();
            gaiaMeshes.forEach((gaiaMesh) -> {
                GltfNodeBuffer nodeBuffer = convertGeometryInfo(gltf, gaiaMesh, node, batchTableMap);
                nodeBuffers.add(nodeBuffer);
            });
        });
    }

    protected Node createNode(GlTF gltf, Node parentNode, GaiaNode gaiaNode, GltfBinary binary, GaiaFeatureTable featureTable) {
        Node node;
        if (parentNode == null) {
            node = gltf.getNodes().get(0);
        } else {
            node = new Node();
            gltf.addNodes(node);
        }

        Matrix4d rotationMatrix = gaiaNode.getTransformMatrix();
        Quaterniond rotationQuaternion = rotationMatrix.getNormalizedRotation(new Quaterniond());
        node.setRotation(new float[]{(float) rotationQuaternion.x, (float) rotationQuaternion.y, (float) rotationQuaternion.z, (float) rotationQuaternion.w});

        node.setName(gaiaNode.getName());
        return node;
    }

    protected GltfNodeBuffer convertGeometryInfo(GlTF gltf, GaiaMesh gaiaMesh, Node node, GaiaBatchTableMap<String, List<String>> batchTableMap) {
        int[] indices = gaiaMesh.getIndices();
        float[] positions = gaiaMesh.getFloatPositions();

        short[] unsignedShortsPositions = null;
        if (gltfOptions.isUseQuantization()) {
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

            if (node.getRotation() != null && node.getTranslation() != null) {
                log.warn("[WARN] When using quantization, rotation and translation are ignored.");
                node.setRotation(null);
                node.setTranslation(null);
            }
        }

        float[] normals = gaiaMesh.getNormals();
        byte[] colors = gaiaMesh.getColors();
        float[] texcoords = gaiaMesh.getTexcoords();
        float[] batchIds = gaiaMesh.getBatchIds();

        int vertexCount = gaiaMesh.getPositionsCount() / 3;
        boolean isOverShortVertices = vertexCount >= 65535;
        if (isOverShortVertices) {
            log.warn("[WARN] The number of vertices count than 65535 ({})", vertexCount);
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
            if (gltfOptions.isUseQuantization() && unsignedShortsPositions != null) {
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
            if (gltfOptions.isUseQuantization()) {
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

        GaiaPrimitive gaiaPrimitive = gaiaMesh.getPrimitives().get(0);
        MeshPrimitive primitive = createPrimitive(nodeBuffer, gaiaPrimitive);
        int meshId = createMesh(gltf, primitive);
        node.setMesh(meshId);
        return nodeBuffer;
    }

    protected MeshPrimitive createPrimitive(GltfNodeBuffer nodeBuffer, GaiaPrimitive gaiaPrimitive) {
        MeshPrimitive primitive = new MeshPrimitive();
        primitive.setMode(GltfConstants.GL_TRIANGLES);
        primitive.setMaterial(gaiaPrimitive.getMaterialIndex());
        primitive.setAttributes(new HashMap<>());
        primitive.setIndices(nodeBuffer.getIndicesAccessorId());

        if (nodeBuffer.getPositionsAccessorId() > -1) {primitive.getAttributes().put(AttributeType.POSITION.getAccessor(), nodeBuffer.getPositionsAccessorId());}
        if (nodeBuffer.getNormalsAccessorId() > -1) {primitive.getAttributes().put(AttributeType.NORMAL.getAccessor(), nodeBuffer.getNormalsAccessorId());}
        if (nodeBuffer.getColorsAccessorId() > -1) {primitive.getAttributes().put(AttributeType.COLOR.getAccessor(), nodeBuffer.getColorsAccessorId());}
        if (nodeBuffer.getTexcoordsAccessorId() > -1) {primitive.getAttributes().put(AttributeType.TEXCOORD.getAccessor(), nodeBuffer.getTexcoordsAccessorId());}
        if (nodeBuffer.getBatchIdAccessorId() > -1) {primitive.getAttributes().put(AttributeType.BATCHID.getAccessor(), nodeBuffer.getBatchIdAccessorId());}

        return primitive;
    }
}
