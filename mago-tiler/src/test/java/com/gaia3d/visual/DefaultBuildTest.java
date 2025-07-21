package com.gaia3d.visual;

import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;

@Tag("default")
@Slf4j
class DefaultBuildTest {

    @Test
    void noArgs() {
        String[] args = {};
        Mago3DTilerMain.main(args);
    }

    @Test
    void help() {
        String[] args = {"-help",};
        Mago3DTilerMain.main(args);
    }

    @Test
    void quiet() {
        String[] args = {"-quiet", "-help",};
        Mago3DTilerMain.main(args);
    }

    @Test
    void debug() {
        String[] args = {"-debug", "-help",};
        Mago3DTilerMain.main(args);
    }

    @Test
    void noInput() {
        //ERROR_CASE
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("sample-empty").getFile());
        String[] args = {"-outputPath", file.getAbsolutePath(),};

        try {
            Mago3DTilerMain.main(args);
            assert false : "Expected an exception to be thrown due to no input path.";
        } catch (Exception e) {
            assert true : "Expected exception was thrown due to no input path.";
        }
    }

    @Test
    void noOutput() {
        //ERROR_CASE
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("sample-empty").getFile());
        String[] args = {"-input", file.getAbsolutePath(), "-inputType", "kml",};

        try {
            Mago3DTilerMain.main(args);
            assert false : "Expected an exception to be thrown due to no output path.";
        } catch (Exception e) {
            assert true : "Expected exception was thrown due to no output path.";
        }
    }

    @Test
    void emptyConvert() {
        //ERROR_CASE
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource("./sample-empty").getFile());
        File output = new File(classLoader.getResource("./sample-empty").getFile());
        String[] args = {"-input", input.getAbsolutePath(), "-output", output.getAbsolutePath(),};
        try {
            Mago3DTilerMain.main(args);
            assert false : "Expected an exception to be thrown due to empty input.";
        } catch (Exception e) {
            assert true : "Expected exception was thrown due to empty input.";
        }
    }

    @Test
    void defaultConvert() {
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource("./sample-kml").getFile());
        File output = new File(classLoader.getResource("./sample-output").getFile());
        String[] args = {"-input", input.getAbsolutePath(), "-output", output.getAbsolutePath(),};
        try {
            Mago3DTilerMain.main(args);
        } catch (Exception e) {
            assert false : "Expected no exception to be thrown for default conversion.";
        }
    }

    @Test
    void zAxisUp() {
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource("./sample-xyz").getFile());
        File output = new File(classLoader.getResource("./sample-output").getFile());
        String[] args = {"-input", input.getAbsolutePath(), "-output", output.getAbsolutePath(),};
        try {
            Mago3DTilerMain.main(args);
        } catch (Exception e) {
            assert false : "Expected no exception to be thrown for Z-axis up conversion.";
        }
    }

    @Test
    void geojson() {
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource("./sample-geojson").getFile());
        File output = new File(classLoader.getResource("./sample-output").getFile());
        String[] args = {"-input", input.getAbsolutePath(), "-output", output.getAbsolutePath(),};
        try {
            Mago3DTilerMain.main(args);
        } catch (Exception e) {
            assert false : "Expected no exception to be thrown for GeoJSON conversion.";
        }
    }

    @Test
    void shape() {
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource("./sample-shape").getFile());
        File output = new File(classLoader.getResource("./sample-output").getFile());
        String[] args = {"-input", input.getAbsolutePath(), "-output", output.getAbsolutePath(),};
        try {
            Mago3DTilerMain.main(args);
        } catch (Exception e) {
            assert false : "Expected no exception to be thrown for Shape conversion.";
        }
    }

    @Test
    void geopackage() {
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource("./sample-geopackage").getFile());
        File output = new File(classLoader.getResource("./sample-output").getFile());
        String[] args = {"-input", input.getAbsolutePath(), "-output", output.getAbsolutePath(),};
        try {
            Mago3DTilerMain.main(args);
        } catch (Exception e) {
            assert false : "Expected no exception to be thrown for GeoPackage conversion.";
        }
    }
}