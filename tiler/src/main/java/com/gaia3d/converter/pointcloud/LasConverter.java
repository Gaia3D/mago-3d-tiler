package com.gaia3d.converter.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.basic.model.GaiaVertex;
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

import java.io.File;
import java.nio.file.Path;
import java.util.*;

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

    private List<GaiaPointCloud> convert(File file) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        List<GaiaPointCloud> pointClouds = new ArrayList<>();
        GaiaPointCloud pointCloud = new GaiaPointCloud();
        List<GaiaVertex> vertices = pointCloud.getVertices();

        LASReader reader = new LASReader(file);
        LASHeader header = reader.getHeader();

        byte major = header.getVersionMajor();
        byte minor = header.getVersionMinor();
        byte recordFormatValue = header.getPointDataRecordFormat();
        long recordLength = header.getPointDataRecordLength();

        boolean hasRgbColor;
        LasRecordFormat recordFormat = LasRecordFormat.fromFormatNumber(recordFormatValue);

        if (recordFormat != null) {
            hasRgbColor = recordFormat.hasColor;
        } else {
            hasRgbColor = false;
        }
        GaiaBoundingBox boundingBox = pointCloud.getGaiaBoundingBox();

        double xScaleFactor = header.getXScaleFactor();
        double xOffset = header.getXOffset();
        double yScaleFactor = header.getYScaleFactor();
        double yOffset = header.getYOffset();
        double zScaleFactor = header.getZScaleFactor();
        double zOffset = header.getZOffset();

        double getMinX = header.getMinX();
        double getMinY = header.getMinY();
        double getMinZ = header.getMinZ();
        double getMaxX = header.getMaxX();
        double getMaxY = header.getMaxY();
        double getMaxZ = header.getMaxZ();

        CoordinateReferenceSystem crs = globalOptions.getCrs();

        BasicCoordinateTransform transformer = new BasicCoordinateTransform(crs, GlobeUtils.wgs84);
        //ProjCoordinate minCoordinate = new ProjCoordinate(getMinX, getMinY, getMinZ);
        //ProjCoordinate maxCoordinate = new ProjCoordinate(getMaxX, getMaxY, getMaxZ);
        ProjCoordinate minCoordinate = transformer.transform(new ProjCoordinate(getMinX, getMinY, getMinZ), new ProjCoordinate());
        ProjCoordinate maxCoordinate = transformer.transform(new ProjCoordinate(getMaxX, getMaxY, getMaxZ), new ProjCoordinate());

        ProjCoordinate minTransformedCoordinate = GlobeUtils.transform(crs, minCoordinate);
        ProjCoordinate maxTransformedCoordinate = GlobeUtils.transform(crs, maxCoordinate);
        minTransformedCoordinate.z = getMinZ;
        maxTransformedCoordinate.z = getMaxZ;

        int pointSkip = globalOptions.getPointSkip();
        CloseablePointIterable pointIterable = reader.getCloseablePoints();
        long pointRecords = header.getNumberOfPointRecords();
        long legacyPointRecords = header.getLegacyNumberOfPointRecords();

        long totalPointRecords = pointRecords + legacyPointRecords;
        long totalPointRecords1percent = totalPointRecords / 100;

        log.info("[Pre] Loading a pointcloud file. : {}", file.getAbsolutePath());
        log.debug("----------------------------------------");
        log.debug(" - LAS Version : {}.{}", major, minor);
        log.debug(" - LAS Point Data Record Format : {}", recordFormat);
        log.debug(" - LAS Point Data Record Length : {}", recordLength);
        log.debug(" - LAS Point Data Record has RGB Color : {}", hasRgbColor);
        log.debug(" - LAS Total Point Records : {}", pointRecords);
        log.debug(" - LAS Total Legacy Point Records : {}", legacyPointRecords);
        log.debug(" - LAS Total Record size : {}", totalPointRecords * recordLength);
        log.debug(" - LAS Min Coordinate : {}", minTransformedCoordinate);
        log.debug(" - LAS Max Coordinate : {}", maxTransformedCoordinate);
        log.debug("----------------------------------------");

        long pointIndex = 0;
        for (LASPoint point : pointIterable) {
            if (pointIndex % totalPointRecords1percent == 0 && pointIndex != 0) {
                log.debug(" - Las Records Loading progress. ({}/100)%", pointIndex / totalPointRecords1percent);
            }
            if (pointIndex % pointSkip == 0) {
                double x = point.getX() * xScaleFactor + xOffset;
                double y = point.getY() * yScaleFactor + yOffset;
                double z = point.getZ() * zScaleFactor + zOffset;

                ProjCoordinate coordinate = new ProjCoordinate(x, y, z);
                ProjCoordinate transformedCoordinate = new ProjCoordinate();
                transformer.transform(coordinate, transformedCoordinate);
                //ProjCoordinate transformedCoordinate = GlobeUtils.transform(crs, coordinate);
                Vector3d position = new Vector3d(transformedCoordinate.x, transformedCoordinate.y, z);
                coordinate = null;
                transformedCoordinate = null;

                byte[] rgb;
                if (hasRgbColor) {
                    rgb = getColorByRGB(point);
                    //rgb = getColorByByteRGB(point); // only for test
                } else {
                    rgb = getColorIntensity(point);
                }

                GaiaVertex vertex = new GaiaVertex();
                vertex.setPosition(position);
                vertex.setColor(rgb);
                vertex.setBatchId(0);
                vertices.add(vertex);
                boundingBox.addPoint(position);
            }
            pointIndex++;
        }
        log.debug("----------------------------------------");

        pointIterable.close();

        // randomize arrays
        Collections.shuffle(vertices);

        pointCloud.setVertices(vertices);
        pointClouds.add(pointCloud);
        System.gc();

        return pointClouds;
    }

    private void transformPositions(List<GaiaVertex> vertices) {
        vertices.forEach((vertex) -> {
            Vector3d positions = vertex.getPosition();
            ProjCoordinate coordinate = new ProjCoordinate(positions.x, positions.y, positions.z);
            ProjCoordinate transformedCoordinate = GlobeUtils.transform(GlobalOptions.getInstance().getCrs(), coordinate);
            positions.x = transformedCoordinate.x;
            positions.y = transformedCoordinate.y;
            positions.z = transformedCoordinate.z;
        });
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
