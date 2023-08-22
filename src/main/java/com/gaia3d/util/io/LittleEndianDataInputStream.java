package com.gaia3d.util.io;

import org.joml.Vector4d;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * LittleEndianDataInputStream
 * @author znkim
 * @since 1.0.0
 * @see FilterInputStream
 */
public class LittleEndianDataInputStream extends FilterInputStream implements DataInput {
    /**
     * Creates a <code>FilterInputStream</code>
     * by assigning the  argument <code>in</code>
     * to the field <code>this.in</code> so as
     * to remember it for later use.
     *
     * @param in the underlying input stream, or <code>null</code> if
     *           this instance is to be created without an underlying stream.
     */
    public LittleEndianDataInputStream(InputStream in) {
        super(in);
    }

    @Override
    public void readFully(byte[] b) throws IOException {

    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {

    }

    @Override
    public int skipBytes(int n) throws IOException {
        return 0;
    }

    @Override
    public boolean readBoolean() throws IOException {
        int b = in.read();
        if (b == -1)
            throw new EOFException();
        return b != 0;
    }

    @Override
    public byte readByte() throws IOException {
        int b = in.read();
        if (b == -1)
            throw new EOFException();
        return (byte) b;
    }

    public byte[] readBytes(int count) throws IOException {
        byte[] bytes = new byte[count];
        for (int i = 0; i < count; i++) {
            bytes[i] = readByte();
        }
        return bytes;
    }

    @Override
    public int readUnsignedByte() throws IOException {
        int b = in.read();
        if (b == -1)
            throw new EOFException();
        return (byte) b;
    }

    @Override
    public short readShort() throws IOException {
        int b1 = in.read();
        int b2 = in.read();
        if ((b1 | b2) < 0)
            throw new EOFException();
        return (short) ((b2 << 8) + b1);
    }

    public short[] readShorts(int count) throws IOException {
        short[] shorts = new short[count];
        for (int i = 0; i < count; i++) {
            shorts[i] = readShort();
        }
        return shorts;
    }

    @Override
    public int readUnsignedShort() throws IOException {
        int b1 = in.read();
        int b2 = in.read();
        if ((b1 | b2) < 0)
            throw new EOFException();
        return (short) ((b2 << 8) + b1);
    }

    @Override
    public char readChar() throws IOException {
        int b1 = in.read();
        int b2 = in.read();
        if ((b1 | b2) < 0)
            throw new EOFException();
        return (char) ((b2 << 8) + b1);
    }

    @Override
    public int readInt() throws IOException {
        int b1 = in.read();
        int b2 = in.read();
        int b3 = in.read();
        int b4 = in.read();
        if ((b1 | b2 | b3 | b4) < 0)
            throw new EOFException();
        return (b4 << 24) + (b3 << 16) + (b2 << 8) + b1;
    }

    public int[] readInts(int count) throws IOException {
        int[] ints = new int[count];
        for (int i = 0; i < count; i++) {
            ints[i] = readInt();
        }
        return ints;
    }

    @Override
    public long readLong() throws IOException {
        int[] b = new int[8];
        for (int i = 0; i < 8; i++) {
            b[i] = in.read();
        }
        if (b[0] < 0)
            throw new EOFException();
        return (((long) b[0] << 56) +
                ((long) (b[1] & 255) << 48) +
                ((long) (b[2] & 255) << 40) +
                ((long) (b[3] & 255) << 32) +
                ((long) (b[4] & 255) << 24) +
                ((b[5] & 255) << 16) +
                ((b[6] & 255) <<  8) +
                ((b[7] & 255)));
    }

    @Override
    public float readFloat() throws IOException {
        int[] b = new int[4];
        for (int i = 0; i < 4; i++) {
            b[i] = in.read();
        }
        if (b[0] < 0)
            throw new EOFException();

        byte[] bytes = bytesToInts(b);
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    public float[] readFloats(int count) throws IOException {
        float[] floats = new float[count];
        for (int i = 0; i < count; i++) {
            floats[i] = readFloat();
        }
        return floats;
    }

    @Override
    public double readDouble() throws IOException {
        int[] b = new int[8];
        for (int i = 0; i < 8; i++) {
            b[i] = in.read();
        }
        if (b[0] < 0)
            throw new EOFException();
        byte[] bytes = bytesToInts(b);
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    @Override
    public String readLine() throws IOException {
        return readUTF();
    }

    public String readText() throws IOException {
        return readUTF();
    }

    public Vector4d readVector4() throws IOException {
        return new Vector4d(readFloat(), readFloat(), readFloat(), readFloat());
    }

    @Override
    public String readUTF() throws IOException {
        int length = readInt();
        byte[] bytes = new byte[length];
        read(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public String readUTF(int length) throws IOException {
        byte[] bytes = new byte[length];
        read(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public byte[] bytesToInts(int[] value) {
        byte[] bytes = new byte[4];
        for (int i = 0; i < value.length; i++) {
            bytes[i] = (byte) value[i];
        }
        return bytes;
    }
}
