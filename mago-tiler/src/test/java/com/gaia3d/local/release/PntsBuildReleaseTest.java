package com.gaia3d.local.release;

import com.gaia3d.local.MagoTestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("release")
@Slf4j
class PntsBuildReleaseTest {

    @Test
    void pointcloud14A() {
        String path = "P14-KyungSan-A";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-crs", "5187",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud14B() {
        String path = "P14-KyungSan-B";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-crs", "5187",
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
                //"--pointRatio", "100",
                "--force4ByteRGB"
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
    void pointcloud13() {
        String path = "P13-Khonkaen";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-crs", "32648",
        };
        MagoTestConfig.execute(args);
    }
}
