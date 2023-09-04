package com.gaia3d.basic.command;

import com.gaia3d.command.TilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;

@Slf4j
class GmlTest {
    private static final String INPUT_PATH = "../../../../sample-external/";
    private static final String OUTPUT_PATH = "../../../../output/";

    @Test
    void help() {
        String[] args = new String[]{"-help"};
        TilerMain.main(args);
    }
    @Test
    void convertGML() {
        String input = "D:\\workspaces\\cityGML\\";
        String output = "D:\\workspaces\\cityGML\\output\\";
        convert(input, output, "sapporo", "4326", "gml");
    }
    @Test
    void convertGmlHawai() {
        String input = "D:\\workspaces\\cityGML\\";
        String output = "D:\\workspaces\\cityGML\\output\\";
        convert(input, output, "hawaii", "4326", "gml");
    }
    private void convert(String inputPath, String outputPath, String suffix, String crs, String inputType) {
        String[] args = new String[]{
                //"-log", outputPath + suffix + "/result.log",
                "-input", inputPath + suffix,
                "-inputType", inputType,
                "-output", outputPath + suffix,
                "-crs", crs,
                "-recursive",
                //"-swapYZ",
                "-maxCount", "32768",
                "-minLod", "0",
                "-maxLod", "3",
                "-multiThread",
                "-refineAdd",
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