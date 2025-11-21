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

@Deprecated
@Slf4j
@AllArgsConstructor
public class GaiaStrictCoordinateExtractor implements PreProcess {

    private final GlobalOptions globalOptions = GlobalOptions.getInstance();

    @Override
    public TileInfo run(TileInfo tileInfo) {
        CoordinateReferenceSystem sourceCrs = globalOptions.getSourceCrs();
        if (sourceCrs != null && sourceCrs.getName().equals("EPSG:4978")) {
            return extractCartesian(tileInfo);
        }
        return extractAndStrictLocalize(tileInfo);
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

    private TileInfo extractAndStrictLocalize(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();
        TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();

        Vector3d sourceCenter = getOrigin(scene);
        sourceCenter = new Vector3d(sourceCenter); // Ensure we have a mutable copy

        // WGS 84 coordinate system (longitude, latitude, altitude)
        Vector3d targetCenter = extractDegree(tileTransformInfo, scene);
        tileTransformInfo.setPosition(targetCenter);

        CoordinateReferenceSystem sourceCrs = globalOptions.getSourceCrs();
        if (!sourceCrs.getName().equals("EPSG:4326")) {
            ProjCoordinate centerCoordinate = new ProjCoordinate(sourceCenter.x, sourceCenter.y, sourceCenter.z);
            ProjCoordinate centerDegreeCoordinate = GlobeUtils.transform(sourceCrs, centerCoordinate);
            Vector3d centerDegreeVector = new Vector3d(centerDegreeCoordinate.x, centerDegreeCoordinate.y, 0.0d);

            GaiaBoundingBox localBoundingBox = new GaiaBoundingBox();
            transformSceneVertexPositionsToLocalCoords(scene, centerDegreeVector, localBoundingBox);
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
        CoordinateReferenceSystem sourceCrs = globalOptions.getSourceCrs();

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

    private void transformSceneVertexPositionsToLocalCoords(GaiaScene scene, Vector3d geoCoordReference, GaiaBoundingBox localBoundingBox) {
        double[] centerCartesianWC = GlobeUtils.geographicToCartesianWgs84(geoCoordReference.x, geoCoordReference.y, geoCoordReference.z);

        Matrix4d globalTransformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerCartesianWC[0], centerCartesianWC[1], centerCartesianWC[2]);
        Matrix4d globalTransformMatrixInverse = new Matrix4d(globalTransformMatrix);
        globalTransformMatrixInverse.invert();

        List<GaiaNode> rootNodes = scene.getNodes();
        for (GaiaNode rootNode : rootNodes) {
            this.transformNodeVertexPositionsToLocalCoords(null, rootNode, globalTransformMatrixInverse, localBoundingBox);
        }
    }

    private void transformNodeVertexPositionsToLocalCoords(Matrix4d parentMatrix, GaiaNode node, Matrix4d globalTMatrixInv, GaiaBoundingBox localBoundingBox) {
        // check for meshes
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        CoordinateReferenceSystem crs = globalOptions.getSourceCrs();

        Matrix4d transformMatrix = new Matrix4d(node.getTransformMatrix());
        if (parentMatrix != null) {
            parentMatrix.mul(transformMatrix, transformMatrix);
        }

        Vector3d offset = globalOptions.getTranslateOffset();
        if (offset == null) {
            offset = new Vector3d();
        }

        List<GaiaMesh> meshes = node.getMeshes();
        if (meshes != null) {
            for (GaiaMesh gaiaMesh : meshes) {
                List<GaiaPrimitive> primitives = gaiaMesh.getPrimitives();
                if (primitives != null && !primitives.isEmpty()) {
                    for (GaiaPrimitive primitive : primitives) {
                        List<GaiaVertex> vertices = primitive.getVertices();
                        if (vertices != null && !vertices.isEmpty()) {
                            for (GaiaVertex vertex : vertices) {
                                Vector3d pos = new Vector3d(vertex.getPosition());
                                pos.add(offset);
                                transformMatrix.transformPosition(pos); // CRS coords

                                // calculate the geoCoords of the "pos"
                                ProjCoordinate vertexSource = new ProjCoordinate(pos.x, pos.y, pos.z);
                                ProjCoordinate vertexWgs84 = GlobeUtils.transform(crs, vertexSource);

                                // calculate the posWC of the "vertexWgs84"
                                double[] posWC = GlobeUtils.geographicToCartesianWgs84(vertexWgs84.x, vertexWgs84.y, vertexSource.z);
                                Vector3d posWCVector = new Vector3d(posWC[0], posWC[1], posWC[2]);
                                Vector3d posLC = globalTMatrixInv.transformPosition(posWCVector);

                                localBoundingBox.addPoint(posLC);
                                vertex.setPosition(posLC);
                            }
                        }
                    }
                }
            }
        }

        if (node.getChildren() != null) {
            for (int i = 0; i < node.getChildren().size(); i++) {
                GaiaNode childNode = node.getChildren().get(i);
                this.transformNodeVertexPositionsToLocalCoords(transformMatrix, childNode, globalTMatrixInv, localBoundingBox);
            }
        }
    }
}
