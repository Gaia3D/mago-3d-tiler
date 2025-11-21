package com.gaia3d.process.postprocess.instance;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.jgltf.tiles.InstancedModelGltfWriter;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.process.postprocess.ContentModel;
import com.gaia3d.process.postprocess.batch.GaiaBatchTableMap;
import com.gaia3d.process.postprocess.batch.GaiaBatcher;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.GlobeUtils;
import com.gaia3d.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix3d;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 3D Tiles 1.1 Instanced Model
 */
@Slf4j
public class Instanced3DModelV2 implements ContentModel {
    private static final String MAGIC = "glb";
    private final InstancedModelGltfWriter gltfWriter;

    public Instanced3DModelV2() {
        this.gltfWriter = new InstancedModelGltfWriter();
    }

    @Override
    public ContentInfo run(ContentInfo contentInfo) {
        String nodeCode = contentInfo.getNodeCode();

        List<TileInfo> tileInfos = contentInfo.getTileInfos();
        int instanceLength = tileInfos.size();

        List<Matrix4d> transformMatrices = new ArrayList<>(instanceLength);

        float[] positions = new float[instanceLength * 3];
        float[] rotations = new float[instanceLength * 4]; // Not used in this implementation, but can be used for rotation quaternions
        float[] scales = new float[instanceLength * 3]; // Assuming uniform scale for simplicity

        float[] batchId = new float[instanceLength];

        Vector3d center = contentInfo.getBoundingBox().getCenter();
        Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);

        Vector3d centerWorldCoordinateYUp = new Vector3d(centerWorldCoordinate.x, centerWorldCoordinate.z, -centerWorldCoordinate.y);

        Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerWorldCoordinateYUp);
        Matrix4d inverseTransformMatrix = transformMatrix.invert(new Matrix4d());

        AtomicInteger positionIndex = new AtomicInteger();
        AtomicInteger rotationIndex = new AtomicInteger();
        AtomicInteger normalUpIndex = new AtomicInteger();
        AtomicInteger normalRightIndex = new AtomicInteger();
        AtomicInteger scaleIndex = new AtomicInteger();
        AtomicInteger batchIdIndex = new AtomicInteger();
        for (TileInfo tileInfo : tileInfos) {
            // GPS Coordinates
            TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
            Vector3d position = tileTransformInfo.getPosition();
            Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
            Vector3d localPosition = positionWorldCoordinate.sub(centerWorldCoordinate, new Vector3d());

            Vector3d localPositionYUp = new Vector3d(localPosition.x, localPosition.y, localPosition.z);
            Matrix3d worldRotationMatrix3d = transformMatrix.get3x3(new Matrix3d());
            // scale
            double scaleX = tileTransformInfo.getScaleX();
            double scaleY = tileTransformInfo.getScaleY();
            double scaleZ = tileTransformInfo.getScaleZ();

            positions[positionIndex.getAndIncrement()] = (float) localPositionYUp.x;
            positions[positionIndex.getAndIncrement()] = (float) localPositionYUp.z;
            positions[positionIndex.getAndIncrement()] = (float) -localPositionYUp.y;

            scales[scaleIndex.getAndIncrement()] = (float) scaleX;
            scales[scaleIndex.getAndIncrement()] = (float) scaleY;
            scales[scaleIndex.getAndIncrement()] = (float) scaleZ;

            batchId[batchIdIndex.get()] = batchIdIndex.getAndIncrement();

            Matrix4d instanceTransformMatrix = new Matrix4d()
                    .translate(localPositionYUp)
                    //.rotateZ(-headingValue)
                    .scale(scaleX, scaleY, scaleZ);
            transformMatrices.add(instanceTransformMatrix);

            Matrix3d xRotationMatrix3d = new Matrix3d();
            xRotationMatrix3d.identity();
            xRotationMatrix3d.rotateX(Math.toRadians(90));
            worldRotationMatrix3d.mul(xRotationMatrix3d, xRotationMatrix3d);

            Quaterniond quaternion = xRotationMatrix3d.getNormalizedRotation(new Quaterniond());
            rotations[rotationIndex.getAndIncrement()] = (float) quaternion.x;
            rotations[rotationIndex.getAndIncrement()] = (float) quaternion.y;
            rotations[rotationIndex.getAndIncrement()] = (float) quaternion.z;
            rotations[rotationIndex.getAndIncrement()] = (float) quaternion.w;
        }

        Instanced3DModelBinary instanced3DModelBinary = new Instanced3DModelBinary();
        instanced3DModelBinary.setPositions(positions);
        instanced3DModelBinary.setRotations(rotations);
        instanced3DModelBinary.setScales(scales);
        instanced3DModelBinary.setFeatureIds(batchId);

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        File outputFile = new File(globalOptions.getOutputPath());
        Path outputRoot = outputFile.toPath()
                .resolve("data");
        if (!outputRoot.toFile()
                .exists() && outputRoot.toFile()
                .mkdir()) {
            log.debug("[Create][data] Created output data directory, {}", outputRoot);
        }

        GaiaFeatureTable featureTable = new GaiaFeatureTable();

        // IMPORTANT!
        featureTable.setInstancedBuffer(instanced3DModelBinary);

        if (!globalOptions.isClassicTransformMatrix()) {
            double[] rtcCenter = new double[3];
            rtcCenter[0] = transformMatrix.m30();
            rtcCenter[1] = transformMatrix.m31();
            rtcCenter[2] = transformMatrix.m32();
            featureTable.setRtcCenter(rtcCenter);
        }

        featureTable.setInstancesLength(instanceLength);
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
                    batchTableMap.get(key)
                            .add(utf8Value);
                });
            }
        });

        String glbFileName = nodeCode + "." + MAGIC;
        File i3dmOutputFile = outputRoot.resolve(glbFileName).toFile();
        createInstance(i3dmOutputFile, contentInfo, tileInfos.get(0), featureTable, batchTableMap);
        return contentInfo;
    }

    private void createInstance(File file, ContentInfo contentInfo, TileInfo tileInfo, GaiaFeatureTable featureTable, GaiaBatchTableMap<String, List<String>> batchTableMap) {
        try {
            GaiaScene firstGaiaScene = tileInfo.getScene();
            firstGaiaScene = firstGaiaScene.clone();

            GaiaSet set = GaiaSet.fromGaiaScene(firstGaiaScene);
            tileInfo.setSet(set);

            List<TileInfo> batchTileInfos = new ArrayList<>();
            batchTileInfos.add(tileInfo);

            GaiaBatcher gaiaBatcher = new GaiaBatcher();
            GaiaSet gaiaSet = gaiaBatcher.runBatching(batchTileInfos, contentInfo.getNodeCode(), contentInfo.getLod());
            GaiaScene resultGaiaScene = new GaiaScene(gaiaSet);
            gltfWriter.writeGlb(resultGaiaScene, file, featureTable, batchTableMap);
        } catch (Exception e) {
            log.error("[ERROR] :", e);
            throw new RuntimeException(e);
        }
    }
}
