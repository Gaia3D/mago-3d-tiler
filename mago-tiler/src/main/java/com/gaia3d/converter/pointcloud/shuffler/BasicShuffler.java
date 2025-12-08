package com.gaia3d.converter.pointcloud.shuffler;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Slf4j
public class BasicShuffler implements Shuffler {

    private static final int RANDOM_SEED = 8291;
    private static List<Integer> SHUFFLE_INDEXES = null;

    /**
     * Shuffles the temp file
     */
    public void shuffle(File sourceFile, File targetFile, int blockSize) {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(sourceFile, "r"); DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(targetFile, false), 8192 * 8))) {
            long loopSize = ((randomAccessFile.length()) / blockSize);

            int shuffleBufferSize = 65536 * 8;
            long shuffleCount = loopSize / shuffleBufferSize;
            long remainder = loopSize % shuffleBufferSize;
            if (remainder > 0) {
                shuffleCount += 1;
            }
            List<Integer> indexes;
            if (SHUFFLE_INDEXES == null) {
                SHUFFLE_INDEXES = createShuffleIndexes(shuffleBufferSize);
            }
            indexes = SHUFFLE_INDEXES;

            byte[] bytes = new byte[blockSize];
            long fileSize = randomAccessFile.length();
            //for (Integer integer : indexes) {
            for (int index = 0; index < indexes.size(); index++) {
                Integer integer = indexes.get(index);
                for (int count = 0; count < shuffleCount; count++) {
                    if (index % 1000 == 0 && count == 0) {
                        log.info("Shuffling block: {} / {}", index, indexes.size());
                    }
                    long accessAddress = (long) shuffleBufferSize * count + integer;
                    long pointer = accessAddress * blockSize;
                    if ((pointer) <= fileSize) {
                        randomAccessFile.seek(pointer);
                        if ((pointer + blockSize) > fileSize) {
                            int lastBlockSize = (int) (fileSize - pointer);
                            byte[] lastBytes = new byte[lastBlockSize];
                            randomAccessFile.read(lastBytes);
                            dataOutputStream.write(lastBytes);
                        } else {
                            randomAccessFile.read(bytes);
                            dataOutputStream.write(bytes);
                        }
                    }
                }
            }
            randomAccessFile.close();
            dataOutputStream.flush();
            dataOutputStream.close();
        } catch (IOException e) {
            log.error("[ERROR] Failed to shuffle temp file", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clear() {

    }

    private List<Integer> createShuffleIndexes(int loop) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < loop; i++) {
            indexes.add(i);
        }
        Collections.shuffle(indexes, new Random(RANDOM_SEED));
        return indexes;
    }
}
