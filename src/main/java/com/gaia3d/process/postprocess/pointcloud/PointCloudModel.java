package com.gaia3d.process.postprocess.pointcloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.basic.structure.GaiaVertex;
import com.gaia3d.process.ProcessOptions;
import com.gaia3d.process.postprocess.TileModel;
import com.gaia3d.process.postprocess.batch.GaiaBatchTable;
import com.gaia3d.process.postprocess.instance.GaiaFeatureTable;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.joml.Vector3d;

import java.io.File;
import java.nio.file.Path;
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

        AtomicInteger vertexCount = new AtomicInteger();
        tileInfos.forEach((tileInfo) -> {
            GaiaPointCloud pointCloud = tileInfo.getPointCloud();
            List<GaiaVertex> gaiaVertex = pointCloud.getVertices();
            vertexCount.addAndGet(gaiaVertex.size());
        });

        float[] positions = new float[vertexCount.get() * 3];

        AtomicInteger i = new AtomicInteger();
        tileInfos.forEach((tileInfo) -> {
            GaiaPointCloud pointCloud = tileInfo.getPointCloud();
            List<GaiaVertex> gaiaVertex = pointCloud.getVertices();
            gaiaVertex.forEach((vertex) -> {
                Vector3d position = vertex.getPosition();
                positions[i.getAndIncrement()] = (float) position.x;
                positions[i.getAndIncrement()] = (float) position.y;
                positions[i.getAndIncrement()] = (float) position.z;
            });
        });

        PointCloudBinary pointCloudBinary = new PointCloudBinary();
        pointCloudBinary.setPositions(positions);
        byte[] featureTableBytes = pointCloudBinary.getBytes();

        GaiaFeatureTable featureTable = new GaiaFeatureTable();
        featureTable.setPointsLength(vertexCount.get());

        GaiaBatchTable batchTable = new GaiaBatchTable();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            String featureTableText = objectMapper.writeValueAsString(featureTable);
            featureTableJson = featureTableText;
            String batchTableText = objectMapper.writeValueAsString(batchTable);
            batchTableJson = batchTableText;
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

        PointCloudBinaryWriter writer = new PointCloudBinaryWriter(featureTableJson, batchTableJson, featureTableBytes, new byte[0]);
        writer.write(outputRoot, contentInfo.getNodeCode());

        return contentInfo;
    }
}
