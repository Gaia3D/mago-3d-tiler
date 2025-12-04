package com.gaia3d.local.release;

import com.gaia3d.local.MagoTestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
                "-tilesVersion", "1.0",
                "--temp", "C:\\temp\\",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud00V2() {
        String path = "P00-hwangyonggak-las";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-V2",
                "-c", "32652",
                "-tilesVersion", "1.1",
                "--temp", "C:\\temp\\",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud00OffsetA() {
        String path = "P00-hwangyonggak-las";
        String[] args = new String[]{
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
        String[] args = new String[]{
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
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
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
    void pointcloud02Big() {
        String path = "P02-busan-big-jingu-las";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5187",
                /*"--pointRatio", "10",*/
                "--temp", "C:\\temp\\",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud03() {
        String path = "P03-thai-khonkaen-las";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "32648",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud04() {
        String path = "P04-github-posikifi-las";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
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
                "-debug",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud06() {
        String path = "P06-classification-las";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud07() {
        String path = "P07-cube-points";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-crs", "5186",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud08() {
        String path = "P08-honam-expressway-las";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-crs", "5186",
                "--temp", "C:\\temp\\",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud10() {
        String path = "P10-jeonju";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-crs", "5186",
                "--temp", "C:\\temp\\",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud11() {
        String path = "P11-gwangju";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-crs", "5174",
                "--temp", "C:\\temp\\",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud12() {
        String path = "P12-sangji-university";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-crs", "5186",
                "--temp", "C:\\temp\\",
                "--pointRatio", "25",
                "--force4ByteRGB"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud13() {
        String path = "P13-Khonkaen";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-crs", "32648",
                "--temp", "H:\\temp\\",
                //"--pointRatio", "1",
        };
        MagoTestConfig.execute(args);
    }
}
