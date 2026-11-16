package com.gaia3d.converter.pointcloud.shuffler;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seeded uniform shuffle of the fixed-size blocks (points) of a file.
 * A file that fits in one section is shuffled in memory and written once. A larger file is scattered block by block
 * into random temp files of about half a section, and each of them is shuffled in memory and appended to the target
 * (Rao-Sandelius), so every file is read and written at most twice. Trailing bytes that do not fill a block stay at the end.
 */
@Slf4j
public class OptimizedCardShuffler extends Shuffler {
    private static final int RANDOM_SEED = 8291;
    private static final int FIXED_SECTION_BYTES = 2024 * (1024 * 1024);
    private static final int STREAM_BUFFER_SIZE = 8192 * 64;
    static final int MAX_SCATTER_FILES = 64;

    private final long sectionBytes;

    public OptimizedCardShuffler() {
        this(FIXED_SECTION_BYTES);
    }

    OptimizedCardShuffler(long sectionBytes) {
        this.sectionBytes = Math.min(sectionBytes, Integer.MAX_VALUE - 8);
    }

    @Override
    public void shuffle(File sourceFile, File targetFile, int blockSize) {
        long sourceFileSize = sourceFile.length();
        long blockCount = sourceFileSize / blockSize;
        long fullBytes = blockCount * blockSize;
        long maxSectionBytes = getMaxSectionBytes(blockSize);
        int passes = countPasses(fullBytes, maxSectionBytes, blockSize);
        log.info("[Pre][Shuffle][{}/{}] File size: {} bytes, Point count: {} bytes, passes: {}", processCount, totalProcessCount, sourceFileSize, blockCount, passes);

        File tempDir = targetFile.getAbsoluteFile().getParentFile();
        Random random = new Random(RANDOM_SEED);
        try (InputStream in = new BufferedInputStream(new FileInputStream(sourceFile), STREAM_BUFFER_SIZE);
             OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile, false), STREAM_BUFFER_SIZE)) {
            shuffleBlocks(in, fullBytes, out, blockSize, maxSectionBytes, random, tempDir);
            in.transferTo(out); // tail bytes
        } catch (IOException e) {
            log.error("[ERROR] Failed to shuffle {}", sourceFile.getAbsolutePath(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clear() {

    }

    private long getMaxSectionBytes(int blockSize) {
        return Math.max(blockSize, (sectionBytes / blockSize) * blockSize);
    }

    // scatter files of about half a section, but never more open at once than MAX_SCATTER_FILES: a larger file scatters
    // into sections bigger than memory, which scatter again (about 64 GB of points per extra pass at the default size)
    static int getScatterCount(long length, long maxSectionBytes, int blockSize) {
        long scatterBytes = Math.max(blockSize, maxSectionBytes / 2);
        return (int) Math.max(2, Math.min(MAX_SCATTER_FILES, (length + scatterBytes - 1) / scatterBytes));
    }

    static int countPasses(long length, long maxSectionBytes, int blockSize) {
        int passes = 1;
        while (length > maxSectionBytes) {
            int scatterCount = getScatterCount(length, maxSectionBytes, blockSize);
            length = (length + scatterCount - 1) / scatterCount;
            passes++;
        }
        return passes;
    }

    /**
     * Writes the next {@code length} bytes of {@code in}, a whole number of blocks, to {@code out} in shuffled block order.
     */
    private void shuffleBlocks(InputStream in, long length, OutputStream out, int blockSize, long maxSectionBytes, Random random, File tempDir) throws IOException {
        if (length <= maxSectionBytes) {
            byte[] buffer = new byte[(int) length];
            readFully(in, buffer, (int) length);
            shuffleInMemory(buffer, blockSize, random);
            out.write(buffer);
            return;
        }

        long blockCount = length / blockSize;
        int scatterCount = getScatterCount(length, maxSectionBytes, blockSize);
        long scatterBytes = length / scatterCount;
        int bufferSize = (int) Math.min(STREAM_BUFFER_SIZE, Math.max(8192, scatterBytes));
        List<File> scatterFiles = new ArrayList<>(scatterCount);
        try {
            List<OutputStream> scatterOutputs = new ArrayList<>(scatterCount);
            try {
                for (int i = 0; i < scatterCount; i++) {
                    File scatterFile = File.createTempFile("shuffle_pass_", ".tmp", tempDir);
                    scatterFiles.add(scatterFile);
                    scatterOutputs.add(new BufferedOutputStream(new FileOutputStream(scatterFile, false), bufferSize));
                }
                log.info("[Pre][Shuffle][{}/{}] Scattering {} points into {} sections...", processCount, totalProcessCount, blockCount, scatterCount);
                byte[] block = new byte[blockSize];
                for (long i = 0; i < blockCount; i++) {
                    readFully(in, block, blockSize);
                    scatterOutputs.get(random.nextInt(scatterCount)).write(block);
                }
            } finally {
                for (OutputStream scatterOutput : scatterOutputs) {
                    scatterOutput.close();
                }
            }

            log.info("[Pre][Shuffle][{}/{}] Shuffling {} sections...", processCount, totalProcessCount, scatterCount);
            for (File scatterFile : scatterFiles) {
                try (InputStream scatterInput = new BufferedInputStream(new FileInputStream(scatterFile), bufferSize)) {
                    shuffleBlocks(scatterInput, scatterFile.length(), out, blockSize, maxSectionBytes, random, tempDir);
                }
                if (!scatterFile.delete()) {
                    log.warn("[WARN] Failed to delete shuffle temp file: {}", scatterFile.getAbsolutePath());
                }
            }
        } finally {
            for (File scatterFile : scatterFiles) {
                if (scatterFile.exists() && !scatterFile.delete()) {
                    log.warn("[WARN] Failed to delete shuffle temp file: {}", scatterFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Fisher-Yates shuffle of the blocks of {@code buffer} in place.
     */
    static void shuffleInMemory(byte[] buffer, int blockSize, Random random) {
        int blockCount = buffer.length / blockSize;
        byte[] temp = new byte[blockSize];
        for (int i = blockCount - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            if (i == j) {
                continue;
            }
            int iOffset = i * blockSize;
            int jOffset = j * blockSize;
            System.arraycopy(buffer, iOffset, temp, 0, blockSize);
            System.arraycopy(buffer, jOffset, buffer, iOffset, blockSize);
            System.arraycopy(temp, 0, buffer, jOffset, blockSize);
        }
    }

    private void readFully(InputStream in, byte[] buffer, int length) throws IOException {
        if (in.readNBytes(buffer, 0, length) != length) {
            throw new EOFException("Unexpected EOF while reading points to shuffle");
        }
    }
}
