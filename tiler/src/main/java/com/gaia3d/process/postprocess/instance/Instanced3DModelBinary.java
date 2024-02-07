package com.gaia3d.process.postprocess.instance;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
public class Instanced3DModelBinary {
    private float[] positions;
    private float[] normalUps;
    private float[] normalRights;
    private float[] scales;

    public byte[] getPositionBytes() {
        byte[] positionsBytes = new byte[positions.length * 4];
        for (int i = 0; i < positions.length; i++) {
            int intBits = Float.floatToIntBits(positions[i]);
            positionsBytes[i * 4] = (byte) (intBits & 0xff);
            positionsBytes[i * 4 + 1] = (byte) ((intBits >> 8) & 0xff);
            positionsBytes[i * 4 + 2] = (byte) ((intBits >> 16) & 0xff);
            positionsBytes[i * 4 + 3] = (byte) ((intBits >> 24) & 0xff);
        }
        return positionsBytes;
    }

    public byte[] getNormalUpBytes() {
        byte[] normalUpsBytes = new byte[normalUps.length * 4];
        for (int i = 0; i < normalUps.length; i++) {
            int intBits = Float.floatToIntBits(normalUps[i]);
            normalUpsBytes[i * 4] = (byte) (intBits & 0xff);
            normalUpsBytes[i * 4 + 1] = (byte) ((intBits >> 8) & 0xff);
            normalUpsBytes[i * 4 + 2] = (byte) ((intBits >> 16) & 0xff);
            normalUpsBytes[i * 4 + 3] = (byte) ((intBits >> 24) & 0xff);
        }
        return normalUpsBytes;
    }

    public byte[] getNormalRightBytes() {
        byte[] normalRightsBytes = new byte[normalRights.length * 4];
        for (int i = 0; i < normalRights.length; i++) {
            int intBits = Float.floatToIntBits(normalRights[i]);
            normalRightsBytes[i * 4] = (byte) (intBits & 0xff);
            normalRightsBytes[i * 4 + 1] = (byte) ((intBits >> 8) & 0xff);
            normalRightsBytes[i * 4 + 2] = (byte) ((intBits >> 16) & 0xff);
            normalRightsBytes[i * 4 + 3] = (byte) ((intBits >> 24) & 0xff);
        }
        return normalRightsBytes;
    }

    public byte[] getScaleBytes() {
        byte[] scalesBytes = new byte[scales.length * 4];
        for (int i = 0; i < scales.length; i++) {
            int intBits = Float.floatToIntBits(scales[i]);
            scalesBytes[i * 4] = (byte) (intBits & 0xff);
            scalesBytes[i * 4 + 1] = (byte) ((intBits >> 8) & 0xff);
            scalesBytes[i * 4 + 2] = (byte) ((intBits >> 16) & 0xff);
            scalesBytes[i * 4 + 3] = (byte) ((intBits >> 24) & 0xff);
        }
        return scalesBytes;
    }
}
