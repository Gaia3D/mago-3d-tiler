package com.gaia3d.render;

import com.gaia3d.basic.halfedge.CameraDirectionType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("default")
class FaceVisibilityDataTest {

    @Test
    void preservesLargeFaceIdsAndClearsCounts() {
        FaceVisibilityData data = new FaceVisibilityData(CameraDirectionType.ZNEG);

        data.incrementPixelFaceVisibility(70_000);
        data.incrementPixelFaceVisibility(70_000);

        assertEquals(2, data.getPixelFaceVisibility(70_000));
        data.clearCounts();
        assertEquals(0, data.getPixelFaceVisibility(70_000));
    }
}
