package com.gaia3d.release.old;

import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Deprecated
@Slf4j
class AxisTest {
    private static final String INPUT_PATH = "D:\\data\\mago-tiler-data\\axis-test-input";
    //private static final String OUTPUT_PATH = "D:\\workspaces\\mago-viewer\\data\\axis-test-output";
    private static final String OUTPUT_PATH = "E:/data/mago-server/output";

    @Test
    void zUp() {
        String path = "z-up";
        String[] args = new String[] {
                "-i", getInputPath().getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void zDown() {
        String path = "z-down";
        String[] args = new String[] {
                "-i", getInputPath().getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-rotateX", "180",
                "-flipUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void yUp() {
        String path = "y-up";
        String[] args = new String[] {
                "-i", getInputPath().getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-rotateX", "90",
                "-swapUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void yDown() {
        String path = "y-down";
        String[] args = new String[] {
                "-i", getInputPath().getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-rotateX", "270",
                "-swapUpAxis",
                "-flipUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    private File getInputPath() {
        return new File(INPUT_PATH);
    }

    private File getOutputPath(String path) {
        return new File(OUTPUT_PATH, path);
    }
}
