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
        if (source == null && tileInfo != null) {
            return tileInfo;
        }
        assert tileInfo != null;
        GaiaScene gaiaScene = tileInfo.getScene();
        GaiaBoundingBox boundingBox = gaiaScene.getBoundingBox();

        Vector3d center = boundingBox.getCenter();
        Vector3d traslation = new Vector3d(center.x, center.y, 0.0d);
        traslation.negate();

        GaiaNode rootNode = gaiaScene.getNodes().get(0);
        Matrix4d transform = rootNode.getTransformMatrix();

        String inputExtension = command.getOptionValue(ProcessOptions.INPUT_TYPE.getArgName());
        FormatType formatType = FormatType.fromExtension(inputExtension);

        if (formatType == FormatType.CITY_GML || formatType == FormatType.SHP) {
            center = new Vector3d(transform.get(3,0), transform.get(3,1), 0.0d);
        }

        // lon/lat position
        ProjCoordinate centerSource = new ProjCoordinate(center.x, center.y, boundingBox.getMinZ());
        ProjCoordinate centerWgs84 = GlobeUtils.transform(source, centerSource);
        Vector3d position = new Vector3d(centerWgs84.x, centerWgs84.y, 0.0d);
        KmlInfo kmlInfo = KmlInfo.builder().position(position).build();


        Matrix4d resultTransfromMatrix = transform.translate(traslation, new Matrix4d());
        rootNode.setTransformMatrix(resultTransfromMatrix);

        boundingBox = gaiaScene.getBoundingBox();
        gaiaScene.setGaiaBoundingBox(boundingBox);
        tileInfo.setTransformMatrix(resultTransfromMatrix);
        tileInfo.setBoundingBox(boundingBox);
        tileInfo.setKmlInfo(kmlInfo);
        return tileInfo;
    }
}
