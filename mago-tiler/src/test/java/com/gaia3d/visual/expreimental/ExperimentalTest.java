package com.gaia3d.visual.expreimental;

import com.gaia3d.visual.MagoTestConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ExperimentalTest {
    @Test
    void incheon() {
        String path = "incheon";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                "-c", "5186"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void lottemartV3() {
        String path = "lottemartV3.gltf";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                "-c", "5186"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void d6_4326() {
        String path = "d6_4326.geojson";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-debug",
                "-c", "4326"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void d6_4326_3D() {
        String path = "d6_4326_3D.geojson";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-debug",
                "-c", "4326",
        };
        MagoTestConfig.execute(args);
    }

    // EPSG:4978 is a common 3D coordinate system (cartesian coordinate system)
    // ECEF(Earth-Centered Earth-Fixed)
    @Disabled
    @Test
    void batched100() {
        String path = "B100-cartesian-sample";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-crs", "4978",
                //"-rotateXAxis", "90",
        };
        MagoTestConfig.execute(args);
    }
}
