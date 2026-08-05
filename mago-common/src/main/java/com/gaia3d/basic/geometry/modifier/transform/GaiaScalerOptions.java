package com.gaia3d.basic.geometry.modifier.transform;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GaiaScalerOptions {
    /* ratio of scaling */
    @Builder.Default
    private double scaleX = 1.0;
    @Builder.Default
    private double scaleY = 1.0;
    @Builder.Default
    private double scaleZ = 1.0;
}
