package com.gaia3d.basic.pointcloud;

import com.gaia3d.basic.model.GaiaVertex;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Slf4j
public class GaiaPointCloudTemp {
    private final File tempFile;
    private final short VERSION = 1106;
    private final short BLOCK_SIZE = 16; // 12 (FLOAT XYZ) + 3 (RGB) + 1 (Padding)
    private final double[] quantizedVolumeScale = new double[3];
    private final double[] quantizedVolumeOffset = new double[3];
    private DataOutputStream outputStream;
    private DataInputStream inputStream;

    public GaiaPointCloudTemp(File file) throws FileNotFoundException {
        this.tempFile = file;
    }

    public boolean readHeader() {
        try {
            this.inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(this.tempFile)));
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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

                double x = floatX * quantizedVolumeScale[0] + quantizedVolumeOffset[0];
                double y = floatY * quantizedVolumeScale[1] + quantizedVolumeOffset[1];
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

    public void writePosition(Vector3d position, byte[] bytes) {
        try {
            // XYZ
            outputStream.writeFloat((float) ((position.x - quantizedVolumeOffset[0]) / quantizedVolumeScale[0]));
            outputStream.writeFloat((float) ((position.y - quantizedVolumeOffset[1]) / quantizedVolumeScale[1]));
            outputStream.writeFloat((float) ((position.z - quantizedVolumeOffset[2]) / quantizedVolumeScale[2]));
            // RGB
            outputStream.write(bytes);
            // padding
            outputStream.writeByte(0);
        } catch (IOException e) {
            log.error("Failed to write bytes to output stream", e);
        }
    }

    public void openOutputStream() throws FileNotFoundException {
        if (outputStream == null) {
            outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(this.tempFile, true)));
        }
    }

    public void closeInputStream() {
        try {
            inputStream.close();
        } catch (IOException e) {
            log.error("Failed to close output stream", e);
        }
    }

    public void closeSteam() {
        try {
            outputStream.close();
        } catch (IOException e) {
            log.error("Failed to close input stream", e);
        }
    }
}
