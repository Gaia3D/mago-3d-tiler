package com.gaia3d.process.preprocess;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.geometry.Position;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.List;

@Slf4j
@AllArgsConstructor
public class GaiaTranslator implements PreProcess {
    private final List<GridCoverage2D> terrains;
    private final List<GridCoverage2D> geoids;

    @Override
    public TileInfo run(TileInfo tileInfo) {
        TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
        if (tileTransformInfo == null) {
            return tileInfo;
        }

        GlobalOptions globalOptions = GlobalOptions.getInstance();

        GaiaScene gaiaScene = tileInfo.getScene();
        GaiaBoundingBox boundingBox = gaiaScene.updateBoundingBox();

        Vector3d floorCenter = tileTransformInfo.getPosition();
        double terrainHeight = getTerrainHeightFromCartographic(floorCenter);

        Vector3d translateOffset = globalOptions.getTranslateOffset();
        Vector3d translation = new Vector3d(translateOffset.x, translateOffset.y, terrainHeight + translateOffset.z);

        List<GaiaNode> nodes = gaiaScene.getNodes();
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
        double resultHeight = 0.0d;
        if (terrains != null && !terrains.isEmpty()) {
            for (GridCoverage2D coverage : terrains) {
                double[] altitude = new double[1];
                altitude[0] = 0.0d;

                try {
                    coverage.evaluate(position, altitude);
                } catch (Exception e) {
                    log.debug("[DEBUG] Failed to load terrain height. Out of range");
                }

                if (Double.isInfinite(altitude[0])) {
                    log.debug("[DEBUG] Failed to load terrain height. Infinite value encountered");
                } else if (Double.isNaN(altitude[0])) {
                    log.debug("[DEBUG] Failed to load terrain height. NaN value encountered");
                } else {
                    resultHeight += altitude[0];
                }
            }
        }

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
