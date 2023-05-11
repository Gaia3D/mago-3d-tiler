package geometry.exchangable;

import de.javagl.jgltf.model.GltfConstants;
import geometry.types.AccessorType;
import geometry.types.AttributeType;
import io.LittleEndianDataInputStream;
import io.LittleEndianDataOutputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;

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

    public void writeBuffer(LittleEndianDataOutputStream stream) throws IOException {
        stream.writeByte(glDimension); // glDimension (SCALAR, VEC2, VEC3, VEC4, MAT2, MAT3, MAT4)
        stream.writeInt(glType); // glType (FLOAT, INT, UNSIGNED_INT, SHORT, UNSIGNED_SHORT, BYTE, UNSIGNED_BYTE)
        stream.writeInt(glTarget); // target (ARRAY_BUFFER, ELEMENT_ARRAY_BUFFER
        stream.writeInt(elementsCount); // elementsCount
        if (glType == GltfConstants.GL_FLOAT) {
            stream.writeInt(floats.length);
            stream.writeFloats(floats);
        } else if (glType == GltfConstants.GL_INT) {
            stream.writeInt(ints.length);
            stream.writeInts(ints);
        } else if (glType == GltfConstants.GL_SHORT || glType == GltfConstants.GL_UNSIGNED_SHORT) {
            stream.writeInt(shorts.length);
            stream.writeShorts(shorts);
        } else if (glType == GltfConstants.GL_BYTE || glType == GltfConstants.GL_UNSIGNED_BYTE) {
            stream.writeInt(bytes.length);
            stream.write(bytes);
        }
    }

    //readBuffer
    public void readBuffer(LittleEndianDataInputStream stream) throws IOException {
        glDimension = stream.readByte();
        glType = stream.readInt();
        glTarget = stream.readInt();
        elementsCount = stream.readInt();
        if (glType == GltfConstants.GL_FLOAT) {
            int length = stream.readInt();
            floats = stream.readFloats(length);
        } else if (glType == GltfConstants.GL_INT) {
            int length = stream.readInt();
            ints = stream.readInts(length);
        } else if (glType == GltfConstants.GL_SHORT || glType == GltfConstants.GL_UNSIGNED_SHORT) {
            int length = stream.readInt();
            shorts = stream.readShorts(length);
        } else if (glType == GltfConstants.GL_BYTE || glType == GltfConstants.GL_UNSIGNED_BYTE) {
            int length = stream.readInt();
            bytes = stream.readBytes(length);
        }
    }
}
