package com.gaia3d.visual.release;

import com.gaia3d.visual.MagoTestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("release")
@Slf4j
class PntsReleaseTest {
    @Test
    void pointcloud00() {
        String path = "P00-hwangyonggak-las";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "32652",
                "-tilesVersion", "1.0",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud00V2() {
        String path = "P00-hwangyonggak-las";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-V2",
                "-c", "32652",
                "-tilesVersion", "1.1",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud00OffsetA() {
        String path = "P00-hwangyonggak-las";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-offset-100-100-100",
                "-zOffset", "100.0",
                "-xOffset", "100.0",
                "-yOffset", "100.0",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud00OffsetB() {
        String path = "P00-hwangyonggak-las";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-offset-100-100-0",
                "-xOffset", "100.0",
                "-yOffset", "100.0",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud01() {
        String path = "P01-sejong-bridge-las";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud02() {
        String path = "P02-busan-jingu-las";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "-debug",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud03() {
        String path = "P03-thai-khonkaen-las";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "32648",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud04() {
        String path = "P04-github-posikifi-las";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud05() {
        String path = "P05-west-honam-las";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "32652",
                "-debug",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud06() {
        String path = "P06-classification-las";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }
}
