package com.gaia3d.converter.pointcloud;

import com.gaia3d.util.geographic.GeographicTilingScheme;
import com.gaia3d.util.geographic.TileCoordinate;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BucketReader {
    private static final int COARSE_LEVEL = 12;
    private static final int POINT_BLOCK_SIZE = 32;
    private static final int BUFFER_SIZE = 4 * 1024 * 1024; // 4MB

    private final GeographicTilingScheme scheme = new GeographicTilingScheme();

    public List<GaiaLasPoint> readFile(Path filePath) throws IOException {
        List<GaiaLasPoint> points = new ArrayList<>();

        File file = filePath.toFile();
        long fileSize = file.length();
        long totalPoints = fileSize / POINT_BLOCK_SIZE;

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE))) {
            for (long i = 0; i < totalPoints; i++) {
                byte[] pointBytes = new byte[POINT_BLOCK_SIZE];
                dis.readFully(pointBytes);
                GaiaLasPoint point = parsePoint(pointBytes);
                points.add(point);
            }
        } catch (IOException e) {
            log.error("[ERROR] Failed to read bucket file: {}", filePath, e);
            throw e;
        }

        // Implement reading logic here
        /*byte[] buffer = new byte[BUFFER_SIZE];
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                for (int offset = 0; offset < bytesRead; offset += POINT_BLOCK_SIZE) {
                    if (offset + POINT_BLOCK_SIZE <= bytesRead) {
                        byte[] pointBytes = new byte[POINT_BLOCK_SIZE];
                        System.arraycopy(buffer, offset, pointBytes, 0, POINT_BLOCK_SIZE);

                        // Parse the pointBytes to create a GaiaLasPoint
                        GaiaLasPoint point = parsePoint(pointBytes);
                        points.add(point);
                    }
                }
            }
        } catch (IOException e) {
            log.error("[ERROR] Failed to read bucket file: {}", filePath, e);
            throw e;
        }*/
        return points;
    }

    private GaiaLasPoint parsePoint(byte[] pointBytes) {
        double lon = BigEndianByteUtils.toDouble(pointBytes, 0);
        double lat = BigEndianByteUtils.toDouble(pointBytes, 8);
        double height = BigEndianByteUtils.toDouble(pointBytes, 16);
        byte[] rgb = new byte[3];
        System.arraycopy(pointBytes, 24, rgb, 0, 3);
        char intensity = BigEndianByteUtils.toChar(pointBytes, 27);
        short classification = BigEndianByteUtils.toShort(pointBytes, 29);

        GaiaLasPoint point = GaiaLasPoint.builder()
                .position(new double[]{lon, lat, height})
                .rgb(rgb)
                .intensity(intensity)
                .classification(classification)
                .build();
        return point;
    }
}
