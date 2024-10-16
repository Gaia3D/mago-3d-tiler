package com.gaia3d.process.preprocess;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.*;
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

public class GaiaTranslatorExact implements PreProcess {
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
        coverages.forEach((coverage) -> {
            DirectPosition2D memSave_posWorld = new DirectPosition2D(DefaultGeographicCRS.WGS84, centerGeoCoord.x, centerGeoCoord.y);
            double[] memSave_alt = new double[1];
            memSave_alt[0] = 0;
            try {
                coverage.evaluate((DirectPosition) memSave_posWorld, memSave_alt);
            } catch (Exception e) {
                log.error("Error : {}", e.getMessage());
                log.warn("Failed to evaluate terrain height", e);
            }
            //log.info("memSave_alt[0] : {}", memSave_alt[0]);
            centerGeoCoord.z = memSave_alt[0];
        });

        KmlInfo kmlInfo = getKmlInfo(tileInfo, centerGeoCoord);
        rootNode.setTransformMatrix(transform);
        tileInfo.setTransformMatrix(transform);

        GaiaBoundingBox boundingBox = gaiaScene.getBoundingBox(); // new
        gaiaScene.setGaiaBoundingBox(boundingBox); // new

        tileInfo.setBoundingBox(boundingBox);
        tileInfo.setKmlInfo(kmlInfo);
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
        // now set all node's transform matrix as identity, bcos in "transformNodeVertexPositionsToLocalCoords" function we used the nodes tMatrix.***
        for (GaiaNode rootNode : rootNodes) {
            this.setNodesTransformMatrixAsIdentity(rootNode);
        }
    }

    private void transformNodeVertexPositionsToLocalCoords(GaiaNode node, Matrix4d globalTMatrixInv, Matrix4d parentMatrix, GaiaBoundingBox resultBBoxLC) {
        // check for meshes.***
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        CoordinateReferenceSystem crs = globalOptions.getCrs();

        // check node's parent matrix.***
        Matrix4d transformMatrix = new Matrix4d(node.getTransformMatrix());
        if (parentMatrix != null) {
            parentMatrix.mul(transformMatrix, transformMatrix);
        }


        List <GaiaMesh> meshes = node.getMeshes();
        if (meshes != null) {
            for (GaiaMesh gaiaMesh : meshes) {
                List<GaiaPrimitive> primitives = gaiaMesh.getPrimitives();
                if (primitives != null && !primitives.isEmpty()) {
                    for (GaiaPrimitive primitive : primitives) {
                        List<GaiaVertex> vertices = primitive.getVertices();
                        if (vertices != null && !vertices.isEmpty()) {
                            for (GaiaVertex vertex : vertices) {
                                Vector3d pos = new Vector3d(vertex.getPosition());
                                transformMatrix.transformPosition(pos); // CRS coords.***

                                // calculate the geoCoords of the "pos".***
                                ProjCoordinate vertexSource = new ProjCoordinate(pos.x, pos.y, pos.z);
                                ProjCoordinate vertexWgs84 = GlobeUtils.transform(crs, vertexSource);

                                // calculate the posWC of the "vertexWgs84".***
                                double[] posWC = GlobeUtils.geographicToCartesianWgs84(vertexWgs84.x, vertexWgs84.y, vertexSource.z);
                                Vector3d posWCVector = new Vector3d(posWC[0], posWC[1], posWC[2]);
                                Vector3d posLC = globalTMatrixInv.transformPosition(posWCVector);

                                resultBBoxLC.addPoint(posLC);

                                // finally set the position of the vertex.***
                                vertex.setPosition(posLC);
                            }
                        }
                    }
                }
            }
        }

        // check for children.***
        if (node.getChildren() != null) {
            for (int i = 0; i < node.getChildren().size(); i++) {
                GaiaNode childNode = node.getChildren().get(i);
                this.transformNodeVertexPositionsToLocalCoords(childNode, globalTMatrixInv, parentMatrix, resultBBoxLC);
            }
        }
    }

    private void setNodesTransformMatrixAsIdentity(GaiaNode node) {
        // check node's parent matrix.***
        Matrix4d transformMatrix = new Matrix4d(node.getTransformMatrix());
        transformMatrix.identity();

        // check for children.***
        if (node.getChildren() != null) {
            for (int i = 0; i < node.getChildren().size(); i++) {
                GaiaNode childNode = node.getChildren().get(i);
                this.setNodesTransformMatrixAsIdentity(childNode);
            }
        }
    }

    private Vector3d getTranslation(GaiaScene gaiaScene) {
        GaiaBoundingBox boundingBox = gaiaScene.getBoundingBox();
        Vector3d center = boundingBox.getCenter();
        Vector3d translation = new Vector3d(center.x, center.y, 0.0d);
        translation.negate();
        return translation;
    }

    private Vector3d getPosition(FormatType formatType, GaiaScene gaiaScene) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        Vector3d position;
        if (formatType == FormatType.CITYGML || formatType == FormatType.INDOORGML || formatType == FormatType.SHP || formatType == FormatType.GEOJSON) {
            GaiaNode rootNode = gaiaScene.getNodes().get(0);
            Matrix4d transform = rootNode.getTransformMatrix();
            Vector3d center = new Vector3d(transform.get(3, 0), transform.get(3, 1), 0.0d);
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
