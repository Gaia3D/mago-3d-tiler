package gltf;

import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class GltfBinary {
    private int materialId = -1;
    private int textureId = -1;
    private int imageId = -1;

    private ByteBuffer body = null;
    private List<GltfNodeBuffer> nodeBuffers = new ArrayList<>();

    public void fill() {
        body = ByteBuffer.allocate(nodeBuffers.stream().mapToInt(GltfNodeBuffer::getTotalByteBufferLength).sum());
        ByteBuffer bodyBuffer = body;
        bodyBuffer.order(ByteOrder.LITTLE_ENDIAN);
        bodyBuffer.clear();
        nodeBuffers.forEach((nodeBuffer) -> {
            if (nodeBuffer.getIndicesBuffer() != null) {
                nodeBuffer.getIndicesBuffer().rewind();
                nodeBuffer.getIndicesBuffer().limit(nodeBuffer.getIndicesBuffer().capacity());
                bodyBuffer.put(nodeBuffer.getIndicesBuffer());
            }
            if (nodeBuffer.getPositionsBuffer() != null) {
                nodeBuffer.getPositionsBuffer().rewind();
                nodeBuffer.getPositionsBuffer().limit(nodeBuffer.getPositionsBuffer().capacity());
                bodyBuffer.put(nodeBuffer.getPositionsBuffer());
            }
            if (nodeBuffer.getNormalsBuffer() != null) {
                nodeBuffer.getNormalsBuffer().rewind();
                nodeBuffer.getNormalsBuffer().limit(nodeBuffer.getNormalsBuffer().capacity());
                bodyBuffer.put(nodeBuffer.getNormalsBuffer());
            }
            if (nodeBuffer.getColorsBuffer() != null) {
                nodeBuffer.getColorsBuffer().rewind();
                nodeBuffer.getColorsBuffer().limit(nodeBuffer.getColorsBuffer().capacity());
                bodyBuffer.put(nodeBuffer.getColorsBuffer());
            }
            if (nodeBuffer.getTexcoordsBuffer() != null) {
                nodeBuffer.getTexcoordsBuffer().rewind();
                nodeBuffer.getTexcoordsBuffer().limit(nodeBuffer.getTexcoordsBuffer().capacity());
                bodyBuffer.put(nodeBuffer.getTexcoordsBuffer());
            }
        });
        bodyBuffer.rewind();
    }
}