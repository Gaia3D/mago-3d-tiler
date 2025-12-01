package com.gaia3d.process.postprocess.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaVertex;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.gltf.tiles.PointCloudGltfWriter;
import com.gaia3d.converter.pointcloud.GaiaLasPoint;
import com.gaia3d.converter.pointcloud.GaiaPointCloud;
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
            vertexCount.addAndGet((int) pointCloud.getPointCount());
            boundingBox.addBoundingBox(pointCloud.getGaiaBoundingBox());
        });

        int vertexLength = vertexCount.get();
        float[] batchIds = new float[vertexLength];
        float[] positions = new float[vertexLength * 3];
        int[] quantizedPositions = new int[vertexLength * 3];
        byte[] colors = new byte[vertexLength * 4];
        char[] intensity = new char[vertexLength];
        short[] classification = new short[vertexLength];

        Vector3d center = boundingBox.getCenter();
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
            pointCloud.maximize(true);

            List<GaiaLasPoint> gaiaVertex = pointCloud.getLasPoints();
            gaiaVertex.forEach((vertex) -> {
                int index = mainIndex.getAndIncrement();
                if (index > vertexLength) {
                    log.error("[ERROR] Index out of bound");
                    return;
                }

                batchIds[index] = index;

                Vector3d wgs84Position = vertex.getVec3Position();
                Vector3d positionWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(wgs84Position);
                Vector3d localPosition = positionWorldCoordinate.mulPosition(transformMatrixInv, new Vector3d());
                localPosition.mulPosition(rotationMatrix4d, localPosition);

                float x = (float) localPosition.x;
                float y = (float) localPosition.y;
                float z = (float) localPosition.z;

                positions[positionIndex.getAndIncrement()] = x;
                positions[positionIndex.getAndIncrement()] = y;
                positions[positionIndex.getAndIncrement()] = z;

                byte[] color = vertex.getRgb();
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
            pointCloud.clearPoints();
        });

        PointCloudBuffer pointCloudBuffer = new PointCloudBuffer();
        pointCloudBuffer.setPositions(positions);
        pointCloudBuffer.setColors(colors);
        pointCloudBuffer.setIntensities(intensity);
        pointCloudBuffer.setClassifications(classification);
        pointCloudBuffer.setBatchIds(batchIds);

        GaiaFeatureTable featureTable = new GaiaFeatureTable();
        featureTable.setPointsLength(vertexLength);

        if (!globalOptions.isClassicTransformMatrix()) {
            double[] rtcCenter = new double[3];
            rtcCenter[0] = transformMatrix.m30();
            rtcCenter[1] = transformMatrix.m31();
            rtcCenter[2] = transformMatrix.m32();
            featureTable.setRtcCenter(rtcCenter);
        }

        GaiaBatchTable batchTable = new GaiaBatchTable();
        String nodeCode = contentInfo.getNodeCode();
        String glbFileName = nodeCode + "." + MAGIC;
        File glbOutputFile = outputRoot.resolve(glbFileName).toFile();

        if (pointCloudBuffer.getPositions().length == 0) {
            log.warn("[WARN] Point cloud has no position data. Skip writing glb file for node : {}", nodeCode);
            return contentInfo;
        }
        this.gltfWriter.writeGlb(pointCloudBuffer, featureTable, batchTable, glbOutputFile);
        return contentInfo;
    }

    private int srgbToLinearByte(int sRGB) {
        float c = sRGB / 255.0f;
        float linear;
        if (c <= 0.04045f) {linear = c / 12.92f;} else {linear = (float) Math.pow((c + 0.055f) / 1.055f, 2.4);}
        int linearByte = Math.round(linear * 255.0f);
        return Math.max(0, Math.min(255, linearByte));
    }

    private int signedByteToUnsignedByte(int signedByte) {
        return signedByte < 0 ? signedByte + 256 : signedByte;
    }
}
