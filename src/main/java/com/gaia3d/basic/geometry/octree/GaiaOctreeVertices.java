package com.gaia3d.basic.geometry.octree;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaVertex;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Setter
@Getter

public class GaiaOctreeVertices extends GaiaOctree<GaiaVertex> {
    private int maxDepth = 5;
    private double minBoxSize = 1.0;

    public GaiaOctreeVertices(GaiaOctree<GaiaVertex> parent, GaiaBoundingBox boundingBox) {
        super(parent, boundingBox);
    }

    @Override
    protected GaiaOctree<GaiaVertex> createChild(GaiaBoundingBox boundingBox) {
        return new GaiaOctreeVertices(this, boundingBox);
    }

    public void setAsCube() {
        // Only modify the maximum values
        GaiaBoundingBox boundingBox = this.getBoundingBox();
        double minX = boundingBox.getMinX();
        double minY = boundingBox.getMinY();
        double minZ = boundingBox.getMinZ();
        double maxX = boundingBox.getMaxX();
        double maxY = boundingBox.getMaxY();
        double maxZ = boundingBox.getMaxZ();
        double x = maxX - minX;
        double y = maxY - minY;
        double z = maxZ - minZ;
        double max = Math.max(x, Math.max(y, z));
        maxX = minX + max;
        maxY = minY + max;
        maxZ = minZ + max;
        boundingBox.setMaxX(maxX);
    }

    public void makeTreeByMinBoxSize(double minBoxSize) {
        GaiaBoundingBox boundingBox = this.getBoundingBox();
        double minX = boundingBox.getMinX();
        double minY = boundingBox.getMinY();
        double minZ = boundingBox.getMinZ();
        double maxX = boundingBox.getMaxX();
        double maxY = boundingBox.getMaxY();
        double maxZ = boundingBox.getMaxZ();

        if ((maxX - minX) < minBoxSize || (maxY - minY) < minBoxSize || (maxZ - minZ) < minBoxSize) {
            return;
        }

        if (this.getDepth() >= maxDepth) {
            return;
        }

        List<GaiaVertex> contents = this.getContents();
        if (contents.isEmpty()) {
            return;
        }

        createChildren();
        distributeContents();

        List<GaiaOctree<GaiaVertex>> children = this.getChildren();
        for (GaiaOctree<GaiaVertex> child : children) {
            GaiaOctreeVertices childVertices = (GaiaOctreeVertices) child;
            childVertices.makeTreeByMinBoxSize(minBoxSize);
        }
    }

    public void makeTreeByMinVertexCount(int minVertexCount) {
        if (this.getDepth() >= maxDepth) {
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
        double minY = boundingBox.getMinY();
        double minZ = boundingBox.getMinZ();
        double maxX = boundingBox.getMaxX();
        double maxY = boundingBox.getMaxY();
        double maxZ = boundingBox.getMaxZ();

        double sizeX = maxX - minX;
        if (sizeX < minBoxSize) {
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

//    public void calculateSize() {
//        //List<GaiaVertex> contents = this.getContents();
//        int verticesCount = vertices.size();
//        if (verticesCount == 0) {
//            return;
//        }
//
//        minX = Double.MAX_VALUE;
//        minY = Double.MAX_VALUE;
//        minZ = Double.MAX_VALUE;
//        maxX = -Double.MAX_VALUE;
//        maxY = -Double.MAX_VALUE;
//        maxZ = -Double.MAX_VALUE;
//
//        for (GaiaVertex vertex : vertices) {
//            Vector3d position = vertex.getPosition();
//            if (position.x < minX) {
//                minX = position.x;
//            }
//            if (position.y < minY) {
//                minY = position.y;
//            }
//            if (position.z < minZ) {
//                minZ = position.z;
//            }
//            if (position.x > maxX) {
//                maxX = position.x;
//            }
//            if (position.y > maxY) {
//                maxY = position.y;
//            }
//            if (position.z > maxZ) {
//                maxZ = position.z;
//            }
//        }
//    }


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

//    public void extractOctreesWithContents(List<GaiaOctreeVertices> octrees) {
//        if (!vertices.isEmpty()) {
//            octrees.add(this);
//        }
//
//        if (children != null) {
//            for (GaiaOctreeVertices child : children) {
//                child.extractOctreesWithContents(octrees);
//            }
//        }
//    }
}
