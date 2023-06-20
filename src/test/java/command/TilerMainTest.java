package command;

import org.junit.jupiter.api.Test;

class TilerMainTest {
    @Test
    void main() {
        Configurator.initLogger();
        //startGS("C:\\data\\plasma-test\\gs-3ds\\", "C:\\data\\plasma-test\\tiler-gs2-test\\");
        //start("C:\\data\\plasma-test\\icgy-3ds\\", "C:\\data\\plasma-test\\tiler-gy-test\\");
        start("C:\\data\\plasma-test\\ws2-3ds\\", "C:\\data\\plasma-test\\divTest\\");
    }

    void start(String inputPath, String outputPath) {
        String[] args= new String[]{
                "-input", inputPath,
                "-inputType", "3ds",
                "-output", outputPath,
                "-outputType", "gltf",
                "-src", "5186",
                "-swapYZ",
                "-quiet",
        };
        TilerMain.main(args);
    }

    void startGS(String inputPath, String outputPath) {
        String[] args= new String[]{
                "-input", inputPath,
                "-inputType", "3ds",
                "-output", outputPath,
                "-outputType", "gltf",
                "-src", "5174",
                "-swapYZ",
                "-quiet",
        };
        TilerMain.main(args);
    }
}