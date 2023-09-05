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
class ShapeKoreaTest {
    private static final String INPUT_PATH = "../../../../sample-external/";
    private static final String OUTPUT_PATH = "../../../../output/";

    private static final String input = "D:\\workspaces\\korea_fixed\\";
    private static final String output = "D:\\workspaces\\korea_fixed\\output\\";

    /*@Test
    void convertKorea() {
        String suffix = "south_korea";
        convert(suffix);
    }*/

    @Test
    void convertShp1() {
        String suffix = "seoul";
        convert(suffix);
    }

    @Test
    void convertShp2() {
        String suffix = "gangwon";
        convert(suffix);
    }

    @Test
    void convertShp3() {
        String suffix = "gyeonggi";
        convert(suffix);
    }

    @Test
    void convertShp4() {
        String suffix = "gyeongnam";
        convert(suffix);
    }

    @Test
    void convertShp5() {
        String suffix = "gyeongbuk";
        convert(suffix);
    }

    @Test
    void convertShp6() {
        String suffix = "gwangju";
        convert(suffix);
    }

    @Test
    void convertShp7() {
        String suffix = "daegu";
        convert(suffix);
    }

    @Test
    void convertShp8() {
        String suffix = "deajeon";
        convert(suffix);
    }

    @Test
    void convertShp9() {
        String suffix = "busan";
        convert(suffix);
    }

    @Test
    void convertShp10() {
        String suffix = "sejong";
        convert(suffix);
    }

    @Test
    void convertShp11() {
        String suffix = "ulsan";
        convert(suffix);
    }

    @Test
    void convertShp12() {
        String suffix = "incheon";
        convert(suffix);
    }

    @Test
    void convertShp13() {
        String suffix = "jeonnam";
        convert(suffix);
    }

    @Test
    void convertShp14() {
        String suffix = "jeonbuk";
        convert(suffix);
    }

    @Test
    void convertShp15() {
        String suffix = "jeju";
        convert(suffix);
    }

    @Test
    void convertShp16() {
        String suffix = "chungnam";
        convert(suffix);
    }

    @Test
    void convertShp17() {
        String suffix = "chungbuk";
        convert(suffix);
    }

    void convert(String suffix) {
        String[] args = new String[]{
                "-input", input + suffix + ".shp",
                "-inputType", "shp",
                "-output", output + suffix,
                "-recursive",
                "-maxCount", "32768",
                "-minLod", "0",
                "-maxLod", "3",
                "-proj", "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43",
                "-multiThread",
                "-refineAdd",
                "-crs", "5174",
                //"-glb",
        };
        TilerMain.main(args);
    }
}