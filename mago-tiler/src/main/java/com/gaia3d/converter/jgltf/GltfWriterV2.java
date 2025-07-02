package com.gaia3d.converter.jgltf;

import com.gaia3d.basic.model.GaiaMesh;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaPrimitive;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.types.AccessorType;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.process.postprocess.batch.GaiaBatchTableMap;
import com.gaia3d.process.postprocess.instance.GaiaFeatureTable;
import com.gaia3d.util.GeometryUtils;
import de.javagl.jgltf.impl.v2.*;
import de.javagl.jgltf.model.GltfConstants;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.GltfModels;
import de.javagl.jgltf.model.io.GltfModelWriter;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
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
 */
@Slf4j
@NoArgsConstructor
public class GltfWriterV2 extends GltfWriter {

    /**
     * Write the glTF file from the GaiaScene object.
     * @param gaiaScene  The GaiaScene object to be written.
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
     * @param gaiaScene  The GaiaScene object to be written.
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

        double[] rctCenterOrigin = featureTable.getRctCenter();

        /*Matrix4f rootMatrix = new Matrix4f();
        rootMatrix.set(rootNode.getMatrix());

        Vector3f rctCenter = new Vector3f();
        rctCenter.set(new float[]{(float) rctCenterOrigin[0], (float) rctCenterOrigin[1], (float) rctCenterOrigin[2]});

        Matrix4f translationMatrix = new Matrix4f();
        translationMatrix.setTranslation(rctCenter);

        rootMatrix = rootMatrix.mul(translationMatrix);
        rootNode.setMatrix(rootMatrix.get(new float[16]));
*/
        rootNode.setTranslation(new float[]{(float) rctCenterOrigin[0], (float) rctCenterOrigin[2], (float) -rctCenterOrigin[1]});

        if (globalOptions.isUseQuantization()) {
            gltf.addExtensionsUsed("KHR_mesh_quantization");
            gltf.addExtensionsRequired("KHR_mesh_quantization");
        }

        // Batch table
        gltf.addExtensionsUsed("EXT_mesh_features");
        //gltf.addExtensionsRequired("EXT_mesh_features");
        gltf.addExtensionsUsed("EXT_structural_metadata");
        //gltf.addExtensionsRequired("EXT_structural_metadata");

        // Instance table
        //gltf.addExtensionsUsed("EXT_mesh_gpu_instancing");

        ExtensionStructuralMetadata extensionStructuralMetadata = ExtensionStructuralMetadata.fromBatchTable(batchTableMap);
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("EXT_structural_metadata", extensionStructuralMetadata);
        gltf.setExtensions(extensions);


        convertNode(gltf, binary, rootNode, gaiaScene.getNodes(), featureTable, batchTableMap);
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
        log.info("[INFO] Apply properties binary to glTF");

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

                int offsetBufferViewId = createBufferView(gltf, 0, bufferOffset.get() + stringBuffer.capacity(), offsetBuffer.capacity(), -1, GL20.GL_ARRAY_BUFFER);
                property.setStringOffsets(offsetBufferViewId);

                bufferOffset.addAndGet(stringBuffer.capacity() + offsetBuffer.capacity());

                buffers.add(stringBuffer);
                buffers.add(offsetBuffer);

                log.info("[INFO] Property: {}, Values: {}", name, values.size());
                //int bufferViewId = createBufferView(gltf, 0, bufferOffset.get(), property.getPrimaryValues().size() * 4, -1, GL20.GL_ARRAY_BUFFER);
            });
        });

    }

    private ByteBuffer[] createStringBuffers(List<String> strings) {
        int totalStringLength = 0;
        byte[][] encodedFeatures = new byte[strings.size()][];
        for (int i = 0; i < strings.size(); i++) {
            String feature = strings.get(i);
            encodedFeatures[i] = feature.getBytes(StandardCharsets.UTF_8);
            totalStringLength += encodedFeatures[i].length;
        }



        /*for (int i = 0; i < features.length; i++) {
            encodedFeatures[i] = features[i].getBytes(StandardCharsets.UTF_8);
            totalStringLength += encodedFeatures[i].length;
        }*/

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


    protected void convertNode(GlTF gltf, GltfBinary binary, Node parentNode, List<GaiaNode> gaiaNodes, GaiaFeatureTable featureTable, GaiaBatchTableMap<String, List<String>> batchTableMap) {
        List<GltfNodeBuffer> nodeBuffers = binary.getNodeBuffers();
        gaiaNodes.forEach((gaiaNode) -> {
            Node node = createNode(gltf, parentNode, gaiaNode);

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

    protected GltfNodeBuffer convertGeometryInfo(GlTF gltf, GaiaMesh gaiaMesh, Node node, GaiaBatchTableMap<String, List<String>> batchTableMap) {
        int[] indices = gaiaMesh.getIndices();
        float[] positions = gaiaMesh.getPositions();

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

    protected Buffer initBuffer(GlTF gltf) {
        if (gltf.getBuffers() == null) {
            Buffer buffer = new Buffer();
            gltf.addBuffers(buffer);
        }
        return gltf.getBuffers().get(0);
    }

    protected void createBuffer(GlTF gltf, GltfNodeBuffer nodeBuffer) {
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
            if (globalOptions.isUseQuantization()) {
                ByteBuffer positionsBuffer = nodeBuffer.getPositionsBuffer();
                int bufferViewId = createBufferView(gltf, bufferId, bufferLength + bufferOffset, positionsBuffer.capacity(), 8, GL20.GL_ARRAY_BUFFER);
                nodeBuffer.setPositionsBufferViewId(bufferViewId);
                BufferView bufferView = gltf.getBufferViews().get(bufferViewId);
                bufferView.setName("positions");
                bufferOffset += positionsBuffer.capacity();
            } else {
                ByteBuffer positionsBuffer = nodeBuffer.getPositionsBuffer();
                int bufferViewId = createBufferView(gltf, bufferId, bufferLength + bufferOffset, positionsBuffer.capacity(), 12, GL20.GL_ARRAY_BUFFER);
                nodeBuffer.setPositionsBufferViewId(bufferViewId);
                BufferView bufferView = gltf.getBufferViews().get(bufferViewId);
                bufferView.setName("positions");
                bufferOffset += positionsBuffer.capacity();
            }
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

    @Override
    protected GltfNodeBuffer initNodeBuffer(GaiaMesh gaiaMesh, boolean isIntegerIndices) {
        GltfNodeBuffer nodeBuffer = new GltfNodeBuffer();
        int SHORT_SIZE = 2;
        int INT_SIZE = 4;
        int FLOAT_SIZE = 4;

        int indicesCapacity = gaiaMesh.getIndicesCount() * (isIntegerIndices ? INT_SIZE : SHORT_SIZE);
        int positionsCapacity = gaiaMesh.getPositionsCount() * FLOAT_SIZE;
        if (globalOptions.isUseQuantization()) {
            int paddedPositionsCount = gaiaMesh.getPositionsCount() / 3 * 4;
            positionsCapacity = paddedPositionsCount * SHORT_SIZE;
        }
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

    protected MeshPrimitive createPrimitive(GltfNodeBuffer nodeBuffer, GaiaPrimitive gaiaPrimitive, List<Material> materials, GaiaBatchTableMap<String, List<String>> batchTableMap) {
        MeshPrimitive primitive = new MeshPrimitive();
        primitive.setMode(GltfConstants.GL_TRIANGLES);
        /*if (materials != null && !materials.isEmpty()) {
            primitive.setMaterial(gaiaPrimitive.getMaterialIndex());
        }*/
        primitive.setMaterial(gaiaPrimitive.getMaterialIndex());
        primitive.setAttributes(new HashMap<>());
        primitive.setIndices(nodeBuffer.getIndicesAccessorId());

        ExtensionMeshFeatures extensionMeshFeatures = ExtensionMeshFeatures.fromBatchTable(batchTableMap);
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("EXT_mesh_features", extensionMeshFeatures);
        primitive.setExtensions(extensions);

        if (nodeBuffer.getPositionsAccessorId() > -1) {
            primitive.getAttributes()
                    .put(AttributeType.POSITION.getAccessor(), nodeBuffer.getPositionsAccessorId());
        }
        if (nodeBuffer.getNormalsAccessorId() > -1) {
            primitive.getAttributes()
                    .put(AttributeType.NORMAL.getAccessor(), nodeBuffer.getNormalsAccessorId());
        }
        if (nodeBuffer.getColorsAccessorId() > -1) {
            primitive.getAttributes()
                    .put(AttributeType.COLOR.getAccessor(), nodeBuffer.getColorsAccessorId());
        }
        if (nodeBuffer.getTexcoordsAccessorId() > -1) {
            primitive.getAttributes()
                    .put(AttributeType.TEXCOORD.getAccessor(), nodeBuffer.getTexcoordsAccessorId());
        }
        if (nodeBuffer.getBatchIdAccessorId() > -1) {
            primitive.getAttributes()
                    .put(AttributeType.FEATURE_ID_0.getAccessor(), nodeBuffer.getBatchIdAccessorId());
        }

        return primitive;
    }
}
