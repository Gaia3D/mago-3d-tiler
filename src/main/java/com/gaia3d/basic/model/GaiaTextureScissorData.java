package com.gaia3d.basic.model;

import com.gaia3d.basic.geometry.GaiaRectangle;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GaiaTextureScissorData {
    private int originMaterialId;
    private int materialId;
    private GaiaRectangle originBoundary;
    private GaiaRectangle currentBoundary;
    private GaiaRectangle batchedBoundary;

    public GaiaRectangle getOriginBoundary() {
        if(originBoundary == null) {
            double w = currentBoundary.getMaxX() - currentBoundary.getMinX();
            double h = currentBoundary.getMaxY() - currentBoundary.getMinY();
            originBoundary = new GaiaRectangle(0, 0, w, h);
        }
        return originBoundary;
    }
}
