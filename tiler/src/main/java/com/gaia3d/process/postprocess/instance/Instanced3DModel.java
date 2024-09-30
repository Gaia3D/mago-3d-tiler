package com.gaia3d.process.postprocess.instance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.structure.GaiaAttribute;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.jgltf.GltfWriter;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.process.postprocess.TileModel;
import com.gaia3d.process.postprocess.batch.GaiaBatchTableMap;
import com.gaia3d.process.postprocess.batch.GaiaBatcher;
import com.gaia3d.process.postprocess.pointcloud.Position;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.GeometryUtils;
import com.gaia3d.util.GlobeUtils;
import com.gaia3d.util.StringUtils;
import com.gaia3d.util.io.LittleEndianDataOutputStream;
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
public class Instanced3DModel implements TileModel {
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
        Matrix4d transformMatrixInv = new Matrix4d(transformMatrix).invert();

        AtomicInteger positionIndex = new AtomicInteger();
        AtomicInteger normalUpIndex = new AtomicInteger();
        AtomicInteger normalRightIndex = new AtomicInteger();
        AtomicInteger scaleIndex = new AtomicInteger();
        AtomicInteger batchIdIndex = new AtomicInteger();
        for (TileInfo tileInfo : tileInfos) {
            //y-up
            Vector3d normalUp = new Vector3d(0, 0, 1);
            Vector3d normalRight = new Vector3d(1, 0, 0);

            // GPS Coordinates
            KmlInfo kmlInfo = tileInfo.getKmlInfo();
            Vector3d position = kmlInfo.getPosition();
            Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
            Vector3d localPosition = positionWorldCoordinate.mulPosition(transformMatrixInv, new Vector3d());

            // local position(Z-UP), gltf position(Y-UP)
            Vector3d localPositionYUp = new Vector3d(localPosition.x, -localPosition.z, localPosition.y);

            // rotate
            double headingValue = Math.toRadians(kmlInfo.getHeading());
            Matrix3d rotationMatrix = new Matrix3d();
            rotationMatrix.rotateY(headingValue);
            normalUp = rotationMatrix.transform(normalUp);
            normalRight = rotationMatrix.transform(normalRight);

            // scale
            double scale = kmlInfo.getScaleZ();

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
            log.info("[Create][data] Created output data directory:", outputRoot);
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
        featureTable.setInstancesLength(instanceLength);
        featureTable.setEastNorthUp(false);
        featureTable.setPosition(new Position(0));
        featureTable.setNormalUp(new Normal(positionBytes.length));
        featureTable.setNormalRight(new Normal(positionBytes.length + normalUpBytes.length));
        featureTable.setScale(new Scale(positionBytes.length + normalUpBytes.length + normalRightBytes.length));

        GaiaBatchTableMap<String, List<String>> batchTableMap = new GaiaBatchTableMap<>();
        AtomicInteger finalBatchIdIndex = new AtomicInteger();
        tileInfos.forEach((tileInfo) -> {
            GaiaAttribute attribute = tileInfo.getScene().getAttribute();
            Map<String, String> attributes = attribute.getAttributes();
            GaiaSet set = tileInfo.getSet();

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

            attributes.forEach((key, value) -> {
                String utf8Value = StringUtils.convertUTF8(value);
                batchTableMap.computeIfAbsent(key, k -> new ArrayList<>());
                batchTableMap.get(key).add(utf8Value);
            });
        });


        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            featureTableJson = StringUtils.doPadding8Bytes(objectMapper.writeValueAsString(featureTable));
            batchTableJson = StringUtils.doPadding8Bytes(objectMapper.writeValueAsString(batchTableMap));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
        featureTableJSONByteLength = featureTableJson.length();
        int featureTableBinaryByteLength = featureTableBytes.length;

        batchTableJSONByteLength = batchTableJson.length();
        int batchTableBinaryByteLength = 0;

        String lod = contentInfo.getLod().toString();
        String gltfUrl = "instance-" + lod + ".glb";
        int byteLength = 32 + featureTableJSONByteLength + featureTableBinaryByteLength + batchTableJSONByteLength + batchTableBinaryByteLength + gltfUrl.length();

        File gltfOutputFile = outputRoot.resolve(gltfUrl).toFile();
        if (!gltfOutputFile.exists()) {
            createInstance(gltfOutputFile, contentInfo, tileInfos.get(0));
        }

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
            log.error(e.getMessage());
        }
        return contentInfo;
    }

    private synchronized void createInstance(File file, ContentInfo contentInfo, TileInfo tileInfo) {
        boolean isVoxelLod = GlobalOptions.getInstance().isVoxelLod();

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

                if (isVoxelLod) {
                    int lod = contentInfo.getLod().getLevel();
                    if (lod > 0) {
                        float octreeMinSize = 4.0f;
                        if (lod == 1) {
                            octreeMinSize = 0.6f;
                        } else if (lod == 2) {
                            octreeMinSize = 2.0f;
                        }

                        GaiaScene simpleScene = GeometryUtils.getGaiaSceneLego(resultGaiaScene, octreeMinSize);
                        resultGaiaScene = simpleScene;
                    }
                }

                boolean isRotateUpAxis = GlobalOptions.getInstance().isSwapUpAxis();
                if(isRotateUpAxis)
                {
                    Matrix4d transformMatrix = resultGaiaScene.getNodes().get(0).getTransformMatrix();
                    transformMatrix.rotateX(Math.toRadians(-90));
                }
                gltfWriter.writeGlb(resultGaiaScene, file);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
