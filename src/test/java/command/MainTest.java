package command;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

class MainTest {

    private static final String INPUT_PATH = "C:\\data\\sample\\";
    private static final String OUTPUT_PATH = "C:\\data\\sample\\output\\";

    @Test
    void versionTest() {
        String[] args= {"-version"};
        Main.main(args);
    }

    @Test
    void helpTest() {
        String[] args= {"-help"};
        Main.main(args);
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
        Main.main(args);
    }

    @Test
    void testMain() {
        String[] args = new String[]{
                "-input", INPUT_PATH,
                "-inputType", "3ds",
                "-output", OUTPUT_PATH,
                "-outputType", "gltf"
        };
        Main.main(args);
    }

}