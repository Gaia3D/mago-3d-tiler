package com.gaia3d.util.io;

import org.joml.Vector4d;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * BigEndianDataInputStream
 * @author znkim
 * @since 1.0.0
 * @see FilterInputStream
 */
public class BigEndianDataInputStream extends DataInputStream implements DataInput {
    /**
     * Creates a <code>FilterInputStream</code>
     * by assigning the  argument <code>in</code>
     * to the field <code>this.in</code> so as
     * to remember it for later use.
     *
     * @param in the underlying input stream, or <code>null</code> if
     *           this instance is to be created without an underlying stream.
     */
    public BigEndianDataInputStream(InputStream in) {
        super(in);
    }

    public byte[] readBytes(int count) throws IOException {
        byte[] bytes = new byte[count];
        for (int i = 0; i < count; i++) {
            bytes[i] = readByte();
        }
        return bytes;
    }

    public short[] readShorts(int count) throws IOException {
        short[] shorts = new short[count];
        for (int i = 0; i < count; i++) {
            shorts[i] = readShort();
        }
        return shorts;
    }

    public int[] readInts(int count) throws IOException {
        int[] ints = new int[count];
        for (int i = 0; i < count; i++) {
            ints[i] = readInt();
        }
        return ints;
    }

    public float[] readFloats(int count) throws IOException {
        float[] floats = new float[count];
        for (int i = 0; i < count; i++) {
            floats[i] = readFloat();
        }
        return floats;
    }

    /*@Override
    public String readLine() throws IOException {
        return readUTF();
    }*/

    public String readText() throws IOException {
        return readUTF();
    }

    public Vector4d readVector4() throws IOException {
        return new Vector4d(readFloat(), readFloat(), readFloat(), readFloat());
    }

    public String readIntAndUTF() throws IOException {
        int length = readInt();
        byte[] bytes = new byte[length];
        if (read(bytes) != length) {
            throw new EOFException();
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public String readIntAndUTF(int length) throws IOException {
        byte[] bytes = new byte[length];
        if (read(bytes) != length) {
            throw new EOFException();
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
