package com.gaia3d.basic.geometry.octree;

import com.gaia3d.basic.geometry.GaiaBoundingBox;

public class OctreeBBoxInfo {
    public GaiaBoundingBox bbox;
    public int maxDepth;
    public double rootCubeSize;
    public double leafSize;

    public OctreeBBoxInfo(
            GaiaBoundingBox bbox,
            int maxDepth,
            double rootCubeSize,
            double leafSize
    ) {
        this.bbox = bbox;
        this.maxDepth = maxDepth;
        this.rootCubeSize = rootCubeSize;
        this.leafSize = leafSize;
    }
}
