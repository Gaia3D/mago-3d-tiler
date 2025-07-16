package com.gaia3d.release.small;

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
                "-i", ReleaseTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "32652",
                "-tilesVersion", "1.0",
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void pointcloud00V2() {
        String path = "P00-hwangyonggak-las";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath() + "-V2",
                "-c", "32652",
                "-tilesVersion", "1.1",
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void pointcloud00Offset() {
        String path = "P00-hwangyonggak-las";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath() + "-offset",
                "-zOffset", "100.0",
                "-xOffset", "100.0",
                "-yOffset", "100.0",
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void pointcloud01() {
        String path = "P01-sejong-bridge-las";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void pointcloud02() {
        String path = "P02-busan-jingu-las";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
                "-debug",
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void pointcloud03() {
        String path = "P03-thai-khonkaen-las";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "32648",
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void pointcloud04() {
        String path = "P04-github-posikifi-las";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void pointcloud05() {
        String path = "P05-west-honam-las";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "32652",
                "-debug",
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void pointcloud06() {
        String path = "P06-classification-las";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        ReleaseTestConfig.execute(args);
    }
}
