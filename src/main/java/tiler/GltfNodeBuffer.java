package tiler;

import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;
import java.util.Optional;

@Getter
@Setter
public class GltfNodeBuffer {
//    Optional<GltfBufferSet> indicesBufferSet = Optional.empty();
//    Optional<GltfBufferSet> positionsBufferSet = Optional.empty();;
//    Optional<GltfBufferSet> normalsBufferSet = Optional.empty();;
//    Optional<GltfBufferSet> colorsBufferSet = Optional.empty();;
//    Optional<GltfBufferSet> textureCoordinatesBufferSet = Optional.empty();;
//    private int totalByteBufferLength = 0;

    private int indicesBufferViewId = -1;
    private int positionsBufferViewId = -1;
    private int normalsBufferViewId = -1;
    private int colorsBufferViewId = -1;
    private int textureCoordinatesBufferViewId = -1;

    private int indicesAccessorId = -1;
    private int positionsAccessorId = -1;
    private int normalsAccessorId = -1;
    private int colorsAccessorId = -1;
    private int textureCoordinatesAccessorId = -1;

    private int totalByteBufferLength = -1;

    private Optional<ByteBuffer> indicesBuffer = Optional.empty();
    private Optional<ByteBuffer> positionsBuffer = Optional.empty();
    private Optional<ByteBuffer> normalsBuffer = Optional.empty();
    private Optional<ByteBuffer> colorsBuffer = Optional.empty();
    private Optional<ByteBuffer> textureCoordinatesBuffer = Optional.empty();

    private Optional<ByteBuffer> textureBuffer = Optional.empty();
}
