package com.gaia3d.converter.gltf;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

/**
 * Quantization class for computing quantization matrices and converting values.
 * This class provides methods to compute a quantization matrix based on input values,
 * convert signed shorts from unsigned shorts, and quantize unsigned shorts.
 * It also includes methods for padding lengths.
 */
@Slf4j
@NoArgsConstructor
public class Quantization {

    /**
     * Computes a quantization matrix based on the provided values.
     * @param values An array of float values to compute the quantization matrix from.
     * @return A quantization matrix that maps the input values to a normalized range.
     */
    public static Matrix4d computeQuantizationMatrix(float[] values) {
        Matrix4d identityMatrix = new Matrix4d();
        return computeQuantizationMatrix(identityMatrix, values);
    }

    /**
     * Computes a quantization matrix based on the provided values and an original transformation matrix.
     * @param originalMatrix The original transformation matrix.
     * @param values An array of float values to compute the quantization matrix from.
     * @return A quantization matrix that maps the input values to a normalized range.
     */
    public static Matrix4d computeQuantizationMatrix(Matrix4d originalMatrix, float[] values) {
        double minX = Float.MAX_VALUE;
        double maxX = -Float.MAX_VALUE;
        double minY = Float.MAX_VALUE;
        double maxY = -Float.MAX_VALUE;
        double minZ = Float.MAX_VALUE;
        double maxZ = -Float.MAX_VALUE;

        for (int i = 0; i < values.length; i += 3) {
            double x = values[i];
            double y = values[i + 1];
            double z = values[i + 2];
            Vector3d v = new Vector3d(x, y, z);
            Vector3d transformed = v.mulPosition(originalMatrix, new Vector3d());

            double tx = transformed.x;
            double ty = transformed.y;
            double tz = transformed.z;

            minX = Math.min(minX, tx);
            maxX = Math.max(maxX, tx);
            minY = Math.min(minY, ty);
            maxY = Math.max(maxY, ty);
            minZ = Math.min(minZ, tz);
            maxZ = Math.max(maxZ, tz);
        }

        double rangeX = (maxX - minX);
        double rangeY = (maxY - minY);
        double rangeZ = (maxZ - minZ);
        double maxRange = Math.max(rangeX, Math.max(rangeY, rangeZ));

        double offsetX = minX;
        double offsetY = minY;
        double offsetZ = minZ;
        return new Matrix4d(
                maxRange, 0, 0, 0,
                0, maxRange, 0, 0,
                0, 0, maxRange, 0,
                offsetX, offsetY, offsetZ, 1);
    }

    /**
     * Calculates the padded length for a given length.
     * @param length The length to be padded.
     * @return The padded length, which is a multiple of 4.
     */
    public static int paddedLength(int length) {
        return length / 3 * 4;
    }

    /**
     * Converts an unsigned short value to a signed short value.
     * @param value The unsigned short value to be converted.
     * @return The signed short value.
     */
    public static short convertSignedShortFromUnsignedShort(int value) {
        if (value > 65536) {
            log.error("[ERROR] :Value must be less than or equal to 65535 -> {}", value);
            throw new IllegalArgumentException("Value must be less than or equal to 65535");
        }

        if (value > Short.MAX_VALUE) {
            return (short) (value - 65536);
        } else {
            return (short) value;
        }
    }

    /**
     * Quantize an array of float values into unsigned shorts.
     * @param values The array of float values to be quantized.
     * @param originalMatrix The original transformation matrix.
     * @param quantizationMatrix The quantization matrix used for quantization.
     * @return An array of quantized unsigned short values.
     */
    public static short[] quantizeUnsignedShorts(float[] values, Matrix4d originalMatrix, Matrix4d quantizationMatrix) {
        int unsignedShortMax = 65535;
        short paddingValue = 0;

        float scaleX = (float) quantizationMatrix.m00();
        float scaleY = (float) quantizationMatrix.m11();
        float scaleZ = (float) quantizationMatrix.m22();

        float offsetX = (float) quantizationMatrix.m30();
        float offsetY = (float) quantizationMatrix.m31();
        float offsetZ = (float) quantizationMatrix.m32();

        int quantizedIndex = 0;
        int paddedLength = paddedLength(values.length);
        short[] quantizedValues = new short[paddedLength];
        for (int i = 0; i < values.length; i += 3) {
            float x = values[i];
            float y = values[i + 1];
            float z = values[i + 2];

            Vector3d v = new Vector3d(x, y, z);
            Vector3d transformed = v.mulPosition(originalMatrix, new Vector3d());

            x = (float) transformed.x;
            y = (float) transformed.y;
            z = (float) transformed.z;

            float qx = (x - offsetX) / scaleX;
            float qy = (y - offsetY) / scaleY;
            float qz = (z - offsetZ) / scaleZ;

            int ix = (int) (qx * unsignedShortMax);
            int iy = (int) (qy * unsignedShortMax);
            int iz = (int) (qz * unsignedShortMax);

            quantizedValues[quantizedIndex++] = convertSignedShortFromUnsignedShort(ix);
            quantizedValues[quantizedIndex++] = convertSignedShortFromUnsignedShort(iy);
            quantizedValues[quantizedIndex++] = convertSignedShortFromUnsignedShort(iz);
            quantizedValues[quantizedIndex++] = paddingValue;
        }
        return quantizedValues;
    }
}
