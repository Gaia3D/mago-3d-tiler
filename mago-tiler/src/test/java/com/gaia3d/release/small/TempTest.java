package com.gaia3d.release.small;

import org.junit.jupiter.api.Test;

public class TempTest {
    @Test
    void incheon() {
        String path = "incheon";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                "-c", "5186"
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void lottemartV3() {
        String path = "lottemartV3.gltf";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                "-c", "5186"
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void d6_4326() {
        String path = "d6_4326.geojson";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
                "-debug",
                "-c", "4326"
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void d6_4326_3D() {
        String path = "d6_4326_3D.geojson";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
                "-debug",
                "-c", "4326",
        };
        ReleaseTestConfig.execute(args);
    }
}
