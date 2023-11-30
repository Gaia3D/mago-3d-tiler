package com.gaia3d.basic.command;

import com.gaia3d.command.TilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;

@Slf4j
class I3dmTest {

    @Test
    void convertSnowman() throws URISyntaxException {
        String input = "D:\\data\\kml\\";
        String output = "D:\\data\\kml\\output\\";
        convert(input, output, "snowman", "kml");
    }

    private void convert(String inputPath, String outputPath, String suffix, String inputType) {
        String[] args = new String[] {
                "-input", inputPath + suffix,
                "-inputType", inputType,
                "-output", outputPath + suffix,
                "-outputType", "i3dm",
                "-crs", "",
                "-recursive",
                "-swapYZ",
                //"-maxCount", "1024",
                //"-minLod", "0",
                //"-maxLod", "3",
                //"-multiThread",
                //"-refineAdd",
                //"-glb",
                //"-debug"
        };
        TilerMain.main(args);
    }
}