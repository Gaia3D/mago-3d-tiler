package com.gaia3d.basic.model;

import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.halfedge.HalfEdgeFace;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class GaiaTextureScissorData {
    private GaiaRectangle originBoundary;
    private GaiaRectangle currentBoundary;
    private GaiaRectangle batchedBoundary;
    private GaiaRectangle texCoordBoundary;
    private List<HalfEdgeFace> faces;
    private int expandedPixel;
    private GaiaRectangle noExpandedBoundary;

    public GaiaRectangle getOriginBoundary() {
        if (originBoundary == null) {
            double w = currentBoundary.getMaxX() - currentBoundary.getMinX();
            double h = currentBoundary.getMaxY() - currentBoundary.getMinY();
            originBoundary = new GaiaRectangle(0, 0, w, h);
        }
        return originBoundary;
    }

    public boolean mergeIfMergeable(GaiaTextureScissorData other) {
        if (!isMergeable(other)) {
            return false;
        }

        texCoordBoundary.addBoundingRectangle(other.texCoordBoundary);
        faces.addAll(other.faces);
        other.faces.clear(); // clear the faces of the other.

        return true;
    }

    public boolean isMergeable(GaiaTextureScissorData other) {
        if (texCoordBoundary == null || other.texCoordBoundary == null) {
            return false;
        }
        if (faces == null || other.faces == null) {
            return false;
        }

        // check the current boundary.
        double thisArea = texCoordBoundary.getArea();
        double otherArea = other.texCoordBoundary.getArea();
        GaiaRectangle mergedBoundary = new GaiaRectangle(texCoordBoundary);
        mergedBoundary.addBoundingRectangle(other.texCoordBoundary);
        double mergedArea = mergedBoundary.getArea();

        return !(mergedArea > thisArea + otherArea);
    }

    public boolean TEST_Check() {
        if (currentBoundary == null || noExpandedBoundary == null) {
            return true;
        }
        if (Math.abs(noExpandedBoundary.getMinX() - (currentBoundary.getMinX() + expandedPixel)) > 0.1) {
            return false;
        }
        if (Math.abs(noExpandedBoundary.getMinY() - (currentBoundary.getMinY() + expandedPixel)) > 0.1) {
            return false;
        }
        if (Math.abs(noExpandedBoundary.getMaxX() - (currentBoundary.getMaxX() - expandedPixel)) > 0.1) {
            return false;
        }
        if (Math.abs(noExpandedBoundary.getMaxY() - (currentBoundary.getMaxY() - expandedPixel)) > 0.1) {
            return false;
        }

        return true;
    }
}
