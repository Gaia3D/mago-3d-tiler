package com.gaia3d.release;

import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
class RealisticReleaseTest {
    private static final String INPUT_PATH = "D:/data/mago-3d-tiler/release-sample";
    private static final String OUTPUT_PATH = "D:/data/mago-server/output";

    @Test
    void realistic00() {
        String path = "R00-bansong-obj";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-pr",
                "-c", "5187",
        };
        execute(args);
    }

    @Test
    void realistic01() {
        String path = "R01-bansong-part-obj";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-pr",
                "-c", "5187",
                "-debug",
        };
        execute(args);
    }

    @Test
    void realistic02() {
        String path = "R02-bansong-all-obj";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-pr",
                "-c", "5187",
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
