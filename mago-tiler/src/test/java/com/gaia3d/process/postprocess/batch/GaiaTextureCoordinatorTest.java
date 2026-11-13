package com.gaia3d.process.postprocess.batch;

import com.gaia3d.basic.exchangable.GaiaBuffer;
import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.basic.types.LevelOfDetail;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.joml.Vector4d;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("default")
class GaiaTextureCoordinatorTest {

    @Test
    void capsLeafAtlasHigherThanCoarserLods() {
        assertEquals(4096, GaiaTextureCoordinator.getBatchAtlasMaxSize(LevelOfDetail.NONE));
        assertEquals(4096, GaiaTextureCoordinator.getBatchAtlasMaxSize(LevelOfDetail.LOD0));
        assertEquals(2048, GaiaTextureCoordinator.getBatchAtlasMaxSize(LevelOfDetail.LOD1));
        assertEquals(2048, GaiaTextureCoordinator.getBatchAtlasMaxSize(LevelOfDetail.LOD4));
    }

    @Test
    void scalesSizesDownWithoutReachingZero() {
        assertArrayEquals(new int[]{512, 1, 250}, GaiaTextureCoordinator.scaleSizes(new int[]{1024, 1, 500}, 0.5));
    }

    @Test
    void cropsSourceToUsedUvRegionWithPadding() {
        List<float[]> texcoords = List.of(new float[]{0.25f, 0.5f, 0.5f, 0.5f}, new float[]{0.5f, 0.75f});
        assertArrayEquals(new int[]{252, 508, 516, 772}, GaiaTextureCoordinator.getUsedSourceRegion(texcoords, 1024, 1024, 4));
    }

    @Test
    void cropStaysInsideSourceAndNeverCollapses() {
        assertArrayEquals(new int[]{0, 0, 4, 1024}, GaiaTextureCoordinator.getUsedSourceRegion(List.<float[]>of(new float[]{0f, 0f, 0f, 1f}), 1024, 1024, 4));
        assertArrayEquals(new int[]{1023, 1023, 1024, 1024}, GaiaTextureCoordinator.getUsedSourceRegion(List.<float[]>of(new float[]{1f, 1f}), 1024, 1024, 0));
        assertArrayEquals(new int[]{0, 0, 64, 32}, GaiaTextureCoordinator.getUsedSourceRegion(List.of(), 64, 32, 4));
    }

    @Test
    void bakesMaterialColorIntoItsAtlasRegionOnly() {
        BufferedImage image = new BufferedImage(4, 2, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 2; y++) {
                image.setRGB(x, y, 0xFFC8C8C8);
            }
        }
        GaiaTextureCoordinator.tintRegion(image, 0, 0, 2, 2, new Vector4d(0.5, 1.0, 0.25, 1.0));
        assertEquals(0xFF64C832, image.getRGB(1, 1));
        assertEquals(0xFFC8C8C8, image.getRGB(2, 0));
        GaiaTextureCoordinator.tintRegion(image, 2, 0, 2, 2, new Vector4d(1.0, 1.0, 1.0, 1.0));
        assertEquals(0xFFC8C8C8, image.getRGB(3, 1));
    }

    @Test
    void texelBudgetFollowsGeometricErrorUnderTheLeafBudget() {
        assertEquals(200.0, GaiaTextureCoordinator.getTargetTexelDensity(LevelOfDetail.LOD0), 1e-9);
        assertEquals(144.0, GaiaTextureCoordinator.getTargetTexelDensity(LevelOfDetail.LOD1), 1e-9); // (1.5 * 16 / 2)^2
        assertEquals(36.0, GaiaTextureCoordinator.getTargetTexelDensity(LevelOfDetail.LOD2), 1e-9);
        assertEquals(9.0, GaiaTextureCoordinator.getTargetTexelDensity(LevelOfDetail.LOD3), 1e-9);
    }

    @Test
    void shrinksOnlySourcesDenserThanTheBudget() {
        assertEquals(0.5, GaiaTextureCoordinator.getDensityScale(800.0, 200.0), 1e-9);
        assertEquals(1.0, GaiaTextureCoordinator.getDensityScale(100.0, 200.0), 1e-9);
        assertEquals(1.0, GaiaTextureCoordinator.getDensityScale(Double.NaN, 200.0), 1e-9);
    }

    @Test
    void measuresTexelsPerSquareMeter() {
        // a 2 m x 1 m quad mapped to the left half of a 64 x 64 texture: 2048 texels over 2 m2
        GaiaBufferDataSet dataSet = new GaiaBufferDataSet();
        dataSet.getBuffers().put(AttributeType.POSITION, floatBuffer(new float[]{0, 0, 0, 2, 0, 0, 2, 1, 0, 0, 1, 0}));
        dataSet.getBuffers().put(AttributeType.TEXCOORD, floatBuffer(new float[]{0, 0, 0.5f, 0, 0.5f, 1, 0, 1}));
        GaiaBuffer indices = new GaiaBuffer();
        indices.setInts(new int[]{0, 1, 2, 0, 2, 3});
        dataSet.getBuffers().put(AttributeType.INDICE, indices);
        assertEquals(1024.0, GaiaTextureCoordinator.getTexelDensity(List.of(dataSet), 64, 64), 1e-6);
    }

    @Test
    void groupsSourcesIntoAtlasesThatFitTheCap() {
        // four 1024 squares fill one 2048 atlas past the 85% mark, so they need two
        int[] sizes = {1024, 1024, 1024, 1024};
        List<List<Integer>> groups = GaiaTextureCoordinator.groupForAtlases(sizes, sizes, 2048, 4);
        assertEquals(2, groups.size());
        assertEquals(List.of(0, 1, 2), groups.get(0));
        assertEquals(List.of(3), groups.get(1));
        // a single atlas when everything fits, and never more than the limit
        assertEquals(1, GaiaTextureCoordinator.groupForAtlases(new int[]{512, 512}, new int[]{512, 512}, 2048, 4).size());
        assertEquals(2, GaiaTextureCoordinator.groupForAtlases(new int[]{2048, 2048, 2048}, new int[]{2048, 2048, 2048}, 2048, 2).size());
    }

    private static GaiaBuffer floatBuffer(float[] values) {
        GaiaBuffer buffer = new GaiaBuffer();
        buffer.setFloats(values);
        return buffer;
    }

    @Test
    void wrapsTexcoordsOutsideUnitRangeLikeTheAtlasRemap() {
        assertEquals(0.25, GaiaTextureCoordinator.toUnitTexcoord(3.25f), 1e-6);
        assertEquals(1.0, GaiaTextureCoordinator.toUnitTexcoord(1.0f), 1e-6);
        assertEquals(0.0, GaiaTextureCoordinator.toUnitTexcoord(-0.5f), 1e-6);
    }
}
