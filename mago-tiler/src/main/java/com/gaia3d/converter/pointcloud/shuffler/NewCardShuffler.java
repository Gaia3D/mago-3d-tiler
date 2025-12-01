package com.gaia3d.converter.pointcloud.shuffler;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Slf4j
public class NewCardShuffler implements Shuffler {
    private static final int RANDOM_SEED = 8291;
    private static final int MIN_CHUNK_BYTES = 256 * 1024 * 1024; // 256MB
    private static final int MAX_CHUNK_BYTES = 1024 * 1024 * 1024; // 1024MB

    @Override
    public void shuffle(File sourceFile, File targetFile, int blockSize) {
        shuffle(sourceFile, targetFile, blockSize, 5);
    }

    /**
     * Multi-pass global shuffle
     * blockSize = 18
     * minSectionBytes = 160_000
     * maxSectionBytes = 320_000
     * passes = 3
     */
    public void shuffle(File sourceFile, File targetFile, int blockSize, int passes) {

        if (passes <= 0) {
            passes = 1;
        }

        if (passes == 1) {
            // 섹션 크기를 한 번 랜덤 선택해서 1회 셔플
            int sectionBytes = chooseSectionBytes(blockSize, MIN_CHUNK_BYTES, MAX_CHUNK_BYTES, 0);
            shuffleOnce(sourceFile, targetFile, blockSize, sectionBytes, 0);
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

                int sectionBytes = chooseSectionBytes(blockSize, MIN_CHUNK_BYTES, MAX_CHUNK_BYTES, pass);
                log.debug("=== Global section shuffle pass {}/{} : {} -> {} (sectionBytes={}) ===", pass + 1, passes, currentIn.getAbsolutePath(), currentOut.getAbsolutePath(), sectionBytes);
                shuffleOnce(currentIn, currentOut, blockSize, sectionBytes, pass);
                currentIn = currentOut;
            }

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

    private int chooseSectionBytes(int blockSize, int minSectionBytes, int maxSectionBytes, int passIndex) {

        // blockSize 배수로 범위 정리
        int minBlocks = Math.max(1, minSectionBytes / blockSize);
        int maxBlocks = Math.max(minBlocks, maxSectionBytes / blockSize);

        Random random = new Random(RANDOM_SEED + 97L * passIndex);
        int rangeBlocks = maxBlocks - minBlocks + 1;
        int chosenBlocks = minBlocks + random.nextInt(rangeBlocks);

        int sectionBytes = chosenBlocks * blockSize;
        log.debug("[Pass {}] chosen sectionBytes={} ({} blocks)", passIndex + 1, sectionBytes, chosenBlocks);
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

        log.debug("[Pass {}] fileSize={} bytes, blockSize={} bytes, tailBytes={}", passIndex + 1, fileSize, blockSize, tailBytes);
        log.debug("[Pass {}] fullBlocks={}, blocksPerSection={}, sectionCount={}", passIndex + 1, fullBlocks, blocksPerSection, sectionCount);
        List<Integer> sectionOrder = new ArrayList<>(sectionCount);
        for (int i = 0; i < sectionCount; i++) {
            sectionOrder.add(i);
        }
        Collections.shuffle(sectionOrder, new Random(RANDOM_SEED + 31L * passIndex));

        try (RandomAccessFile raf = new RandomAccessFile(sourceFile, "r"); DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(targetFile, false), 8192 * 8))) {
            int maxSectionBytes = blocksPerSection * blockSize;
            byte[] sectionBuffer = new byte[maxSectionBytes];

            for (int orderIdx = 0; orderIdx < sectionOrder.size(); orderIdx++) {
                int sectionIndex = sectionOrder.get(orderIdx);

                long startBlockIndex = (long) sectionIndex * blocksPerSection;
                if (startBlockIndex >= fullBlocks) {
                    continue;
                }

                int blocksInThisSection = (int) Math.min(blocksPerSection, fullBlocks - startBlockIndex);
                int sectionByteLength = blocksInThisSection * blockSize;

                long sectionStartPointer = startBlockIndex * (long) blockSize;

                log.debug("[Pass {}] Section {}/{} (index={}, blocks={}, bytes={})", passIndex + 1, orderIdx + 1, sectionCount, sectionIndex, blocksInThisSection, sectionByteLength);

                // 이 섹션 영역을 한 번에 읽어오기
                raf.seek(sectionStartPointer);
                readFully(raf, sectionBuffer, 0, sectionByteLength);

                int[] localOrder = new int[blocksInThisSection];
                for (int i = 0; i < blocksInThisSection; i++) {
                    localOrder[i] = i;
                }
                shuffleArray(localOrder, new Random(RANDOM_SEED + 131L * passIndex + sectionIndex));

                for (int i = 0; i < blocksInThisSection; i++) {
                    int blk = localOrder[i];
                    int offset = blk * blockSize;
                    out.write(sectionBuffer, offset, blockSize);
                }
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
            log.error("[ERROR] Failed to shuffle once", e);
            throw new RuntimeException(e);
        }
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
