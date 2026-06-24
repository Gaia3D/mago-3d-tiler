package com.gaia3d.render;

import com.gaia3d.basic.halfedge.CameraDirectionType;

public final class CameraDirectionCandidate {
    public final CameraDirectionType cameraDirectionType;
    public final int pixelCount;

    public CameraDirectionCandidate(
            CameraDirectionType cameraDirectionType,
            int pixelCount
    ) {
        this.cameraDirectionType = cameraDirectionType;
        this.pixelCount = pixelCount;
    }
}
