package com.gaia3d.basic.halfedge;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

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

    private Map<Integer, Double> mapMaxDiffAngDegrees;
    private Map<Integer, Double> mapHedgeMinLength;
    private Map<Integer, Double> mapFrontierMaxDiffAngDeg;
    private Map<Integer, Double> mapMaxAspectRatio;
    private Map<Integer, Integer> mapMaxCollapsesCount;
    private Map<Integer, Double> mapSmallHedgeSize;

    public void setBasicValues(double maxDiffAngDegrees, double hedgeMinLength, double frontierMaxDiffAngDeg, double maxAspectRatio, int maxCollapsesCount, int iterationsCount, double smallHedgeSize) {
        this.maxDiffAngDegrees = maxDiffAngDegrees;
        this.hedgeMinLength = hedgeMinLength;
        this.frontierMaxDiffAngDeg = frontierMaxDiffAngDeg;
        this.maxAspectRatio = maxAspectRatio;
        this.maxCollapsesCount = maxCollapsesCount;
        this.iterationsCount = iterationsCount;
        this.smallHedgeSize = smallHedgeSize;
    }

    public void setBasicValuesFromIteration(int iteration, double maxDiffAngDegrees, double hedgeMinLength, double frontierMaxDiffAngDeg, double maxAspectRatio, int maxCollapsesCount, double smallHedgeSize) {
        if (mapMaxDiffAngDegrees == null) mapMaxDiffAngDegrees = new HashMap<>();
        if (mapHedgeMinLength == null) mapHedgeMinLength = new HashMap<>();
        if (mapFrontierMaxDiffAngDeg == null) mapFrontierMaxDiffAngDeg = new HashMap<>();
        if (mapMaxAspectRatio == null) mapMaxAspectRatio = new HashMap<>();
        if (mapMaxCollapsesCount == null) mapMaxCollapsesCount = new HashMap<>();
        if (mapSmallHedgeSize == null) mapSmallHedgeSize = new HashMap<>();

        mapMaxDiffAngDegrees.put(iteration, maxDiffAngDegrees);
        mapHedgeMinLength.put(iteration, hedgeMinLength);
        mapFrontierMaxDiffAngDeg.put(iteration, frontierMaxDiffAngDeg);
        mapMaxAspectRatio.put(iteration, maxAspectRatio);
        mapMaxCollapsesCount.put(iteration, maxCollapsesCount);
        mapSmallHedgeSize.put(iteration, smallHedgeSize);
    }

    public double getMaxDiffAngDegreesByIteration(int iteration) {
        if (mapMaxDiffAngDegrees != null && mapMaxDiffAngDegrees.containsKey(iteration)) {
            return mapMaxDiffAngDegrees.get(lod);
        }
        return -1.0;
    }

    public double getHedgeMinLengthByIteration(int iteration) {
        if (mapHedgeMinLength != null && mapHedgeMinLength.containsKey(iteration)) {
            return mapHedgeMinLength.get(lod);
        }
        return -1.0;
    }

    public double getFrontierMaxDiffAngDegByIteration(int iteration) {
        if (mapFrontierMaxDiffAngDeg != null && mapFrontierMaxDiffAngDeg.containsKey(iteration)) {
            return mapFrontierMaxDiffAngDeg.get(lod);
        }
        return -1.0;
    }

    public double getMaxAspectRatioByIteration(int iteration) {
        if (mapMaxAspectRatio != null && mapMaxAspectRatio.containsKey(iteration)) {
            return mapMaxAspectRatio.get(lod);
        }
        return -1.0;
    }

    public int getMaxCollapsesCountByIteration(int iteration) {
        if (mapMaxCollapsesCount != null && mapMaxCollapsesCount.containsKey(iteration)) {
            return mapMaxCollapsesCount.get(lod);
        }
        return -1;
    }

    public double getSmallHedgeSizeByIteration(int iteration) {
        if (mapSmallHedgeSize != null && mapSmallHedgeSize.containsKey(iteration)) {
            return mapSmallHedgeSize.get(lod);
        }
        return -1.0;
    }
}
