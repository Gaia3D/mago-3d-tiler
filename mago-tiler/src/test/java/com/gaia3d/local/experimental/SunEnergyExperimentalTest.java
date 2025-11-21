package com.gaia3d.local.experimental;

import com.gaia3d.local.MagoTestConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("experimental")
public class SunEnergyExperimentalTest {

    @Test
    void sunEnergyTest01() {
        String path = "sun-energy-test";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                //"-rotateXAxis", "90",
                "-splitByNode",
                "-crs", "5179",
        };
        MagoTestConfig.execute(args);
    }
}
