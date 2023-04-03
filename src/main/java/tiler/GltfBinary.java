package tiler;

import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;

@Getter
@Setter
public class GltfBinary {
    private int indicesBufferId;
    private int verticesBufferId;
    private int normalsBufferId;
    private int colorsBufferId;
    private int textureCoordinatesBufferId;

    private ByteBuffer body;
    private ByteBuffer indicesBuffer;
    private ByteBuffer verticesBuffer;
    private ByteBuffer normalsBuffer;
    private ByteBuffer colorsBuffer;
    private ByteBuffer textureCoordinatesBuffer;

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
        body.rewind();
    }
}
