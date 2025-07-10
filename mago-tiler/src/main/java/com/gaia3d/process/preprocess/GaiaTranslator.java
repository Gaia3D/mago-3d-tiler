package com.gaia3d.process.preprocess;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.GlobeUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import org.opengis.geometry.DirectPosition;

import java.util.List;

@Slf4j
@AllArgsConstructor
public class GaiaTranslator implements PreProcess {
    private final List<GridCoverage2D> coverages;

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        FormatType inputType = globalOptions.getInputFormat();

        GaiaScene gaiaScene = tileInfo.getScene();
        GaiaNode rootNode = gaiaScene.getNodes().get(0);
        Matrix4d transform = rootNode.getTransformMatrix();

        Vector3d scale = new Vector3d();
        transform.getScale(scale);

        Vector3d center = getPosition(inputType, gaiaScene);
        /*Vector3d translation = getTranslation(gaiaScene);
        if (inputType.equals(FormatType.KML)) {
            translation = new Vector3d(0.0d, 0.0d, 0.0d);
        }*/

        // set position terrain height
        if (!coverages.isEmpty()) {
            center.z = 0.0d;
            coverages.forEach((coverage) -> {
                DirectPosition worldPosition = new DirectPosition2D(DefaultGeographicCRS.WGS84, center.x, center.y);
                double[] altitude = new double[1];
                altitude[0] = 0.0d;
                try {
                    coverage.evaluate(worldPosition, altitude);
                } catch (Exception e) {
                    log.debug("[DEBUG] Failed to load terrain height. Out of range");
                }
                if (altitude[0] != 0.0d && !Double.isNaN(altitude[0])) {
                    center.z = altitude[0];
                }
            });
        }

        //TileTransformInfo tileTransformInfo = getKmlInfo(tileInfo, center);
        //Matrix4d translationMatrix = new Matrix4d().translate(translation);
        //Matrix4d resultTransformMatrix = new Matrix4d();
        //translationMatrix.mul(transform, resultTransformMatrix);
        //rootNode.setTransformMatrix(resultTransformMatrix);
        //tileInfo.setTransformMatrix(resultTransformMatrix);
        //GaiaBoundingBox boundingBox = gaiaScene.updateBoundingBox();
        //tileInfo.setBoundingBox(boundingBox);
        //tileInfo.setTileTransformInfo(tileTransformInfo);
        return tileInfo;
    }

    private Vector3d getTranslation(GaiaScene gaiaScene) {
        GaiaBoundingBox boundingBox = gaiaScene.updateBoundingBox();
        Vector3d center = boundingBox.getCenter();
        Vector3d translation = new Vector3d(center.x, center.y, 0.0d);
        translation.negate();
        return translation;
    }

    private Vector3d getPosition(FormatType formatType, GaiaScene gaiaScene) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        Vector3d position;
        Vector3d offset = globalOptions.getTranslateOffset();
        if (offset == null) {
            offset = new Vector3d();
        }
        if (formatType == FormatType.CITYGML || formatType == FormatType.INDOORGML || formatType == FormatType.SHP || formatType == FormatType.GEOJSON || formatType == FormatType.GEO_PACKAGE) {
            GaiaNode rootNode = gaiaScene.getNodes().get(0);
            Matrix4d transform = rootNode.getTransformMatrix();
            Vector3d center = new Vector3d(transform.get(3, 0), transform.get(3, 1), 0.0d);
            center.add(offset);
            position = new Vector3d(center.x, center.y, offset.z);
        } else {
            CoordinateReferenceSystem source = globalOptions.getCrs();
            GaiaBoundingBox boundingBox = gaiaScene.updateBoundingBox();
            Vector3d center = boundingBox.getCenter();
            center.add(offset);
            if (source != null) {
                ProjCoordinate centerSource = new ProjCoordinate(center.x, center.y, boundingBox.getMinZ());
                ProjCoordinate centerWgs84 = GlobeUtils.transform(source, centerSource);
                position = new Vector3d(centerWgs84.x, centerWgs84.y, offset.z);
            } else {
                position = new Vector3d(center.x, center.y, 0.0d);
            }
        }
        return position;
    }

    @Deprecated
    private TileTransformInfo getKmlInfo(TileInfo tileInfo, Vector3d position) {
        TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
        if (tileTransformInfo == null) {
            tileTransformInfo = TileTransformInfo.builder().position(position).build();
        }
        return tileTransformInfo;
    }
}
