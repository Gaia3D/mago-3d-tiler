package com.gaia3d.basic.geometry.octree;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Setter
@Getter

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

    public GaiaOctreeCoordinate getLeft()
    {
        GaiaOctreeCoordinate left = new GaiaOctreeCoordinate();
        left.setDepthAndCoord(depth, x - 1, y, z);
        return left;
    }

    public GaiaOctreeCoordinate getRight()
    {
        GaiaOctreeCoordinate right = new GaiaOctreeCoordinate();
        right.setDepthAndCoord(depth, x + 1, y, z);
        return right;
    }

    public GaiaOctreeCoordinate getFront()
    {
        GaiaOctreeCoordinate front = new GaiaOctreeCoordinate();
        front.setDepthAndCoord(depth, x, y - 1, z);
        return front;
    }

    public GaiaOctreeCoordinate getRear()
    {
        GaiaOctreeCoordinate back = new GaiaOctreeCoordinate();
        back.setDepthAndCoord(depth, x, y + 1, z);
        return back;
    }

    public GaiaOctreeCoordinate getTop()
    {
        GaiaOctreeCoordinate top = new GaiaOctreeCoordinate();
        top.setDepthAndCoord(depth, x, y, z + 1);
        return top;
    }

    public GaiaOctreeCoordinate getBottom()
    {
        GaiaOctreeCoordinate bottom = new GaiaOctreeCoordinate();
        bottom.setDepthAndCoord(depth, x, y, z - 1);
        return bottom;
    }

    public GaiaOctreeCoordinate getParentCoord()
    {
        if(this.depth == 0)
        {
            return null;
        }

        GaiaOctreeCoordinate parent = new GaiaOctreeCoordinate();
        parent.setDepthAndCoord(depth - 1, x / 2, y / 2, z / 2);
        return parent;
    }

    public List<GaiaOctreeCoordinate> getFullPath(List<GaiaOctreeCoordinate> resultFullPath)
    {
        if(resultFullPath == null)
        {
            resultFullPath = new ArrayList<>();
        }

        GaiaOctreeCoordinate current = new GaiaOctreeCoordinate();
        current.setDepthAndCoord(depth, x, y, z);
        resultFullPath.add(current);
        while(current.getDepth() > 0)
        {
            current = current.getParentCoord();
            resultFullPath.add(current);
        }

        // reverse the list
        Collections.reverse(resultFullPath);

        return resultFullPath;
    }

    public boolean isEqual(GaiaOctreeCoordinate coord)
    {
        return this.depth == coord.depth && this.x == coord.x && this.y == coord.y && this.z == coord.z;
    }


}
