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
            this.lasPoints.clear();
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
        /*double offsetX = volume.x;
        double offsetY = volume.y;
        double offsetZ = volume.z;
        double halfX = offsetX / 2;
        double halfY = offsetY / 2;
        double halfZ = offsetZ / 2;
        if (halfX > offsetY) {
            return distributeHalf(true);
        } else if (halfY > offsetX) {
            return distributeHalf(false);
        } *//*else if (offsetZ < offsetX || offsetZ < offsetY) {
            return distributeQuad();
        }*//* else if (offsetZ > 100.0) {
            return distributeOct();
        } else {
            return distributeQuad();
        }*/
        return distributeOct();
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
        //gaiaPointCloudA.computeBoundingBox();
        gaiaPointCloudA.setPointCount(gaiaPointCloudA.getLasPoints().size());

        pointClouds.add(gaiaPointCloudB);
        //gaiaPointCloudB.computeBoundingBox();
        gaiaPointCloudB.setPointCount(gaiaPointCloudB.getLasPoints().size());

        if (isX) {
            gaiaPointCloudA.setCode("L");
            gaiaPointCloudB.setCode("R");

            GaiaBoundingBox adjustedBoxA = new GaiaBoundingBox();
            adjustedBoxA.setMinX(minX);
            adjustedBoxA.setMinY(minY);
            adjustedBoxA.setMinZ(gaiaBoundingBox.getMinZ());
            adjustedBoxA.setMaxX(midX);
            adjustedBoxA.setMaxY(maxY);
            adjustedBoxA.setMaxZ(gaiaBoundingBox.getMaxZ());
            gaiaPointCloudA.setGaiaBoundingBox(adjustedBoxA);

            GaiaBoundingBox adjustedBoxB = new GaiaBoundingBox();
            adjustedBoxB.setMinX(midX);
            adjustedBoxB.setMinY(minY);
            adjustedBoxB.setMinZ(gaiaBoundingBox.getMinZ());
            adjustedBoxB.setMaxX(maxX);
            adjustedBoxB.setMaxY(maxY);
            adjustedBoxB.setMaxZ(gaiaBoundingBox.getMaxZ());
            gaiaPointCloudB.setGaiaBoundingBox(adjustedBoxB);


        } else {
            gaiaPointCloudA.setCode("B");
            gaiaPointCloudB.setCode("F");

            GaiaBoundingBox adjustedBoxA = new GaiaBoundingBox();
            adjustedBoxA.setMinX(minX);
            adjustedBoxA.setMinY(minY);
            adjustedBoxA.setMinZ(gaiaBoundingBox.getMinZ());
            adjustedBoxA.setMaxX(maxX);
            adjustedBoxA.setMaxY(midY);
            adjustedBoxA.setMaxZ(gaiaBoundingBox.getMaxZ());
            gaiaPointCloudA.setGaiaBoundingBox(adjustedBoxA);

            GaiaBoundingBox adjustedBoxB = new GaiaBoundingBox();
            adjustedBoxB.setMinX(minX);
            adjustedBoxB.setMinY(midY);
            adjustedBoxB.setMinZ(gaiaBoundingBox.getMinZ());
            adjustedBoxB.setMaxX(maxX);
            adjustedBoxB.setMaxY(maxY);
            adjustedBoxB.setMaxZ(gaiaBoundingBox.getMaxZ());
            gaiaPointCloudB.setGaiaBoundingBox(adjustedBoxB);
        }
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
        //gaiaPointCloudA.computeBoundingBox();
        GaiaBoundingBox adjustedBoxA = new GaiaBoundingBox();
        adjustedBoxA.setMinX(minX);
        adjustedBoxA.setMinY(minY);
        adjustedBoxA.setMinZ(gaiaBoundingBox.getMinZ());
        adjustedBoxA.setMaxX(midX);
        adjustedBoxA.setMaxY(midY);
        adjustedBoxA.setMaxZ(gaiaBoundingBox.getMaxZ());
        gaiaPointCloudA.setGaiaBoundingBox(adjustedBoxA);
        gaiaPointCloudA.setPointCount(gaiaPointCloudA.getLasPoints().size());

        pointClouds.add(gaiaPointCloudB);
        //gaiaPointCloudB.computeBoundingBox();
        GaiaBoundingBox adjustedBoxB = new GaiaBoundingBox();
        adjustedBoxB.setMinX(midX);
        adjustedBoxB.setMinY(minY);
        adjustedBoxB.setMinZ(gaiaBoundingBox.getMinZ());
        adjustedBoxB.setMaxX(maxX);
        adjustedBoxB.setMaxY(midY);
        adjustedBoxB.setMaxZ(gaiaBoundingBox.getMaxZ());
        gaiaPointCloudB.setGaiaBoundingBox(adjustedBoxB);
        gaiaPointCloudB.setPointCount(gaiaPointCloudB.getLasPoints().size());

        pointClouds.add(gaiaPointCloudC);
        //gaiaPointCloudC.computeBoundingBox();
        GaiaBoundingBox adjustedBoxC = new GaiaBoundingBox();
        adjustedBoxC.setMinX(minX);
        adjustedBoxC.setMinY(midY);
        adjustedBoxC.setMinZ(gaiaBoundingBox.getMinZ());
        adjustedBoxC.setMaxX(midX);
        adjustedBoxC.setMaxY(maxY);
        adjustedBoxC.setMaxZ(gaiaBoundingBox.getMaxZ());
        gaiaPointCloudC.setGaiaBoundingBox(adjustedBoxC);
        gaiaPointCloudC.setPointCount(gaiaPointCloudC.getLasPoints().size());

        pointClouds.add(gaiaPointCloudD);
        //gaiaPointCloudD.computeBoundingBox();
        GaiaBoundingBox adjustedBoxD = new GaiaBoundingBox();
        adjustedBoxD.setMinX(midX);
        adjustedBoxD.setMinY(midY);
        adjustedBoxD.setMinZ(gaiaBoundingBox.getMinZ());
        adjustedBoxD.setMaxX(maxX);
        adjustedBoxD.setMaxY(maxY);
        adjustedBoxD.setMaxZ(gaiaBoundingBox.getMaxZ());
        gaiaPointCloudD.setGaiaBoundingBox(adjustedBoxD);
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
            Vector3d position = vertex.getVec3Position();

            /*if (midX < position.x()) {
                if (midY < position.y()) {
                    verticesD.add(vertex);
                } else {
                    verticesB.add(vertex);
                }
            } else {
                if (midY < position.y()) {
                    verticesC.add(vertex);
                } else {
                    verticesA.add(vertex);
                }
            }*/

            if (midZ < position.z()) {
                if (midX < position.x()) {
                    if (midY < position.y()) {
                        verticesH.add(vertex);
                    } else {
                        verticesF.add(vertex);
                    }
                } else {
                    if (midY < position.y()) {
                        verticesG.add(vertex);
                    } else {
                        verticesE.add(vertex);
                    }
                }
            } else {
                if (midX < position.x()) {
                    if (midY < position.y()) {
                        verticesD.add(vertex);
                    } else {
                        verticesB.add(vertex);
                    }
                } else {
                    if (midY < position.y()) {
                        verticesC.add(vertex);
                    } else {
                        verticesA.add(vertex);
                    }
                }
            }




            /*if (position.z() < midZ) {
                if (midX < position.x()) {
                    if (midY < position.y()) {
                        verticesC.add(vertex);
                    } else {
                        verticesB.add(vertex);
                    }
                } else {
                    if (midY < position.y()) {
                        verticesD.add(vertex);
                    } else {
                        verticesA.add(vertex);
                    }
                }
            } else {
                if (midX < position.x()) {
                    if (midY < position.y()) {
                        verticesG.add(vertex);
                    } else {
                        verticesF.add(vertex);
                    }
                } else {
                    if (midY < position.y()) {
                        verticesH.add(vertex);
                    } else {
                        verticesE.add(vertex);
                    }
                }
            }*/
        }

        //gaiaPointCloudA.computeBoundingBox();
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
        }

        //gaiaPointCloudB.computeBoundingBox();
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
        }

        //gaiaPointCloudC.computeBoundingBox();
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
        }

        //gaiaPointCloudD.computeBoundingBox();
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
        }

        //gaiaPointCloudE.computeBoundingBox();
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
        }

        //gaiaPointCloudF.computeBoundingBox();
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
        }

        //gaiaPointCloudG.computeBoundingBox();
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
        }

        //gaiaPointCloudH.computeBoundingBox();
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
        }
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

        //chunkPointCloud.computeBoundingBox();
        //remainderPointCloud.computeBoundingBox();
        chunkPointCloud.setPointCount(chunkPointCloud.getLasPoints().size());
        remainderPointCloud.setPointCount(remainderPointCloud.getLasPoints().size());
        pointClouds.add(chunkPointCloud);
        pointClouds.add(remainderPointCloud);
        return pointClouds;
    }
}
