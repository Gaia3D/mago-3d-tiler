package com.gaia3d.converter.pointcloud.shuffler;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Slf4j
public class BucketShuffler implements Shuffler {

    private static final int RANDOM_SEED = 8291;

    // 버킷 하나에서 쓸 셔플 패턴
    private static volatile int[] SHUFFLE_TEMPLATE;

    private static int[] getShuffleTemplate(int size) {
        int[] local = SHUFFLE_TEMPLATE;
        if (local == null || local.length != size) {
            synchronized (BasicShuffler.class) {
                local = SHUFFLE_TEMPLATE;
                if (local == null || local.length != size) {
                    local = createShuffleTemplate(size);
                    SHUFFLE_TEMPLATE = local;
                    log.info("Created shuffle template, size={}", size);
                }
            }
        }
        return local;
    }

    private static int[] createShuffleTemplate(int size) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = i;
        }
        Random random = new Random(RANDOM_SEED);
        // Fisher-Yates
        for (int i = size - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
        return arr;
    }

    /**
     * High-performance bucket-based shuffle.
     */
    public void shuffle(File sourceFile, File targetFile, int blockSize) {
        long fileSize = sourceFile.length();
        long blockCount = fileSize / blockSize;
        boolean hasPartial = (fileSize % blockSize) != 0;
        if (hasPartial) {
            blockCount += 1;
        }

        int blocksPerBucket = (100 * 1024 * 1024) / blockSize;
        if (blocksPerBucket < 1) blocksPerBucket = 1;

        long bucketCount = (blockCount + blocksPerBucket - 1) / blocksPerBucket;

        // 버킷 순서만 Collections.shuffle (이건 개수가 적어서 싸다)
        List<Integer> bucketOrder = new ArrayList<>();
        for (int i = 0; i < bucketCount; i++) {
            bucketOrder.add(i);
        }
        Collections.shuffle(bucketOrder, new Random(RANDOM_SEED));

        // 여기서 한 번만 템플릿 만들어서 재사용
        int[] shuffleTemplate = getShuffleTemplate(blocksPerBucket);

        try (RandomAccessFile raf = new RandomAccessFile(sourceFile, "r");
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(targetFile, false), 8192 * 8))) {

            byte[] blockBuf = new byte[blockSize];

            for (int bucketIdx : bucketOrder) {

                long startBlock = (long) bucketIdx * blocksPerBucket;
                long endBlock = Math.min(startBlock + blocksPerBucket, blockCount);
                int bucketBlockSize = (int) (endBlock - startBlock);

                log.info("Shuffling bucket {} / {} ({} blocks)",
                        bucketIdx + 1, bucketCount, bucketBlockSize);

                if (bucketBlockSize == blocksPerBucket) {
                    // ✅ 풀 버킷: 템플릿 그대로 사용
                    for (int i = 0; i < blocksPerBucket; i++) {
                        int idx = shuffleTemplate[i];
                        long globalBlock = startBlock + idx;
                        long pointer = globalBlock * blockSize;
                        if (pointer >= fileSize) {
                            continue;
                        }
                        raf.seek(pointer);
                        if (pointer + blockSize > fileSize) {
                            int lastSize = (int) (fileSize - pointer);
                            byte[] lastBytes = new byte[lastSize];
                            int read = raf.read(lastBytes);
                            if (read != lastSize) {
                                throw new IOException("Partial block read mismatch");
                            }
                            out.write(lastBytes);
                        } else {
                            int read = raf.read(blockBuf);
                            if (read != blockSize) {
                                throw new IOException("Full block read mismatch");
                            }
                            out.write(blockBuf);
                        }
                    }
                } else {
                    // ✅ 마지막 작은 버킷: 템플릿에서 bucketBlockSize만큼 골라쓰기
                    // 0..blocksPerBucket-1에서 shuffle된 템플릿 중,
                    // 값이 bucketBlockSize보다 작은 것만 순서대로 뽑으면
                    // 0..bucketBlockSize-1의 랜덤 순열이 된다.
                    int picked = 0;
                    for (int i = 0; i < blocksPerBucket && picked < bucketBlockSize; i++) {
                        int idx = shuffleTemplate[i];
                        if (idx >= bucketBlockSize) {
                            continue;
                        }
                        long globalBlock = startBlock + idx;
                        long pointer = globalBlock * blockSize;
                        if (pointer >= fileSize) {
                            continue;
                        }
                        raf.seek(pointer);
                        if (pointer + blockSize > fileSize) {
                            int lastSize = (int) (fileSize - pointer);
                            byte[] lastBytes = new byte[lastSize];
                            int read = raf.read(lastBytes);
                            if (read != lastSize) {
                                throw new IOException("Partial block read mismatch (tail bucket)");
                            }
                            out.write(lastBytes);
                        } else {
                            int read = raf.read(blockBuf);
                            if (read != blockSize) {
                                throw new IOException("Full block read mismatch (tail bucket)");
                            }
                            out.write(blockBuf);
                        }
                        picked++;
                    }
                }
            }

            out.flush();
            out.close();
            raf.close();
        } catch (IOException e) {
            log.error("[ERROR] Failed to shuffle temp file", e);
            throw new RuntimeException(e);
        }
    }
}
