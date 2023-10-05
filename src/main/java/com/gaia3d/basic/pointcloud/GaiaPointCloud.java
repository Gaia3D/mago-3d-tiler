package com.gaia3d.basic.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.GaiaVertex;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.process.tileprocess.tile.tileset.node.BoundingVolume;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GaiaPointCloud {
    String code = "A";
    private Path originalPath;
    private GaiaBoundingBox gaiaBoundingBox = new GaiaBoundingBox();
    List<GaiaVertex> vertices = new ArrayList<>();

    // Quarter based on the bounding box
    public List<GaiaPointCloud> distribute() {
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
