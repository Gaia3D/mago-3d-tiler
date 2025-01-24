package com.gaia3d.release.old;

import com.gaia3d.command.mago.Mago3DTilerMain;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Deprecated
public class PointsTest {
    private static final String INPUT_PATH = "D:\\data\\mago-tiler-data\\stress-test-input";
    //private static final String OUTPUT_PATH = "D:\\workspaces\\mago-viewer\\data\\stress-test-output";
    private static final String OUTPUT_PATH = "E:/data/mago-server/output";

    @Disabled
    @Test
    void testPostLas() {
        String path = "POST-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-crs", "3857",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void testDemoJinguMini() {
        String path = "JINGU-LAS-MINI";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-crs", "5187",
                "-pointRatio", "100",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void testDemoJinguBig() {
        String path = "JINGU-LAS-BIG";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-crs", "5187",
                "-pointRatio", "100",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void testDemoLas() {
        String path = "DEMO-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-crs", "32648",
                "-pointRatio", "50",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void testDemoLasAll() {
        String path = "DEMO-LAS-ALL";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-crs", "32648",
                "-pointRatio", "100",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void testThaiQuarter() {
        String path = "THAILAND-QUARTER-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-crs", "32648",
                "-pointRatio", "25",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testThaiAll() {
        String path = "THAILAND-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-crs", "32648",
                "-pointRatio", "25",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testSangAmLas() {
        String path = "SANGAM-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-proj", "+proj=utm +zone=52 +datum=WGS84 +units=m +no_defs",
                "-pointRatio", "25",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testHighwayLas() {
        String path = "HIGHWAY-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-crs", "5186",
                //"-pointSkip", "1",
                //"-pointSkip", "4",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void testSewoonLas() {
        // wrong bounding box
        String path = "SEWOON-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-proj", "+proj=utm +zone=52 +datum=WGS84 +units=m +no_defs",
                //"-pointSkip", "1",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testSangjiUniLas() {
        String path = "SANGJI-UNI-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-crs", "5186",
                "-force4ByteRGB",
                "-pointRatio", "100",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testNdtpLas() {
        String path = "NDTP-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-crs", "5186",
                "-pointRatio", "25",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testSinchonLas() {
        String path = "SINCHON-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-proj", "+proj=utm +zone=52 +datum=WGS84 +units=m +no_defs",
                "-pointRatio", "25",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testDaeryung01() {
        String path = "DAERYUNG01-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-crs", "5186",
                "-pointRatio", "100",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testDaeryung02() {
        String path = "DAERYUNG02-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-crs", "5186",
                "-pointRatio", "50",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testBAD() {
        String path = "BAD-KML-GLB";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "kml",
                "-ot", "i3dm",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void convertKoreaSeoul() {
        String path = "KOREA-SEOUL-SHP";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "shp",
                "-crs", "5186",
                "-terrain", getInputPath(path).getAbsolutePath() + File.separator + "korea-compressed.tif",
        };
        Mago3DTilerMain.main(args);
    }


    private File getInputPath(String path) {
        return new File(INPUT_PATH, path);
    }

    private File getOutputPath(String path) {
        return new File(OUTPUT_PATH, path);
    }
}
