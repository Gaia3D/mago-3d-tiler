package com.gaia3d.basic.geometry.octree;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.util.GaiaOctreeUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Setter
@Getter

public class GaiaOctreeFaces extends GaiaOctree<GaiaFaceData> {
    //private int idx = -1;
    //private GaiaOctreeCoordinate coordinate = new GaiaOctreeCoordinate();
    private int maxDepth = 5;

    public GaiaOctreeFaces(GaiaOctree<GaiaFaceData> parent, GaiaBoundingBox boundingBox) {
        super(parent, boundingBox);
    }

    public void addFaceDataList(List<GaiaFaceData> faceDataList) {
        List<GaiaFaceData> contents = this.getContents();
        contents.addAll(faceDataList);
    }

    public void distributeContentsByCenterPoint() {
        List<GaiaFaceData> contents = this.getContents();
        if (contents.isEmpty()) {
            return;
        }
        GaiaBoundingBox bbox = this.getBoundingBox();
        double minX = bbox.getMinX();
        double minY = bbox.getMinY();
        double minZ = bbox.getMinZ();
        double maxX = bbox.getMaxX();
        double maxY = bbox.getMaxY();
        double maxZ = bbox.getMaxZ();

        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;
        double midZ = (minZ + maxZ) / 2.0;

        int debugCounter = 0;
        List<GaiaOctree<GaiaFaceData>> children = this.getChildren();
        for (GaiaFaceData faceData : contents) {
            Vector3d centerPoint = faceData.getCenterPoint();
            if (centerPoint.x < midX) {
                // 0, 3, 4, 7
                if (centerPoint.y < midY) {
                    // 0, 4
                    if (centerPoint.z < midZ) {
                        children.get(0).addContent(faceData);
                    } else {
                        children.get(4).addContent(faceData);
                    }
                } else {
                    // 3, 7
                    if (centerPoint.z < midZ) {
                        children.get(3).addContent(faceData);
                    } else {
                        children.get(7).addContent(faceData);
                    }
                }
            } else {
                // 1, 2, 5, 6
                if (centerPoint.y < midY) {
                    // 1, 5
                    if (centerPoint.z < midZ) {
                        children.get(1).addContent(faceData);
                    } else {
                        children.get(5).addContent(faceData);
                    }
                } else {
                    // 2, 6
                    if (centerPoint.z < midZ) {
                        children.get(2).addContent(faceData);
                    } else {
                        children.get(6).addContent(faceData);
                    }
                }
            }

            debugCounter++;
        }

        // once the contents are distributed, clear the list
        contents.clear();
    }

    public boolean intersectsBoundingBox(GaiaBoundingBox bbox) {
        GaiaBoundingBox thisBbox = this.getBoundingBox();
        if (thisBbox == null || bbox == null) {
            return false;
        }
        return thisBbox.intersects(bbox);
    }

    public void distributeContentsByBoundingBox(boolean distributionUnique) {
        List<GaiaFaceData> contents = this.getContents();
        if (contents.isEmpty()) {
            return;
        }

        int debugCounter = 0;
        List<GaiaOctree<GaiaFaceData>> children = this.getChildren();
        for (GaiaFaceData faceData : contents) {
            GaiaBoundingBox bbox = faceData.getBoundingBox();
            for (int i = 0; i < 8; i++) {
                GaiaOctreeFaces child = (GaiaOctreeFaces) children.get(i);
                GaiaBoundingBox childBbox = child.getBoundingBox();
                if (childBbox.intersects(bbox)) {
                    child.addContent(faceData);
                    if (distributionUnique) {
                        break;
                    }
                }
            }

            debugCounter++;
        }

        // once the contents are distributed, clear the list
        contents.clear();
    }

    public GaiaBoundingBox getBoundingBox() {
        GaiaBoundingBox boundingBox = this.getBoundingBox();
        return boundingBox.clone();
    }

    public void makeTree(double minBoxSize) {
        GaiaBoundingBox bbox = this.getBoundingBox();
        double minX = bbox.getMinX();
        double minY = bbox.getMinY();
        double minZ = bbox.getMinZ();
        double maxX = bbox.getMaxX();
        double maxY = bbox.getMaxY();
        double maxZ = bbox.getMaxZ();
        if ((maxX - minX) < minBoxSize || (maxY - minY) < minBoxSize || (maxZ - minZ) < minBoxSize) {
            return;
        }

        if (this.getDepth() >= maxDepth) {
            return;
        }

        List<GaiaFaceData> contents = this.getContents();
        if (contents.isEmpty()) {
            return;
        }

        createChildren();
        distributeContentsByCenterPoint();

        List<GaiaOctree<GaiaFaceData>> children = this.getChildren();
        for (GaiaOctree<GaiaFaceData> child : children) {
            GaiaOctreeFaces childFaces = (GaiaOctreeFaces) child;
            childFaces.makeTree(minBoxSize);
        }
    }

//    public void extractOctreesWithContents(List<GaiaOctreeFaces> octrees) {
//        if (!faceDataList.isEmpty()) {
//            octrees.add(this);
//        }
//        if (children != null) {
//            for (GaiaOctreeFaces child : children) {
//                child.extractOctreesWithContents(octrees);
//            }
//        }
//    }

//    public boolean[] hasNeighbor() {
//        boolean[] result = new boolean[6];
//        result[0] = false;
//        result[1] = false;
//        result[2] = false;
//        result[3] = false;
//        result[4] = false;
//        result[5] = false;
//
//        int depth = this.getDepth();
//        if (depth == 0) {
//            return result;
//        }
//
//        GaiaOctreeFaces parent = (GaiaOctreeFaces) this.getParent();
//        if (parent == null) {
//            return result;
//        }
//
//        boolean includeChildren = false;
//
//        //Left octree
//        GaiaOctreeCoordinate leftCoord = this.coordinate.getLeft();
//        GaiaOctreeFaces leftOctree = getOctreeByCoordinate(leftCoord);
//        if (leftOctree == null) {
//            result[0] = false;
//        } else if (leftOctree.hasContents(includeChildren)) {
//            result[0] = true;
//        }
//
//        //Right octree
//        GaiaOctreeCoordinate rightCoord = this.coordinate.getRight();
//        GaiaOctreeFaces rightOctree = getOctreeByCoordinate(rightCoord);
//        if (rightOctree == null) {
//            result[1] = false;
//        } else if (rightOctree.hasContents(includeChildren)) {
//            result[1] = true;
//        }
//
//        //Front octree
//        GaiaOctreeCoordinate frontCoord = this.coordinate.getFront();
//        GaiaOctreeFaces frontOctree = getOctreeByCoordinate(frontCoord);
//        if (frontOctree == null) {
//            result[2] = false;
//        } else if (frontOctree.hasContents(includeChildren)) {
//            result[2] = true;
//        }
//
//        //Rear octree
//        GaiaOctreeCoordinate rearCoord = this.coordinate.getRear();
//        GaiaOctreeFaces rearOctree = getOctreeByCoordinate(rearCoord);
//        if (rearOctree == null) {
//            result[3] = false;
//        } else if (rearOctree.hasContents(includeChildren)) {
//            result[3] = true;
//        }
//
//        //Top octree
//        GaiaOctreeCoordinate topCoord = this.coordinate.getTop();
//        GaiaOctreeFaces topOctree = getOctreeByCoordinate(topCoord);
//        if (topOctree == null) {
//            result[5] = false;
//        } else if (topOctree.hasContents(includeChildren)) {
//            result[5] = true;
//        }
//
//        //Bottom octree
//        GaiaOctreeCoordinate bottomCoord = this.coordinate.getBottom();
//        GaiaOctreeFaces bottomOctree = getOctreeByCoordinate(bottomCoord);
//        if (bottomOctree == null) {
//            result[4] = false;
//        } else if (bottomOctree.hasContents(includeChildren)) {
//            result[4] = true;
//        }
//
//
//        return result;
//    }
}
