package com.gaia3d.process.preprocess;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.opengis.geometry.DirectPosition;

import java.util.List;

@Slf4j
@AllArgsConstructor
public class GaiaTranslator implements PreProcess {
    private final List<GridCoverage2D> coverages;

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
        if (coverages != null && !coverages.isEmpty()) {
            for (GridCoverage2D coverage : coverages) {
                DirectPosition worldPosition = new DirectPosition2D(DefaultGeographicCRS.WGS84, center.x, center.y);
                double[] altitude = new double[1];
                altitude[0] = 0.0d;

                try {
                    coverage.evaluate(worldPosition, altitude);
                } catch (Exception e) {
                    log.debug("[DEBUG] Failed to load terrain height. Out of range");
                }

                if (Double.isInfinite(altitude[0])) {
                    log.debug("[DEBUG] Failed to load terrain height. Infinite value encountered");
                } else if (Double.isNaN(altitude[0])) {
                    log.debug("[DEBUG] Failed to load terrain height. NaN value encountered");
                } else {
                    return altitude[0];
                }
            }
        }
        return 0.0d;
    }
}
