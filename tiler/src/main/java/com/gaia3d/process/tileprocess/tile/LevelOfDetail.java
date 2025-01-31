package com.gaia3d.process.tileprocess.tile;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LevelOfDetail {
    NONE(-1,0, 0, 1.0f, new float[]{1.0f, 1.0f, 1.0f}),
    LOD0(0,0,0, 1.0f, new float[]{1.0f, 0.4f, 0.4f}),
    LOD1(1, 2, 8, 0.5f, new float[]{0.4f, 1.0f, 0.4f}),
    LOD2(2, 4, 50, 0.25f, new float[]{0.4f, 0.4f, 1.0f}),
    LOD3(3, 8, 120, 0.125f, new float[]{1.0f, 1.0f, 0.4f}),
    LOD4(4, 16, 180, 0.0625f, new float[]{0.4f, 1.0f, 1.0f}),
    LOD5(5, 32, 240, 0.03125f, new float[]{1.0f, 0.4f, 1.0f}),
    LOD6(6, 64, 300, 0.01625f, new float[]{1.0f, 1.0f, 1.0f}),
    LOD7(7, 128, 360, 0.008125f, new float[]{0.4f, 0.4f, 0.4f}),
    LOD8(8, 256, 420, 0.0040625f, new float[]{0.4f, 0.4f, 0.4f}),
    LOD9(9, 512, 480, 0.00203125f, new float[]{0.4f, 0.4f, 0.4f}),
    LOD10(10, 1024, 540, 0.001015625f, new float[]{0.4f, 0.4f, 0.4f});

    final int level;
    final int geometricError;
    final int geometricErrorBlock;
    final float textureScale;
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