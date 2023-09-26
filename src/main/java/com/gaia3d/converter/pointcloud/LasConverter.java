package com.gaia3d.converter.pointcloud;

import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.converter.Converter;
import com.github.mreutegg.laszip4j.LASHeader;
import com.github.mreutegg.laszip4j.LASPoint;
import com.github.mreutegg.laszip4j.LASReader;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class LasConverter implements Converter {
    @Override
    public List<GaiaScene> load(String path) {
        return convert(new File(path));
    }

    @Override
    public List<GaiaScene> load(File file) {
        return convert(file);
    }

    @Override
    public List<GaiaScene> load(Path path) {
        return convert(path.toFile());
    }

    private List<GaiaScene> convert(File file) {
        LASReader reader = new LASReader(file);
        LASHeader header = reader.getHeader();

        Iterable<LASPoint> pointIterable = reader.getPoints();

        log.info("header: {}", header);

        double xScaleFactor = header.getXScaleFactor();
        double xOffset = header.getXOffset();
        double yScaleFactor = header.getYScaleFactor();
        double yOffset = header.getYOffset();
        double zScaleFactor = header.getZScaleFactor();
        double zOffset = header.getZOffset();

        List<Vector3d> points = new ArrayList<>();
        pointIterable.forEach(point -> {
            double x = point.getX() * xScaleFactor + xOffset;
            double y = point.getY() * yScaleFactor + yOffset;
            double z = point.getZ() * zScaleFactor + zOffset;
            points.add(new Vector3d(x, y, z));
        });

        // randomize arrays
        Collections.shuffle(points);

        return null;
    }
}
