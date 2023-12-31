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
        log.info("help Test");
        TilerMain.main(args);
    }

    @Test
    void version() {
        String[] args = new String[]{"-version"};
        TilerMain.main(args);
    }

    //@Test
    void sapporoObj() {
        String path = "D:\\workspaces\\sapporo-test\\";
        String[] args = new String[]{
                "-i", path + "sapporo-lod1",
                "-it", "obj",
                "-o", path +  "sapporo-lod1-output",
                "-crs", "6680",
                "-r",
                //"-swapYZ",
                "-maxCount", "4",
                "-minLod", "0",
                "-maxLod", "3",
                //"-multiThread",
                "-refineAdd",
                //"-glb",
                //"-debug"
        };
        TilerMain.main(args);
    }

    @Test
    void convertWs2Kml() throws URISyntaxException {
        String input = "D:\\temp\\sample-external\\";
        String output = getAbsolutePath(OUTPUT_PATH);
        convert(input, output, "collada-ws2", "", "kml");
    }

    @Test
    void convertSnowman() throws URISyntaxException {
        String input = "D:\\data\\kml\\";
        String output = "D:\\data\\kml\\output\\";
        convert(input, output, "snowman", "", "kml");
    }

    @Test
    void convertSeoul() {
        String path = "F:\\workspace\\";
        String[] args = new String[]{
        "-i", path + "seoul-input",
        "-it", "kml",
        "-o", path +  "seoul-output-over",
        "-c", "",
        "-yz",
        "-mx", "65536",
        "-nl", "0",
        "-xl", "4",
        "-refineAdd",
        "-mt",
        //"-glb",
        //"-debug"
        };
        TilerMain.main(args);
    }

    //@Test
    void convertSeoulMini() {
        String path = "F:\\workspace\\";
        String[] args = new String[]{
                "-i", path + "optimize",
                "-it", "kml",
                "-o", path +  "seoul-output-10000",
                "-c", "",
                "-yz",
                "-mx", "65536",
                "-nl", "0",
                "-xl", "5",
                "-refineAdd",
                "-mt",
                //"-glb",
                //"-debug"
        };
        TilerMain.main(args);
    }

    //@Test
    void convertTree() {
        String path = "D:\\workspaces\\Instance\\";
        String[] args = new String[]{
                "-i", path + "instance-test",
                "-it", "obj",
                "-o", path +  "test1",
                "-crs", "6668",
                //"-swapYZ",
                "-maxCount", "256",
                "-minLod", "0",
                "-maxLod", "3",
                "-multiThread",
                "-debug"
        };
        TilerMain.main(args);
    }

    @Test
    void convertGML() {
        String input = "D:\\workspaces\\cityGML\\";
        String output = "D:\\workspaces\\cityGML\\output\\";
        convert(input, output, "sapporo", "4326", "gml");
    }

    @Test
    void convertShp() {
        String input = "D:\\workspaces\\shapeSample\\";
        String output = "D:\\workspaces\\shapeSample\\output\\";
        convert(input, output, "busan", "4326", "shp");
    }

    @Test
    void convertShpSejong() {
        String input = "D:\\workspaces\\shapeSample\\";
        String output = "D:\\workspaces\\shapeSample\\output\\";
        convert(input, output, "sejong", "4326", "shp");
    }

    @Test
    void convertGmlHawai() {
        String input = "D:\\workspaces\\cityGML\\";
        String output = "D:\\workspaces\\cityGML\\output\\";
        convert(input, output, "hawaii", "4326", "gml");
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
                //"-log", outputPath + suffix + "/result.log",
                "-input", inputPath + suffix,
                "-inputType", inputType,
                "-output", outputPath + suffix,
                "-crs", crs,
                "-recursive",
                "-swapYZ",
                "-maxCount", "32768",
                "-minLod", "0",
                "-maxLod", "3",
                //"-multiThread",
                //"-refineAdd",
                "-glb",
                "-debug"
        };
        TilerMain.main(args);
    }

    private String getAbsolutePath(String classPath) throws URISyntaxException {
        File file = new File(getClass().getResource(classPath).toURI());
        return file.getAbsolutePath() + File.separator;
    }
}