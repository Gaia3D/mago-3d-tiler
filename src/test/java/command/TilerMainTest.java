package command;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;

@Slf4j
class TilerMainTest {
    private static final String INPUT_PATH = "../sample-external/";
    private static final String OUTPUT_PATH = "../output/";

    @Test
    void convertWs() throws URISyntaxException {
        String input = getAbsolutePath(INPUT_PATH);
        String output = getAbsolutePath(OUTPUT_PATH);
        convert(input, output, "3d-tiles-ws2", "5186");
    }

    @Test
    void convertGy() throws URISyntaxException {
        String input = getAbsolutePath(INPUT_PATH);
        String output = getAbsolutePath(OUTPUT_PATH);
        convert(input, output, "3d-tiles-gy", "5186");
    }

    /*@Test
    void convertWs2Gy() throws URISyntaxException {
        String input = getAbsolutePath(INPUT_PATH);
        String output = getAbsolutePath(OUTPUT_PATH);
        convert(input, output, "3d-tiles-ws2gy", "5186");
    }*/

    @Test
    void convertGs() throws URISyntaxException {
        String input = getAbsolutePath(INPUT_PATH);
        String output = getAbsolutePath(OUTPUT_PATH);
        convert(input, output, "3d-tiles-gs2", "5174");
    }

    private void convert(String inputPath, String outputPath, String suffix, String srs) {
        String[] args= new String[]{
                "-input", inputPath + suffix,
                "-inputType", "3ds",
                "-output", outputPath + suffix,
                "-outputType", "gltf",
                "-srs", srs,
                "-swapYZ",
                "-debug"
        };
        TilerMain.main(args);
    }

    private String getAbsolutePath(String classPath) throws URISyntaxException {
        File file = new File(getClass().getResource(classPath).toURI());
        assert(file != null);
        return file.getAbsolutePath() + File.separator;
    }
}