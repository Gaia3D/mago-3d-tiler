package com.gaia3d.process.postprocess.pointcloud;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
public class PointCloudBinary {
    //private double[] positions;
    private int[] positions;
    private byte[] colors;
    private float[] batchIds;
    private short[] normals;

    // int -> unsigned short
    public byte[] getPositionBytes() {
        byte[] positionsBytes = new byte[positions.length * 2];
        // Convert double array to byte array(Little Endian)
        for (int i = 0; i < positions.length; i++) {
            int intBits = positions[i];
            short shortBits = toUnsignedShort(intBits);
            positionsBytes[i * 2] = (byte) (shortBits & 0xff);
            positionsBytes[i * 2 + 1] = (byte) ((shortBits >> 8) & 0xff);
        }
        return positionsBytes;
    }

    /*public byte[] getPositionBytes() {
        byte[] positionsBytes = new byte[positions.length * 4];
        // Convert double array to byte array(Little Endian)
        for (int i = 0; i < positions.length; i++) {
            int intBits = Float.floatToIntBits(positions[i]);
            positionsBytes[i * 4] = (byte) (intBits & 0xff);
            positionsBytes[i * 4 + 1] = (byte) ((intBits >> 8) & 0xff);
            positionsBytes[i * 4 + 2] = (byte) ((intBits >> 16) & 0xff);
            positionsBytes[i * 4 + 3] = (byte) ((intBits >> 24) & 0xff);
        }
        return positionsBytes;
    }*/

    public byte[] getBatchIdBytes() {
        byte[] batchIdsBytes = new byte[batchIds.length * 4];
        // Convert float array to byte array(Little Endian)
        for (int i = 0; i < batchIds.length; i++) {
            int intBits = Float.floatToIntBits(batchIds[i]);
            batchIdsBytes[i * 4] = (byte) (intBits & 0xff);
            batchIdsBytes[i * 4 + 1] = (byte) ((intBits >> 8) & 0xff);
            batchIdsBytes[i * 4 + 2] = (byte) ((intBits >> 16) & 0xff);
            batchIdsBytes[i * 4 + 3] = (byte) ((intBits >> 24) & 0xff);
        }
        return batchIdsBytes;
    }

    public byte[] getColorBytes() {
        return colors;
    }

    private short toUnsignedShort(int value) {
        if (value < 0 || value > 65535) {
            throw new IllegalArgumentException("Value out of range for unsigned short: " + value);
        }
        if (value <= 32767) {
            return (short) value;
        } else {
            return (short) (value - 65536);
        }
    }
}
