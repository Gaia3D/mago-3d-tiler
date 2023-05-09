package geometry.exchangable;

import de.javagl.jgltf.impl.v1.Accessor;
import de.javagl.jgltf.model.GltfConstants;
import geometry.structure.GaiaVertex;
import geometry.types.AccessorType;
import geometry.types.AttributeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.lwjgl.opengl.GL20;
import renderable.RenderableBuffer;
import util.BinaryUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaBuffer {
    AttributeType attributeType;
    AccessorType accessorType;

    int elementsCount = -1;

    byte glDimension;
    int glType;
    int glTarget;

    float[] floats;
    int[] ints;
    short[] shorts;
    byte[] bytes;

    public void writeBuffer(DataOutputStream stream) throws IOException {
        BinaryUtils.writeByte(stream, glDimension); // glDimension (SCALAR, VEC2, VEC3, VEC4, MAT2, MAT3, MAT4)
        BinaryUtils.writeInt(stream, glType); // glType (FLOAT, INT, UNSIGNED_INT, SHORT, UNSIGNED_SHORT, BYTE, UNSIGNED_BYTE)
        BinaryUtils.writeInt(stream, glTarget); // target (ARRAY_BUFFER, ELEMENT_ARRAY_BUFFER
        BinaryUtils.writeInt(stream, elementsCount); // elementsCount
        if (glType == GltfConstants.GL_FLOAT) {
            BinaryUtils.writeInt(stream, floats.length);
            BinaryUtils.writeFloats(stream, floats);
        } else if (glType == GltfConstants.GL_INT) {
            BinaryUtils.writeInt(stream, ints.length);
            BinaryUtils.writeInts(stream, ints);
        } else if (glType == GltfConstants.GL_SHORT) {
            BinaryUtils.writeInt(stream, shorts.length);
            BinaryUtils.writeShorts(stream, shorts);
        } else if (glType == GltfConstants.GL_UNSIGNED_SHORT) {
            BinaryUtils.writeInt(stream, shorts.length);
            BinaryUtils.writeShorts(stream, shorts);
        } else if (glType == GltfConstants.GL_BYTE) {
            BinaryUtils.writeInt(stream, bytes.length);
            BinaryUtils.writeBytes(stream, bytes);
        } else if (glType == GltfConstants.GL_UNSIGNED_BYTE) {
            BinaryUtils.writeInt(stream, bytes.length);
            BinaryUtils.writeBytes(stream, bytes);
        }
    }

    //readBuffer
    public void readBuffer(DataInputStream stream) throws IOException {
        glDimension = BinaryUtils.readByte(stream);
        glType = BinaryUtils.readInt(stream);
        glTarget = BinaryUtils.readInt(stream);
        elementsCount = BinaryUtils.readInt(stream);
        if (glType == GltfConstants.GL_FLOAT) {
            int length = BinaryUtils.readInt(stream);
            floats = BinaryUtils.readFloats(stream, length);
        } else if (glType == GltfConstants.GL_INT) {
            int length = BinaryUtils.readInt(stream);
            ints = BinaryUtils.readInts(stream, length);
        } else if (glType == GltfConstants.GL_SHORT) {
            int length = BinaryUtils.readInt(stream);
            shorts = BinaryUtils.readShorts(stream, length);
        } else if (glType == GltfConstants.GL_UNSIGNED_SHORT) {
            int length = BinaryUtils.readInt(stream);
            shorts = BinaryUtils.readShorts(stream, length);
        } else if (glType == GltfConstants.GL_BYTE) {
            int length = BinaryUtils.readInt(stream);
            bytes = BinaryUtils.readBytes(stream, length);
        } else if (glType == GltfConstants.GL_UNSIGNED_BYTE) {
            int length = BinaryUtils.readInt(stream);
            bytes = BinaryUtils.readBytes(stream, length);
        }
    }
}
