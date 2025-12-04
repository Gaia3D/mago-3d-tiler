package com.gaia3d.converter.pointcloud;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Slf4j
public class BigEndianByteUtils {

    public static byte[] fromInt(int value) {
        return new byte[]{
                (byte) ((value >> 24) & 0xFF),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
    }

    public static byte[] fromInts(int[] values) {
        byte[] bytes = new byte[values.length * 4];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);
        for (int value : values) {
            buffer.putInt(value);
        }
        return bytes;
    }

    public static int toInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) |
               ((bytes[offset + 1] & 0xFF) << 16) |
               ((bytes[offset + 2] & 0xFF) << 8) |
               ((bytes[offset + 3] & 0xFF));
    }

    public static byte[] fromShort(short value) {
        return new byte[]{
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
    }

    public static byte[] fromShorts(short[] values) {
        byte[] bytes = new byte[values.length * 2];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);
        for (short value : values) {
            buffer.putShort(value);
        }
        return bytes;
    }

    public static short toShort(byte[] bytes, int offset) {
        return (short) (((bytes[offset] & 0xFF) << 8) |
                        ((bytes[offset + 1] & 0xFF)));
    }

    public static byte[] fromChar(char value) {
        return new byte[]{
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
    }

    public static byte[] fromChars(char[] values) {
        byte[] bytes = new byte[values.length * 2];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);
        for (char value : values) {
            buffer.putChar(value);
        }
        return bytes;
    }

    public static char toChar(byte[] bytes, int offset) {
        return (char) (((bytes[offset] & 0xFF) << 8) |
                        ((bytes[offset + 1] & 0xFF)));
    }

    public static byte[] fromLong(long value) {
        return new byte[]{
                (byte) ((value >> 56) & 0xFF),
                (byte) ((value >> 48) & 0xFF),
                (byte) ((value >> 40) & 0xFF),
                (byte) ((value >> 32) & 0xFF),
                (byte) ((value >> 24) & 0xFF),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
    }

    public static byte[] fromLongs(long[] values) {
        byte[] bytes = new byte[values.length * 8];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);
        for (long value : values) {
            buffer.putLong(value);
        }
        return bytes;
    }

    public static long toLong(byte[] bytes, int offset) {
        return ((long)(bytes[offset] & 0xFF) << 56) |
               ((long)(bytes[offset + 1] & 0xFF) << 48) |
               ((long)(bytes[offset + 2] & 0xFF) << 40) |
               ((long)(bytes[offset + 3] & 0xFF) << 32) |
               ((long)(bytes[offset + 4] & 0xFF) << 24) |
               ((long)(bytes[offset + 5] & 0xFF) << 16) |
               ((long)(bytes[offset + 6] & 0xFF) << 8) |
               ((long)(bytes[offset + 7] & 0xFF));
    }

    public static byte[] fromFloat(float value) {
        byte[] bytes = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putFloat(value);
        buffer.flip();
        buffer.get(bytes);
        buffer.clear();
        return bytes;
    }

    public static byte[] fromFloats(float[] values) {
        byte[] bytes = new byte[values.length * 4];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);
        for (float value : values) {
            buffer.putFloat(value);
        }
        return bytes;
    }

    public static float toFloat(byte[] bytes, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, 4);
        buffer.order(ByteOrder.BIG_ENDIAN);
        float result = buffer.getFloat();
        buffer.clear();
        return result;
    }

    public static byte[] fromDouble(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putDouble(value);
        buffer.flip();
        buffer.get(bytes);
        return bytes;
    }

    public static byte[] fromDoubles(double[] values) {
        byte[] bytes = new byte[values.length * 8];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);
        for (double value : values) {
            buffer.putDouble(value);
        }
        return bytes;
    }

    public static double toDoubleNew(byte[] bytes, int offset) {
        long longTemp =
                ((long) (bytes[offset]     & 0xFF) << 56) |
                ((long) (bytes[offset + 1] & 0xFF) << 48) |
                ((long) (bytes[offset + 2] & 0xFF) << 40) |
                ((long) (bytes[offset + 3] & 0xFF) << 32) |
                ((long) (bytes[offset + 4] & 0xFF) << 24) |
                ((long) (bytes[offset + 5] & 0xFF) << 16) |
                ((long) (bytes[offset + 6] & 0xFF) << 8)  |
                ((long) (bytes[offset + 7] & 0xFF));
        return Double.longBitsToDouble(longTemp);
    }

    public static double[] toDoublesNew(byte[] bytes) {
        int count = (bytes.length / 8);
        double[] result = new double[count];
        for (int i = 0; i < count; i++) {
            int offset = i * 8;
            long longTemp =
                    ((long) (bytes[offset]     & 0xFF) << 56) |
                    ((long) (bytes[offset + 1] & 0xFF) << 48) |
                    ((long) (bytes[offset + 2] & 0xFF) << 40) |
                    ((long) (bytes[offset + 3] & 0xFF) << 32) |
                    ((long) (bytes[offset + 4] & 0xFF) << 24) |
                    ((long) (bytes[offset + 5] & 0xFF) << 16) |
                    ((long) (bytes[offset + 6] & 0xFF) << 8)  |
                    ((long) (bytes[offset + 7] & 0xFF));
            result[i] = Double.longBitsToDouble(longTemp);
        }
        return result;
    }

    public static double toDouble(byte[] bytes, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, 8);
        buffer.order(ByteOrder.BIG_ENDIAN);
        double result = buffer.getDouble();
        buffer.clear();
        return result;
    }

    public static double[] toDoubles(byte[] bytes) {
        int count = (bytes.length / 8);
        double[] result = new double[count];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < count; i++) {
            result[i] = buffer.getDouble();
        }
        buffer.clear();
        return result;
    }
}
