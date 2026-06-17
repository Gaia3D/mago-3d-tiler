package com.gaia3d.renderer.engine.dataStructure;

import com.gaia3d.basic.halfedge.CameraDirectionType;

import java.util.Arrays;

public class FaceVisibilityData {

    private static final int INITIAL_CAPACITY = 1024;

    private final CameraDirectionType cameraDirectionType;

    private int[] pixelCountByFaceId;
    private int maxUsedFaceId = -1;

    public FaceVisibilityData(CameraDirectionType cameraDirectionType) {
        this.cameraDirectionType = cameraDirectionType;
        this.pixelCountByFaceId = new int[INITIAL_CAPACITY];
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

        if (faceId > maxUsedFaceId) {
            maxUsedFaceId = faceId;
        }
    }

    public int getPixelFaceVisibility(int faceId) {
        if (faceId < 0 || faceId >= pixelCountByFaceId.length) {
            return 0;
        }

        return pixelCountByFaceId[faceId];
    }

    private void ensureCapacity(int faceId) {
        if (faceId < pixelCountByFaceId.length) {
            return;
        }

        int newCapacity;

        if (pixelCountByFaceId.length == 0) {
            newCapacity = Math.max(
                    INITIAL_CAPACITY,
                    faceId + 1
            );
        } else {
            newCapacity = pixelCountByFaceId.length;

            while (newCapacity <= faceId) {
                int grownCapacity =
                        newCapacity + (newCapacity >> 1);

                if (grownCapacity <= newCapacity) {
                    newCapacity = faceId + 1;
                    break;
                }

                newCapacity = grownCapacity;
            }
        }

        pixelCountByFaceId =
                Arrays.copyOf(pixelCountByFaceId, newCapacity);
    }

    public void clearCounts() {
        if (maxUsedFaceId < 0) {
            return;
        }

        Arrays.fill(
                pixelCountByFaceId,
                0,
                maxUsedFaceId + 1,
                0
        );

        maxUsedFaceId = -1;
    }

    public void deleteObjects() {
        pixelCountByFaceId = new int[0];
        maxUsedFaceId = -1;
    }
}