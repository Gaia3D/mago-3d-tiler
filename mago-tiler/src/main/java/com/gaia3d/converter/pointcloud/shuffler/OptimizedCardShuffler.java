package com.gaia3d.converter.pointcloud.shuffler;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Random;

@Slf4j
public class OptimizedCardShuffler extends Shuffler {
    private static final int RANDOM_SEED = 8291;
    private static final int FIXED_SECTION_BYTES = 2024 * (1024 * 1024);
    private static final int minimumPasses = 4;
    private static final int maximumPasses = 12;

    private int[] maximumLocalOrder;
    private int[] tailTempLocalOrder = null;

    @Override
    public void shuffle(File sourceFile, File targetFile, int blockSize) {
        long sourceFileSize = sourceFile.length();
        long blockCount = sourceFileSize / blockSize;
        int passes = (int) (blockCount / 20_000_000L);
        if (passes < minimumPasses) {
            passes = minimumPasses;
        } else if (passes > maximumPasses) {
            passes = maximumPasses;
        }

        log.info("[Pre][Shuffle][{}/{}] File size: {} bytes, Point count: {} bytes, passes: {}", processCount, totalProcessCount, sourceFileSize, blockCount, passes);
        shuffle(sourceFile, targetFile, blockSize, passes);
    }

    @Override
    public void clear() {
        maximumLocalOrder = null;
    }

    public void shuffle(File sourceFile, File targetFile, int blockSize, int passes) {
        if (passes <= 0) {
            passes = 1;
            log.warn("Invalid number of passes specified. Defaulting to 1 pass.");
        }

        if (passes == 1) {
            int sectionBytes = chooseSectionBytes(blockSize);
            shuffleOnce(sourceFile, targetFile, blockSize, sectionBytes, 0);
            tailTempLocalOrder = null;
            return;
        }

        File tempDir = targetFile.getParentFile();
        if (tempDir == null) {
            tempDir = new File(".");
        }

        File tmp1 = null;
        File tmp2 = null;

        try {
            tmp1 = File.createTempFile("shuffle_pass_", ".tmp", tempDir);
            if (passes > 2) {
                tmp2 = File.createTempFile("shuffle_pass_", ".tmp", tempDir);
            }

            File currentIn = sourceFile;
            File currentOut;

            for (int pass = 0; pass < passes; pass++) {
                log.info("[Pre][Shuffle][{}/{}][{}/{}] Point Cloud Shuffling...", processCount, totalProcessCount, pass + 1, passes);
                boolean lastPass = (pass == passes - 1);

                if (lastPass) {
                    currentOut = targetFile;
                } else if (pass == 0) {
                    currentOut = tmp1;
                } else if (tmp2 == null) {
                    currentOut = tmp1;
                } else {
                    currentOut = (pass % 2 == 1) ? tmp2 : tmp1;
                }

                int sectionBytes = chooseSectionBytes(blockSize);
                log.debug("=== Global section shuffle pass {}/{} : {} -> {} (sectionBytes={}) ===", pass + 1, passes, currentIn.getAbsolutePath(), currentOut.getAbsolutePath(), sectionBytes);

                shuffleOnce(currentIn, currentOut, blockSize, sectionBytes, pass);
                currentIn = currentOut;
            }
            tailTempLocalOrder = null;
        } catch (IOException e) {
            log.error("[ERROR] Failed to prepare temp files for multi-pass shuffle", e);
            throw new RuntimeException(e);
        } finally {
            if (tmp1 != null && tmp1.exists() && !tmp1.equals(targetFile)) {
                tmp1.delete();
            }
            if (tmp2 != null && tmp2.exists() && !tmp2.equals(targetFile)) {
                tmp2.delete();
            }
        }
    }

    private int chooseSectionBytes(int blockSize) {
        int sectionBytes = FIXED_SECTION_BYTES;
        if (sectionBytes < blockSize) {
            sectionBytes = blockSize;
        } else {
            sectionBytes = (sectionBytes / blockSize) * blockSize;
        }

        log.debug("Fixed sectionBytes={} ({} blocks)", sectionBytes, sectionBytes / blockSize);
        return sectionBytes;
    }

    private void shuffleOnce(File sourceFile, File targetFile, int blockSize, int sectionBytes, int passIndex) {
        long fileSize = sourceFile.length();
        if (fileSize == 0L) {
            log.warn("Source file is empty. Nothing to shuffle.");
            copyAsIs(sourceFile, targetFile);
            return;
        }

        long fullBlocks = fileSize / blockSize;
        int tailBytes = (int) (fileSize % blockSize);
        if (fullBlocks == 0) {
            log.warn("File smaller than one block. Copy as-is.");
            copyAsIs(sourceFile, targetFile);
            return;
        }

        int blocksPerSection = Math.max(1, sectionBytes / blockSize);
        long sectionCountLong = (fullBlocks + blocksPerSection - 1) / blocksPerSection;
        int sectionCount = (int) sectionCountLong;

        log.debug("[Pass {}] fullBlocks={}, blocksPerSection={}, sectionCount={}", passIndex + 1, fullBlocks, blocksPerSection, sectionCount);

        long shiftBase = Math.max(1L, blocksPerSection / 2L + 1L);
        long offsetBlocks = (shiftBase * passIndex) % fullBlocks;
        log.debug("[Pass {}] offsetBlocks={}", passIndex + 1, offsetBlocks);

        int[] sectionOrder = new int[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            sectionOrder[i] = i;
        }
        shuffleArray(sectionOrder, new Random(RANDOM_SEED + 13L * passIndex));
        try (RandomAccessFile raf = new RandomAccessFile(sourceFile, "r");
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(targetFile, false), 8192 * 64))) {

            int maxSectionBytes = blocksPerSection * blockSize;
            byte[] sectionBuffer = new byte[maxSectionBytes];

            for (int orderIdx = 0; orderIdx < sectionCount; orderIdx++) {
                int sectionIndex = sectionOrder[orderIdx];

                long logicalStart = (long) sectionIndex * blocksPerSection;
                if (logicalStart >= fullBlocks) {
                    continue;
                }
                int logicalLen = (int) Math.min(blocksPerSection, fullBlocks - logicalStart);
                long firstPhysIndex = (offsetBlocks + logicalStart) % fullBlocks;
                int part1Len = (int) Math.min(logicalLen, fullBlocks - firstPhysIndex);
                int part1Bytes = part1Len * blockSize;

                long part1StartPointer = firstPhysIndex * (long) blockSize;
                raf.seek(part1StartPointer);
                readFully(raf, sectionBuffer, 0, part1Bytes);

                int part2Len = logicalLen - part1Len;
                if (part2Len > 0) {
                    int part2Bytes = part2Len * blockSize;
                    long part2StartPointer = 0L;
                    raf.seek(part2StartPointer);
                    readFully(raf, sectionBuffer, part1Bytes, part2Bytes);
                }

                int tempSectionBytes = logicalLen * blockSize;
                byte[] shuffledSection = new byte[tempSectionBytes];

                if (logicalLen == blocksPerSection) {
                    if (maximumLocalOrder == null || maximumLocalOrder.length != blocksPerSection) {
                        log.info("[Pre][Shuffle] Preparing Local Block Order for sections with {} blocks...", blocksPerSection);
                        maximumLocalOrder = new int[blocksPerSection];
                        for (int i = 0; i < blocksPerSection; i++) {
                            maximumLocalOrder[i] = i;
                        }
                        shuffleArray(maximumLocalOrder, new Random(RANDOM_SEED * 31L + blocksPerSection));
                    }

                    for (int i = 0; i < logicalLen; i++) {
                        int blk = maximumLocalOrder[i];
                        int srcOffset = blk * blockSize;
                        int dstOffset = i * blockSize;
                        System.arraycopy(sectionBuffer, srcOffset, shuffledSection, dstOffset, blockSize);
                    }
                } else {
                    int[] tailLocalOrder = tailTempLocalOrder;
                    if (tailLocalOrder == null || tailLocalOrder.length != logicalLen) {
                        log.info("[Pre][Shuffle] Preparing Tail Local Block Order for {} blocks...", logicalLen);
                        tailLocalOrder = new int[logicalLen];
                        for (int i = 0; i < logicalLen; i++) {
                            tailLocalOrder[i] = i;
                        }
                        shuffleArray(tailLocalOrder, new Random(RANDOM_SEED * 31L + logicalLen));
                        tailTempLocalOrder = tailLocalOrder;
                    }

                    for (int i = 0; i < logicalLen; i++) {
                        int blk = tailLocalOrder[i];
                        int srcOffset = blk * blockSize;
                        int dstOffset = i * blockSize;
                        System.arraycopy(sectionBuffer, srcOffset, shuffledSection, dstOffset, blockSize);
                    }
                }
                out.write(shuffledSection, 0, tempSectionBytes);
            }

            if (tailBytes > 0) {
                long tailPointer = fullBlocks * (long) blockSize;
                raf.seek(tailPointer);
                byte[] tail = new byte[tailBytes];
                readFully(raf, tail, 0, tailBytes);
                out.write(tail);
            }
            out.flush();
        } catch (IOException e) {
            log.error("[ERROR] Failed to shuffle once (offset-based)", e);
            throw new RuntimeException(e);
        }
        tailTempLocalOrder = null;
    }

    private void readFully(RandomAccessFile raf, byte[] buf, int off, int len) throws IOException {
        int readTotal = 0;
        while (readTotal < len) {
            int read = raf.read(buf, off + readTotal, len - readTotal);
            if (read == -1) {
                throw new EOFException("Unexpected EOF while reading section");
            }
            readTotal += read;
        }
    }

    private void shuffleArray(int[] arr, Random random) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
    }

    private void copyAsIs(File source, File target) {
        try (InputStream in = new BufferedInputStream(new FileInputStream(source)); OutputStream out = new BufferedOutputStream(new FileOutputStream(target, false))) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        } catch (IOException e) {
            log.error("[ERROR] Failed to copy file as-is", e);
            throw new RuntimeException(e);
        }
    }
}
