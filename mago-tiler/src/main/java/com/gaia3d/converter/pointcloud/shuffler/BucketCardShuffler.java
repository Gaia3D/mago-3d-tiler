package com.gaia3d.converter.pointcloud.shuffler;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Slf4j
public class BucketCardShuffler implements Shuffler  {
    private static final int RANDOM_SEED = 8291;
    // 동시에 열 버킷 개수 (너무 크게 하면 OS 파일 핸들 제한에 걸릴 수 있음)
    private static final int DEFAULT_BUCKET_COUNT = 256;

    /**
     * 기본 셔플: 버킷 기반 스트리밍 셔플
     */
    public void shuffle(File sourceFile, File targetFile, int blockSize) {
        shuffle(sourceFile, targetFile, blockSize, DEFAULT_BUCKET_COUNT);
    }

    /**
     * 버킷 개수를 지정해서 셔플
     */
    public void shuffle(File sourceFile, File targetFile, int blockSize, int bucketCount) {
        if (bucketCount <= 0) {
            bucketCount = DEFAULT_BUCKET_COUNT;
        }

        long fileSize = sourceFile.length();
        if (fileSize == 0L) {
            log.warn("Source file is empty. Nothing to shuffle.");
            copyAsIs(sourceFile, targetFile);
            return;
        }

        log.info("Start streaming bucket shuffle: fileSize={}, blockSize={}, buckets={}",
                fileSize, blockSize, bucketCount);

        File tempDir = targetFile.getParentFile();
        if (tempDir == null) {
            tempDir = new File(".");
        }

        File[] bucketFiles = new File[bucketCount];
        DataOutputStream[] bucketOuts = new DataOutputStream[bucketCount];

        try {
            // 1) 버킷 파일들 만들고 출력 스트림 열기
            for (int i = 0; i < bucketCount; i++) {
                bucketFiles[i] = File.createTempFile("shuffle_bucket_" + i + "_", ".bin", tempDir);
                bucketOuts[i] = new DataOutputStream(
                        new BufferedOutputStream(new FileOutputStream(bucketFiles[i]), 8192 * 8)
                );
            }

            // 2) source를 순차적으로 읽으면서 각 블록을 랜덤 버킷에 던져 넣기
            Random random = new Random(RANDOM_SEED);
            try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(sourceFile), 8192 * 8)) {

                byte[] blockBuf = new byte[blockSize];
                long blockIndex = 0;

                while (true) {
                    int readTotal = 0;
                    while (readTotal < blockSize) {
                        int read = in.read(blockBuf, readTotal, blockSize - readTotal);
                        if (read == -1) {
                            break;
                        }
                        readTotal += read;
                    }
                    if (readTotal == 0) {
                        // EOF
                        break;
                    }

                    // 마지막 partial block도 섞어서 넣고 싶으면 이대로 사용
                    // 만약 항상 blockSize 정렬만 있다고 가정하면,
                    // readTotal != blockSize 인 경우는 에러로 처리해도 됨.
                    int bucketIndex = random.nextInt(bucketCount);
                    bucketOuts[bucketIndex].write(blockBuf, 0, readTotal);

                    blockIndex++;
                    if (blockIndex % 1_000_000 == 0) {
                        log.info("Assigned {} blocks to buckets...", blockIndex);
                    }

                    if (readTotal < blockSize) {
                        // 마지막 partial block 처리 후 EOF
                        break;
                    }
                }

                log.info("Total blocks assigned to buckets: {}", blockIndex);
            }

            // 3) 버킷 스트림 닫기
            for (int i = 0; i < bucketCount; i++) {
                if (bucketOuts[i] != null) {
                    bucketOuts[i].flush();
                    bucketOuts[i].close();
                }
            }

            // 4) 버킷 순서 셔플 후, 그 순서대로 targetFile에 이어붙이기
            List<Integer> bucketOrder = new ArrayList<>(bucketCount);
            for (int i = 0; i < bucketCount; i++) {
                bucketOrder.add(i);
            }
            Collections.shuffle(bucketOrder, new Random(RANDOM_SEED * 31L + 7L));

            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile, false), 8192 * 8)) {
                byte[] copyBuf = new byte[8192 * 16];

                long totalWritten = 0L;
                int orderIdx = 0;

                for (int bucketIdx : bucketOrder) {
                    File bucketFile = bucketFiles[bucketIdx];
                    if (bucketFile == null || !bucketFile.exists() || bucketFile.length() == 0L) {
                        orderIdx++;
                        continue;
                    }

                    log.info("Merging bucket {}/{} (index={}, size={} bytes)",
                            orderIdx + 1, bucketCount, bucketIdx, bucketFile.length());

                    try (BufferedInputStream bin = new BufferedInputStream(new FileInputStream(bucketFile), 8192 * 8)) {
                        int read;
                        while ((read = bin.read(copyBuf)) != -1) {
                            out.write(copyBuf, 0, read);
                            totalWritten += read;
                        }
                    }

                    orderIdx++;
                }

                out.flush();
                log.info("Finished merging buckets. Total written: {} bytes", totalWritten);
            }

        } catch (IOException e) {
            log.error("[ERROR] Failed to shuffle with bucket streaming", e);
            throw new RuntimeException(e);
        } finally {
            // 5) 버킷 임시 파일 정리
            for (int i = 0; i < bucketCount; i++) {
                if (bucketOuts[i] != null) {
                    try {
                        bucketOuts[i].close();
                    } catch (IOException ignore) {}
                }
                if (bucketFiles[i] != null && bucketFiles[i].exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    bucketFiles[i].delete();
                }
            }
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