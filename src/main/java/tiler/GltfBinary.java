package tiler;

import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
public class GltfBinary {
    private int materialId = -1;
    private int textureId = -1;
    private int imageId = -1;

    private Optional<ByteBuffer> body = Optional.empty();
    private List<GltfNodeBuffer> nodeBuffers = new ArrayList<>();

    public void fill() {
        body = Optional.of(ByteBuffer.allocate(nodeBuffers.stream().mapToInt(GltfNodeBuffer::getTotalByteBufferLength).sum()));
        if (body.isPresent()) {
            ByteBuffer bodyBuffer = body.get();
            bodyBuffer.clear();
            nodeBuffers.stream().forEach((nodeBuffer) -> {
                if (nodeBuffer.getIndicesBuffer().isPresent()) {
                    nodeBuffer.getIndicesBuffer().get().rewind();
                    nodeBuffer.getIndicesBuffer().get().limit(nodeBuffer.getIndicesBuffer().get().capacity());
                    bodyBuffer.put(nodeBuffer.getIndicesBuffer().get());
                }
                if (nodeBuffer.getPositionsBuffer().isPresent()) {
                    nodeBuffer.getPositionsBuffer().get().rewind();
                    nodeBuffer.getPositionsBuffer().get().limit(nodeBuffer.getPositionsBuffer().get().capacity());
                    bodyBuffer.put(nodeBuffer.getPositionsBuffer().get());
                }
                if (nodeBuffer.getNormalsBuffer().isPresent()) {
                    nodeBuffer.getNormalsBuffer().get().rewind();
                    nodeBuffer.getNormalsBuffer().get().limit(nodeBuffer.getNormalsBuffer().get().capacity());
                    bodyBuffer.put(nodeBuffer.getNormalsBuffer().get());
                }
                if (nodeBuffer.getColorsBuffer().isPresent()) {
                    nodeBuffer.getColorsBuffer().get().rewind();
                    nodeBuffer.getColorsBuffer().get().limit(nodeBuffer.getColorsBuffer().get().capacity());
                    bodyBuffer.put(nodeBuffer.getColorsBuffer().get());
                }
                if (nodeBuffer.getTextureCoordinatesBuffer().isPresent()) {
                    nodeBuffer.getTextureCoordinatesBuffer().get().rewind();
                    nodeBuffer.getTextureCoordinatesBuffer().get().limit(nodeBuffer.getTextureCoordinatesBuffer().get().capacity());
                    bodyBuffer.put(nodeBuffer.getTextureCoordinatesBuffer().get());
                }
            });
            bodyBuffer.rewind();
        }
    }
}