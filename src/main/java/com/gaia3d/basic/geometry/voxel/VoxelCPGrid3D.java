package com.gaia3d.basic.geometry.voxel;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

@Slf4j
@Getter
@Setter

public class VoxelCPGrid3D {
    private int gridsCountX;
    private int gridsCountY;
    private int gridsCountZ;
    private VoxelCP[][][] voxels;
    private double[] minMaxValues;

    public VoxelCPGrid3D(int gridsCountX, int gridsCountY, int gridsCountZ) {
        this.gridsCountX = gridsCountX;
        this.gridsCountY = gridsCountY;
        this.gridsCountZ = gridsCountZ;
        this.voxels = new VoxelCP[gridsCountX][gridsCountY][gridsCountZ];

        for (int x = 0; x < gridsCountX; x++) {
            for (int y = 0; y < gridsCountY; y++) {
                for (int z = 0; z < gridsCountZ; z++) {
                    voxels[x][y][z] = new VoxelCP();
                }
            }
        }
    }

    public VoxelCP getVoxel(int x, int y, int z) {
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
