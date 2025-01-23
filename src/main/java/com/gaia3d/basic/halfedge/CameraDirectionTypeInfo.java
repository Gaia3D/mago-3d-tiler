package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4d;

@Getter
@Setter
public class CameraDirectionTypeInfo {
    CameraDirectionType cameraDirectionType = CameraDirectionType.CAMERA_DIRECTION_UNKNOWN;
    double angleDegree = 0.0; // angle between camera direction and the normal of the face
}
