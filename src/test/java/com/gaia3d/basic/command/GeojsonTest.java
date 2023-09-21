package com.gaia3d.basic.command;

import com.gaia3d.command.TilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.locationtech.proj4j.BasicCoordinateTransform;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

@Slf4j
class GeojsonTest {
    @Test
    void help() {
        String[] args = new String[]{"-help"};
        TilerMain.main(args);
    }

    @Test
    void convertGeoJson() {
        String input = "D:\\temp\\ws2\\";
        String output = "D:\\temp\\ws2\\output\\";
        convert(input, output, "geojson", "5186", "geojson");
    }

    @Test
    void convertJson() {
        String input = "D:\\temp\\ws2\\";
        String output = "D:\\temp\\ws2\\output\\";
        convert(input, output, "json", "5186", "json");
    }

    private void convert(String inputPath, String outputPath, String suffix, String crs, String inputType) {
        String[] args = new String[]{
                //"-log", outputPath + suffix + "/result.log",
                "-input", inputPath,
                "-inputType", inputType,
                "-output", outputPath + suffix,
                "-crs", crs,
                "-recursive",
                //"-swapYZ",
                "-maxCount", "8192",
                "-minLod", "0",
                "-maxLod", "3",
                //"-multiThread",
                "-refineAdd",
                //"-glb",
                "-debug"
        };
        TilerMain.main(args);
    }
}