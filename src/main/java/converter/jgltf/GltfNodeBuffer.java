package converter.jgltf;

import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;

@Getter
@Setter
public class GltfNodeBuffer {
    private int indicesBufferViewId = -1;
    private int positionsBufferViewId = -1;
    private int normalsBufferViewId = -1;
    private int colorsBufferViewId = -1;
    private int texcoordsBufferViewId = -1;
    private int batchIdBufferViewId = -1;

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
}
