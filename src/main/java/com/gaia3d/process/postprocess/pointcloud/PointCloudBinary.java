package com.gaia3d.process.postprocess.pointcloud;

import com.gaia3d.util.io.LittleEndianDataOutputStream;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

@Slf4j
@Setter
@Getter
public class PointCloudBinary {
    private float[] positions;

    public byte[] getBytes() {
        byte[] positionsBytes = new byte[positions.length * 4];
        // Convert float array to byte array(Big Endian)
        for (int i = 0; i < positions.length; i++) {
            int intBits = Float.floatToIntBits(positions[i]);
            positionsBytes[i * 4] = (byte) (intBits >> 24);
            positionsBytes[i * 4 + 1] = (byte) (intBits >> 16);
            positionsBytes[i * 4 + 2] = (byte) (intBits >> 8);
            positionsBytes[i * 4 + 3] = (byte) (intBits);
        }
        return positionsBytes;
    }

}
