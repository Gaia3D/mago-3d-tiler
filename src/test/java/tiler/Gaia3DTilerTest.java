package tiler;

import command.Configurator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class Gaia3DTilerTest {
    private static final String INPUT_PATH = "C:\\data\\plasma-test\\ws2-3ds\\";
    private static final String OUTPUT_PATH = "C:\\data\\plasma-test\\output2\\";

    @Test
    void createRoot() {
        Configurator.initLogger();
        File input = new File(INPUT_PATH);
        File output = new File(OUTPUT_PATH);
        Path inputPath = input.toPath();
        Path outputPath = output.toPath();

        Gaia3DTiler tiler = new Gaia3DTiler(inputPath, outputPath);
        tiler.excute();
    }
}