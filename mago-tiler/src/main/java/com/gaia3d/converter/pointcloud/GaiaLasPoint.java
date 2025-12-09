package com.gaia3d.converter.pointcloud;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

import java.io.File;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Getter
@Setter
@Builder
public class GaiaLasPoint {
    public static final int BYTES_SIZE = 32;
    private double x, y, z; // 24 bytes
    private byte r, g, b, a; // 4 bytes
    private char intensity; // 2 bytes
    private short classification; // 2 bytes

    public void setPosition(double[] position) {
        this.x = position[0];
        this.y = position[1];
        this.z = position[2];
    }

    public double[] getPosition() {
        return new double[] {x, y, z};
    }

    public Vector3d getVec3Position() {
        return new Vector3d(x, y, z);
    }

    public void setRgb(byte[] rgb) {
        this.r = rgb[0];
        this.g = rgb[1];
        this.b = rgb[2];
    }

    public byte[] getRgb() {
        return new byte[] {r, g, b};
    }

    public byte[] getRgba() {
        return new byte[] {r, g, b, a};
    }

    private static final ThreadLocal<ByteBuffer> DIRECT_BUFFER = ThreadLocal.withInitial(() ->
        ByteBuffer.allocateDirect(32).order(ByteOrder.BIG_ENDIAN)
    );

    // ByteBuffer.allocateDirect(32)
    public byte[] toBytes() {
        ByteBuffer buf = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN);
        //ByteBuffer buf = DIRECT_BUFFER.get();

        buf.putDouble(x);
        buf.putDouble(y);
        buf.putDouble(z);

        buf.put(r);
        buf.put(g);
        buf.put(b);
        buf.put(a);

        buf.putChar(intensity);
        buf.putShort(classification);

        return buf.array();
    }

    @Deprecated
    public byte[] toBytesOld() {
        byte[] bytes = new byte[32];
        int index = 0;
        byte[] xBytes = BigEndianByteUtils.fromDouble(x);
        System.arraycopy(xBytes, 0, bytes, index, xBytes.length);
        index += xBytes.length;
        byte[] yBytes = BigEndianByteUtils.fromDouble(y);
        System.arraycopy(yBytes, 0, bytes, index, yBytes.length);
        index += yBytes.length;
        byte[] zBytes = BigEndianByteUtils.fromDouble(z);
        System.arraycopy(zBytes, 0, bytes, index, zBytes.length);
        index += zBytes.length;
        bytes[index++] = r;
        bytes[index++] = g;
        bytes[index++] = b;
        bytes[index++] = a;
        byte[] intensityBytes = BigEndianByteUtils.fromChar(intensity);
        System.arraycopy(intensityBytes, 0, bytes, index, intensityBytes.length);
        index += intensityBytes.length;
        byte[] classificationBytes = BigEndianByteUtils.fromShort(classification);
        System.arraycopy(classificationBytes, 0, bytes, index, classificationBytes.length);
        return bytes;
    }

    public static GaiaLasPoint fromBytes(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        //ByteBuffer buf = DIRECT_BUFFER.get();
        //buf.clear();
        //buf.put(bytes);

        double x = buf.getDouble();
        double y = buf.getDouble();
        double z = buf.getDouble();

        byte r = buf.get();
        byte g = buf.get();
        byte b = buf.get();
        byte a = buf.get();

        char intensity = buf.getChar();
        short classification = buf.getShort();

        return GaiaLasPoint.builder()
                .x(x)
                .y(y)
                .z(z)
                .r(r)
                .g(g)
                .b(b)
                .a(a)
                .intensity(intensity)
                .classification(classification)
                .build();
    }

    @Deprecated
    public static GaiaLasPoint fromBytesOld(byte[] bytes) {
        int index = 0;
        byte[] positionBytes = new byte[24];
        System.arraycopy(bytes, index, positionBytes, 0, positionBytes.length);
        double[] position = BigEndianByteUtils.toDoublesNew(positionBytes);
        index += positionBytes.length;
        byte[] rgb = new byte[4];
        System.arraycopy(bytes, index, rgb, 0, rgb.length);
        index += rgb.length;
        byte[] intensityBytes = new byte[2];
        System.arraycopy(bytes, index, intensityBytes, 0, intensityBytes.length);
        char intensity = BigEndianByteUtils.toChar(intensityBytes, 0);
        index += intensityBytes.length;
        byte[] classificationBytes = new byte[2];
        System.arraycopy(bytes, index, classificationBytes, 0, classificationBytes.length);
        short classification = BigEndianByteUtils.toShort(classificationBytes, 0);
        return GaiaLasPoint.builder()
                .x(position[0])
                .y(position[1])
                .z(position[2])
                .r(rgb[0])
                .g(rgb[1])
                .b(rgb[2])
                .a(rgb[3])
                .intensity(intensity)
                .classification(classification)
                .build();
    }
}
