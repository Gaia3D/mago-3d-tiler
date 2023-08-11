package com.gaia3d.basic.command;

import com.gaia3d.command.TilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;

@Slf4j
class TilerMainOldTest {
    private static final String INPUT_PATH = "../../../../sample-external/";
    private static final String OUTPUT_PATH = "../../../../output/";

    @Test
    void help() throws URISyntaxException {
        String[] args= new String[]{
                "-help"
        };
        TilerMain.main(args);
    }

    @Test
    void test51() throws URISyntaxException {
        String input = "D:\\51zone\\51zone_3DS\\";
        String output = "D:\\51zone\\51zone_3DS_output\\";
        String[] args= new String[]{
                "-input", input,
                "-inputType", "3ds",
                "-output", output,
                "-swapYZ",
                "-maxCount", "512",
                "-crs", "32651",
                //"-reverseTexCoord",
                "-glb",
                "-debug",
        };
        TilerMain.main(args);
    }

    @Test
    void test52() throws URISyntaxException {
        String input = "D:\\52zone\\52zone_3DS\\";
        String output = "D:\\52zone\\52zone_3DS_output\\";
        String[] args= new String[]{
                "-input", input,
                "-inputType", "3ds",
                "-output", output,
                "-swapYZ",
                "-maxCount", "512",
                "-crs", "32652",
                //"-reverseTexCoord",
                "-glb",
                "-debug",
        };
        TilerMain.main(args);
    }

    @Test
    void order() throws URISyntaxException {
        String input = "D:\\workspaces\\ComplicatedModels";
        String output = "D:\\workspaces\\ComplicatedModels\\test";
        String[] args= new String[]{
                "-input", input,
                "-inputType", "kml",
                "-output", output,
                "-swapYZ",
                "-maxCount", "2048",
                //"-reverseTexCoord",
                "-gltf",
                //"-debug",
        };
        TilerMain.main(args);
    }

    @Test
    void convertWs2() throws URISyntaxException {
        String input = getAbsolutePath(INPUT_PATH);
        String output = getAbsolutePath(OUTPUT_PATH);
        convert(input, output, "3d-tiles-ws2", "5186");
    }

    @Test
    void convertWs2Kml() throws URISyntaxException {
        String input = getAbsolutePath(INPUT_PATH);
        String output = getAbsolutePath(OUTPUT_PATH);
        String suffix = "collada-ws2";
        String[] args= new String[]{
                "-input", input + suffix,
                "-inputType", "kml",
                "-output", output + suffix,
                "-outputType", "gltf",
                "-swapYZ",
                "-maxCount", "512",
                //"-glb",
                //"-debug"
        };
        TilerMain.main(args);
    }

    @Test
    void convertSeoul() {
        String input = "D:\\workspaces\\su_kml_collada\\mini\\";
        String output = "D:\\workspaces\\su_kml_collada\\mini_output\\";
        String[] args= new String[]{
                "-input", input,
                "-inputType", "kml",
                "-output", output,
                "-outputType", "gltf",
                "-swapYZ",
                "-maxCount", "512",
                "-minLod", "3",
                "-maxLod", "3"
                //"-gltf",
                //"-debug",
        };
        TilerMain.main(args);
    }

    @Test
    void convertSeoulBig() {
        String input = "F:\\inputdata\\";
        String output = "F:\\outputdata+\\";
        String[] args= new String[]{
                "-input", input,
                "-inputType", "kml",
                "-log", output + "result.log",
                "-output", output,
                "-swapYZ",
                "-maxCount", "8192",
                //"-gltf",
        };
        TilerMain.main(args);
    }

    @Test
    void convertTEST() {
        String input = "D:\\MAGO_TEST_FOLDER\\ComplicatedModels";
        String output = "D:\\MAGO_TEST_FOLDER\\ComplicatedModels\\output";
        String[] args= new String[]{
                "-input", input,
                "-inputType", "kml",
                "-output", output,
                "-outputType", "gltf",
                "-swapYZ",
                "-glb",
                //"-debug",
                //"-maxCount", "256",
        };
        TilerMain.main(args);
    }

    @Test
    void convertWs1() throws URISyntaxException {
        String input = getAbsolutePath(INPUT_PATH);
        String output = getAbsolutePath(OUTPUT_PATH);
        convert(input, output, "3d-tiles-ws1", "5186");
    }

    @Test
    void convertAsjs() throws URISyntaxException {
        String input = getAbsolutePath(INPUT_PATH);
        String output = getAbsolutePath(OUTPUT_PATH);
        convert(input, output, "3d-tiles-asjs", "5186");
    }

    @Test
    void convertGy() throws URISyntaxException {
        String input = getAbsolutePath(INPUT_PATH);
        String output = getAbsolutePath(OUTPUT_PATH);
        convert(input, output, "3d-tiles-gy", "5186");
    }

    @Test
    void convertGs() throws URISyntaxException {
        String input = getAbsolutePath(INPUT_PATH);
        String output = getAbsolutePath(OUTPUT_PATH);
        convert(input, output, "3d-tiles-gs2", "5174");
    }

    private void convert(String inputPath, String outputPath, String suffix, String crs) {
        String[] args= new String[]{
                //"-log", outputPath + suffix + "/result.log",
                "-input", inputPath + suffix,
                "-inputType", "3ds",
                "-output", outputPath + suffix,
                "-crs", crs,
                "-swapYZ",
                "-maxCount", "1024",
                //"-minLod", "3",
                //"-maxLod", "3",
                //"-glb",
                "-debug"
        };
        TilerMain.main(args);
    }

    private String getAbsolutePath(String classPath) throws URISyntaxException {
        File file = new File(getClass().getResource(classPath).toURI());
        return file.getAbsolutePath() + File.separator;
    }
}