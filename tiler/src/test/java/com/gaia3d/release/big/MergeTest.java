package com.gaia3d.release.big;

import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
class MergeTest {
    private static final String INPUT_PATH = "E:/data/mago-server/output";
    private static final String OUTPUT_PATH = "E:/data/mago-server/output";

    @Test
    void merge00() {
        String name = "output-building-tiles";
        File inputPath = getInputPath(name);
        File outputPath = getOutputPath(name);
        try {
            String[] args = new String[]{
                    "-i", inputPath.getAbsolutePath(),
                    "-o", outputPath.getAbsolutePath(),
                    "-merge",
            };
            execute(args);
        } catch (Exception e) {
            log.error("[ERROR] : e");
        }
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
