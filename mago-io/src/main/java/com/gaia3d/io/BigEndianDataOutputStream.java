package com.gaia3d.io;

import org.joml.Vector4d;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * BigEndianDataOutputStream
 */
public class BigEndianDataOutputStream extends DataOutputStream implements DataOutput {

    public BigEndianDataOutputStream(OutputStream out) {
        super(out);
    }

    public void writeShorts(short[] v) throws IOException {
        for (short s : v) {
            writeShort(s);
        }
    }

    public void writeInts(int[] v) throws IOException {
        for (int i : v) {
            writeInt(i);
        }
    }

    public void writeFloats(float[] v) throws IOException {
        for (float f : v) {
            writeFloat(f);
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

    public void writeIntAndUTF(String s) throws IOException {
        writeInt(s.length());
        write(s.getBytes(StandardCharsets.UTF_8));
    }

    public void writePureText(String s) throws IOException {
        write(s.getBytes(StandardCharsets.UTF_8));
    }
}
