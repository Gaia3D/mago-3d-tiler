package com.gaia3d.converter.pointcloud.shuffler;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Slf4j
public class CardShuffler implements Shuffler {

    private static final int RANDOM_SEED = 8291;

    // 한 번에 메모리에 올릴 최대 청크 크기 (필요하면 조절)
    private static final int MIN_CHUNK_BYTES = 256 * 1024 * 1024; // 256MB
    private static final int MAX_CHUNK_BYTES = 1024 * 1024 * 1024; // 256MB
    private static final int BUFFERED_IO_SIZE = 8192 * 8; // 64KB

    //private static final int MIN_CHUNK_BYTES = 1024 * 1024; // 16MB
    //private static final int MAX_CHUNK_BYTES = 4 * 1024 * 1024; // 64MB

    /**
     * 기본 1회 전역 셔플
     */
    public void shuffle(File sourceFile, File targetFile, int blockSize) {
        shuffle(sourceFile, targetFile, blockSize, 5);
    }

    /**
     * 전역 셔플을 globalPasses 횟수만큼 수행
     *  - globalPasses <= 0 이면 1로 취급
     */
    public void shuffle(File sourceFile, File targetFile, int blockSize, int globalPasses) {
        int passes = Math.max(1, globalPasses);

        if (passes == 1) {
            // 1회만이면 바로 source → target 으로 셔플
            shuffleOnce(sourceFile, targetFile, blockSize, 0);
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
                    // passes == 2인 경우, 여기로는 안 들어옴
                    currentOut = tmp1;
                } else {
                    // tmp1 <-> tmp2 핑퐁
                    currentOut = (pass % 2 == 1) ? tmp2 : tmp1;
                }

                log.info("Global shuffle pass {}/{} : {} -> {}",
                        pass + 1, passes,
                        currentIn.getAbsolutePath(),
                        currentOut.getAbsolutePath());

                shuffleOnce(currentIn, currentOut, blockSize, pass);

                // 다음 pass의 입력 파일은 방금 만든 출력 파일
                currentIn = currentOut;
            }

        } catch (IOException e) {
            log.error("[ERROR] Failed to create temp files for multi-pass shuffle", e);
            throw new RuntimeException(e);
        } finally {
            if (tmp1 != null && tmp1.exists() && !tmp1.equals(targetFile)) {
                // 삭제 실패해도 무시
                //noinspection ResultOfMethodCallIgnored
                tmp1.delete();
            }
            if (tmp2 != null && tmp2.exists() && !tmp2.equals(targetFile)) {
                //noinspection ResultOfMethodCallIgnored
                tmp2.delete();
            }
        }
    }

    private void shuffleOnce(File sourceFile, File targetFile, int blockSize, int passIndex) {
        long fileSize = sourceFile.length();
        if (fileSize == 0L) {
            copyAsIs(sourceFile, targetFile);
            return;
        }

        long fullBlocks = fileSize / blockSize;
        int lastPartialBytes = (int) (fileSize % blockSize);

        if (fullBlocks == 0) {
            copyAsIs(sourceFile, targetFile);
            return;
        }

        // 🔹 패스마다 청크 바이트 크기를 살짝 다르게 (재현 가능하도록 Random 사용)
        Random passRand = new Random(RANDOM_SEED + 17L * passIndex);

        int maxBytes = MAX_CHUNK_BYTES;
        int minBytes = MIN_CHUNK_BYTES;

        if (minBytes < blockSize) {
            minBytes = blockSize;
        }
        if (maxBytes < minBytes) {
            maxBytes = minBytes;
        }

        int chunkBytesForThisPass;
        if (maxBytes == minBytes) {
            chunkBytesForThisPass = maxBytes;
        } else {
            int range = maxBytes - minBytes + 1;
            chunkBytesForThisPass = minBytes + passRand.nextInt(range);
        }

        long blocksPerChunkLong = chunkBytesForThisPass / blockSize;
        if (blocksPerChunkLong < 1) {
            blocksPerChunkLong = 1;
        }

        int blocksPerChunk = (int) Math.min(fullBlocks, blocksPerChunkLong);
        long chunkCountLong = (fullBlocks + blocksPerChunk - 1) / blocksPerChunk;
        int chunkCount = (int) chunkCountLong;

        log.info("[Pass {}] fileSize={} bytes, blockSize={} bytes", passIndex + 1, fileSize, blockSize);
        log.info("[Pass {}] fullBlocks={}, lastPartialBytes={}", passIndex + 1, fullBlocks, lastPartialBytes);
        log.info("[Pass {}] chunkBytes={} ({} blocksPerChunk), chunkCount={}",
                passIndex + 1, chunkBytesForThisPass, blocksPerChunk, chunkCount);

        // 청크 인덱스 0..chunkCount-1 셔플 (전역 순서 셔플)
        List<Integer> chunkOrder = new ArrayList<>(chunkCount);
        for (int i = 0; i < chunkCount; i++) {
            chunkOrder.add(i);
        }
        // passIndex 를 seed에 섞어서 pass마다 패턴을 다르게
        Collections.shuffle(chunkOrder, new Random(RANDOM_SEED + 31L * passIndex));

        try (RandomAccessFile raf = new RandomAccessFile(sourceFile, "r");
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(targetFile, false), BUFFERED_IO_SIZE))) {

            for (int orderIndex = 0; orderIndex < chunkOrder.size(); orderIndex++) {
                int chunkIndex = chunkOrder.get(orderIndex);

                long chunkStartBlock = (long) chunkIndex * blocksPerChunk;
                long remainingBlocks = fullBlocks - chunkStartBlock;
                if (remainingBlocks <= 0) {
                    continue;
                }

                int blocksInThisChunk = (int) Math.min(blocksPerChunk, remainingBlocks);
                int chunkBytes = blocksInThisChunk * blockSize;

                log.info("[Pass {}] Processing chunk {}/{} (chunkIndex={}, blocks={})",
                        passIndex + 1, chunkCount, orderIndex + 1, chunkIndex, blocksInThisChunk);

                // 1) 이 청크 영역을 메모리로 한 번에 읽기
                byte[] chunkBuffer = new byte[chunkBytes];
                long chunkStartPointer = chunkStartBlock * (long) blockSize;
                raf.seek(chunkStartPointer);
                readFully(raf, chunkBuffer, 0, chunkBytes);

                // 2) 청크 내부 블록 인덱스 0..blocksInThisChunk-1 셔플
                int[] blockOrder = new int[blocksInThisChunk];
                for (int i = 0; i < blocksInThisChunk; i++) {
                    blockOrder[i] = i;
                }
                shuffleArray(blockOrder, new Random(RANDOM_SEED + 131L * passIndex + chunkIndex));

                // 3) 셔플된 순서대로 out에 write
                for (int i = 0; i < blocksInThisChunk; i++) {
                    int blockIdx = blockOrder[i];
                    int offset = blockIdx * blockSize;
                    out.write(chunkBuffer, offset, blockSize);
                }
            }

            // 마지막 partial bytes가 있으면 그대로 뒤에 붙임
            if (lastPartialBytes > 0) {
                long tailPointer = fullBlocks * (long) blockSize;
                raf.seek(tailPointer);
                byte[] tail = new byte[lastPartialBytes];
                readFully(raf, tail, 0, lastPartialBytes);
                out.write(tail);
            }

            out.flush();
            out.close();
            raf.close();
        } catch (IOException e) {
            log.error("[ERROR] Failed to shuffle file (single pass)", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * RandomAccessFile.read()는 한 번에 len을 다 안 읽을 수 있으므로
     * len만큼 모두 채워질 때까지 반복해서 읽어준다.
     */
    private void readFully(RandomAccessFile raf, byte[] buf, int off, int len) throws IOException {
        int readTotal = 0;
        while (readTotal < len) {
            int read = raf.read(buf, off + readTotal, len - readTotal);
            if (read == -1) {
                throw new EOFException("Unexpected EOF while reading chunk");
            }
            readTotal += read;
        }
    }

    /**
     * int 배열용 Fisher–Yates 셔플
     */
    private void shuffleArray(int[] arr, Random random) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
    }

    private void copyAsIs(File source, File target) {
        try (InputStream in = new BufferedInputStream(new FileInputStream(source));
             OutputStream out = new BufferedOutputStream(new FileOutputStream(target, false))) {
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