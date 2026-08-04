package com.gaia3d.terrain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.zip.GZIPInputStream;

final class QuantizedMeshTile {
    private static final int MAX_QUANTIZED_VALUE = 32767;
    private static final int HEADER_SIZE = 88;
    private static final int SPATIAL_GRID_SIZE = 32;

    private final float minimumHeight;
    private final float maximumHeight;
    private final int[] u;
    private final int[] v;
    private final int[] height;
    private final int[] indices;
    private final int[][] triangleBuckets;

    private QuantizedMeshTile(
            float minimumHeight,
            float maximumHeight,
            int[] u,
            int[] v,
            int[] height,
            int[] indices) {
        this.minimumHeight = minimumHeight;
        this.maximumHeight = maximumHeight;
        this.u = u;
        this.v = v;
        this.height = height;
        this.indices = indices;
        this.triangleBuckets = buildTriangleBuckets(u, v, indices);
    }

    static QuantizedMeshTile decode(byte[] encoded) throws IOException {
        byte[] bytes = decompressIfNecessary(encoded);
        if (bytes.length < HEADER_SIZE + Integer.BYTES) {
            throw new IOException("Quantized Mesh tile is too short");
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(24);
        float minimumHeight = buffer.getFloat();
        float maximumHeight = buffer.getFloat();
        if (!Float.isFinite(minimumHeight) || !Float.isFinite(maximumHeight) || minimumHeight > maximumHeight) {
            throw new IOException("Invalid Quantized Mesh height range");
        }
        buffer.position(HEADER_SIZE);

        long vertexCountUnsigned = Integer.toUnsignedLong(buffer.getInt());
        if (vertexCountUnsigned > Integer.MAX_VALUE) {
            throw new IOException("Quantized Mesh vertex count is too large: " + vertexCountUnsigned);
        }
        int vertexCount = (int) vertexCountUnsigned;
        int[] u = decodeVertexArray(buffer, vertexCount);
        int[] v = decodeVertexArray(buffer, vertexCount);
        int[] height = decodeVertexArray(buffer, vertexCount);

        int indexElementSize = vertexCount > 65536 ? Integer.BYTES : Short.BYTES;
        align(buffer, indexElementSize);
        ensureRemaining(buffer, Integer.BYTES);
        long triangleCountUnsigned = Integer.toUnsignedLong(buffer.getInt());
        if (triangleCountUnsigned > Integer.MAX_VALUE / 3) {
            throw new IOException("Quantized Mesh triangle count is too large: " + triangleCountUnsigned);
        }
        int indexCount = (int) triangleCountUnsigned * 3;
        ensureRemaining(buffer, (long) indexCount * indexElementSize);
        int[] indices = new int[indexCount];
        int highest = 0;
        for (int i = 0; i < indexCount; i++) {
            int code = indexElementSize == Short.BYTES
                    ? Short.toUnsignedInt(buffer.getShort())
                    : buffer.getInt();
            indices[i] = highest - code;
            if (code == 0) {
                highest++;
            }
            if (indices[i] < 0 || indices[i] >= vertexCount) {
                throw new IOException("Invalid Quantized Mesh triangle index: " + indices[i]);
            }
        }

        return new QuantizedMeshTile(minimumHeight, maximumHeight, u, v, height, indices);
    }

    OptionalDouble sample(double normalizedU, double normalizedV) {
        double targetU = clamp(normalizedU) * MAX_QUANTIZED_VALUE;
        double targetV = clamp(normalizedV) * MAX_QUANTIZED_VALUE;
        int cellX = gridCell(targetU);
        int cellY = gridCell(targetV);
        int[] candidates = triangleBuckets[cellY * SPATIAL_GRID_SIZE + cellX];
        for (int triangleOffset : candidates) {
            int a = indices[triangleOffset];
            int b = indices[triangleOffset + 1];
            int c = indices[triangleOffset + 2];
            double denominator = (v[b] - v[c]) * (double) (u[a] - u[c])
                    + (u[c] - u[b]) * (double) (v[a] - v[c]);
            if (Math.abs(denominator) < 1.0e-12) {
                continue;
            }

            double weightA = ((v[b] - v[c]) * (targetU - u[c])
                    + (u[c] - u[b]) * (targetV - v[c])) / denominator;
            double weightB = ((v[c] - v[a]) * (targetU - u[c])
                    + (u[a] - u[c]) * (targetV - v[c])) / denominator;
            double weightC = 1.0 - weightA - weightB;
            double epsilon = 1.0e-7;
            if (weightA >= -epsilon && weightB >= -epsilon && weightC >= -epsilon) {
                double quantizedHeight = weightA * height[a] + weightB * height[b] + weightC * height[c];
                double decodedHeight = minimumHeight
                        + (maximumHeight - minimumHeight) * quantizedHeight / MAX_QUANTIZED_VALUE;
                return OptionalDouble.of(decodedHeight);
            }
        }
        return OptionalDouble.empty();
    }

    private static int[][] buildTriangleBuckets(int[] u, int[] v, int[] indices) {
        @SuppressWarnings("unchecked")
        List<Integer>[] buckets = new List[SPATIAL_GRID_SIZE * SPATIAL_GRID_SIZE];
        for (int triangleOffset = 0; triangleOffset < indices.length; triangleOffset += 3) {
            int a = indices[triangleOffset];
            int b = indices[triangleOffset + 1];
            int c = indices[triangleOffset + 2];
            int minimumX = gridCell(Math.min(u[a], Math.min(u[b], u[c])));
            int maximumX = gridCell(Math.max(u[a], Math.max(u[b], u[c])));
            int minimumY = gridCell(Math.min(v[a], Math.min(v[b], v[c])));
            int maximumY = gridCell(Math.max(v[a], Math.max(v[b], v[c])));
            for (int y = minimumY; y <= maximumY; y++) {
                for (int x = minimumX; x <= maximumX; x++) {
                    int bucketIndex = y * SPATIAL_GRID_SIZE + x;
                    if (buckets[bucketIndex] == null) {
                        buckets[bucketIndex] = new ArrayList<>();
                    }
                    buckets[bucketIndex].add(triangleOffset);
                }
            }
        }

        int[][] result = new int[buckets.length][];
        for (int i = 0; i < buckets.length; i++) {
            List<Integer> bucket = buckets[i];
            result[i] = bucket == null ? new int[0] : bucket.stream().mapToInt(Integer::intValue).toArray();
        }
        return result;
    }

    private static int gridCell(double quantizedCoordinate) {
        int cell = (int) (quantizedCoordinate * SPATIAL_GRID_SIZE / (MAX_QUANTIZED_VALUE + 1.0));
        return Math.max(0, Math.min(SPATIAL_GRID_SIZE - 1, cell));
    }

    private static int[] decodeVertexArray(ByteBuffer buffer, int count) throws IOException {
        ensureRemaining(buffer, (long) count * Short.BYTES);
        int[] result = new int[count];
        int value = 0;
        for (int i = 0; i < count; i++) {
            int encoded = Short.toUnsignedInt(buffer.getShort());
            value += (encoded >>> 1) ^ -(encoded & 1);
            if (value < 0 || value > MAX_QUANTIZED_VALUE) {
                throw new IOException("Invalid quantized vertex value: " + value);
            }
            result[i] = value;
        }
        return result;
    }

    private static void align(ByteBuffer buffer, int alignment) throws IOException {
        int remainder = buffer.position() % alignment;
        if (remainder != 0) {
            int padding = alignment - remainder;
            ensureRemaining(buffer, padding);
            buffer.position(buffer.position() + padding);
        }
    }

    private static void ensureRemaining(ByteBuffer buffer, long required) throws IOException {
        if (required > buffer.remaining()) {
            throw new IOException("Unexpected end of Quantized Mesh tile");
        }
    }

    private static byte[] decompressIfNecessary(byte[] bytes) throws IOException {
        if (bytes.length < 2 || (bytes[0] & 0xff) != 0x1f || (bytes[1] & 0xff) != 0x8b) {
            return bytes;
        }
        try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(bytes));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            input.transferTo(output);
            return output.toByteArray();
        }
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
