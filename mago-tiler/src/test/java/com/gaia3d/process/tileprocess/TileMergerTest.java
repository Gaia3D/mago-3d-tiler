package com.gaia3d.process.tileprocess;

import com.gaia3d.process.tileprocess.tile.tileset.node.BoundingVolume;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("default")
class TileMergerTest {

    @Test
    void convertsBoxBoundingVolumeToRegion() {
        BoundingVolume box = new BoundingVolume(BoundingVolume.BoundingVolumeType.BOX);
        box.setBox(new double[]{
                6378137.0, 0.0, 0.0,
                10.0, 0.0, 0.0,
                0.0, 10.0, 0.0,
                0.0, 0.0, 10.0
        });

        BoundingVolume region = TileMerger.toRegionBoundingVolume(box, null);

        assertNotNull(region);
        assertNotNull(region.getRegion());
        assertNull(region.getBox());
        assertTrue(region.getRegion()[0] <= region.getRegion()[2]);
        assertTrue(region.getRegion()[1] <= region.getRegion()[3]);
        assertTrue(region.getRegion()[4] <= region.getRegion()[5]);
    }

    @Test
    void convertsSphereBoundingVolumeToRegion() {
        BoundingVolume sphere = new BoundingVolume(BoundingVolume.BoundingVolumeType.SPHERE);
        sphere.setSphere(new double[]{6378137.0, 0.0, 0.0, 15.0});

        BoundingVolume region = TileMerger.toRegionBoundingVolume(sphere, null);

        assertNotNull(region);
        assertNotNull(region.getRegion());
        assertNull(region.getSphere());
        assertTrue(region.getRegion()[0] <= region.getRegion()[2]);
        assertTrue(region.getRegion()[1] <= region.getRegion()[3]);
        assertTrue(region.getRegion()[4] <= region.getRegion()[5]);
    }
}
