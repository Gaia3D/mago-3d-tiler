package com.gaia3d.converter.jgltf.tiles;

import com.gaia3d.basic.types.AccessorType;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.converter.jgltf.GltfBinary;
import com.gaia3d.converter.jgltf.GltfNodeBuffer;
import com.gaia3d.converter.jgltf.GltfWriter;
import com.gaia3d.converter.jgltf.Quantization;
import com.gaia3d.converter.jgltf.extension.ExtensionConstant;
import com.gaia3d.converter.jgltf.extension.ExtensionStructuralMetadata;
import com.gaia3d.converter.jgltf.extension.ExtensionStructuralMetadataMapper;
import com.gaia3d.process.postprocess.batch.GaiaBatchTable;
import com.gaia3d.process.postprocess.instance.GaiaFeatureTable;
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
import org.lwjgl.opengl.GL20;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        gltf.addExtensionsUsed(ExtensionConstant.STRUCTURAL_METADATA.getExtensionName());

        ExtensionStructuralMetadata extensionStructuralMetadata = ExtensionStructuralMetadata.fromPointCloudBuffer(pointCloudBuffer);
        Map<String, Object> extensions = new HashMap<>();
        extensions.put(ExtensionConstant.STRUCTURAL_METADATA.getExtensionName(), extensionStructuralMetadata);
        gltf.setExtensions(extensions);

        convertNode(gltf, binary, rootNode, pointCloudBuffer, featureTable, batchTable);

        applyInstanceFeaturesBinary(gltf, binary, pointCloudBuffer);

        binary.fill();
        if (binary.getBody() != null) {
            GltfAssetV2 asset = new GltfAssetV2(gltf, binary.getBody());
            return GltfModels.create(asset);
        }
        return null;
    }

    private void applyInstanceFeaturesBinary(GlTF gltf, GltfBinary binary, PointCloudBuffer pointCloudBuffer) {
        List<GltfNodeBuffer> nodeBuffers = binary.getNodeBuffers();
        nodeBuffers.forEach(nodeBuffer -> {
            List<ByteBuffer> instancingBuffers = binary.getInstancingBuffers();
            byte[] classificationBytes = pointCloudBuffer.getClassificationPadBytes();
            byte[] intensityBytes = pointCloudBuffer.getIntensityPadBytes();

            int binaryTotalByteBufferLength = binary.calcTotalByteBufferLength();
            int classificationByteBufferLength = classificationBytes.length;
            int intensityByteBufferLength = intensityBytes.length;

            classificationByteBufferLength = padMultiple4(classificationByteBufferLength);
            intensityByteBufferLength = padMultiple4(intensityByteBufferLength);

            ByteBuffer classificationByteBuffer = ByteBuffer.allocate(classificationByteBufferLength)
                    .order(ByteOrder.LITTLE_ENDIAN);
            ByteBuffer intensityByteBuffer = ByteBuffer.allocate(intensityByteBufferLength)
                    .order(ByteOrder.LITTLE_ENDIAN);
            classificationByteBuffer.put(classificationBytes);
            intensityByteBuffer.put(intensityBytes);
            classificationByteBuffer.flip();
            intensityByteBuffer.flip();

            instancingBuffers.add(classificationByteBuffer);
            instancingBuffers.add(intensityByteBuffer);

            int classificationBufferViewId = createBufferView(gltf, 0, binaryTotalByteBufferLength, classificationByteBufferLength, 4, GL20.GL_ARRAY_BUFFER);
            int intensityBufferViewId = createBufferView(gltf, 0, binaryTotalByteBufferLength + classificationByteBufferLength, intensityByteBufferLength, 4, GL20.GL_ARRAY_BUFFER);

            BufferView classificationBufferView = gltf.getBufferViews()
                    .get(classificationBufferViewId);
            classificationBufferView.setName("classifications");
            BufferView intensityBufferView = gltf.getBufferViews()
                    .get(intensityBufferViewId);
            intensityBufferView.setName("intensities");

            int classificationAccessorId = createAccessor(gltf, classificationBufferViewId, 0, classificationByteBufferLength / 4, GltfConstants.GL_UNSIGNED_SHORT, AccessorType.SCALAR, false);
            int intensityAccessorId = createAccessor(gltf, intensityBufferViewId, 0, intensityByteBufferLength / 4, GltfConstants.GL_UNSIGNED_SHORT, AccessorType.SCALAR, false);

            MeshPrimitive primitive = gltf.getMeshes()
                    .get(0)
                    .getPrimitives()
                    .get(0);
            Map<String, Integer> attributes = primitive.getAttributes();

            Map<AttributeType, Integer> accessorMap = nodeBuffer.getAccessorMap();
            AttributeType classificationType = AttributeType.CLASSIFICATION;
            accessorMap.put(classificationType, classificationAccessorId);
            if (accessorMap.containsKey(classificationType)) {
                attributes.put(classificationType.getAccessor(), accessorMap.get(classificationType));
            }

            AttributeType intensityType = AttributeType.INTENSITY;
            accessorMap.put(intensityType, intensityAccessorId);
            if (accessorMap.containsKey(intensityType)) {
                attributes.put(intensityType.getAccessor(), accessorMap.get(intensityType));
            }

            ExtensionStructuralMetadataMapper extensionStructuralMetadata = new ExtensionStructuralMetadataMapper();
            List<Integer> propertyAttributes = new ArrayList<>();
            propertyAttributes.add(0); // First property is always the position
            extensionStructuralMetadata.setPropertyAttributes(propertyAttributes);

            Map<String, Object> extensions = new HashMap<>();
            extensions.put(ExtensionConstant.STRUCTURAL_METADATA.getExtensionName(), extensionStructuralMetadata);
            primitive.setExtensions(extensions);
        });


    }


    protected void convertNode(GlTF gltf, GltfBinary binary, Node parentNode, PointCloudBuffer pointCloudBuffer, GaiaFeatureTable featureTable, GaiaBatchTable batchTable) {
        List<GltfNodeBuffer> nodeBuffers = binary.getNodeBuffers();

        Node node = createNode(gltf, parentNode);
        GltfNodeBuffer nodeBuffer = convertGeometryInfo(gltf, binary, pointCloudBuffer, node, featureTable, batchTable);

        int nodeId = gltf.getNodes()
                .size() - 1;
        if (parentNode != null) {
            parentNode.addChildren(nodeId);
        }

        nodeBuffers.add(nodeBuffer);
    }

    protected Node createNode(GlTF gltf, Node parentNode) {
        Node node;
        if (parentNode == null) {
            node = gltf.getNodes()
                    .get(0);
        } else {
            node = new Node();
            gltf.addNodes(node);
        }

        node.setName("PointCloudNode");
        return node;
    }

    protected GltfNodeBuffer convertGeometryInfo(GlTF gltf, GltfBinary binary, PointCloudBuffer pointCloudBuffer, Node node, GaiaFeatureTable featureTable, GaiaBatchTable batchTable) {
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
        byte[] colors = pointCloudBuffer.getColorBytes();
        float[] batchIds = pointCloudBuffer.getBatchIds();
        //short[] classifications = pointCloudBuffer.getClassifications();
        //char[] intensities = pointCloudBuffer.getIntensities();

        GltfNodeBuffer nodeBuffer = initNodeBuffer(pointCloudBuffer);
        createBuffer(gltf, nodeBuffer);

        ByteBuffer positionsBuffer = nodeBuffer.getPositionsBuffer();
        ByteBuffer normalsBuffer = nodeBuffer.getNormalsBuffer();
        ByteBuffer colorsBuffer = nodeBuffer.getColorsBuffer();
        ByteBuffer batchIdBuffer = nodeBuffer.getBatchIdBuffer();

        int positionsBufferViewId = nodeBuffer.getPositionsBufferViewId();
        int normalsBufferViewId = nodeBuffer.getNormalsBufferViewId();
        int colorsBufferViewId = nodeBuffer.getColorsBufferViewId();
        int batchIdBufferViewId = nodeBuffer.getBatchIdBufferViewId();

        if (positionsBuffer != null) {
            for (short position : unsignedShortsPositions) {
                positionsBuffer.putShort(position);
            }
        }
        if (normalsBuffer != null) {
            for (Short normal : normals) {
                normalsBuffer.putShort(normal);
            }
        }
        if (colorsBuffer != null) {
            for (Byte color : colors) {
                colorsBuffer.put(color);
            }
        }
        if (batchIdBuffer != null) {
            for (Float batchId : batchIds) {
                batchIdBuffer.putFloat(batchId);
            }
        }

        if (positionsBufferViewId > -1 && positions.length > 0) {
            int verticesAccessorId = createAccessor(gltf, positionsBufferViewId, 0, positions.length / 3, GltfConstants.GL_UNSIGNED_SHORT, AccessorType.VEC3, true);
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
        Buffer buffer = null;
        if (gltf.getBuffers() == null) {
            buffer = new Buffer();
            gltf.addBuffers(buffer);
        }
        return buffer;
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
