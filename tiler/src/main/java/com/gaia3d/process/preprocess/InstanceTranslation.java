package com.gaia3d.process.preprocess;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.joml.Vector3d;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.geometry.DirectPosition;

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

        KmlInfo kmlInfo = tileInfo.getKmlInfo();
        Vector3d position = kmlInfo.getPosition();
        Vector3d center = new Vector3d(position);
        if (offset != null) {
            center.add(offset);
        } else {
            offset = new Vector3d(0.0d, 0.0d, 0.0d);
        }

        AtomicReference<Double> altitude = new AtomicReference<>(0.0d);
        String altitudeMode = kmlInfo.getAltitudeMode();
        if (altitudeMode != null && altitudeMode.equals("absolute")) {
            altitude.set(position.z);
        } else {
            // set position terrain height
            if (!coverages.isEmpty()) {
                altitude.set(0.0d);
                coverages.forEach((coverage) -> {
                    DirectPosition worldPosition = new DirectPosition2D(DefaultGeographicCRS.WGS84, center.x, center.y);
                    double[] altitudeArray = new double[1];
                    altitudeArray[0] = 0.0d;
                    try {
                        coverage.evaluate(worldPosition, altitudeArray);
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
