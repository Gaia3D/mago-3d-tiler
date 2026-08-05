package com.gaia3d.local.experimental;

import com.gaia3d.local.MagoTestConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("experimental")
public class GeoDataCubeOriginalTest {

    @Test
    void voxel01() {
        String input = "D:\\data\\mago-voxelizer\\input\\";
        String path = "geostory-intersection\\geostory-intersection-01";
        String[] args = new String[]{
                "-i", input + path,
                "-c", "32652",
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-r"
        };
        MagoTestConfig.execute(args);
    }
}
