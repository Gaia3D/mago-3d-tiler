package com.gaia3d.command.mago;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.joml.Random;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

@Slf4j
class Mago3DTilerUnitTest {

    private static final String INPUT_PATH = "D:\\Mago3DTiler-UnitTest\\input";
    //private static final String OUTPUT_PATH = "D:\\Mago3DTiler-UnitTest\\output";
    private static final String OUTPUT_PATH = "C:\\Workspaces\\GitSources\\mago-viewer\\data\\tilesets\\";

    @Test
    void case00() {
        String path = "case00-3ds-gy";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-crs", "5186",
                //"-autoUpAxis",
                "-output", output.getAbsolutePath(),
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case01() {
        String path = "case01-3ds-ws2";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-crs", "5186",
                //"-autoUpAxis",
                "-output", output.getAbsolutePath(),
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case02() {
        String path = "case02-kml-ws2";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                //"-autoUpAxis", // is not supported
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case03() {
        String path = "case03-shp-seoul";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-inputType", "shp",
                "-output", output.getAbsolutePath(),
                "-autoUpAxis",
                "-refineAdd",
                "-proj", "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case04() {
        String path = "case04-las-mapo";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-inputType", "las",
                "-autoUpAxis",
                "-output", output.getAbsolutePath(),
                "-proj", "+proj=utm +zone=52 +datum=WGS84 +units=m +no_defs",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case05() {
        String path = "case05-kml-trees-instance";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-outputType", "i3dm",
                "-autoUpAxis",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case06() {
        String path = "case06-kml-auto-instance";
        sampleI3dm(path, 20 , 100);
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-outputType", "i3dm",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case06B3dm() {
        String path = "case06-kml-auto-batched";
        //String path = "case06-kml-auto-instance";
        //sampleI3dm(path, 20 , 100);
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                //"-autoUpAxis",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case07() {
        String path = "case07-shp-seoul";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        File terrain = new File(input, "terrain.tif");
        //FileUtils.deleteQuietly(output);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-terrain", terrain.getAbsolutePath(),
                "-inputType", "shp",
                "-crs", "5181",
                "-maxCount", "32768",
                "-autoUpAxis",
                "-refineAdd",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case08() {
        String path = "case08-geojson-ws2";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-inputType", "geojson",
                "-crs", "5186",
                "-autoUpAxis",
                "-refineAdd",
        };
        Mago3DTilerMain.main(args);
    }

    //@Test
    void case09() {
        String path = "case09-obj-hwangyonggak";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-inputType", "kml",
                "-autoUpAxis",
                //"-crs", "32652"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case10() {
        String path = "case10-laz-hwangyonggak";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-inputType", "laz",
                "-proj", "+proj=utm +zone=52 +datum=WGS84 +units=m +no_defs",
        };
        Mago3DTilerMain.main(args);
    }

    //@Test
    void case011() {
        String path = "case11-shp-jeju";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-inputType", "shp",
                "-refineAdd",
        };
        Mago3DTilerMain.main(args);
    }

    //@Test
    void case012() {
        String path = "case12-kml-seoul";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        //FileUtils.deleteQuietly(output);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-refineAdd",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case013() {
        String path = "case13-kml-seoul-forest-instance";
        File input = new File(INPUT_PATH, path);
        File terrain = new File(input, "terrain.tif");
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-inputType", "shp",
                "-output", output.getAbsolutePath(),
                "-outputType", "i3dm",
                "-terrain", terrain.getAbsolutePath(),
                //"-crs", "5179",
                "-autoUpAxis",
                "-refineAdd",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case014() {
        String path = "case14-3ds-seoul";
        File input = new File(INPUT_PATH, path);
        File terrain = new File(input, "terrain.tif");
        File output = new File(OUTPUT_PATH, path);
        //FileUtils.deleteQuietly(output);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-output", output.getAbsolutePath(),
                "-refineAdd",
                //"-terrain", terrain.getAbsolutePath(),
                "-crs", "5186",
                "-r",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    //@Test
    void sampleI3dm(String filePath, int length, int fileCount) {
        Vector3d min = new Vector3d(128.4602 , 37.7214, 0.0);
        Vector3d max = new Vector3d(128.4802, 37.7414, 0.0);

        File output = new File(INPUT_PATH, filePath);
        if (output.mkdirs()) {
            log.info("output directory created: {}", output.getAbsolutePath());
        }

        int number = 0;
        Random random = new Random();
        String xmlHeader =
                "<?xml version=\"1.0\" encoding=\"UTF - 8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                "    <Document>\n" +
                "        <Placemark name=\"test\">\n" +
                "            <name>Instance Optional" + number + "</name>\n" +
                "            <description>Optional</description>\n";
        String xmlFooter =
                "            <ExtendedData>\n" +
                "                <Data name=\"Height\">\n" +
                "                    <value>3.0</value>\n" +
                "                </Data>\n" +
                "                <Data name=\"Type\">\n" +
                "                    <value>Tree 1</value>\n" +
                "                </Data>\n" +
                "            </ExtendedData>\n" +
                "        </Placemark>\n" +
                "    </Document>\n" +
                "</kml>";

        for (int c = 0; c < fileCount; c++) {
            String xml = "";
            StringBuilder xmlBodys = new StringBuilder();
            String path = "sample-instances-" + number + ".kml";
            System.out.println("path: " + path);
            for (int i = 0; i < length; i++) {
                for (int j = 0; j < length; j++) {
                    double scale = random.nextFloat() * 0.5 + 0.5;
                    //scale = 1.0f;
                    double xpos = ((max.x - min.x) * random.nextFloat()) + min.x;
                    double ypos = ((max.y - min.y) * random.nextFloat()) + min.y;
                    xmlBodys.append(
                            "<Model>\n" +
                            "    <altitudeMode>clampToGround</altitudeMode>\n" +
                            "    <Location>\n" +
                            "        <altitude>0.0</altitude>\n" +
                            "        <longitude>" + xpos + "</longitude>\n" +
                            "        <latitude>" + ypos + "</latitude>\n" +
                            "    </Location>\n" +
                            "    <Orientation>\n" +
                            "        <heading>"+ random.nextInt(360) +"</heading>\n" +
                            "        <tilt>0</tilt>\n" +
                            "        <roll>0</roll>\n" +
                            "    </Orientation>\n" +
                            "    <Scale>\n" +
                            "        <x>" + scale + "</x>\n" +
                            "        <y>" + scale + "</y>\n" +
                            "        <z>" + scale + "</z>\n" +
                            "    </Scale>\n" +
                            "    <Link>\n" +
                            "        <href>tree.dae</href>\n" +
                            "    </Link>\n" +
                            "</Model>\n");
                }
            }
            number++;
            xml += xmlHeader;
            xml += xmlBodys.toString();
            xml += xmlFooter;
            File outputFile = new File(output, path);
            try {
                FileUtils.writeStringToFile(outputFile, xml, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*@Test
    void createTemp() throws IOException {
        int fileCount = 2000000;
        for (int count = 0; count < fileCount; count++) {
            File tempFile = new File("C:\\Workspaces\\GitSources\\mago\\mago-3d-tiler\\viewer\\mago-3d-tiler-data\\case07-shp-seoul\\temp", "building_" + count + ".mgb");
            // write temp file
            tempFile.createNewFile();
        }

    }*/
}