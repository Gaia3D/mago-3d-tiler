package com.gaia3d.basic.pointcloud;

import com.gaia3d.basic.model.GaiaVertex;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.joml.Vector3d;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Setter
@Getter
@Slf4j
public class GaiaPointCloudTemp {
    private final short VERSION = 1106;
    private final int BUFFER_SIZE = 8192; // 8KB
    private final int RANDOM_SEED = 42;

    private static List<Integer> SHUFFLE_INDEXES = null;

    /* Header Total Size 52 byte */
    private final short HEADER_SIZE = 52; // 2 (Version) + 2 (Block Size) + 24 (Quantized Volume Scale) + 24 (Quantized Volume Offset)
    private final short BLOCK_SIZE = 16; // 12 (FLOAT XYZ) + 3 (RGB) + 1 (Padding)
    private final double[] quantizedVolumeScale = new double[3];
    private final double[] quantizedVolumeOffset = new double[3];

    private File tempFile;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;

    public GaiaPointCloudTemp(File file) {
        this.tempFile = file;
    }

    public boolean readHeader() {
        try {
            this.inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(this.tempFile), BUFFER_SIZE));
            /* header total size = 2 + 2 + 24 + 24 = 52 bytes */
            // version 2 bytes
            if (this.VERSION != inputStream.readShort()) {
                log.error("Invalid Pointscloud temp version");
                return false;
            }
            // block size 2 bytes
            if (this.BLOCK_SIZE != inputStream.readShort()) {
                log.error("Invalid block size");
                return false;
            }
            // quantized volume scale (double xyz) 24 bytes
            this.quantizedVolumeScale[0] = inputStream.readDouble();
            this.quantizedVolumeScale[1] = inputStream.readDouble();
            this.quantizedVolumeScale[2] = inputStream.readDouble();
            // quantized volume offset (double xyz) 24 bytes
            this.quantizedVolumeOffset[0] = inputStream.readDouble();
            this.quantizedVolumeOffset[1] = inputStream.readDouble();
            this.quantizedVolumeOffset[2] = inputStream.readDouble();
            return true;
        } catch (IOException e) {
            log.error("Failed to read header from input stream", e);
            return false;
        }
    }

    public void writeHeader() {
        try {
            if (this.tempFile.exists()) {
                this.tempFile.delete();
                log.info("Deleted existing temp file: {}", this.tempFile.getAbsolutePath());
            }

            outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(this.tempFile, true), BUFFER_SIZE));
            /* header total size = 2 + 2 + 24 + 24 = 52 bytes */
            // version 2 bytes
            outputStream.writeShort(VERSION);
            // block size 2 bytes
            outputStream.writeShort(BLOCK_SIZE);
            // quantized volume scale (double xyz) 24 bytes
            outputStream.writeDouble(quantizedVolumeScale[0]);
            outputStream.writeDouble(quantizedVolumeScale[1]);
            outputStream.writeDouble(quantizedVolumeScale[2]);
            // quantized volume offset (double xyz) 24 bytes
            outputStream.writeDouble(quantizedVolumeOffset[0]);
            outputStream.writeDouble(quantizedVolumeOffset[1]);
            outputStream.writeDouble(quantizedVolumeOffset[2]);
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /*public List<GaiaVertex> readTempChunk(int chunkSize) {
        List<GaiaVertex> result = new ArrayList<>();
        chunkSize = chunkSize - (chunkSize % BLOCK_SIZE);
        try {
            int availableSize = inputStream.available();
            if (availableSize < chunkSize) {
                chunkSize = availableSize;
                //return result;
            } else if (availableSize == 0) {
                log.info("End of file");
                return result;
            }
            byte[] bytes = new byte[chunkSize];
            inputStream.read(bytes);
            for (int i = 0; i < chunkSize; i += BLOCK_SIZE) {
                float floatX = ByteBuffer.wrap(bytes, i, 4).order(ByteOrder.BIG_ENDIAN).getFloat();
                float floatY = ByteBuffer.wrap(bytes, i + 4, 4).order(ByteOrder.BIG_ENDIAN).getFloat();
                float floatZ = ByteBuffer.wrap(bytes, i + 8, 4).order(ByteOrder.BIG_ENDIAN).getFloat();
                byte[] color = Arrays.copyOfRange(bytes, i + 12, i + 15);

                double x = floatX * quantizedVolumeScale[0] + quantizedVolumeOffset[0];
                double y = floatY * quantizedVolumeScale[1] + quantizedVolumeOffset[1];
                double z = floatZ * quantizedVolumeScale[2] + quantizedVolumeOffset[2];

                GaiaVertex vertex = new GaiaVertex();
                vertex.setColor(color);
                vertex.setPosition(new Vector3d(x, y, z));
                result.add(vertex);
            }
        } catch (IOException e) {
            log.error("Failed to read temp from input stream", e);
            throw new RuntimeException(e);
        }
        return result;
    }*/

    /*public List<GaiaVertex> readTempFast() {
        List<GaiaVertex> vertices = new ArrayList<>();
        try {
            int availableSize = inputStream.available();
            log.info("Available size: {}", availableSize);
            byte[] bytes = new byte[availableSize];
            inputStream.read(bytes);
            for (int i = 0; i < availableSize; i += BLOCK_SIZE) {
                float floatX = ByteBuffer.wrap(bytes, i, 4).order(ByteOrder.BIG_ENDIAN).getFloat();
                float floatY = ByteBuffer.wrap(bytes, i + 4, 4).order(ByteOrder.BIG_ENDIAN).getFloat();
                float floatZ = ByteBuffer.wrap(bytes, i + 8, 4).order(ByteOrder.BIG_ENDIAN).getFloat();
                byte[] color = Arrays.copyOfRange(bytes, i + 12, i + 15);

                double x = floatX * quantizedVolumeScale[0] + quantizedVolumeOffset[0];
                double y = floatY * quantizedVolumeScale[1] + quantizedVolumeOffset[1];
                double z = floatZ * quantizedVolumeScale[2] + quantizedVolumeOffset[2];

                GaiaVertex vertex = new GaiaVertex();
                vertex.setColor(color);
                vertex.setPosition(new Vector3d(x, y, z));
                vertices.add(vertex);
            }
        } catch (IOException e) {
            log.error("Failed to read temp from input stream", e);
            throw new RuntimeException(e);
        }
        return vertices;
    }*/

    public List<GaiaVertex> readTemp() {
        List<GaiaVertex> vertices = new ArrayList<>();
        try {
            while (inputStream.available() > 0) {
                float floatX = inputStream.readFloat();
                float floatY = inputStream.readFloat();
                float floatZ = inputStream.readFloat();
                byte[] bytes = new byte[3];
                inputStream.read(bytes);
                inputStream.readByte(); // padding

                /*if (floatX < 0) {
                    floatX = 0;
                } else if (floatX > 1) {
                    floatX = 1;
                }*/
                double x = floatX * quantizedVolumeScale[0] + quantizedVolumeOffset[0];

                /*if (floatY < 0) {
                    floatY = 0;
                } else if (floatY > 1) {
                    floatY = 1;
                }*/
                double y = floatY * quantizedVolumeScale[1] + quantizedVolumeOffset[1];

                /*if (floatZ < 0) {
                    floatZ = 0;
                } else if (floatZ > 1) {
                    floatZ = 1;
                }*/
                double z = floatZ * quantizedVolumeScale[2] + quantizedVolumeOffset[2];

                GaiaVertex vertex = new GaiaVertex();
                vertex.setColor(bytes);
                vertex.setPosition(new Vector3d(x, y, z));
                vertices.add(vertex);
            }
        } catch (IOException e) {
            log.error("Failed to read temp from input stream", e);
        }
        return vertices;
    }

    public void writePositionsFast(List<GaiaVertex> vertices) {
        try {
            int size = vertices.size() * BLOCK_SIZE;
            byte[] bytes = new byte[size];
            int index = 0;
            for (GaiaVertex vertex : vertices) {
                Vector3d position = vertex.getPosition();
                byte[] color = vertex.getColor();
                float x = (float) ((position.x - quantizedVolumeOffset[0]) / quantizedVolumeScale[0]);
                float y = (float) ((position.y - quantizedVolumeOffset[1]) / quantizedVolumeScale[1]);
                float z = (float) ((position.z - quantizedVolumeOffset[2]) / quantizedVolumeScale[2]);

                // XYZ
                byte[] xBytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(x).array();
                byte[] yBytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(y).array();
                byte[] zBytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(z).array();
                System.arraycopy(xBytes, 0, bytes, index, 4);
                System.arraycopy(yBytes, 0, bytes, index + 4, 4);
                System.arraycopy(zBytes, 0, bytes, index + 8, 4);
                // RGB
                System.arraycopy(color, 0, bytes, index + 12, 3);
                // padding
                bytes[index + 15] = 0;
                index += BLOCK_SIZE;
            }
            outputStream.write(bytes);
        } catch (Exception e) {
            log.error("Failed to write positions to output stream", e);
        }
    }

    public void writePosition(Vector3d position, byte[] bytes) {
        try {
            float x = (float) ((position.x - quantizedVolumeOffset[0]) / quantizedVolumeScale[0]);
            float y = (float) ((position.y - quantizedVolumeOffset[1]) / quantizedVolumeScale[1]);
            float z = (float) ((position.z - quantizedVolumeOffset[2]) / quantizedVolumeScale[2]);

            // XYZ
            outputStream.writeFloat(x);
            outputStream.writeFloat(y);
            outputStream.writeFloat(z);
            // RGB
            outputStream.write(bytes);
            // padding
            outputStream.writeByte(0);
        } catch (IOException e) {
            log.error("Failed to write bytes to output stream", e);
        }
    }

    /**
     * Shuffles the temp file
     */
    /*public void shuffleTemp(int limitSize) {
        String fileName = "shuffled-" + this.tempFile.getName();
        File shuffledFile = new File(this.tempFile.getParent(), fileName);
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(this.tempFile, "r");
            DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(shuffledFile, false), BUFFER_SIZE));
            int headerSize = 52;
            int blockCount = (int) ((randomAccessFile.length() - headerSize) / BLOCK_SIZE);
            int loop = blockCount;
            if (loop > limitSize) {
                loop = limitSize;
            }

            int calcSize = headerSize + (loop * BLOCK_SIZE);
            if (shuffledFile.exists() && shuffledFile.length() == calcSize) {
                log.info("Temp file already exists : {}", shuffledFile.getAbsolutePath());
                return;
            } else {
                log.info("size diff => {} : {}", shuffledFile.length(), calcSize);
            }

            // Read header
            short version = randomAccessFile.readShort();
            short blockSize = randomAccessFile.readShort();
            double[] volumeScale = new double[3];
            volumeScale[0] = randomAccessFile.readDouble();
            volumeScale[1] = randomAccessFile.readDouble();
            volumeScale[2] = randomAccessFile.readDouble();
            double[] volumeOffset = new double[3];
            volumeOffset[0] = randomAccessFile.readDouble();
            volumeOffset[1] = randomAccessFile.readDouble();
            volumeOffset[2] = randomAccessFile.readDouble();

            // Write header
            dataOutputStream.writeShort(version);
            dataOutputStream.writeShort(blockSize);
            dataOutputStream.writeDouble(volumeScale[0]);
            dataOutputStream.writeDouble(volumeScale[1]);
            dataOutputStream.writeDouble(volumeScale[2]);
            dataOutputStream.writeDouble(volumeOffset[0]);
            dataOutputStream.writeDouble(volumeOffset[1]);
            dataOutputStream.writeDouble(volumeOffset[2]);

            List<Integer> indexes = new ArrayList<>();
            for (int i = 0; i < blockCount; i++) {
                indexes.add(i);
            }
            Collections.shuffle(indexes);

            byte[] bytes = new byte[blockSize];
            log.info("- Shuffling points limit {}/{} ({})%", loop, blockCount, (blockCount < 0) ? "original" : (loop * 100 / blockCount));
            for (int i = 0; i < loop; i++) {
                randomAccessFile.seek(headerSize + (indexes.get(i) * blockSize));
                randomAccessFile.read(bytes);
                dataOutputStream.write(bytes);
            }
            randomAccessFile.close();
            dataOutputStream.close();
            this.tempFile.delete();
            this.tempFile = shuffledFile;
        } catch (FileNotFoundException e) {
            log.error("Failed to shuffle temp file", e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("Failed to shuffle temp file", e);
            throw new RuntimeException(e);
        }
    }*/

    /**
     * Shuffles the temp file
     */
    public void shuffleTempMoreFast(int shuffleNumber, int shuffleLength) {
        String fileName = "shuffled-" + this.tempFile.getName();
        File shuffledFile = new File(this.tempFile.getParent(), fileName);
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(this.tempFile, "rw");
            DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(shuffledFile, false), BUFFER_SIZE));
            int headerSize = 52;
            int blockCount = (int) ((randomAccessFile.length() - headerSize) / BLOCK_SIZE);
            int loop = blockCount;

            /*int calcSize = headerSize + (loop * BLOCK_SIZE);
            if (shuffledFile.exists() && shuffledFile.length() == calcSize) {
                log.info("Temp file already exists : {}", shuffledFile.getAbsolutePath());
                return;
            } else {
                log.info("size diff => {} : {}", shuffledFile.length(), calcSize);
            }*/

            // Read header
            short version = randomAccessFile.readShort();
            short blockSize = randomAccessFile.readShort();
            double[] volumeScale = new double[3];
            volumeScale[0] = randomAccessFile.readDouble();
            volumeScale[1] = randomAccessFile.readDouble();
            volumeScale[2] = randomAccessFile.readDouble();
            double[] volumeOffset = new double[3];
            volumeOffset[0] = randomAccessFile.readDouble();
            volumeOffset[1] = randomAccessFile.readDouble();
            volumeOffset[2] = randomAccessFile.readDouble();

            // Write header
            dataOutputStream.writeShort(version);
            dataOutputStream.writeShort(blockSize);
            dataOutputStream.writeDouble(volumeScale[0]);
            dataOutputStream.writeDouble(volumeScale[1]);
            dataOutputStream.writeDouble(volumeScale[2]);
            dataOutputStream.writeDouble(volumeOffset[0]);
            dataOutputStream.writeDouble(volumeOffset[1]);
            dataOutputStream.writeDouble(volumeOffset[2]);


            int shuffleBufferSize = 65536 * 8;
            int shuffleCount = loop / shuffleBufferSize;
            int remainder = loop % shuffleBufferSize;
            if (remainder > 0) {
                shuffleCount += 1;
            }
            List<Integer> indexes;
            if (SHUFFLE_INDEXES == null) {
                SHUFFLE_INDEXES = createShuffleIndexes(shuffleBufferSize);
            }
            indexes = SHUFFLE_INDEXES;

            //int shuffleBlockSize = blockSize * shuffleCount;
            byte[] bytes = new byte[blockSize];
            //byte[] shuffleBytes = new byte[shuffleBlockSize];

            log.info("[Pre][{}/{}][Shuffle] TotalPoints: {}, shuffleBufferSize: {}, shuffleCount: {}, blockSize: {}", shuffleNumber, shuffleLength, loop, shuffleBufferSize, shuffleCount, blockSize);

            /*log.info("- Shuffling points limit {}/{} ({})%", loop, blockCount, (blockCount < 0) ? "original" : (loop * 100 / blockCount));
            log.info("- Shuffle Buffer Size: {}", shuffleBufferSize);
            log.info("- Shuffle Count: {}", shuffleCount);
            log.info("- Shuffle block size: {}", blockSize);
            log.info("- loop / shuffleCount : {}", loop / shuffleBufferSize);
            log.info("- loop % shuffleCount : {}", loop % shuffleBufferSize);
            log.info("- total loop : {}", shuffleBufferSize * shuffleCount);*/

            int fileSize = (int) randomAccessFile.length();
            for (Integer integer : indexes) {
                for (int count = 0; count < shuffleCount; count++) {
                    int index = (shuffleBufferSize * count) + integer;
                    int pointer = headerSize + (index * blockSize);
                    if ((pointer) <= fileSize) {
                        randomAccessFile.seek(pointer);
                        randomAccessFile.read(bytes);
                        dataOutputStream.write(bytes);
                    }
                }
            }
            dataOutputStream.flush();
            dataOutputStream.close();
            randomAccessFile.close();
            FileUtils.deleteQuietly(this.tempFile);
            this.tempFile = shuffledFile;
        } catch (FileNotFoundException e) {
            log.error("Failed to shuffle temp file", e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("Failed to shuffle temp file", e);
            throw new RuntimeException(e);
        }
    }

    private List<Integer> createShuffleIndexes(int loop) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < loop; i++) {
            indexes.add(i);
        }
        Collections.shuffle(indexes, new Random(RANDOM_SEED));
        return indexes;
    }

    // 두 블록을 교환하는 메서드
    private void swapBlocks(FileChannel channel, long index1, long index2, int headerSize, int blockSize) throws IOException {
        ByteBuffer buffer1 = ByteBuffer.allocate(blockSize);
        ByteBuffer buffer2 = ByteBuffer.allocate(blockSize);

        // 첫 번째 블록 읽기
        channel.position(headerSize + (index1 * blockSize));
        channel.read(buffer1);
        //buffer1.flip();

        // 두 번째 블록 읽기
        channel.position(headerSize + (index2 * blockSize));
        channel.read(buffer2);
        //buffer2.flip();

        // 첫 번째 블록을 두 번째 위치에 쓰기
        channel.position(headerSize + (index2 * blockSize));
        channel.write(buffer1);

        // 두 번째 블록을 첫 번째 위치에 s쓰기
        channel.position(headerSize + (index1 * blockSize));
        channel.write(buffer2);
    }

}
