package com.gaia3d.basic.geometry.voxel;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

@Slf4j
@Getter
@Setter

public class PositionedVoxel3DGrid {
    private int gridsCountX;
    private int gridsCountY;
    private int gridsCountZ;
    private PositionedVoxel[][][] voxels;
    private double[] minMaxValues;

    public PositionedVoxel3DGrid(int gridsCountX, int gridsCountY, int gridsCountZ) {
        this.gridsCountX = gridsCountX;
        this.gridsCountY = gridsCountY;
        this.gridsCountZ = gridsCountZ;
        this.voxels = new PositionedVoxel[gridsCountX][gridsCountY][gridsCountZ];

        for (int x = 0; x < gridsCountX; x++) {
            for (int y = 0; y < gridsCountY; y++) {
                for (int z = 0; z < gridsCountZ; z++) {
                    voxels[x][y][z] = new PositionedVoxel();
                }
            }
        }
    }

    public void expand(int expandX, int expandY, int expandZ) {
        // expand one in positive direction and negative direction
        int newGridsCountX = gridsCountX + expandX * 2;
        int newGridsCountY = gridsCountY + expandY * 2;
        int newGridsCountZ = gridsCountZ + expandZ * 2;

        PositionedVoxel[][][] newVoxels = new PositionedVoxel[newGridsCountX][newGridsCountY][newGridsCountZ];
        for (int x = 0; x < newGridsCountX; x++) {
            for (int y = 0; y < newGridsCountY; y++) {
                for (int z = 0; z < newGridsCountZ; z++) {
                    newVoxels[x][y][z] = new PositionedVoxel();
                }
            }
        }
        for (int x = 0; x < gridsCountX; x++) {
            for (int y = 0; y < gridsCountY; y++) {
                for (int z = 0; z < gridsCountZ; z++) {
                    newVoxels[x + expandX][y + expandY][z + expandZ] = voxels[x][y][z];
                }
            }
        }
        this.gridsCountX = newGridsCountX;
        this.gridsCountY = newGridsCountY;
        this.gridsCountZ = newGridsCountZ;
        this.voxels = newVoxels;

    }

    public PositionedVoxel getVoxel(int x, int y, int z) {
        if (x < 0 || x >= gridsCountX || y < 0 || y >= gridsCountY || z < 0 || z >= gridsCountZ) {
            return null;
        }
        return voxels[x][y][z];
    }

    public double getVoxelValue(int x, int y, int z) {
        return voxels[x][y][z].getValue();
    }

    public Vector3d getVoxelPosition(int x, int y, int z) {
        return voxels[x][y][z].getPosition();
    }
}
