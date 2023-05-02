package util;

import org.joml.Vector4d;

import java.io.IOException;
import java.io.OutputStream;

public class BinaryUtils {
    public static void writeByte(OutputStream stream, byte value) throws IOException {
        stream.write(value);
    }

    public static void writeBoolean(OutputStream stream, boolean value) throws IOException {
        stream.write((byte) (value ? 1 : 0));
    }

    public static void writeInt(OutputStream stream, int value) throws IOException {
        stream.write(value);
    }

    public static void writeText(OutputStream stream, String value) throws IOException {
        byte[] bytes = value.getBytes();
        stream.write(value.length());
        stream.write(bytes);
    }

    public static void writeFloat(OutputStream stream, float value) throws IOException {
        stream.write(floatToBytes(value));
    }

    public static void writeVector4(OutputStream stream, Vector4d values) throws IOException {
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
}
