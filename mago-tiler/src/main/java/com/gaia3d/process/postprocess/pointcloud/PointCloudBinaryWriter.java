package com.gaia3d.process.postprocess.pointcloud;

import com.gaia3d.io.LittleEndianDataOutputStream;
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

        boolean isFeatureTableAligned = (HEADER_SIZE + featureTableJSONByteLength) % 8 == 0;
        int featureTablePadLength = 0;
        if (!isFeatureTableAligned) {
            featureTablePadLength = 8 - ((HEADER_SIZE + featureTableJSONByteLength) % 8);
            byteLength += featureTablePadLength;
            featureTableJSONByteLength += featureTablePadLength;
        }

        boolean featureTableBinaryAligned = featureTableBinaryByteLength % 8 == 0;
        int featureTableBinaryPadLength = 0;
        if (!featureTableBinaryAligned) {
            featureTableBinaryPadLength = 8 - (featureTableBinaryByteLength % 8);
            byteLength += featureTableBinaryPadLength;
            featureTableBinaryByteLength += featureTableBinaryPadLength;
        }

        boolean isBatchTableBinaryAligned = batchTableBinaryByteLength % 8 == 0;
        int batchTableBinaryPadLength = 0;
        if (!isBatchTableBinaryAligned) {
            batchTableBinaryPadLength = 8 - (batchTableBinaryByteLength % 8);
            byteLength += batchTableBinaryPadLength;
            batchTableBinaryByteLength += batchTableBinaryPadLength;
        }

        boolean isFileAligned = byteLength % 8 == 0;
        int lastPadLength = 0;
        if (!isFileAligned) {
            lastPadLength = 8 - (byteLength % 8);
            byteLength += lastPadLength;
        }

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
            if (featureTablePadLength > 0) {
                byte[] featureTablePadding = new byte[featureTablePadLength];
                for (int i = 0; i < featureTablePadLength; i++) {
                    featureTablePadding[i] = 0x20;
                }
                stream.write(featureTablePadding);
            }
            stream.write(featureTableBytes);
            if (featureTableBinaryPadLength > 0) {
                byte[] featureTableBinaryPadding = new byte[featureTableBinaryPadLength];
                for (int i = 0; i < featureTableBinaryPadLength; i++) {
                    featureTableBinaryPadding[i] = 0x00;
                }
                stream.write(featureTableBinaryPadding);
            }

            stream.writePureText(batchTableJson);
            stream.write(batchTableBytes);
            if (batchTableBinaryPadLength > 0) {
                byte[] batchTableBinaryPadding = new byte[batchTableBinaryPadLength];
                for (int i = 0; i < batchTableBinaryPadLength; i++) {
                    batchTableBinaryPadding[i] = 0x00;
                }
                stream.write(batchTableBinaryPadding);
            }

            if (lastPadLength > 0) {
                byte[] lastPadding = new byte[lastPadLength];
                for (int i = 0; i < lastPadLength; i++) {
                    lastPadding[i] = 0x20;
                }
                stream.write(lastPadding);
            }
        } catch (Exception e) {
            log.error("[ERROR] :", e);
        }
    }
}
