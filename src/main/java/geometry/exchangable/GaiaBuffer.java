package geometry.exchangable;

import de.javagl.jgltf.impl.v1.Accessor;
import de.javagl.jgltf.model.GltfConstants;
import geometry.types.AccessorType;
import geometry.types.AttributeType;
import util.BinaryUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class GaiaBuffer {
    AttributeType attributeType;
    AccessorType accessorType;

    int glDimension;
    int glType;
    int glTarget;

    float[] floats;
    int[] ints;
    short[] shorts;
    byte[] bytes;

    public void writeBuffer(DataOutputStream stream) throws IOException {
        BinaryUtils.writeByte(stream, (byte) glDimension); // glDimension (SCALAR, VEC2, VEC3, VEC4, MAT2, MAT3, MAT4)
        BinaryUtils.writeInt(stream, glType); // glType (FLOAT, INT, UNSIGNED_INT, SHORT, UNSIGNED_SHORT, BYTE, UNSIGNED_BYTE)
        BinaryUtils.writeInt(stream, glTarget); // target (ARRAY_BUFFER, ELEMENT_ARRAY_BUFFER
        BinaryUtils.writeInt(stream, -1); // elementsCount
        if (glDimension == GltfConstants.GL_FLOAT) {
            BinaryUtils.writeInt(stream, floats.length);

        } else if (glDimension == GltfConstants.GL_INT) {
            BinaryUtils.writeInt(stream, ints.length);

        } else if (glDimension == GltfConstants.GL_SHORT) {
            BinaryUtils.writeInt(stream, shorts.length);

        } else if (glDimension == GltfConstants.GL_UNSIGNED_SHORT) {
            BinaryUtils.writeInt(stream, shorts.length);

        } else if (glDimension == GltfConstants.GL_BYTE) {
            BinaryUtils.writeInt(stream, bytes.length);

        } else if (glDimension == GltfConstants.GL_UNSIGNED_BYTE) {
            BinaryUtils.writeInt(stream, bytes.length);

        }


        //BinaryUtils.writeInt(stream, dimension); // elementsCount

        // DataTarget.************************
        // gl.ARRAY_BUFFER = 34962
        // gl.ELEMENT_ARRAY_BUFFER = 34963

        // DataGlType.************************
        // glType == 5126// gl.FLOAT.
        // glType == 5124// gl.INT.
        // glType == 5125// gl.UNSIGNED_INT.
        // glType == 5122// gl.SHORT.
        // glType == 5123// gl.UNSIGNED_SHORT.
        // glType == 5120// gl.BYTE.
        // glType == 5121// gl.UNSIGNED_BYTE.


        /*
        fwrite(&dataDim_char, sizeof(char), 1, f);
        fwrite(&m_dataGlType, sizeof(int), 1, f);
        fwrite(&m_dataTarget, sizeof(int), 1, f);
        fwrite(&m_elementsCount, sizeof(int), 1, f);
        */
        //TODO
    }
}
