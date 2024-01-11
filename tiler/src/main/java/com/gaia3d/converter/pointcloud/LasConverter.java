package com.gaia3d.converter.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.basic.structure.GaiaVertex;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.util.GlobeUtils;
import com.github.mreutegg.laszip4j.LASHeader;
import com.github.mreutegg.laszip4j.LASPoint;
import com.github.mreutegg.laszip4j.LASReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        log.info("[LoadFile] Loading a pointcloud file. : {}", file.getAbsolutePath());
        log.info(" - LAS Version : {}.{}", major, minor);
        log.info(" - LAS Point Data Record Format : {}", recordFormat);
        log.info(" - LAS Point Data Record Length : {}", recordLength);
        log.info(" - LAS Point Data Record has RGB Color : {}", hasRgbColor);

        Iterable<LASPoint> pointIterable = reader.getPoints();
        GaiaBoundingBox boundingBox = pointCloud.getGaiaBoundingBox();

        double xScaleFactor = header.getXScaleFactor();
        double xOffset = header.getXOffset();
        double yScaleFactor = header.getYScaleFactor();
        double yOffset = header.getYOffset();
        double zScaleFactor = header.getZScaleFactor();
        double zOffset = header.getZOffset();

        CoordinateReferenceSystem crs = globalOptions.getCrs();
        pointIterable.forEach(point -> {
            double x = point.getX() * xScaleFactor + xOffset;
            double y = point.getY() * yScaleFactor + yOffset;
            double z = point.getZ() * zScaleFactor + zOffset;

            ProjCoordinate coordinate = new ProjCoordinate(x, y, z);
            ProjCoordinate transformedCoordinate = GlobeUtils.transform(crs, coordinate);

            Vector3d position = new Vector3d(transformedCoordinate.x, transformedCoordinate.y, z);

            byte[] rgb;
            if (hasRgbColor) {
                rgb = getColorByRGB(point);
            } else {
                rgb = getColorIntensity(point);
            }

            GaiaVertex vertex = new GaiaVertex();
            vertex.setPosition(position);
            vertex.setColor(rgb);
            vertex.setBatchId(0);
            vertices.add(vertex);
            boundingBox.addPoint(position);
        });

        // randomize arrays
        Collections.shuffle(vertices);

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
