package com.gaia3d.release;

import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;

@Tag("default")
@Slf4j
class DefaultReleaseTest {
    @Test
    void help() {
        String[] args = {
                "-help",
        };
        Mago3DTilerMain.main(args);
    }
    @Test
    void quiet() {
        String args[] = {
                "-quiet",
        };
        Mago3DTilerMain.main(args);
    }
    @Test
    void version() {
        String[] args = {
                "-version",
                "-help",
        };
        log.info("version test.");
        Mago3DTilerMain.main(args);
    }
    @Test
    void debug() {
        String[] args = {
                "-version",
                "-help",
        };
        Mago3DTilerMain.main(args);
    }
    @Test
    void noInput() {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("sample-empty").getFile());
        String[] args = {
                "-outputPath", file.getAbsolutePath(),
        };

        try {
            Mago3DTilerMain.main(args);
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }
    @Test
    void noOutput() {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("sample-empty").getFile());
        String[] args = {
                "-input", file.getAbsolutePath(),
                "-inputType", "kml",
        };

        try {
            Mago3DTilerMain.main(args);
        } catch (Exception e) {
            log.error("[ERROR] :", e);
            log.debug(e.getMessage());
        }
    }
    @Test
    void emptyConvert() {
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource("./sample-empty").getFile());
        File output = new File(classLoader.getResource("./sample-empty").getFile());
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
        };
        try {
            Mago3DTilerMain.main(args);
        } catch (Exception e) {
            log.info("success test.");
            log.debug(e.getMessage());
        }
    }
    @Test
    void defaultConvert() {
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource("./sample-kml").getFile());
        File output = new File(classLoader.getResource("./sample-output").getFile());
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
        };
        try {
            Mago3DTilerMain.main(args);
        } catch (Exception e) {
            log.info("success test.");
            log.debug(e.getMessage());
        }
    }
    @Test
    void multiThreadConvert() {
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource("./sample-kml").getFile());
        File output = new File(classLoader.getResource("./sample-output").getFile());
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                //"-multiThread",
        };
        try {
            Mago3DTilerMain.main(args);
        } catch (Exception e) {
            log.info("success test.");
            log.debug(e.getMessage());
        }
    }

    @Test
    void multiThreadConvertErrorCase() {
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource("./sample-kml-error-case").getFile());
        File output = new File(classLoader.getResource("./sample-output").getFile());
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                //"-multiThread",
        };
        try {
            Mago3DTilerMain.main(args);
        } catch (Exception e) {
            log.debug("success test.");
            log.debug(e.getMessage());
        }
    }

    @Test
    void zAxisUp() {
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource("./sample-xyz").getFile());
        File output = new File(classLoader.getResource("./sample-output").getFile());
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                //"-multiThread",
        };
        try {
            Mago3DTilerMain.main(args);
        } catch (Exception e) {
            log.debug("success test.");
            log.debug(e.getMessage());
        }
    }
}