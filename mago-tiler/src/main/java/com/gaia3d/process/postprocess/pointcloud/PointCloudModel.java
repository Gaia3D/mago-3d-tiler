package com.gaia3d.process.postprocess.pointcloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.pointcloud.GaiaLasPoint;
import com.gaia3d.converter.pointcloud.GaiaPointCloud;
import com.gaia3d.process.postprocess.ComponentType;
import com.gaia3d.process.postprocess.ContentModel;
import com.gaia3d.process.postprocess.DataType;
import com.gaia3d.process.postprocess.batch.GaiaBatchTable;
import com.gaia3d.process.postprocess.instance.GaiaFeatureTable;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.GlobeUtils;
import com.gaia3d.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix3d;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.proj4j.BasicCoordinateTransform;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RequiredArgsConstructor
public class PointCloudModel implements ContentModel {
    @Override
    public ContentInfo run(ContentInfo contentInfo) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        String featureTableJson;
        String batchTableJson;

        File outputFile = new File(globalOptions.getOutputPath());
        Path outputRoot = outputFile.toPath().resolve("data");
        File outputRootFile = outputRoot.toFile();
        if (!outputRootFile.mkdir() && !outputRootFile.exists()) {
            log.error("[ERROR] Failed to create output directory : {}", outputRoot);
        }
        List<TileInfo> tileInfos = contentInfo.getTileInfos();

        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        AtomicLong vertexCount = new AtomicLong();
        tileInfos.forEach((tileInfo) -> {
            GaiaPointCloud pointCloud = tileInfo.getPointCloud();
            vertexCount.addAndGet(pointCloud.getPointCount());
            boundingBox.addBoundingBox(pointCloud.getGaiaBoundingBox());
        });

        Vector3d originalMinPosition = boundingBox.getMinPosition();
        Vector3d originalMaxPosition = boundingBox.getMaxPosition();
        CoordinateReferenceSystem source = globalOptions.getSourceCrs();
        BasicCoordinateTransform transformer = new BasicCoordinateTransform(source, GlobeUtils.wgs84);

        ProjCoordinate transformedMinCoordinate = transformer.transform(new ProjCoordinate(originalMinPosition.x, originalMinPosition.y, originalMinPosition.z), new ProjCoordinate());
        Vector3d minPosition = new Vector3d(transformedMinCoordinate.x, transformedMinCoordinate.y, originalMinPosition.z);
        ProjCoordinate transformedMaxCoordinate = transformer.transform(new ProjCoordinate(originalMaxPosition.x, originalMaxPosition.y, originalMaxPosition.z), new ProjCoordinate());
        Vector3d maxPosition = new Vector3d(transformedMaxCoordinate.x, transformedMaxCoordinate.y, originalMaxPosition.z);
        GaiaBoundingBox wgs84BoundingBox = new GaiaBoundingBox();
        wgs84BoundingBox.addPoint(minPosition);
        wgs84BoundingBox.addPoint(maxPosition);

        int vertexLength = Math.toIntExact(vertexCount.get());
        float[] positions = new float[vertexLength * 3];
        int[] quantizedPositions = new int[vertexLength * 3];
        byte[] colors = new byte[vertexLength * 3];
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

        GaiaBoundingBox quantizedVolume = new GaiaBoundingBox();
        AtomicInteger mainIndex = new AtomicInteger();
        AtomicInteger positionIndex = new AtomicInteger();
        AtomicInteger colorIndex = new AtomicInteger();
        tileInfos.forEach((tileInfo) -> {
            GaiaPointCloud pointCloud = tileInfo.getPointCloud();
            pointCloud.maximize(false);
            List<GaiaLasPoint> gaiaVertex = pointCloud.getLasPoints();
            gaiaVertex.forEach((vertex) -> {
                int index = mainIndex.getAndIncrement();
                if (index > vertexLength) {
                    log.error("[ERROR] Index out of bound");
                    return;
                }

                Vector3d wgs84Position = vertex.getVec3Position();
                Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(wgs84Position);
                Vector3d localPosition = positionWorldCoordinate.mulPosition(transformMatrixInv, new Vector3d());
                localPosition.mulPosition(rotationMatrix4d, localPosition);

                float x = (float) localPosition.x;
                float y = (float) -localPosition.z;
                float z = (float) localPosition.y;
                quantizedVolume.addPoint(new Vector3d(x, y, z));

                positions[positionIndex.getAndIncrement()] = x;
                positions[positionIndex.getAndIncrement()] = y;
                positions[positionIndex.getAndIncrement()] = z;

                byte[] color = vertex.getRgb();
                colors[colorIndex.getAndIncrement()] = color[0];
                colors[colorIndex.getAndIncrement()] = color[1];
                colors[colorIndex.getAndIncrement()] = color[2];

                intensity[index] = vertex.getIntensity();
                classification[index] = vertex.getClassification();
            });
            pointCloud.clearPoints();
        });

        // quantization
        Vector3d quantizationScale = calcQuantizedVolumeScale(quantizedVolume);
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
        }

        // check quantizationScale, quantizationOffset is NaN or Infinity
        if (Double.isNaN(quantizationScale.x) || Double.isNaN(quantizationScale.y) || Double.isNaN(quantizationScale.z)) {
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
        }

        PointCloudBuffer pointCloudBuffer = new PointCloudBuffer();
        pointCloudBuffer.setQuantizedPositions(quantizedPositions);
        pointCloudBuffer.setColors(colors);
        pointCloudBuffer.setIntensities(intensity);
        pointCloudBuffer.setClassifications(classification);

        byte[] positionBytes = pointCloudBuffer.getQuantizedPositionBytes();
        byte[] colorBytes = pointCloudBuffer.getColorBytes();
        byte[] intensityBytes = pointCloudBuffer.getIntensityBytes();
        byte[] classificationBytes = pointCloudBuffer.getClassificationBytes();

        byte[] featureTableBytes = new byte[positionBytes.length + colorBytes.length];
        System.arraycopy(positionBytes, 0, featureTableBytes, 0, positionBytes.length);
        System.arraycopy(colorBytes, 0, featureTableBytes, positionBytes.length, colorBytes.length);

        byte[] batchTableBytes = new byte[intensityBytes.length + classificationBytes.length];
        System.arraycopy(intensityBytes, 0, batchTableBytes, 0, intensityBytes.length);
        System.arraycopy(classificationBytes, 0, batchTableBytes, intensityBytes.length, classificationBytes.length);

        GaiaFeatureTable featureTable = new GaiaFeatureTable();
        featureTable.setPointsLength(vertexLength);
        featureTable.setQuantizedVolumeOffset(new Float[]{(float) quantizationOffset.x, (float) quantizationOffset.y, (float) quantizationOffset.z});
        featureTable.setQuantizedVolumeScale(new Float[]{(float) quantizationScale.x, (float) quantizationScale.y, (float) quantizationScale.z});
        featureTable.setPositionQuantized(new ByteAddress(0, ComponentType.UNSIGNED_SHORT, DataType.VEC3));
        featureTable.setColor(new ByteAddress(positionBytes.length, ComponentType.UNSIGNED_BYTE, DataType.VEC3));

        if (!globalOptions.isClassicTransformMatrix()) {
            Double[] rtcCenter = new Double[3];
            rtcCenter[0] = transformMatrix.m30();
            rtcCenter[1] = transformMatrix.m31();
            rtcCenter[2] = transformMatrix.m32();
            featureTable.setRtcCenter(rtcCenter);
        }

        GaiaBatchTable batchTable = new GaiaBatchTable();
        batchTable.setIntensity(new ByteAddress(0, ComponentType.UNSIGNED_SHORT, DataType.SCALAR));
        batchTable.setClassification(new ByteAddress(intensityBytes.length, ComponentType.UNSIGNED_SHORT, DataType.SCALAR));

        ObjectMapper objectMapper = new ObjectMapper();
        if (!globalOptions.isDebug()) {
            objectMapper.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
        }
        try {
            featureTableJson = StringUtils.doPadding8Bytes(objectMapper.writeValueAsString(featureTable));
            batchTableJson = StringUtils.doPadding8Bytes(objectMapper.writeValueAsString(batchTable));
        } catch (JsonProcessingException e) {
            log.error("[ERROR] :", e);
            throw new RuntimeException(e);
        }

        PointCloudBinaryWriter writer = new PointCloudBinaryWriter(featureTableJson, batchTableJson, featureTableBytes, batchTableBytes);
        writer.write(outputRoot, contentInfo.getNodeCode());
        return contentInfo;
    }

    private Vector3d calcQuantizedVolumeOffset(GaiaBoundingBox boundingBox) {
        Vector3d min = boundingBox.getMinPosition();
        return min;
    }

    private Vector3d calcQuantizedVolumeScale(GaiaBoundingBox boundingBox) {
        Vector3d volume = boundingBox.getVolume();
        if (volume.x == 0) {
            volume.x = 1;
        }
        if (volume.y == 0) {
            volume.y = 1;
        }
        if (volume.z == 0) {
            volume.z = 1;
        }
        return volume;
    }
}
