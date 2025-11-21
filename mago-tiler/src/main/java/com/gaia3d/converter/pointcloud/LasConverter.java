package com.gaia3d.converter.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.basic.pointcloud.GaiaPointCloudHeader;
import com.gaia3d.basic.pointcloud.GaiaPointCloudTemp;
import com.gaia3d.command.mago.GlobalConstants;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.util.GlobeUtils;
import com.github.mreutegg.laszip4j.CloseablePointIterable;
import com.github.mreutegg.laszip4j.LASHeader;
import com.github.mreutegg.laszip4j.LASPoint;
import com.github.mreutegg.laszip4j.LASReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.locationtech.proj4j.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class LasConverter {
    public List<GaiaPointCloud> load(String path) {
        return convert(new File(path));
    }

    public List<GaiaPointCloud> load(File file) {
        return convert(file);
    }

    public List<GaiaPointCloud> load(Path path) {
        return convert(path.toFile());
    }

    public GaiaPointCloudHeader readHeader(File file) {
        LASReader reader = new LASReader(file);
        LASHeader header = reader.getHeader();

        GlobalOptions globalOptions = GlobalOptions.getInstance();

        double getMinX = header.getMinX();
        double getMinY = header.getMinY();
        double getMinZ = header.getMinZ();
        double getMaxX = header.getMaxX();
        double getMaxY = header.getMaxY();
        double getMaxZ = header.getMaxZ();
        Vector3d min = new Vector3d(getMinX, getMinY, getMinZ);
        Vector3d max = new Vector3d(getMaxX, getMaxY, getMaxZ);

        // Apply translation offset
        Vector3d transform = globalOptions.getTranslateOffset();
        if (transform != null) {
            min = new Vector3d(min.x + transform.x, min.y + transform.y, min.z + transform.z);
            max = new Vector3d(max.x + transform.x, max.y + transform.y, max.z + transform.z);
        }

        if (min.x > max.x || min.y > max.y || min.z > max.z) {
            log.error("[ERROR] Min point is greater than Max point.");
            return null;
        } else if (min.x == max.x && min.y == max.y && min.z == max.z) {
            log.error("[ERROR] Min point is equal to Max point.");
            return null;
        } else if (min.x == 0 && min.y == 0 && min.z == 0 && max.x == 0 && max.y == 0 && max.z == 0) {
            log.error("[ERROR] Min point and Max point are all zero.");
            return null;
        }

        GaiaBoundingBox srsBoundingBox = new GaiaBoundingBox();
        srsBoundingBox.addPoint(min);
        srsBoundingBox.addPoint(max);

        long pointRecords = header.getNumberOfPointRecords();
        long legacyPointRecords = header.getLegacyNumberOfPointRecords();
        long totalPointRecords = pointRecords + legacyPointRecords;
        return GaiaPointCloudHeader.builder().index(-1).uuid(UUID.randomUUID()).size(totalPointRecords).srsBoundingBox(srsBoundingBox).build();
    }

    public void loadToTemp(GaiaPointCloudHeader pointCloudHeader, File file) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();

        LASReader reader = new LASReader(file);
        LASHeader header = reader.getHeader();
        double xScaleFactor = header.getXScaleFactor();
        double xOffset = header.getXOffset();
        double yScaleFactor = header.getYScaleFactor();
        double yOffset = header.getYOffset();
        double zScaleFactor = header.getZScaleFactor();
        double zOffset = header.getZOffset();

        // Apply translation offset
        Vector3d transform = globalOptions.getTranslateOffset();
        if (transform != null) {
            xOffset = xOffset + transform.x;
            yOffset = yOffset + transform.y;
            zOffset = zOffset + transform.z;
        }

        CloseablePointIterable pointIterable = reader.getCloseablePoints();
        long pointRecords = header.getNumberOfPointRecords();
        long legacyPointRecords = header.getLegacyNumberOfPointRecords();
        long totalPointsSize = pointRecords + legacyPointRecords;
        byte recordFormatValue = header.getPointDataRecordFormat();
        boolean hasRgbColor;
        LasRecordFormat recordFormat = LasRecordFormat.fromFormatNumber(recordFormatValue);
        if (recordFormat != null) {
            hasRgbColor = recordFormat.hasColor;
        } else {
            hasRgbColor = false;
        }

        String version = header.getVersionMajor() + "." + header.getVersionMinor();
        String systemId = header.getSystemIdentifier();
        String softwareId = header.getGeneratingSoftware();
        String fileCreationDate = (short) header.getFileCreationYear() + "-" + (short) header.getFileCreationDayOfYear();
        String headerSize = (short) header.getHeaderSize() + " bytes";

        log.debug("Version: {}", version);
        log.debug("System ID: {}", systemId);
        log.debug("Software ID: {}", softwareId);
        log.debug("File Creation Date: {}", fileCreationDate);
        log.debug("Header Size: {}", headerSize);

        boolean isDefaultCrs = globalOptions.getSourceCrs().equals(GlobalConstants.DEFAULT_SOURCE_CRS);
        try {
            header.getVariableLengthRecords().forEach((record) -> {
                if (isDefaultCrs && record.getUserID().equals("LASF_Projection")) {
                    String wktCRS = record.getDataAsString();
                    CoordinateReferenceSystem crs = GlobeUtils.convertWkt(wktCRS);
                    if (crs != null) {
                        var convertedCrs = GlobeUtils.convertProj4jCrsFromGeotoolsCrs(crs);
                        globalOptions.setSourceCrs(convertedCrs);
                        log.info(" - Coordinate Reference System : {}", wktCRS);
                    } else {
                        String epsg = GlobeUtils.extractEpsgCodeFromWTK(wktCRS);
                        if (epsg != null) {
                            CRSFactory factory = new CRSFactory();
                            globalOptions.setSourceCrs(factory.createFromName("EPSG:" + epsg));
                            log.info(" - Coordinate Reference System : {}", epsg);
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.error("[ERROR] Failed to read LAS header.", e);
        }

        int percentage = globalOptions.getPointRatio();
        if (percentage < 1) {
            percentage = 1;
        } else if (percentage > 100) {
            percentage = 100;
        }
        int volumeFactor = (int) Math.ceil(100.0 / percentage);
        int count = 0;
        for (LASPoint point : pointIterable) {
            if (count++ % volumeFactor != 0) {
                continue;
            }
            double x = point.getX() * xScaleFactor + xOffset;
            double y = point.getY() * yScaleFactor + yOffset;
            double z = point.getZ() * zScaleFactor + zOffset;

            Vector3d position = new Vector3d(x, y, z);

            byte[] rgb;
            if (hasRgbColor) {
                if (globalOptions.isForce4ByteRGB()) {
                    rgb = getColorByByteRGB(point); // only for test
                } else {
                    rgb = getColorByRGB(point);
                }
            } else {
                rgb = new byte[3];
                rgb[0] = (byte) 128;
                rgb[1] = (byte) 128;
                rgb[2] = (byte) 128;
            }

            int byteLength = 0;
            byte[] intensity = convertToByteIntensity(point.getIntensity());
            byte[] classification = convertToByteClassification(point.getClassification());
            byteLength += 4; // 4 bytes for rgb
            byteLength += intensity.length;
            byteLength += classification.length;

            byte[] totalByte = new byte[byteLength];
            // concatenate byte arrays
            int index = 0;
            System.arraycopy(rgb, 0, totalByte, index, rgb.length);
            index += rgb.length;
            System.arraycopy(intensity, 0, totalByte, index, intensity.length);
            index += intensity.length;
            System.arraycopy(classification, 0, totalByte, index, classification.length);

            GaiaPointCloudTemp tempFile = pointCloudHeader.findTemp(position);
            if (tempFile == null) {
                log.error("[ERROR] Failed to find temp file.");
            } else {
                tempFile.writePosition(position, totalByte);
            }
        }
    }

    private List<GaiaPointCloud> convert(File file) {
        List<GaiaPointCloud> pointClouds = new ArrayList<>();
        GaiaPointCloud pointCloud = new GaiaPointCloud();
        GaiaBoundingBox boundingBox = pointCloud.getGaiaBoundingBox();
        try {
            GaiaPointCloudTemp readTemp = new GaiaPointCloudTemp(file);
            readTemp.readHeader();

            double[] quantizationOffset = readTemp.getQuantizedVolumeOffset();
            double[] quantizationScale = readTemp.getQuantizedVolumeScale();
            double[] originalMinPosition = new double[]{quantizationOffset[0], quantizationOffset[1], quantizationOffset[2]};
            double[] originalMaxPosition = new double[]{quantizationOffset[0] + quantizationScale[0], quantizationOffset[1] + quantizationScale[1], quantizationOffset[2] + quantizationScale[2]};
            Vector3d minPosition = new Vector3d(originalMinPosition[0], originalMinPosition[1], originalMinPosition[2]);
            Vector3d maxPosition = new Vector3d(originalMaxPosition[0], originalMaxPosition[1], originalMaxPosition[2]);

            boundingBox.addPoint(minPosition);
            boundingBox.addPoint(maxPosition);

            readTemp.getInputStream().close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        GaiaPointCloudTemp readTemp = new GaiaPointCloudTemp(file);
        pointCloud.setMinimized(true);
        pointCloud.setVertices(null);
        pointCloud.setGaiaBoundingBox(boundingBox);
        pointCloud.setPointCloudTemp(readTemp);
        pointClouds.add(pointCloud);
        return pointClouds;
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

    private byte[] convertToByteIntensity(char intensity) {
        byte[] rgb = new byte[2];
        rgb[0] = (byte) ((intensity >> 8) & 0xFF); // High byte
        rgb[1] = (byte) (intensity & 0xFF); // Low byte
        return rgb;
    }

    private byte[] convertToByteClassification(short classification) {
        byte[] rgb = new byte[2];
        rgb[0] = (byte) ((classification >> 8) & 0xFF); // High byte
        rgb[1] = (byte) (classification & 0xFF); // Low byte
        return rgb;
    }
}
