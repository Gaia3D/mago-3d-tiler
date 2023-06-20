package command;

import org.junit.jupiter.api.Test;

class ConverterMainTest {
    private static final String INPUT_PATH = "C:\\data\\plasma-test\\";
    private static final String OUTPUT_PATH = "C:\\data\\plasma-test\\output\\";

    @Test
    void versionTest() {
        String[] args= {"-version"};
        ConverterMain.main(args);
    }

    @Test
    void helpTest() {
        String[] args= {"-help"};
        ConverterMain.main(args);
    }

    @Test
    void quietTest() {
        String[] args= new String[]{
                "-input", INPUT_PATH,
                "-inputType", "3ds",
                "-output", OUTPUT_PATH,
                "-outputType", "gltf",
                "-quiet",
        };
        ConverterMain.main(args);
    }

    @Test
    void shortOptionTest() {
        String[] args= new String[]{
                "-i", INPUT_PATH,
                "-it", "3ds",
                "-o", OUTPUT_PATH,
                "-ot", "gltf",
        };
        ConverterMain.main(args);
    }

    @Test
    void testMain() {
        String[] args = new String[]{
                "-input", INPUT_PATH,
                "-inputType", "3ds",
                "-output", OUTPUT_PATH,
                "-outputType", "gltf"
        };
        ConverterMain.main(args);
    }
}