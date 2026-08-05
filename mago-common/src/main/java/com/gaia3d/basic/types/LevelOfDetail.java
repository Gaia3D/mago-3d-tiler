package com.gaia3d.basic.types;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LevelOfDetail {
    NONE(-1, 0, 0, 1.0f, 1.0f, new float[]{1.0f, 1.0f, 1.0f}),
    LOD0(0, 0, 0, 1.0f, 1.0f, new float[]{1.0f, 0.4f, 0.4f}),
    LOD1(1, 2, 8, 0.5f, 0.8f, new float[]{0.4f, 1.0f, 0.4f}),
    LOD2(2, 4, 50, 0.25f, 0.6f, new float[]{0.4f, 0.4f, 1.0f}),
    LOD3(3, 8, 120, 0.125f, 0.4f, new float[]{1.0f, 1.0f, 0.4f}),
    LOD4(4, 16, 180, 0.0625f, 0.2f, new float[]{0.4f, 1.0f, 1.0f}),
    LOD5(5, 32, 240, 0.03125f, 0.1f, new float[]{1.0f, 0.4f, 1.0f}),
    LOD6(6, 64, 300, 0.01625f, 0.05f, new float[]{1.0f, 1.0f, 1.0f}),
    LOD7(7, 128, 360, 0.008125f, 0.05f, new float[]{0.4f, 0.4f, 0.4f}),
    LOD8(8, 256, 420, 0.0040625f, 0.05f, new float[]{0.4f, 0.4f, 0.4f}),
    LOD9(9, 512, 480, 0.00203125f, 0.05f, new float[]{0.4f, 0.4f, 0.4f}),
    LOD10(10, 1024, 540, 0.001015625f, 0.05f, new float[]{0.4f, 0.4f, 0.4f}),
    LOD11(11, 2048, 600, 0.0005078125f, 0.05f, new float[]{0.4f, 0.4f, 0.4f}),
    LOD12(12, 4096, 660, 0.00025390625f, 0.05f, new float[]{0.4f, 0.4f, 0.4f}),
    LOD13(13, 8192, 720, 0.000126953125f, 0.05f, new float[]{0.4f, 0.4f, 0.4f}),
    LOD14(14, 16384, 780, 0.0000634765625f, 0.05f, new float[]{0.4f, 0.4f, 0.4f}),
    LOD15(15, 32768, 840, 0.00003173828125f, 0.05f, new float[]{0.4f, 0.4f, 0.4f}),
    LOD16(16, 65536, 900, 0.000015869140625f, 0.05f, new float[]{0.4f, 0.4f, 0.4f}),
    LOD17(17, 131072, 960, 0.0000079345703125f, 0.05f, new float[]{0.4f, 0.4f, 0.4f}),
    LOD18(18, 262144, 1020, 0.00000396728515625f, 0.05f, new float[]{0.4f, 0.4f, 0.4f}),
    LOD19(19, 524288, 1080, 0.000001983642578125f, 0.05f, new float[]{0.4f, 0.4f, 0.4f}),
    LOD20(20, 1048576, 1140, 0.0000009918212890625f, 0.005f, new float[]{0.4f, 0.4f, 0.4f}),
    LOD21(21, 2097152, 1200, 0.00000049591064453125f, 0.005f, new float[]{0.4f, 0.4f, 0.4f}),
    LOD22(22, 4194304, 1260, 0.000000247955322265625f, 0.005f, new float[]{0.4f, 0.4f, 0.4f}),
    LOD23(23, 8388608, 1320, 0.0000001239776611328125f, 0.005f, new float[]{0.4f, 0.4f, 0.4f});

    final int level;
    final int geometricError;
    final int geometricErrorBlock;
    final float textureScale;
    final float realisticScale;
    final float[] debugColor;

    public static LevelOfDetail getByLevel(int level) {
        for (LevelOfDetail lod : LevelOfDetail.values()) {
            if (lod.getLevel() == level) {
                return lod;
            }
        }
        return NONE;
    }
}