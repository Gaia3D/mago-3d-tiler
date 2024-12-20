package com.gaia3d.release.old;

import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;

@Deprecated
@Slf4j
class CityGmlTest {
    private static final String INPUT_PATH = "D:\\data\\mago-tiler-data\\release-test-input";
    private static final String OUTPUT_PATH = "D:/data/mago-server/output";

    @Test
    void citygml01() {
        String path = "CITYGML-1";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "citygml",
                "-crs", "5186",
                "-output", output.getAbsolutePath(),
                "-debug",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void citygml02() {
        String path = "CITYGML-2";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "citygml",
                "-crs", "5186",
                "-output", output.getAbsolutePath(),
                "-debug",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void citygml03() {
        String path = "CITYGML-3";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "citygml",
                "-crs", "5186",
                "-output", output.getAbsolutePath(),
                "-debug",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void citygml04() {
        String path = "CITYGML-4";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "citygml",
                "-crs", "5186",
                "-output", output.getAbsolutePath(),
                "-debug",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void citygml05() {
        String path = "CITYGML-5";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "citygml",
                "-crs", "5186",
                "-output", output.getAbsolutePath(),
                "-debug",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void citygml06() {
        String path = "CITYGML-6";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "citygml",
                "-crs", "5186",
                "-output", output.getAbsolutePath(),
                "-debug",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void citygml07() {
        String path = "CITYGML-7";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "citygml",
                "-crs", "5186",
                "-output", output.getAbsolutePath(),
                "-debug",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void citygml08() {
        String path = "CITYGML-8";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "citygml",
                "-crs", "5186",
                "-output", output.getAbsolutePath(),
                "-debug",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void citygml09() {
        String path = "CITYGML-9";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "citygml",
                "-crs", "5186",
                "-output", output.getAbsolutePath(),
                "-debug",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void citygml10() {
        String path = "CITYGML-10";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "citygml",
                "-crs", "5186",
                "-output", output.getAbsolutePath(),
                "-debug",
        };
        Mago3DTilerMain.main(args);
    }

    private File getInputPath(String path) {
        return new File(INPUT_PATH, path);
    }

    private File getOutputPath(String path) {
        return new File(OUTPUT_PATH, path);
    }
}
