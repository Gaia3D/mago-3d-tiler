package com.gaia3d.process.preprocess;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.GaiaNode;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.process.ProcessOptions;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.GlobeUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

@Slf4j
@AllArgsConstructor
public class GaiaTranslator implements PreProcess {

    private final CoordinateReferenceSystem source;
    private final CommandLine command;

    @Override
    public TileInfo run(TileInfo tileInfo) {
        String inputExtension = command.getOptionValue(ProcessOptions.INPUT_TYPE.getArgName());
        FormatType formatType = FormatType.fromExtension(inputExtension);
        /*if (formatType == FormatType.KML) {
            return tileInfo;
        }*/

        /*if (source == null && tileInfo != null) {
            return tileInfo;
        }*/

        GaiaScene gaiaScene = tileInfo.getScene();
        GaiaNode rootNode = gaiaScene.getNodes().get(0);
        //GaiaBoundingBox boundingBox = gaiaScene.getBoundingBox();
        Matrix4d transform = rootNode.getTransformMatrix();

        Vector3d center = getPosition(formatType, gaiaScene);
        Vector3d translation = getTranslation(gaiaScene);
        KmlInfo kmlInfo = getKmlInfo(tileInfo, center);

        /*GaiaBoundingBox boundingBox = gaiaScene.getBoundingBox();

        Vector3d center = boundingBox.getCenter();
        Vector3d traslation = new Vector3d(center.x, center.y, 0.0d);
        traslation.negate();

        GaiaNode rootNode = gaiaScene.getNodes().get(0);
        Matrix4d transform = rootNode.getTransformMatrix();

        Vector3d position = new Vector3d(center.x, center.y, 0.0d);
        if (formatType == FormatType.CITY_GML || formatType == FormatType.SHP || formatType == FormatType.GEOJSON) {
            center = new Vector3d(transform.get(3,0), transform.get(3,1), 0.0d);
            position = new Vector3d(center.x, center.y, 0.0d);
        } else {
            ProjCoordinate centerSource = new ProjCoordinate(center.x, center.y, boundingBox.getMinZ());
            ProjCoordinate centerWgs84 = GlobeUtils.transform(source, centerSource);
            position = new Vector3d(centerWgs84.x, centerWgs84.y, 0.0d);
        }
        KmlInfo kmlInfo = KmlInfo.builder().position(position).build();*/

        Matrix4d resultTransfromMatrix = transform.translate(translation, new Matrix4d());
        rootNode.setTransformMatrix(resultTransfromMatrix);

        GaiaBoundingBox boundingBox = gaiaScene.getBoundingBox();
        gaiaScene.setGaiaBoundingBox(boundingBox);
        tileInfo.setTransformMatrix(resultTransfromMatrix);
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
        Vector3d position;
        if (formatType == FormatType.CITY_GML || formatType == FormatType.SHP || formatType == FormatType.GEOJSON) {
            GaiaNode rootNode = gaiaScene.getNodes().get(0);
            Matrix4d transform = rootNode.getTransformMatrix();
            Vector3d center = new Vector3d(transform.get(3,0), transform.get(3,1), 0.0d);
            position = new Vector3d(center.x, center.y, 0.0d);
        } else {
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
