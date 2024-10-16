package com.gaia3d.release;

import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
class BatchTest {
    private static final String INPUT_PATH = "D:\\data\\mago-tiler-data\\release-test-input";
    private static final String OUTPUT_PATH = "C:\\Workspaces\\GitSources\\mago-viewer\\data\\release-test-output";

    private File getInputPath(String path) {
        return new File(INPUT_PATH, path);
    }

    private File getOutputPath(String path) {
        return new File(OUTPUT_PATH, path);
    }

    private void run(String folderName) {
        File input = new File(INPUT_PATH, folderName);
        if (!input.exists()) {
            log.warn("Not found: {}", input);
            return;
        }

        File output = new File(OUTPUT_PATH, folderName);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case01() {
        run("case01");
    }

    @Test
    void case02() {
        run("case02");
    }

    @Test
    void case03() {
        run("case03");
    }

}
