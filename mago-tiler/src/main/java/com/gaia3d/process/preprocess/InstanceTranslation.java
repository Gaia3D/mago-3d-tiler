package com.gaia3d.process.preprocess;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.terrain.GeoTiffTerrainHeightProvider;
import com.gaia3d.terrain.TerrainHeightProvider;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.joml.Vector3d;

import java.util.List;
import java.util.OptionalDouble;

@Slf4j
public class InstanceTranslation implements PreProcess {
    private final TerrainHeightProvider terrainHeightProvider;

    public InstanceTranslation(List<GridCoverage2D> coverages) {
        this(new GeoTiffTerrainHeightProvider(coverages));
    }

    public InstanceTranslation(TerrainHeightProvider terrainHeightProvider) {
        this.terrainHeightProvider = terrainHeightProvider;
    }

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

        double altitude = 0.0d;
        String altitudeMode = tileTransformInfo.getAltitudeMode();
        if (altitudeMode != null && altitudeMode.equals("absolute")) {
            altitude = position.z;
        } else {
            OptionalDouble sampledHeight = terrainHeightProvider.sample(center.x, center.y);
            if (sampledHeight.isPresent()) {
                altitude = sampledHeight.getAsDouble();
            } else {
                log.debug("[DEBUG] Failed to load terrain height. Out of range");
            }
        }
        position.set(position.x, position.y, altitude + center.z);
        return tileInfo;
    }
}
