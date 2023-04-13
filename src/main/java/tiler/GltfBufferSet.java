package tiler;

import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;

@Setter
@Getter
public class GltfBufferSet {
    private int bufferId = -1;
    private int bufferViewId = -1;
    private int accessorId = -1;

    private int dataLength = -1;
    private int bufferStride = -1;
    private int byteLength = -1;
    ByteBuffer buffer = null;

    public GltfBufferSet(int dataLength, int stride) {
        this.dataLength = dataLength;
        this.byteLength = dataLength * stride;
        this.bufferStride = stride;
        buffer = ByteBuffer.allocate(0);
    }
}
