package command;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;

class ConverterMainTest {
    private static final String INPUT_PATH = "../sample/";
    private static final String OUTPUT_PATH = "../output/";

    @Test
    void shortOptionTest() throws URISyntaxException {
        String[] args= new String[]{
                "-i", getInputPath(),
                "-it", "3ds",
                "-o", getOutputPath(),
                "-ot", "gltf",
                "-r",
        };
        ConverterMain.main(args);
    }

    @Test
    void testMain() throws URISyntaxException {
        String[] args = new String[]{
                "-input", getInputPath(),
                "-inputType", "3ds",
                "-output", getOutputPath(),
                "-outputType", "gltf"
        };
        ConverterMain.main(args);
    }

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
    void quietTest() throws URISyntaxException {
        String[] args= new String[]{
                "-input", getInputPath(),
                "-inputType", "3ds",
                "-output", getOutputPath(),
                "-outputType", "gltf",
                "-quiet",
        };
        ConverterMain.main(args);
    }

    private String getInputPath() throws URISyntaxException {
        File file = new File(getClass().getResource(INPUT_PATH).toURI());
        assert(file != null);
        return file.getAbsolutePath();
    }

    private String getOutputPath() throws URISyntaxException {
        File file = new File(getClass().getResource(OUTPUT_PATH).toURI());
        assert(file != null);
        return file.getAbsolutePath();
    }
}