package com.gaia3d.converter.jgltf;

import com.gaia3d.command.Configurator;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class QuantizerTest {

    @Test
    void computeQuantizationMatrix() {
        Configurator.initConsoleLogger();

        // cube sample
        float[] positions = new float[] {
                -1, -1, -1, // left-bottom-back
                -1, -1, 1, // left-bottom-front
                -1, 1, -1, // left-top-back
                -1, 1, 1, // left-top-front
                1, -1, -1, // right-bottom-back
                1, -1, 1, // right-bottom-front
                1, 1, -1, // right-top-back
                1, 1, 1 // right-top-front
        };

        Matrix4d originalMatrix = new Matrix4d();
        originalMatrix.identity();

        // expected quantization matrix
        double[] expectedMatrixArray = new double[] {
                2.0, 0.0, 0.0, 0.0,
                0.0, 2.0, 0.0, 0.0,
                0.0, 0.0, 2.0, 0.0,
                -1.0, -1.0, -1.0, 1.0
        };
        Matrix4d expectedMatrix = new Matrix4d();
        expectedMatrix.set(expectedMatrixArray);

        Matrix4d quantizationMatrix = Quantizer.computeQuantizationMatrix(positions);
        assertEquals(expectedMatrix, quantizationMatrix);
    }

    @Test
    void paddedLength() {
        Configurator.initConsoleLogger();

        int xyzArrayLength = 3;
        int vertexCount = 5;

        int positionLength = vertexCount * xyzArrayLength;
        int paddedPositionLength = Quantizer.paddedLength(positionLength);
        assertEquals(20, paddedPositionLength);

        vertexCount = 1;
        positionLength = vertexCount * xyzArrayLength;
        paddedPositionLength = Quantizer.paddedLength(positionLength);
        assertEquals(4, paddedPositionLength);

        vertexCount = 0;
        positionLength = vertexCount * xyzArrayLength;
        paddedPositionLength = Quantizer.paddedLength(positionLength);
        assertEquals(0, paddedPositionLength);

        vertexCount = 100000;
        positionLength = vertexCount * xyzArrayLength;
        paddedPositionLength = Quantizer.paddedLength(positionLength);
        assertEquals(400000, paddedPositionLength);
    }

    @Test
    void convertSignedShortFromUnsignedShort() {
        Configurator.initConsoleLogger();

        short signedShort = Quantizer.convertSignedShortFromUnsignedShort(65535);
        assertEquals(-1, signedShort);

        signedShort = Quantizer.convertSignedShortFromUnsignedShort(32767);
        assertEquals(32767, signedShort);

        signedShort = Quantizer.convertSignedShortFromUnsignedShort(32768);
        assertEquals(-32768, signedShort);

        signedShort = Quantizer.convertSignedShortFromUnsignedShort(0);
        assertEquals(0, signedShort);

        signedShort = Quantizer.convertSignedShortFromUnsignedShort(1);
        assertEquals(1, signedShort);

        signedShort = Quantizer.convertSignedShortFromUnsignedShort(65534);
        assertEquals(-2, signedShort);
    }

    @Test
    void quantizeUnsignedShorts() {
        Configurator.initConsoleLogger();

        // cube sample
        float[] positions = new float[] {
                -1, -1, -1, // left-bottom-back
                -1, -1, 1, // left-bottom-front
                -1, 1, -1, // left-top-back
                -1, 1, 1, // left-top-front
                1, -1, -1, // right-bottom-back
                1, -1, 1, // right-bottom-front
                1, 1, -1, // right-top-back
                1, 1, 1 // right-top-front
        };

        Matrix4d originalMatrix = new Matrix4d();
        originalMatrix.identity();

        Matrix4d quantizationMatrix = Quantizer.computeQuantizationMatrix(positions);
        short[] quantizedPositions = Quantizer.quantizeUnsignedShorts(positions, originalMatrix, quantizationMatrix);
        assertEquals(32, quantizedPositions.length);

        // expected quantized positions
        short[] expectedQuantizedPositions = new short[] {
                0, 0, 0, 0,
                0, 0, -1, 0,
                0, -1, 0, 0,
                0, -1, -1, 0,
                -1, 0, 0, 0,
                -1, 0, -1, 0,
                -1, -1, 0, 0,
                -1, -1, -1, 0
        };
        assertArrayEquals(expectedQuantizedPositions, quantizedPositions);
    }
}