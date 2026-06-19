package com.gaia3d.basic.magogl;

import lombok.Getter;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.Objects;

@Getter
public final class MagoFbo {

    private final String name;

    private int width;
    private int height;

    /*
     * Color packed as ARGB:
     *
     * bits 31..24 = alpha
     * bits 23..16 = red
     * bits 15..8  = green
     * bits 7..0   = blue
     */
    private int[] colorBuffer;

    /*
     * Depth values normally in range [0, 1].
     * Default clear value is 1.0, like OpenGL.
     */
    private float[] depthBuffer;

    public MagoFbo(String name, int width, int height) {
        this.name = Objects.requireNonNull(name, "name must not be null");

        validateDimensions(width, height);

        this.width = width;
        this.height = height;

        allocateBuffers();
    }

    private void allocateBuffers() {
        int pixelCount = Math.multiplyExact(width, height);

        colorBuffer = new int[pixelCount];
        depthBuffer = new float[pixelCount];

        clear(0x00000000, 1.0f);
    }

    public void clear(int argbColor, float depth) {
        ensureAllocated();

        Arrays.fill(colorBuffer, argbColor);
        Arrays.fill(depthBuffer, depth);
    }

    public void clearColor(int argbColor) {
        Arrays.fill(colorBuffer, argbColor);
    }

    public void clearDepth(float depth) {
        Arrays.fill(depthBuffer, depth);
    }

    public int getIndex(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException(
                    "Pixel outside framebuffer: x=" + x + ", y=" + y
            );
        }

        return y * width + x;
    }

    public int getColor(int x, int y) {
        return colorBuffer[getIndex(x, y)];
    }

    public float getDepth(int x, int y) {
        return depthBuffer[getIndex(x, y)];
    }

    public void setColor(int x, int y, int argbColor) {
        colorBuffer[getIndex(x, y)] = argbColor;
    }

    public void setDepth(int x, int y, float depth) {
        depthBuffer[getIndex(x, y)] = depth;
    }

    public boolean testAndSetDepth(
            int x,
            int y,
            float depth,
            int argbColor
    ) {
        if (!Float.isFinite(depth)) {
            return false;
        }

        int index = getIndex(x, y);

        if (!(depth < depthBuffer[index])) {
            return false;
        }

        depthBuffer[index] = depth;
        colorBuffer[index] = argbColor;

        return true;
    }

    public void resize(int newWidth, int newHeight) {
        validateDimensions(newWidth, newHeight);

        if (newWidth == width
                && newHeight == height
                && colorBuffer != null
                && depthBuffer != null) {

            return;
        }

        this.width = newWidth;
        this.height = newHeight;

        allocateBuffers();
    }

    public BufferedImage getBufferedImage() {
        BufferedImage image = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_ARGB
        );

        int[] imageData =
                ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        for (int y = 0; y < height; y++) {
            int sourceOffset = y * width;
            int destinationOffset = (height - y - 1) * width;

            System.arraycopy(
                    colorBuffer,
                    sourceOffset,
                    imageData,
                    destinationOffset,
                    width
            );
        }

        return image;
    }

    public void getBufferedImageInto(BufferedImage image) {
        Objects.requireNonNull(image, "image must not be null");

        if (image.getWidth() != width || image.getHeight() != height) {
            throw new IllegalArgumentException(
                    "BufferedImage size does not match MagoFbo size."
            );
        }

        if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
            throw new IllegalArgumentException(
                    "BufferedImage must be TYPE_INT_ARGB."
            );
        }

        int[] imageData =
                ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        for (int y = 0; y < height; y++) {
            int sourceOffset = y * width;
            int destinationOffset = (height - y - 1) * width;

            System.arraycopy(
                    colorBuffer,
                    sourceOffset,
                    imageData,
                    destinationOffset,
                    width
            );
        }
    }

    public void cleanup() {
        colorBuffer = null;
        depthBuffer = null;
    }

    private void ensureAllocated() {
        if (colorBuffer == null || depthBuffer == null) {
            throw new IllegalStateException(
                    "MagoFbo has already been cleaned up: " + name
            );
        }
    }

    private static void validateDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Framebuffer dimensions must be greater than zero: "
                            + width + "x" + height
            );
        }
    }
}