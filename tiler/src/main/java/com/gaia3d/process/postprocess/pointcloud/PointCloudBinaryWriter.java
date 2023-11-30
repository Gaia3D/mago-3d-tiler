package com.gaia3d.process.postprocess.pointcloud;

import com.gaia3d.util.io.LittleEndianDataOutputStream;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;

@Slf4j
public class PointCloudBinaryWriter {
    private static final String MAGIC = "pnts";
    private static final int VERSION = 1;
    private static final int HEADER_SIZE = 28;

    private final String featureTableJson;
    private final String batchTableJson;

    private final byte[] featureTableBytes;
    private final byte[] batchTableBytes;

    public PointCloudBinaryWriter(String featureTable, String batchTable, byte[] featureTableBytes, byte[] batchTableBytes) {
        this.featureTableJson = featureTable;
        this.batchTableJson = batchTable;
        this.featureTableBytes = featureTableBytes;
        this.batchTableBytes = batchTableBytes;
    }

    public void write(Path outputRoot, String nodeCode) {
        int featureTableJSONByteLength = featureTableJson.length();
        int batchTableJSONByteLength = batchTableJson.length();

        int featureTableBinaryByteLength = featureTableBytes.length;
        int batchTableBinaryByteLength = batchTableBytes.length;

        int byteLength = HEADER_SIZE + featureTableJSONByteLength + batchTableJSONByteLength + featureTableBinaryByteLength + batchTableBinaryByteLength;

        File b3dmOutputFile = outputRoot.resolve(nodeCode + "." + MAGIC).toFile();
        try (LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(new BufferedOutputStream(new FileOutputStream(b3dmOutputFile)))) {
            // 28-byte header (first 20 bytes)
            stream.writePureText(MAGIC);
            stream.writeInt(VERSION);
            stream.writeInt(byteLength);
            stream.writeInt(featureTableJSONByteLength);
            stream.writeInt(featureTableBinaryByteLength);

            // 28-byte header (next 8 bytes)
            stream.writeInt(batchTableJSONByteLength);
            stream.writeInt(batchTableBinaryByteLength);

            // body
            stream.writePureText(featureTableJson);
            stream.write(featureTableBytes);

            stream.writePureText(batchTableJson);
            stream.write(batchTableBytes);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
