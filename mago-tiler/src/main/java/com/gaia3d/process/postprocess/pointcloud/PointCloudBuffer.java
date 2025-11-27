package com.gaia3d.process.postprocess.pointcloud;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
public class PointCloudBuffer {
    //private double[] positions;

    private int[] quantizedPositions; // Quantized positions as integers
    private float[] positions;
    private byte[] colors;
    private float[] batchIds;
    private short[] normals;

    private char[] intensities;
    private short[] classifications;

    // int -> unsigned short
    public byte[] getQuantizedPositionBytes() {
        byte[] positionsBytes = new byte[quantizedPositions.length * 2];
        // Convert double array to byte array(Little Endian)
        for (int i = 0; i < quantizedPositions.length; i++) {
            int intBits = quantizedPositions[i];
            short shortBits = toUnsignedShort(intBits);
            positionsBytes[i * 2] = (byte) (shortBits & 0xff);
            positionsBytes[i * 2 + 1] = (byte) ((shortBits >> 8) & 0xff);
        }
        return positionsBytes;
    }

    public byte[] getPositionBytes() {
        byte[] positionsBytes = new byte[positions.length * 4];
        // Convert float array to byte array(Little Endian)
        for (int i = 0; i < positions.length; i++) {
            int intBits = Float.floatToIntBits(positions[i]);
            positionsBytes[i * 4] = (byte) (intBits & 0xff);
            positionsBytes[i * 4 + 1] = (byte) ((intBits >> 8) & 0xff);
            positionsBytes[i * 4 + 2] = (byte) ((intBits >> 16) & 0xff);
            positionsBytes[i * 4 + 3] = (byte) ((intBits >> 24) & 0xff);
        }
        return positionsBytes;
    }

    public byte[] getBatchIdBytes() {
        byte[] batchIdBytes = new byte[batchIds.length * 4];
        // Convert float array to byte array(Little Endian)
        for (int i = 0; i < batchIds.length; i++) {
            int intBits = Float.floatToIntBits(batchIds[i]);
            batchIdBytes[i * 4] = (byte) (intBits & 0xff);
            batchIdBytes[i * 4 + 1] = (byte) ((intBits >> 8) & 0xff);
            batchIdBytes[i * 4 + 2] = (byte) ((intBits >> 16) & 0xff);
            batchIdBytes[i * 4 + 3] = (byte) ((intBits >> 24) & 0xff);
        }
        return batchIdBytes;
    }

    public byte[] getColorBytes() {
        return colors;
    }

    public byte[] getNormalBytes() {
        byte[] normalsBytes = new byte[normals.length * 2];
        // Convert short array to byte array(Little Endian)
        for (int i = 0; i < normals.length; i++) {
            short shortBits = normals[i];
            normalsBytes[i * 2] = (byte) (shortBits & 0xff);
            normalsBytes[i * 2 + 1] = (byte) ((shortBits >> 8) & 0xff);
        }
        return normalsBytes;
    }

    public byte[] getIntensityBytes() {
        byte[] intensitiesBytes = new byte[intensities.length * 2];
        // Convert short array to byte array(Little Endian)
        for (int i = 0; i < intensities.length; i++) {
            char shortBits = intensities[i];
            intensitiesBytes[i * 2 + 1] = (byte) (shortBits & 0xff);
            intensitiesBytes[i * 2] = (byte) ((shortBits >> 8) & 0xff);
        }
        return intensitiesBytes;
    }

    public byte[] getIntensityPadBytes() {
        // Assuming intensities are padded to 2 bytes
        byte[] intensitiesBytes = new byte[intensities.length * 4];
        for (int i = 0; i < intensities.length; i++) {
            char shortBits = intensities[i];
            intensitiesBytes[i * 4 + 1] = (byte) (shortBits & 0xff);
            intensitiesBytes[i * 4] = (byte) ((shortBits >> 8) & 0xff);
            intensitiesBytes[i * 4 + 2] = 0; // Padding byte
            intensitiesBytes[i * 4 + 3] = 0; // Padding byte
        }
        return intensitiesBytes;
    }

    public byte[] getClassificationBytes() {
        byte[] classificationsBytes = new byte[classifications.length * 2];
        // Convert short array to byte array(Little Endian)
        for (int i = 0; i < classifications.length; i++) {
            short shortBits = classifications[i];
            classificationsBytes[i * 2 + 1] = (byte) (shortBits & 0xff);
            classificationsBytes[i * 2] = (byte) ((shortBits >> 8) & 0xff);
        }
        return classificationsBytes;
    }

    public byte[] getClassificationPadBytes() {
        // Assuming classifications are padded to 2 bytes
        byte[] classificationsBytes = new byte[classifications.length * 4];
        for (int i = 0; i < classifications.length; i++) {
            short shortBits = classifications[i];
            classificationsBytes[i * 4 + 1] = (byte) (shortBits & 0xff);
            classificationsBytes[i * 4] = (byte) ((shortBits >> 8) & 0xff);
            classificationsBytes[i * 4 + 2] = 0; // Padding byte
            classificationsBytes[i * 4 + 3] = 0; // Padding byte
        }
        return classificationsBytes;
    }


    private short toUnsignedShort(int value) {
        if (value < 0 || value > 65535) {
            //value = value & 0xffff;
            throw new IllegalArgumentException("Value out of range for unsigned short: " + value);
        }
        if (value <= 32767) {
            return (short) value;
        } else {
            return (short) (value - 65536);
        }
    }
}
