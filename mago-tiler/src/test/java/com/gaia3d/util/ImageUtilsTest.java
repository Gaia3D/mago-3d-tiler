package com.gaia3d.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ImageUtilsTest {

    @Test
    void getNearestPowerOfTwo() {
        int size = ImageUtils.getNearestPowerOfTwo(1025);
        assertEquals(1024, size);

        size = ImageUtils.getNearestPowerOfTwo(1024);
        assertEquals(1024, size);

        size = ImageUtils.getNearestPowerOfTwo(3072);
        assertEquals(2048, size);

        size = ImageUtils.getNearestPowerOfTwo(3073);
        assertEquals(4096, size);
    }

    @Test
    void getNearestPowerOfTwoHigher() {
        int size = ImageUtils.getNearestPowerOfTwoHigher(1025);
        assertEquals(2048, size);

        size = ImageUtils.getNearestPowerOfTwoHigher(1024);
        assertEquals(1024, size);

        size = ImageUtils.getNearestPowerOfTwoHigher(3072);
        assertEquals(4096, size);

        size = ImageUtils.getNearestPowerOfTwoHigher(3159);
        assertEquals(4096, size);
    }

    @Test
    void getChildFileFindsCaseInsensitiveTexturePath(@TempDir Path tempDir) throws IOException {
        Path textureDirectory = tempDir.resolve("textures");
        Files.createDirectories(textureDirectory);
        Files.writeString(textureDirectory.resolve("b_bd002.jpg"), "test");

        File file = ImageUtils.getChildFile(tempDir.toFile(), "TEXTURES/B_BD002.JPG");

        assertNotNull(file);
        assertEquals("b_bd002.jpg", file.getName());
        assertEquals(Path.of("textures", "b_bd002.jpg").toString(),
                ImageUtils.getChildPath(tempDir.toFile(), "TEXTURES/B_BD002.JPG"));
    }
}
