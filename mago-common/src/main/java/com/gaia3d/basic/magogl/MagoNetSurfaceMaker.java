package com.gaia3d.basic.magogl;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.halfedge.HalfEdgeSurface;

public final class MagoNetSurfaceMaker {

    private static final float DEPTH_CLEAR_VALUE =
            1.0f;

    private static final float DEPTH_EPSILON =
            1.0e-6f;

    private static final int BORDER_DEPTH_SEARCH_RADIUS =
            2;

    private MagoNetSurfaceMaker() {
    }

    public static HalfEdgeSurface make(
            MagoFbo fbo,
            GaiaBoundingBox bbox,
            int numCols,
            int numRows,
            boolean flipDepthY,
            boolean makeSkirt
    ) {
        if (fbo == null || bbox == null) {
            return null;
        }

        float[][] sourceDepth =
                flipDepthY
                        ? fbo.getDepthGridFlippedY()
                        : fbo.getDepthGridRaw();

        float[][] scaledDepth =
                resizeDepthNearest(
                        sourceDepth,
                        numCols,
                        numRows
                );

        return makeFromDepthGrid(
                numCols,
                numRows,
                scaledDepth,
                bbox,
                makeSkirt
        );
    }

    public static HalfEdgeSurface makeFromDepthGrid(
            int numCols,
            int numRows,
            float[][] depthValues,
            GaiaBoundingBox bbox,
            boolean makeSkirt
    ) {
        // Aquí iría getHalfEdgeSurfaceRegularNetWithSkirt(...)
        return null;
    }

    private static float[][] resizeDepthNearest(
            float[][] source,
            int targetWidth,
            int targetHeight
    ) {
        // Escalador 1024x1024 → 180x180.
        return null;
    }

    private static boolean isValidDepth(
            float depth
    ) {
        return Float.isFinite(depth)
                && depth
                < DEPTH_CLEAR_VALUE
                - DEPTH_EPSILON;
    }
}
