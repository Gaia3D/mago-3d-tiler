package com.gaia3d.visual.experimental;

import com.gaia3d.visual.MagoTestConfig;
import org.junit.jupiter.api.Test;

public class SunEnergyExperimentalTest {

    @Test
    void sunEnergyTest01() {
        String path = "sun-energy-test";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                //"-rotateXAxis", "90",
                "-crs", "5179",
        };
        MagoTestConfig.execute(args);
    }
}
