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
    private File tempFile;
    private short header;
    private short blockSize;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;

    public GaiaPointCloudTemp(File file, short blockSize) throws FileNotFoundException {
        this.tempFile = file;
        this.blockSize = blockSize;
    }

    public void writeHeader() {
        try {
            outputStream.writeShort(header);
            outputStream.writeShort(blockSize);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void write(Vector3d position, byte[] bytes) {
        try {
            outputStream.writeDouble(position.x);
            outputStream.writeDouble(position.y);
            outputStream.writeDouble(position.z);
            outputStream.write(bytes);
        } catch (IOException e) {
            log.error("Failed to write bytes to output stream", e);
        }
    }

    public boolean readHeader() {
        try {
            inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(this.tempFile)));
            this.header = inputStream.readShort();
            this.blockSize = inputStream.readShort();
            return true;
        } catch (IOException e) {
            log.error("Failed to read header from input stream", e);
            return false;
        }
    }

    public List<GaiaVertex> readTemp() {
        List<GaiaVertex> vertices = new ArrayList<>();
        try {
            while (inputStream.available() > 0) {
                double x = inputStream.readDouble();
                double y = inputStream.readDouble();
                double z = inputStream.readDouble();
                byte[] bytes = new byte[3];
                inputStream.read(bytes);

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
