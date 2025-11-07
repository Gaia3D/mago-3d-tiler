package com.gaia3d.basic.geometry.octree;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaVertex;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.List;

@Slf4j
@Setter
@Getter
public class GaiaOctreeVertices extends GaiaOctree<GaiaVertex> {
    private int limitDepth = 5;
    private double limitBoxSize = 1.0;

    public GaiaOctreeVertices(GaiaOctree<GaiaVertex> parent, GaiaBoundingBox boundingBox) {
        super(parent, boundingBox);

        if (parent != null) {
            GaiaOctreeVertices parentVertex = (GaiaOctreeVertices) parent;
            this.limitDepth = parentVertex.getLimitDepth();
            this.limitBoxSize = parentVertex.getLimitBoxSize();
        }
    }

    @Override
    protected GaiaOctree<GaiaVertex> createChild(GaiaBoundingBox boundingBox) {
        return new GaiaOctreeVertices(this, boundingBox);
    }

    public void makeTreeByMinVertexCount(int minVertexCount) {
        if (this.getDepth() >= limitDepth) {
            return;
        }

        List<GaiaVertex> contents = this.getContents();
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

        List<GaiaOctree<GaiaVertex>> children = this.getChildren();
        for (GaiaOctree<GaiaVertex> child : children) {
            GaiaOctreeVertices childVertex = (GaiaOctreeVertices) child;
            childVertex.makeTreeByMinVertexCount(minVertexCount);
        }
    }

    public void distributeContents() {
        List<GaiaVertex> contents = this.getContents();
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

        List<GaiaOctree<GaiaVertex>> children = this.getChildren();

        for (GaiaVertex vertex : contents) {
            Vector3d position = vertex.getPosition();
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

        // clear the vertices list
        contents.clear();
    }
}
