package com.gaia3d.basic.halfedge;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CameraDirectionTypeInfo {
    private CameraDirectionType cameraDirectionType = CameraDirectionType.CAMERA_DIRECTION_UNKNOWN;
    private double angleDegree = 0.0; // angle between camera direction and the normal of the face
}
