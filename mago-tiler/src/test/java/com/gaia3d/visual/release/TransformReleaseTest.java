package com.gaia3d.visual.release;

import com.gaia3d.visual.MagoTestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("release")
@Slf4j
class TransformReleaseTest {

    @Test
    void batched00Original() {
        String path = "B00-up-axis-glb";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched00RotateX90() {
        String path = "B00-up-axis-glb";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-rotateX90(manual)",
                "-c", "5186",
                "-rotateXAxis", "90",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched00DegreeZ() {
        String path = "B00-up-axis-glb";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-127-37-manual-0-0-0",
                //"-c", "5186",
                "-rotateXAxis", "90",
                "-lon", "127.0",
                "-lat", "37.0",
                "-xOffset", "0",
                "-yOffset", "0",
                "-zOffset", "0",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched00DegreeA() {
        String path = "B00-up-axis-glb";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-127-37-manual-10-0-0",
                //"-c", "5186",
                "-rotateXAxis", "90",
                "-lon", "127.0",
                "-lat", "37.0",
                "-xOffset", "10",
                "-yOffset", "0",
                "-zOffset", "0",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched00DegreeB() {
        String path = "B00-up-axis-glb";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-127-37-manual-0-10-0",
                //"-c", "5186",
                "-rotateXAxis", "90",
                "-lon", "127.0",
                "-lat", "37.0",
                "-xOffset", "0",
                "-yOffset", "10",
                "-zOffset", "0",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched00DegreeC() {
        String path = "B00-up-axis-glb";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-127-37-manual-0-0-10",
                //"-c", "5186",
                "-rotateXAxis", "90",
                "-lon", "127.0",
                "-lat", "37.0",
                "-xOffset", "0",
                "-yOffset", "0",
                "-zOffset", "10",
        };
        MagoTestConfig.execute(args);
    }


    @Test
    void batched00DegreeZWithTerrain() {
        String path = "B00-up-axis-glb";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-127-37-manual-0-0-0-terrain",
                //"-c", "5186",
                "-rotateXAxis", "90",
                "-lon", "127.0",
                "-lat", "37.0",
                "-xOffset", "0",
                "-yOffset", "0",
                "-zOffset", "0",
                "-terrain", MagoTestConfig.getTerrainPath("geoided-aster-southkorea.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched00DegreeAWithTerrain() {
        String path = "B00-up-axis-glb";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-127-37-manual-10-0-0-terrain",
                //"-c", "5186",
                "-rotateXAxis", "90",
                "-lon", "127.0",
                "-lat", "37.0",
                "-xOffset", "10",
                "-yOffset", "0",
                "-zOffset", "0",
                "-terrain", MagoTestConfig.getTerrainPath("geoided-aster-southkorea.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched00DegreeBWithTerrain() {
        String path = "B00-up-axis-glb";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-127-37-manual-0-10-0-terrain",
                //"-c", "5186",
                "-rotateXAxis", "90",
                "-lon", "127.0",
                "-lat", "37.0",
                "-xOffset", "0",
                "-yOffset", "10",
                "-zOffset", "0",
                "-terrain", MagoTestConfig.getTerrainPath("geoided-aster-southkorea.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched00DegreeCWithTerrain() {
        String path = "B00-up-axis-glb";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-127-37-manual-0-0-10-terrain",
                //"-c", "5186",
                "-rotateXAxis", "90",
                "-lon", "127.0",
                "-lat", "37.0",
                "-xOffset", "0",
                "-yOffset", "0",
                "-zOffset", "10",
                "-terrain", MagoTestConfig.getTerrainPath("geoided-aster-southkorea.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }
}
