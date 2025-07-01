package com.gaia3d.renderer.engine.dataStructure;

import com.gaia3d.basic.halfedge.CameraDirectionType;

import java.util.HashMap;
import java.util.Map;

public class FaceVisibilityData {
    private final CameraDirectionType cameraDirectionType;
    private final Map<Integer, Integer> faceIdToPixelCountMap;

    public FaceVisibilityData(CameraDirectionType cameraDirectionType) {
        this.cameraDirectionType = cameraDirectionType;
        this.faceIdToPixelCountMap = new HashMap<>();
    }

    public void incrementPixelFaceVisibility(int faceId) {
        if (!faceIdToPixelCountMap.containsKey(faceId)) {
            faceIdToPixelCountMap.put(faceId, 1);
        } else {
            int count = faceIdToPixelCountMap.getOrDefault(faceId, 0);
            faceIdToPixelCountMap.put(faceId, count + 1);
        }
    }

    public int getPixelFaceVisibility(int faceId) {
        return faceIdToPixelCountMap.getOrDefault(faceId, 0);
    }

    public void deleteObjects() {
        faceIdToPixelCountMap.clear();
    }
}
