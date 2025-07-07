package com.gaia3d.converter.jgltf;

import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaMesh;
import com.gaia3d.basic.model.GaiaPrimitive;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.basic.types.AccessorType;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.converter.jgltf.extension.*;
import com.gaia3d.process.postprocess.batch.GaiaBatchTable;
import com.gaia3d.process.postprocess.batch.GaiaBatchTableMap;
import com.gaia3d.process.postprocess.instance.GaiaFeatureTable;
import com.gaia3d.process.postprocess.instance.Instanced3DModelBinary;
import com.gaia3d.process.postprocess.pointcloud.PointCloudBuffer;
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PointCloudGltfWriter is responsible for writing point cloud data to a GLB file format.
 * It converts point cloud buffers, feature tables, and batch tables into a GltfModel,
 * then writes that model to a binary GLB file.
 * This class extends GltfWriter and provides specific implementations for point cloud data.
 */
@Slf4j
@NoArgsConstructor
public class PointCloudGltfWriter extends GltfWriter {

    public void writeGlb(PointCloudBuffer pointCloudBuffer, GaiaFeatureTable featureTable, GaiaBatchTable batchTable, File outputPath) {
        try {
            GltfModel gltfModel = convert(pointCloudBuffer, featureTable, batchTable, outputPath);
            GltfModelWriter writer = new GltfModelWriter();
            writer.writeBinary(gltfModel, outputPath);
        } catch (IOException e) {
            log.error("[ERROR] :", e);
            log.error("[ERROR] Failed to write glb file.");
        }
    }

    protected GltfModel convert(PointCloudBuffer pointCloudBuffer, GaiaFeatureTable featureTable, GaiaBatchTable batchTable, File outputPath) {
        GltfBinary binary = new GltfBinary();
        GlTF gltf = new GlTF();
        gltf.setAsset(genAsset());
        gltf.addSamplers(genSampler());

        Node rootNode = initNode();
        initScene(gltf, rootNode);

        double[] rtcCenterOrigin = featureTable.getRtcCenter();
        rootNode.setTranslation(new float[]{(float) rtcCenterOrigin[0], (float) rtcCenterOrigin[2], (float) -rtcCenterOrigin[1]});

        gltf.addExtensionsUsed(ExtensionConstant.MESH_QUANTIZATION.getExtensionName());
        gltf.addExtensionsRequired(ExtensionConstant.MESH_QUANTIZATION.getExtensionName());

        convertNode(gltf, binary, rootNode, pointCloudBuffer, featureTable, batchTable);

        binary.fill();
        if (binary.getBody() != null) {
            GltfAssetV2 asset = new GltfAssetV2(gltf, binary.getBody());
            return GltfModels.create(asset);
        }
        return null;
    }

    private void applyPropertiesBinary(GlTF gltf, GltfBinary binary, ExtensionStructuralMetadata extensionStructuralMetadata) {
        log.info("[Info] Apply properties binary to glTF");

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
                log.info("[Info] Property: {}, Values: {}", name, values.size());
            });
        });
    }

    private void applyInstanceFeaturesBinary(GlTF gltf, GltfBinary binary, GaiaFeatureTable featureTable) {
        log.info("[Info] Apply instance features binary to glTF");

        int totalByteBufferLength = binary.calcTotalByteBufferLength() + binary.calcTotalImageByteBufferLength() + binary.calcTotalPropertyByteBufferLength();
        //AtomicInteger bufferOffset = new AtomicInteger(totalByteBufferLength);

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
        int rotationAccessorId = createAccessor(gltf, rotationBufferViewId, 0, featureCount, GltfConstants.GL_FLOAT, AccessorType.VEC4, true);
        int scaleAccessorId = createAccessor(gltf, scaleBufferViewId, 0, featureCount, GltfConstants.GL_FLOAT, AccessorType.VEC3, false);
        int featureIdAccessorId = createAccessor(gltf, featureIdBufferViewId, 0, featureCount, GltfConstants.GL_FLOAT, AccessorType.SCALAR, false);

        List<Node> nodes = gltf.getNodes();
        nodes.forEach(node -> {
            if (node.getMesh() != null && node.getMesh() >= 0) {
                log.info("[Info] Apply instance features to node: {}", node.getName());

                //featureTable.getInstancedBuffer();

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


        /*ExtensionInstanceFeatures extensionInstanceFeatures = ExtensionInstanceFeatures.fromBatchTable(featureTable);
        extensionInstanceFeatures.getFeatureIds().forEach(featureId -> {
            ByteBuffer[] stringBuffers = createStringBuffers(featureId.getValues());

            ByteBuffer stringBuffer = stringBuffers[0];
            ByteBuffer offsetBuffer = stringBuffers[1];

            int stringBufferViewId = createBufferView(gltf, 0, bufferOffset.get(), stringBuffer.capacity(), -1, GL20.GL_ARRAY_BUFFER);
            featureId.setValues(stringBufferViewId);

            int offsetBufferViewId = createBufferView(gltf, 0, bufferOffset.get() + stringBuffer.capacity(), offsetBuffer.capacity(), -1, GL20.GL_ARRAY_BUFFER);
            featureId.setStringOffsets(offsetBufferViewId);

            bufferOffset.addAndGet(stringBuffer.capacity() + offsetBuffer.capacity());

            buffers.add(stringBuffer);
            buffers.add(offsetBuffer);
            log.info("[Info] Feature ID: {}, Values: {}", featureId.getName(), featureId.getValues().size());
        });*/
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

        ByteBuffer stringBuffer = ByteBuffer.allocate(totalStringLength)
                .order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer offsetBuffer = ByteBuffer.allocate(encodedFeaturesCount)
                .order(ByteOrder.LITTLE_ENDIAN);

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


    protected void convertNode(GlTF gltf, GltfBinary binary, Node parentNode, PointCloudBuffer pointCloudBuffer, GaiaFeatureTable featureTable, GaiaBatchTable batchTable) {
        List<GltfNodeBuffer> nodeBuffers = binary.getNodeBuffers();

        Node node = createNode(gltf, parentNode, binary, pointCloudBuffer, featureTable);
        GltfNodeBuffer nodeBuffer = convertGeometryInfo(gltf, pointCloudBuffer, node, featureTable, batchTable);

        int nodeId = gltf.getNodes().size() - 1;
        if (parentNode != null) {
            parentNode.addChildren(nodeId);
        }

        nodeBuffers.add(nodeBuffer);
    }

    protected Node createNode(GlTF gltf, Node parentNode, GltfBinary binary, PointCloudBuffer pointCloudBuffer, GaiaFeatureTable featureTable) {
        Node node;
        if (parentNode == null) {
            node = gltf.getNodes().get(0);
        } else {
            node = new Node();
            gltf.addNodes(node);
        }

        /*Matrix4d rotationMatrix = gaiaNode.getTransformMatrix();
        Quaterniond rotationQuaternion = rotationMatrix.getNormalizedRotation(new Quaterniond());
        node.setRotation(new float[]{
                (float) rotationQuaternion.x,
                (float) rotationQuaternion.y,
                (float) rotationQuaternion.z,
                (float) rotationQuaternion.w
        });*/

        node.setName("PointCloudNode");
        return node;
    }

    protected GltfNodeBuffer convertGeometryInfo(GlTF gltf, PointCloudBuffer pointCloudBuffer, Node node, GaiaFeatureTable featureTable, GaiaBatchTable batchTable) {

        //int[] indices = gaiaMesh.getIndices();
        float[] positions = pointCloudBuffer.getPositions();

        short[] unsignedShortsPositions = null;
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

        short[] normals = pointCloudBuffer.getNormals();
        byte[] colors = pointCloudBuffer.getColorBytes(); // 4bytes
        //float[] texcoords = gaiaMesh.getTexcoords();
        float[] batchIds = pointCloudBuffer.getBatchIds();

        //int vertexCount = featureTable.getPointsLength();

        GltfNodeBuffer nodeBuffer = initNodeBuffer(pointCloudBuffer);
        createBuffer(gltf, nodeBuffer);

        //ByteBuffer indicesBuffer = nodeBuffer.getIndicesBuffer();
        ByteBuffer positionsBuffer = nodeBuffer.getPositionsBuffer();
        //ByteBuffer normalsBuffer = nodeBuffer.getNormalsBuffer();
        ByteBuffer colorsBuffer = nodeBuffer.getColorsBuffer();
        //ByteBuffer texcoordsBuffer = nodeBuffer.getTexcoordsBuffer();
        ByteBuffer batchIdBuffer = nodeBuffer.getBatchIdBuffer();

        //int indicesBufferViewId = nodeBuffer.getIndicesBufferViewId();
        int positionsBufferViewId = nodeBuffer.getPositionsBufferViewId();
        //int normalsBufferViewId = nodeBuffer.getNormalsBufferViewId();
        int colorsBufferViewId = nodeBuffer.getColorsBufferViewId();
        //int texcoordsBufferViewId = nodeBuffer.getTexcoordsBufferViewId();
        int batchIdBufferViewId = nodeBuffer.getBatchIdBufferViewId();

        /*if (indicesBuffer != null) {
            for (int indicesValue : indices) {
                if (isOverShortVertices) {
                    indicesBuffer.putInt(indicesValue);
                } else {
                    short indicesValueShort = (short) indicesValue;
                    indicesBuffer.putShort(indicesValueShort);
                }
            }
        }*/
        if (positionsBuffer != null) {
            for (short position : unsignedShortsPositions) {
                positionsBuffer.putShort(position);
            }
        }
        /*if (normalsBuffer != null) {
            for (Short normal : normals) {
                normalsBuffer.putShort(normal);
            }
        }*/
        if (colorsBuffer != null) {
            for (Byte color : colors) {
                colorsBuffer.put(color);
            }
        }
        /*if (texcoordsBuffer != null) {
            for (Float textureCoordinate : texcoords) {
                texcoordsBuffer.putFloat(textureCoordinate);
            }
        }*/
        if (batchIdBuffer != null) {
            for (Float batchId : batchIds) {
                batchIdBuffer.putFloat(batchId);
            }
        }

        if (positionsBufferViewId > -1 && positions.length > 0) {
            int verticesAccessorId = createAccessor(gltf, positionsBufferViewId, 0, positions.length / 3, GltfConstants.GL_UNSIGNED_SHORT, AccessorType.VEC3, true);
            nodeBuffer.setPositionsAccessorId(verticesAccessorId);
        }
        /*if (normalsBufferViewId > -1 && normals.length > 0) {
            int normalsAccessorId = createAccessor(gltf, normalsBufferViewId, 0, normals.length / 3, GltfConstants.GL_FLOAT, AccessorType.VEC3, false);
            nodeBuffer.setNormalsAccessorId(normalsAccessorId);
        }*/
        if (colorsBufferViewId > -1 && colors.length > 0) {
            int colorsAccessorId = createAccessor(gltf, colorsBufferViewId, 0, colors.length / 4, GltfConstants.GL_UNSIGNED_BYTE, AccessorType.VEC4, true);
            nodeBuffer.setColorsAccessorId(colorsAccessorId);
        }
        if (batchIdBufferViewId > -1 && batchIds.length > 0) {
            int batchIdAccessorId = createAccessor(gltf, batchIdBufferViewId, 0, batchIds.length, GltfConstants.GL_FLOAT, AccessorType.SCALAR, false);
            nodeBuffer.setBatchIdAccessorId(batchIdAccessorId);
        }

        MeshPrimitive primitive = createPrimitive(gltf, nodeBuffer);
        int meshId = createMesh(gltf, primitive);
        node.setMesh(meshId);
        return nodeBuffer;
    }

    protected Buffer initBuffer(GlTF gltf) {
        if (gltf.getBuffers() == null) {
            Buffer buffer = new Buffer();
            gltf.addBuffers(buffer);
        }
        return gltf.getBuffers()
                .get(0);
    }

    protected void createBuffer(GlTF gltf, GltfNodeBuffer nodeBuffer) {
        Buffer buffer = initBuffer(gltf);
        int bufferLength = buffer.getByteLength() == null ? 0 : buffer.getByteLength();
        int bufferId = 0;
        int bufferOffset = 0;
        /*if (nodeBuffer.getIndicesBuffer() != null) {
            ByteBuffer indicesBuffer = nodeBuffer.getIndicesBuffer();
            int bufferViewId = createBufferView(gltf, bufferId, bufferLength + bufferOffset, indicesBuffer.capacity(), -1, GL20.GL_ELEMENT_ARRAY_BUFFER);
            nodeBuffer.setIndicesBufferViewId(bufferViewId);
            BufferView bufferView = gltf.getBufferViews()
                    .get(bufferViewId);
            bufferView.setName("indices");
            bufferOffset += indicesBuffer.capacity();
        }*/
        if (nodeBuffer.getPositionsBuffer() != null) {
            ByteBuffer positionsBuffer = nodeBuffer.getPositionsBuffer();
            int bufferViewId = createBufferView(gltf, bufferId, bufferLength + bufferOffset, positionsBuffer.capacity(), 8, GL20.GL_ARRAY_BUFFER);
            nodeBuffer.setPositionsBufferViewId(bufferViewId);
            BufferView bufferView = gltf.getBufferViews()
                    .get(bufferViewId);
            bufferView.setName("positions");
            bufferOffset += positionsBuffer.capacity();
        }
        /*if (nodeBuffer.getNormalsBuffer() != null) {
            ByteBuffer normalsBuffer = nodeBuffer.getNormalsBuffer();
            int bufferViewId = createBufferView(gltf, bufferId, bufferLength + bufferOffset, normalsBuffer.capacity(), 12, GL20.GL_ARRAY_BUFFER);
            nodeBuffer.setNormalsBufferViewId(bufferViewId);
            BufferView bufferView = gltf.getBufferViews()
                    .get(bufferViewId);
            bufferView.setName("normals");
            bufferOffset += normalsBuffer.capacity();
        }*/
        if (nodeBuffer.getColorsBuffer() != null) {
            ByteBuffer colorsBuffer = nodeBuffer.getColorsBuffer();
            int bufferViewId = createBufferView(gltf, bufferId, bufferLength + bufferOffset, colorsBuffer.capacity(), 4, GL20.GL_ARRAY_BUFFER);
            nodeBuffer.setColorsBufferViewId(bufferViewId);
            BufferView bufferView = gltf.getBufferViews()
                    .get(bufferViewId);
            bufferView.setName("colors");
            bufferOffset += colorsBuffer.capacity();
        }
        /*if (nodeBuffer.getTexcoordsBuffer() != null) {
            ByteBuffer texcoordsBuffer = nodeBuffer.getTexcoordsBuffer();
            int bufferViewId = createBufferView(gltf, bufferId, bufferLength + bufferOffset, texcoordsBuffer.capacity(), 8, GL20.GL_ARRAY_BUFFER);
            nodeBuffer.setTexcoordsBufferViewId(bufferViewId);
            BufferView bufferView = gltf.getBufferViews()
                    .get(bufferViewId);
            bufferView.setName("texcoords");
            bufferOffset += texcoordsBuffer.capacity();
        }*/
        if (nodeBuffer.getBatchIdBuffer() != null) {
            ByteBuffer batchIdBuffer = nodeBuffer.getBatchIdBuffer();
            int bufferViewId = createBufferView(gltf, bufferId, bufferLength + bufferOffset, batchIdBuffer.capacity(), 4, GL20.GL_ARRAY_BUFFER);
            nodeBuffer.setBatchIdBufferViewId(bufferViewId);
            BufferView bufferView = gltf.getBufferViews()
                    .get(bufferViewId);
            bufferView.setName("batchIds");
            bufferOffset += batchIdBuffer.capacity();
        }
        buffer.setByteLength(bufferLength + bufferOffset);
    }

    protected GltfNodeBuffer initNodeBuffer(PointCloudBuffer pointCloudBuffer) {
        GltfNodeBuffer nodeBuffer = new GltfNodeBuffer();
        int SHORT_SIZE = 2;
        int INT_SIZE = 4;
        int FLOAT_SIZE = 4;

        int vertexCount = pointCloudBuffer.getPositions().length / 3;

        int paddedPositionsCount = pointCloudBuffer.getPositions().length / 3 * 4;
        int positionsCapacity = paddedPositionsCount * SHORT_SIZE;
        //int normalsCapacity = gaiaMesh.getPositionsCount() * FLOAT_SIZE;
        int colorsCapacity = pointCloudBuffer.getColors().length;
        //int batchIdCapacity = pointCloudBuffer.getBatchIds().length * FLOAT_SIZE;

        //indicesCapacity = padMultiple4(indicesCapacity);
        positionsCapacity = padMultiple4(positionsCapacity);
        //normalsCapacity = padMultiple4(normalsCapacity);
        colorsCapacity = padMultiple4(colorsCapacity);
        //texcoordCapacity = padMultiple4(texcoordCapacity);
        //batchIdCapacity = padMultiple4(batchIdCapacity);

        int bodyLength = 0;
        //bodyLength += indicesCapacity;
        bodyLength += positionsCapacity;
        //bodyLength += normalsCapacity;
        bodyLength += colorsCapacity;
        //bodyLength += texcoordCapacity;
        //bodyLength += batchIdCapacity;

        nodeBuffer.setTotalByteBufferLength(bodyLength);
        /*if (indicesCapacity > 0) {
            ByteBuffer indicesBuffer = ByteBuffer.allocate(indicesCapacity);
            indicesBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nodeBuffer.setIndicesBuffer(indicesBuffer);
        }*/
        if (positionsCapacity > 0) {
            ByteBuffer positionsBuffer = ByteBuffer.allocate(positionsCapacity);
            positionsBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nodeBuffer.setPositionsBuffer(positionsBuffer);
        }
        /*if (normalsCapacity > 0) {
            ByteBuffer normalsBuffer = ByteBuffer.allocate(normalsCapacity);
            normalsBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nodeBuffer.setNormalsBuffer(normalsBuffer);
        }*/
        if (colorsCapacity > 0) {
            ByteBuffer colorsBuffer = ByteBuffer.allocate(colorsCapacity);
            colorsBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nodeBuffer.setColorsBuffer(colorsBuffer);
        }
        /*if (texcoordCapacity > 0) {
            ByteBuffer texcoordsBuffer = ByteBuffer.allocate(texcoordCapacity);
            texcoordsBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nodeBuffer.setTexcoordsBuffer(texcoordsBuffer);
        }*/
        /*if (batchIdCapacity > 0) {
            ByteBuffer batchIdBuffer = ByteBuffer.allocate(batchIdCapacity);
            batchIdBuffer.order(ByteOrder.LITTLE_ENDIAN);
            nodeBuffer.setBatchIdBuffer(batchIdBuffer);
        }*/
        return nodeBuffer;
    }

    protected MeshPrimitive createPrimitive(GlTF gltf, GltfNodeBuffer nodeBuffer) {
        MeshPrimitive primitive = new MeshPrimitive();
        /* Points mode for point cloud */
        primitive.setMode(GltfConstants.GL_POINTS);
        Map<String, Integer> attributes = new HashMap<>();

        primitive.setAttributes(attributes);
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
