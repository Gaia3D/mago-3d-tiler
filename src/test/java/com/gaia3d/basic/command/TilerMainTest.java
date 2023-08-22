package com.gaia3d.basic.command;

import com.gaia3d.command.TilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;

@Slf4j
class TilerMainTest {
    private static final String INPUT_PATH = "../../../../sample-external/";
    private static final String OUTPUT_PATH = "../../../../output/";

    @Test
    void help() {
        String[] args = new String[]{"-help"};
        TilerMain.main(args);
    }

    @Test
    void convertWs2Kml() throws URISyntaxException {
        String input = "D:\\temp\\sample-external\\";
        String output = getAbsolutePath(OUTPUT_PATH);
        convert(input, output, "collada-ws2", "", "kml");
    }

    @Test
    void convertSeoul() {
        String path = "F:\\workspace\\";
        String[] args = new String[]{
        "-i", path + "seoul-input",
        "-it", "kml",
        "-o", path +  "seoul-output-debug",
        "-l", path + "seoul-output-debug" + "/result.log",
        "-c", "",
        "-yz",
        "-mx", "32768",
        "-nl", "0",
        "-xl", "0",
        "-mt",
        //"-glb",
        //"-debug"
        };
        TilerMain.main(args);
    }

    @Test
    void convertWs2() throws URISyntaxException {
        String input = getAbsolutePath(INPUT_PATH);
        String output = getAbsolutePath(OUTPUT_PATH);
        convert(input, output, "3d-tiles-ws2", "5186", "3ds");
    }

    @Test
    void convertWs1() throws URISyntaxException {
        String input = getAbsolutePath(INPUT_PATH);
        String output = getAbsolutePath(OUTPUT_PATH);
        convert(input, output, "3d-tiles-ws1", "5186", "3ds");
    }

    @Test
    void convertAsjs() throws URISyntaxException {
        String input = getAbsolutePath(INPUT_PATH);
        String output = getAbsolutePath(OUTPUT_PATH);
        convert(input, output, "3d-tiles-asjs", "5186", "3ds");
    }

    @Test
    void convertGy() throws URISyntaxException {
        String input = getAbsolutePath(INPUT_PATH);
        String output = getAbsolutePath(OUTPUT_PATH);
        convert(input, output, "3d-tiles-gy", "5186", "3ds");
    }

    @Test
    void convertGs() throws URISyntaxException {
        String input = getAbsolutePath(INPUT_PATH);
        String output = getAbsolutePath(OUTPUT_PATH);
        convert(input, output, "3d-tiles-gs2", "5174", "3ds");
    }

    private void convert(String inputPath, String outputPath, String suffix, String crs, String inputType) {
        String[] args = new String[]{
                "-log", outputPath + suffix + "/result.log",
                "-input", inputPath + suffix,
                "-inputType", inputType,
                "-output", outputPath + suffix,
                "-crs", crs,
                "-swapYZ",
                "-maxCount", "1024",
                "-minLod", "0",
                "-maxLod", "3",
                "-multiThread",
                "-glb",
                //"-debug"
        };
        TilerMain.main(args);
    }

    private String getAbsolutePath(String classPath) throws URISyntaxException {
        File file = new File(getClass().getResource(classPath).toURI());
        return file.getAbsolutePath() + File.separator;
    }
}