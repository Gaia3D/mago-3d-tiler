package com.gaia3d.util.io;

import org.joml.Vector4d;

import java.io.DataOutput;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * LittleEndianDataOutputStream
 */
public class LittleEndianDataOutputStream extends FilterOutputStream implements DataOutput {

    public LittleEndianDataOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        out.write(v ? 1 : 0);
    }

    @Override
    public void writeByte(int v) throws IOException {
        out.write(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        out.write(v & 0xFF);
        out.write((v >>> 8) & 0xFF);
    }

    public void writeShorts(short[] v) throws IOException {
        for (short s : v) {
            writeShort(s);
        }
    }

    @Override
    public void writeChar(int v) throws IOException {
        out.write(v & 0xFF);
        out.write((v >>> 8) & 0xFF);
    }

    @Override
    public void writeInt(int v) throws IOException {
        out.write(v & 0xFF);
        out.write((v >>>  8) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 24) & 0xFF);
    }

    public void writeInts(int[] v) throws IOException {
        for (int i : v) {
            writeInt(i);
        }
    }

    private final byte[] writeBuffer = new byte[8];
    @Override
    public void writeLong(long v) throws IOException {
        writeBuffer[0] = (byte)(v);
        writeBuffer[1] = (byte)(v >>>  8);
        writeBuffer[2] = (byte)(v >>> 16);
        writeBuffer[3] = (byte)(v >>> 24);
        writeBuffer[4] = (byte)(v >>> 32);
        writeBuffer[5] = (byte)(v >>> 40);
        writeBuffer[6] = (byte)(v >>> 48);
        writeBuffer[7] = (byte)(v >>> 56);
        out.write(writeBuffer, 0, 8);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        //writeInt(Float.floatToIntBits(v));
        byte[] bytes = floatToBytes(v);
        write(bytes);
        bytes = null;
    }

    public void writeFloats(float[] v) throws IOException {
        for (float f : v) {
            writeFloat(f);
        }
    }

    @Override
    public void writeDouble(double v) throws IOException {
        byte[] bytes = doubleToBytes(v);
        write(bytes);
        bytes = null;
    }

    @Override
    public void writeBytes(String s) throws IOException {
        out.write(s.getBytes());
    }

    @Override
    public void writeChars(String s) throws IOException {
        //out.write(s.getBytes());
        int len = s.length();
        for (int i = 0 ; i < len ; i++) {
            int v = s.charAt(i);
            out.write((v) & 0xFF);
            out.write((v >>> 8) & 0xFF);
        }
    }

    public void writeText(String s) throws IOException {
        writeUTF(s);
    }

    public void writeVector4(Vector4d values) throws IOException {
        float r = (float) values.x();
        float g = (float) values.y();
        float b = (float) values.z();
        float a = (float) values.w();
        writeFloat(r);
        writeFloat(g);
        writeFloat(b);
        writeFloat(a);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeInt(s.length());
        write(bytes);
    }

    public void writePureText(String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        write(bytes);
    }

    // The Soulution for Float NaN Error
    private byte[] floatToBytes(float value) {
        byte[] bytes = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        //ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putFloat(value);
        buffer.flip();
        buffer.get(bytes);
        buffer.clear();
        return bytes;
    }

    // The Soulution for Double NaN Error
    private byte[] doubleToBytes(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        //ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(value);
        buffer.flip();
        buffer.get(bytes);
        return bytes;
    }
}
