package tiler;

import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;

@Getter
@Setter
public class GltfBinary {
    private int indicesBufferViewId = -1;
    private int verticesBufferViewId = -1;
    private int normalsBufferViewId = -1;
    private int colorsBufferViewId = -1;
    private int textureCoordinatesBufferViewId = -1;
    private int textureBufferViewId = -1;

    private int indicesAccessorId = -1;
    private int verticesAccessorId = -1;
    private int normalsAccessorId = -1;
    private int colorsAccessorId = -1;
    private int textureCoordinatesAccessorId = -1;
    private int textureAccessorId = -1;

    private ByteBuffer body;
    private ByteBuffer indicesBuffer;
    private ByteBuffer verticesBuffer;
    private ByteBuffer normalsBuffer;
    private ByteBuffer colorsBuffer;
    private ByteBuffer textureCoordinatesBuffer;
    private ByteBuffer textureBuffer;

    public void fill() {
        body.clear();
        if (indicesBuffer != null) {
            indicesBuffer.rewind();
            indicesBuffer.limit(indicesBuffer.capacity());
            body.put(indicesBuffer);
        }
        if (verticesBuffer != null) {
            verticesBuffer.rewind();
            verticesBuffer.limit(verticesBuffer.capacity());
            body.put(verticesBuffer);
        }
        if (normalsBuffer != null) {
            normalsBuffer.rewind();
            normalsBuffer.limit(normalsBuffer.capacity());
            body.put(normalsBuffer);
        }
        if (colorsBuffer != null) {
            colorsBuffer.rewind();
            colorsBuffer.limit(colorsBuffer.capacity());
            body.put(colorsBuffer);
        }
        if (textureCoordinatesBuffer != null) {
            textureCoordinatesBuffer.rewind();
            textureCoordinatesBuffer.limit(textureCoordinatesBuffer.capacity());
            body.put(textureCoordinatesBuffer);
        }
        if (textureBuffer != null) {
            textureBuffer.rewind();
            textureBuffer.limit(textureBuffer.capacity());
            body.put(textureBuffer);
        }
        body.rewind();
    }
}
