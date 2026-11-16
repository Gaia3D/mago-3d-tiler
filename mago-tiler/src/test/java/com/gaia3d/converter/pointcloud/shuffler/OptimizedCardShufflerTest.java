package com.gaia3d.converter.pointcloud.shuffler;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@Tag("default")
class OptimizedCardShufflerTest {
    private static final int BLOCK_SIZE = 32;

    @TempDir
    Path tempDir;

    @Test
    void shufflesInOnePassWhenTheFileFitsASection() throws IOException {
        File source = writeBlocks("source.bin", 1000, 5);
        File target = tempDir.resolve("target.bin").toFile();
        new OptimizedCardShuffler().shuffle(source, target, BLOCK_SIZE);
        assertPermutation(source, target, 1000, 5);
    }

    @Test
    void shufflesThroughTempFilesWhenTheFileExceedsASection() throws IOException {
        File source = writeBlocks("source.bin", 5000, 7);
        File target = tempDir.resolve("target.bin").toFile();
        new OptimizedCardShuffler(BLOCK_SIZE * 300L).shuffle(source, target, BLOCK_SIZE);
        assertPermutation(source, target, 5000, 7);
        try (var files = Files.list(tempDir)) {
            assertEquals(2, files.count(), "scatter temp files must be removed");
        }
    }

    @Test
    void scattersAgainWhenSectionsWouldNeedTooManyOpenFiles() throws IOException {
        // 20-block sections would need 500 scatter files, so the 64 files hold more than a section and scatter again
        long sectionBytes = BLOCK_SIZE * 20L;
        assertEquals(OptimizedCardShuffler.MAX_SCATTER_FILES, OptimizedCardShuffler.getScatterCount(5000L * BLOCK_SIZE, sectionBytes, BLOCK_SIZE));
        assertEquals(3, OptimizedCardShuffler.countPasses(5000L * BLOCK_SIZE, sectionBytes, BLOCK_SIZE));

        File source = writeBlocks("source.bin", 5000, 3);
        File target = tempDir.resolve("target.bin").toFile();
        new OptimizedCardShuffler(sectionBytes).shuffle(source, target, BLOCK_SIZE);
        assertPermutation(source, target, 5000, 3);
        try (var files = Files.list(tempDir)) {
            assertEquals(2, files.count(), "scatter temp files must be removed");
        }
    }

    @Test
    void boundsPassesAtTheDefaultSection() {
        long section = (2024L * 1024 * 1024 / 18) * 18;
        assertEquals(1, OptimizedCardShuffler.countPasses(1L << 30, section, 18));
        assertEquals(2, OptimizedCardShuffler.countPasses(50L << 30, section, 18));
        assertEquals(3, OptimizedCardShuffler.countPasses(200L << 30, section, 18));
    }

    @Test
    void isDeterministic() throws IOException {
        File source = writeBlocks("source.bin", 3000, 0);
        File first = tempDir.resolve("first.bin").toFile();
        File second = tempDir.resolve("second.bin").toFile();
        new OptimizedCardShuffler(BLOCK_SIZE * 400L).shuffle(source, first, BLOCK_SIZE);
        new OptimizedCardShuffler(BLOCK_SIZE * 400L).shuffle(source, second, BLOCK_SIZE);
        assertArrayEquals(Files.readAllBytes(first.toPath()), Files.readAllBytes(second.toPath()));
    }

    @Test
    void spreadsBlocksAcrossTheFile() throws IOException {
        int blockCount = 4000;
        File source = writeBlocks("source.bin", blockCount, 0);
        File target = tempDir.resolve("target.bin").toFile();
        new OptimizedCardShuffler(BLOCK_SIZE * 500L).shuffle(source, target, BLOCK_SIZE);
        int[] ids = readIds(target, blockCount);
        // the first tenth of the output, read as a coarse LOD, must sample the whole input range evenly
        int[] histogram = new int[10];
        for (int i = 0; i < blockCount / 10; i++) {
            histogram[ids[i] * 10 / blockCount]++;
        }
        for (int count : histogram) {
            assertTrue(count > 20 && count < 60, "uneven sample " + Arrays.toString(histogram));
        }
    }

    private File writeBlocks(String name, int blockCount, int tailBytes) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(blockCount * BLOCK_SIZE + tailBytes);
        for (int i = 0; i < blockCount; i++) {
            buffer.putInt(i * BLOCK_SIZE, i);
            buffer.putInt(i * BLOCK_SIZE + BLOCK_SIZE - 4, ~i);
        }
        for (int i = 0; i < tailBytes; i++) {
            buffer.put(blockCount * BLOCK_SIZE + i, (byte) (100 + i));
        }
        File file = tempDir.resolve(name).toFile();
        Files.write(file.toPath(), buffer.array());
        return file;
    }

    private int[] readIds(File file, int blockCount) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(Files.readAllBytes(file.toPath()));
        int[] ids = new int[blockCount];
        for (int i = 0; i < blockCount; i++) {
            ids[i] = buffer.getInt(i * BLOCK_SIZE);
            assertEquals(~ids[i], buffer.getInt(i * BLOCK_SIZE + BLOCK_SIZE - 4), "block " + i + " is torn");
        }
        return ids;
    }

    private void assertPermutation(File source, File target, int blockCount, int tailBytes) throws IOException {
        assertEquals(source.length(), target.length());
        int[] ids = readIds(target, blockCount);
        int[] sorted = ids.clone();
        Arrays.sort(sorted);
        for (int i = 0; i < blockCount; i++) {
            assertEquals(i, sorted[i]);
        }
        int moved = 0;
        for (int i = 0; i < blockCount; i++) {
            if (ids[i] != i) {
                moved++;
            }
        }
        assertTrue(moved > blockCount * 0.9, "blocks were not shuffled");
        byte[] sourceBytes = Files.readAllBytes(source.toPath());
        byte[] targetBytes = Files.readAllBytes(target.toPath());
        int tailStart = blockCount * BLOCK_SIZE;
        assertArrayEquals(Arrays.copyOfRange(sourceBytes, tailStart, tailStart + tailBytes), Arrays.copyOfRange(targetBytes, tailStart, tailStart + tailBytes));
    }
}
