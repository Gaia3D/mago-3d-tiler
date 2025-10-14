package com.gaia3d.basic.geometry.octree;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.halfedge.HalfEdgeVertex;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.List;

@Slf4j
@Setter
@Getter
public class HalfEdgeOctreeVertices extends GaiaOctree<HalfEdgeVertex> {
    private int limitDepth = 5;
    private double limitBoxSize = 1.0;
    private int limitVertexCount = 10; // Maximum number of vertices to create a child node

    public HalfEdgeOctreeVertices(GaiaOctree<HalfEdgeVertex> parent, GaiaBoundingBox boundingBox) {
        super(parent, boundingBox);

        if (parent != null) {
            // Inherit limits from parent octree
            HalfEdgeOctreeVertices parentVertices = (HalfEdgeOctreeVertices) parent;
            this.limitDepth = parentVertices.getLimitDepth();
            this.limitBoxSize = parentVertices.getLimitBoxSize();
            this.limitVertexCount = parentVertices.getLimitVertexCount();
        }
    }

    @Override
    protected GaiaOctree<HalfEdgeVertex> createChild(GaiaBoundingBox boundingBox) {
        return new HalfEdgeOctreeVertices(this, boundingBox);
    }

    public void distributeContents() {
        List<HalfEdgeVertex> vertices = this.getContents();
        if (vertices.isEmpty()) {
            return;
        }

        GaiaBoundingBox boundingBox = this.getBoundingBox();
        double minX = boundingBox.getMinX();
        double maxX = boundingBox.getMaxX();
        double minY = boundingBox.getMinY();
        double maxY = boundingBox.getMaxY();
        double minZ = boundingBox.getMinZ();
        double maxZ = boundingBox.getMaxZ();

        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;
        double midZ = (minZ + maxZ) / 2.0;

        List<GaiaOctree<HalfEdgeVertex>> children = this.getChildren();

        for (HalfEdgeVertex vertex : vertices) {
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
        vertices.clear();
    }

    public void makeTree() {
        List<HalfEdgeVertex> vertices = this.getContents();
        if (this.getDepth() >= limitDepth) {
            return;
        }

        if (vertices.isEmpty()) {
            return;
        }

        int vertexCount = vertices.size();
        if (vertexCount < limitVertexCount) {
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

        List<GaiaOctree<HalfEdgeVertex>> children = this.getChildren();
        for (GaiaOctree<HalfEdgeVertex> child : children) {
            HalfEdgeOctreeVertices childVertex = (HalfEdgeOctreeVertices) child;
            childVertex.makeTree();
        }
    }
}
