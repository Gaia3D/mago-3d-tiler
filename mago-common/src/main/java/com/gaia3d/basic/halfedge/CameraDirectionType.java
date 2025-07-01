package com.gaia3d.basic.halfedge;

import org.joml.Vector3d;

public enum CameraDirectionType {
    CAMERA_DIRECTION_UNKNOWN, CAMERA_DIRECTION_XPOS, CAMERA_DIRECTION_XNEG, CAMERA_DIRECTION_ZPOS, CAMERA_DIRECTION_ZNEG, CAMERA_DIRECTION_YPOS, CAMERA_DIRECTION_YNEG, CAMERA_DIRECTION_XPOS_ZNEG, CAMERA_DIRECTION_XNEG_ZNEG, CAMERA_DIRECTION_XNEG_ZPOS, CAMERA_DIRECTION_XPOS_ZPOS, CAMERA_DIRECTION_YPOS_ZNEG, CAMERA_DIRECTION_YPOS_ZPOS, CAMERA_DIRECTION_YNEG_ZNEG, CAMERA_DIRECTION_YNEG_ZPOS;

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
            default:
                break;
        }

        result.normalize();

        return result;
    }

    public static CameraDirectionType getBestObliqueCameraDirectionType(Vector3d normal) {
        CameraDirectionType result = CameraDirectionType.CAMERA_DIRECTION_UNKNOWN;

        Vector3d camDirYPos = new Vector3d(0, 1, -1);
        camDirYPos.normalize();

        Vector3d camDirYNeg = new Vector3d(0, -1, -1);
        camDirYNeg.normalize();

        Vector3d camDirXPos = new Vector3d(1, 0, -1);
        camDirXPos.normalize();

        Vector3d camDirXNeg = new Vector3d(-1, 0, -1);
        camDirXNeg.normalize();

        double dotYPos = normal.dot(camDirYPos);
        double dotYNeg = normal.dot(camDirYNeg);
        double dotXPos = normal.dot(camDirXPos);
        double dotXNeg = normal.dot(camDirXNeg);

        // choose the most opposite direction
        // the most opposite direction is the most negative dot product
        if (dotYPos < dotYNeg) {
            if (dotYPos < dotXPos) {
                if (dotYPos < dotXNeg) {
                    result = CameraDirectionType.CAMERA_DIRECTION_YPOS_ZNEG;
                } else {
                    result = CameraDirectionType.CAMERA_DIRECTION_XNEG_ZNEG;
                }
            } else {
                if (dotXPos < dotXNeg) {
                    result = CameraDirectionType.CAMERA_DIRECTION_XPOS_ZNEG;
                } else {
                    result = CameraDirectionType.CAMERA_DIRECTION_XNEG_ZNEG;
                }
            }
        } else {
            if (dotYNeg < dotXPos) {
                if (dotYNeg < dotXNeg) {
                    result = CameraDirectionType.CAMERA_DIRECTION_XNEG_ZNEG;
                } else {
                    result = CameraDirectionType.CAMERA_DIRECTION_XNEG_ZNEG;
                }
            } else {
                if (dotXPos < dotXNeg) {
                    result = CameraDirectionType.CAMERA_DIRECTION_XPOS_ZNEG;
                } else {
                    result = CameraDirectionType.CAMERA_DIRECTION_YPOS_ZNEG;
                }
            }
        }

        return result;
    }

}
