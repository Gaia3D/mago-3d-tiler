package com.gaia3d.process.preprocess;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.geometry.Position;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.joml.Vector3d;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@AllArgsConstructor
public class InstanceTranslation implements PreProcess {
    private final List<GridCoverage2D> coverages;

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        Vector3d offset = globalOptions.getTranslateOffset();

        TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
        Vector3d position = tileTransformInfo.getPosition();
        Vector3d center = new Vector3d(position);
        if (offset != null) {
            center.add(offset);
        } else {
            offset = new Vector3d(0.0d, 0.0d, 0.0d);
        }

        AtomicReference<Double> altitude = new AtomicReference<>(0.0d);
        String altitudeMode = tileTransformInfo.getAltitudeMode();
        if (altitudeMode != null && altitudeMode.equals("absolute")) {
            altitude.set(position.z);
        } else {
            // set position terrain height
            if (!coverages.isEmpty()) {
                altitude.set(0.0d);
                coverages.forEach((coverage) -> {
                    Position position2d = new Position2D(DefaultGeographicCRS.WGS84, center.x, center.y);
                    double[] altitudeArray = new double[1];
                    altitudeArray[0] = 0.0d;
                    try {
                        coverage.evaluate(position2d, altitudeArray);
                    } catch (Exception e) {
                        log.debug("[DEBUG] Failed to load terrain height. Out of range");
                    }
                    if (altitudeArray[0] != 0.0d && !Double.isNaN(altitudeArray[0])) {
                        altitude.set(altitudeArray[0]);
                    }
                });
            }
        }
        position.set(position.x, position.y, altitude.get() + center.z);
        return tileInfo;
    }
}
