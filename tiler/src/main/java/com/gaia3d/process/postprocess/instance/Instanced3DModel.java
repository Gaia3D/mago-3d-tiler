package com.gaia3d.process.postprocess.instance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.jgltf.GltfWriter;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.process.postprocess.TileModel;
import com.gaia3d.process.postprocess.batch.GaiaBatchTable;
import com.gaia3d.process.postprocess.pointcloud.Color;
import com.gaia3d.process.postprocess.pointcloud.Position;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.DecimalUtils;
import com.gaia3d.util.GlobeUtils;
import com.gaia3d.util.io.LittleEndianDataOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Instanced3DModel implements TileModel {
    private static final String MAGIC = "i3dm";
    private static final int VERSION = 1;
    private final GltfWriter gltfWriter;

    public Instanced3DModel() {
        this.gltfWriter = new GltfWriter();
        int featureTableJSONByteLength;
        int batchTableJSONByteLength;
        String featureTableJson;
        String batchTableJson;
    }

    @Override
    public ContentInfo run(ContentInfo contentInfo) {
        int featureTableJSONByteLength = 0;
        int batchTableJSONByteLength = 0;
        String featureTableJson = "";
        String batchTableJson = "";
        int gltfFormat = 0;

        String nodeCode = contentInfo.getNodeCode();

        List<TileInfo> tileInfos = contentInfo.getTileInfos();
        int instanceLength = tileInfos.size();

        float[] positions = new float[instanceLength * 3];
        float[] normalUps = new float[instanceLength * 3];
        float[] normalRights = new float[instanceLength * 3];

        Vector3d center = contentInfo.getBoundingBox().getCenter();
        Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
        Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerWorldCoordinate);
        Matrix4d transformMatrixInv = new Matrix4d(transformMatrix).invert();

        AtomicInteger positionIndex = new AtomicInteger();
        AtomicInteger normalUpIndex = new AtomicInteger();
        AtomicInteger normalRightIndex = new AtomicInteger();
        for (TileInfo tileInfo : tileInfos) {
            Vector3d normalUp = new Vector3d(0, 0, 1);
            Vector3d normalRight = new Vector3d(1, 0, 0);

            // GPS Coordinates
            KmlInfo kmlInfo = tileInfo.getKmlInfo();
            Vector3d position = kmlInfo.getPosition();
            Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
            //Vector3d localPosition = positionWorldCoordinate.sub(centerWorldCoordinate);


            //Vecter3d test = new Vecter3d(0,0,0);
            Vector3d localPosition = positionWorldCoordinate.mulPosition(transformMatrixInv, new Vector3d());

            //Vector3d localPosition = new Vector3d(0, 0, 0);

            positions[positionIndex.getAndIncrement()] = (float) localPosition.x;
            positions[positionIndex.getAndIncrement()] = (float) -localPosition.z;
            positions[positionIndex.getAndIncrement()] = (float) localPosition.y;

            normalUps[normalUpIndex.getAndIncrement()] = (float) normalUp.x;
            normalUps[normalUpIndex.getAndIncrement()] = (float) normalUp.y;
            normalUps[normalUpIndex.getAndIncrement()] = (float) normalUp.z;

            normalRights[normalRightIndex.getAndIncrement()] = (float) normalRight.x;
            normalRights[normalRightIndex.getAndIncrement()] = (float) normalRight.y;
            normalRights[normalRightIndex.getAndIncrement()] = (float) normalRight.z;
        }

        Instanced3DModelBinary instanced3DModelBinary = new Instanced3DModelBinary();
        instanced3DModelBinary.setPositions(positions);
        instanced3DModelBinary.setNormalUps(normalUps);
        instanced3DModelBinary.setNormalRights(normalRights);

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        File outputFile = new File(globalOptions.getOutputPath());
        Path outputRoot = outputFile.toPath().resolve("data");
        if (!outputRoot.toFile().exists() && outputRoot.toFile().mkdir()) {
            log.info("[Create][data] Created output data directory:", outputRoot);
        }

        byte[] positionBytes = instanced3DModelBinary.getPositionBytes();
        byte[] normalUpBytes = instanced3DModelBinary.getNormalUpBytes();
        byte[] normalRightBytes = instanced3DModelBinary.getNormalRightBytes();

        byte[] featureTableBytes = new byte[positionBytes.length + normalUpBytes.length + normalRightBytes.length];
        System.arraycopy(positionBytes, 0, featureTableBytes, 0, positionBytes.length);
        System.arraycopy(normalUpBytes, 0, featureTableBytes, positionBytes.length, normalUpBytes.length);
        System.arraycopy(normalRightBytes, 0, featureTableBytes, positionBytes.length + normalUpBytes.length, normalRightBytes.length);

        //byte[] batchTableBytes = new byte[0];
        //byte[] batchIdBytes = pointCloudBinary.getBatchIdBytes();
        //byte[] batchTableBytes = new byte[batchIdBytes.length];
        //System.arraycopy(batchIdBytes, 0, batchTableBytes, 0, batchIdBytes.length);

        GaiaFeatureTable featureTable = new GaiaFeatureTable();
        featureTable.setInstancesLength(instanceLength);
        featureTable.setPosition(new Position(0));
        featureTable.setEastNorthUp(false);
        featureTable.setNormalUp(new Normal(positionBytes.length));
        featureTable.setNormalRight(new Normal(positionBytes.length + normalUpBytes.length));

        GaiaBatchTable batchTable = new GaiaBatchTable();
        /*int batchLength = tileInfos.size();
        for (int i = 0; i < batchLength; i++) {
            batchTable.getBatchId().add(String.valueOf(i));
        }*/

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            StringBuilder featureTableText = new StringBuilder(objectMapper.writeValueAsString(featureTable));
            int featureTableJsonOffset = featureTableText.length() % 8;
            featureTableText.append(" ".repeat(Math.max(0, featureTableJsonOffset)));
            featureTableJson = featureTableText.toString();

            StringBuilder batchTableText = new StringBuilder(objectMapper.writeValueAsString(batchTable));
            int batchTableJsonOffset = batchTableText.length() % 8;
            batchTableText.append(" ".repeat(Math.max(0, batchTableJsonOffset)));
            batchTableJson = batchTableText.toString();
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
        featureTableJSONByteLength = featureTableJson.length();
        int featureTableBinaryByteLength = featureTableBytes.length;

        batchTableJSONByteLength = batchTableJson.length();
        int batchTableBinaryByteLength = 0;

        String gltfUrl = "../snowman-1.glb";
        int byteLength = 32 + featureTableJSONByteLength + featureTableBinaryByteLength + batchTableJSONByteLength + batchTableBinaryByteLength + gltfUrl.length();


        File b3dmOutputFile = outputRoot.resolve(nodeCode + "." + MAGIC).toFile();
        try (LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(new BufferedOutputStream(new FileOutputStream(b3dmOutputFile)))) {
            // 32-byte header (first 20 bytes)
            stream.writePureText(MAGIC);
            stream.writeInt(VERSION);
            stream.writeInt(byteLength);
            stream.writeInt(featureTableJSONByteLength);
            stream.writeInt(featureTableBinaryByteLength);

            // 32-byte header (next 12 bytes)
            stream.writeInt(batchTableJSONByteLength);
            stream.writeInt(batchTableBinaryByteLength);
            stream.writeInt(gltfFormat);

            // body
            stream.writePureText(featureTableJson);
            stream.write(featureTableBytes);
            stream.writePureText(batchTableJson);
            //stream.write(new byte[0]);
            stream.writePureText(gltfUrl);// padding
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return contentInfo;
    }
}
