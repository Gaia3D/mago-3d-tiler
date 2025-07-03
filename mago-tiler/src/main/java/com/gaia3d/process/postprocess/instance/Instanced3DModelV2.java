package com.gaia3d.process.postprocess.instance;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.jgltf.InstancedModelGltfWriter;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.process.postprocess.ContentModel;
import com.gaia3d.process.postprocess.batch.GaiaBatchTableMap;
import com.gaia3d.process.postprocess.batch.GaiaBatcher;
import com.gaia3d.process.postprocess.pointcloud.ByteAddress;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.GeometryUtils;
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

        //float[] normalUps = new float[instanceLength * 3];
        //float[] normalRights = new float[instanceLength * 3];
        float[] batchId = new float[instanceLength];

        Vector3d center = contentInfo.getBoundingBox().getCenter();
        Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);

        Vector3d centerWorldCoordinateYUp = new Vector3d(centerWorldCoordinate.x, centerWorldCoordinate.z, -centerWorldCoordinate.y);

        Matrix4d transformMatrix = GlobeUtils. (centerWorldCoordinateYUp);
        Matrix4d inverseTransformMatrix = transformMatrix.invert(new Matrix4d());

        AtomicInteger positionIndex = new AtomicInteger();
        AtomicInteger rotationIndex = new AtomicInteger();
        AtomicInteger normalUpIndex = new AtomicInteger();
        AtomicInteger normalRightIndex = new AtomicInteger();
        AtomicInteger scaleIndex = new AtomicInteger();
        AtomicInteger batchIdIndex = new AtomicInteger();
        for (TileInfo tileInfo : tileInfos) {
            //y-up
            //Vector3d normalRight = new Vector3d(1, 0, 0);
            //Vector3d normalUp = new Vector3d(0, 1, 0);
            Vector3d normalRight = new Vector3d(1, 0, 0);
            Vector3d normalUp = new Vector3d(0, 0, -1);

            // GPS Coordinates
            KmlInfo kmlInfo = tileInfo.getKmlInfo();
            Vector3d position = kmlInfo.getPosition();
            Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(position);
            Vector3d localPosition = positionWorldCoordinate.sub(centerWorldCoordinate, new Vector3d());

            Vector3d localPositionYUp = new Vector3d(localPosition.x, localPosition.y, localPosition.z);
            Matrix3d worldRotationMatrix3d = transformMatrix.get3x3(new Matrix3d());
            Matrix3d inverseWorldRotationMatrix3d = inverseTransformMatrix.get3x3(new Matrix3d());
            //worldRotationMatrix3d.rotateX(Math.toRadians(-90));


            // rotate
            double headingValue = Math.toRadians(kmlInfo.getHeading());
            double pitchValue = Math.toRadians(kmlInfo.getTilt());
            double rollValue = Math.toRadians(kmlInfo.getRoll());

            /*Matrix3d rotationMatrix = new Matrix3d();
            //rotationMatrix.rotateZ(-headingValue);
            //rotationMatrix.rotateX(-pitchValue);
            //rotationMatrix.rotateY(-rollValue);
            worldRotationMatrix3d.mul(rotationMatrix, worldRotationMatrix3d);*/

//            normalUp = worldRotationMatrix3d.transform(normalUp);
//            normalRight = worldRotationMatrix3d.transform(normalRight);

            // scale
            double scaleX = kmlInfo.getScaleX();
            double scaleY = kmlInfo.getScaleY();
            double scaleZ = kmlInfo.getScaleZ();

            positions[positionIndex.getAndIncrement()] = (float) localPositionYUp.x;
            positions[positionIndex.getAndIncrement()] = (float) localPositionYUp.z;
            positions[positionIndex.getAndIncrement()] = (float) -localPositionYUp.y;

//            normalUps[normalUpIndex.getAndIncrement()] = (float) normalUp.x;
//            normalUps[normalUpIndex.getAndIncrement()] = (float) normalUp.y;
//            normalUps[normalUpIndex.getAndIncrement()] = (float) normalUp.z;
//
//            normalRights[normalRightIndex.getAndIncrement()] = (float) normalRight.x;
//            normalRights[normalRightIndex.getAndIncrement()] = (float) normalRight.y;
//            normalRights[normalRightIndex.getAndIncrement()] = (float) normalRight.z;

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

            /*Quaterniond quaternion = instanceTransformMatrix.getNormalizedRotation(new Quaterniond());
            rotations[rotationIndex.getAndIncrement()] = (float) quaternion.x;
            rotations[rotationIndex.getAndIncrement()] = (float) quaternion.z;
            rotations[rotationIndex.getAndIncrement()] = (float) quaternion.y;
            rotations[rotationIndex.getAndIncrement()] = (float) quaternion.w;*/
        }

        Instanced3DModelBinary instanced3DModelBinary = new Instanced3DModelBinary();
        instanced3DModelBinary.setPositions(positions);
        instanced3DModelBinary.setRotations(rotations);
        instanced3DModelBinary.setScales(scales);
        instanced3DModelBinary.setFeatureIds(batchId);
        //instanced3DModelBinary.setNormalUps(normalUps);
        //instanced3DModelBinary.setNormalRights(normalRights);

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        File outputFile = new File(globalOptions.getOutputPath());
        Path outputRoot = outputFile.toPath()
                .resolve("data");
        if (!outputRoot.toFile()
                .exists() && outputRoot.toFile()
                .mkdir()) {
            log.debug("[Create][data] Created output data directory, {}", outputRoot);
        }

        /*byte[] positionBytes = instanced3DModelBinary.getPositionBytes();
        byte[] normalUpBytes = instanced3DModelBinary.getNormalUpBytes();
        byte[] normalRightBytes = instanced3DModelBinary.getNormalRightBytes();
        byte[] scaleBytes = instanced3DModelBinary.getScaleBytes();
        byte[] featureTableBytes = new byte[positionBytes.length + normalUpBytes.length + normalRightBytes.length + scaleBytes.length];
        System.arraycopy(positionBytes, 0, featureTableBytes, 0, positionBytes.length);
        System.arraycopy(normalUpBytes, 0, featureTableBytes, positionBytes.length, normalUpBytes.length);
        System.arraycopy(normalRightBytes, 0, featureTableBytes, positionBytes.length + normalUpBytes.length, normalRightBytes.length);
        System.arraycopy(scaleBytes, 0, featureTableBytes, positionBytes.length + normalUpBytes.length + normalRightBytes.length, scaleBytes.length)*/;

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
        /*featureTable.setEastNorthUp(false);
        featureTable.setPosition(new ByteAddress(0));
        featureTable.setNormalUp(new ByteAddress(positionBytes.length));
        featureTable.setNormalRight(new ByteAddress(positionBytes.length + normalUpBytes.length));
        featureTable.setScale(new ByteAddress(positionBytes.length + normalUpBytes.length + normalRightBytes.length));*/

        GaiaBatchTableMap<String, List<String>> batchTableMap = new GaiaBatchTableMap<>();
        AtomicInteger finalBatchIdIndex = new AtomicInteger();
        tileInfos.forEach((tileInfo) -> {
            GaiaAttribute attribute = tileInfo.getScene()
                    .getAttribute();
            Map<String, String> attributes = tileInfo.getKmlInfo()
                    .getProperties();

            String UUID = attribute.getIdentifier()
                    .toString();
            String FileName = attribute.getFileName();
            String NodeName = attribute.getNodeName();

            UUID = StringUtils.convertUTF8(UUID);
            FileName = StringUtils.convertUTF8(FileName);
            NodeName = StringUtils.convertUTF8(NodeName);

            batchTableMap.computeIfAbsent("UUID", k -> new ArrayList<>());
            batchTableMap.get("UUID")
                    .add(UUID);

            batchTableMap.computeIfAbsent("FileName", k -> new ArrayList<>());
            batchTableMap.get("FileName")
                    .add(FileName);

            batchTableMap.computeIfAbsent("NodeName", k -> new ArrayList<>());
            batchTableMap.get("NodeName")
                    .add(NodeName);

            batchTableMap.computeIfAbsent("BatchId", k -> new ArrayList<>());
            batchTableMap.get("BatchId")
                    .add(String.valueOf(batchId[finalBatchIdIndex.getAndIncrement()]));

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
        File i3dmOutputFile = outputRoot.resolve(glbFileName)
                .toFile();
        createInstance(i3dmOutputFile, contentInfo, tileInfos.get(0), featureTable, batchTableMap);
        return contentInfo;
    }

    private void createInstance(File file, ContentInfo contentInfo, TileInfo tileInfo, GaiaFeatureTable featureTable, GaiaBatchTableMap<String, List<String>> batchTableMap) {
        boolean isVoxelLod = GlobalOptions.getInstance()
                .isVoxelLod();

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
            GaiaBoundingBox boundingBox = resultGaiaScene.getBoundingBox();
            float minSize = (float) boundingBox.getMinSize();

            if (isVoxelLod) {
                int lod = contentInfo.getLod()
                        .getLevel();
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
            }
            gltfWriter.writeGlb(resultGaiaScene, file, featureTable, batchTableMap);
        } catch (Exception e) {
            log.error("[ERROR] :", e);
            throw new RuntimeException(e);
        }
    }
}
