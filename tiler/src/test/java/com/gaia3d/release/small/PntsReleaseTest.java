package com.gaia3d.release.small;

import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;

@Tag("release")
@Slf4j
class PntsReleaseTest {
    private static final String INPUT_PATH = "D:/data/mago-3d-tiler/release-sample";
    private static final String OUTPUT_PATH = "E:/data/mago-server/output";

    @Test
    void pointcloud00() {
        String path = "P00-hwangyonggak-las";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "32652",
        };
        execute(args);
    }

    @Test
    void pointcloud00Offset() {
        String path = "P00-hwangyonggak-las";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath() + "-offset",
                "-zOffset", "100.0",
                "-xOffset", "100.0",
                "-yOffset", "100.0",
                //"-c", "32652",
        };
        execute(args);
    }

    @Test
    void pointcloud01() {
        String path = "P01-sejong-bridge-las";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-c", "5186"
        };
        execute(args);
    }

    @Test
    void pointcloud02() {
        String path = "P02-busan-jingu-las";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-c", "5187",
                "-pointRatio", "100",
                "-debug",
        };
        execute(args);
    }

    @Test
    void pointcloud03() {
        String path = "P03-thai-khonkaen-las";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "32648",
                "-pointRatio", "25",
        };
        execute(args);
    }

    @Test
    void pointcloud04() {
        String path = "P04-github-posikifi-las";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-c", "5186",
                //"-pointRatio", "25",
        };
        execute(args);
    }

    @Test
    void pointcloud05() {
        String path = "P05-west-honam-las";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "32652",
                "-debug",
        };
        execute(args);
    }

    @Test
    void pointcloud06() {
        String path = "P06-classification-las";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
        };
        execute(args);
    }

    private void execute(String[] args) {
        Mago3DTilerMain.main(args);
    }

    private File getInputPath(String path) {
        return new File(INPUT_PATH, path);
    }

    private File getOutputPath(String path) {
        return new File(OUTPUT_PATH, path);
    }
}
