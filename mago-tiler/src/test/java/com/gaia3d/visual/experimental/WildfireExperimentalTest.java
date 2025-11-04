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

    @Test
    void forest5000haReplaceWithAdd() {
        String path = "forest-5000ha-300ha";
        String[] args = new String[] {
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-ReplaceWithAdd",
                "-c", "5186",
                "-ot", "i3dm",
                "-it", "gpkg",
                "--refineAdd",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("dem05-cog.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void forest5000haReplace() {
        String path = "forest-5000ha-300ha";
        String[] args = new String[] {
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-replace",
                "-c", "5186",
                "-ot", "i3dm",
                "-it", "gpkg",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("dem05-cog.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void forest5000haAdd() {
        String path = "forest-5000ha-300ha";
        String[] args = new String[] {
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-add",
                "-c", "5186",
                "-ot", "i3dm",
                "-it", "gpkg",
                "--refineAdd",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("dem05-cog.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }


    @Test
    void seoraksan300ha() {
        String path = "seoraksan-300ha";
        String[] args = new String[] {
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "-ot", "i3dm",
                "-it", "gpkg",
                //"--refineAdd",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("dem05-cog.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void seoraksan600ha() {
        String path = "seoraksan-600ha";
        String[] args = new String[] {
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "-ot", "i3dm",
                "-it", "gpkg",
                "--refineAdd",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("dem05-cog.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void seoraksan1200ha() {
        String path = "seoraksan-1200ha";
        String[] args = new String[] {
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "-ot", "i3dm",
                "-it", "gpkg",
                "--refineAdd",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("dem05-cog.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }
}
