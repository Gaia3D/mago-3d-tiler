package com.gaia3d.command.mago;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.joml.Random;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.io.File;

@Deprecated
@Slf4j
class Mago3DTilerSeoulTest {

    private static final String INPUT_PATH = "D:\\Mago3DTiler-UnitTest\\input";

    private static final String OUTPUT_PATH = "C:\\Workspaces\\GitSources\\mago-viewer\\data\\tilesets\\";

    @Test
    void seoul01() {
        String path = "seoul-set/1";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5181",
                //"-proj", "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul02() {
        String path = "seoul-set/2";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul03() {
        String path = "seoul-set/3";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul04() {
        String path = "seoul-set/4";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul05() {
        String path = "seoul-set/5";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul06() {
        String path = "seoul-set/6";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul07() {
        String path = "seoul-set/7";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul08() {
        String path = "seoul-set/8";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul09() {
        String path = "seoul-set/9";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul10() {
        String path = "seoul-set/10";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul11() {
        String path = "seoul-set/11";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul12() {
        String path = "seoul-set/12";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul13() {
        String path = "seoul-set/13";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul14() {
        String path = "seoul-set/14";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul15() {
        String path = "seoul-set/15";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5181",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul16() {
        String path = "seoul-set/16";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul17() {
        String path = "seoul-set/17";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul18() {
        String path = "seoul-set/18";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul19() {
        String path = "seoul-set/19";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul20() {
        String path = "seoul-set/20";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5181",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul21() {
        String path = "seoul-set/21";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul22() {
        String path = "seoul-set/22";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void seoul23() {
        String path = "seoul-set/23";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }
}