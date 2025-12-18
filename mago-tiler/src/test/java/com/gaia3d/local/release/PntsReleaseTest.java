package com.gaia3d.local.release;

import com.gaia3d.local.MagoTestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

@Tag("release")
@Slf4j
class PntsReleaseTest {

    @Test
    void pointcloud00() {
        String path = "P00-hwangyonggak-las";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "32652",
                "--geoid", "EGM96",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud00OLD() {
        String path = "P00-hwangyonggak-las";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-OLD",
                "-c", "32652",
                "-tilesVersion", "1.0",
                "--temp", "C:\\temp\\",
        };
        MagoTestConfig.execute(args);
    }

    /*@Disabled
    @Test
    void pointcloud00SSD() {
        String[] args = new String[]{
                "-i", Path.of("C:\\Workspace\\mago-3d-tiler\\P00-hwangyonggak-las").toAbsolutePath().toString(),
                "-o", Path.of("C:\\Workspace-data\\mago-3d-tiler\\P00-hwangyonggak-las-3dtiles").toAbsolutePath().toString(),
                "-c", "32652",
                "--temp", "C:\\temp\\",
                "--quiet",
        };
        MagoTestConfig.execute(args);
    }*/

    @Test
    void pointcloud00V2WrongCRS() {
        String path = "P00-hwangyonggak-las";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-V2",
                "-c", "4326",
                "--temp", "C:\\temp\\",
        };
        try {
            MagoTestConfig.execute(args);
        } catch (Exception e) {
            log.info("Expected exception caught: {}", e.getMessage());
        }
    }

    @Test
    void pointcloud00OffsetA() {
        String path = "P00-hwangyonggak-las";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-offset-300-300-100",
                "-c", "32652",
                "-zOffset", "300.0",
                "-xOffset", "300.0",
                "-yOffset", "100.0",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud01() {
        String path = "P01-sejong-bridge-las";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "--temp", "C:\\temp\\",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud02() {
        String path = "P02-busan-jingu-las";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5187",
                "--temp", "C:\\temp\\",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud03() {
        String path = "P03-khonkaen-mini";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "32648",
                "--temp", "C:\\temp\\",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud04() {
        String path = "P04-github-posikifi-las";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "--temp", "C:\\temp\\",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud05() {
        String path = "P05-west-honam-las";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "32652",
                "--temp", "C:\\temp\\",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud06() {
        String path = "P06-classification-las";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "--temp", "C:\\temp\\",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud07() {
        String path = "P07-cube-points";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "--temp", "C:\\temp\\",
                "-crs", "5186",
        };
        MagoTestConfig.execute(args);
    }
}
