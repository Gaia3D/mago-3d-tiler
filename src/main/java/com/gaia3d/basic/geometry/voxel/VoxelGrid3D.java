package com.gaia3d.basic.geometry.voxel;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

@Getter
@Setter
@Slf4j
/**
 * Class representing a 3D grid of voxels.
 * Each voxel can hold a color value.
 */
public class VoxelGrid3D {
    private int gridsCountX;
    private int gridsCountY;
    private int gridsCountZ;
    private Voxel[][][] voxels;
    private GaiaBoundingBox boundingBox;

    public VoxelGrid3D(int gridsCountX, int gridsCountY, int gridsCountZ, GaiaBoundingBox boundingBox) {
        this.gridsCountX = gridsCountX;
        this.gridsCountY = gridsCountY;
        this.gridsCountZ = gridsCountZ;
        this.voxels = new Voxel[gridsCountX][gridsCountY][gridsCountZ];
        this.boundingBox = boundingBox;

        for (int x = 0; x < gridsCountX; x++) {
            for (int y = 0; y < gridsCountY; y++) {
                for (int z = 0; z < gridsCountZ; z++) {
                    voxels[x][y][z] = new Voxel();
                }
            }
        }
    }

    public Voxel getVoxel(int x, int y, int z) {
        if (x < 0 || x >= gridsCountX || y < 0 || y >= gridsCountY || z < 0 || z >= gridsCountZ) {
            return null;
        }
        return voxels[x][y][z];
    }

    public float getVoxelAlphaFloat(int x, int y, int z) {
        return voxels[x][y][z].getAlphaFloat();
    }

    public void setVoxelsByAlphaXY(int gridZ, byte[] bufferArray) {
        int index = 0;
        for (int y = 0; y < gridsCountY; y++) {
            for (int x = 0; x < gridsCountX; x++) {
                byte r = bufferArray[index++];
                byte g = bufferArray[index++];
                byte b = bufferArray[index++];
                byte a = bufferArray[index++];

                int alphaInt = a & 0xFF;
                Voxel voxel = getVoxel(x, y, gridZ);
                int existentAlphaInt = voxel.getAlphaInt();

                if (existentAlphaInt < alphaInt) {
                    voxel.setByteColor4(r, g, b, a);
                }
            }
        }
    }

    public void setVoxelsByAlphaXZ(int gridY, byte[] bufferArray) {
        int index = 0;
        for (int z = 0; z < gridsCountZ; z++) {
            for (int x = 0; x < gridsCountX; x++) {
                byte r = bufferArray[index++];
                byte g = bufferArray[index++];
                byte b = bufferArray[index++];
                byte a = bufferArray[index++];


                int alphaInt = (int) a & 0xFF;

                Voxel voxel = getVoxel(x, gridY, z);
                int existentAlphaInt = voxel.getAlphaInt();

                if (existentAlphaInt < alphaInt) {
                    voxel.setByteColor4(r, g, b, a);
                }
            }
        }
    }

    public void setVoxelsByAlphaYZ(int gridX, byte[] bufferArray) {
        int index = 0;
        for (int z = 0; z < gridsCountZ; z++) {
            for (int y = gridsCountY - 1; y >= 0; y--) {
                byte r = bufferArray[index++];
                byte g = bufferArray[index++];
                byte b = bufferArray[index++];
                byte a = bufferArray[index++];

                int alphaInt = a & 0xFF;

                Voxel voxel = getVoxel(gridX, y, z);
                int existentAlphaInt = voxel.getAlphaInt();

                if (existentAlphaInt < alphaInt) {
                    voxel.setByteColor4(r, g, b, a);
                }
            }
        }
    }

    public Vector3d getVoxelPosition(int x, int y, int z) {
        double voxelX = boundingBox.getMinX() + (boundingBox.getMaxX() - boundingBox.getMinX()) * ((double) x / (double) gridsCountX);
        double voxelY = boundingBox.getMinY() + (boundingBox.getMaxY() - boundingBox.getMinY()) * ((double) y / (double) gridsCountY);
        double voxelZ = boundingBox.getMinZ() + (boundingBox.getMaxZ() - boundingBox.getMinZ()) * ((double) z / (double) gridsCountZ);

        return new Vector3d(voxelX, voxelY, voxelZ);
    }

    public void expand(int expandQuantity) {
        // expand 1quantity to left and right, up and down, front and back
        int newGridsCountX = gridsCountX + expandQuantity * 2;
        int newGridsCountY = gridsCountY + expandQuantity * 2;
        int newGridsCountZ = gridsCountZ + expandQuantity * 2;
        Voxel[][][] newVoxels = new Voxel[newGridsCountX][newGridsCountY][newGridsCountZ];
        for (int x = 0; x < newGridsCountX; x++) {
            for (int y = 0; y < newGridsCountY; y++) {
                for (int z = 0; z < newGridsCountZ; z++) {
                    if (x >= expandQuantity && x < gridsCountX + expandQuantity &&
                            y >= expandQuantity && y < gridsCountY + expandQuantity &&
                            z >= expandQuantity && z < gridsCountZ + expandQuantity) {
                        newVoxels[x][y][z] = voxels[x - expandQuantity][y - expandQuantity][z - expandQuantity];
                    } else {
                        newVoxels[x][y][z] = new Voxel();
                    }
                }
            }
        }
        this.gridsCountX = newGridsCountX;
        this.gridsCountY = newGridsCountY;
        this.gridsCountZ = newGridsCountZ;
        this.voxels = newVoxels;
        double minX = boundingBox.getMinX() - (boundingBox.getMaxX() - boundingBox.getMinX()) * ((double) expandQuantity / (double) gridsCountX);
        double minY = boundingBox.getMinY() - (boundingBox.getMaxY() - boundingBox.getMinY()) * ((double) expandQuantity / (double) gridsCountY);
        double minZ = boundingBox.getMinZ() - (boundingBox.getMaxZ() - boundingBox.getMinZ()) * ((double) expandQuantity / (double) gridsCountZ);
        double maxX = boundingBox.getMaxX() + (boundingBox.getMaxX() - boundingBox.getMinX()) * ((double) expandQuantity / (double) gridsCountX);
        double maxY = boundingBox.getMaxY() + (boundingBox.getMaxY() - boundingBox.getMinY()) * ((double) expandQuantity / (double) gridsCountY);
        double maxZ = boundingBox.getMaxZ() + (boundingBox.getMaxZ() - boundingBox.getMinZ()) * ((double) expandQuantity / (double) gridsCountZ);
        boundingBox.setMinX(minX);
        boundingBox.setMinY(minY);
        boundingBox.setMinZ(minZ);
        boundingBox.setMaxX(maxX);
        boundingBox.setMaxY(maxY);
        boundingBox.setMaxZ(maxZ);
    }
}
