package com.gaia3d.process.tileprocess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("default")
class TilesetRootTransformUpdaterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void parseTransformAllowsCommonSeparators() {
        TilesetRootTransformUpdater updater = new TilesetRootTransformUpdater();

        double[] transform = updater.parseTransform("[1, 0, 0, 0; 0 1 0 0 0 0 1 0 10 20 30 1]");

        assertArrayEquals(new double[]{
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                10, 20, 30, 1
        }, transform);
    }

    @Test
    void updateAddsTransformAndRecalculatesBoundingVolumes() throws Exception {
        File input = tempDir.resolve("input").resolve("tileset.json").toFile();
        File output = tempDir.resolve("output").resolve("tileset.json").toFile();
        Files.createDirectories(input.toPath().getParent());
        Files.writeString(input.toPath(), """
                {
                  "asset": {"version": "1.1"},
                  "geometricError": 128,
                  "root": {
                    "boundingVolume": {"box": [0,0,0,1,0,0,0,1,0,0,0,1]},
                    "geometricError": 64,
                    "refine": "ADD"
                  },
                  "extras": {"kept": true}
                }
                """, StandardCharsets.UTF_8);

        new TilesetRootTransformUpdater().update(input, output, new double[]{
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                10, 20, 30, 1
        });

        JsonNode written = objectMapper.readTree(output);
        JsonNode transform = written.path("root").path("transform");
        assertTrue(transform.isArray());
        assertEquals(16, transform.size());
        assertEquals(10.0, transform.get(12).asDouble());
        assertEquals(20.0, transform.get(13).asDouble());
        assertEquals(30.0, transform.get(14).asDouble());
        assertTrue(written.path("root").path("boundingVolume").path("box").isMissingNode());
        assertEquals(6, written.path("root").path("boundingVolume").path("region").size());
        assertTrue(written.path("extras").path("kept").asBoolean());
    }

    @Test
    void updateBacksUpOriginalToOutputDirectoryWhenOutputDiffersFromInput() throws Exception {
        File input = tempDir.resolve("input").resolve("tileset.json").toFile();
        File output = tempDir.resolve("output").resolve("tileset.json").toFile();
        Files.createDirectories(input.toPath().getParent());
        Files.writeString(input.toPath(), """
                {
                  "asset": {"version": "1.1"},
                  "root": {
                    "boundingVolume": {"box": [6378137,0,0,1,0,0,0,1,0,0,0,1]},
                    "geometricError": 0
                  }
                }
                """, StandardCharsets.UTF_8);

        new TilesetRootTransformUpdater().update(input, output, new double[]{
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
        });

        File backup = tempDir.resolve("output").resolve("tileset-original.json").toFile();
        assertTrue(backup.isFile());
        assertTrue(objectMapper.readTree(backup).path("root").path("transform").isMissingNode());
        assertEquals(16, objectMapper.readTree(output).path("root").path("transform").size());
    }

    @Test
    void updateMultipliesExistingRootTransform() throws Exception {
        File input = tempDir.resolve("tileset.json").toFile();
        Files.writeString(input.toPath(), """
                {
                  "asset": {"version": "1.1"},
                  "root": {
                    "transform": [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1],
                    "boundingVolume": {"box": [6378137,0,0,1,0,0,0,1,0,0,0,1]},
                    "geometricError": 0
                  }
                }
                """, StandardCharsets.UTF_8);

        TilesetRootTransformUpdater updater = new TilesetRootTransformUpdater();

        updater.update(input, input, new double[]{
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                10, 20, 30, 1
        });

        JsonNode written = objectMapper.readTree(input);
        JsonNode transform = written.path("root").path("transform");
        assertEquals(11.0, transform.get(12).asDouble());
        assertEquals(20.0, transform.get(13).asDouble());
        assertEquals(30.0, transform.get(14).asDouble());
        assertEquals(6, written.path("root").path("boundingVolume").path("region").size());
    }

    @Test
    void updateBacksUpOriginalWhenOutputOverwritesInput() throws Exception {
        File tileset = tempDir.resolve("tileset.json").toFile();
        Files.writeString(tileset.toPath(), """
                {
                  "asset": {"version": "1.1"},
                  "root": {"geometricError": 0}
                }
                """, StandardCharsets.UTF_8);

        new TilesetRootTransformUpdater().update(tileset, tileset, new double[]{
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                10, 20, 30, 1
        });

        File backup = tempDir.resolve("tileset-original.json").toFile();
        assertTrue(backup.isFile());
        assertTrue(objectMapper.readTree(backup).path("root").path("transform").isMissingNode());
        assertEquals(16, objectMapper.readTree(tileset).path("root").path("transform").size());
    }
}
