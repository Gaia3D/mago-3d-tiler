package com.gaia3d.converter.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.octree.GaiaOctreeVertices;
import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.basic.model.GaiaVertex;
import com.gaia3d.basic.pointcloud.GaiaPointCloudHeader;
import com.gaia3d.basic.pointcloud.GaiaPointCloudTemp;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.util.GlobeUtils;
import com.github.mreutegg.laszip4j.CloseablePointIterable;
import com.github.mreutegg.laszip4j.LASHeader;
import com.github.mreutegg.laszip4j.LASPoint;
import com.github.mreutegg.laszip4j.LASReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.locationtech.proj4j.BasicCoordinateTransform;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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

        double getMinX = header.getMinX();
        double getMinY = header.getMinY();
        double getMinZ = header.getMinZ();
        double getMaxX = header.getMaxX();
        double getMaxY = header.getMaxY();
        double getMaxZ = header.getMaxZ();
        Vector3d min = new Vector3d(getMinX, getMinY, getMinZ);
        Vector3d max = new Vector3d(getMaxX, getMaxY, getMaxZ);

        GaiaBoundingBox srsBoundingBox = new GaiaBoundingBox();
        srsBoundingBox.addPoint(min);
        srsBoundingBox.addPoint(max);

        long pointRecords = header.getNumberOfPointRecords();
        long legacyPointRecords = header.getLegacyNumberOfPointRecords();
        long totalPointRecords = pointRecords + legacyPointRecords;

        return GaiaPointCloudHeader.builder()
                .index(-1)
                .uuid(UUID.randomUUID())
                .size(totalPointRecords)
                .srsBoundingBox(srsBoundingBox)
                .build();
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
        CloseablePointIterable pointIterable = reader.getCloseablePoints();

        long totalPointsSize = pointCloudHeader.getSize();
        int pointsPerGrid = globalOptions.getPointsPerGrid();
        int factor = (int) (totalPointsSize / pointsPerGrid);

        // 
        if (globalOptions.isSourcePrecision()) {
            factor = 1;
        } else if (factor > 8) {
            factor = 8;
        } else if (factor > 1) {
            factor = factor - 1;
        }

        int count = 0;
        for (LASPoint point : pointIterable) {
            if (count++ % factor != 0) {
                continue;
            }
            double x = point.getX() * xScaleFactor + xOffset;
            double y = point.getY() * yScaleFactor + yOffset;
            double z = point.getZ() * zScaleFactor + zOffset;
            byte[] rgb = getColorByRGB(point);
            //byte[] rgb = getColorByByteRGB(point); // only for test
            Vector3d position = new Vector3d(x, y, z);

            GaiaVertex vertex = new GaiaVertex();
            vertex.setPosition(position);
            vertex.setColor(rgb);

            GaiaPointCloudTemp tempFile = pointCloudHeader.findTemp(position);
            if (tempFile == null) {
                log.error("Failed to find temp file.");
            } else {
                tempFile.writePosition(position, rgb);
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
     * Reduce points by using HashMap
     * @param vertices List<GaiaVertex>
     * @return List<GaiaVertex>
     */
    private List<GaiaVertex> reducePointsA(List<GaiaVertex> vertices) {
        String format = "%.5f";
        Map<String, GaiaVertex> hashMap = new HashMap<>();
        vertices.forEach((vertex) -> {
            Vector3d position = vertex.getPosition();
            double x = position.x;
            double y = position.y;
            double z = position.z;

            String xStr = String.format(format, x);
            String yStr = String.format(format, y);
            String zStr = String.format(format, z);
            String key = xStr + "-" + yStr + "-" + zStr;
            if (hashMap.containsKey(key)) {
                //log.error("Duplicated key : {}", key);
            } else {
                hashMap.put(key, vertex);
            }
        });
        List<GaiaVertex> newVertices = new ArrayList<>(hashMap.values());
        return newVertices;
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

    /**
     * Get color by intensity (Gray scale)
     * @param point LASPoint
     * @return byte[3]
     */
    private byte[] getColorIntensity(LASPoint point) {
        char intensity = point.getIntensity();
        double intensityDouble = (double) intensity / 65535;

        byte color = (byte) (intensityDouble * 255);
        byte[] rgb = new byte[3];
        rgb[0] = color;
        rgb[1] = color;
        rgb[2] = color;
        return rgb;
    }
}
