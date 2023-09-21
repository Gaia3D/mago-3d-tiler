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
class ShapeTest {
    private static final String INPUT_PATH = "../../../../sample-external/";
    private static final String OUTPUT_PATH = "../../../../output/";

    @Test
    void test() throws IOException {
        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem wgs84 = factory.createFromParameters("WGS84", "+proj=longlat +datum=WGS84 +no_defs");

        //446971.3,193230.0
        String test = factory.readEpsgFromParameters("EPSG:5174");

        CoordinateReferenceSystem crs4 = factory.createFromName("EPSG:5174");
        CoordinateReferenceSystem crs = factory.createFromParameters("EPSG:5174", "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43");


        ProjCoordinate input = new ProjCoordinate(126.92312831407592, 37.52661656108093, 0);
        //ProjCoordinate output = GlobeUtils.transform(input, crs);

        BasicCoordinateTransform transformer = new BasicCoordinateTransform(wgs84, crs);
        ProjCoordinate output = new ProjCoordinate();
        transformer.transform(input, output);

        log.error("test");
    }

    @Test
    void help() {
        String[] args = new String[]{"-help"};
        TilerMain.main(args);
    }

    @Test
    void convertShp() {
        String input = "D:\\workspaces\\shapeSample\\";
        String output = "D:\\workspaces\\shapeSample\\output\\";
        convert(input, output, "busan", "4326", "shp");
    }

    @Test
    void convertSeoulShp() {
        String input = "D:\\workspaces\\shapeSample\\";
        String output = "D:\\workspaces\\shapeSample\\output\\";
        String suffix = "seoul";
        String inputType = "shp";
        String[] args = new String[]{
                "-input", input + suffix,
                "-inputType", inputType,
                "-output", output + suffix,
                "-recursive",
                //"-swapYZ",
                "-maxCount", "32768",
                "-minLod", "0",
                "-maxLod", "3",
                //"-proj", "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43",
                "-multiThread",
                "-refineAdd",
                "-crs", "4326",
                //"-glb",
                //"-debug"
        };
        TilerMain.main(args);
    }

    @Test
    void sample() {
        String input = "D:\\workspaces\\shapeSample\\";
        String output = "D:\\workspaces\\shapeSample\\output\\";
        String suffix = "sample";
        String inputType = "shp";
        String[] args = new String[]{
                "-input", input + suffix,
                "-inputType", inputType,
                "-output", output + suffix,
                "-recursive",
                //"-swapYZ",
                "-maxCount", "32768",
                "-minLod", "0",
                "-maxLod", "3",
                "-proj", "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43",
                //"-multiThread",
                "-refineAdd",
                "-crs", "5174",
                //"-glb",
                //"-debug"
        };
        TilerMain.main(args);
    }

    @Test
    void convertSeoul5174Shp() {
        String input = "D:\\workspaces\\shapeSample\\";
        String output = "D:\\workspaces\\shapeSample\\output\\";
        String suffix = "seoul_5174";
        String inputType = "shp";
        String[] args = new String[]{
                "-input", input + suffix,
                "-inputType", inputType,
                "-output", output + suffix,
                "-recursive",
                //"-swapYZ",
                "-maxCount", "32768",
                "-minLod", "0",
                "-maxLod", "3",
                "-proj", "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43",
                "-multiThread",
                "-refineAdd",
                //"-crs", "5174",
                //"-glb",
                //"-debug"
        };
        TilerMain.main(args);
    }

    @Test
    void convertSeoul5186Shp() {
        String input = "D:\\workspaces\\shapeSample\\";
        String output = "D:\\workspaces\\shapeSample\\output\\";
        convert(input, output, "seoul_5186", "5186", "shp");
    }

    @Test
    void convertSeoul4326Shp() {
        String input = "D:\\workspaces\\shapeSample\\";
        String output = "D:\\workspaces\\shapeSample\\output\\";
        convert(input, output, "seoul_4326", "4326", "shp");
    }

    @Test
    void convertYeouido() {
        String input = "D:\\workspaces\\shapeSample\\";
        String output = "D:\\workspaces\\shapeSample\\output\\";
        convert(input, output, "yeouido", "4326", "shp");
    }

    @Test
    void convertBusanShp() {
        String input = "D:\\workspaces\\shapeSample\\";
        String output = "D:\\workspaces\\shapeSample\\output\\";
        String suffix = "busan";
        String inputType = "shp";
        String[] args = new String[]{
                "-input", input + suffix,
                "-inputType", inputType,
                "-output", output + suffix,
                "-recursive",
                //"-swapYZ",
                "-maxCount", "32768",
                "-minLod", "0",
                "-maxLod", "3",
                "-proj", "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43",
                "-multiThread",
                "-refineAdd",
                //"-crs", "4326",
                //"-glb",
                //"-debug"
        };
        TilerMain.main(args);
    }

    @Test
    void convertSejongShp() {
        String input = "D:\\workspaces\\shapeSample\\";
        String output = "D:\\workspaces\\shapeSample\\output\\";
        convert(input, output, "sejong", "4326", "shp");
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
                "-maxCount", "8192",
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