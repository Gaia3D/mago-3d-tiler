package com.gaia3d.basic.halfedge;

import org.joml.Vector3d;

public enum CameraDirectionType {
    CAMERA_DIRECTION_UNKNOWN,
    CAMERA_DIRECTION_XPOS,
    CAMERA_DIRECTION_XNEG,
    CAMERA_DIRECTION_ZPOS,
    CAMERA_DIRECTION_ZNEG,
    CAMERA_DIRECTION_YPOS,
    CAMERA_DIRECTION_YNEG,
    CAMERA_DIRECTION_XPOS_ZNEG,
    CAMERA_DIRECTION_XNEG_ZNEG,
    CAMERA_DIRECTION_XNEG_ZPOS,
    CAMERA_DIRECTION_XPOS_ZPOS,
    CAMERA_DIRECTION_YPOS_ZNEG,
    CAMERA_DIRECTION_YPOS_ZPOS,
    CAMERA_DIRECTION_YNEG_ZNEG,
    CAMERA_DIRECTION_YNEG_ZPOS,
    CAMERA_DIRECTION_XPOS_YPOS_ZNEG,
    CAMERA_DIRECTION_XPOS_YNEG_ZNEG,
    CAMERA_DIRECTION_XNEG_YPOS_ZNEG,
    CAMERA_DIRECTION_XNEG_YNEG_ZNEG,
    CAMERA_DIRECTION_XPOS_YPOS_ZPOS,
    CAMERA_DIRECTION_XPOS_YNEG_ZPOS,
    CAMERA_DIRECTION_XNEG_YPOS_ZPOS,
    CAMERA_DIRECTION_XNEG_YNEG_ZPOS,;

    public static Vector3d getCameraDirection(CameraDirectionType cameraDirectionType) {
        Vector3d result = new Vector3d();
        double z = 1.0;
        switch (cameraDirectionType) {
            case CAMERA_DIRECTION_XPOS:
                result.set(1, 0, 0);
                break;
            case CAMERA_DIRECTION_XNEG:
                result.set(-1, 0, 0);
                break;
            case CAMERA_DIRECTION_ZPOS:
                result.set(0, 0, z);
                break;
            case CAMERA_DIRECTION_ZNEG:
                result.set(0, 0, -z);
                break;
            case CAMERA_DIRECTION_YPOS:
                result.set(0, 1, 0);
                break;
            case CAMERA_DIRECTION_YNEG:
                result.set(0, -1, 0);
                break;
            case CAMERA_DIRECTION_XPOS_ZNEG:
                result.set(1, 0, -z);
                break;
            case CAMERA_DIRECTION_XNEG_ZNEG:
                result.set(-1, 0, -z);
                break;
            case CAMERA_DIRECTION_XNEG_ZPOS:
                result.set(-1, 0, z);
                break;
            case CAMERA_DIRECTION_XPOS_ZPOS:
                result.set(1, 0, z);
                break;
            case CAMERA_DIRECTION_YPOS_ZNEG:
                result.set(0, 1, -z);
                break;
            case CAMERA_DIRECTION_YPOS_ZPOS:
                result.set(0, 1, z);
                break;
            case CAMERA_DIRECTION_YNEG_ZNEG:
                result.set(0, -1, -z);
                break;
            case CAMERA_DIRECTION_YNEG_ZPOS:
                result.set(0, -1, z);
                break;
            case CAMERA_DIRECTION_XPOS_YPOS_ZNEG:
                result.set(1, 1, -z);
                break;
            case CAMERA_DIRECTION_XPOS_YNEG_ZNEG:
                result.set(1, -1, -z);
                break;
            case CAMERA_DIRECTION_XNEG_YPOS_ZNEG:
                result.set(-1, 1, -z);
                break;
            case CAMERA_DIRECTION_XNEG_YNEG_ZNEG:
                result.set(-1, -1, -z);
                break;
            case CAMERA_DIRECTION_XPOS_YPOS_ZPOS:
                result.set(1, 1, z);
                break;
            case CAMERA_DIRECTION_XPOS_YNEG_ZPOS:
                result.set(1, -1, z);
                break;
            case CAMERA_DIRECTION_XNEG_YPOS_ZPOS:
                result.set(-1, 1, z);
                break;
            case CAMERA_DIRECTION_XNEG_YNEG_ZPOS:
                result.set(0, -1, z);
                break;
            default:
                break;
        }

        result.normalize();

        return result;
    }

    public static CameraDirectionType getBestObliqueCameraDirectionType(Vector3d normal) {
        CameraDirectionType result = CameraDirectionType.CAMERA_DIRECTION_UNKNOWN;

        // invert normal and do dot product test
        Vector3d invertedNormal = new Vector3d(normal).mul(-1.0);
        double maxDot = -Double.MAX_VALUE;
        for (CameraDirectionType cameraDirectionType : CameraDirectionType.values()) {
            // In oblique camera direction selection, we only consider oblique directions.
            if (cameraDirectionType == CAMERA_DIRECTION_UNKNOWN || cameraDirectionType == CAMERA_DIRECTION_XPOS ||
                    cameraDirectionType == CAMERA_DIRECTION_XNEG || cameraDirectionType == CAMERA_DIRECTION_ZPOS ||
                    cameraDirectionType == CAMERA_DIRECTION_ZNEG || cameraDirectionType == CAMERA_DIRECTION_YPOS ||
                    cameraDirectionType == CAMERA_DIRECTION_YNEG) {
                continue;
            }
            Vector3d cameraDirection = getCameraDirection(cameraDirectionType);
            double dot = invertedNormal.dot(cameraDirection);
            if (dot > maxDot) {
                maxDot = dot;
                result = cameraDirectionType;
            }
        }
        return result;
    }

}
