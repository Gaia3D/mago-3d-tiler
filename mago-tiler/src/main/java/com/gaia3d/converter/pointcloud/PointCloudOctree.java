package com.gaia3d.converter.pointcloud;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.octree.GaiaOctree;
import com.gaia3d.basic.geometry.octree.GaiaOctreeVertices;
import com.gaia3d.basic.model.GaiaVertex;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.List;

@Slf4j
@Setter
@Getter
public class PointCloudOctree extends GaiaOctree<GaiaLasPoint> {
    private int limitDepth = 15;
    private double limitBoxSize = 25.0;

    public PointCloudOctree(GaiaOctree<GaiaLasPoint> parent, GaiaBoundingBox boundingBox) {
        super(parent, boundingBox);

        if (parent != null) {
            PointCloudOctree parentVertex = (PointCloudOctree) parent;
            this.limitDepth = parentVertex.getLimitDepth();
            this.limitBoxSize = parentVertex.getLimitBoxSize();
        }
    }

    @Override
    protected GaiaOctree<GaiaLasPoint> createChild(GaiaBoundingBox boundingBox) {
        return new PointCloudOctree(this, boundingBox);
    }

    public void makeTreeByMinVertexCount(int minVertexCount) {
        if (this.getDepth() >= limitDepth) {
            return;
        }

        List<GaiaLasPoint> contents = this.getContents();
        if (contents.isEmpty()) {
            return;
        }

        int vertexCount = contents.size();
        if (vertexCount < minVertexCount) {
            return;
        }

        GaiaBoundingBox boundingBox = this.getBoundingBox();
        double minX = boundingBox.getMinX();
        double maxX = boundingBox.getMaxX();

        double sizeX = maxX - minX;
        if (sizeX < limitBoxSize) {
            return;
        }

        createChildren();
        distributeContents();

        List<GaiaOctree<GaiaLasPoint>> children = this.getChildren();
        for (GaiaOctree<GaiaLasPoint> child : children) {
            PointCloudOctree childVertex = (PointCloudOctree) child;
            childVertex.makeTreeByMinVertexCount(minVertexCount);
        }
    }

    public void distributeContents() {
        List<GaiaLasPoint> contents = this.getContents();
        if (contents.isEmpty()) {
            return;
        }

        GaiaBoundingBox boundingBox = this.getBoundingBox();
        double minX = boundingBox.getMinX();
        double minY = boundingBox.getMinY();
        double minZ = boundingBox.getMinZ();
        double maxX = boundingBox.getMaxX();
        double maxY = boundingBox.getMaxY();
        double maxZ = boundingBox.getMaxZ();

        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;
        double midZ = (minZ + maxZ) / 2.0;

        List<GaiaOctree<GaiaLasPoint>> children = this.getChildren();

        for (GaiaLasPoint vertex : contents) {
            double[] positionDouble = vertex.getPosition();
            Vector3d position = new Vector3d(positionDouble[0], positionDouble[1], positionDouble[2]);
            if (position.x < midX) {
                if (position.y < midY) {
                    if (position.z < midZ) {
                        children.get(0).addContent(vertex);
                    } else {
                        children.get(4).addContent(vertex);
                    }
                } else {
                    if (position.z < midZ) {
                        children.get(3).addContent(vertex);
                    } else {
                        children.get(7).addContent(vertex);
                    }
                }
            } else {
                if (position.y < midY) {
                    if (position.z < midZ) {
                        children.get(1).addContent(vertex);
                    } else {
                        children.get(5).addContent(vertex);
                    }
                } else {
                    if (position.z < midZ) {
                        children.get(2).addContent(vertex);
                    } else {
                        children.get(6).addContent(vertex);
                    }
                }
            }
        }

        contents.clear();
    }
}
