package com.gaia3d.basic.structure.splitter;

import com.gaia3d.basic.structure.GaiaFace;
import com.gaia3d.basic.structure.GaiaVertex;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Setter
@Getter
public class GaiaOctreeSceneSplitter {

    // Octree index
    //        bottom               up
    //     +------+------+     +------+------+
    //     |      |      |     |      |      |
    //     |  3   |   2  |     |  7   |   6  |
    //     |      |      |     |      |      |
    //     +------+------+     +------+------+
    //     |      |      |     |      |      |
    //     |  0   |   1  |     |  4   |   5  |
    //     |      |      |     |      |      |
    //     +------+------+     +------+------+

    int depth;
    GaiaOctreeSceneSplitter parent;
    double minX;
    double minY;
    double minZ;
    double maxX;
    double maxY;
    double maxZ;

    String name;
    int octreeIndex;

    GaiaOctreeSceneSplitter[] children;

    // data.***
    List<GaiaFace> faces;
    List<GaiaVertex> vertices; // must multiply the transformMatrix.***
    Matrix4d transformMatrix;
    Map<GaiaFace, Vector3d> mapFaceTransformedCenterPos;

    public GaiaOctreeSceneSplitter(GaiaOctreeSceneSplitter parentOwner, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (parentOwner == null) {
            this.depth = 0;
            this.octreeIndex = 0;
            this.name = "0";
        } else {
            this.depth = parentOwner.depth + 1;
        }
        this.parent = parentOwner;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;

        this.children = null;
        this.faces = null;
    }

    public String makeName() {
        String name = String.valueOf(octreeIndex);
        String totalName = "";
        if (this.parent != null) {
            totalName = this.parent.makeName();
        }

        this.name = totalName + name;
        return this.name;
    }

    public void addFace(GaiaFace face) {
        if (this.faces == null) {
            this.faces = new ArrayList<>();
        }
        this.faces.add(face);
    }

    private void createChildren() {
        if (this.children == null) {
            this.children = new GaiaOctreeSceneSplitter[8];
            double midX = (minX + maxX) / 2;
            double midY = (minY + maxY) / 2;
            double midZ = (minZ + maxZ) / 2;
            this.children[0] = new GaiaOctreeSceneSplitter(this, minX, minY, minZ, midX, midY, midZ);
            this.children[1] = new GaiaOctreeSceneSplitter(this, midX, minY, minZ, maxX, midY, midZ);
            this.children[2] = new GaiaOctreeSceneSplitter(this, midX, midY, minZ, maxX, maxY, midZ);
            this.children[3] = new GaiaOctreeSceneSplitter(this, minX, midY, minZ, midX, maxY, midZ);
            this.children[4] = new GaiaOctreeSceneSplitter(this, minX, minY, midZ, midX, midY, maxZ);
            this.children[5] = new GaiaOctreeSceneSplitter(this, midX, minY, midZ, maxX, midY, maxZ);
            this.children[6] = new GaiaOctreeSceneSplitter(this, midX, midY, midZ, maxX, maxY, maxZ);
            this.children[7] = new GaiaOctreeSceneSplitter(this, minX, midY, midZ, midX, maxY, maxZ);

            for (int i = 0; i < 8; i++) {
                this.children[i].octreeIndex = i;
                this.children[i].makeName();
            }
        }
    }

    public void extractOctreesWithContents(List<GaiaOctreeSceneSplitter> octrees) {
        if (this.faces != null && !this.faces.isEmpty()) {
            octrees.add(this);
        }

        if (this.children != null) {
            for (GaiaOctreeSceneSplitter child : this.children) {
                child.extractOctreesWithContents(octrees);
            }
        }
    }

    public void distributeContents(int targetDepth) {
        if (this.depth == targetDepth) {
            return;
        }

        if (this.children == null) {
            this.createChildren();
        }

        if (this.faces != null && !this.faces.isEmpty()) {
            Vector3d octreeCenterPos = this.getCenterPosition();
            for (GaiaFace face : this.faces) {
                Vector3d transformedFaceCenterPos = this.mapFaceTransformedCenterPos.get(face);
                if (transformedFaceCenterPos.x <= octreeCenterPos.x) {
                    if (transformedFaceCenterPos.y <= octreeCenterPos.y) {
                        if (transformedFaceCenterPos.z <= octreeCenterPos.z) {
                            // 0
                            children[0].addFace(face);
                        } else {
                            // 4
                            children[4].addFace(face);
                        }
                    } else {
                        if (transformedFaceCenterPos.z <= octreeCenterPos.z) {
                            // 3
                            children[3].addFace(face);
                        } else {
                            // 7
                            children[7].addFace(face);
                        }
                    }
                } else {
                    if (transformedFaceCenterPos.y <= octreeCenterPos.y) {
                        if (transformedFaceCenterPos.z <= octreeCenterPos.z) {
                            // 1
                            children[1].addFace(face);
                        } else {
                            // 5
                            children[5].addFace(face);
                        }
                    } else {
                        if (transformedFaceCenterPos.z <= octreeCenterPos.z) {
                            // 2
                            children[2].addFace(face);
                        } else {
                            // 6
                            children[6].addFace(face);
                        }
                    }
                }
            }
            this.faces = null;
        }

        if (this.depth < targetDepth) {
            for (GaiaOctreeSceneSplitter child : this.children) {
                child.mapFaceTransformedCenterPos = this.mapFaceTransformedCenterPos;
                child.transformMatrix = this.transformMatrix;
                child.distributeContents(targetDepth);
            }
        }
    }

    public void takeCubeForm() {
        // take cube form, maintain the left-bottom-front corner
        double x = maxX - minX;
        double y = maxY - minY;
        double z = maxZ - minZ;
        double max = Math.max(Math.max(x, y), z);

        maxX = minX + max;
        maxY = minY + max;
        maxZ = minZ + max;
    }

    public Vector3d getCenterPosition() {
        return new Vector3d((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
    }

    public Vector3d getSize() {
        return new Vector3d(maxX - minX, maxY - minY, maxZ - minZ);
    }
}
