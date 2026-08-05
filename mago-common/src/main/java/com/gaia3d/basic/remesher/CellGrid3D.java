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
        this.origin = new Vector3d(origin);
        this.cellSize = cellSize;
    }

    public Vector3i getCellIndex(double x, double y, double z) {
        double epsilon = 1e-6;
        int cellX = (int) Math.floor((x - origin.x) / cellSize + epsilon);
        int cellY = (int) Math.floor((y - origin.y) / cellSize + epsilon);
        int cellZ = (int) Math.floor((z - origin.z) / cellSize + epsilon);
        return new Vector3i(cellX, cellY, cellZ);
    }

    // Return the 3D cell index that contains the point v
    public Vector3i getCellIndex(Vector3d v) {
        if (v == null || origin == null || cellSize <= 0.0) {
            return null;
        }

        double epsilon = 1e-6;

        int ix = (int) Math.floor(((v.x - origin.x) / cellSize) + epsilon);
        int iy = (int) Math.floor(((v.y - origin.y) / cellSize) + epsilon);
        int iz = (int) Math.floor(((v.z - origin.z) / cellSize) + epsilon);

        return new Vector3i(ix, iy, iz);
    }

}