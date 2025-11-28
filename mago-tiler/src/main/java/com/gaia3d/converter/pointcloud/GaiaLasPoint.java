package com.gaia3d.converter.pointcloud;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

import java.io.File;
import java.io.Serializable;

@Getter
@Setter
@Builder
public class GaiaLasPoint {
    public static final int BYTES_SIZE = 32;
    private double[] position; // 24 bytes
    private byte[] rgb; // 4 bytes
    private char intensity; // 2 bytes
    private short classification; // 2 bytes

    public Vector3d getVec3Position() {
        return new Vector3d(position[0], position[1], position[2]);
    }

    public byte[] toBytes() {
        byte[] bytes = new byte[32];
        int index = 0;
        byte[] positionBytes = BigEndianByteUtils.fromDoubles(position);
        System.arraycopy(positionBytes, 0, bytes, index, positionBytes.length);
        index += positionBytes.length;
        System.arraycopy(rgb, 0, bytes, index, rgb.length);
        index += rgb.length;
        byte[] intensityBytes = BigEndianByteUtils.fromChar(intensity);
        System.arraycopy(intensityBytes, 0, bytes, index, intensityBytes.length);
        index += intensityBytes.length;
        byte[] classificationBytes = BigEndianByteUtils.fromShort(classification);
        System.arraycopy(classificationBytes, 0, bytes, index, classificationBytes.length);
        return bytes;
    }

    public static GaiaLasPoint fromBytes(byte[] bytes) {
        int index = 0;
        byte[] positionBytes = new byte[24];
        System.arraycopy(bytes, index, positionBytes, 0, positionBytes.length);
        double[] position = BigEndianByteUtils.toDoubles(positionBytes);
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
                .position(position)
                .rgb(rgb)
                .intensity(intensity)
                .classification(classification)
                .build();
    }
}
