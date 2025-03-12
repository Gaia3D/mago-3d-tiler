package com.gaia3d.basic.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaVertex;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
public class GaiaPointCloud implements Serializable {
    GaiaPointCloudTemp pointCloudTemp = null;
    private String code = "A";
    private Path originalPath;
    private GaiaBoundingBox gaiaBoundingBox = new GaiaBoundingBox();
    private List<GaiaVertex> vertices = new ArrayList<>();
    private int vertexCount = 0;
    private GaiaAttribute gaiaAttribute = new GaiaAttribute();
    private boolean isMinimized = false;
    private File minimizedFile = null;
    private Vector3d quantizedVolumeScale = null;
    private Vector3d quantizedVolumeOffset = null;

    public void minimizeTemp() {
        vertices = null;
        isMinimized = true;
    }

    public void minimize(File minimizedFile) {
        if (this.isMinimized) {
            log.warn("[WARN] The point cloud is already minimized.");
            return;
        }

        Vector3d quantizationOffset = gaiaBoundingBox.getMinPosition();
        Vector3d quantizationScale = gaiaBoundingBox.getVolume();
        // correct the scale if it is zero
        if (quantizationScale.x == 0) {
            quantizationScale.x = 1;
        }
        if (quantizationScale.y == 0) {
            quantizationScale.y = 1;
        }
        if (quantizationScale.z == 0) {
            quantizationScale.z = 1;
        }

        this.quantizedVolumeScale = quantizationScale;
        this.quantizedVolumeOffset = quantizationOffset;
        this.pointCloudTemp = new GaiaPointCloudTemp(minimizedFile);
        double[] volumeOffset = pointCloudTemp.getQuantizedVolumeOffset();
        volumeOffset[0] = quantizationOffset.x;
        volumeOffset[1] = quantizationOffset.y;
        volumeOffset[2] = quantizationOffset.z;
        double[] volumeScale = pointCloudTemp.getQuantizedVolumeScale();
        volumeScale[0] = quantizationScale.x;
        volumeScale[1] = quantizationScale.y;
        volumeScale[2] = quantizationScale.z;
        pointCloudTemp.writeHeader();
        this.vertexCount = vertices.size();
        pointCloudTemp.writePositionsFast(vertices);
        this.vertices.clear();
        try {
            pointCloudTemp.getOutputStream().flush();
            pointCloudTemp.getOutputStream().close();
        } catch (IOException e) {
            log.error("[Error][minimize] : Failed to minimize the point cloud.", e);
            throw new RuntimeException(e);
        }

        // Minimize the point cloud
        this.vertices = null;
        this.isMinimized = true;
        this.minimizedFile = minimizedFile;
    }

    public void maximizeTemp() {
        if (!isMinimized) {
            log.warn("[WARN] The point cloud is already maximized.");
            return;
        }

        pointCloudTemp.readHeader();

        List<GaiaVertex> vertices = pointCloudTemp.readTemp();
        vertexCount = vertices.size();
        try {
            pointCloudTemp.getInputStream().close();
        } catch (IOException e) {
            log.error("[Error][maximize] : Failed to maximize the point cloud.", e);
        }
        this.vertices = vertices;
    }

    public void maximizeTempOld() {
        if (!isMinimized) {
            log.warn("[WARN] The point cloud is already maximized.");
            return;
        }
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(minimizedFile)))) {
            List<GaiaVertex> vertices = (List<GaiaVertex>) objectInputStream.readObject();
            this.vertices = vertices;

            vertices.forEach(vertex -> {
                short[] quantizedPosition = vertex.getQuantizedPosition();
                int xQuantizedPositionInt = toDoubleFromUnsignedShort(quantizedPosition[0]);
                int yQuantizedPositionInt = toDoubleFromUnsignedShort(quantizedPosition[1]);
                int zQuantizedPositionInt = toDoubleFromUnsignedShort(quantizedPosition[2]);

                double xQuantizedPosition = xQuantizedPositionInt / 65535.0;
                double yQuantizedPosition = yQuantizedPositionInt / 65535.0;
                double zQuantizedPosition = zQuantizedPositionInt / 65535.0;

                Vector3d position = new Vector3d(quantizedVolumeScale.x * xQuantizedPosition + quantizedVolumeOffset.x, quantizedVolumeScale.y * yQuantizedPosition + quantizedVolumeOffset.y, quantizedVolumeScale.z * zQuantizedPosition + quantizedVolumeOffset.z);
                vertex.setPosition(position);
                vertex.setQuantizedPosition(null);
            });
        } catch (Exception e) {
            log.error("[Error][maximize] : Failed to maximize the point cloud.", e);
        }
    }

    public void maximize() {
        maximizeTemp();

        // Maximize the point cloud
        isMinimized = false;
        this.minimizedFile = null;
        this.quantizedVolumeScale = null;
        this.quantizedVolumeOffset = null;
    }


    public List<GaiaPointCloud> distribute() {
        double minX = gaiaBoundingBox.getMinX();
        double minY = gaiaBoundingBox.getMinY();
        double minZ = gaiaBoundingBox.getMinZ();
        double maxX = gaiaBoundingBox.getMaxX();
        double maxY = gaiaBoundingBox.getMaxY();
        double maxZ = gaiaBoundingBox.getMaxZ();

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

        GaiaBoundingBox gaiaBoundingBoxA = new GaiaBoundingBox();
        GaiaPointCloud gaiaPointCloudA = new GaiaPointCloud();
        gaiaPointCloudA.setCode("A");
        gaiaPointCloudA.setOriginalPath(originalPath);
        gaiaPointCloudA.setGaiaBoundingBox(gaiaBoundingBoxA);
        List<GaiaVertex> verticesA = gaiaPointCloudA.getVertices();

        GaiaBoundingBox gaiaBoundingBoxB = new GaiaBoundingBox();
        GaiaPointCloud gaiaPointCloudB = new GaiaPointCloud();
        gaiaPointCloudB.setCode("B");
        gaiaPointCloudB.setOriginalPath(originalPath);
        gaiaPointCloudB.setGaiaBoundingBox(gaiaBoundingBoxB);
        List<GaiaVertex> verticesB = gaiaPointCloudB.getVertices();

        double minX = gaiaBoundingBox.getMinX();
        double minY = gaiaBoundingBox.getMinY();
        double maxX = gaiaBoundingBox.getMaxX();
        double maxY = gaiaBoundingBox.getMaxY();
        double midX = (minX + maxX) / 2;
        double midY = (minY + maxY) / 2;

        for (GaiaVertex vertex : this.vertices) {
            Vector3d center = vertex.getPosition();
            if (isX) {
                if (midX < center.x()) {
                    verticesB.add(vertex);
                    gaiaBoundingBoxB.addPoint(center);
                } else {
                    verticesA.add(vertex);
                    gaiaBoundingBoxA.addPoint(center);
                }
            } else {
                if (midY < center.y()) {
                    verticesB.add(vertex);
                    gaiaBoundingBoxB.addPoint(center);
                } else {
                    verticesA.add(vertex);
                    gaiaBoundingBoxA.addPoint(center);
                }
            }
        }

        pointClouds.add(gaiaPointCloudA);
        pointClouds.add(gaiaPointCloudB);
        return pointClouds;
    }

    // Quarter based on the bounding box
    public List<GaiaPointCloud> distributeQuad() {
        List<GaiaPointCloud> pointClouds = new ArrayList<>();

        GaiaBoundingBox gaiaBoundingBoxA = new GaiaBoundingBox();
        GaiaPointCloud gaiaPointCloudA = new GaiaPointCloud();
        gaiaPointCloudA.setCode("A");
        gaiaPointCloudA.setOriginalPath(originalPath);
        gaiaPointCloudA.setGaiaBoundingBox(gaiaBoundingBoxA);
        List<GaiaVertex> verticesA = gaiaPointCloudA.getVertices();

        GaiaBoundingBox gaiaBoundingBoxB = new GaiaBoundingBox();
        GaiaPointCloud gaiaPointCloudB = new GaiaPointCloud();
        gaiaPointCloudB.setCode("B");
        gaiaPointCloudB.setOriginalPath(originalPath);
        gaiaPointCloudB.setGaiaBoundingBox(gaiaBoundingBoxB);
        List<GaiaVertex> verticesB = gaiaPointCloudB.getVertices();

        GaiaBoundingBox gaiaBoundingBoxC = new GaiaBoundingBox();
        GaiaPointCloud gaiaPointCloudC = new GaiaPointCloud();
        gaiaPointCloudC.setCode("C");
        gaiaPointCloudC.setOriginalPath(originalPath);
        gaiaPointCloudC.setGaiaBoundingBox(gaiaBoundingBoxC);
        List<GaiaVertex> verticesC = gaiaPointCloudC.getVertices();

        GaiaBoundingBox gaiaBoundingBoxD = new GaiaBoundingBox();
        GaiaPointCloud gaiaPointCloudD = new GaiaPointCloud();
        gaiaPointCloudD.setCode("D");
        gaiaPointCloudD.setOriginalPath(originalPath);
        gaiaPointCloudD.setGaiaBoundingBox(gaiaBoundingBoxD);
        List<GaiaVertex> verticesD = gaiaPointCloudD.getVertices();

        double minX = gaiaBoundingBox.getMinX();
        double minY = gaiaBoundingBox.getMinY();
        double maxX = gaiaBoundingBox.getMaxX();
        double maxY = gaiaBoundingBox.getMaxY();
        double midX = (minX + maxX) / 2;
        double midY = (minY + maxY) / 2;

        for (GaiaVertex vertex : this.vertices) {
            Vector3d center = vertex.getPosition();
            if (midX < center.x()) {
                if (midY < center.y()) {
                    verticesC.add(vertex);
                    gaiaBoundingBoxC.addPoint(center);
                } else {
                    verticesB.add(vertex);
                    gaiaBoundingBoxB.addPoint(center);
                }
            } else {
                if (midY < center.y()) {
                    verticesD.add(vertex);
                    gaiaBoundingBoxD.addPoint(center);
                } else {
                    verticesA.add(vertex);
                    gaiaBoundingBoxA.addPoint(center);
                }
            }
        }

        pointClouds.add(gaiaPointCloudA);
        pointClouds.add(gaiaPointCloudB);
        pointClouds.add(gaiaPointCloudC);
        pointClouds.add(gaiaPointCloudD);
        return pointClouds;
    }

    // Octree based on the bounding box
    public List<GaiaPointCloud> distributeOct() {
        List<GaiaPointCloud> pointClouds = new ArrayList<>();

        GaiaBoundingBox gaiaBoundingBoxA = new GaiaBoundingBox();
        GaiaPointCloud gaiaPointCloudA = new GaiaPointCloud();
        gaiaPointCloudA.setCode("A");
        gaiaPointCloudA.setOriginalPath(originalPath);
        gaiaPointCloudA.setGaiaBoundingBox(gaiaBoundingBoxA);
        gaiaPointCloudA.setGaiaAttribute(gaiaAttribute);
        List<GaiaVertex> verticesA = gaiaPointCloudA.getVertices();

        GaiaBoundingBox gaiaBoundingBoxB = new GaiaBoundingBox();
        GaiaPointCloud gaiaPointCloudB = new GaiaPointCloud();
        gaiaPointCloudB.setCode("B");
        gaiaPointCloudB.setOriginalPath(originalPath);
        gaiaPointCloudB.setGaiaBoundingBox(gaiaBoundingBoxB);
        gaiaPointCloudB.setGaiaAttribute(gaiaAttribute);
        List<GaiaVertex> verticesB = gaiaPointCloudB.getVertices();

        GaiaBoundingBox gaiaBoundingBoxC = new GaiaBoundingBox();
        GaiaPointCloud gaiaPointCloudC = new GaiaPointCloud();
        gaiaPointCloudC.setCode("C");
        gaiaPointCloudC.setOriginalPath(originalPath);
        gaiaPointCloudC.setGaiaBoundingBox(gaiaBoundingBoxC);
        gaiaPointCloudC.setGaiaAttribute(gaiaAttribute);
        List<GaiaVertex> verticesC = gaiaPointCloudC.getVertices();

        GaiaBoundingBox gaiaBoundingBoxD = new GaiaBoundingBox();
        GaiaPointCloud gaiaPointCloudD = new GaiaPointCloud();
        gaiaPointCloudD.setCode("D");
        gaiaPointCloudD.setOriginalPath(originalPath);
        gaiaPointCloudD.setGaiaBoundingBox(gaiaBoundingBoxD);
        gaiaPointCloudD.setGaiaAttribute(gaiaAttribute);
        List<GaiaVertex> verticesD = gaiaPointCloudD.getVertices();

        GaiaBoundingBox gaiaBoundingBoxE = new GaiaBoundingBox();
        GaiaPointCloud gaiaPointCloudE = new GaiaPointCloud();
        gaiaPointCloudE.setCode("E");
        gaiaPointCloudE.setOriginalPath(originalPath);
        gaiaPointCloudE.setGaiaBoundingBox(gaiaBoundingBoxE);
        gaiaPointCloudE.setGaiaAttribute(gaiaAttribute);
        List<GaiaVertex> verticesE = gaiaPointCloudE.getVertices();

        GaiaBoundingBox gaiaBoundingBoxF = new GaiaBoundingBox();
        GaiaPointCloud gaiaPointCloudF = new GaiaPointCloud();
        gaiaPointCloudF.setCode("F");
        gaiaPointCloudF.setOriginalPath(originalPath);
        gaiaPointCloudF.setGaiaBoundingBox(gaiaBoundingBoxF);
        gaiaPointCloudF.setGaiaAttribute(gaiaAttribute);
        List<GaiaVertex> verticesF = gaiaPointCloudF.getVertices();

        GaiaBoundingBox gaiaBoundingBoxG = new GaiaBoundingBox();
        GaiaPointCloud gaiaPointCloudG = new GaiaPointCloud();
        gaiaPointCloudG.setCode("G");
        gaiaPointCloudG.setOriginalPath(originalPath);
        gaiaPointCloudG.setGaiaBoundingBox(gaiaBoundingBoxG);
        gaiaPointCloudG.setGaiaAttribute(gaiaAttribute);
        List<GaiaVertex> verticesG = gaiaPointCloudG.getVertices();

        GaiaBoundingBox gaiaBoundingBoxH = new GaiaBoundingBox();
        GaiaPointCloud gaiaPointCloudH = new GaiaPointCloud();
        gaiaPointCloudH.setCode("H");
        gaiaPointCloudH.setOriginalPath(originalPath);
        gaiaPointCloudH.setGaiaBoundingBox(gaiaBoundingBoxH);
        gaiaPointCloudH.setGaiaAttribute(gaiaAttribute);
        List<GaiaVertex> verticesH = gaiaPointCloudH.getVertices();

        double minX = gaiaBoundingBox.getMinX();
        double minY = gaiaBoundingBox.getMinY();
        double minZ = gaiaBoundingBox.getMinZ();
        double maxX = gaiaBoundingBox.getMaxX();
        double maxY = gaiaBoundingBox.getMaxY();
        double maxZ = gaiaBoundingBox.getMaxZ();

        double midX = (minX + maxX) / 2;
        double midY = (minY + maxY) / 2;
        double midZ = (minZ + maxZ) / 2;

        for (GaiaVertex vertex : this.vertices) {
            Vector3d center = vertex.getPosition();
            if (midZ < center.z()) {
                if (midX < center.x()) {
                    if (midY < center.y()) {
                        verticesC.add(vertex);
                        gaiaBoundingBoxC.addPoint(center);
                    } else {
                        verticesB.add(vertex);
                        gaiaBoundingBoxB.addPoint(center);
                    }
                } else {
                    if (midY < center.y()) {
                        verticesD.add(vertex);
                        gaiaBoundingBoxD.addPoint(center);
                    } else {
                        verticesA.add(vertex);
                        gaiaBoundingBoxA.addPoint(center);
                    }
                }
            } else {
                if (midX < center.x()) {
                    if (midY < center.y()) {
                        verticesG.add(vertex);
                        gaiaBoundingBoxG.addPoint(center);
                    } else {
                        verticesF.add(vertex);
                        gaiaBoundingBoxF.addPoint(center);
                    }
                } else {
                    if (midY < center.y()) {
                        verticesH.add(vertex);
                        gaiaBoundingBoxH.addPoint(center);
                    } else {
                        verticesE.add(vertex);
                        gaiaBoundingBoxE.addPoint(center);
                    }
                }
            }
        }

        pointClouds.add(gaiaPointCloudA);
        pointClouds.add(gaiaPointCloudB);
        pointClouds.add(gaiaPointCloudC);
        pointClouds.add(gaiaPointCloudD);
        pointClouds.add(gaiaPointCloudE);
        pointClouds.add(gaiaPointCloudF);
        pointClouds.add(gaiaPointCloudG);
        pointClouds.add(gaiaPointCloudH);
        return pointClouds;
    }

    public List<GaiaPointCloud> divideChunkSize(int chunkSize) {
        List<GaiaPointCloud> pointClouds = new ArrayList<>();

        GaiaPointCloud chunkPointCloud = new GaiaPointCloud();
        chunkPointCloud.setOriginalPath(originalPath);
        chunkPointCloud.setGaiaBoundingBox(gaiaBoundingBox);
        chunkPointCloud.setGaiaAttribute(gaiaAttribute);

        GaiaPointCloud remainderPointCloud = new GaiaPointCloud();
        remainderPointCloud.setOriginalPath(originalPath);
        remainderPointCloud.setGaiaBoundingBox(gaiaBoundingBox);
        remainderPointCloud.setGaiaAttribute(gaiaAttribute);

        if (vertices.size() > chunkSize) {
            chunkPointCloud.setVertices(new ArrayList<>(vertices.subList(0, chunkSize)));
            remainderPointCloud.setVertices(new ArrayList<>(vertices.subList(chunkSize, vertices.size())));
        } else {
            chunkPointCloud.setVertices(new ArrayList<>(vertices.subList(0, vertices.size())));
        }

        pointClouds.add(chunkPointCloud);
        pointClouds.add(remainderPointCloud);
        return pointClouds;
    }

    private short toUnsignedShort(int value) {
        if (value < 0 || value > 65535) {
            throw new IllegalArgumentException("Value out of range for unsigned short: " + value);
        }
        if (value <= 32767) {
            return (short) value;
        } else {
            return (short) (value - 65536);
        }
    }

    private int toDoubleFromUnsignedShort(short value) {
        if (value < 0) {
            return value + 65536;
        } else {
            return value;
        }
    }
}
