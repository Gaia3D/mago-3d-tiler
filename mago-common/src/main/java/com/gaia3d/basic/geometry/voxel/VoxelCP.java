package com.gaia3d.basic.geometry.voxel;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

@Slf4j
@Getter
@Setter

public class VoxelCP {
    // voxel custom position class.***
    // This class is used to represent a voxel with a custom position in 3D space.
    private double value = 0.0;
    private Vector3d position = new Vector3d(0.0, 0.0, 0.0);
}
