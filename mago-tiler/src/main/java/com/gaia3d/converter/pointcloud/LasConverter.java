package com.gaia3d.converter.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.converter.pointcloud.shuffler.*;
import com.gaia3d.util.GlobeUtils;
import com.github.mreutegg.laszip4j.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.locationtech.proj4j.BasicCoordinateTransform;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class LasConverter {
    // positions(24) + rgb(4) + intensity(2) + classification(2) = 32 bytes
    private final int POINT_BLOCK_SIZE = 32;
    private final LasConverterOptions options;
    private final BucketWriter bucketWriter;
    private final BucketReader bucketReader;

    public LasConverter(LasConverterOptions options) {
        this.options = options;
        try {
            this.bucketWriter = new BucketWriter(options.getTempDirectory());
            this.bucketReader = new BucketReader();
        } catch (IOException e) {
            log.error("[ERROR] Failed to initialize BucketWriter.", e);
            throw new RuntimeException(e);
        }
    }

    public void convert(File file) {
        LASReader reader = new LASReader(file);
        LASHeader header = reader.getHeader();
        double xScaleFactor = header.getXScaleFactor();
        double xOffset = header.getXOffset();
        double yScaleFactor = header.getYScaleFactor();
        double yOffset = header.getYOffset();
        double zScaleFactor = header.getZScaleFactor();
        double zOffset = header.getZOffset();

        CloseablePointIterable pointIterable = reader.getCloseablePoints();
        long pointRecords = header.getNumberOfPointRecords();
        long legacyPointRecords = header.getLegacyNumberOfPointRecords();
        long totalPointsSize = pointRecords == 0 ? legacyPointRecords : pointRecords;
        byte recordFormatValue = header.getPointDataRecordFormat();
        LasRecordFormat recordFormat = LasRecordFormat.fromFormatNumber(recordFormatValue);
        boolean hasRgbColor = recordFormat != null && recordFormat.hasColor;
        printHeaderInfo(header);

        boolean isForce4ByteRGB = options.isForce4ByteRgb();
        CoordinateReferenceSystem sourceCrs = getProjCRS(header);
        CoordinateReferenceSystem targetCrs = GlobeUtils.wgs84;
        BasicCoordinateTransform transformer = new BasicCoordinateTransform(sourceCrs, targetCrs);
        boolean canTransform = sourceCrs != null && !sourceCrs.equals(GlobeUtils.wgs84);
        ProjCoordinate sourceCoord = new ProjCoordinate();
        ProjCoordinate targetCoord = new ProjCoordinate();

        /*long pointCount = 0;
        log.info("Starting point conversion...");
        GaiaBoundingBox degreeBoundingBox = new GaiaBoundingBox();
        GaiaBoundingBox ecefBoundingBox = new GaiaBoundingBox();

        for (LASPoint point : pointIterable) {
            double x = point.getX() * xScaleFactor + xOffset;
            double y = point.getY() * yScaleFactor + yOffset;
            double z = point.getZ() * zScaleFactor + zOffset;
            if (canTransform) {
                sourceCoord.x = x;
                sourceCoord.y = y;
                sourceCoord.z = z;
                transformer.transform(sourceCoord, targetCoord);
                x = targetCoord.x;
                y = targetCoord.y;
                z = targetCoord.z;
            }

            // convert WGS84 to ECEF
            double[] ecef = GlobeUtils.geographicToCartesianWgs84(x, y, z);

            degreeBoundingBox.addPoint(x, y, z);
            ecefBoundingBox.addPoint(ecef[0], ecef[1], ecef[2]);
            pointCount++;
        }
        float spacing = (float) Math.sqrt(((ecefBoundingBox.getMaxX() - ecefBoundingBox.getMinX()) * (ecefBoundingBox.getMaxY() - ecefBoundingBox.getMinY())) / pointCount);
        spacing = Math.round(spacing * 1000.0f) / 1000.0f;
        log.info("Estimated point spacing: {} meters", spacing);
        log.info("Point conversion completed. Total points: {}", pointCount);
        log.info("Bounding Box - MinX: {}, MinY: {}, MinZ: {}, MaxX: {}, MaxY: {}, MaxZ: {}",
                ecefBoundingBox.getMinX(), ecefBoundingBox.getMinY(), ecefBoundingBox.getMinZ(),
                ecefBoundingBox.getMaxX(), ecefBoundingBox.getMaxY(), ecefBoundingBox.getMaxZ());*/

        for (LASPoint point : pointIterable) {
            double x = point.getX() * xScaleFactor + xOffset;
            double y = point.getY() * yScaleFactor + yOffset;
            double z = point.getZ() * zScaleFactor + zOffset;

            if (canTransform) {
                sourceCoord.x = x;
                sourceCoord.y = y;
                sourceCoord.z = z;
                transformer.transform(sourceCoord, targetCoord);
                x = targetCoord.x;
                y = targetCoord.y;
                z = targetCoord.z;
                /*double[] ecef = GlobeUtils.geographicToCartesianWgs84(targetCoord.x, targetCoord.y, targetCoord.z);
                x = ecef[0];
                y = ecef[1];
                z = ecef[2];*/
            }

            byte[] rgb = getRgbColor(point, hasRgbColor, isForce4ByteRGB);
            GaiaLasPoint gaiaLasPoint = GaiaLasPoint.builder()
                    .position(new double[]{x, y, z})
                    .rgb(rgb)
                    .intensity(point.getIntensity())
                    .classification(point.getClassification())
                    .build();

            try {
                bucketWriter.addPoint(gaiaLasPoint);
            } catch (IOException e) {
                log.error("[ERROR] Failed to write point to bucket.", e);
                throw new RuntimeException(e);
            }
        }
    }

    private byte[] getRgbColor(LASPoint point, boolean hasRGB, boolean force4ByteRGB) {
        if (hasRGB) {
            if (force4ByteRGB) {
                return getColorByByteRGB(point); // only for test
            } else {
                return getColorByRGB(point);
            }
        } else {
            byte[] rgb = new byte[3];
            rgb[0] = (byte) -127;
            rgb[1] = (byte) -127;
            rgb[2] = (byte) -127;
            return rgb;
        }
    }

    private void printHeaderInfo(LASHeader header) {
        String version = header.getVersionMajor() + "." + header.getVersionMinor();
        String systemId = header.getSystemIdentifier();
        String softwareId = header.getGeneratingSoftware();
        String fileCreationDate = (short) header.getFileCreationYear() + "-" + (short) header.getFileCreationDayOfYear();
        String headerSize = (short) header.getHeaderSize() + " bytes";
        long pointRecords = header.getNumberOfPointRecords();
        long legacyPointRecords = header.getLegacyNumberOfPointRecords();
        long totalPointsSize = pointRecords == 0 ? legacyPointRecords : pointRecords;
        byte recordFormatValue = header.getPointDataRecordFormat();
        LasRecordFormat recordFormat = LasRecordFormat.fromFormatNumber(recordFormatValue);
        boolean hasRgbColor = recordFormat != null && recordFormat.hasColor;

        log.info("=== LAS File Header Information ===");
        log.info("Version: {}", version);
        log.info("System ID: {}", systemId);
        log.info("Software ID: {}", softwareId);
        log.info("File Creation Date: {}", fileCreationDate);
        log.info("Header Size: {}", headerSize);
        log.info("Number of Point Records: {}", pointRecords);
        log.info("Legacy Number of Point Records: {}", legacyPointRecords);
        log.info("Total Number of Point Records: {}", totalPointsSize);
    }

    private CoordinateReferenceSystem getProjCRS(LASHeader header) {
        AtomicReference<CoordinateReferenceSystem> atomicCrs = new AtomicReference<>(options.getSourceCrs());
        boolean isDefaultCrs = options.getSourceCrs().equals(GlobeUtils.wgs84);
        if (isDefaultCrs) {
            try {
                Iterable<LASVariableLengthRecord> records = header.getVariableLengthRecords();
                if (records != null) {
                    header.getVariableLengthRecords().forEach((record) -> {
                        if (record.getUserID().equals("LASF_Projection")) {
                            String wktCRS = record.getDataAsString();
                            var crs = GlobeUtils.convertWkt(wktCRS);
                            if (crs != null) {
                                var convertedCrs = GlobeUtils.convertProj4jCrsFromGeotoolsCrs(crs);
                                atomicCrs.set(convertedCrs);
                            } else {
                                String epsg = GlobeUtils.extractEpsgCodeFromWTK(wktCRS);
                                if (epsg != null) {
                                    CRSFactory factory = new CRSFactory();
                                    var convertedCrs = factory.createFromName("EPSG:" + epsg);
                                    atomicCrs.set(convertedCrs);
                                }
                            }
                        }
                    });
                }
            } catch (Exception e) {
                log.debug("[ERROR] Failed to read LAS header.", e);
            }
        }
        CoordinateReferenceSystem crs = atomicCrs.get();
        log.info(" - Coordinate Reference System : {}", crs);
        return crs;
    }

    public void close() {
        try {
            bucketWriter.close();
        } catch (IOException e) {
            log.error("[ERROR] Failed to close BucketWriter.", e);
            throw new RuntimeException(e);
        }
    }

    public List<File> getBucketFiles() {
        Path tempPath = options.getTempDirectory();
        return FileUtils.listFiles(tempPath.toFile(), new String[]{"bin"}, true).stream().toList();
    }

    public void createVoxel() {
        Path tempPath = options.getTempDirectory();
        List<File> bucketFiles = getBucketFiles();
        for (File bucketFile : bucketFiles) {
            try {
                GaiaBoundingBox boundingBox = new GaiaBoundingBox();
                List<GaiaLasPoint> lasPoints = bucketReader.readFile(bucketFile.toPath());
                // Convert geographic to ECEF
                lasPoints = lasPoints.stream().parallel().map(point -> {
                    double[] pos = point.getPosition();
                    double[] ecef = GlobeUtils.geographicToCartesianWgs84(pos[0], pos[1], pos[2]);
                    point.setPosition(ecef);
                    return point;
                }).toList();
                for (GaiaLasPoint point : lasPoints) {
                    double[] pos = point.getPosition();
                    boundingBox.addPoint(pos[0], pos[1], pos[2]);
                }
                PointCloudOctree octree = new PointCloudOctree(null, boundingBox);
                octree.addContents(lasPoints);
                octree.setLimitDepth(10);
                octree.setLimitBoxSize(25.0);
                octree.makeTreeByMinVertexCount(10000);

                log.info("Creating voxel for bucket file: {} with {} points", bucketFile.getName(), lasPoints.size());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void createShuffle() {
        Shuffler shuffler = new NewCardShuffler();
        List<File> bucketFiles = getBucketFiles();
        log.info("[Pre] Starting shuffling of {} bucket files...", bucketFiles.size());
        bucketFiles.stream().parallel().forEach(bucketFile -> {
            File shuffledFile = new File(bucketFile.getParent(), "shuffled_" + bucketFile.getName());
            log.info("[Pre] Shuffling bucket file: {} to {}", bucketFile.getAbsolutePath(), shuffledFile.getAbsolutePath());
            shuffler.shuffle(bucketFile, shuffledFile, POINT_BLOCK_SIZE);

            boolean isSameSize = bucketFile.length() == shuffledFile.length();
            if (!isSameSize) {
                log.warn("Shuffled file size does not match original! Original: {}, Shuffled: {}", bucketFile.length(), shuffledFile.length());
                throw new RuntimeException("Shuffled file size mismatch.");
            } else {
                log.info("Shuffling completed successfully for file: {}", bucketFile.getName());
                try {
                    FileUtils.delete(bucketFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    FileUtils.moveFile(shuffledFile, bucketFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        log.info("[Pre] Completed shuffling of bucket files.");
    }

    public List<GaiaLasPoint> readTempFile(File temppFile) {
        try {
            return bucketReader.readFile(temppFile.toPath());
        } catch (IOException e) {
            log.error("[ERROR] Failed to read shuffled file: {}", temppFile.getAbsolutePath(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Get color by RGB
     * @param point LASPoint
     * @return byte[3]
     */
    private byte[] getColorByRGB(LASPoint point) {
        double red = (double) point.getRed() / 65535;
        double green = (double) point.getGreen() / 65535;
        double blue = (double) point.getBlue() / 65535;

        byte[] rgb = new byte[3];
        rgb[0] = (byte) (red * 255);
        rgb[1] = (byte) (green * 255);
        rgb[2] = (byte) (blue * 255);
        return rgb;
    }

    /**
     * Get color by RGB
     * @param point LASPoint
     * @return byte[3]
     */
    private byte[] getColorByByteRGB(LASPoint point) {
        byte[] rgb = new byte[3];
        rgb[0] = (byte) point.getRed();
        rgb[1] = (byte) point.getGreen();
        rgb[2] = (byte) point.getBlue();
        return rgb;
    }
}
