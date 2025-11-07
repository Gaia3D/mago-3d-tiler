package com.gaia3d.basic.remesher;

import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;
import org.joml.Vector3i;

@Getter
@Setter

public class CellGrid3D {
    public Vector3d origin; // origen del grid global
    public double cellSize;

    public CellGrid3D(Vector3d origin, double cellSize) {
        this.origin = origin;
        this.cellSize = cellSize;
    }

    // Return the cell index that contains the point v
    public Vector3i getCellIndex(Vector3d v) {
        // epsilon = 1e-6 for double precision issues
        // epsilon = 1e-4 for float precision issues
        double epsilon = 1e-6;
        int ix = (int) Math.floor(((v.x - origin.x) / cellSize + epsilon));
        int iy = (int) Math.floor(((v.y - origin.y) / cellSize + epsilon));
        int iz = (int) Math.floor(((v.z - origin.z) / cellSize + epsilon));
        return new Vector3i(ix, iy, iz);
    }

    public Vector3i getCellIndex_original(Vector3d v) {
        int ix = (int) Math.floor((v.x - origin.x) / cellSize);
        int iy = (int) Math.floor((v.y - origin.y) / cellSize);
        int iz = (int) Math.floor((v.z - origin.z) / cellSize);
        return new Vector3i(ix, iy, iz);
    }

    // Returns the center of the cell (useful for remeshing)
    public Vector3d getCellCenter(Vector3i index) {
        return new Vector3d(
                origin.x + (index.x + 0.5) * cellSize,
                origin.y + (index.y + 0.5) * cellSize,
                origin.z + (index.z + 0.5) * cellSize
        );
    }

    public Vector3d getCellPositionMin(Vector3i index) {
        return new Vector3d(
                origin.x + index.x * cellSize,
                origin.y + index.y * cellSize,
                origin.z + index.z * cellSize
        );
    }

    public Vector3d getCellPositionMax(Vector3i index) {
        return new Vector3d(
                origin.x + (index.x + 1) * cellSize,
                origin.y + (index.y + 1) * cellSize,
                origin.z + (index.z + 1) * cellSize
        );
    }
}
