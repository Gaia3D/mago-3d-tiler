package util;

import jdk.jfr.Unsigned;
import org.joml.Vector4d;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class BinaryUtils {
    public static void writeByte(DataOutputStream stream, byte value) throws IOException {
        stream.write(value);
    }

    public static void writeBoolean(DataOutputStream stream, boolean value) throws IOException {
        stream.write((byte) (value ? 1 : 0));
    }

    public static void writeInt(DataOutputStream stream, int value) throws IOException {
        stream.write(value);
    }

    public static void writeShort(DataOutputStream stream, short value) throws IOException {
        stream.writeShort(value);
    }

    public static void writeText(DataOutputStream stream, String value) throws IOException {
        byte[] bytes = value.getBytes();
        stream.write(value.length());
        stream.write(bytes);
    }

    public static void writeFloat(DataOutputStream stream, float value) throws IOException {
        stream.write(floatToBytes(value));
    }

    public static void writeVector4(DataOutputStream stream, Vector4d values) throws IOException {
        float r = (float) values.x();
        float g = (float) values.y();
        float b = (float) values.z();
        float a = (float) values.w();
        writeFloat(stream, r);
        writeFloat(stream, g);
        writeFloat(stream, b);
        writeFloat(stream, a);
    }

    public static byte[] floatToBytes(float value) {
        int intBits =  Float.floatToIntBits(value);
        return new byte[] {
                (byte) (intBits >> 24),
                (byte) (intBits >> 16),
                (byte) (intBits >> 8),
                (byte) (intBits) };
    }

    public static void writeBytes(DataOutputStream stream, byte[] bytes) throws IOException {
        stream.write(bytes);
    }

    public static void writeFloats(DataOutputStream stream, float[] floats) throws IOException {
        for (float value : floats) {
            writeFloat(stream, value);
        }
    }

    public static void writeInts(DataOutputStream stream, int[] ints) throws IOException {
        for (int value : ints) {
            writeInt(stream, value);
        }
    }

    public static void writeShorts(DataOutputStream stream, short[] shorts) throws IOException {
        for (short value : shorts) {
            writeShort(stream, value);
        }
    }
}
