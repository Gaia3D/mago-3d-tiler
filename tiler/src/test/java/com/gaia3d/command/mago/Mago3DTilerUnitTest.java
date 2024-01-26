package com.gaia3d.command.mago;

import lombok.extern.slf4j.Slf4j;
import org.joml.Random;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
class Mago3DTilerUnitTest {

    private static final String INPUT_PATH = "D:\\Mago3DTiler-UnitTest\\input";
    private static final String OUTPUT_PATH = "D:\\Mago3DTiler-UnitTest\\output";

    @Test
    void case01() {
        String path = "case01-3ds-ws2";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-crs", "5186",
                "-output", output.getAbsolutePath(),
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case02() {
        String path = "case02-kml-ws2";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case03() {
        String path = "case03-shp-seoul";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-inputType", "shp",
                "-output", output.getAbsolutePath(),
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
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-inputType", "las",
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
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-outputType", "i3dm",
                "-autoUpAxis",
                "-glb"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void case06() {
        String path = "auto-created-i3dm";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-outputType", "i3dm",
                "-autoUpAxis",
                "-glb"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void sampleI3dm() {
        Vector3d min = new Vector3d(126.728563, 37.370850, 0.0);
        Vector3d max = new Vector3d(126.733563, 37.375850, 0.0);

        File output = new File(INPUT_PATH, "auto-created-i3dm");
        output.mkdirs();

        int number = 0;
        int length = 100;
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                //random position in the bounding box
                double xpos = ((max.x - min.x) * random.nextFloat()) + min.x;
                double ypos = ((max.y - min.y) * random.nextFloat()) + min.y;

                String path = "sample-i3dm-" + number + ".kml";
                String xml =
                        "<?xml version=\"1.0\" encoding=\"UTF - 8\"?>\n" +
                                "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                                "    <Document>\n" +
                                "        <Placemark name=\"test\">\n" +
                                "            <name>Instance Optional" + number + "</name>\n" +
                                "            <description>Optional</description>\n" +
                                "            <Model>\n" +
                                "                <altitudeMode>clampToGround</altitudeMode>\n" +
                                "                <Location>\n" +
                                "                    <altitude>0.0</altitude>\n" +
                                "                    <longitude>" + xpos + "</longitude>\n" +
                                "                    <latitude>" + ypos + "</latitude>\n" +
                                "                </Location>\n" +
                                "                <Orientation>\n" +
                                "                    <heading>0</heading>\n" +
                                "                    <tilt>0</tilt>\n" +
                                "                    <roll>0</roll>\n" +
                                "                </Orientation>\n" +
                                "                <Scale>\n" +
                                "                    <x>1</x>\n" +
                                "                    <y>1</y>\n" +
                                "                    <z>1</z>\n" +
                                "                </Scale>\n" +
                                "                <Link>\n" +
                                "                    <href>instance.dae</href>\n" +
                                "                </Link>\n" +
                                "            </Model>\n" +
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
                number++;
                File outputFile = new File(output, path);
                try {
                    org.apache.commons.io.FileUtils.writeStringToFile(outputFile, xml, "UTF-8");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}