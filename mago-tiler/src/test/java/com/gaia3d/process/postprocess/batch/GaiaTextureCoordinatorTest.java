package com.gaia3d.process.postprocess.batch;

import com.gaia3d.basic.types.LevelOfDetail;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("default")
class GaiaTextureCoordinatorTest {

    @Test
    void capsLeafAtlasHigherThanCoarserLods() {
        assertEquals(2048, GaiaTextureCoordinator.getBatchAtlasMaxSize(LevelOfDetail.NONE));
        assertEquals(2048, GaiaTextureCoordinator.getBatchAtlasMaxSize(LevelOfDetail.LOD0));
        assertEquals(1024, GaiaTextureCoordinator.getBatchAtlasMaxSize(LevelOfDetail.LOD1));
        assertEquals(1024, GaiaTextureCoordinator.getBatchAtlasMaxSize(LevelOfDetail.LOD4));
    }

    @Test
    void scalesSizesDownWithoutReachingZero() {
        assertArrayEquals(new int[]{512, 1, 250}, GaiaTextureCoordinator.scaleSizes(new int[]{1024, 1, 500}, 0.5));
    }
}
