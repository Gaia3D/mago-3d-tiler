package com.gaia3d.process.postprocess.instance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.jgltf.GltfWriter;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.io.LittleEndianDataOutputStream;
import com.gaia3d.process.postprocess.ContentModel;
import com.gaia3d.process.postprocess.batch.GaiaBatchTableMap;
import com.gaia3d.process.postprocess.batch.GaiaBatcher;
import com.gaia3d.process.postprocess.pointcloud.ByteAddress;
import com.gaia3d.process.preprocess.sub.TransformBaker;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.GeometryUtils;
import com.gaia3d.util.GlobeUtils;
import com.gaia3d.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix3d;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Instanced3DModel implements ContentModel {
    private static final String MAGIC = "i3dm";
    private static final int VERSION = 1;
    private final GltfWriter gltfWriter;

    public Instanced3DModel() {
        this.gltfWriter = new GltfWriter();
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
        float[] scales = new float[instanceLength];
        short[] batchId = new short[instanceLength];

        Vector3d center = contentInfo.getBoundingBox().getCenter();
        Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
        Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerWorldCoordinate);

        AtomicInteger positionIndex = new AtomicInteger();
        AtomicInteger normalUpIndex = new AtomicInteger();
        AtomicInteger normalRightIndex = new AtomicInteger();
        AtomicInteger scaleIndex = new AtomicInteger();
        AtomicInteger batchIdIndex = new AtomicInteger();
        for (TileInfo tileInfo : tileInfos) {
            //y-up
            Vector3d normalUp = new Vector3d(0, 1, 0);
            Vector3d normalRight = new Vector3d(1, 0, 0);

            // GPS Coordinates
            TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
            Vector3d position = tileTransformInfo.getPosition();
            Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
            Vector3d localPosition = positionWorldCoordinate.sub(centerWorldCoordinate, new Vector3d());

            Vector3d localPositionYUp = new Vector3d(localPosition.x, localPosition.y, localPosition.z);
            Matrix3d worldRotationMatrix3d = transformMatrix.get3x3(new Matrix3d());

            // rotate
            double headingValue = Math.toRadians(tileTransformInfo.getHeading());
            Matrix3d rotationMatrix = new Matrix3d();
            rotationMatrix.rotateZ(-headingValue);
            worldRotationMatrix3d.mul(rotationMatrix, worldRotationMatrix3d);

            normalUp = worldRotationMatrix3d.transform(normalUp);
            normalRight = worldRotationMatrix3d.transform(normalRight);

            // scale
            double scale = tileTransformInfo.getScaleZ();

            positions[positionIndex.getAndIncrement()] = (float) localPositionYUp.x;
            positions[positionIndex.getAndIncrement()] = (float) localPositionYUp.y;
            positions[positionIndex.getAndIncrement()] = (float) localPositionYUp.z;

            normalUps[normalUpIndex.getAndIncrement()] = (float) normalUp.x;
            normalUps[normalUpIndex.getAndIncrement()] = (float) normalUp.y;
            normalUps[normalUpIndex.getAndIncrement()] = (float) normalUp.z;

            normalRights[normalRightIndex.getAndIncrement()] = (float) normalRight.x;
            normalRights[normalRightIndex.getAndIncrement()] = (float) normalRight.y;
            normalRights[normalRightIndex.getAndIncrement()] = (float) normalRight.z;

            scales[scaleIndex.getAndIncrement()] = (float) scale;

            batchId[batchIdIndex.get()] = (short) batchIdIndex.getAndIncrement();
        }

        Instanced3DModelBinary instanced3DModelBinary = new Instanced3DModelBinary();
        instanced3DModelBinary.setPositions(positions);
        instanced3DModelBinary.setNormalUps(normalUps);
        instanced3DModelBinary.setNormalRights(normalRights);
        instanced3DModelBinary.setScales(scales);

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        File outputFile = new File(globalOptions.getOutputPath());
        Path outputRoot = outputFile.toPath().resolve("data");
        if (!outputRoot.toFile().exists() && outputRoot.toFile().mkdir()) {
            log.debug("[Create][data] Created output data directory, {}", outputRoot);
        }

        byte[] positionBytes = instanced3DModelBinary.getPositionBytes();
        byte[] normalUpBytes = instanced3DModelBinary.getNormalUpBytes();
        byte[] normalRightBytes = instanced3DModelBinary.getNormalRightBytes();
        byte[] scaleBytes = instanced3DModelBinary.getScaleBytes();

        byte[] featureTableBytes = new byte[positionBytes.length + normalUpBytes.length + normalRightBytes.length + scaleBytes.length];
        System.arraycopy(positionBytes, 0, featureTableBytes, 0, positionBytes.length);
        System.arraycopy(normalUpBytes, 0, featureTableBytes, positionBytes.length, normalUpBytes.length);
        System.arraycopy(normalRightBytes, 0, featureTableBytes, positionBytes.length + normalUpBytes.length, normalRightBytes.length);
        System.arraycopy(scaleBytes, 0, featureTableBytes, positionBytes.length + normalUpBytes.length + normalRightBytes.length, scaleBytes.length);

        GaiaFeatureTable featureTable = new GaiaFeatureTable();
        if (!globalOptions.isClassicTransformMatrix()) {
            double[] rtcCenter = new double[3];
            rtcCenter[0] = transformMatrix.m30();
            rtcCenter[1] = transformMatrix.m31();
            rtcCenter[2] = transformMatrix.m32();
            featureTable.setRtcCenter(rtcCenter);
        }

        featureTable.setInstancesLength(instanceLength);
        featureTable.setEastNorthUp(false);
        featureTable.setPosition(new ByteAddress(0));
        featureTable.setNormalUp(new ByteAddress(positionBytes.length));
        featureTable.setNormalRight(new ByteAddress(positionBytes.length + normalUpBytes.length));
        featureTable.setScale(new ByteAddress(positionBytes.length + normalUpBytes.length + normalRightBytes.length));

        int lod = contentInfo.getLod().getLevel();

        GaiaBatchTableMap<String, List<String>> batchTableMap = new GaiaBatchTableMap<>();
        AtomicInteger finalBatchIdIndex = new AtomicInteger();
        tileInfos.forEach((tileInfo) -> {
            GaiaAttribute attribute = tileInfo.getScene().getAttribute();
            Map<String, String> attributes = tileInfo.getTileTransformInfo().getProperties();

            String UUID = attribute.getIdentifier().toString();
            String FileName = attribute.getFileName();
            String NodeName = attribute.getNodeName();

            UUID = StringUtils.convertUTF8(UUID);
            FileName = StringUtils.convertUTF8(FileName);
            NodeName = StringUtils.convertUTF8(NodeName);

            batchTableMap.computeIfAbsent("UUID", k -> new ArrayList<>());
            batchTableMap.get("UUID").add(UUID);

            batchTableMap.computeIfAbsent("FileName", k -> new ArrayList<>());
            batchTableMap.get("FileName").add(FileName);

            batchTableMap.computeIfAbsent("NodeName", k -> new ArrayList<>());
            batchTableMap.get("NodeName").add(NodeName);

            batchTableMap.computeIfAbsent("BatchId", k -> new ArrayList<>());
            batchTableMap.get("BatchId").add(String.valueOf(batchId[finalBatchIdIndex.getAndIncrement()]));

            batchTableMap.computeIfAbsent("LOD", k -> new ArrayList<>());
            batchTableMap.get("LOD").add(String.valueOf(lod));

            if (attributes != null) {
                attributes.forEach((key, value) -> {
                    String utf8Value = StringUtils.convertUTF8(value);
                    batchTableMap.computeIfAbsent(key, k -> new ArrayList<>());
                    batchTableMap.get(key).add(utf8Value);
                });
            }
        });

        ObjectMapper objectMapper = new ObjectMapper();
        if (!globalOptions.isDebug()) {
            objectMapper.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
        }
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            featureTableJson = StringUtils.doPadding8Bytes(objectMapper.writeValueAsString(featureTable));
            batchTableJson = StringUtils.doPadding8Bytes(objectMapper.writeValueAsString(batchTableMap));
        } catch (JsonProcessingException e) {
            log.error("[ERROR] :", e);
            throw new RuntimeException(e);
        }
        featureTableJSONByteLength = featureTableJson.length();
        int featureTableBinaryByteLength = featureTableBytes.length;

        batchTableJSONByteLength = batchTableJson.length();
        int batchTableBinaryByteLength = 0;

        String gltfUrl = "instance-" + lod + ".glb";
        int byteLength = 32 + featureTableJSONByteLength + featureTableBinaryByteLength + batchTableJSONByteLength + batchTableBinaryByteLength + gltfUrl.length();

        File gltfOutputFile = outputRoot.resolve(gltfUrl).toFile();
        if (!gltfOutputFile.exists()) {
            createInstance(gltfOutputFile, contentInfo, tileInfos.get(0));
        }

        File i3dmOutputFile = outputRoot.resolve(nodeCode + "." + MAGIC).toFile();
        try (LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(new BufferedOutputStream(new FileOutputStream(i3dmOutputFile)))) {
            // 32-byte header (first 20 bytes)
            stream.writePureText(MAGIC);
            stream.writeInt(VERSION);
            stream.writeInt(byteLength);
            stream.writeInt(featureTableJSONByteLength);
            stream.writeInt(featureTableBinaryByteLength);

            // 32-byte header (next 12 bytes)
            stream.writeInt(batchTableJSONByteLength);
            //stream.writeInt(0);
            stream.writeInt(batchTableBinaryByteLength);
            stream.writeInt(gltfFormat);

            // body
            stream.writePureText(featureTableJson);
            stream.write(featureTableBytes);
            stream.writePureText(batchTableJson);
            //stream.write(new byte[0]);
            stream.writePureText(gltfUrl);// padding
        } catch (Exception e) {
            log.error("[ERROR] :", e);
        }
        return contentInfo;
    }

    private synchronized void createInstance(File file, ContentInfo contentInfo, TileInfo tileInfo) {
        //boolean isVoxelLod = GlobalOptions.getInstance().isVoxelLod();

        try {
            if (!file.exists()) {
                log.info("[Create][Instance] Create instance file : {}", file.getName());
                GaiaScene firstGaiaScene = tileInfo.getScene();
                firstGaiaScene = firstGaiaScene.clone();

                GaiaSet set = GaiaSet.fromGaiaScene(firstGaiaScene);
                tileInfo.setSet(set);

                List<TileInfo> batchTileInfos = new ArrayList<>();
                batchTileInfos.add(tileInfo);

                GaiaBatcher gaiaBatcher = new GaiaBatcher();
                GaiaSet gaiaSet = gaiaBatcher.runBatching(batchTileInfos, contentInfo.getNodeCode(), contentInfo.getLod());
                GaiaScene resultGaiaScene = new GaiaScene(gaiaSet);

                GaiaBoundingBox boundingBox = resultGaiaScene.updateBoundingBox();
                float minSize = (float) boundingBox.getMinSize();

                /*if (isVoxelLod) {
                    int lod = contentInfo.getLod().getLevel();
                    if (lod > 0) {
                        float octreeMinSize = minSize;
                        if (lod == 1) {
                            octreeMinSize = minSize / 8.0f;
                        } else if (lod == 2) {
                            octreeMinSize = minSize / 4.0f;
                        } else if (lod == 3) {
                            octreeMinSize = minSize / 2.0f;
                        } else if (lod == 4) {
                            octreeMinSize = minSize;
                        }
                        resultGaiaScene = GeometryUtils.getGaiaSceneLego(resultGaiaScene, octreeMinSize);
                    }
                }*/

                Matrix4d transformMatrix = resultGaiaScene.getNodes().get(0).getTransformMatrix();
                transformMatrix.rotateX(Math.toRadians(-90));

                gltfWriter.writeGlb(resultGaiaScene, file);
            }
        } catch (Exception e) {
            log.error("[ERROR] :", e);
            throw new RuntimeException(e);
        }
    }
}
