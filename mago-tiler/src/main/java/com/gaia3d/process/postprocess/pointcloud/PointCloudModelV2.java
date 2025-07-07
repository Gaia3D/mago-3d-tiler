package com.gaia3d.process.postprocess.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaVertex;
import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.jgltf.PointCloudGltfWriter;
import com.gaia3d.process.postprocess.ContentModel;
import com.gaia3d.process.postprocess.batch.GaiaBatchTable;
import com.gaia3d.process.postprocess.instance.GaiaFeatureTable;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.GlobeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix3d;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.proj4j.BasicCoordinateTransform;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.InvalidValueException;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class PointCloudModelV2 implements ContentModel {
    private static final String MAGIC = "glb";
    private final PointCloudGltfWriter gltfWriter;

    public PointCloudModelV2() {
        this.gltfWriter = new PointCloudGltfWriter();
    }

    @Override
    public ContentInfo run(ContentInfo contentInfo) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();

        File outputFile = new File(globalOptions.getOutputPath());
        Path outputRoot = outputFile.toPath().resolve("data");
        File outputRootFile = outputRoot.toFile();
        if (!outputRootFile.mkdir() && !outputRootFile.exists()) {
            log.error("[ERROR] Failed to create output directory : {}", outputRoot);
        }
        List<TileInfo> tileInfos = contentInfo.getTileInfos();

        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        AtomicInteger vertexCount = new AtomicInteger();
        tileInfos.forEach((tileInfo) -> {
            GaiaPointCloud pointCloud = tileInfo.getPointCloud();
            vertexCount.addAndGet(pointCloud.getVertexCount());
            boundingBox.addBoundingBox(pointCloud.getGaiaBoundingBox());
        });

        Vector3d originalMinPosition = boundingBox.getMinPosition();
        Vector3d originalMaxPosition = boundingBox.getMaxPosition();
        CoordinateReferenceSystem source = globalOptions.getCrs();
        BasicCoordinateTransform transformer = new BasicCoordinateTransform(source, GlobeUtils.wgs84);

        ProjCoordinate transformedMinCoordinate = transformer.transform(new ProjCoordinate(originalMinPosition.x, originalMinPosition.y, originalMinPosition.z), new ProjCoordinate());
        Vector3d minPosition = new Vector3d(transformedMinCoordinate.x, transformedMinCoordinate.y, originalMinPosition.z);
        ProjCoordinate transformedMaxCoordinate = transformer.transform(new ProjCoordinate(originalMaxPosition.x, originalMaxPosition.y, originalMaxPosition.z), new ProjCoordinate());
        Vector3d maxPosition = new Vector3d(transformedMaxCoordinate.x, transformedMaxCoordinate.y, originalMaxPosition.z);
        GaiaBoundingBox wgs84BoundingBox = new GaiaBoundingBox();
        wgs84BoundingBox.addPoint(minPosition);
        wgs84BoundingBox.addPoint(maxPosition);

        int vertexLength = vertexCount.get();
        float[] batchIds = new float[vertexLength];
        float[] positions = new float[vertexLength * 3];
        int[] quantizedPositions = new int[vertexLength * 3];
        byte[] colors = new byte[vertexLength * 4];
        char[] intensity = new char[vertexLength];
        short[] classification = new short[vertexLength];

        Vector3d center = wgs84BoundingBox.getCenter();
        Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
        Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerWorldCoordinate);
        Matrix4d transformMatrixInv = new Matrix4d(transformMatrix).invert();

        Matrix3d rotationMatrix3d = transformMatrix.get3x3(new Matrix3d());
        Matrix3d xRotationMatrix3d = new Matrix3d();
        xRotationMatrix3d.identity();
        xRotationMatrix3d.rotateX(Math.toRadians(-90));
        xRotationMatrix3d.mul(rotationMatrix3d, rotationMatrix3d);
        Matrix4d rotationMatrix4d = new Matrix4d(rotationMatrix3d);

        //GaiaBoundingBox quantizedVolume = new GaiaBoundingBox();
        AtomicInteger mainIndex = new AtomicInteger();
        AtomicInteger positionIndex = new AtomicInteger();
        AtomicInteger colorIndex = new AtomicInteger();

        tileInfos.forEach((tileInfo) -> {
            GaiaPointCloud pointCloud = tileInfo.getPointCloud();
            pointCloud.maximize();
            List<GaiaVertex> gaiaVertex = pointCloud.getVertices();
            gaiaVertex.forEach((vertex) -> {
                int index = mainIndex.getAndIncrement();
                if (index > vertexLength) {
                    log.error("[ERROR] Index out of bound");
                    return;
                }

                batchIds[index] = index;

                Vector3d position = vertex.getPosition();
                Vector3d wgs84Position = new Vector3d();
                try {
                    ProjCoordinate transformedCoordinate = transformer.transform(new ProjCoordinate(position.x, position.y, position.z), new ProjCoordinate());
                    wgs84Position = new Vector3d(transformedCoordinate.x, transformedCoordinate.y, position.z);
                } catch (InvalidValueException e) {
                    log.debug("Invalid value exception", e);
                }

                Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(wgs84Position);
                Vector3d localPosition = positionWorldCoordinate.mulPosition(transformMatrixInv, new Vector3d());
                localPosition.mulPosition(rotationMatrix4d, localPosition);

                float x = (float) localPosition.x;
                float y = (float) localPosition.y;
                float z = (float) localPosition.z;
                //float y = (float) -localPosition.z;
                //float z = (float) localPosition.y;
                //quantizedVolume.addPoint(new Vector3d(x, y, z));

                positions[positionIndex.getAndIncrement()] = x;
                positions[positionIndex.getAndIncrement()] = y;
                positions[positionIndex.getAndIncrement()] = z;

                byte[] color = vertex.getColor();
                color[0] = (byte) srgbToLinearByte(signedByteToUnsignedByte(color[0]));
                color[1] = (byte) srgbToLinearByte(signedByteToUnsignedByte(color[1]));
                color[2] = (byte) srgbToLinearByte(signedByteToUnsignedByte(color[2]));
                colors[colorIndex.getAndIncrement()] = color[0];
                colors[colorIndex.getAndIncrement()] = color[1];
                colors[colorIndex.getAndIncrement()] = color[2];
                colors[colorIndex.getAndIncrement()] = -1;

                intensity[index] = vertex.getIntensity();
                classification[index] = vertex.getClassification();
            });
            pointCloud.minimizeTemp();
        });

        // quantization
        /*Vector3d quantizationScale = calcQuantizedVolumeScale(quantizedVolume);
        Vector3d quantizationOffset = calcQuantizedVolumeOffset(quantizedVolume);
        for (int i = 0; i < positions.length; i += 3) {
            double x = positions[i];
            double y = positions[i + 1];
            double z = positions[i + 2];
            double xQuantized = (x - quantizationOffset.x) / quantizationScale.x;
            double yQuantized = (y - quantizationOffset.y) / quantizationScale.y;
            double zQuantized = (z - quantizationOffset.z) / quantizationScale.z;

            quantizedPositions[i] = (int) (xQuantized * 65535);
            quantizedPositions[i + 1] = (int) (yQuantized * 65535);
            quantizedPositions[i + 2] = (int) (zQuantized * 65535);

            // Clamp to 16-bit unsigned integer range
            if (quantizedPositions[i] < 0) {
                quantizedPositions[i] = 0;
                log.error("[ERROR] Quantized position x is less than 0");
            } else if (quantizedPositions[i] > 65535) {
                quantizedPositions[i] = 65535;
                log.error("[ERROR] Quantized position x is greater than 65535");
            }

            if (quantizedPositions[i + 1] < 0) {
                quantizedPositions[i + 1] = 0;
                log.error("[ERROR] Quantized position y is less than 0");
            } else if (quantizedPositions[i + 1] > 65535) {
                quantizedPositions[i + 1] = 65535;
                log.error("[ERROR] Quantized position y is greater than 65535");
            }

            if (quantizedPositions[i + 2] < 0) {
                quantizedPositions[i + 2] = 0;
                log.error("[ERROR] Quantized position z is less than 0");
            } else if (quantizedPositions[i + 2] > 65535) {
                quantizedPositions[i + 2] = 65535;
                log.error("[ERROR] Quantized position z is greater than 65535");
            }
        }*/

        // check quantizationScale, quantizationOffset is NaN or Infinity
        /*if (Double.isNaN(quantizationScale.x) || Double.isNaN(quantizationScale.y) || Double.isNaN(quantizationScale.z)) {
            log.error("[ERROR] Quantization scale is NaN");
            log.error("[ERROR] Quantization scale : {}", quantizationScale);
            log.error("[ERROR] Quantized volume : {}", quantizedVolume);
        } else if (Double.isInfinite(quantizationScale.x) || Double.isInfinite(quantizationScale.y) || Double.isInfinite(quantizationScale.z)) {
            log.error("[ERROR] Quantization scale is Infinite");
            log.error("[ERROR] Quantization scale : {}", quantizationScale);
            log.error("[ERROR] Quantized volume : {}", quantizedVolume);
        } else if (Double.isNaN(quantizationOffset.x) || Double.isNaN(quantizationOffset.y) || Double.isNaN(quantizationOffset.z)) {
            log.error("[ERROR] Quantization offset is NaN");
            log.error("[ERROR] Quantization offset : {}", quantizationOffset);
            log.error("[ERROR] Quantized volume : {}", quantizedVolume);
        } else if (Double.isInfinite(quantizationOffset.x) || Double.isInfinite(quantizationOffset.y) || Double.isInfinite(quantizationOffset.z)) {
            log.error("[ERROR] Quantization offset is Infinite");
            log.error("[ERROR] Quantization offset : {}", quantizationOffset);
            log.error("[ERROR] Quantized volume : {}", quantizedVolume);
        }*/

        PointCloudBuffer pointCloudBuffer = new PointCloudBuffer();
        pointCloudBuffer.setPositions(positions);
        pointCloudBuffer.setColors(colors);
        pointCloudBuffer.setIntensities(intensity);
        pointCloudBuffer.setClassifications(classification);
        pointCloudBuffer.setBatchIds(batchIds);

        /*byte[] positionBytes = pointCloudBinary.getQuantizedPositionBytes();
        byte[] colorBytes = pointCloudBinary.getColorBytes();
        byte[] intensityBytes = pointCloudBinary.getIntensityBytes();
        byte[] classificationBytes = pointCloudBinary.getClassificationBytes();

        byte[] featureTableBytes = new byte[positionBytes.length + colorBytes.length];
        System.arraycopy(positionBytes, 0, featureTableBytes, 0, positionBytes.length);
        System.arraycopy(colorBytes, 0, featureTableBytes, positionBytes.length, colorBytes.length);

        byte[] batchTableBytes = new byte[intensityBytes.length + classificationBytes.length];
        System.arraycopy(intensityBytes, 0, batchTableBytes, 0, intensityBytes.length);
        System.arraycopy(classificationBytes, 0, batchTableBytes, intensityBytes.length, classificationBytes.length);*/

        GaiaFeatureTable featureTable = new GaiaFeatureTable();
        featureTable.setPointsLength(vertexLength);
        //featureTable.setQuantizedVolumeOffset(new float[]{(float) quantizationOffset.x, (float) quantizationOffset.y, (float) quantizationOffset.z});
        //featureTable.setQuantizedVolumeScale(new float[]{(float) quantizationScale.x, (float) quantizationScale.y, (float) quantizationScale.z});
        //featureTable.setPositionQuantized(new ByteAddress(0, ComponentType.UNSIGNED_SHORT, DataType.VEC3));
        //featureTable.setColor(new ByteAddress(positionBytes.length, ComponentType.UNSIGNED_BYTE, DataType.VEC3));

        if (!globalOptions.isClassicTransformMatrix()) {
            double[] rtcCenter = new double[3];
            rtcCenter[0] = transformMatrix.m30();
            rtcCenter[1] = transformMatrix.m31();
            rtcCenter[2] = transformMatrix.m32();
            featureTable.setRtcCenter(rtcCenter);
        }

        GaiaBatchTable batchTable = new GaiaBatchTable();
        //batchTable.setIntensity(new ByteAddress(0, ComponentType.UNSIGNED_SHORT, DataType.SCALAR));
        //atchTable.setClassification(new ByteAddress(intensityBytes.length, ComponentType.UNSIGNED_SHORT, DataType.SCALAR));

        String nodeCode = contentInfo.getNodeCode();
        String glbFileName = nodeCode + "." + MAGIC;
        File glbOutputFile = outputRoot.resolve(glbFileName).toFile();
        this.gltfWriter.writeGlb(pointCloudBuffer, featureTable, batchTable, glbOutputFile);


        /*ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);

        try {
            featureTableJson = StringUtils.doPadding8Bytes(objectMapper.writeValueAsString(featureTable));
            batchTableJson = StringUtils.doPadding8Bytes(objectMapper.writeValueAsString(batchTable));
        } catch (JsonProcessingException e) {
            log.error("[ERROR] :", e);
            throw new RuntimeException(e);
        }
        PointCloudBinaryWriter writer = new PointCloudBinaryWriter(featureTableJson, batchTableJson, featureTableBytes, batchTableBytes);
        writer.write(outputRoot, contentInfo.getNodeCode());*/


        return contentInfo;
    }

    int srgbToLinearByte(int sRGB) {
        float c = sRGB / 255.0f; // 정규화
        float linear;
        if (c <= 0.04045f) linear = c / 12.92f;
        else linear = (float) Math.pow((c + 0.055f) / 1.055f, 2.4);
        int linearByte = Math.round(linear * 255.0f);
        // 0~255 범위를 벗어나지 않도록 클램프
        return Math.max(0, Math.min(255, linearByte));
    }

    int signedByteToUnsignedByte(int signedByte) {
        // Java의 byte는 -128 ~ 127 범위이므로, 이를 0 ~ 255 범위로 변환
        return signedByte < 0 ? signedByte + 256 : signedByte;
    }
}
