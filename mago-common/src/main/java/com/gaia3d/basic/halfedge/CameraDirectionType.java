package com.gaia3d.basic.halfedge;

import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

public enum CameraDirectionType {
    UNKNOWN,
    XPOS,
    XNEG,
    ZPOS,
    ZNEG,
    YPOS,
    YNEG,
    XPOS_ZNEG,
    XNEG_ZNEG,
    XNEG_ZPOS,
    XPOS_ZPOS,
    YPOS_ZNEG,
    YPOS_ZPOS,
    YNEG_ZNEG,
    YNEG_ZPOS,
    XPOS_YPOS_ZNEG,
    XPOS_YNEG_ZNEG,
    XNEG_YPOS_ZNEG,
    XNEG_YNEG_ZNEG,
    XPOS_YPOS_ZPOS,
    XPOS_YNEG_ZPOS,
    XNEG_YPOS_ZPOS,
    XNEG_YNEG_ZPOS;

    public String getName() {
        return this.name();
    }

    public static Vector3d getCameraDirection(CameraDirectionType cameraDirectionType) {
        Vector3d result = new Vector3d();
        double z = 1.0;
        switch (cameraDirectionType) {
            case XPOS:
                result.set(1, 0, 0);
                break;
            case XNEG:
                result.set(-1, 0, 0);
                break;
            case ZPOS:
                result.set(0, 0, z);
                break;
            case ZNEG:
                result.set(0, 0, -z);
                break;
            case YPOS:
                result.set(0, 1, 0);
                break;
            case YNEG:
                result.set(0, -1, 0);
                break;
            case XPOS_ZNEG:
                result.set(1, 0, -z);
                break;
            case XNEG_ZNEG:
                result.set(-1, 0, -z);
                break;
            case XNEG_ZPOS:
                result.set(-1, 0, z);
                break;
            case XPOS_ZPOS:
                result.set(1, 0, z);
                break;
            case YPOS_ZNEG:
                result.set(0, 1, -z);
                break;
            case YPOS_ZPOS:
                result.set(0, 1, z);
                break;
            case YNEG_ZNEG:
                result.set(0, -1, -z);
                break;
            case YNEG_ZPOS:
                result.set(0, -1, z);
                break;
            case XPOS_YPOS_ZNEG:
                result.set(1, 1, -z);
                break;
            case XPOS_YNEG_ZNEG:
                result.set(1, -1, -z);
                break;
            case XNEG_YPOS_ZNEG:
                result.set(-1, 1, -z);
                break;
            case XNEG_YNEG_ZNEG:
                result.set(-1, -1, -z);
                break;
            case XPOS_YPOS_ZPOS:
                result.set(1, 1, z);
                break;
            case XPOS_YNEG_ZPOS:
                result.set(1, -1, z);
                break;
            case XNEG_YPOS_ZPOS:
                result.set(-1, 1, z);
                break;
            case XNEG_YNEG_ZPOS:
                result.set(-1, -1, z);
                break;
            default:
                break;
        }

        result.normalize();

        return result;
    }

    public static String getName(CameraDirectionType type) {
        if (type == null) {
            return     UNKNOWN.name();
        }

        return type.name();
    }

    public static CameraDirectionType getBestObliqueCameraDirectionType(Vector3d normal) {
        CameraDirectionType result = CameraDirectionType.    UNKNOWN;

        // invert normal and do dot product test
        Vector3d invertedNormal = new Vector3d(normal).mul(-1.0);
        double maxDot = -Double.MAX_VALUE;
        for (CameraDirectionType cameraDirectionType : CameraDirectionType.values()) {
            // In oblique camera direction selection, we only consider oblique directions.
            if (cameraDirectionType ==     UNKNOWN || cameraDirectionType == XPOS ||
                    cameraDirectionType == XNEG || cameraDirectionType == ZPOS ||
                    cameraDirectionType == ZNEG || cameraDirectionType == YPOS ||
                    cameraDirectionType == YNEG) {
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

    public static List<CameraDirectionType> get9ObliqueCameraDirectionTypes(List<CameraDirectionType> result){
        if(result == null){
            result = new ArrayList<>();
        }

        result.add(ZNEG);
        result.add(XPOS_ZNEG);
        result.add(XNEG_ZNEG);
        result.add(YPOS_ZNEG);
        result.add(YNEG_ZNEG);
        result.add(XPOS_YPOS_ZNEG);
        result.add(XPOS_YNEG_ZNEG);
        result.add(XNEG_YPOS_ZNEG);
        result.add(XNEG_YNEG_ZNEG);

        return result;
    }

}
