package com.gaia3d.process.tileprocess.tile;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LevelOfDetail {
    NONE(-1,0, 0, 1.0f, new float[]{1.0f, 1.0f, 1.0f}),
    LOD0(0,0,0, 1.0f, new float[]{1.0f, 0.4f, 0.4f}),
    LOD1(1, 4, 8, 0.5f, new float[]{0.4f, 1.0f, 0.4f}),
    LOD2(2, 16, 50, 0.25f, new float[]{0.4f, 0.4f, 1.0f}),
    LOD3(3, 32, 120, 0.125f, new float[]{1.0f, 1.0f, 0.4f}),
    LOD4(4, 64, 180, 0.0625f, new float[]{0.4f, 1.0f, 1.0f}),
    LOD5(5, 128, 240, 0.03125f, new float[]{1.0f, 0.4f, 1.0f}),
    LOD6(6, 256, 300, 0.01625f, new float[]{1.0f, 1.0f, 1.0f});

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
