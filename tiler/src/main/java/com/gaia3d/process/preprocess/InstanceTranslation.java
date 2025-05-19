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

        AtomicReference<Double> altitude = new AtomicReference<>((double) 0);
        String altitudeMode = kmlInfo.getAltitudeMode();
        if (altitudeMode != null && altitudeMode.equals("absolute")) {
            altitude.set(position.z);
        } else {
            try {
                coverages.forEach((coverage) -> {
                    DirectPosition2D memSave_posWorld = new DirectPosition2D(DefaultGeographicCRS.WGS84, center.x, center.y);
                    double[] memSave_alt = new double[1];
                    memSave_alt[0] = 0;
                    coverage.evaluate((DirectPosition) memSave_posWorld, memSave_alt);
                    altitude.set(memSave_alt[0]);
                });
            } catch (PointOutsideCoverageException e) {
                log.warn("[WARN] Fail to get altitude from DEM coverage. : {}", e.getMessage());
            }
        }
        position.set(position.x, position.y, altitude.get() + center.z);
        return tileInfo;
    }
}
