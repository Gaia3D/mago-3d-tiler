package com.gaia3d.converter.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.joml.Vector3d;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class GaiaPointCloud {
    //public final int CHUNK_SIZE = GaiaLasPoint.BYTES_SIZE * 20_000_000;
    public final long CHUNK_SIZE = GaiaLasPoint.BYTES_SIZE * 40_000_000L;

    private String code = "A";
    private Path originalPath;
    private GaiaBoundingBox gaiaBoundingBox = new GaiaBoundingBox();
    private List<GaiaLasPoint> lasPoints = new ArrayList<>();
    private long pointCount = 0;
    private File minimizedFile = null;

    private long limitPointCount = -1;
    private GaiaPointCloud parent = null;
    private List<GaiaPointCloud> children = new ArrayList<>();

    public String getFullCode() {
        StringBuilder sb = new StringBuilder();
        return getFullCode(sb);
    }

    public String createFullCodePath() {
        // RABCCABEF -> R/A/B/C/C/A/B/E/F
        String fullCode = getFullCode();
        StringBuilder sb = new StringBuilder();
        int maxDepth = 4;
        for (int i = 0; i < fullCode.length() && i < maxDepth; i++) {
            sb.append(fullCode.charAt(i));
            if (i < fullCode.length() - 1) {
                sb.append(File.separator);
            }
        }
        return sb.toString();
    }

    private String getFullCode(StringBuilder sb) {
        if (parent != null) {
            parent.getFullCode(sb);
        }
        sb.append(code);
        return sb.toString();
    }

    public void clearPoints() {
        if (lasPoints != null) {
            lasPoints.clear();
            lasPoints = null;
        }
    }

    public void removeMinimizedFile() {
        if (minimizedFile != null && minimizedFile.exists()) {
            FileUtils.deleteQuietly(minimizedFile);
            minimizedFile = null;
        }
    }

    public long getPointCount() {
        if (pointCount == 0 && lasPoints != null) {
            pointCount = lasPoints.size();
        }
        return pointCount;
    }

    public long getAllPointCount() {
        long total = getPointCount();
        if (children != null) {
            for (GaiaPointCloud child : children) {
                total += child.getAllPointCount();
            }
        }
        return total;
    }

    public int getMaxDepth() {
        int maxDepth = 0;
        if (children != null) {
            for (GaiaPointCloud child : children) {
                int childDepth = child.getMaxDepth();
                if (childDepth > maxDepth) {
                    maxDepth = childDepth;
                }
            }
        }
        return maxDepth + 1;
    }

    public int getNodeCount() {
        int count = 1; // Count this node
        if (children != null) {
            for (GaiaPointCloud child : children) {
                count += child.getNodeCount();
            }
        }
        return count;
    }

    public List<GaiaPointCloud> getFullLeaves() {
        List<GaiaPointCloud> leaves = new ArrayList<>();
        boolean hasPoints = (lasPoints != null && !lasPoints.isEmpty());
        boolean isFull = this.pointCount == this.limitPointCount;
        if (hasPoints && isFull) {
            leaves.add(this);
        }
        if (children != null) {
            for (GaiaPointCloud child : children) {
                leaves.addAll(child.getFullLeaves());
            }
        }
        return leaves;
    }

    public List<GaiaPointCloud> getAllLeaves() {
        List<GaiaPointCloud> leaves = new ArrayList<>();
        boolean hasPoints = (lasPoints != null && !lasPoints.isEmpty());
        if (hasPoints) {
            leaves.add(this);
        }
        if (children != null) {
            for (GaiaPointCloud child : children) {
                leaves.addAll(child.getAllLeaves());
            }
        }
        return leaves;
    }

    public void setLasPoints(List<GaiaLasPoint> lasPoints) {
        this.lasPoints = lasPoints;
        this.pointCount = lasPoints.size();
    }

    public void minimize(File minimizedFile) {
        if (lasPoints == null || lasPoints.isEmpty()) {
            // 비어 있으면 그냥 빈 파일 하나 만들어두고 끝내도 됨
            try {
                if (!minimizedFile.exists()) {
                    minimizedFile.getParentFile().mkdirs();
                    minimizedFile.createNewFile();
                }
            } catch (IOException e) {
                log.error("Failed to create empty minimized file: {}", minimizedFile.getAbsolutePath(), e);
            }
            this.minimizedFile = minimizedFile;
            this.pointCount = 0;
            return;
        }

        this.minimizedFile = minimizedFile;
        this.pointCount = lasPoints.size();

        // 한 번에 몇 개 포인트를 버퍼링할지 (튜닝 가능)
        final int pointsPerBatch = 4096; // 예: 4096 * 32B ≈ 128KB
        final int pointSize = GaiaLasPoint.BYTES_SIZE;
        final int batchBufferSize = pointsPerBatch * pointSize;

        byte[] batchBuffer = new byte[batchBufferSize];
        int bufferOffset = 0; // batchBuffer 내 현재 위치

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(minimizedFile), 1024 * 1024)) {
            for (GaiaLasPoint point : lasPoints) {
                byte[] pointBytes = point.toBytes();
                if (bufferOffset + pointSize > batchBufferSize) {
                    bos.write(batchBuffer, 0, bufferOffset);
                    bufferOffset = 0;
                }
                System.arraycopy(pointBytes, 0, batchBuffer, bufferOffset, pointSize);
                bufferOffset += pointSize;
            }

            if (bufferOffset > 0) {
                bos.write(batchBuffer, 0, bufferOffset);
            }

            bos.flush();
        } catch (IOException e) {
            log.error("Failed to minimize point cloud to file: {}", minimizedFile.getAbsolutePath(), e);
        } finally {
            if (this.lasPoints != null) {
                this.lasPoints.clear();
                this.lasPoints = null;
            }
        }
    }

    @Deprecated
    public void minimizeOld(File minimizedFile) {
        this.minimizedFile = minimizedFile;
        this.pointCount = lasPoints.size();
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(minimizedFile), 8192 * 8)) {
            List<GaiaLasPoint> points = this.getLasPoints();
            for (GaiaLasPoint point : points) {
                byte[] pointBytes = point.toBytes();
                bos.write(pointBytes);
            }
            bos.flush();
            this.lasPoints.clear();
            this.lasPoints = null;
        } catch (IOException e) {
            log.error("Failed to minimize point cloud to file: {}", minimizedFile.getAbsolutePath(), e);
        }
    }

    public int getChunkCount(long chunkSize) {
        if (this.minimizedFile == null) {
            log.warn("No minimized file to get chunk count from.");
            return 0;
        }

        long originalFileLength = this.minimizedFile.length();
        int chunkCount = (int) (originalFileLength / chunkSize);
        if (originalFileLength % chunkSize != 0) {
            chunkCount++;
        }
        return chunkCount;
    }

    public GaiaPointCloud readChunk(long chunkSize, long offset) {
        if (this.minimizedFile == null) {
            log.warn("No minimized file to read chunk from.");
            return null;
        }

        long originalFileLength = this.minimizedFile.length();
        if (offset >= originalFileLength) {
            log.warn("Offset {} is beyond the end of the minimized file.", offset);
            return null;
        }
        if (originalFileLength < offset + chunkSize) {
            chunkSize = originalFileLength - offset;
        }

        long chunkPointCount = chunkSize / GaiaLasPoint.BYTES_SIZE;

        GaiaPointCloud chunkPointCloud = new GaiaPointCloud();
        chunkPointCloud.setOriginalPath(this.originalPath);
        chunkPointCloud.setGaiaBoundingBox(this.gaiaBoundingBox);
        chunkPointCloud.setMinimizedFile(this.minimizedFile);
        chunkPointCloud.setCode("R");
        /*try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(this.minimizedFile))) {
            long totalPoints = this.pointCount;
            bis.skip(offset);
            List<GaiaLasPoint> points = new ArrayList<>();
            for (long i = 0; i < chunkPointCount && (i + offset / GaiaLasPoint.BYTES_SIZE) < totalPoints; i++) {
                byte[] pointBytes = new byte[GaiaLasPoint.BYTES_SIZE];
                int bytesRead = bis.read(pointBytes);
                if (bytesRead != GaiaLasPoint.BYTES_SIZE) {
                    log.error("Unexpected end of file while reading point cloud chunk.");
                    break;
                }
                GaiaLasPoint point = GaiaLasPoint.fromBytes(pointBytes);
                points.add(point);
            }
            chunkPointCloud.setLasPoints(points);
            chunkPointCloud.setPointCount(points.size());

            if (points.size() != chunkPointCount) {
                log.warn("Expected to read {} points, but only read {} points.", chunkPointCount, points.size());
            }
        } catch (IOException e) {
            log.error("Failed to read point cloud chunk from file: {}", this.minimizedFile.getAbsolutePath(), e);
        }*/
        try (RandomAccessFile raf = new RandomAccessFile(this.minimizedFile, "r")) {
            raf.seek(offset);
            List<GaiaLasPoint> points = new ArrayList<>();
            for (long i = 0; i < chunkPointCount && (i + offset / GaiaLasPoint.BYTES_SIZE) < (originalFileLength / GaiaLasPoint.BYTES_SIZE); i++) {
                byte[] pointBytes = new byte[GaiaLasPoint.BYTES_SIZE];
                int bytesRead = raf.read(pointBytes);
                if (bytesRead != GaiaLasPoint.BYTES_SIZE) {
                    log.error("Unexpected end of file while reading point cloud chunk.");
                    break;
                }
                GaiaLasPoint point = GaiaLasPoint.fromBytes(pointBytes);
                points.add(point);
            }
            chunkPointCloud.setLasPoints(points);
            chunkPointCloud.setPointCount(points.size());

            if (points.size() != chunkPointCount) {
                log.warn("Expected to read {} points, but only read {} points.", chunkPointCount, points.size());
            }
        } catch (IOException e) {
            log.error("Failed to read point cloud chunk from file: {}", this.minimizedFile.getAbsolutePath(), e);
        }
        return chunkPointCloud;
    }

    public void maximize(boolean deleteAfterMaximize) {
        if (this.minimizedFile == null) {
            log.warn("No minimized file to maximize.");
            return;
        }

        File file = this.minimizedFile;
        long fileLength = file.length();
        long totalPointsLong = fileLength / GaiaLasPoint.BYTES_SIZE;

        if (totalPointsLong > Integer.MAX_VALUE) {
            throw new IllegalStateException("Too many points to load into memory: " + totalPointsLong);
        }

        int totalPoints = (int) totalPointsLong;
        final int pointSize = GaiaLasPoint.BYTES_SIZE;

        List<GaiaLasPoint> points = new ArrayList<>(totalPoints > 0 ? totalPoints : 16);
        byte[] pointBytes = new byte[pointSize];
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file), 1024 * 1024)) { // 1MB 버퍼

            for (int i = 0; i < totalPoints; i++) {
                int readTotal = 0;
                while (readTotal < pointSize) {
                    int r = bis.read(pointBytes, readTotal, pointSize - readTotal);
                    if (r == -1) {
                        log.error("Unexpected end of file while reading point cloud. readTotal={}, expected={}", readTotal, pointSize);
                        break;
                    }
                    readTotal += r;
                }
                if (readTotal != pointSize) {
                    // 파일이 손상 된 경우 - 일단 여기까지 읽은 포인트만 사용
                    break;
                }

                GaiaLasPoint point = GaiaLasPoint.fromBytes(pointBytes);
                points.add(point);
            }

            this.lasPoints = points;
            this.pointCount = points.size();

            bis.close();
            if (deleteAfterMaximize) {
                FileUtils.deleteQuietly(file);
            }
            this.minimizedFile = null;
        } catch (IOException e) {
            log.error("Failed to maximize point cloud from file: {}", file.getAbsolutePath(), e);
        }
    }

    @Deprecated
    public void maximizeOld(boolean deleteAfterMaximize) {
        if (this.minimizedFile == null) {
            log.warn("No minimized file to maximize.");
            return;
        }

        long fileLength = this.minimizedFile.length();
        long totalPoints = fileLength / GaiaLasPoint.BYTES_SIZE;
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(this.minimizedFile))) {
            List<GaiaLasPoint> points = new ArrayList<>();
            for (long i = 0; i < totalPoints; i++) {
                byte[] pointBytes = new byte[GaiaLasPoint.BYTES_SIZE];
                int bytesRead = bis.read(pointBytes);
                if (bytesRead != GaiaLasPoint.BYTES_SIZE) {
                    log.error("Unexpected end of file while reading point cloud.");
                    break;
                }
                GaiaLasPoint point = GaiaLasPoint.fromBytes(pointBytes);
                points.add(point);
            }
            this.lasPoints = points;
            if (deleteAfterMaximize) {
                FileUtils.deleteQuietly(this.minimizedFile);
            }
            this.minimizedFile = null;
        } catch (IOException e) {
            log.error("Failed to maximize point cloud from file: {}", this.minimizedFile.getAbsolutePath(), e);
        }
    }

    public void computeBoundingBox() {
        gaiaBoundingBox = new GaiaBoundingBox();
        for (GaiaLasPoint vertex : lasPoints) {
            Vector3d center = vertex.getVec3Position();
            gaiaBoundingBox.addPoint(center);
        }
    }

    public List<GaiaPointCloud> distribute() {
        return distributeOct();
    }

    // Octree based on the bounding box
    public List<GaiaPointCloud> distributeOct() {
        List<GaiaPointCloud> pointClouds = new ArrayList<>();

        long estimatedPerChild = this.getPointCount() / 8;

        GaiaPointCloud gaiaPointCloudA = new GaiaPointCloud();
        gaiaPointCloudA.setCode("A");
        gaiaPointCloudA.setOriginalPath(originalPath);
        gaiaPointCloudA.setParent(this);
        ArrayList<GaiaLasPoint> verticesA = (ArrayList<GaiaLasPoint>) gaiaPointCloudA.getLasPoints();
        verticesA.ensureCapacity((int) estimatedPerChild);

        GaiaPointCloud gaiaPointCloudB = new GaiaPointCloud();
        gaiaPointCloudB.setCode("B");
        gaiaPointCloudB.setOriginalPath(originalPath);
        gaiaPointCloudB.setParent(this);
        ArrayList<GaiaLasPoint> verticesB = (ArrayList<GaiaLasPoint>) gaiaPointCloudB.getLasPoints();
        verticesB.ensureCapacity((int) estimatedPerChild);

        GaiaPointCloud gaiaPointCloudC = new GaiaPointCloud();
        gaiaPointCloudC.setCode("C");
        gaiaPointCloudC.setOriginalPath(originalPath);
        gaiaPointCloudC.setParent(this);
        ArrayList<GaiaLasPoint> verticesC = (ArrayList<GaiaLasPoint>) gaiaPointCloudC.getLasPoints();
        verticesC.ensureCapacity((int) estimatedPerChild);

        GaiaPointCloud gaiaPointCloudD = new GaiaPointCloud();
        gaiaPointCloudD.setCode("D");
        gaiaPointCloudD.setOriginalPath(originalPath);
        gaiaPointCloudD.setParent(this);
        ArrayList<GaiaLasPoint> verticesD = (ArrayList<GaiaLasPoint>) gaiaPointCloudD.getLasPoints();
        verticesD.ensureCapacity((int) estimatedPerChild);

        GaiaPointCloud gaiaPointCloudE = new GaiaPointCloud();
        gaiaPointCloudE.setCode("E");
        gaiaPointCloudE.setOriginalPath(originalPath);
        gaiaPointCloudE.setParent(this);
        ArrayList<GaiaLasPoint> verticesE = (ArrayList<GaiaLasPoint>) gaiaPointCloudE.getLasPoints();
        verticesE.ensureCapacity((int) estimatedPerChild);

        GaiaPointCloud gaiaPointCloudF = new GaiaPointCloud();
        gaiaPointCloudF.setCode("F");
        gaiaPointCloudF.setOriginalPath(originalPath);
        gaiaPointCloudF.setParent(this);
        ArrayList<GaiaLasPoint> verticesF = (ArrayList<GaiaLasPoint>) gaiaPointCloudF.getLasPoints();
        verticesF.ensureCapacity((int) estimatedPerChild);

        GaiaPointCloud gaiaPointCloudG = new GaiaPointCloud();
        gaiaPointCloudG.setCode("G");
        gaiaPointCloudG.setOriginalPath(originalPath);
        gaiaPointCloudG.setParent(this);
        ArrayList<GaiaLasPoint> verticesG = (ArrayList<GaiaLasPoint>) gaiaPointCloudG.getLasPoints();
        verticesG.ensureCapacity((int) estimatedPerChild);

        GaiaPointCloud gaiaPointCloudH = new GaiaPointCloud();
        gaiaPointCloudH.setCode("H");
        gaiaPointCloudH.setOriginalPath(originalPath);
        gaiaPointCloudH.setParent(this);
        ArrayList<GaiaLasPoint> verticesH = (ArrayList<GaiaLasPoint>) gaiaPointCloudH.getLasPoints();
        verticesH.ensureCapacity((int) estimatedPerChild);

        double minX = gaiaBoundingBox.getMinX();
        double minY = gaiaBoundingBox.getMinY();
        double minZ = gaiaBoundingBox.getMinZ();
        double maxX = gaiaBoundingBox.getMaxX();
        double maxY = gaiaBoundingBox.getMaxY();
        double maxZ = gaiaBoundingBox.getMaxZ();

        double midX = (minX + maxX) / 2;
        double midY = (minY + maxY) / 2;
        double midZ = (minZ + maxZ) / 2;

        // bit-mask octree indexing
        @SuppressWarnings("unchecked")
        List<GaiaLasPoint>[] bucketLists = new List[] {
                verticesA, // 000
                verticesC, // 001 (y)
                verticesB, // 010 (x)
                verticesD, // 011 (x|y)
                verticesE, // 100 (z)
                verticesG, // 101 (z|y)
                verticesF, // 110 (z|x)
                verticesH  // 111 (z|x|y)
        };

        for (GaiaLasPoint vertex : this.getLasPoints()) {
            double x = vertex.getX();
            double y = vertex.getY();
            double z = vertex.getZ();

            int idx = 0;
            if (z > midZ) idx |= 4;
            if (x > midX) idx |= 2;
            if (y > midY) idx |= 1;

            bucketLists[idx].add(vertex);
        }

        /*for (GaiaLasPoint vertex : this.getLasPoints()) {
            //Vector3d position = vertex.getVec3Position();
            double x = vertex.getX();
            double y = vertex.getY();
            double z = vertex.getZ();

            if (midZ < z) {
                if (midX < x) {
                    if (midY < y) {
                        verticesH.add(vertex);
                    } else {
                        verticesF.add(vertex);
                    }
                } else {
                    if (midY < y) {
                        verticesG.add(vertex);
                    } else {
                        verticesE.add(vertex);
                    }
                }
            } else {
                if (midX < x) {
                    if (midY < y) {
                        verticesD.add(vertex);
                    } else {
                        verticesB.add(vertex);
                    }
                } else {
                    if (midY < y) {
                        verticesC.add(vertex);
                    } else {
                        verticesA.add(vertex);
                    }
                }
            }
        }*/

        GaiaBoundingBox adjustedBoxA = new GaiaBoundingBox();
        adjustedBoxA.setMinX(gaiaBoundingBox.getMinX());
        adjustedBoxA.setMinY(gaiaBoundingBox.getMinY());
        adjustedBoxA.setMinZ(gaiaBoundingBox.getMinZ());
        adjustedBoxA.setMaxX(midX);
        adjustedBoxA.setMaxY(midY);
        adjustedBoxA.setMaxZ(midZ);
        gaiaPointCloudA.setGaiaBoundingBox(adjustedBoxA);
        gaiaPointCloudA.setPointCount(gaiaPointCloudA.getLasPoints().size());
        if (!gaiaPointCloudA.getLasPoints().isEmpty()) {
            pointClouds.add(gaiaPointCloudA);
            verticesA.trimToSize();
        }

        GaiaBoundingBox adjustedBoxB = new GaiaBoundingBox();
        adjustedBoxB.setMinX(midX);
        adjustedBoxB.setMinY(gaiaBoundingBox.getMinY());
        adjustedBoxB.setMinZ(gaiaBoundingBox.getMinZ());
        adjustedBoxB.setMaxX(gaiaBoundingBox.getMaxX());
        adjustedBoxB.setMaxY(midY);
        adjustedBoxB.setMaxZ(midZ);
        gaiaPointCloudB.setGaiaBoundingBox(adjustedBoxB);
        gaiaPointCloudB.setPointCount(gaiaPointCloudB.getLasPoints().size());
        if (!gaiaPointCloudB.getLasPoints().isEmpty()) {
            pointClouds.add(gaiaPointCloudB);
            verticesB.trimToSize();
        }

        GaiaBoundingBox adjustedBoxC = new GaiaBoundingBox();
        adjustedBoxC.setMinX(gaiaBoundingBox.getMinX());
        adjustedBoxC.setMinY(midY);
        adjustedBoxC.setMinZ(gaiaBoundingBox.getMinZ());
        adjustedBoxC.setMaxX(midX);
        adjustedBoxC.setMaxY(gaiaBoundingBox.getMaxY());
        adjustedBoxC.setMaxZ(midZ);
        gaiaPointCloudC.setGaiaBoundingBox(adjustedBoxC);
        gaiaPointCloudC.setPointCount(gaiaPointCloudC.getLasPoints().size());
        if (!gaiaPointCloudC.getLasPoints().isEmpty()) {
            pointClouds.add(gaiaPointCloudC);
            verticesC.trimToSize();
        }

        GaiaBoundingBox adjustedBoxD = new GaiaBoundingBox();
        adjustedBoxD.setMinX(midX);
        adjustedBoxD.setMinY(midY);
        adjustedBoxD.setMinZ(gaiaBoundingBox.getMinZ());
        adjustedBoxD.setMaxX(gaiaBoundingBox.getMaxX());
        adjustedBoxD.setMaxY(gaiaBoundingBox.getMaxY());
        adjustedBoxD.setMaxZ(midZ);
        gaiaPointCloudD.setGaiaBoundingBox(adjustedBoxD);
        gaiaPointCloudD.setPointCount(gaiaPointCloudD.getLasPoints().size());
        if (!gaiaPointCloudD.getLasPoints().isEmpty()) {
            pointClouds.add(gaiaPointCloudD);
            verticesD.trimToSize();
        }

        GaiaBoundingBox adjustedBoxE = new GaiaBoundingBox();
        adjustedBoxE.setMinX(gaiaBoundingBox.getMinX());
        adjustedBoxE.setMinY(gaiaBoundingBox.getMinY());
        adjustedBoxE.setMinZ(midZ);
        adjustedBoxE.setMaxX(midX);
        adjustedBoxE.setMaxY(midY);
        adjustedBoxE.setMaxZ(gaiaBoundingBox.getMaxZ());
        gaiaPointCloudE.setGaiaBoundingBox(adjustedBoxE);
        gaiaPointCloudE.setPointCount(gaiaPointCloudE.getLasPoints().size());
        if (!gaiaPointCloudE.getLasPoints().isEmpty()) {
            pointClouds.add(gaiaPointCloudE);
            verticesE.trimToSize();
        }

        GaiaBoundingBox adjustedBoxF = new GaiaBoundingBox();
        adjustedBoxF.setMinX(midX);
        adjustedBoxF.setMinY(gaiaBoundingBox.getMinY());
        adjustedBoxF.setMinZ(midZ);
        adjustedBoxF.setMaxX(gaiaBoundingBox.getMaxX());
        adjustedBoxF.setMaxY(midY);
        adjustedBoxF.setMaxZ(gaiaBoundingBox.getMaxZ());
        gaiaPointCloudF.setGaiaBoundingBox(adjustedBoxF);
        gaiaPointCloudF.setPointCount(gaiaPointCloudF.getLasPoints().size());
        if (!gaiaPointCloudF.getLasPoints().isEmpty()) {
            pointClouds.add(gaiaPointCloudF);
            verticesF.trimToSize();
        }

        GaiaBoundingBox adjustedBoxG = new GaiaBoundingBox();
        adjustedBoxG.setMinX(gaiaBoundingBox.getMinX());
        adjustedBoxG.setMinY(midY);
        adjustedBoxG.setMinZ(midZ);
        adjustedBoxG.setMaxX(midX);
        adjustedBoxG.setMaxY(gaiaBoundingBox.getMaxY());
        adjustedBoxG.setMaxZ(gaiaBoundingBox.getMaxZ());
        gaiaPointCloudG.setGaiaBoundingBox(adjustedBoxG);
        gaiaPointCloudG.setPointCount(gaiaPointCloudG.getLasPoints().size());
        if (!gaiaPointCloudG.getLasPoints().isEmpty()) {
            pointClouds.add(gaiaPointCloudG);
            verticesG.trimToSize();
        }

        GaiaBoundingBox adjustedBoxH = new GaiaBoundingBox();
        adjustedBoxH.setMinX(midX);
        adjustedBoxH.setMinY(midY);
        adjustedBoxH.setMinZ(midZ);
        adjustedBoxH.setMaxX(gaiaBoundingBox.getMaxX());
        adjustedBoxH.setMaxY(gaiaBoundingBox.getMaxY());
        adjustedBoxH.setMaxZ(gaiaBoundingBox.getMaxZ());
        gaiaPointCloudH.setGaiaBoundingBox(adjustedBoxH);
        gaiaPointCloudH.setPointCount(gaiaPointCloudH.getLasPoints().size());
        if (!gaiaPointCloudH.getLasPoints().isEmpty()) {
            pointClouds.add(gaiaPointCloudH);
            verticesH.trimToSize();
        }
        return pointClouds;
    }

    public List<GaiaPointCloud> divideChunkSize(int chunkSize) {
        List<GaiaPointCloud> pointClouds = new ArrayList<>();

        GaiaPointCloud chunkPointCloud = new GaiaPointCloud();
        chunkPointCloud.setCode(code);
        chunkPointCloud.setOriginalPath(originalPath);
        chunkPointCloud.setGaiaBoundingBox(gaiaBoundingBox);
        chunkPointCloud.setLimitPointCount(limitPointCount);

        GaiaPointCloud remainderPointCloud = new GaiaPointCloud();
        remainderPointCloud.setCode(code);
        remainderPointCloud.setOriginalPath(originalPath);
        remainderPointCloud.setGaiaBoundingBox(gaiaBoundingBox);

        if (lasPoints.size() > chunkSize) {
            chunkPointCloud.setLasPoints(new ArrayList<>(lasPoints.subList(0, chunkSize)));
            remainderPointCloud.setLasPoints(new ArrayList<>(lasPoints.subList(chunkSize, lasPoints.size())));
        } else {
            chunkPointCloud.setLasPoints(new ArrayList<>(lasPoints.subList(0, lasPoints.size())));
        }

        chunkPointCloud.setPointCount(chunkPointCloud.getLasPoints().size());
        remainderPointCloud.setPointCount(remainderPointCloud.getLasPoints().size());
        pointClouds.add(chunkPointCloud);
        pointClouds.add(remainderPointCloud);
        return pointClouds;
    }

    public void addChild(GaiaPointCloud child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
    }

    public void combine(GaiaPointCloud other) {
        if (other == null || other.getLasPoints() == null) {
            return;
        }
        if (this.lasPoints == null) {
            this.lasPoints = new ArrayList<>();
        }
        this.lasPoints.addAll(other.getLasPoints());
        this.pointCount = this.lasPoints.size();
        GaiaBoundingBox otherBox = other.getGaiaBoundingBox();
        this.gaiaBoundingBox.addBoundingBox(otherBox);
    }
}
