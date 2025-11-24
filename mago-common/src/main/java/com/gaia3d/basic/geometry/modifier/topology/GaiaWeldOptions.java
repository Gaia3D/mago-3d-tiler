package com.gaia3d.basic.geometry.modifier.topology;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GaiaWeldOptions {
    @Builder.Default
    private double error = 0.0001;
    @Builder.Default
    private boolean checkTexCoord = true;
    @Builder.Default
    private boolean checkNormal = true;
    @Builder.Default
    private boolean checkColor = true;
    @Builder.Default
    private boolean checkBatchId = false;

    /*public GaiaWeldOptions(double error, boolean checkTexCoord, boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        this.error = error;
        this.checkTexCoord = checkTexCoord;
        this.checkNormal = checkNormal;
        this.checkColor = checkColor;
        this.checkBatchId = checkBatchId;
    }*/
}
