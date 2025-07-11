package com.gaia3d.process.preprocess;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.GlobeUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import java.util.List;

@Slf4j
@AllArgsConstructor
public class GaiaCoordinateExtractor implements PreProcess {

    private final GlobalOptions globalOptions = GlobalOptions.getInstance();

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();

        List<GaiaNode> nodes = scene.getNodes();
        Vector3d sourceCenter = getOrigin(scene);
        TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();

        sourceCenter = new Vector3d(sourceCenter); // Ensure we have a mutable copy

        // WGS 84 coordinate system (longitude, latitude, altitude)
        Vector3d targetCenter = extractDegree(tileTransformInfo, scene);
        tileTransformInfo.setPosition(targetCenter);

        Vector3d translation = new Vector3d(-sourceCenter.x, -sourceCenter.y, -sourceCenter.z);
        Matrix4d translationMatrix = new Matrix4d().translate(translation);
        for (GaiaNode node : nodes) {
            Matrix4d transform = node.getTransformMatrix();
            transform.mul(translationMatrix);
            node.setTransformMatrix(transform);
        }
        tileInfo.updateSceneInfo();
        return tileInfo;
    }

    private Vector3d getOrigin(GaiaScene scene) {
        Vector3d sourceCenter;
        FormatType formatType = globalOptions.getInputFormat();
        boolean isParametric = formatType == FormatType.CITYGML || formatType == FormatType.INDOORGML || formatType == FormatType.SHP || formatType == FormatType.GEOJSON || formatType == FormatType.GEO_PACKAGE;
        if (isParametric) {
            GaiaNode rootNode = scene.getNodes().get(0);
            Matrix4d transformMatrix = rootNode.getTransformMatrix();
            sourceCenter = transformMatrix.getTranslation(new Vector3d());
            //sourceCenter.z = 0.0d;
        } else if (formatType == FormatType.KML) {
            sourceCenter = new Vector3d(0.0d, 0.0d, 0.0d);
        } else {
            GaiaBoundingBox boundingBox = scene.updateBoundingBox();
            sourceCenter = new Vector3d(boundingBox.getCenter());
            //double floorZ = boundingBox.getMinZ();
            //sourceCenter.z = floorZ; // Use the minimum Z value as the source center Z
        }
        sourceCenter.z = 0.0d; // Ensure Z is set to 0 for consistency
        return sourceCenter;
    }

    private Vector3d extractDegree(TileTransformInfo tileTransformInfo, GaiaScene gaiaScene) {
        Vector3d degreeCenter;
        CoordinateReferenceSystem source = globalOptions.getCrs();
        FormatType formatType = globalOptions.getInputFormat();
        boolean isParametric = globalOptions.isParametric();
        if (isParametric) {
            GaiaNode rootNode = gaiaScene.getNodes().get(0);
            Matrix4d transformMatrix = rootNode.getTransformMatrix();
            degreeCenter = transformMatrix.getTranslation(new Vector3d());
            degreeCenter.z = 0.0d; // Set Z to 0 for parametric formats
        } else if (formatType == FormatType.KML) {
            Vector3d position = tileTransformInfo.getPosition();
            degreeCenter = new Vector3d(position);
        } else {
            GaiaBoundingBox boundingBox = gaiaScene.updateBoundingBox();
            double floorZ = boundingBox.getMinZ();
            Vector3d boxCenter = boundingBox.getCenter();
            if (source != null) {
                ProjCoordinate centerSource = new ProjCoordinate(boxCenter.x, boxCenter.y, boxCenter.z);
                ProjCoordinate centerWgs84 = GlobeUtils.transform(source, centerSource);
                degreeCenter = new Vector3d(centerWgs84.x, centerWgs84.y, 0);
            } else {
                degreeCenter = new Vector3d(boxCenter.x, boxCenter.y, 0);
            }
        }
        return degreeCenter;
    }
}
