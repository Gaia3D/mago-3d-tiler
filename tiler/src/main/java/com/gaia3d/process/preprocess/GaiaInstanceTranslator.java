package com.gaia3d.process.preprocess;

import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.joml.Vector3d;
import org.opengis.geometry.DirectPosition;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@AllArgsConstructor
public class GaiaInstanceTranslator implements PreProcess {
    private final List<GridCoverage2D> coverages;
    @Override
    public TileInfo run(TileInfo tileInfo) {
        KmlInfo kmlInfo = tileInfo.getKmlInfo();
        Vector3d position = kmlInfo.getPosition();
        Vector3d center = new Vector3d(position);

        AtomicReference<Double> altitude = new AtomicReference<>((double) 0);
        coverages.forEach((coverage) -> {
            DirectPosition2D memSave_posWorld = new DirectPosition2D(DefaultGeographicCRS.WGS84, center.x, center.y);
            double[] memSave_alt = new double[1];
            memSave_alt[0] = 0;
            coverage.evaluate((DirectPosition) memSave_posWorld, memSave_alt);
            //log.info("altitude[0] : {}", memSave_alt[0]);
            altitude.set(memSave_alt[0]);
        });

        position.set(position.x, position.y, altitude.get());
        return tileInfo;
    }
}
