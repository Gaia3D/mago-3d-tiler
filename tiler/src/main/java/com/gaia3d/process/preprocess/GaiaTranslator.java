package com.gaia3d.process.preprocess;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.GaiaNode;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.KmlInfo;
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
        FormatType inputType = FormatType.fromExtension(globalOptions.getInputFormat());

        GaiaScene gaiaScene = tileInfo.getScene();
        GaiaNode rootNode = gaiaScene.getNodes().get(0);
        Matrix4d transform = rootNode.getTransformMatrix();

        Vector3d scale = new Vector3d();
        transform.getScale(scale);

        Vector3d center = getPosition(inputType, gaiaScene);
        Vector3d translation = getTranslation(gaiaScene);

        // set position terrain height
        coverages.forEach((coverage) -> {
            DirectPosition2D memSave_posWorld = new DirectPosition2D(DefaultGeographicCRS.WGS84, center.x, center.y);
            double[] memSave_alt = new double[1];
            memSave_alt[0] = 0;
            try {
                coverage.evaluate((DirectPosition) memSave_posWorld, memSave_alt);
            } catch (Exception e) {
                log.warn("Failed to evaluate terrain height", e);
            }
            //log.info("memSave_alt[0] : {}", memSave_alt[0]);
            center.z = memSave_alt[0];
        });

        KmlInfo kmlInfo = getKmlInfo(tileInfo, center);
        Matrix4d translationMatrix = new Matrix4d().translate(translation); // new
        Matrix4d resultTransfromMatrix = new Matrix4d(); // new
        translationMatrix.mul(transform, resultTransfromMatrix); // new

        rootNode.setTransformMatrix(resultTransfromMatrix);
        tileInfo.setTransformMatrix(resultTransfromMatrix);

        GaiaBoundingBox boundingBox = gaiaScene.getBoundingBox(); // new
        gaiaScene.setGaiaBoundingBox(boundingBox); // new

        tileInfo.setBoundingBox(boundingBox);
        tileInfo.setKmlInfo(kmlInfo);
        return tileInfo;
    }

    private Vector3d getTranslation(GaiaScene gaiaScene) {
        GaiaBoundingBox boundingBox = gaiaScene.getBoundingBox();
        Vector3d center = boundingBox.getCenter();
        Vector3d traslation = new Vector3d(center.x, center.y, 0.0d);
        traslation.negate();
        return traslation;
    }

    private Vector3d getPosition(FormatType formatType, GaiaScene gaiaScene) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        Vector3d position;
        if (formatType == FormatType.CITY_GML || formatType == FormatType.SHP || formatType == FormatType.GEOJSON) {
            GaiaNode rootNode = gaiaScene.getNodes().get(0);
            Matrix4d transform = rootNode.getTransformMatrix();
            Vector3d center = new Vector3d(transform.get(3,0), transform.get(3,1), 0.0d);
            position = new Vector3d(center.x, center.y, 0.0d);
        } else {
            CoordinateReferenceSystem source = globalOptions.getCrs();
            GaiaBoundingBox boundingBox = gaiaScene.getBoundingBox();
            Vector3d center = boundingBox.getCenter();
            if (source != null) {
                ProjCoordinate centerSource = new ProjCoordinate(center.x, center.y, boundingBox.getMinZ());
                ProjCoordinate centerWgs84 = GlobeUtils.transform(source, centerSource);
                position = new Vector3d(centerWgs84.x, centerWgs84.y, 0.0d);
            } else {
                position = new Vector3d(center.x, center.y, 0.0d);
            }
        }
        return position;
    }

    private KmlInfo getKmlInfo(TileInfo tileInfo, Vector3d position) {
        KmlInfo kmlInfo = tileInfo.getKmlInfo();
        if (kmlInfo == null) {
            kmlInfo = KmlInfo.builder().position(position).build();
        }
        return kmlInfo;
    }
}
