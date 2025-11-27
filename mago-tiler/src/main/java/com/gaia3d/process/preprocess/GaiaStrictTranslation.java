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
import org.geotools.api.geometry.Position;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import java.util.List;

@Slf4j
@AllArgsConstructor
public class GaiaStrictTranslation implements PreProcess {
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

        Vector3d centerGeoCoord = getPosition(inputType, gaiaScene);

        GaiaBoundingBox bboxLC = new GaiaBoundingBox();
        this.transformSceneVertexPositionsToLocalCoords(gaiaScene, centerGeoCoord, bboxLC);

        // set position terrain height
        if (!coverages.isEmpty()) {
            centerGeoCoord.z = 0.0d;
            coverages.forEach((coverage) -> {
                Position position2D = new Position2D(DefaultGeographicCRS.WGS84, centerGeoCoord.x, centerGeoCoord.y);
                double[] altitudeArray = new double[1];
                altitudeArray[0] = 0.0d;
                try {
                    coverage.evaluate(position2D, altitudeArray);
                } catch (Exception e) {
                    log.debug("[DEBUG] Failed to load terrain height. Out of range");
                }
                if (altitudeArray[0] != 0.0d && !Double.isNaN(altitudeArray[0])) {
                    centerGeoCoord.z = altitudeArray[0];
                }
            });
        }

        // calculate cartographic bounding box
        double[] centerCartesianWC = GlobeUtils.geographicToCartesianWgs84(centerGeoCoord.x, centerGeoCoord.y, centerGeoCoord.z);
        Matrix4d tMatrixAtCenterGeoCoord = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerCartesianWC[0], centerCartesianWC[1], centerCartesianWC[2]);
        //Matrix4d globalTMatrixInv = new Matrix4d(tMatrixAtCenterGeoCoord);
        //globalTMatrixInv.invert();

        // Calculate cartographicBoundingBox
        double minPosLCX = bboxLC.getMinX();
        double minPosLCY = bboxLC.getMinY();
        double minPosLCZ = bboxLC.getMinZ();

        double maxPosLCX = bboxLC.getMaxX();
        double maxPosLCY = bboxLC.getMaxY();
        double maxPosLCZ = bboxLC.getMaxZ();

        Vector3d leftDownBottomLC = new Vector3d(minPosLCX, minPosLCY, minPosLCZ);
        Vector3d rightDownBottomLC = new Vector3d(maxPosLCX, minPosLCY, minPosLCZ);
        Vector3d rightUpBottomLC = new Vector3d(maxPosLCX, maxPosLCY, minPosLCZ);

        Vector3d leftDownBottomWC = tMatrixAtCenterGeoCoord.transformPosition(leftDownBottomLC);
        Vector3d geoCoordLeftDownBottom = GlobeUtils.cartesianToGeographicWgs84(leftDownBottomWC);

        Vector3d rightDownBottomWC = tMatrixAtCenterGeoCoord.transformPosition(rightDownBottomLC);
        Vector3d geoCoordRightDownBottom = GlobeUtils.cartesianToGeographicWgs84(rightDownBottomWC);

        Vector3d rightUpBottomWC = tMatrixAtCenterGeoCoord.transformPosition(rightUpBottomLC);
        Vector3d geoCoordRightUpBottom = GlobeUtils.cartesianToGeographicWgs84(rightUpBottomWC);

        double minLonDegCut = geoCoordLeftDownBottom.x;
        double minLatDegCut = geoCoordLeftDownBottom.y;
        double maxLonDegCut = geoCoordRightDownBottom.x;
        double maxLatDegCut = geoCoordRightUpBottom.y;

        GaiaBoundingBox cartographicBoundingBox = new GaiaBoundingBox(minLonDegCut, minLatDegCut, bboxLC.getMinZ(), maxLonDegCut, maxLatDegCut, bboxLC.getMaxZ(), false);
        tileInfo.setCartographicBBox(cartographicBoundingBox);
        // End calculate cartographicBoundingBox.---

        TileTransformInfo tileTransformInfo = getKmlInfo(tileInfo, centerGeoCoord);

        rootNode.setTransformMatrix(transform);
        GaiaBoundingBox boundingBox = gaiaScene.updateBoundingBox();
        gaiaScene.setGaiaBoundingBox(boundingBox);

        tileInfo.setBoundingBox(boundingBox);
        tileInfo.setTileTransformInfo(tileTransformInfo);
        return tileInfo;
    }

    private void transformSceneVertexPositionsToLocalCoords(GaiaScene scene, Vector3d geoCoordReference, GaiaBoundingBox resultBBoxLocalCoords) {
        double[] centerCartesianWC = GlobeUtils.geographicToCartesianWgs84(geoCoordReference.x, geoCoordReference.y, geoCoordReference.z);
        Matrix4d transformMatrixAtCenter = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerCartesianWC[0], centerCartesianWC[1], centerCartesianWC[2]);
        Matrix4d globalTMatrixInv = new Matrix4d(transformMatrixAtCenter);
        globalTMatrixInv.invert();
        List<GaiaNode> rootNodes = scene.getNodes();
        for (GaiaNode rootNode : rootNodes) {
            this.transformNodeVertexPositionsToLocalCoords(rootNode, globalTMatrixInv, null, resultBBoxLocalCoords);
        }
        // now set all node's transform matrix as identity, bcos in "transformNodeVertexPositionsToLocalCoords" function we used the nodes tMatrix
        for (GaiaNode rootNode : rootNodes) {
            this.setNodesTransformMatrixAsIdentity(rootNode);
        }
    }

    private void transformNodeVertexPositionsToLocalCoords(GaiaNode node, Matrix4d globalTMatrixInv, Matrix4d parentMatrix, GaiaBoundingBox resultBBoxLC) {
        // check for meshes
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        CoordinateReferenceSystem crs = globalOptions.getSourceCrs();

        // check node's parent matrix
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

                                resultBBoxLC.addPoint(posLC);

                                // finally set the position of the vertex
                                vertex.setPosition(posLC);
                            }
                        }
                    }
                }
            }
        }

        // check for children
        if (node.getChildren() != null) {
            for (int i = 0; i < node.getChildren().size(); i++) {
                GaiaNode childNode = node.getChildren().get(i);
                this.transformNodeVertexPositionsToLocalCoords(childNode, globalTMatrixInv, transformMatrix, resultBBoxLC);
            }
        }
    }

    private void setNodesTransformMatrixAsIdentity(GaiaNode node) {
        // check node's parent matrix
        Matrix4d transformMatrix = node.getTransformMatrix();
        transformMatrix.identity();

        // check for children
        if (node.getChildren() != null) {
            for (int i = 0; i < node.getChildren().size(); i++) {
                GaiaNode childNode = node.getChildren().get(i);
                this.setNodesTransformMatrixAsIdentity(childNode);
            }
        }
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
            CoordinateReferenceSystem source = globalOptions.getSourceCrs();
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

    private TileTransformInfo getKmlInfo(TileInfo tileInfo, Vector3d position) {
        TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
        if (tileTransformInfo == null) {
            tileTransformInfo = TileTransformInfo.builder().position(position).build();
        } else {
            tileTransformInfo.setPosition(position);
        }
        return tileTransformInfo;
    }
}
