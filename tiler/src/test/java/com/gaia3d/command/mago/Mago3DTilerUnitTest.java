package com.gaia3d.command.mago;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
class Mago3DTilerUnitTest {

    private static final String INPUT_PATH = "D:\\Mago3DTiler-UnitTest\\input";
    private static final String OUTPUT_PATH = "D:\\Mago3DTiler-UnitTest\\output";

    @Test
    void case01() {
        String path = "case01-3ds-ws2";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String args[] = {
            "-input", input.getAbsolutePath(),
            "-inputType", "3ds",
            "-crs", "5186",
            "-output", output.getAbsolutePath(),
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case02() {
        String path = "case02-kml-ws2";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case03() {
        String path = "case03-shp-seoul";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-inputType", "shp",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-proj", "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case04() {
        String path = "case04-las-mapo";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-inputType", "las",
                "-output", output.getAbsolutePath(),
                "-proj", "+proj=utm +zone=52 +datum=WGS84 +units=m +no_defs",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case05() {
        String path = "case05-kml-trees-instance";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-outputType", "i3dm",
                "-glb"
        };
        Mago3DTilerMain.main(args);
    }
}