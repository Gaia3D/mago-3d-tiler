package com.gaia3d.basic.halfedge;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DecimateParameters {
    private double maxDiffAngDegrees = 15.0;
    private double hedgeMinLength = 0.5;
    private double frontierMaxDiffAngDeg = 4.0;
    private double maxAspectRatio = 6.0;
    private int maxCollapsesCount = 1000000;

    public void setBasicValues(double maxDiffAngDegrees, double hedgeMinLength, double frontierMaxDiffAngDeg, double maxAspectRatio, int maxCollapsesCount) {
        this.maxDiffAngDegrees = maxDiffAngDegrees;
        this.hedgeMinLength = hedgeMinLength;
        this.frontierMaxDiffAngDeg = frontierMaxDiffAngDeg;
        this.maxAspectRatio = maxAspectRatio;
        this.maxCollapsesCount = maxCollapsesCount;
    }
}
