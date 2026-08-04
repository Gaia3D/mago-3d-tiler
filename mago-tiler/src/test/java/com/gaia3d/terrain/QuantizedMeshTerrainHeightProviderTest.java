package com.gaia3d.terrain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalDouble;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("default")
class QuantizedMeshTerrainHeightProviderTest {
    private static final int MAX_QUANTIZED_VALUE = 32767;

    @TempDir
    Path temporaryDirectory;

    @Test
    void samplesGzippedTmsTileUsingTriangleInterpolation() throws IOException {
        writeLayerJson(0);
        Path tilePath = temporaryDirectory.resolve("0/0/0.terrain");
        Files.createDirectories(tilePath.getParent());
        Files.write(tilePath, gzip(createPlaneTile()));

        QuantizedMeshTerrainHeightProvider provider =
                new QuantizedMeshTerrainHeightProvider(temporaryDirectory.resolve("layer.json"));

        // EPSG:4326 level zero tile 0/0 covers lon [-180, 0], lat [-90, 90].
        // lon=-135, lat=-45 therefore maps to local u=0.25, v=0.25.
        OptionalDouble sampled = provider.sample(-135.0, -45.0);
        assertTrue(sampled.isPresent());

        double expectedQuantizedHeight = 8192.0 * 0.25 + 16384.0 * 0.25;
        double expectedHeight = 100.0 + 100.0 * expectedQuantizedHeight / MAX_QUANTIZED_VALUE;
        assertEquals(expectedHeight, sampled.getAsDouble(), 1.0e-9);
    }

    @Test
    void fallsBackToAnAvailableParentTile() throws IOException {
        writeLayerJson(2);
        Path tilePath = temporaryDirectory.resolve("0/0/0.terrain");
        Files.createDirectories(tilePath.getParent());
        Files.write(tilePath, createPlaneTile());

        QuantizedMeshTerrainHeightProvider provider =
                new QuantizedMeshTerrainHeightProvider(temporaryDirectory.resolve("layer.json"));

        assertTrue(provider.sample(-135.0, -45.0).isPresent());
    }

    private void writeLayerJson(int maximumZoom) throws IOException {
        String json = """
                {
                  "tilejson": "2.1.0",
                  "format": "quantized-mesh-1.0",
                  "version": "1.0.0",
                  "scheme": "tms",
                  "projection": "EPSG:4326",
                  "minzoom": 0,
                  "maxzoom": %d,
                  "tiles": ["{z}/{x}/{y}.terrain?v={version}"]
                }
                """.formatted(maximumZoom);
        Files.writeString(temporaryDirectory.resolve("layer.json"), json);
    }

    private static byte[] createPlaneTile() {
        int[] u = {0, 32767, 0, 32767};
        int[] v = {0, 0, 32767, 32767};
        int[] height = {0, 8192, 16384, 24576};
        int[] highWaterCodes = {0, 0, 0, 2, 0, 2};

        ByteBuffer buffer = ByteBuffer.allocate(88 + 4 + 3 * u.length * 2 + 4 + highWaterCodes.length * 2 + 16)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(24);
        buffer.putFloat(100.0f);
        buffer.putFloat(200.0f);
        buffer.position(88);
        buffer.putInt(u.length);
        putDeltaZigZag(buffer, u);
        putDeltaZigZag(buffer, v);
        putDeltaZigZag(buffer, height);
        buffer.putInt(2);
        for (int code : highWaterCodes) {
            buffer.putShort((short) code);
        }
        // West, south, east and north edge vertex counts.
        buffer.putInt(0).putInt(0).putInt(0).putInt(0);
        return buffer.array();
    }

    private static void putDeltaZigZag(ByteBuffer buffer, int[] values) {
        int previous = 0;
        for (int value : values) {
            int delta = value - previous;
            int encoded = (delta << 1) ^ (delta >> 31);
            buffer.putShort((short) encoded);
            previous = value;
        }
    }

    private static byte[] gzip(byte[] bytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(bytes);
        }
        return output.toByteArray();
    }
}
