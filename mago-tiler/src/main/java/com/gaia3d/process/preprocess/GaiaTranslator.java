package com.gaia3d.process.preprocess;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.terrain.GeoTiffTerrainHeightProvider;
import com.gaia3d.terrain.TerrainHeightProvider;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.geometry.Position;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.List;

@Slf4j
public class GaiaTranslator implements PreProcess {
    private final TerrainHeightProvider terrainHeightProvider;
    private final List<GridCoverage2D> geoids;
    private GaiaScene recentScene = null;

    public GaiaTranslator(List<GridCoverage2D> terrains, List<GridCoverage2D> geoids) {
        this(new GeoTiffTerrainHeightProvider(terrains), geoids);
    }

    public GaiaTranslator(TerrainHeightProvider terrainHeightProvider, List<GridCoverage2D> geoids) {
        this.terrainHeightProvider = terrainHeightProvider;
        this.geoids = geoids;
    }

    @Override
    public TileInfo run(TileInfo tileInfo) {
        TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
        if (tileTransformInfo == null) {
            return tileInfo;
        }

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        GaiaScene scene = tileInfo.getScene();
        if (recentScene == scene) {
            return tileInfo;
        }
        recentScene = scene;

        GaiaBoundingBox boundingBox = scene.updateBoundingBox();

        Vector3d floorCenter = tileTransformInfo.getPosition();
        double terrainHeight = getTerrainHeightFromCartographic(floorCenter);

        Vector3d translateOffset = globalOptions.getTranslateOffset();
        Vector3d translation = new Vector3d(translateOffset.x, translateOffset.y, terrainHeight + translateOffset.z);

        List<GaiaNode> nodes = scene.getNodes();
        for (GaiaNode node : nodes) {
            Matrix4d transform = node.getTransformMatrix();
            Matrix4d translateMatrix = new Matrix4d().identity();
            translateMatrix.translate(translation);
            transform.mul(translateMatrix, transform);
        }

        tileInfo.updateSceneInfo();
        return tileInfo;
    }

    private double getTerrainHeightFromCartographic(Vector3d cartographic) {
        Vector3d center = new Vector3d(cartographic.x, cartographic.y, 0.0);
        Position position = new Position2D(DefaultGeographicCRS.WGS84, center.x, center.y);
        double resultHeight = terrainHeightProvider.sample(center.x, center.y).orElse(0.0d);

        if (geoids != null && !geoids.isEmpty()) {
            for (GridCoverage2D coverage : geoids) {
                double[] geoidHeight = new double[1];
                geoidHeight[0] = 0.0d;

                try {
                    coverage.evaluate(position, geoidHeight);
                } catch (Exception e) {
                    log.debug("[DEBUG] Failed to load geoid height. Out of range");
                }

                if (Double.isInfinite(geoidHeight[0])) {
                    log.debug("[DEBUG] Failed to load geoid height. Infinite value encountered");
                } else if (Double.isNaN(geoidHeight[0])) {
                    log.debug("[DEBUG] Failed to load geoid height. NaN value encountered");
                } else {
                    resultHeight += geoidHeight[0];
                }
            }
        }
        return resultHeight;
    }
}
