package com.gaia3d.converter.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.util.geographic.GeographicTilingScheme;
import com.gaia3d.util.geographic.TileCoordinate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.joml.Vector3d;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BucketReader {
    private static final int BUFFER_SIZE = 4 * 1024 * 1024; // 4MB

    private final GeographicTilingScheme scheme = new GeographicTilingScheme();

    public GaiaPointCloud readFileToGaiaPointCloud(File sourceFile, File targetFile) throws IOException {
        GaiaPointCloud pointCloud = new GaiaPointCloud();
        long fileSize = sourceFile.length();
        long totalPoints = fileSize / LasConverter.POINT_BLOCK_SIZE;

        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        pointCloud.setGaiaBoundingBox(boundingBox);
        pointCloud.setPointCount(totalPoints);
        pointCloud.setMinimizedFile(targetFile);

        //long pointCount = 0;
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(sourceFile), BUFFER_SIZE))) {
            log.info("[Pre] Reading bucket file: {} ({} points)", sourceFile.getAbsolutePath(), totalPoints);
            for (long i = 0; i < totalPoints; i++) {
                byte[] pointBytes = new byte[LasConverter.POINT_BLOCK_SIZE];
                dis.readFully(pointBytes);
                GaiaLasPoint point = parsePoint(pointBytes);
                Vector3d position = point.getVec3Position();
                boundingBox.addPoint(position);

                position = null;
                point = null;
            }
            dis.close();
            log.info("[Pre] Finished reading bucket file: {} ({} points)", sourceFile, totalPoints);
            log.info("[Pre] Minimizing point cloud and writing to temp file: {}", targetFile);
            FileUtils.moveFile(sourceFile, targetFile);
            log.info("[Pre] Finished writing to temp file: {}", targetFile);
        } catch (IOException e) {
            log.error("[ERROR] Failed to read bucket file: {}", sourceFile, e);
            throw e;
        }
        return pointCloud;
    }

    public List<GaiaLasPoint> readFile(Path filePath) throws IOException {
        List<GaiaLasPoint> points = new ArrayList<>();

        File file = filePath.toFile();
        long fileSize = file.length();
        long totalPoints = fileSize / LasConverter.POINT_BLOCK_SIZE;

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE))) {
            for (long i = 0; i < totalPoints; i++) {
                byte[] pointBytes = new byte[LasConverter.POINT_BLOCK_SIZE];
                dis.readFully(pointBytes);
                GaiaLasPoint point = parsePoint(pointBytes);
                points.add(point);
            }
        } catch (IOException e) {
            log.error("[ERROR] Failed to read bucket file: {}", filePath, e);
            throw e;
        }
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
