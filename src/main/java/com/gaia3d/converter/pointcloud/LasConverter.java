package com.gaia3d.converter.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.basic.structure.GaiaVertex;
import com.gaia3d.util.GlobeUtils;
import com.github.mreutegg.laszip4j.LASHeader;
import com.github.mreutegg.laszip4j.LASPoint;
import com.github.mreutegg.laszip4j.LASReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.joml.Vector3d;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class LasConverter {
    private final CommandLine command;
    private final CoordinateReferenceSystem crs;

    public LasConverter(CommandLine command, CoordinateReferenceSystem crs) {
        this.command = command;
        this.crs = crs;
    }

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
        List<GaiaPointCloud> pointClouds = new ArrayList<>();
        GaiaPointCloud pointCloud = new GaiaPointCloud();
        List<GaiaVertex> vertices = pointCloud.getVertices();

        LASReader reader = new LASReader(file);
        LASHeader header = reader.getHeader();

        log.info("[LoadFile] Loading a pointcloud file. : {}", file.getAbsolutePath());

        Iterable<LASPoint> pointIterable = reader.getPoints();
        GaiaBoundingBox boundingBox = pointCloud.getGaiaBoundingBox();

        double xScaleFactor = header.getXScaleFactor();
        double xOffset = header.getXOffset();
        double yScaleFactor = header.getYScaleFactor();
        double yOffset = header.getYOffset();
        double zScaleFactor = header.getZScaleFactor();
        double zOffset = header.getZOffset();

        pointIterable.forEach(point -> {
            double x = point.getX() * xScaleFactor + xOffset;
            double y = point.getY() * yScaleFactor + yOffset;
            double z = point.getZ() * zScaleFactor + zOffset;
            //Vector3d position = new Vector3d(x, y, z);

            ProjCoordinate coordinate = new ProjCoordinate(x, y, z);
            ProjCoordinate transformedCoordinate = GlobeUtils.transform(crs, coordinate);

            Vector3d position = new Vector3d(transformedCoordinate.x, transformedCoordinate.y, z);

            double red = (double) point.getRed() / 65535;
            double green = (double) point.getGreen() / 65535;
            double blue = (double) point.getBlue() / 65535;

            byte[] rgb = new byte[3];
            rgb[0] = (byte) (red * 255);
            rgb[1] = (byte) (green * 255);
            rgb[2] = (byte) (blue * 255);

            GaiaVertex vertex = new GaiaVertex();
            //vertex.setPosition(new Vector3d(x, y, z));
            vertex.setPosition(position);
            vertex.setColor(rgb);
            vertex.setBatchId(0);
            vertices.add(vertex);
            boundingBox.addPoint(position);
        });

        // BatchId setting
        /*for (int i = 0; i < vertices.size(); i++) {
            vertices.get(i).setBatchId(i);
        }*/

        // randomize arrays
        Collections.shuffle(vertices);
        //var divided = pointCloud.divide();
        //pointClouds.add(divided.get(0));

        pointClouds.add(pointCloud);
        return pointClouds;
    }
}
