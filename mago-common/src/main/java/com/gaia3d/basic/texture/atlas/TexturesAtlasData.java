package com.gaia3d.basic.texture.atlas;


import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.halfedge.CameraDirectionType;
import com.gaia3d.basic.halfedge.PlaneType;
import lombok.Getter;
import lombok.Setter;

import java.awt.image.BufferedImage;

@Getter
@Setter
public class TexturesAtlasData {
    private int classifyId = -1;
    private PlaneType planeType = PlaneType.UNKNOWN;
    private CameraDirectionType cameraDirectionType = CameraDirectionType.UNKNOWN;
    private BufferedImage textureImage;
    private GaiaRectangle originalBoundary;
    private GaiaRectangle currentBoundary;
    private GaiaRectangle batchedBoundary;
    private GaiaRectangle texCoordBoundary;
    private GaiaBoundingBox FaceGroupBBox;

    public void setTextureImage(BufferedImage textureImage) {
        this.textureImage = textureImage;
        double w = textureImage.getWidth();
        double h = textureImage.getHeight();
        this.originalBoundary = new GaiaRectangle(0, 0, w, h);
    }

    public void deleteObjects() {
        if (textureImage != null) {
            textureImage.flush();
        }
        textureImage = null;
        originalBoundary = null;
        currentBoundary = null;
        batchedBoundary = null;
        texCoordBoundary = null;
        FaceGroupBBox = null;
    }
}
