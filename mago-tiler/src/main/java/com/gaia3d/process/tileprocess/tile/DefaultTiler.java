package com.gaia3d.process.tileprocess.tile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.exception.TileProcessingException;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;
import com.gaia3d.process.tileprocess.tile.tileset.asset.*;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import com.gaia3d.util.GlobeUtils;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class DefaultTiler {

    protected double calcGeometricError(List<TileInfo> tileInfos) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        double minimumGeometricError = globalOptions.getMinGeometricError();
        double maximumGeometricError = globalOptions.getMaxGeometricError();
        double calculatedGeometricError = tileInfos.stream().mapToDouble(tileInfo -> {
            GaiaBoundingBox boundingBox = tileInfo.getBoundingBox();
            return boundingBox.getLongestDistance();
        }).max().orElse(0.0d);
        return Math.min(Math.max(minimumGeometricError, calculatedGeometricError), maximumGeometricError);
    }

    protected GaiaBoundingBox calcCartographicBoundingBox(List<TileInfo> tileInfos) {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        tileInfos.forEach(tileInfo -> {
            TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
            Vector3d position = tileTransformInfo.getPosition();
            GaiaBoundingBox localBoundingBox = tileInfo.getBoundingBox();
            // rotate
            localBoundingBox = localBoundingBox.convertLocalToLonlatBoundingBox(position);
            boundingBox.addBoundingBox(localBoundingBox);
        });
        return boundingBox;
    }

    protected GaiaBoundingBox calcCartesianBoundingBox(List<TileInfo> tileInfos) {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        tileInfos.forEach(tileInfo -> {
            TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
            Vector3d cartesian = tileTransformInfo.getPosition();
            //Vector3d cartesian = GlobeUtils.cartesianToGeographicWgs84(cartographic);

            Matrix4d transformMatrix = new Matrix4d().identity();
            transformMatrix.setTranslation(cartesian);
            GaiaBoundingBox localBoundingBox = tileInfo.getBoundingBox();
            localBoundingBox = localBoundingBox.multiplyMatrix4d(transformMatrix);
            boundingBox.addBoundingBox(localBoundingBox);
        });
        return boundingBox;
    }

    protected void rotateX90(Matrix4d matrix) {
        Matrix4d rotationMatrix = new Matrix4d();
        rotationMatrix.identity();
        rotationMatrix.rotateX(Math.toRadians(-90));
        matrix.mul(rotationMatrix, matrix);
    }

    protected Matrix4d getTransformMatrixFromCartesian(GaiaBoundingBox cartesianBoundingBox) {
        Vector3d cartesianCenter = cartesianBoundingBox.getCenter();
        Matrix4d transformMatrix = new Matrix4d().identity();
        transformMatrix.setTranslation(cartesianCenter);
        return transformMatrix;
    }

    protected Matrix4d getTransformMatrixFromCartographic(GaiaBoundingBox cartographicBoundingBox) {
        Vector3d center = cartographicBoundingBox.getCenter();
        double[] cartesian = GlobeUtils.geographicToCartesianWgs84(center.x, center.y, center.z);
        return GlobeUtils.transformMatrixAtCartesianPointWgs84(cartesian[0], cartesian[1], cartesian[2]);
    }

    protected AssetV1 createAsset() {
        AssetV1 asset = new AssetV1();
        return asset;
    }

    protected Node createRoot() {
        Node root = new Node();
        root.setParent(root);
        root.setNodeCode("R");
        root.setRefine(Node.RefineType.REPLACE);
        root.setChildren(new ArrayList<>());
        return root;
    }

    protected File writeTileset(Tileset tileset, String output) {
        Node rootNode = tileset.getRoot();
        if (rootNode == null) {
            log.error("[ERROR] Tileset root node is null");
            throw new TileProcessingException("Tileset root node is null");
        } else if (rootNode.getBoundingVolume() == null) {
            log.error("[ERROR] Tileset root node bounding volume is null");
            throw new TileProcessingException("Tileset root node bounding volume is null");
        } else if (rootNode.getGeometricError() == 0 && tileset.getGeometricError() == 0) {
            log.error("[ERROR] Tileset root node geometric error is 0");
            throw new TileProcessingException("Tileset root node geometric error is 0");
        } else if (rootNode.getChildren() == null || rootNode.getChildren().isEmpty()) {
            log.error("[ERROR] Tileset root node children is null or empty");
            throw new TileProcessingException("Tileset root node children is null or empty");
        }

        Path outputPath = new File(output).toPath();
        File tilesetFile = outputPath.resolve("tileset.json").toFile();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tilesetFile))) {
            String result = objectMapper.writeValueAsString(tileset);
            log.info("[Tile][Tileset] write 'tileset.json' file.");
            writer.write(result);
            //globalOptions.setTilesetSize(result.length());
        } catch (IOException e) {
            log.error("[ERROR] :", e);
            throw new TileProcessingException(e.getMessage());
        }

        return tilesetFile;
    }
}
