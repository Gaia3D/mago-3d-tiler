package command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TilerMainTest {
    private static final String INPUT_PATH = "C:\\data\\plasma-test\\gs-3ds\\";
    private static final String OUTPUT_PATH = "C:\\data\\plasma-test\\output-gs\\";

    @Test
    void main() {
        Configurator.initLogger();
        start("C:\\data\\plasma-test\\gs-3ds\\", "C:\\data\\plasma-test\\tiler-gs2\\");
        start("C:\\data\\plasma-test\\icgy-3ds\\", "C:\\data\\plasma-test\\tiler-icgy\\");
        start("C:\\data\\plasma-test\\ws2-3ds\\", "C:\\data\\plasma-test\\tiler-ws2\\");
    }

    void start(String inputPath, String outputPath) {
        String[] args= new String[]{
                "-input", inputPath,
                "-inputType", "3ds",
                "-output", outputPath,
                "-outputType", "gltf",
                "-quiet",
        };
        TilerMain.main(args);
    }
}