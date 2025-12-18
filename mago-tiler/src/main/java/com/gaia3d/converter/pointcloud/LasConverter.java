package com.gaia3d.converter.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.converter.pointcloud.shuffler.*;
import com.gaia3d.util.GlobeUtils;
import com.github.mreutegg.laszip4j.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.geotools.api.geometry.Position;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.joml.Vector3d;
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
    /* positions(24) + rgb(4) + intensity(2) + classification(2) = 32 bytes */
    public static final int COARSE_LEVEL = 13;
    public static final int POINT_BLOCK_SIZE = 32;
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
        boolean isForceCrs = options.isForceCrs();
        CoordinateReferenceSystem sourceCrs = getProjCRS(header, isForceCrs);
        CoordinateReferenceSystem targetCrs = GlobeUtils.wgs84;
        BasicCoordinateTransform transformer = new BasicCoordinateTransform(sourceCrs, targetCrs);
        boolean needTransform = sourceCrs != null && !sourceCrs.equals(GlobeUtils.wgs84);
        ProjCoordinate sourceCoord = new ProjCoordinate();
        ProjCoordinate targetCoord = new ProjCoordinate();

        Vector3d translation = options.getTranslation();
        boolean applyTranslation = translation.x != 0.0 || translation.y != 0.0 || translation.z != 0.0;

        float percentage = options.getPointPercentage();
        if (percentage < 1) {
            percentage = 1;
        } else if (percentage > 100) {
            percentage = 100;
        }
        int volumeFilter = (int) Math.ceil(100.0 / percentage);

        boolean showProgress = totalPointsSize >= 1000000;
        int progressInterval = (int) (totalPointsSize / 100);
        if (progressInterval == 0) {
            progressInterval = 1;
        }

        List<GridCoverage2D> geoTiffs = options.getGeoTiffs();
        List<GridCoverage2D> geoidTiffs = options.getGeoidTiffs();
        boolean hasTerrain = geoTiffs != null && !geoTiffs.isEmpty();
        boolean hasGeoid = geoidTiffs != null && !geoidTiffs.isEmpty();
        long index = 0;
        for (LASPoint point : pointIterable) {
            if (index % volumeFilter != 0) {
                index++;
                continue;
            }
            if (showProgress && index % progressInterval == 0) {
                int progress = (int) ((index * 100) / totalPointsSize);
                log.info("[Load] - Processing point {}/{} ({}%)", index, totalPointsSize, progress);
            }
            double x = point.getX() * xScaleFactor + xOffset;
            double y = point.getY() * yScaleFactor + yOffset;
            double z = point.getZ() * zScaleFactor + zOffset;

            if (applyTranslation) {
                x += translation.x;
                y += translation.y;
                z += translation.z;
            }

            if (needTransform) {
                sourceCoord.x = x;
                sourceCoord.y = y;
                sourceCoord.z = z;
                transformer.transform(sourceCoord, targetCoord);
                x = targetCoord.x;
                y = targetCoord.y;
                z = targetCoord.z;
            }

            if (hasTerrain || hasGeoid) {
                Vector3d cartographic = GlobeUtils.cartesianToGeographicWgs84(x, y, z);
                double terrainHeight = getTerrainHeightFromCartographic(geoTiffs, geoidTiffs, cartographic);
                z += terrainHeight;
            }

            byte[] rgb = getRgbColor(point, hasRgbColor, isForce4ByteRGB);
            GaiaLasPoint gaiaLasPoint = GaiaLasPoint.builder()
                    .x(x)
                    .y(y)
                    .z(z)
                    .r(rgb[0])
                    .g(rgb[1])
                    .b(rgb[2])
                    .a((byte) 255)
                    .intensity(point.getIntensity())
                    .classification(point.getClassification())
                    .build();
            try {
                bucketWriter.addPoint(gaiaLasPoint);
            } catch (IOException e) {
                log.error("[ERROR] Failed to write point to bucket.", e);
                throw new RuntimeException(e);
            }
            index++;
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

        log.debug("=== LAS File Header Information ===");
        log.debug("Version: {}", version);
        log.debug("System ID: {}", systemId);
        log.debug("Software ID: {}", softwareId);
        log.debug("File Creation Date: {}", fileCreationDate);
        log.debug("Header Size: {}", headerSize);
        log.debug("Number of Point Records: {}", pointRecords);
        log.debug("Legacy Number of Point Records: {}", legacyPointRecords);
        log.debug("Total Number of Point Records: {}", totalPointsSize);
    }

    private CoordinateReferenceSystem getProjCRS(LASHeader header, boolean isForceCrs) {
        AtomicReference<CoordinateReferenceSystem> atomicCrs = new AtomicReference<>(options.getSourceCrs());
        boolean isDefaultCrs = options.getSourceCrs().equals(GlobeUtils.wgs84);
        if (!isForceCrs && isDefaultCrs) {
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
            log.info(" - Coordinate Reference System : {}", atomicCrs.get());
        }
        return atomicCrs.get();
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
                lasPoints = lasPoints.stream().parallel().peek(point -> {
                    double[] pos = point.getPosition();
                    double[] ecef = GlobeUtils.geographicToCartesianWgs84(pos[0], pos[1], pos[2]);
                    point.setPosition(ecef);
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
        Shuffler shuffler = new OptimizedCardShuffler();

        List<File> bucketFiles = getBucketFiles();
        log.info("[Pre] Starting shuffling of {} bucket files...", bucketFiles.size());
        // bucketFiles.stream().parallel().forEach(bucketFile -> {
        int fileCount = bucketFiles.size();
        int index = 0;
        shuffler.setTotalProcessCount(fileCount);
        for (File bucketFile : bucketFiles) {
            File shuffledFile = new File(bucketFile.getParent(), "shuffled_" + bucketFile.getName());
            log.info("[Pre][Shuffle][{}/{}] Shuffling bucket file: {}", ++index, fileCount, bucketFile.getAbsoluteFile());
            shuffler.setProcessCount(index);
            shuffler.shuffle(bucketFile, shuffledFile, POINT_BLOCK_SIZE);
            boolean isSameSize = bucketFile.length() == shuffledFile.length();
            if (!isSameSize) {
                log.warn("Shuffled file size does not match original! Original: {}, Shuffled: {}", bucketFile.length(), shuffledFile.length());
                throw new RuntimeException("Shuffled file size mismatch.");
            } else {
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
            log.info("[Pre][Shuffle][{}/{}] Completed shuffling bucket file: {}", index, fileCount, bucketFile.getAbsoluteFile());
        }
        shuffler.clear();
        log.info("[Pre] Completed shuffling of bucket files.");
    }

    public GaiaPointCloud readTempFileToGaiaPointCloud(File sourceFile, File targetFile) {
        try {
            return bucketReader.readFileToGaiaPointCloud(sourceFile, targetFile);
        } catch (IOException e) {
            log.error("[ERROR] Failed to read shuffled file: {}", sourceFile.getAbsolutePath(), e);
            throw new RuntimeException(e);
        }
    }

    public List<GaiaLasPoint> readTempFile(File tempFile) {
        try {
            return bucketReader.readFile(tempFile.toPath());
        } catch (IOException e) {
            log.error("[ERROR] Failed to read shuffled file: {}", tempFile.getAbsolutePath(), e);
            throw new RuntimeException(e);
        }
    }

    private float calculateSpacing(GaiaBoundingBox bbox, long pointCount) {
        float spacing = (float) Math.sqrt(((bbox.getMaxX() - bbox.getMinX()) * (bbox.getMaxY() - bbox.getMinY())) / pointCount);
        spacing = Math.round(spacing * 1000.0f) / 1000.0f;
        return spacing;
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

    private double getTerrainHeightFromCartographic(List<GridCoverage2D> terrains, List<GridCoverage2D> geoids, Vector3d cartographic) {
        Vector3d center = new Vector3d(cartographic.x, cartographic.y, 0.0);
        Position position = new Position2D(DefaultGeographicCRS.WGS84, center.x, center.y);
        double resultHeight = 0.0d;
        if (terrains != null && !terrains.isEmpty()) {
            for (GridCoverage2D coverage : terrains) {
                double[] altitude = new double[1];
                altitude[0] = 0.0d;

                try {
                    coverage.evaluate(position, altitude);
                } catch (Exception e) {
                    log.debug("[DEBUG] Failed to load terrain height. Out of range");
                }

                if (Double.isInfinite(altitude[0])) {
                    log.debug("[DEBUG] Failed to load terrain height. Infinite value encountered");
                } else if (Double.isNaN(altitude[0])) {
                    log.debug("[DEBUG] Failed to load terrain height. NaN value encountered");
                } else {
                    resultHeight += altitude[0];
                }
            }
        }

        if (geoids != null && !geoids.isEmpty()) {
            for (GridCoverage2D coverage : geoids) {
                double[] geoidHeight = new double[1];
                geoidHeight[0] = 0.0d;

                try {
                    coverage.evaluate(position, geoidHeight);
                } catch (Exception e) {
                    log.debug("[DEBUG] Failed to load geoid height. Out of range");
                }

                if (Double.isInfinite(geoidHeight[0])) {
                    log.debug("[DEBUG] Failed to load geoid height. Infinite value encountered");
                } else if (Double.isNaN(geoidHeight[0])) {
                    log.debug("[DEBUG] Failed to load geoid height. NaN value encountered");
                } else {
                    resultHeight += geoidHeight[0];
                }
            }
        }
        return resultHeight;
    }
}
