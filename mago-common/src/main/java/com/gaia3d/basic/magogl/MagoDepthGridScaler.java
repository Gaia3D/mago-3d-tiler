package com.gaia3d.basic.magogl;

import java.util.Arrays;

public final class MagoDepthGridScaler {

    private static final float BACKGROUND_DEPTH =
            1.0f;

    private static final float DEPTH_EPSILON =
            1e-6f;

    private MagoDepthGridScaler() {
    }


    public static float[][] resizeDepthNearestVerified(
            float[][] source,
            int targetWidth,
            int targetHeight
    ) {
        validateDepthGrid(source);

        if (targetWidth <= 0 || targetHeight <= 0) {
            throw new IllegalArgumentException(
                    "Invalid target dimensions: "
                            + targetWidth
                            + "x"
                            + targetHeight
            );
        }

        int sourceWidth =
                source.length;

        int sourceHeight =
                source[0].length;

        float[][] result =
                new float[targetWidth][targetHeight];

        /*
         * Fundamental para el diagnóstico:
         * una celda no escrita permanecerá como NaN,
         * no como un falso depth 0.0.
         */
        for (int x = 0; x < targetWidth; x++) {
            Arrays.fill(
                    result[x],
                    Float.NaN
            );
        }

        for (int targetX = 0;
             targetX < targetWidth;
             targetX++) {

            int sourceX =
                    mapEndpointPreserving(
                            targetX,
                            targetWidth,
                            sourceWidth
                    );

            for (int targetY = 0;
                 targetY < targetHeight;
                 targetY++) {

                int sourceY =
                        mapEndpointPreserving(
                                targetY,
                                targetHeight,
                                sourceHeight
                        );

                float sourceDepth =
                        source[sourceX][sourceY];

                /*
                 * Dado que el mínimo del source es 0.21436873,
                 * esto nunca debería cumplirse.
                 */
                if (sourceDepth <= 1.0e-6f) {
                    throw new IllegalStateException(
                            "Unexpected near-zero source depth:"
                                    + " source=("
                                    + sourceX + ", " + sourceY
                                    + "), target=("
                                    + targetX + ", " + targetY
                                    + "), depth="
                                    + sourceDepth
                    );
                }

                result[targetX][targetY] =
                        sourceDepth;
            }
        }

        /*
         * Verificar EL RESULTADO, no los valores fuente
         * durante la copia.
         */
        int unassignedCount =
                0;

        int zeroCount =
                0;

        float minimum =
                Float.POSITIVE_INFINITY;

        float maximum =
                Float.NEGATIVE_INFINITY;

        for (int x = 0; x < targetWidth; x++) {
            for (int y = 0; y < targetHeight; y++) {

                float depth =
                        result[x][y];

                if (Float.isNaN(depth)) {
                    System.out.println(
                            "[UNASSIGNED DEPTH] x="
                                    + x
                                    + ", y="
                                    + y
                    );

                    unassignedCount++;
                    continue;
                }

                if (depth == 0.0f) {
                    System.out.println(
                            "[ZERO DEPTH AFTER COPY] x="
                                    + x
                                    + ", y="
                                    + y
                    );

                    zeroCount++;
                }

                minimum =
                        Math.min(minimum, depth);

                maximum =
                        Math.max(maximum, depth);
            }
        }

        System.out.println(
                "Verified resized depth:"
                        + " unassigned="
                        + unassignedCount
                        + ", zeros="
                        + zeroCount
                        + ", min="
                        + minimum
                        + ", max="
                        + maximum
        );

        if (unassignedCount > 0) {
            throw new IllegalStateException(
                    "Depth resize left "
                            + unassignedCount
                            + " target cells unassigned."
            );
        }

        if (zeroCount > 0) {
            throw new IllegalStateException(
                    "Depth resize produced "
                            + zeroCount
                            + " zero values."
            );
        }

        return result;
    }

    private static int mapEndpointPreserving(
            int targetIndex,
            int targetSize,
            int sourceSize
    ) {
        if (targetSize <= 1 || sourceSize <= 1) {
            return 0;
        }

        double normalized =
                targetIndex
                        / (double) (targetSize - 1);

        int sourceIndex =
                (int) Math.round(
                        normalized
                                * (sourceSize - 1)
                );

        return Math.max(
                0,
                Math.min(
                        sourceSize - 1,
                        sourceIndex
                )
        );
    }

    private static boolean isNearZeroDepth(
            float depth,
            float epsilon
    ) {
        return Float.isFinite(depth)
                && Math.abs(depth) <= epsilon;
    }

    private static boolean isExactZeroDepth(
            float depth
    ) {
        /*
         * Detecta tanto +0.0f como -0.0f.
         */
        return (
                Float.floatToRawIntBits(depth)
                        & 0x7FFFFFFF
        ) == 0;
    }

    private static void validateDepthGrid(
            float[][] depthGrid
    ) {
        if (depthGrid == null
                || depthGrid.length == 0
                || depthGrid[0] == null
                || depthGrid[0].length == 0) {

            throw new IllegalArgumentException(
                    "Depth grid must not be null or empty."
            );
        }

        int height =
                depthGrid[0].length;

        for (int x = 0; x < depthGrid.length; x++) {
            if (depthGrid[x] == null
                    || depthGrid[x].length != height) {

                throw new IllegalArgumentException(
                        "Depth grid must be rectangular. Invalid x="
                                + x
                );
            }
        }
    }

    private static void verifyNearestPerimeter(
            float[][] source,
            float[][] target
    ) {
        int sourceWidth =
                source.length;

        int sourceHeight =
                source[0].length;

        int targetWidth =
                target.length;

        int targetHeight =
                target[0].length;

        /*
         * Bordes inferior y superior.
         */
        for (int targetX = 0;
             targetX < targetWidth;
             targetX++) {

            int sourceX =
                    mapEndpointPreserving(
                            targetX,
                            targetWidth,
                            sourceWidth
                    );

            verifyCopiedDepth(
                    source[sourceX][0],
                    target[targetX][0],
                    sourceX,
                    0,
                    targetX,
                    0
            );

            verifyCopiedDepth(
                    source[sourceX][sourceHeight - 1],
                    target[targetX][targetHeight - 1],
                    sourceX,
                    sourceHeight - 1,
                    targetX,
                    targetHeight - 1
            );
        }

        /*
         * Bordes izquierdo y derecho.
         */
        for (int targetY = 0;
             targetY < targetHeight;
             targetY++) {

            int sourceY =
                    mapEndpointPreserving(
                            targetY,
                            targetHeight,
                            sourceHeight
                    );

            verifyCopiedDepth(
                    source[0][sourceY],
                    target[0][targetY],
                    0,
                    sourceY,
                    0,
                    targetY
            );

            verifyCopiedDepth(
                    source[sourceWidth - 1][sourceY],
                    target[targetWidth - 1][targetY],
                    sourceWidth - 1,
                    sourceY,
                    targetWidth - 1,
                    targetY
            );
        }
    }

    private static void verifyCopiedDepth(
            float sourceDepth,
            float targetDepth,
            int sourceX,
            int sourceY,
            int targetX,
            int targetY
    ) {
        if (Float.floatToRawIntBits(sourceDepth)
                != Float.floatToRawIntBits(targetDepth)) {

            throw new IllegalStateException(
                    "Nearest depth mismatch:"
                            + " source=("
                            + sourceX
                            + ", "
                            + sourceY
                            + ") depth="
                            + sourceDepth
                            + ", target=("
                            + targetX
                            + ", "
                            + targetY
                            + ") depth="
                            + targetDepth
            );
        }
    }

    private static float findNearestValidDepth(
            float[][] source,
            int centerX,
            int centerY,
            int searchRadius
    ) {
        float centerDepth =
                source[centerX][centerY];

        if (isValidDepth(centerDepth)) {
            return centerDepth;
        }

        int width =
                source.length;

        int height =
                source[0].length;

        float bestDepth =
                BACKGROUND_DEPTH;

        int bestDistanceSquared =
                Integer.MAX_VALUE;

        for (int offsetY = -searchRadius;
             offsetY <= searchRadius;
             offsetY++) {

            int y =
                    centerY + offsetY;

            if (y < 0 || y >= height) {
                continue;
            }

            for (int offsetX = -searchRadius;
                 offsetX <= searchRadius;
                 offsetX++) {

                int x =
                        centerX + offsetX;

                if (x < 0 || x >= width) {
                    continue;
                }

                float depth =
                        source[x][y];

                if (!isValidDepth(depth)) {
                    continue;
                }

                int distanceSquared =
                        offsetX * offsetX
                                + offsetY * offsetY;

                if (distanceSquared
                        < bestDistanceSquared) {

                    bestDistanceSquared =
                            distanceSquared;

                    bestDepth =
                            depth;
                }
            }
        }

        return bestDepth;
    }

    private static void validateSource(
            float[][] source
    ) {
        if (source == null || source.length == 0) {
            throw new IllegalArgumentException(
                    "Source depth grid must not be null or empty."
            );
        }

        if (source[0] == null
                || source[0].length == 0) {

            throw new IllegalArgumentException(
                    "Source depth grid height must be greater than zero."
            );
        }

        int expectedHeight =
                source[0].length;

        for (int x = 0; x < source.length; x++) {
            if (source[x] == null
                    || source[x].length
                    != expectedHeight) {

                throw new IllegalArgumentException(
                        "Source depth grid must be rectangular. "
                                + "Invalid column x="
                                + x
                );
            }
        }
    }

    public static boolean isValidDepth(
            float depth
    ) {
        return Float.isFinite(depth)
                && depth
                < BACKGROUND_DEPTH
                - DEPTH_EPSILON;
    }
}
