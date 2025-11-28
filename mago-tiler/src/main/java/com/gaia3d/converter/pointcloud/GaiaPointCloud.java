package com.gaia3d.converter.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.hsqldb.lib.FileUtil;
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
    private static final int BYTE_SIZE = 32; // Size of GaiaLasPoint in bytes
    private String code = "A";
    private Path originalPath;
    private GaiaBoundingBox gaiaBoundingBox = new GaiaBoundingBox();
    private List<GaiaLasPoint> lasPoints = new ArrayList<>();
    private long pointCount = 0;
    private File minimizedFile = null;

    public void clearPoints() {
        if (lasPoints != null) {
            lasPoints.clear();
            lasPoints = null;
        }
    }

    public long getPointCount() {
        if (pointCount == 0 && lasPoints != null) {
            pointCount = lasPoints.size();
        }
        return pointCount;
    }

    public void setLasPoints(List<GaiaLasPoint> lasPoints) {
        this.lasPoints = lasPoints;
        this.pointCount = lasPoints.size();
    }

    public void minimize(File minimizedFile) {
        this.minimizedFile = minimizedFile;
        this.pointCount = lasPoints.size();
        this.computeBoundingBox();

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(minimizedFile))) {
            List<GaiaLasPoint> points = this.getLasPoints();
            for (GaiaLasPoint point : points) {
                byte[] pointBytes = point.toBytes();
                bos.write(pointBytes);
            }
            this.lasPoints = null;
        } catch (IOException e) {
            log.error("Failed to minimize point cloud to file: {}", minimizedFile.getAbsolutePath(), e);
        }
    }

    public void maximize(boolean deleteAfterMaximize) {
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
        Vector3d volume = gaiaBoundingBox.getVolume();
        double offsetX = volume.x;
        double offsetY = volume.y;
        double offsetZ = volume.z;

        double halfX = offsetX / 2;
        double halfY = offsetY / 2;
        double halfZ = offsetZ / 2;

        if (halfX > offsetY) {
            return distributeHalf(true);
        } else if (halfY > offsetX) {
            return distributeHalf(false);
        } else if (offsetZ < offsetX || offsetZ < offsetY) {
            return distributeQuad();
        } else {
            return distributeOct();
        }
    }

    // Quarter based on the bounding box
    public List<GaiaPointCloud> distributeHalf(boolean isX) {
        List<GaiaPointCloud> pointClouds = new ArrayList<>();

        GaiaPointCloud gaiaPointCloudA = new GaiaPointCloud();
        gaiaPointCloudA.setCode("A");
        gaiaPointCloudA.setOriginalPath(originalPath);
        List<GaiaLasPoint> verticesA = gaiaPointCloudA.getLasPoints();

        GaiaPointCloud gaiaPointCloudB = new GaiaPointCloud();
        gaiaPointCloudB.setCode("B");
        gaiaPointCloudB.setOriginalPath(originalPath);
        List<GaiaLasPoint> verticesB = gaiaPointCloudA.getLasPoints();

        double minX = gaiaBoundingBox.getMinX();
        double minY = gaiaBoundingBox.getMinY();
        double maxX = gaiaBoundingBox.getMaxX();
        double maxY = gaiaBoundingBox.getMaxY();
        double midX = (minX + maxX) / 2;
        double midY = (minY + maxY) / 2;

        for (GaiaLasPoint vertex : this.getLasPoints()) {
            Vector3d center = vertex.getVec3Position();
            if (isX) {
                if (midX < center.x()) {
                    verticesB.add(vertex);
                } else {
                    verticesA.add(vertex);
                }
            } else {
                if (midY < center.y()) {
                    verticesB.add(vertex);
                } else {
                    verticesA.add(vertex);
                }
            }
        }

        pointClouds.add(gaiaPointCloudA);
        gaiaPointCloudA.computeBoundingBox();
        gaiaPointCloudA.setPointCount(gaiaPointCloudA.getLasPoints().size());

        pointClouds.add(gaiaPointCloudB);
        gaiaPointCloudB.computeBoundingBox();
        gaiaPointCloudB.setPointCount(gaiaPointCloudB.getLasPoints().size());
        return pointClouds;
    }

    // Quarter based on the bounding box
    public List<GaiaPointCloud> distributeQuad() {
        List<GaiaPointCloud> pointClouds = new ArrayList<>();

        GaiaPointCloud gaiaPointCloudA = new GaiaPointCloud();
        gaiaPointCloudA.setCode("A");
        gaiaPointCloudA.setOriginalPath(originalPath);
        List<GaiaLasPoint> verticesA = gaiaPointCloudA.getLasPoints();

        GaiaPointCloud gaiaPointCloudB = new GaiaPointCloud();
        gaiaPointCloudB.setCode("B");
        gaiaPointCloudB.setOriginalPath(originalPath);
        List<GaiaLasPoint> verticesB = gaiaPointCloudB.getLasPoints();

        GaiaPointCloud gaiaPointCloudC = new GaiaPointCloud();
        gaiaPointCloudC.setCode("C");
        gaiaPointCloudC.setOriginalPath(originalPath);
        List<GaiaLasPoint> verticesC = gaiaPointCloudC.getLasPoints();

        GaiaPointCloud gaiaPointCloudD = new GaiaPointCloud();
        gaiaPointCloudD.setCode("D");
        gaiaPointCloudD.setOriginalPath(originalPath);
        List<GaiaLasPoint> verticesD = gaiaPointCloudD.getLasPoints();

        double minX = gaiaBoundingBox.getMinX();
        double minY = gaiaBoundingBox.getMinY();
        double maxX = gaiaBoundingBox.getMaxX();
        double maxY = gaiaBoundingBox.getMaxY();
        double midX = (minX + maxX) / 2;
        double midY = (minY + maxY) / 2;

        for (GaiaLasPoint vertex : this.getLasPoints()) {
            Vector3d center = vertex.getVec3Position();
            if (midX < center.x()) {
                if (midY < center.y()) {
                    verticesC.add(vertex);
                } else {
                    verticesB.add(vertex);
                }
            } else {
                if (midY < center.y()) {
                    verticesD.add(vertex);
                } else {
                    verticesA.add(vertex);
                }
            }
        }

        pointClouds.add(gaiaPointCloudA);
        gaiaPointCloudA.computeBoundingBox();
        gaiaPointCloudA.setPointCount(gaiaPointCloudA.getLasPoints().size());

        pointClouds.add(gaiaPointCloudB);
        gaiaPointCloudB.computeBoundingBox();
        gaiaPointCloudB.setPointCount(gaiaPointCloudB.getLasPoints().size());

        pointClouds.add(gaiaPointCloudC);
        gaiaPointCloudC.computeBoundingBox();
        gaiaPointCloudC.setPointCount(gaiaPointCloudC.getLasPoints().size());

        pointClouds.add(gaiaPointCloudD);
        gaiaPointCloudD.computeBoundingBox();
        gaiaPointCloudD.setPointCount(gaiaPointCloudD.getLasPoints().size());
        return pointClouds;
    }

    // Octree based on the bounding box
    public List<GaiaPointCloud> distributeOct() {
        List<GaiaPointCloud> pointClouds = new ArrayList<>();

        GaiaPointCloud gaiaPointCloudA = new GaiaPointCloud();
        gaiaPointCloudA.setCode("A");
        gaiaPointCloudA.setOriginalPath(originalPath);
        List<GaiaLasPoint> verticesA = gaiaPointCloudA.getLasPoints();

        GaiaPointCloud gaiaPointCloudB = new GaiaPointCloud();
        gaiaPointCloudB.setCode("B");
        gaiaPointCloudB.setOriginalPath(originalPath);
        List<GaiaLasPoint> verticesB = gaiaPointCloudB.getLasPoints();

        GaiaPointCloud gaiaPointCloudC = new GaiaPointCloud();
        gaiaPointCloudC.setCode("C");
        gaiaPointCloudC.setOriginalPath(originalPath);
        List<GaiaLasPoint> verticesC = gaiaPointCloudC.getLasPoints();

        GaiaPointCloud gaiaPointCloudD = new GaiaPointCloud();
        gaiaPointCloudD.setCode("D");
        gaiaPointCloudD.setOriginalPath(originalPath);
        List<GaiaLasPoint> verticesD = gaiaPointCloudD.getLasPoints();

        GaiaPointCloud gaiaPointCloudE = new GaiaPointCloud();
        gaiaPointCloudE.setCode("E");
        gaiaPointCloudE.setOriginalPath(originalPath);
        List<GaiaLasPoint> verticesE = gaiaPointCloudE.getLasPoints();

        GaiaPointCloud gaiaPointCloudF = new GaiaPointCloud();
        gaiaPointCloudF.setCode("F");
        gaiaPointCloudF.setOriginalPath(originalPath);
        List<GaiaLasPoint> verticesF = gaiaPointCloudF.getLasPoints();

        GaiaPointCloud gaiaPointCloudG = new GaiaPointCloud();
        gaiaPointCloudG.setCode("G");
        gaiaPointCloudG.setOriginalPath(originalPath);
        List<GaiaLasPoint> verticesG = gaiaPointCloudG.getLasPoints();

        GaiaPointCloud gaiaPointCloudH = new GaiaPointCloud();
        gaiaPointCloudH.setCode("H");
        gaiaPointCloudH.setOriginalPath(originalPath);
        List<GaiaLasPoint> verticesH = gaiaPointCloudH.getLasPoints();

        double minX = gaiaBoundingBox.getMinX();
        double minY = gaiaBoundingBox.getMinY();
        double minZ = gaiaBoundingBox.getMinZ();
        double maxX = gaiaBoundingBox.getMaxX();
        double maxY = gaiaBoundingBox.getMaxY();
        double maxZ = gaiaBoundingBox.getMaxZ();

        double midX = (minX + maxX) / 2;
        double midY = (minY + maxY) / 2;
        double midZ = (minZ + maxZ) / 2;

        for (GaiaLasPoint vertex : this.getLasPoints()) {
            Vector3d center = vertex.getVec3Position();
            if (midZ < center.z()) {
                if (midX < center.x()) {
                    if (midY < center.y()) {
                        verticesC.add(vertex);
                    } else {
                        verticesB.add(vertex);
                    }
                } else {
                    if (midY < center.y()) {
                        verticesD.add(vertex);
                    } else {
                        verticesA.add(vertex);
                    }
                }
            } else {
                if (midX < center.x()) {
                    if (midY < center.y()) {
                        verticesG.add(vertex);
                    } else {
                        verticesF.add(vertex);
                    }
                } else {
                    if (midY < center.y()) {
                        verticesH.add(vertex);
                    } else {
                        verticesE.add(vertex);
                    }
                }
            }
        }

        pointClouds.add(gaiaPointCloudA);
        gaiaPointCloudA.computeBoundingBox();
        gaiaPointCloudA.setPointCount(gaiaPointCloudA.getLasPoints().size());

        pointClouds.add(gaiaPointCloudB);
        gaiaPointCloudB.computeBoundingBox();
        gaiaPointCloudB.setPointCount(gaiaPointCloudB.getLasPoints().size());

        pointClouds.add(gaiaPointCloudC);
        gaiaPointCloudC.computeBoundingBox();
        gaiaPointCloudC.setPointCount(gaiaPointCloudC.getLasPoints().size());

        pointClouds.add(gaiaPointCloudD);
        gaiaPointCloudD.computeBoundingBox();
        gaiaPointCloudD.setPointCount(gaiaPointCloudD.getLasPoints().size());

        pointClouds.add(gaiaPointCloudE);
        gaiaPointCloudE.computeBoundingBox();
        gaiaPointCloudE.setPointCount(gaiaPointCloudE.getLasPoints().size());

        pointClouds.add(gaiaPointCloudF);
        gaiaPointCloudF.computeBoundingBox();
        gaiaPointCloudF.setPointCount(gaiaPointCloudF.getLasPoints().size());

        pointClouds.add(gaiaPointCloudG);
        gaiaPointCloudG.computeBoundingBox();
        gaiaPointCloudG.setPointCount(gaiaPointCloudG.getLasPoints().size());

        pointClouds.add(gaiaPointCloudH);
        gaiaPointCloudH.computeBoundingBox();
        gaiaPointCloudH.setPointCount(gaiaPointCloudH.getLasPoints().size());
        return pointClouds;
    }

    public List<GaiaPointCloud> divideChunkSize(int chunkSize) {
        List<GaiaPointCloud> pointClouds = new ArrayList<>();

        GaiaPointCloud chunkPointCloud = new GaiaPointCloud();
        chunkPointCloud.setOriginalPath(originalPath);
        chunkPointCloud.setGaiaBoundingBox(gaiaBoundingBox);

        GaiaPointCloud remainderPointCloud = new GaiaPointCloud();
        remainderPointCloud.setOriginalPath(originalPath);
        remainderPointCloud.setGaiaBoundingBox(gaiaBoundingBox);

        if (lasPoints.size() > chunkSize) {
            chunkPointCloud.setLasPoints(new ArrayList<>(lasPoints.subList(0, chunkSize)));
            remainderPointCloud.setLasPoints(new ArrayList<>(lasPoints.subList(chunkSize, lasPoints.size())));
        } else {
            chunkPointCloud.setLasPoints(new ArrayList<>(lasPoints.subList(0, lasPoints.size())));
        }

        chunkPointCloud.computeBoundingBox();
        remainderPointCloud.computeBoundingBox();
        chunkPointCloud.setPointCount(chunkPointCloud.getLasPoints().size());
        remainderPointCloud.setPointCount(remainderPointCloud.getLasPoints().size());
        pointClouds.add(chunkPointCloud);
        pointClouds.add(remainderPointCloud);
        return pointClouds;
    }
}
