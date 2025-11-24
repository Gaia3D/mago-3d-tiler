package com.gaia3d.converter.gltf;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * GltfBinary is a class that contains the binary data of the glTF file.
 * It contains the binary data of the glTF file, and the information of the buffer of each node.
 * The binary data of the glTF file is stored in the body variable.
 */
@Getter
@Setter
@Slf4j
public class GltfBinary {
    private int materialId = -1;
    private int textureId = -1;
    private int imageId = -1;

    private ByteBuffer body = null;
    private List<GltfNodeBuffer> nodeBuffers = new ArrayList<>();
    private List<ImageBuffer> imageBuffers = new ArrayList<>();
    private List<ByteBuffer> propertyBuffers = new ArrayList<>();
    private List<ByteBuffer> instancingBuffers = new ArrayList<>();

    public int calcTotalByteBufferLength() {
        return nodeBuffers.stream()
                .mapToInt(GltfNodeBuffer::getTotalByteBufferLength)
                .sum();
    }

    public int calcTotalImageByteBufferLength() {
        return imageBuffers.stream()
                .mapToInt(ImageBuffer::getByteBufferLength)
                .sum();
    }

    public int calcTotalPropertyByteBufferLength() {
        return propertyBuffers.stream()
                .mapToInt(ByteBuffer::capacity)
                .sum();
    }

    public int calcTotalInstancingByteBufferLength() {
        return instancingBuffers.stream()
                .mapToInt(ByteBuffer::capacity)
                .sum();
    }

    /**
     * Fills the body variable with the binary data of the glTF file.
     * It iterates through the nodeBuffers list and puts the binary data of each node into the body variable.
     */
    public void fill() {
        int imageBuffersTotalLength = imageBuffers.stream()
                .mapToInt(ImageBuffer::getByteBufferLength)
                .sum();
        int nodeBuffersTotalLength = nodeBuffers.stream()
                .mapToInt(GltfNodeBuffer::getTotalByteBufferLength)
                .sum();
        int propertyBuffersTotalLength = propertyBuffers.stream()
                .mapToInt(ByteBuffer::capacity)
                .sum();
        int instancingBuffersTotalLength = instancingBuffers.stream()
                .mapToInt(ByteBuffer::capacity)
                .sum();

        int totalByteBufferLength = imageBuffersTotalLength + nodeBuffersTotalLength + propertyBuffersTotalLength + instancingBuffersTotalLength;

        body = ByteBuffer.allocate(totalByteBufferLength);
        ByteBuffer bodyBuffer = body;
        bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
        bodyBuffer.clear();

        nodeBuffers.forEach((nodeBuffer) -> {
            if (nodeBuffer.getIndicesBuffer() != null) {
                nodeBuffer.getIndicesBuffer()
                        .rewind();
                nodeBuffer.getIndicesBuffer()
                        .limit(nodeBuffer.getIndicesBuffer()
                                .capacity());
                bodyBuffer.put(nodeBuffer.getIndicesBuffer());
            }
            if (nodeBuffer.getPositionsBuffer() != null) {
                nodeBuffer.getPositionsBuffer()
                        .rewind();
                nodeBuffer.getPositionsBuffer()
                        .limit(nodeBuffer.getPositionsBuffer()
                                .capacity());
                bodyBuffer.put(nodeBuffer.getPositionsBuffer());
            }
            if (nodeBuffer.getNormalsBuffer() != null) {
                nodeBuffer.getNormalsBuffer()
                        .rewind();
                nodeBuffer.getNormalsBuffer()
                        .limit(nodeBuffer.getNormalsBuffer()
                                .capacity());
                bodyBuffer.put(nodeBuffer.getNormalsBuffer());
            }
            if (nodeBuffer.getColorsBuffer() != null) {
                nodeBuffer.getColorsBuffer()
                        .rewind();
                nodeBuffer.getColorsBuffer()
                        .limit(nodeBuffer.getColorsBuffer()
                                .capacity());
                bodyBuffer.put(nodeBuffer.getColorsBuffer());
            }
            if (nodeBuffer.getTexcoordsBuffer() != null) {
                nodeBuffer.getTexcoordsBuffer()
                        .rewind();
                nodeBuffer.getTexcoordsBuffer()
                        .limit(nodeBuffer.getTexcoordsBuffer()
                                .capacity());
                bodyBuffer.put(nodeBuffer.getTexcoordsBuffer());
            }
            if (nodeBuffer.getBatchIdBuffer() != null) {
                nodeBuffer.getBatchIdBuffer()
                        .rewind();
                nodeBuffer.getBatchIdBuffer()
                        .limit(nodeBuffer.getBatchIdBuffer()
                                .capacity());
                bodyBuffer.put(nodeBuffer.getBatchIdBuffer());
            }
        });

        imageBuffers.forEach((imageBuffer) -> {
            if (imageBuffer.getByteBuffer() != null) {
                imageBuffer.getByteBuffer()
                        .rewind();
                imageBuffer.getByteBuffer()
                        .limit(imageBuffer.getByteBuffer()
                                .capacity());
                bodyBuffer.put(imageBuffer.getByteBuffer());
            }
        });

        propertyBuffers.forEach((propertyBuffer) -> {
            propertyBuffer.rewind();
            propertyBuffer.limit(propertyBuffer.capacity());
            bodyBuffer.put(propertyBuffer);
        });

        instancingBuffers.forEach((instancingBuffer) -> {
            instancingBuffer.rewind();
            instancingBuffer.limit(instancingBuffer.capacity());
            bodyBuffer.put(instancingBuffer);
        });
        bodyBuffer.rewind();
    }
}