package com.gaia3d.process.postprocess.batch;

import com.gaia3d.basic.geometry.GaiaRectangle;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.awt.image.BufferedImage;

@Getter
@Setter
@NoArgsConstructor
public class GaiaBatchImage {
    int originMaterialId;
    int materialId;
    GaiaRectangle originBoundary;
    GaiaRectangle batchedBoundary;
    // region of the source image the UVs actually reference, {x0, y0, x1, y1} in source pixels
    int[] sourceCrop;
    int sourceWidth;
    int sourceHeight;
    // source already cropped to sourceCrop and scaled to the planned size; null when the texture is read when drawing
    BufferedImage preparedImage;
}
