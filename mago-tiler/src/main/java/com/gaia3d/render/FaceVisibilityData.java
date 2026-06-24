package com.gaia3d.render;

import com.gaia3d.basic.halfedge.CameraDirectionType;

import java.util.Arrays;

public final class FaceVisibilityData {
    private static final int INITIAL_CAPACITY = 1024;

    private final CameraDirectionType cameraDirectionType;
    private int[] pixelCountByFaceId = new int[INITIAL_CAPACITY];
    private int maxUsedFaceId = -1;

    public FaceVisibilityData(CameraDirectionType cameraDirectionType) {
        this.cameraDirectionType = cameraDirectionType;
    }

    public CameraDirectionType getCameraDirectionType() {
        return cameraDirectionType;
    }

    public void incrementPixelFaceVisibility(int faceId) {
        if (faceId < 0) {
            return;
        }
        ensureCapacity(faceId);
        pixelCountByFaceId[faceId]++;
        maxUsedFaceId = Math.max(maxUsedFaceId, faceId);
    }

    public int getPixelFaceVisibility(int faceId) {
        if (faceId < 0 || faceId >= pixelCountByFaceId.length) {
            return 0;
        }
        return pixelCountByFaceId[faceId];
    }

    public void clearCounts() {
        if (maxUsedFaceId >= 0) {
            Arrays.fill(pixelCountByFaceId, 0, maxUsedFaceId + 1, 0);
            maxUsedFaceId = -1;
        }
    }

    public void deleteObjects() {
        pixelCountByFaceId = new int[0];
        maxUsedFaceId = -1;
    }

    private void ensureCapacity(int faceId) {
        if (faceId < pixelCountByFaceId.length) {
            return;
        }
        int newCapacity = Math.max(INITIAL_CAPACITY, pixelCountByFaceId.length);
        while (newCapacity <= faceId) {
            int grown = newCapacity + (newCapacity >> 1);
            if (grown <= newCapacity) {
                newCapacity = faceId + 1;
                break;
            }
            newCapacity = grown;
        }
        pixelCountByFaceId = Arrays.copyOf(pixelCountByFaceId, newCapacity);
    }
}
