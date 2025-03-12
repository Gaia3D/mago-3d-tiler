package com.gaia3d.renderer.engine.dataStructure;

import com.gaia3d.basic.halfedge.CameraDirectionType;

import java.util.Map;

public class FaceVisibilityDataManager {
    private final Map<CameraDirectionType, FaceVisibilityData> faceVisibilityDataMap;

    public FaceVisibilityDataManager() {
        faceVisibilityDataMap = new java.util.HashMap<>();
    }

    public FaceVisibilityData getFaceVisibilityData(CameraDirectionType cameraDirectionType) {
        // if no existing data, create new one
        if (!faceVisibilityDataMap.containsKey(cameraDirectionType)) {
            faceVisibilityDataMap.put(cameraDirectionType, new FaceVisibilityData(cameraDirectionType));
        }
        return faceVisibilityDataMap.get(cameraDirectionType);
    }

    public CameraDirectionType getBestCameraDirectionTypeOfFace(int faceId) {
        CameraDirectionType bestCameraDirectionType = null;
        int maxPixelCount = 0;
        for (Map.Entry<CameraDirectionType, FaceVisibilityData> entry : faceVisibilityDataMap.entrySet()) {
            int pixelCount = entry.getValue().getPixelFaceVisibility(faceId);
            if (pixelCount > maxPixelCount) {
                maxPixelCount = pixelCount;
                bestCameraDirectionType = entry.getKey();
            }
        }
        return bestCameraDirectionType;
    }

    public void deleteObjects() {
        for (FaceVisibilityData faceVisibilityData : faceVisibilityDataMap.values()) {
            faceVisibilityData.deleteObjects();
        }
        faceVisibilityDataMap.clear();
    }
}
