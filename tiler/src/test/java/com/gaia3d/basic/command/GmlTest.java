package com.gaia3d.basic.command;

import com.gaia3d.command.TilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;

@Slf4j
class GmlTest {
    private static final String INPUT_PATH = "D:\\workspaces\\cityGML\\";
    private static final String OUTPUT_PATH = "D:\\workspaces\\cityGML\\output\\";

    @Test
    void convertGmlSapporo() {
        convert("sapporo", "4326");
    }
    @Test
    void convertGmlMoran() {
        convert("moran", "4326");
    }
    @Test
    void convertGmlHawaii() {
        convert("hawaii", "4326");
    }
    private void convert(String suffix, String crs) {
        String[] args = new String[]{
                "-input", INPUT_PATH + suffix,
                "-inputType", "gml",
                "-output", OUTPUT_PATH + suffix,
                "-crs", crs,
                "-recursive",
                //"-swapYZ",
                "-flipCoordinate", //sapporo, moran
                "-maxCount", "32768",
                "-minLod", "0",
                "-maxLod", "3",
                "-refineAdd",
                "-multiThread",
                //"-glb",
                //"-debug"
        };
        TilerMain.main(args);
    }

    private String getAbsolutePath(String classPath) throws URISyntaxException {
        File file = new File(getClass().getResource(classPath).toURI());
        return file.getAbsolutePath() + File.separator;
    }
}