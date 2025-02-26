package com.gaia3d.release;

import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
class RealisticReleaseTest {
    private static final String INPUT_PATH = "D:/data/mago-3d-tiler/release-sample";
    private static final String OUTPUT_PATH = "E:/data/mago-server/output";

    @Disabled
    @Test
    void realistic00() {
        String path = "R00-bansong-obj";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-log", getLogPath(path).getAbsolutePath(),
                "-pr",
                "-c", "5187",
                "-rotateX", "90",
                "-leaveTemp",
                //"-glb",
                "-debug",
        };
        execute(args);
    }

    @Disabled
    @Test
    void realistic01() {
        String path = "R01-bansong-part-obj";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-log", getLogPath(path).getAbsolutePath(),
                "-pr",
                "-c", "5187",
                "-leaveTemp",
                "-rotateX", "90",
                //"-debug",
        };
        execute(args);
    }

    @Test
    void realistic02() {
        String path = "R02-bansong-all-obj";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-log", getLogPath(path).getAbsolutePath(),
                "-pr",
                "-c", "5187",
                "-rotateX", "90",
                "-leaveTemp",
                //"-debug",
        };
        execute(args);
    }

    @Disabled
    @Test
    void realistic03() {
        String path = "R03-gilcheon-part-obj";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-log", getLogPath(path).getAbsolutePath(),
                "-pr",
                "-c", "5187",
                "-rotateX", "90",
                //"-debug",
        };
        execute(args);
    }

    @Test
    void realistic04() {
        String path = "R04-gilcheon-all-obj";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-log", getLogPath(path).getAbsolutePath(),
                "-pr",
                "-c", "5187",
                "-rotateX", "90",
                //"-debug",
        };
        execute(args);
    }

    @Test
    void realistic05() {
        String path = "R05-sangcheon-all-obj";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-log", getLogPath(path).getAbsolutePath(),
                "-pr",
                "-c", "5187",
                "-rotateX", "90",
                //"-debug",
        };
        execute(args);
    }

    @Test
    void realistic051() {
        String path = "R05-sangcheon-part-obj";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-log", getLogPath(path).getAbsolutePath(),
                "-pr",
                "-c", "5187",
                "-rotateX", "90",
                //"-debug",
        };
        execute(args);
    }

    @Disabled
    @Test
    void realistic06() {
        String path = "R06-khonkhan-part-obj";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-log", getLogPath(path).getAbsolutePath(),
                "-pr",
                "-c", "32648",
                //"-rotateX", "90",
                //"-debug",
        };
        execute(args);
    }

    @Disabled
    @Test
    void realistic07() {
        String path = "R07-sejong-bridge-ifc";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "-pr",
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

    private File getLogPath(String path) {
        File logPath = new File(OUTPUT_PATH, path);
        return new File(logPath, "log.txt");
    }
}
