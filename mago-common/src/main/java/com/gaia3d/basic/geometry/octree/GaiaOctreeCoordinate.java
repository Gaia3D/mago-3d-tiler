package com.gaia3d.basic.geometry.octree;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Getter
@Setter
public class GaiaOctreeCoordinate {
    private int depth = 0;
    private int x = 0;
    private int y = 0;
    private int z = 0;

    public void setDepthAndCoord(int depth, int x, int y, int z) {
        this.depth = depth;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public GaiaOctreeCoordinate getLeft() {
        GaiaOctreeCoordinate left = new GaiaOctreeCoordinate();
        left.setDepthAndCoord(depth, x - 1, y, z);
        return left;
    }

    public GaiaOctreeCoordinate getRight() {
        GaiaOctreeCoordinate right = new GaiaOctreeCoordinate();
        right.setDepthAndCoord(depth, x + 1, y, z);
        return right;
    }

    public GaiaOctreeCoordinate getFront() {
        GaiaOctreeCoordinate front = new GaiaOctreeCoordinate();
        front.setDepthAndCoord(depth, x, y - 1, z);
        return front;
    }

    public GaiaOctreeCoordinate getRear() {
        GaiaOctreeCoordinate back = new GaiaOctreeCoordinate();
        back.setDepthAndCoord(depth, x, y + 1, z);
        return back;
    }

    public GaiaOctreeCoordinate getTop() {
        GaiaOctreeCoordinate top = new GaiaOctreeCoordinate();
        top.setDepthAndCoord(depth, x, y, z + 1);
        return top;
    }

    public GaiaOctreeCoordinate getBottom() {
        GaiaOctreeCoordinate bottom = new GaiaOctreeCoordinate();
        bottom.setDepthAndCoord(depth, x, y, z - 1);
        return bottom;
    }

    public GaiaOctreeCoordinate getParentCoord() {
        if (this.depth == 0) {
            return null;
        }

        GaiaOctreeCoordinate parent = new GaiaOctreeCoordinate();
        parent.setDepthAndCoord(depth - 1, x / 2, y / 2, z / 2);
        return parent;
    }

    public int getIndexAtDepth() {
        int index = 0;
        int maxIndex = getMaxIndexAtDepth();
        if (x < 0 || x >= maxIndex || y < 0 || y >= maxIndex || z < 0 || z >= maxIndex) {
            log.error("Invalid octree coordinate. depth: {}, x: {}, y: {}, z: {}", depth, x, y, z);
            return -1;
        }

        int xValue = x;
        int yValue = y * maxIndex;
        int zValue = z * maxIndex * maxIndex;
        index = xValue + yValue + zValue;
        return index;
        //return x + (y * (int) Math.pow(2, depth)) + (z * (int) Math.pow(2, depth) * (int) Math.pow(2, depth));
    }

    public int getMaxIndexAtDepth() {
        return (int) Math.pow(2, depth);
    }

    public List<GaiaOctreeCoordinate> getFullPath(List<GaiaOctreeCoordinate> resultFullPath) {
        if (resultFullPath == null) {
            resultFullPath = new ArrayList<>();
        }

        GaiaOctreeCoordinate current = new GaiaOctreeCoordinate();
        current.setDepthAndCoord(depth, x, y, z);
        resultFullPath.add(current);
        while (current.getDepth() > 0) {
            current = current.getParentCoord();
            resultFullPath.add(current);
        }

        // reverse the list
        Collections.reverse(resultFullPath);

        return resultFullPath;
    }

    public boolean isEqual(GaiaOctreeCoordinate coord) {
        return this.depth == coord.depth && this.x == coord.x && this.y == coord.y && this.z == coord.z;
    }

    public GaiaOctreeIndex getOctreeIndex() {
        // children indices.
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

        GaiaOctreeCoordinate parentCoord = this.getParentCoord();
        if (parentCoord == null) {
            return GaiaOctreeIndex.LEFT_FRONT_BOTTOM;
        }

        int x = this.getX();
        int y = this.getY();
        int z = this.getZ();

        int parentX = parentCoord.getX();
        int parentY = parentCoord.getY();
        int parentZ = parentCoord.getZ();

        int originX = parentX * 2;
        int originY = parentY * 2;
        int originZ = parentZ * 2;

        int difX = x - originX;
        int difY = y - originY;
        int difZ = z - originZ;

        int index;
        if (difX > 0) {
            // 1, 2, 5, 6
            if (difY > 0) {
                // 2, 6
                if (difZ > 0) {
                    // 6
                    index = 6;
                } else {
                    // 2
                    index = 2;
                }
            } else {
                // 1, 5
                if (difZ > 0) {
                    // 5
                    index = 5;
                } else {
                    // 1
                    index = 1;
                }
            }
        } else {
            // 0, 3, 4, 7
            if (difY > 0) {
                // 3, 7
                if (difZ > 0) {
                    // 7
                    index = 7;
                } else {
                    // 3
                    index = 3;
                }
            } else {
                // 0, 4
                if (difZ > 0) {
                    // 4
                    index = 4;
                } else {
                    // 0
                    index = 0;
                }
            }
        }

        return GaiaOctreeIndex.fromIndex(index);
    }
}
