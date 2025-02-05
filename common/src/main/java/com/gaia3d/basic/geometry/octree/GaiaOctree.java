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
@NoArgsConstructor
@AllArgsConstructor

public class GaiaOctree {
    private GaiaOctree parent = null;
    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;
    private int idx = -1;
    private GaiaOctreeCoordinate coordinate = new GaiaOctreeCoordinate();
    private int maxDepth = 5;
    private List<GaiaFaceData> faceDataList = new ArrayList<>();

    private GaiaOctree[] children = null;
    // children indices
    // down                         up
    // +---------+---------+        +---------+---------+
    // |         |         |        |         |         |
    // |    3    |    2    |        |    7    |    6    |
    // |         |         |        |         |         |
    // +---------+---------+        +---------+---------+
    // |         |         |        |         |         |
    // |    0    |    1    |        |    4    |    5    |
    // |         |         |        |         |         |
    // +---------+---------+        +---------+---------+

    public GaiaOctree(GaiaOctree parent) {
        if (parent != null) {
            this.parent = parent;
            this.coordinate.setDepth(parent.coordinate.getDepth() + 1);
            this.maxDepth = parent.maxDepth;
        } else {
            this.coordinate.setDepthAndCoord(0, 0, 0, 0);
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

    public void addFaceDataList(List<GaiaFaceData> faceDataList) {
        this.faceDataList.addAll(faceDataList);
    }

    public void setSize(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public void createChildren() {
        children = new GaiaOctree[8];
        for (int i = 0; i < 8; i++) {
            children[i] = new GaiaOctree(this);
            children[i].idx = i;
        }

        // now set children sizes.***
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

        // now set children coords.***
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
    }

    public void distributeContentsByCenterPoint() {
        if (faceDataList.size() == 0) {
            return;
        }

        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;
        double midZ = (minZ + maxZ) / 2.0;

        int debugCounter = 0;
        for (GaiaFaceData faceData : faceDataList) {
            Vector3d centerPoint = faceData.getCenterPoint();
            if (centerPoint.x < midX) {
                // 0, 3, 4, 7
                if (centerPoint.y < midY) {
                    // 0, 4
                    if (centerPoint.z < midZ) {
                        children[0].addFaceData(faceData);
                    } else {
                        children[4].addFaceData(faceData);
                    }
                } else {
                    // 3, 7
                    if (centerPoint.z < midZ) {
                        children[3].addFaceData(faceData);
                    } else {
                        children[7].addFaceData(faceData);
                    }
                }
            } else {
                // 1, 2, 5, 6
                if (centerPoint.y < midY) {
                    // 1, 5
                    if (centerPoint.z < midZ) {
                        children[1].addFaceData(faceData);
                    } else {
                        children[5].addFaceData(faceData);
                    }
                } else {
                    // 2, 6
                    if (centerPoint.z < midZ) {
                        children[2].addFaceData(faceData);
                    } else {
                        children[6].addFaceData(faceData);
                    }
                }
            }

            debugCounter++;
        }

        // once the contents are distributed, clear the list
        faceDataList.clear();
    }

    public boolean intersectsBoundingBox(GaiaBoundingBox bbox) {
        if (maxX < bbox.getMinX() || minX > bbox.getMaxX()) {
            return false;
        }
        if (maxY < bbox.getMinY() || minY > bbox.getMaxY()) {
            return false;
        }
        return !(maxZ < bbox.getMinZ()) && !(minZ > bbox.getMaxZ());

    }

    public void distributeContentsByBoundingBox(boolean distributionUnique) {
        if (faceDataList.size() == 0) {
            return;
        }

        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;
        double midZ = (minZ + maxZ) / 2.0;

        int debugCounter = 0;
        for (GaiaFaceData faceData : faceDataList) {
            GaiaBoundingBox bbox = faceData.getBoundingBox();
            for (int i = 0; i < 8; i++) {
                if (children[i].intersectsBoundingBox(bbox)) {
                    children[i].addFaceData(faceData);
                    if (distributionUnique) {
                        break;
                    }
                }
            }

            debugCounter++;
        }

        // once the contents are distributed, clear the list
        faceDataList.clear();
    }

    private void addFaceData(GaiaFaceData faceData) {
        this.faceDataList.add(faceData);
    }

    public void recalculateSize() {
        if (faceDataList.isEmpty()) {
            return;
        }

        for (int i = 0; i < faceDataList.size(); i++) {
            GaiaFaceData faceData = faceDataList.get(i);
            if (faceData == null) {
                continue;
            }

            GaiaBoundingBox boundingBox = faceData.getBoundingBox();
            if (boundingBox == null) {
                continue;
            }

            if (i == 0) {
                minX = boundingBox.getMinX();
                minY = boundingBox.getMinY();
                minZ = boundingBox.getMinZ();
                maxX = boundingBox.getMaxX();
                maxY = boundingBox.getMaxY();
                maxZ = boundingBox.getMaxZ();
                continue;
            }

            if (boundingBox.getMinX() < minX) {
                minX = boundingBox.getMinX();
            }
            if (boundingBox.getMinY() < minY) {
                minY = boundingBox.getMinY();
            }
            if (boundingBox.getMinZ() < minZ) {
                minZ = boundingBox.getMinZ();
            }

            if (boundingBox.getMaxX() > maxX) {
                maxX = boundingBox.getMaxX();
            }
            if (boundingBox.getMaxY() > maxY) {
                maxY = boundingBox.getMaxY();
            }
            if (boundingBox.getMaxZ() > maxZ) {
                maxZ = boundingBox.getMaxZ();
            }
        }

    }

    public GaiaBoundingBox getBoundingBox() {
        return new GaiaBoundingBox(minX, minY, minZ, maxX, maxY, maxZ, true);
    }

    public void makeTree(double minBoxSize) {
        if ((maxX - minX) < minBoxSize || (maxY - minY) < minBoxSize || (maxZ - minZ) < minBoxSize) {
            return;
        }

        if (this.coordinate.getDepth() >= maxDepth) {
            return;
        }

        if (faceDataList.isEmpty()) {
            return;
        }

        createChildren();
        distributeContentsByCenterPoint();
//        boolean distributionUnique = false;
//        distributeContentsByBoundingBox(distributionUnique);

        for (GaiaOctree child : children) {
            child.makeTree(minBoxSize);
        }
    }

    public void extractOctreesWithContents(List<GaiaOctree> octrees) {
        if (!faceDataList.isEmpty()) {
            octrees.add(this);
        }
        if (children != null) {
            for (GaiaOctree child : children) {
                child.extractOctreesWithContents(octrees);
            }
        }
    }

    public GaiaOctree getRoot() {
        GaiaOctree root = null;

        if (this.parent == null) {
            root = this;
        } else {
            root = this.parent.getRoot();
        }

        return root;
    }

    public boolean hasContents(boolean includeChildren) {
        if (faceDataList.size() > 0) {
            return true;
        }

        if (includeChildren && children != null) {
            for (GaiaOctree child : children) {
                if (child.hasContents(includeChildren)) {
                    return true;
                }
            }
        }

        return false;
    }

    public GaiaOctree getOctreeByCoordinate(GaiaOctreeCoordinate coord) {
        if (coord.getDepth() < 0) {
            return null;
        }

        GaiaOctree resultOctree = null;
        List<GaiaOctreeCoordinate> fullPath = coord.getFullPath(null);
        resultOctree = this.getOctreeByFullPath(fullPath);

        return resultOctree;
    }

    public GaiaOctree getOctreeByFullPath(List<GaiaOctreeCoordinate> fullPath) {
        if (fullPath == null || fullPath.size() == 0) {
            return null;
        }

        GaiaOctree root = this.getRoot();
        GaiaOctree current = root;
        int coordsCount = fullPath.size();
        for (int i = 0; i < coordsCount; i++) {
            GaiaOctreeCoordinate coord = fullPath.get(i);

            if (current == null) {
                return null;
            }

            if (current.coordinate.isEqual(coord)) {
                if (i < coordsCount - 1) {
                    GaiaOctreeCoordinate coordNext = fullPath.get(i + 1);

                    int idx = GaiaOctreeUtils.getOctreeIndex(coordNext);
                    if (idx < 0 || idx >= 8) {
                        return null;
                    }

                    if (current.children != null) {
                        current = current.children[idx];
                    } else {
                        return null;
                    }
                } else {
                    return current;
                }
            } else {
                return null;
            }
        }

        return current;
    }

    public boolean[] hasNeighbor() {
        boolean[] result = new boolean[6];
        result[0] = false;
        result[1] = false;
        result[2] = false;
        result[3] = false;
        result[4] = false;
        result[5] = false;

        int depth = this.coordinate.getDepth();
        if (depth == 0) {
            return result;
        }

        GaiaOctree parent = this.parent;
        if (parent == null) {
            return result;
        }

        boolean includeChildren = false;

        //Left octree.***
        GaiaOctreeCoordinate leftCoord = this.coordinate.getLeft();
        GaiaOctree leftOctree = getOctreeByCoordinate(leftCoord);
        if (leftOctree == null) {
            result[0] = false;
        } else if (leftOctree.hasContents(includeChildren)) {
            result[0] = true;
        }

        //Right octree.***
        GaiaOctreeCoordinate rightCoord = this.coordinate.getRight();
        GaiaOctree rightOctree = getOctreeByCoordinate(rightCoord);
        if (rightOctree == null) {
            result[1] = false;
        } else if (rightOctree.hasContents(includeChildren)) {
            result[1] = true;
        }

        //Front octree.***
        GaiaOctreeCoordinate frontCoord = this.coordinate.getFront();
        GaiaOctree frontOctree = getOctreeByCoordinate(frontCoord);
        if (frontOctree == null) {
            result[2] = false;
        } else if (frontOctree.hasContents(includeChildren)) {
            result[2] = true;
        }

        //Rear octree.***
        GaiaOctreeCoordinate rearCoord = this.coordinate.getRear();
        GaiaOctree rearOctree = getOctreeByCoordinate(rearCoord);
        if (rearOctree == null) {
            result[3] = false;
        } else if (rearOctree.hasContents(includeChildren)) {
            result[3] = true;
        }

        //Top octree.***
        GaiaOctreeCoordinate topCoord = this.coordinate.getTop();
        GaiaOctree topOctree = getOctreeByCoordinate(topCoord);
        if (topOctree == null) {
            result[5] = false;
        } else if (topOctree.hasContents(includeChildren)) {
            result[5] = true;
        }

        //Bottom octree.***
        GaiaOctreeCoordinate bottomCoord = this.coordinate.getBottom();
        GaiaOctree bottomOctree = getOctreeByCoordinate(bottomCoord);
        if (bottomOctree == null) {
            result[4] = false;
        } else if (bottomOctree.hasContents(includeChildren)) {
            result[4] = true;
        }


        return result;
    }
}
