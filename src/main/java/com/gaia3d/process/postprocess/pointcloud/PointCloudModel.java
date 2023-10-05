package com.gaia3d.process.postprocess.pointcloud;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.basic.structure.GaiaVertex;
import com.gaia3d.process.ProcessOptions;
import com.gaia3d.process.postprocess.TileModel;
import com.gaia3d.process.postprocess.batch.GaiaBatchTable;
import com.gaia3d.process.postprocess.instance.GaiaFeatureTable;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.GlobeUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@AllArgsConstructor
public class PointCloudModel implements TileModel {
    private static final String MAGIC = "pnts";
    private static final int VERSION = 1;
    private final CommandLine command;

    @Override
    public ContentInfo run(ContentInfo contentInfo) {
        int featureTableJSONByteLength;
        int batchTableJSONByteLength;
        String featureTableJson;
        String batchTableJson;

        File outputFile = new File(command.getOptionValue(ProcessOptions.OUTPUT.getArgName()));
        Path outputRoot = outputFile.toPath().resolve("data");
        outputRoot.toFile().mkdir();

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
        //vertexLength = 100000;

        float[] positions = new float[vertexLength * 3];
        Vector3d center = boundingBox.getCenter();
        Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
        Matrix4d transformMatrix = GlobeUtils.normalAtCartesianPointWgs84(centerWorldCoordinate);
        Matrix4d transfromMatrixInv = new Matrix4d(transformMatrix).invert();

        byte[] colors = new byte[vertexLength * 3];
        float[] batchIds = new float[vertexLength];
        //Arrays.fill(batchIds, (float) 0);

        AtomicInteger mainIndex = new AtomicInteger();
        AtomicInteger positionIndex = new AtomicInteger();
        AtomicInteger colorIndex= new AtomicInteger();
        AtomicInteger batchIdIndex= new AtomicInteger();
        int finalVertexLength = vertexLength;
        tileInfos.forEach((tileInfo) -> {
            GaiaPointCloud pointCloud = tileInfo.getPointCloud();
            List<GaiaVertex> gaiaVertex = pointCloud.getVertices();
            gaiaVertex.forEach((vertex) -> {
                int index = mainIndex.getAndIncrement();
                if (index >= finalVertexLength) {
                    return;
                }
                Vector3d position = vertex.getPosition();
                Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
                Vector3d localPosition = positionWorldCoordinate.mulPosition(transfromMatrixInv, new Vector3d());

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

        //pointCloudBinary.setColor(colors);

        byte[] positionBytes = pointCloudBinary.getPositionBytes();
        byte[] colorBytes = pointCloudBinary.getColorBytes();
        byte[] featureTableBytes = new byte[positionBytes.length + colorBytes.length /*+ batchIdBytes.length*/];
        System.arraycopy(positionBytes, 0, featureTableBytes, 0, positionBytes.length);
        System.arraycopy(colorBytes, 0, featureTableBytes, positionBytes.length, colorBytes.length);
        //System.arraycopy(batchIdBytes, 0, featureTableBytes, positionBytes.length + colorBytes.length, batchIdBytes.length);

        byte[] batchIdBytes = pointCloudBinary.getBatchIdBytes();
        byte[] batchTableBytes = new byte[batchIdBytes.length];
        System.arraycopy(batchIdBytes, 0, batchTableBytes, 0, batchIdBytes.length);

        GaiaFeatureTable featureTable = new GaiaFeatureTable();
        featureTable.setPointsLength(vertexLength);
        featureTable.setPosition(new Position(0));
        featureTable.setColor(new Color(positionBytes.length));
        featureTable.setBatchLength(1);

        BatchId batchIdObject = new BatchId(0, "FLOAT");
        featureTable.setBatchId(batchIdObject);

        GaiaBatchTable batchTable = new GaiaBatchTable();
        List<String> batchTableIds = batchTable.getBatchId();
        batchTableIds.add("0");

        ObjectMapper objectMapper = new ObjectMapper();
        //objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        //objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

        try {
            String featureTableText = objectMapper.writeValueAsString(featureTable);
            int featureTableJsonOffset = featureTableText.length() % 8;
            for (int k = 0; k < featureTableJsonOffset; k++) {
                featureTableText += " ";
            }
            featureTableJson = featureTableText;

            String batchTableText = objectMapper.writeValueAsString(batchTable);
            int batchTableJsonOffset = batchTableText.length() % 8;
            for (int k = 0; k < batchTableJsonOffset; k++) {
                batchTableText += " ";
            }
            batchTableJson = batchTableText;
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

        PointCloudBinaryWriter writer = new PointCloudBinaryWriter(featureTableJson, batchTableJson, featureTableBytes, batchTableBytes);
        writer.write(outputRoot, contentInfo.getNodeCode());

        return contentInfo;
    }
}
