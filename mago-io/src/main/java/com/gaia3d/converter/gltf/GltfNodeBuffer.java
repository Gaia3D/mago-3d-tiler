package com.gaia3d.converter.gltf;

import com.gaia3d.basic.types.AttributeType;
import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * GltfBinary is a class that contains the binary data of the glTF file.
 * It contains the binary data of the glTF file, and the information of the buffer of each node.
 * The binary data of the glTF file is stored in the body variable.
 */
@Getter
@Setter
public class GltfNodeBuffer {
    private int indicesBufferViewId = -1;
    private int positionsBufferViewId = -1;
    private int normalsBufferViewId = -1;
    private int colorsBufferViewId = -1;
    private int texcoordsBufferViewId = -1;
    private int batchIdBufferViewId = -1;
    private int imageBufferViewId = -1;

    private int indicesAccessorId = -1;
    private int positionsAccessorId = -1;
    private int normalsAccessorId = -1;
    private int colorsAccessorId = -1;
    private int texcoordsAccessorId = -1;
    private int batchIdAccessorId = -1;

    private int totalByteBufferLength = -1;

    private ByteBuffer indicesBuffer = null;
    private ByteBuffer positionsBuffer = null;
    private ByteBuffer normalsBuffer = null;
    private ByteBuffer colorsBuffer = null;
    private ByteBuffer texcoordsBuffer = null;
    private ByteBuffer batchIdBuffer = null;
    private ByteBuffer textureBuffer = null;

    private Map<AttributeType, Integer> accessorMap = new HashMap<>();

    public int getMaxBufferViewId() {
        return Math.max(Math.max(Math.max(Math.max(Math.max(indicesBufferViewId, positionsBufferViewId), normalsBufferViewId), colorsBufferViewId), texcoordsBufferViewId), batchIdBufferViewId);
    }

    public int getMaxAccessorId() {
        return Math.max(Math.max(Math.max(Math.max(Math.max(indicesAccessorId, positionsAccessorId), normalsAccessorId), colorsAccessorId), texcoordsAccessorId), batchIdAccessorId);
    }
}