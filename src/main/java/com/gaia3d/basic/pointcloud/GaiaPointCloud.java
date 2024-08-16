package com.gaia3d.basic.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.GaiaAttribute;
import com.gaia3d.basic.structure.GaiaVertex;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GaiaPointCloud implements Serializable {
    private String code = "A";
    private Path originalPath;
    private GaiaBoundingBox gaiaBoundingBox = new GaiaBoundingBox();
    private List<GaiaVertex> vertices = new ArrayList<>();
    private GaiaAttribute gaiaAttribute = new GaiaAttribute();

    public List<GaiaPointCloud> distribute() {
        double minX = gaiaBoundingBox.getMinX();
        double minY = gaiaBoundingBox.getMinY();
        double minZ = gaiaBoundingBox.getMinZ();
        double maxX = gaiaBoundingBox.getMaxX();
        double maxY = gaiaBoundingBox.getMaxY();
        double maxZ = gaiaBoundingBox.getMaxZ();

        double offsetX = maxX - minX;
        double offsetY = maxY - minY;
        double offsetZ = maxZ - minZ;

        if (offsetZ < offsetX || offsetZ < offsetY) {
            return distributeQuad();
        } else {
            return distributeOct();
        }
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

        GaiaBoundingBox gaiaBoundingBoxE = new GaiaBoundingBox();
        GaiaPointCloud gaiaPointCloudE = new GaiaPointCloud();
        gaiaPointCloudE.setCode("E");
        gaiaPointCloudE.setOriginalPath(originalPath);
        gaiaPointCloudE.setGaiaBoundingBox(gaiaBoundingBoxE);
        List<GaiaVertex> verticesE = gaiaPointCloudE.getVertices();

        GaiaBoundingBox gaiaBoundingBoxF = new GaiaBoundingBox();
        GaiaPointCloud gaiaPointCloudF = new GaiaPointCloud();
        gaiaPointCloudF.setCode("F");
        gaiaPointCloudF.setOriginalPath(originalPath);
        gaiaPointCloudF.setGaiaBoundingBox(gaiaBoundingBoxF);
        List<GaiaVertex> verticesF = gaiaPointCloudF.getVertices();

        GaiaBoundingBox gaiaBoundingBoxG = new GaiaBoundingBox();
        GaiaPointCloud gaiaPointCloudG = new GaiaPointCloud();
        gaiaPointCloudG.setCode("G");
        gaiaPointCloudG.setOriginalPath(originalPath);
        gaiaPointCloudG.setGaiaBoundingBox(gaiaBoundingBoxG);
        List<GaiaVertex> verticesG = gaiaPointCloudG.getVertices();

        GaiaBoundingBox gaiaBoundingBoxH = new GaiaBoundingBox();
        GaiaPointCloud gaiaPointCloudH = new GaiaPointCloud();
        gaiaPointCloudH.setCode("H");
        gaiaPointCloudH.setOriginalPath(originalPath);
        gaiaPointCloudH.setGaiaBoundingBox(gaiaBoundingBoxH);
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

        GaiaPointCloud remainderPointCloud = new GaiaPointCloud();
        remainderPointCloud.setOriginalPath(originalPath);
        remainderPointCloud.setGaiaBoundingBox(gaiaBoundingBox);

        if (vertices.size() > chunkSize) {
            chunkPointCloud.setVertices(vertices.subList(0, chunkSize));
            remainderPointCloud.setVertices(vertices.subList(chunkSize, vertices.size()));
        } else {
            chunkPointCloud.setVertices(vertices.subList(0, vertices.size()));
        }

        pointClouds.add(chunkPointCloud);
        pointClouds.add(remainderPointCloud);
        return pointClouds;
    }
}
