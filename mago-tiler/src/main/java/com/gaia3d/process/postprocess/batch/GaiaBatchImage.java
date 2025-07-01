package com.gaia3d.process.postprocess.batch;

import com.gaia3d.basic.geometry.GaiaRectangle;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GaiaBatchImage {
    int originMaterialId;
    int materialId;
    GaiaRectangle originBoundary;
    GaiaRectangle batchedBoundary;
}
