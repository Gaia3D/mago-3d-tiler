package com.gaia3d.process.postprocess.batch;

import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.types.LevelOfDetail;
import org.joml.Vector2d;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("default")
class GaiaTextureCoordinatorTest {

    @Test
    void halvesAtlasCapPerLodDownToFloor() {
        assertEquals(4096, GaiaTextureCoordinator.getBatchAtlasMaxSize(LevelOfDetail.NONE));
        assertEquals(4096, GaiaTextureCoordinator.getBatchAtlasMaxSize(LevelOfDetail.LOD0));
        assertEquals(2048, GaiaTextureCoordinator.getBatchAtlasMaxSize(LevelOfDetail.LOD1));
        assertEquals(1024, GaiaTextureCoordinator.getBatchAtlasMaxSize(LevelOfDetail.LOD2));
        assertEquals(1024, GaiaTextureCoordinator.getBatchAtlasMaxSize(LevelOfDetail.LOD4));
    }

    @Test
    void keepsSourcesThatFitAndShrinksManySourcesToTheCap() {
        assertEquals(1.0, GaiaTextureCoordinator.getSourceScale(images(4, 1024), 4096));
        // 1000 x 1024^2 would pack to ~32k^2; each source must shrink to fit 4096^2 in total
        double scale = GaiaTextureCoordinator.getSourceScale(images(1000, 1024), 4096);
        assertEquals(4096.0 * 4096.0, 1000 * Math.pow(1024 * scale, 2), 1.0);
    }

    private List<GaiaBatchImage> images(int count, int size) {
        return Collections.nCopies(count, size).stream().map(s -> {
            GaiaBatchImage image = new GaiaBatchImage();
            image.setOriginBoundary(new GaiaRectangle(new Vector2d(0, 0), new Vector2d(s, s)));
            return image;
        }).toList();
    }
}
