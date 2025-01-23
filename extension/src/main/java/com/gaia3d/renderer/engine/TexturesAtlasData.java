package com.gaia3d.renderer.engine;


import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.halfedge.CameraDirectionType;
import com.gaia3d.basic.halfedge.PlaneType;
import lombok.Getter;
import lombok.Setter;

import java.awt.image.BufferedImage;

@Setter
@Getter

public class TexturesAtlasData {
    private int classifyId = -1;
    private PlaneType planeType = PlaneType.UNKNOWN;
    private CameraDirectionType cameraDirectionType = CameraDirectionType.CAMERA_DIRECTION_UNKNOWN;
    private BufferedImage textureImage;
    private GaiaRectangle originalBoundary;
    private GaiaRectangle currentBoundary;
    private GaiaRectangle batchedBoundary;
    private GaiaBoundingBox FaceGroupBBox;

    public void setTextureImage(BufferedImage textureImage) {
        this.textureImage = textureImage;
        double w = textureImage.getWidth();
        double h = textureImage.getHeight();
        this.originalBoundary = new GaiaRectangle(0, 0, w, h);
    }
}
