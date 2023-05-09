package util;

import jdk.jfr.Unsigned;
import org.joml.Vector4d;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class BinaryUtils {
    public static byte readByte(DataInputStream stream) throws IOException {
        return stream.readByte();
    }

    public static boolean readBoolean(DataInputStream stream) throws IOException {
        return stream.readByte() != 0;
    }

    public static short readShort(DataInputStream stream) throws IOException {
        return swap(stream.readShort());
    }

    public static int readInt(DataInputStream stream) throws IOException {
        return swap(stream.readInt());
    }

    public static float readFloat(DataInputStream stream) throws IOException {
        float value = stream.readFloat();
        float value1 = swap(value);
        //System.out.println(value + "::" + value1);
        return value1;
    }

    public static String readText(DataInputStream stream) throws IOException {
        int length = readInt(stream);
        byte[] bytes = new byte[length];
        stream.read(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static Vector4d readVector4(DataInputStream stream) throws IOException {
        float x = readFloat(stream);
        float y = readFloat(stream);
        float z = readFloat(stream);
        float w = readFloat(stream);
        return new Vector4d(x, y, z, w);
    }

    public static byte[] readBytes(DataInputStream stream, int length) throws IOException {
        byte[] bytes = new byte[length];
        stream.read(bytes);
        return bytes;
    }

    public static float[] readFloats(DataInputStream stream, int length) throws IOException {
        float[] floats = new float[length];
        for (int i = 0; i < length; i++) {
            floats[i] = readFloat(stream);
        }
        return floats;
    }

    public static int[] readInts(DataInputStream stream, int length) throws IOException {
        int[] ints = new int[length];
        for (int i = 0; i < length; i++) {
            ints[i] = readInt(stream);
        }
        return ints;
    }

    public static short[] readShorts(DataInputStream stream, int length) throws IOException {
        short[] shorts = new short[length];
        for (int i = 0; i < length; i++) {
            shorts[i] = readShort(stream);
        }
        return shorts;
    }

    public static void writeByte(DataOutputStream stream, byte value) throws IOException {
        stream.write(value);
    }

    public static void writeBoolean(DataOutputStream stream, boolean value) throws IOException {
        stream.write((byte) (value ? 1 : 0));
    }

    public static void writeShort(DataOutputStream stream, short value) throws IOException {
        short swaped = swap(value);
        stream.writeShort(swaped);
    }

    public static void writeInt(DataOutputStream stream, int value) throws IOException {
        int swaped = swap(value);
        stream.writeInt(swaped);
    }

    public static void writeText(DataOutputStream stream, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeInt(stream, value.length());
        stream.write(bytes);
    }

    public static void writeFloat(DataOutputStream stream, float value) throws IOException {
        byte[] bytes = floatToBytesLE(value);
        stream.write(bytes);
        //byte[] bytes = floatToBytesLE(value);
        //float swaped = swap(value);
        //stream.writeFloat(swaped);
        //System.out.println(value + "::" + swaped);
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

    /*static byte[] floatToBytes(float value) {
        int intBits =  Float.floatToIntBits(value);
        return new byte[] {
                (byte) (intBits >> 24),
                (byte) (intBits >> 16),
                (byte) (intBits >> 8),
                (byte) (intBits) };
    }*/

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

    /*
     * Swapping byte orders of given numeric types
     */
    public static short swap(short x){
        return (short)((x << 8) | ((x >> 8) & 0xff));
    }

    public static char swap(char x){
        return (char)((x << 8) | ((x >> 8) & 0xff));
    }

    public static int swap(int x){
        return (int)((swap((short)x) << 16) | (swap((short)(x >> 16)) & 0xffff));
    }

    public static long swap(long x){
        return (long)(((long)swap((int)(x)) << 32) | ((long)swap((int)(x >> 32)) & 0xffffffffL));
    }

    public static float swap(float x){
        return Float.intBitsToFloat(swap(Float.floatToRawIntBits(x)));
    }

    /*public static float swap(float x) {
        byte[] bytes = floatToBytesLE(x);

    }*/


    public static double swap(double x){
        return Double.longBitsToDouble(swap(Double.doubleToRawLongBits(x)));
    }




    public static byte[] floatToBytesLE(float value) {
        byte[] bytes = new byte[4];
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putFloat(value);
        buffer.flip();
        buffer.get(bytes);
        return bytes;
    }
}
