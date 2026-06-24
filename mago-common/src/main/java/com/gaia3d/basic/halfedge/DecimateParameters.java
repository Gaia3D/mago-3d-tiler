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
    private int iterationsCount = 1;
    private double smallHedgeSize = 1.0;
    private int lod = -1;
    private WeldingParameters weldingParameters = new WeldingParameters();

    // small triangle parameters
    private double smallTriangleMinArea = 1.0;
    private double smallTrianglesMinSize = 0.5;

    public void setBasicValues(double maxDiffAngDegrees, double hedgeMinLength, double frontierMaxDiffAngDeg, double maxAspectRatio, int maxCollapsesCount, int iterationsCount, double smallHedgeSize) {
        this.maxDiffAngDegrees = maxDiffAngDegrees;
        this.hedgeMinLength = hedgeMinLength;
        this.frontierMaxDiffAngDeg = frontierMaxDiffAngDeg;
        this.maxAspectRatio = maxAspectRatio;
        this.maxCollapsesCount = maxCollapsesCount;
        this.iterationsCount = iterationsCount;
        this.smallHedgeSize = smallHedgeSize;
    }

    public DecimateParameters clone() {
        DecimateParameters clone = new DecimateParameters();
        clone.setMaxDiffAngDegrees(this.maxDiffAngDegrees);
        clone.setHedgeMinLength(this.hedgeMinLength);
        clone.setFrontierMaxDiffAngDeg(this.frontierMaxDiffAngDeg);
        clone.setMaxAspectRatio(this.maxAspectRatio);
        clone.setMaxCollapsesCount(this.maxCollapsesCount);
        clone.setIterationsCount(this.iterationsCount);
        clone.setSmallHedgeSize(this.smallHedgeSize);
        clone.setLod(this.lod);

        if (this.weldingParameters != null) {
            clone.setWeldingParameters(this.weldingParameters.clone());
        }
        return clone;
    }

    public void setAsDecimateLeaf() {
        setBasicValues(5.0, 0.01, 0.0, 40.0, 1000000, 1, 1.0);
        WeldingParameters weldingParameters = getWeldingParameters();
        weldingParameters.setCheckTexCoords(true);
    }

    public void setByLod(int lod) {
        // smallHedgeSize = 0.02 (scivile)
        // smallHedgeSize = 1.0 (sangam)
        if (lod == 0) {
            setBasicValues(5.0,
                    0.01,
                    0.0,
                    40.0,
                    1000000,
                    5,
                    1.0);
        } else if (lod == 1) {
            setBasicValues(
                    7.0,
                    0.0001,
                    0.9,
                    40.0,
                    1000000,
                    5,
                    0.02);
        } else if (lod == 2) {
            setBasicValues(10.0,
                    0.0001,
                    0.9,
                    40.0,
                    1000000,
                    5,
                    0.02);
        } else if (lod == 3) {
            setBasicValues(10.0,
                    0.0001,
                    0.9,
                    40.0,
                    1000000,
                    5,
                    0.02);
        } else if (lod > 3) {
            setBasicValues(10.0,
                    0.0001,
                    0.9,
                    40.0,
                    1000000,
                    5,
                    0.02);
        }
    }
}
