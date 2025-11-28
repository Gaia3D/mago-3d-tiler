package com.gaia3d.converter.pointcloud;

import com.gaia3d.util.geographic.GeographicTilingScheme;
import com.gaia3d.util.geographic.TileCoordinate;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class BucketWriter implements Closeable {
    private static final int COARSE_LEVEL = 13;
    private static final int POINT_BLOCK_SIZE = 32;
    private static final int BUFFER_SIZE = 4 * 1024 * 1024; // 4MB

    private static class BucketBuffer {
        byte[] buf = new byte[BUFFER_SIZE];
        int offset = 0;
    }

    private final GeographicTilingScheme scheme = new GeographicTilingScheme();
    private final Map<Integer, BucketBuffer> buffers = new HashMap<>();

    // 버킷별로 파일 핸들을 관리하는 풀 (LRU로 close/open 관리)
    private final FileHandlePool fileHandlePool;

    public BucketWriter(Path tempRootDir) throws IOException {
        this.fileHandlePool = new FileHandlePool(tempRootDir, 128); // 동시에 128개까지만
    }

    public void addPoint(GaiaLasPoint point) throws IOException {
        double[] position = point.getPosition();
        double lon = position[0];
        double lat = position[1];
        int bucketId = computeBucketId(lon, lat);
        BucketBuffer buffer = buffers.computeIfAbsent(bucketId, id -> new BucketBuffer());

        byte[] pointBytes = BigEndianByteUtils.fromDoubles(position);
        byte[] rgb = point.getRgb();
        byte[] intensity = BigEndianByteUtils.fromChar(point.getIntensity());
        byte[] classification = BigEndianByteUtils.fromShort(point.getClassification());
        byte[] totalByte = new byte[POINT_BLOCK_SIZE];
        System.arraycopy(pointBytes, 0, totalByte, 0, pointBytes.length);
        System.arraycopy(rgb, 0, totalByte, pointBytes.length, rgb.length);
        System.arraycopy(intensity, 0, totalByte, pointBytes.length + rgb.length, intensity.length);
        System.arraycopy(classification, 0, totalByte, pointBytes.length + rgb.length + intensity.length, classification.length);

        // 레코드 쓰기 (예: lon, lat, height만)
        if (buffer.offset + POINT_BLOCK_SIZE > buffer.buf.length) {
            flushBuffer(bucketId, buffer);
        }

        writeBytes(buffer, totalByte);
    }

    private int computeBucketId(double lon, double lat) {
        TileCoordinate tile = scheme.positionToTile(COARSE_LEVEL, lat, lon);
        // 간단히 x,y를 16bit씩 packing
        return (tile.x & 0xFFFF) | ((tile.y & 0xFFFF) << 16);
    }

    private TileCoordinate bucketIdToTileCoordinate(int bucketId) {
        int x = bucketId & 0xFFFF;
        int y = (bucketId >> 16) & 0xFFFF;
        return new TileCoordinate(COARSE_LEVEL, x, y);
    }

    private void flushBuffer(int bucketId, BucketBuffer buffer) throws IOException {
        if (buffer.offset == 0) return;

        OutputStream out = fileHandlePool.getOutputStream(bucketId); // append mode
        out.write(buffer.buf, 0, buffer.offset);
        buffer.offset = 0;
    }

    private static void writeBytes(BucketBuffer buffer, byte[] bytes) {
        System.arraycopy(bytes, 0, buffer.buf, buffer.offset, bytes.length);
        buffer.offset += bytes.length;
    }

    @Override
    public void close() throws IOException {
        // 모든 버퍼 flush
        for (Map.Entry<Integer, BucketBuffer> entry : buffers.entrySet()) {
            flushBuffer(entry.getKey(), entry.getValue());
        }
        fileHandlePool.close();
    }
}
