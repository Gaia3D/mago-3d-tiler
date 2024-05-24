package com.gaia3d.release;

import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
class ReleaseUnitTest {
    private static final String INPUT_PATH = "D:\\data\\mago-tiler-data\\release-unit-test-input";
    private static final String OUTPUT_PATH = "C:\\Workspaces\\GitSources\\mago-viewer\\data\\release-unit-test-output";

    @Test
    void testKmlWithCollada() {
        String path = "NAMYANGJU-WANGSUK-KML-DAE";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testMaxStudio() {
        String path = "NAMYANGJU-WANGSUK-3DS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "3ds",
                "-crs", "5186"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testPointCloud() {
        String path = "HWANGYONGGAK-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-crs", "32652"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testSangjiUnivPointCloud() {
        String path = "SANGJUUNIV-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-crs", "5186",
                "-r"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testInstancedModel() {
        String path = "INSTANCED-MODEL-KML";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "kml",
                "-ot", "i3dm",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testShapeSeoul() {
        String path = "SEOUL-PART-SHP";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "shp",
                "-crs", "5186"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testGeojsonPolygon() {
        String path = "NAMYANGJU-WANGSUK-GEOJSON";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "geojson",
                //"-crs", "5186"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testComplicatedKmlWithMaxStudio() {
        String path = "COMPLICATED-KML-3DS";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "kml",
                "-output", output.getAbsolutePath(),
                "-r"
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
