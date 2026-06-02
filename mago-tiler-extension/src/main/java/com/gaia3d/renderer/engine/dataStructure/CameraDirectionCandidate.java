package com.gaia3d.renderer.engine.dataStructure;

import com.gaia3d.basic.halfedge.CameraDirectionType;

public class CameraDirectionCandidate {
    public CameraDirectionType cameraDirectionType;
    public int pixelCount;

    public CameraDirectionCandidate(CameraDirectionType cameraDirectionType, int pixelCount) {
        this.cameraDirectionType = cameraDirectionType;
        this.pixelCount = pixelCount;
    }
}
