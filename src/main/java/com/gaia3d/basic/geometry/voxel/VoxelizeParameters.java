package com.gaia3d.basic.geometry.voxel;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
/**
 * Class representing parameters for voxelization.
 * This class can be extended to include specific parameters for voxelization.
 */
public class VoxelizeParameters {
    private int gridsCountX = 0;
    private int gridsCountY = 0;
    private int gridsCountZ = 0;
    private double voxelsForMeter = 20.0; // 20 voxels per meter as default
    private double texturePixelsForMeter = 20.0; // 20 pixels per meter as default

    public VoxelizeParameters() {
    }
}
