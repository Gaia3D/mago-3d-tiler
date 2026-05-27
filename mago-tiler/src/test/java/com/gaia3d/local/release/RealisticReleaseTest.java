package com.gaia3d.local.release;

import com.gaia3d.command.mago.Mago3DTilerMain;
import com.gaia3d.local.MagoTestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("release")
@Slf4j
class RealisticReleaseTest {

    @Test
    void realistic01() {
        String path = "R01-bansong-part-obj";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-it", "obj",
                "-log", MagoTestConfig.getLogPath(path).getAbsolutePath(),
                "-pg",
                "-c", "5187",
                "-leaveTemp",
                "-rotateX", "90",
                //"-debug",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void realistic031() {
        String path = "R03-gilcheon-part-obj";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-log", MagoTestConfig.getLogPath(path).getAbsolutePath(),
                "-pg",
                "-c", "5187",
                "-rotateX", "90",
                //"-debug",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void realistic051() {
        String path = "R05-sangcheon-part-obj";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-log", MagoTestConfig.getLogPath(path).getAbsolutePath(),
                "-pg",
                "-c", "5187",
                "-rotateX", "90",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void realistic06() {
        String path = "R06-khonkhan-part-obj";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-log", MagoTestConfig.getLogPath(path).getAbsolutePath(),
                "-pg",
                "-c", "32648",
                "-rotateX", "90",
        };
        MagoTestConfig.execute(args);
    }


    private void execute(String[] args) {
        Mago3DTilerMain.main(args);
    }
}
