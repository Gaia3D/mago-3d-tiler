package com.gaia3d.basic.geometry.octree;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.halfedge.HalfEdgeFace;
import com.gaia3d.basic.halfedge.HalfEdgeSurface;
import com.gaia3d.basic.halfedge.HalfEdgeVertex;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Setter
@Getter

public class HalfEdgeOctree {
    private HalfEdgeOctree parent = null;
    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;
    private int idx = -1;
    private GaiaOctreeCoordinate coordinate = new GaiaOctreeCoordinate();
    private int maxDepth = 5;
    private double minBoxSize = 0.1;
    private HalfEdgeOctree[] children = null;

    private List<HalfEdgeVertex> vertices = new ArrayList<>();
    private List<HalfEdgeFace> faces = new ArrayList<>();
    private List<HalfEdgeSurface> surfaces = new ArrayList<>();

    public HalfEdgeOctree(HalfEdgeOctree parent) {
        this.parent = parent;

        if (parent != null) {
            this.maxDepth = parent.maxDepth;
        }
    }

    public void setAsCube() {
        // Only modify the maximum values
        double x = maxX - minX;
        double y = maxY - minY;
        double z = maxZ - minZ;
        double max = Math.max(x, Math.max(y, z));
        maxX = minX + max;
        maxY = minY + max;
        maxZ = minZ + max;
    }

    public void createChildren() {
        children = new HalfEdgeOctree[8];
        for (int i = 0; i < 8; i++) {
            children[i] = new HalfEdgeOctree(this);
            children[i].idx = i;
        }

        // now set children sizes
        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;
        double midZ = (minZ + maxZ) / 2.0;

        children[0].setSize(minX, minY, minZ, midX, midY, midZ);
        children[1].setSize(midX, minY, minZ, maxX, midY, midZ);
        children[2].setSize(midX, midY, minZ, maxX, maxY, midZ);
        children[3].setSize(minX, midY, minZ, midX, maxY, midZ);

        children[4].setSize(minX, minY, midZ, midX, midY, maxZ);
        children[5].setSize(midX, minY, midZ, maxX, midY, maxZ);
        children[6].setSize(midX, midY, midZ, maxX, maxY, maxZ);
        children[7].setSize(minX, midY, midZ, midX, maxY, maxZ);

        // now set children coords
        int L = this.coordinate.getDepth();
        int X = this.coordinate.getX();
        int Y = this.coordinate.getY();
        int Z = this.coordinate.getZ();
        children[0].coordinate.setDepthAndCoord(L + 1, X * 2, Y * 2, Z * 2);
        children[1].coordinate.setDepthAndCoord(L + 1, X * 2 + 1, Y * 2, Z * 2);
        children[2].coordinate.setDepthAndCoord(L + 1, X * 2 + 1, Y * 2 + 1, Z * 2);
        children[3].coordinate.setDepthAndCoord(L + 1, X * 2, Y * 2 + 1, Z * 2);

        children[4].coordinate.setDepthAndCoord(L + 1, X * 2, Y * 2, Z * 2 + 1);
        children[5].coordinate.setDepthAndCoord(L + 1, X * 2 + 1, Y * 2, Z * 2 + 1);
        children[6].coordinate.setDepthAndCoord(L + 1, X * 2 + 1, Y * 2 + 1, Z * 2 + 1);
        children[7].coordinate.setDepthAndCoord(L + 1, X * 2, Y * 2 + 1, Z * 2 + 1);

        for (int i = 0; i < 8; i++) {
            children[i].setMinBoxSize(minBoxSize);
            children[i].setMaxDepth(maxDepth);
        }
    }

    public void makeTreeByMaxDepth(int maxDepth) {
        if (this.coordinate.getDepth() >= maxDepth) {
            return;
        }

        createChildren();
        distributeContents();

        for (HalfEdgeOctree child : children) {
            child.makeTreeByMaxDepth(maxDepth);
        }
    }

    public void makeTreeByMinBoxSize(double minBoxSize) {
        if ((maxX - minX) < minBoxSize || (maxY - minY) < minBoxSize || (maxZ - minZ) < minBoxSize) {
            return;
        }

        if (this.coordinate.getDepth() >= maxDepth) {
            return;
        }

        if (vertices.isEmpty()) {
            return;
        }

        createChildren();
        distributeContents();

        for (HalfEdgeOctree child : children) {
            child.makeTreeByMinBoxSize(minBoxSize);
        }
    }

    public void makeTreeByMinVertexCount(int minVertexCount) {
        if (this.coordinate.getDepth() >= maxDepth) {
            return;
        }

        if (vertices.isEmpty()) {
            return;
        }

        int vertexCount = vertices.size();
        if (vertexCount < minVertexCount) {
            return;
        }

        double sizeX = maxX - minX;
        if (sizeX < minBoxSize) {
            return;
        }

        createChildren();
        distributeContents();

        for (HalfEdgeOctree child : children) {
            child.makeTreeByMinVertexCount(minVertexCount);
        }
    }

    public void calculateSize() {
        int verticesCount = vertices.size();
        if (verticesCount == 0) {
            return;
        }

        minX = Double.MAX_VALUE;
        minY = Double.MAX_VALUE;
        minZ = Double.MAX_VALUE;
        maxX = -Double.MAX_VALUE;
        maxY = -Double.MAX_VALUE;
        maxZ = -Double.MAX_VALUE;

        for (HalfEdgeVertex vertex : vertices) {
            Vector3d position = vertex.getPosition();
            if (position.x < minX) {
                minX = position.x;
            }
            if (position.y < minY) {
                minY = position.y;
            }
            if (position.z < minZ) {
                minZ = position.z;
            }
            if (position.x > maxX) {
                maxX = position.x;
            }
            if (position.y > maxY) {
                maxY = position.y;
            }
            if (position.z > maxZ) {
                maxZ = position.z;
            }
        }
    }

    public void setSize(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

//    public void distributeFacesToLeaf() {
//        if (this.faces.isEmpty()) return;
//
//        if (this.children == null) return;
//
//        double midX = (minX + maxX) / 2.0;
//        double midY = (minY + maxY) / 2.0;
//        double midZ = (minZ + maxZ) / 2.0;
//
//        for (HalfEdgeFace face : this.faces) {
//            Vector3d center = face.getBarycenter(null);
//            if (center.x < midX) {
//                if (center.y < midY) {
//                    if (center.z < midZ) {
//                        children[0].faces.add(face);
//                    } else {
//                        children[4].faces.add(face);
//                    }
//                } else {
//                    if (center.z < midZ) {
//                        children[3].faces.add(face);
//                    } else {
//                        children[7].faces.add(face);
//                    }
//                }
//            } else {
//                if (center.y < midY) {
//                    if (center.z < midZ) {
//                        children[1].faces.add(face);
//                    } else {
//                        children[5].faces.add(face);
//                    }
//                } else {
//                    if (center.z < midZ) {
//                        children[2].faces.add(face);
//                    } else {
//                        children[6].faces.add(face);
//                    }
//                }
//            }
//        }
//
//        this.faces.clear();
//
//        for (HalfEdgeOctree child : children) {
//            child.distributeFacesToLeaf();
//        }
//    }

    public void distributeFacesToTargetDepth(int targetDepth) {
        if (this.faces.isEmpty()) return;

        if (this.getCoordinate().getDepth() >= targetDepth) return;

        if (this.children == null) {
            this.createChildren();
        }

        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;
        double midZ = (minZ + maxZ) / 2.0;

        for (HalfEdgeFace face : this.faces) {
            Vector3d center = face.getBarycenter(null);
            if (center.x < midX) {
                if (center.y < midY) {
                    if (center.z < midZ) {
                        children[0].faces.add(face);
                    } else {
                        children[4].faces.add(face);
                    }
                } else {
                    if (center.z < midZ) {
                        children[3].faces.add(face);
                    } else {
                        children[7].faces.add(face);
                    }
                }
            } else {
                if (center.y < midY) {
                    if (center.z < midZ) {
                        children[1].faces.add(face);
                    } else {
                        children[5].faces.add(face);
                    }
                } else {
                    if (center.z < midZ) {
                        children[2].faces.add(face);
                    } else {
                        children[6].faces.add(face);
                    }
                }
            }
        }

        // clear the faces list
        this.faces.clear();

        if (this.getCoordinate().getDepth() < targetDepth) {
            for (HalfEdgeOctree child : children) {
                child.distributeFacesToTargetDepth(targetDepth);
            }
        }
    }

    public boolean intersectsPoint(Vector3d point) {
        if (point.x < minX || point.x > maxX) {
            return false;
        }
        if (point.y < minY || point.y > maxY) {
            return false;
        }
        if (point.z < minZ || point.z > maxZ) {
            return false;
        }
        return true;
    }

    public boolean intersectsBoundingBox(GaiaBoundingBox box) {
        if (box.getMaxX() < minX || box.getMinX() > maxX) {
            return false;
        } else if (box.getMaxY() < minY || box.getMinY() > maxY) {
            return false;
        } else if (box.getMaxZ() < minZ || box.getMinZ() > maxZ) {
            return false;
        }
        return true;
    }

//    public void distributeFacesToTargetDepth(int targetDepth, boolean canBeRepeated) {
//        if (this.faces.isEmpty()) return;
//
//        if (this.getCoordinate().getDepth() >= targetDepth) return;
//
//        if (this.children == null) {
//            this.createChildren();
//        }
//
//        double midX = (minX + maxX) / 2.0;
//        double midY = (minY + maxY) / 2.0;
//        double midZ = (minZ + maxZ) / 2.0;
//
//        for (HalfEdgeFace face : this.faces) {
//            Vector3d center = face.getBarycenter(null);
//            int index = 0;
//            if (center.x < midX) {
//                if (center.y < midY) {
//                    if (center.z < midZ) {
//                        children[0].faces.add(face);
//                        index = 0;
//                    } else {
//                        children[4].faces.add(face);
//                        index = 4;
//                    }
//                } else {
//                    if (center.z < midZ) {
//                        children[3].faces.add(face);
//                        index = 3;
//                    } else {
//                        children[7].faces.add(face);
//                        index = 7;
//                    }
//                }
//            } else {
//                if (center.y < midY) {
//                    if (center.z < midZ) {
//                        children[1].faces.add(face);
//                        index = 1;
//                    } else {
//                        children[5].faces.add(face);
//                        index = 5;
//                    }
//                } else {
//                    if (center.z < midZ) {
//                        children[2].faces.add(face);
//                        index = 2;
//                    } else {
//                        children[6].faces.add(face);
//                        index = 6;
//                    }
//                }
//            }
//
//            if (canBeRepeated) {
//                double octreeSize = this.getMaxSize();
//                double error = octreeSize * 0.005;
//
//                boolean isAlmostAxisAlignedFace = false;
//                GaiaBoundingBox faceBBox = face.getBoundingBox();
//                Vector3d normal = face.getPlaneNormal();
//                double normalX = Math.abs(normal.x);
//                double normalY = Math.abs(normal.y);
//                double normalZ = Math.abs(normal.z);
//
//                if (normalX > normalY && normalX > normalZ && normalX > 0.85) {
//                    double lengthY = faceBBox.getLengthY();
//                    double lengthZ = faceBBox.getLengthZ();
//                    faceBBox.expandXYZ(error, -lengthY * 0.1, -lengthZ * 0.1);
//                    isAlmostAxisAlignedFace = true;
//                }
//                else if (normalY > normalX && normalY > normalZ && normalY > 0.85) {
//                    double lengthX = faceBBox.getLengthX();
//                    double lengthZ = faceBBox.getLengthZ();
//                    faceBBox.expandXYZ(-lengthX * 0.1, error, -lengthZ * 0.1);
//                    isAlmostAxisAlignedFace = true;
//                }
//
//                if (isAlmostAxisAlignedFace) {
//                    for (int idx = 0; idx < 8; idx++) {
//                        if (idx == index) {
//                            continue;
//                        }
//                        HalfEdgeOctree child = children[idx];
//                        if (child.intersectsBoundingBox(faceBBox)) {
//                            child.faces.add(face);
//                        }
//                    }
//                }
//            }
//        }
//
//        // clear the faces list
//        this.faces.clear();
//
//        if (this.getCoordinate().getDepth() < targetDepth) {
//            for (HalfEdgeOctree child : children) {
//                child.distributeFacesToTargetDepth(targetDepth, canBeRepeated);
//            }
//        }
//    }

    public void distributeContents() {
        if (vertices.isEmpty()) {
            return;
        }

        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;
        double midZ = (minZ + maxZ) / 2.0;

        for (HalfEdgeVertex vertex : vertices) {
            Vector3d position = vertex.getPosition();
            if (position.x < midX) {
                if (position.y < midY) {
                    if (position.z < midZ) {
                        children[0].addVertex(vertex);
                    } else {
                        children[4].addVertex(vertex);
                    }
                } else {
                    if (position.z < midZ) {
                        children[3].addVertex(vertex);
                    } else {
                        children[7].addVertex(vertex);
                    }
                }
            } else {
                if (position.y < midY) {
                    if (position.z < midZ) {
                        children[1].addVertex(vertex);
                    } else {
                        children[5].addVertex(vertex);
                    }
                } else {
                    if (position.z < midZ) {
                        children[2].addVertex(vertex);
                    } else {
                        children[6].addVertex(vertex);
                    }
                }
            }
        }

        // clear the vertices list
        vertices.clear();
    }

    private void addVertex(HalfEdgeVertex vertex) {
        vertices.add(vertex);
    }

    public void extractOctreesWithFaces(List<HalfEdgeOctree> octrees) {
        if (!faces.isEmpty()) {
            octrees.add(this);
        }

        if (children != null) {
            for (HalfEdgeOctree child : children) {
                child.extractOctreesWithFaces(octrees);
            }
        }
    }

    public void extractOctreesWithContents(List<HalfEdgeOctree> octrees) {
        if (!vertices.isEmpty()) {
            octrees.add(this);
        }

        if (children != null) {
            for (HalfEdgeOctree child : children) {
                child.extractOctreesWithContents(octrees);
            }
        }
    }

    public double getSizeX() {
        return maxX - minX;
    }

    public double getSizeY() {
        return maxY - minY;
    }

    public double getSizeZ() {
        return maxZ - minZ;
    }

    public double getMaxSize() {
        double x = maxX - minX;
        double y = maxY - minY;
        double z = maxZ - minZ;
        return Math.max(x, Math.max(y, z));
    }
}
