package com.gaia3d.visual.experimental;

import com.gaia3d.visual.MagoTestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Slf4j
public class WildfireExperimentalTest {

    @Test
    void koreaForestService() {
        String path = "korea-forest-service";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5179",
                "-ot", "i3dm",
                "-instance", MagoTestConfig.getTempPath(path).getAbsolutePath() + "/instance.glb",
                "-terrain", MagoTestConfig.getTerrainPath("dem05-cog.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }
}
