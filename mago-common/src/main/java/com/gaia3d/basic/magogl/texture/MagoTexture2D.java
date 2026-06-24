package com.gaia3d.basic.magogl.texture;

import org.joml.Vector4f;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.util.Objects;
import java.util.Arrays;

public final class MagoTexture2D {

    private final int width;
    private final int height;
    private int[] pixels;

    public MagoTexture2D(
            int width,
            int height,
            int[] pixels
    ) {
        this.width = width;
        this.height = height;
        this.pixels = pixels;
    }

    public static MagoTexture2D fromBufferedImage(
            BufferedImage image
    ) {
        Objects.requireNonNull(
                image,
                "image must not be null"
        );

        BufferedImage compatibleImage = image;

        if (image.getType()
                != BufferedImage.TYPE_INT_ARGB) {

            compatibleImage =
                    new BufferedImage(
                            image.getWidth(),
                            image.getHeight(),
                            BufferedImage.TYPE_INT_ARGB
                    );

            Graphics2D graphics =
                    compatibleImage.createGraphics();

            try {
                graphics.drawImage(
                        image,
                        0,
                        0,
                        null
                );
            } finally {
                graphics.dispose();
            }
        }

        DataBuffer dataBuffer =
                compatibleImage
                        .getRaster()
                        .getDataBuffer();

        if (!(dataBuffer instanceof DataBufferInt)) {
            throw new IllegalStateException(
                    "Expected DataBufferInt"
            );
        }

        int[] pixels =
                ((DataBufferInt) dataBuffer)
                        .getData();

        return new MagoTexture2D(
                compatibleImage.getWidth(),
                compatibleImage.getHeight(),
                pixels
        );
    }


    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int[] getPixelsCopy() {
        if (pixels == null) {
            throw new IllegalStateException("MagoTexture2D has already been deleted.");
        }
        return Arrays.copyOf(pixels, pixels.length);
    }

    public void sampleNearest(
            float u,
            float v,
            MagoTextureWrap wrapS,
            MagoTextureWrap wrapT,
            boolean invertV,
            Vector4f result
    ) {
        Objects.requireNonNull(result, "result must not be null");

        float sampleV = invertV ? 1.0f - v : v;

        float wrappedU = wrapCoordinate(u, wrapS);
        float wrappedV = wrapCoordinate(sampleV, wrapT);

        int x = Math.round(
                wrappedU * (width - 1)
        );

        int y = Math.round(
                wrappedV * (height - 1)
        );

        x = clamp(x, 0, width - 1);
        y = clamp(y, 0, height - 1);

        int argb = pixels[y * width + x];

        unpackArgb(argb, result);
    }

    public void sampleBilinear(
            float u,
            float v,
            MagoTextureWrap wrapS,
            MagoTextureWrap wrapT,
            boolean invertV,
            Vector4f result
    ) {
        Objects.requireNonNull(result, "result must not be null");

        float sampleV = invertV ? 1.0f - v : v;

        float wrappedU = wrapCoordinate(u, wrapS);
        float wrappedV = wrapCoordinate(sampleV, wrapT);

        /*
         * GPU-like bilinear sampling:
         * convert UV to texel space centered on texel centers.
         *
         * texel center 0 is at 0.5
         */
        float texelX = wrappedU * width - 0.5f;
        float texelY = wrappedV * height - 0.5f;

        int x0 = (int) Math.floor(texelX);
        int y0 = (int) Math.floor(texelY);

        int x1 = x0 + 1;
        int y1 = y0 + 1;

        float tx = texelX - x0;
        float ty = texelY - y0;

        int sx0 = wrapTexelIndex(x0, width, wrapS);
        int sx1 = wrapTexelIndex(x1, width, wrapS);
        int sy0 = wrapTexelIndex(y0, height, wrapT);
        int sy1 = wrapTexelIndex(y1, height, wrapT);

        int c00 = pixels[sy0 * width + sx0];
        int c10 = pixels[sy0 * width + sx1];
        int c01 = pixels[sy1 * width + sx0];
        int c11 = pixels[sy1 * width + sx1];

        float r00 = ((c00 >>> 16) & 0xFF) / 255.0f;
        float g00 = ((c00 >>> 8) & 0xFF) / 255.0f;
        float b00 = (c00 & 0xFF) / 255.0f;
        float a00 = ((c00 >>> 24) & 0xFF) / 255.0f;

        float r10 = ((c10 >>> 16) & 0xFF) / 255.0f;
        float g10 = ((c10 >>> 8) & 0xFF) / 255.0f;
        float b10 = (c10 & 0xFF) / 255.0f;
        float a10 = ((c10 >>> 24) & 0xFF) / 255.0f;

        float r01 = ((c01 >>> 16) & 0xFF) / 255.0f;
        float g01 = ((c01 >>> 8) & 0xFF) / 255.0f;
        float b01 = (c01 & 0xFF) / 255.0f;
        float a01 = ((c01 >>> 24) & 0xFF) / 255.0f;

        float r11 = ((c11 >>> 16) & 0xFF) / 255.0f;
        float g11 = ((c11 >>> 8) & 0xFF) / 255.0f;
        float b11 = (c11 & 0xFF) / 255.0f;
        float a11 = ((c11 >>> 24) & 0xFF) / 255.0f;

        float r0 = lerp(r00, r10, tx);
        float g0 = lerp(g00, g10, tx);
        float b0 = lerp(b00, b10, tx);
        float a0 = lerp(a00, a10, tx);

        float r1 = lerp(r01, r11, tx);
        float g1 = lerp(g01, g11, tx);
        float b1 = lerp(b01, b11, tx);
        float a1 = lerp(a01, a11, tx);

        result.set(
                lerp(r0, r1, ty),
                lerp(g0, g1, ty),
                lerp(b0, b1, ty),
                lerp(a0, a1, ty)
        );
    }

    private static BufferedImage resizeIfNecessary(
            BufferedImage source,
            int maxSize
    ) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        int largestDimension =
                Math.max(
                        sourceWidth,
                        sourceHeight
                );

        if (largestDimension <= maxSize) {
            return source;
        }

        double scale =
                (double) maxSize
                        / largestDimension;

        int targetWidth =
                Math.max(
                        1,
                        (int) Math.round(
                                sourceWidth * scale
                        )
                );

        int targetHeight =
                Math.max(
                        1,
                        (int) Math.round(
                                sourceHeight * scale
                        )
                );

        BufferedImage resized =
                new BufferedImage(
                        targetWidth,
                        targetHeight,
                        BufferedImage.TYPE_INT_ARGB
                );

        Graphics2D graphics =
                resized.createGraphics();

        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );

            graphics.drawImage(
                    source,
                    0,
                    0,
                    targetWidth,
                    targetHeight,
                    null
            );
        } finally {
            graphics.dispose();
        }

        return resized;
    }

    private static float wrapCoordinate(
            float coordinate,
            MagoTextureWrap wrap
    ) {
        if (!Float.isFinite(coordinate)) {
            return 0.0f;
        }

        return switch (wrap) {
            case CLAMP_TO_EDGE ->
                    Math.max(0.0f, Math.min(1.0f, coordinate));

            case REPEAT -> coordinate
                    - (float) Math.floor(coordinate);
        };
    }

    private static int wrapTexelIndex(
            int index,
            int size,
            MagoTextureWrap wrap
    ) {
        return switch (wrap) {
            case CLAMP_TO_EDGE -> clamp(index, 0, size - 1);

            case REPEAT -> {
                int mod = index % size;
                if (mod < 0) {
                    mod += size;
                }
                yield mod;
            }
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static void unpackArgb(
            int argb,
            Vector4f result
    ) {
        result.set(
                ((argb >>> 16) & 0xFF) / 255.0f,
                ((argb >>> 8) & 0xFF) / 255.0f,
                (argb & 0xFF) / 255.0f,
                ((argb >>> 24) & 0xFF) / 255.0f
        );
    }

    public void delete(){
        pixels = null;
    }
}
