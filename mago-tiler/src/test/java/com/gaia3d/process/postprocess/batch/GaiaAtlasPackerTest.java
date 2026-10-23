package com.gaia3d.process.postprocess.batch;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("default")
class GaiaAtlasPackerTest {

    @Test
    void packsEqualSquaresIntoASquare() {
        int[] sizes = new int[16];
        java.util.Arrays.fill(sizes, 256);

        GaiaAtlasPacker.Result result = GaiaAtlasPacker.pack(sizes, sizes);

        assertEquals(1024, result.width());
        assertEquals(1024, result.height());
        assertNoOverlap(sizes, sizes, result);
    }

    @Test
    void packsMixedSizesWithoutOverlapAndLittleWaste() {
        Random random = new Random(7);
        int count = 1000;
        int[] widths = new int[count];
        int[] heights = new int[count];
        long area = 0;
        for (int i = 0; i < count; i++) {
            widths[i] = 16 + random.nextInt(1009);
            heights[i] = 16 + random.nextInt(1009);
            area += (long) widths[i] * heights[i];
        }

        GaiaAtlasPacker.Result result = GaiaAtlasPacker.pack(widths, heights);

        assertNoOverlap(widths, heights, result);
        double occupancy = area / ((double) result.longestSide() * result.longestSide());
        assertTrue(occupancy > 0.85, "occupancy " + occupancy);
    }

    private void assertNoOverlap(int[] widths, int[] heights, GaiaAtlasPacker.Result result) {
        for (int i = 0; i < widths.length; i++) {
            assertTrue(result.xs()[i] >= 0 && result.ys()[i] >= 0);
            assertTrue(result.xs()[i] + widths[i] <= result.width() && result.ys()[i] + heights[i] <= result.height());
            for (int j = i + 1; j < widths.length; j++) {
                boolean overlap = result.xs()[i] < result.xs()[j] + widths[j] && result.xs()[j] < result.xs()[i] + widths[i]
                        && result.ys()[i] < result.ys()[j] + heights[j] && result.ys()[j] < result.ys()[i] + heights[i];
                assertFalse(overlap, "images " + i + " and " + j + " overlap");
            }
        }
    }
}
