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
import org.geotools.geopkg.Tile;
import org.joml.Matrix3d;
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
        TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();

        CoordinateReferenceSystem sourceCrs = globalOptions.getCrs();
        if (sourceCrs != null && sourceCrs.getName().equals("EPSG:4978")) {
            log.info("[INFO] Using EPSG:4978 coordinate system.");
            return extractCartesian(tileInfo);
        }
        return extractAndLocalize(tileInfo);
    }

    private TileInfo extractAndLocalize(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();
        TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();

        List<GaiaNode> nodes = scene.getNodes();
        Vector3d sourceCenter = getOrigin(scene);

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

    private TileInfo extractCartesian(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();
        TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();

        List<GaiaNode> nodes = scene.getNodes();
        GaiaBoundingBox boundingBox = scene.updateBoundingBox();
        Vector3d sourceCenter = new Vector3d(boundingBox.getCenter());
        tileTransformInfo.setPosition(sourceCenter);

        Vector3d translation = sourceCenter.negate(new Vector3d());
        Matrix4d transformMatrix = new Matrix4d().identity();
        transformMatrix.setTranslation(translation);
        for (GaiaNode node : nodes) {
            Matrix4d transform = node.getTransformMatrix();
            transform.mul(transformMatrix, transform);
            node.setTransformMatrix(transform);
        }
        tileInfo.updateSceneInfo();
        return tileInfo;
    }


    private Vector3d getOrigin(GaiaScene scene) {
        Vector3d sourceCenter = new Vector3d(0.0d, 0.0d, 0.0d);
        FormatType formatType = globalOptions.getInputFormat();
        boolean isParametric = formatType == FormatType.CITYGML || formatType == FormatType.INDOORGML || formatType == FormatType.SHP || formatType == FormatType.GEOJSON || formatType == FormatType.GEO_PACKAGE;
        if (isParametric) {

        } else if (formatType == FormatType.KML) {

        } else {
            GaiaBoundingBox boundingBox = scene.updateBoundingBox();
            sourceCenter = new Vector3d(boundingBox.getCenter());
        }

        sourceCenter.z = 0.0d;
        return sourceCenter;
    }

    private Vector3d extractDegree(TileTransformInfo tileTransformInfo, GaiaScene scene) {
        CoordinateReferenceSystem sourceCrs = globalOptions.getCrs();

        Vector3d degreeCenter = new Vector3d(0.0d, 0.0d, 0.0d);
        FormatType formatType = globalOptions.getInputFormat();
        boolean isParametric = globalOptions.isParametric();
        if (isParametric) {
            Vector3d translation = scene.getTranslation();
            degreeCenter.set(translation);
        } else if (formatType == FormatType.KML) {
            Vector3d position = tileTransformInfo.getPosition();
            degreeCenter.set(position);
        } else {
            GaiaBoundingBox boundingBox = scene.updateBoundingBox();
            Vector3d boxCenter = boundingBox.getCenter();
            if (sourceCrs != null && sourceCrs.getName().equals("EPSG:4978")) {
                degreeCenter = GlobeUtils.cartesianToGeographicWgs84(boxCenter);
                log.info("[INFO] Using EPSG:4978 coordinate system. Center: {} -> {}", boxCenter, degreeCenter);
            } else if (sourceCrs != null) {
                ProjCoordinate centerSource = new ProjCoordinate(boxCenter.x, boxCenter.y, boxCenter.z);
                ProjCoordinate centerWgs84 = GlobeUtils.transform(sourceCrs, centerSource);
                degreeCenter = new Vector3d(centerWgs84.x, centerWgs84.y, 0);
            } else {
                degreeCenter = new Vector3d(boxCenter.x, boxCenter.y, 0);
            }
        }
        return degreeCenter;
    }

    private Matrix3d clampEpsilonMatrix(Matrix3d matrix) {
        double epsilon = 1e-1;
        Matrix3d clampedMatrix = new Matrix3d(matrix);
        clampedMatrix.m00(clampEpsilon(matrix.m00(), epsilon));
        clampedMatrix.m01(clampEpsilon(matrix.m01(), epsilon));
        clampedMatrix.m02(clampEpsilon(matrix.m02(), epsilon));

        clampedMatrix.m10(clampEpsilon(matrix.m10(), epsilon));
        clampedMatrix.m11(clampEpsilon(matrix.m11(), epsilon));
        clampedMatrix.m12(clampEpsilon(matrix.m12(), epsilon));

        clampedMatrix.m20(clampEpsilon(matrix.m20(), epsilon));
        clampedMatrix.m21(clampEpsilon(matrix.m21(), epsilon));
        clampedMatrix.m22(clampEpsilon(matrix.m22(), epsilon));

        return clampedMatrix;
    }

    public static double clampEpsilon(double value, double epsilon) {
        if (Math.abs(value) < epsilon) {
            return 0.0f;
        } else if (Math.abs(value - 1.0f) < epsilon) {
            return 1.0f;
        } else if (Math.abs(value + 1.0f) < epsilon) {
            return -1.0f;
        } else if (value > 1.0f) {
            return 1.0f;
        } else if (value < -1.0f) {
            return -1.0f;
        }
        return value;
    }
}
