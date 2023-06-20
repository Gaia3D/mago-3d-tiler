package tiler;

import command.Configurator;
import geometry.types.FormatType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class Gaia3DTilerTest {
    private static final String INPUT_PATH = "C:\\data\\plasma-test\\ws2-3ds\\";
    private static final String OUTPUT_PATH = "C:\\data\\plasma-test\\output\\";

    //private static final String INPUT_PATH = "C:\\data\\plasma-test\\gs-3ds\\";
    //private static final String OUTPUT_PATH = "C:\\data\\plasma-test\\output-gs\\";

    //private static final String INPUT_PATH = "C:\\data\\plasma-test\\icgy-3ds\\";
    //private static final String OUTPUT_PATH = "C:\\data\\plasma-test\\output-icgy\\";

    @Test
    void createRoot() {
        Configurator.initLogger();
        File input = new File(INPUT_PATH);
        File output = new File(OUTPUT_PATH);
        output.mkdir();

        Path inputPath = input.toPath();
        Path outputPath = output.toPath();

        Gaia3DTiler tiler = new Gaia3DTiler(inputPath, outputPath, FormatType.MAX_3DS,null);
        tiler.excute();
    }
}