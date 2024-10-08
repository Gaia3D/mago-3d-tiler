package com.gaia3d.process.postprocess.pointcloud;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.basic.structure.GaiaVertex;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.postprocess.TileModel;
import com.gaia3d.process.postprocess.batch.GaiaBatchTable;
import com.gaia3d.process.postprocess.instance.GaiaFeatureTable;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.GlobeUtils;
import com.gaia3d.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class PointCloudModel implements TileModel {
    @Override
    public ContentInfo run(ContentInfo contentInfo) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        String featureTableJson;
        String batchTableJson;

        File outputFile = new File(globalOptions.getOutputPath());
        Path outputRoot = outputFile.toPath().resolve("data");
        File outputRootFile = outputRoot.toFile();
        if (!outputRootFile.mkdir() && !outputRootFile.exists()) {
            log.error("Failed to create output directory : {}", outputRoot);
        }
        List<TileInfo> tileInfos = contentInfo.getTileInfos();

        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        AtomicInteger vertexCount = new AtomicInteger();
        tileInfos.forEach((tileInfo) -> {
            GaiaPointCloud pointCloud = tileInfo.getPointCloud();
            List<GaiaVertex> gaiaVertex = pointCloud.getVertices();
            vertexCount.addAndGet(gaiaVertex.size());
            boundingBox.addBoundingBox(pointCloud.getGaiaBoundingBox());
        });

        int vertexLength = vertexCount.get();

        float[] positions = new float[vertexLength * 3];
        Vector3d center = boundingBox.getCenter();
        Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
        Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerWorldCoordinate);
        Matrix4d transformMatrixInv = new Matrix4d(transformMatrix).invert();

        byte[] colors = new byte[vertexLength * 3];
        float[] batchIds = new float[vertexLength];

        AtomicInteger mainIndex = new AtomicInteger();
        AtomicInteger positionIndex = new AtomicInteger();
        AtomicInteger colorIndex= new AtomicInteger();
        AtomicInteger batchIdIndex= new AtomicInteger();
        tileInfos.forEach((tileInfo) -> {
            GaiaPointCloud pointCloud = tileInfo.getPointCloud();
            List<GaiaVertex> gaiaVertex = pointCloud.getVertices();
            gaiaVertex.forEach((vertex) -> {
                int index = mainIndex.getAndIncrement();
                if (index >= vertexLength) {
                    return;
                }
                Vector3d position = vertex.getPosition();
                Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                Vector3d localPosition = positionWorldCoordinate.mulPosition(transformMatrixInv, new Vector3d());

                float batchId = vertex.getBatchId();

                positions[positionIndex.getAndIncrement()] = (float) localPosition.x;
                positions[positionIndex.getAndIncrement()] = (float) -localPosition.z;
                positions[positionIndex.getAndIncrement()] = (float) localPosition.y;

                byte[] color = vertex.getColor();
                colors[colorIndex.getAndIncrement()] = color[0];
                colors[colorIndex.getAndIncrement()] = color[1];
                colors[colorIndex.getAndIncrement()] = color[2];

                batchIds[batchIdIndex.getAndIncrement()] = batchId;
            });
        });

        PointCloudBinary pointCloudBinary = new PointCloudBinary();
        pointCloudBinary.setPositions(positions);
        pointCloudBinary.setColors(colors);
        pointCloudBinary.setBatchIds(batchIds);

        byte[] positionBytes = pointCloudBinary.getPositionBytes();
        byte[] colorBytes = pointCloudBinary.getColorBytes();
        byte[] featureTableBytes = new byte[positionBytes.length + colorBytes.length /*+ batchIdBytes.length*/];
        System.arraycopy(positionBytes, 0, featureTableBytes, 0, positionBytes.length);
        System.arraycopy(colorBytes, 0, featureTableBytes, positionBytes.length, colorBytes.length);

        byte[] batchIdBytes = pointCloudBinary.getBatchIdBytes();
        byte[] batchTableBytes = new byte[batchIdBytes.length];
        System.arraycopy(batchIdBytes, 0, batchTableBytes, 0, batchIdBytes.length);

        GaiaFeatureTable featureTable = new GaiaFeatureTable();
        featureTable.setPointsLength(vertexLength);
        featureTable.setPosition(new Position(0));
        featureTable.setColor(new Color(positionBytes.length));
        featureTable.setBatchLength(1); // TODO is it needed?

        BatchId batchIdObject = new BatchId(0, "FLOAT");
        featureTable.setBatchId(batchIdObject);

        GaiaBatchTable batchTable = new GaiaBatchTable();
        List<String> batchTableIds = batchTable.getBatchId();
        batchTableIds.add("0");

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.getFactory().configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        try {
            featureTableJson = StringUtils.doPadding8Bytes(objectMapper.writeValueAsString(featureTable));
            batchTableJson = StringUtils.doPadding8Bytes(objectMapper.writeValueAsString(batchTable));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

        PointCloudBinaryWriter writer = new PointCloudBinaryWriter(featureTableJson, batchTableJson, featureTableBytes, batchTableBytes);
        writer.write(outputRoot, contentInfo.getNodeCode());

        return contentInfo;
    }
}
