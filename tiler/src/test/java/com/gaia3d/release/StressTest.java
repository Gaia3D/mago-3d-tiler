package com.gaia3d.release;

import com.gaia3d.command.mago.Mago3DTilerMain;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

public class StressTest {
    private static final String INPUT_PATH = "D:\\data\\mago-tiler-data\\stress-test-input";
    //private static final String OUTPUT_PATH = "D:\\workspaces\\mago-viewer\\data\\stress-test-output";
    private static final String OUTPUT_PATH = "D:/data/mago-server/output";

    @Disabled
    @Test
    void cityGmlLod1() {
        String path = "CITYGML-LOD1";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "citygml",
                "-crs", "5186",
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void testIfcMep() {
        String path = "LARGE-MEP-IFC";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "ifc",
                //"-crs", "5186",
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void testOSMShape() {
        String path = "OSM-SHP";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "shp",
                "-crs", "4326",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void testYeouidoShape() {
        String path = "YEOUIDO-SHP";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "shp",
                "-crs", "5186",
                //"-terrain", getInputPath(path).getAbsolutePath() + File.separator + "korea-compressed.tif",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void testSeoulShape() {
        String path = "SEOUL-SHP";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "shp",
                "-crs", "5186",
                //"-terrain", getInputPath(path).getAbsolutePath() + File.separator + "korea-compressed.tif",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void testSouthKoreaShape() {
        String path = "SOUTH-KOREA-SHP";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "shp",
                "-crs", "5186",
                "-terrain", getInputPath(path).getAbsolutePath() + File.separator + "korea-compressed.tif",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void testSejongUndergroundShape() {
        String path = "SEJONG-UNDERGROUND-SHP";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "shp",
                "-crs", "5186",
                "-dc", "STD_DIP",
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void testLargeBuildingFbxFromHayashiSang() {
        String path = "LARGE-BUILDING-FBX";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-it", "fbx",
                "-crs", "6674",
                "-minLod", "0",
                "-maxLod", "0",
                //"-refineAdd",
                "-swapUpAxis",
                //"-reverseUpAxis",
                //"-rotateUpAxis",
                //"-glb",
                //"-largeMesh",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void testSangJiUni() {
        String path = "SANGJI-UNI-DAE";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-it", "dae",
                //"-crs", "5186",
                "-minLod", "0",
                "-maxLod", "0",
                //"-autoUpAxis",
                // "-rotateUpAxis",
                "-refineAdd",
                //"-glb",
                //"-recursive",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void testSaehanCollada() {
        String path = "SEAHAN-DAE";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "kml",
                "-crs", "5186",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void testDemoObj() {
        String path = "DEMO-OBJ";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "obj",
                "-crs", "32648",
                "-xOffset", "268943",
                "-yOffset", "1818915",
                //"-proj", "+proj=utm +lat_0=0 +lon_0=0 +x_0=268943 +y_0=1818915 +zone=48 +datum=WGS84 +units=m +no_defs",
                //"-proj", "+proj=gnom +lat_0=16.440659400004286 +lon_0=102.83604640169834 +x_0=0 +y_0=0 +ellps=WGS84 +datum=WGS84 +units=m +no_defs",
                //"-rotateX", "-90",
                "-minLod", "0",
                "-maxLod", "0",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

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

    @Test
    void testDemoJingu() {
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

    @Test
    void testDemoLasAll() {
        String path = "DEMO-LAS-ALL";
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

    @Disabled
    @Test
    void testSangjiUniLas() {
        String path = "SANGJI-UNI-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-crs", "5186",
                "-force4ByteRGB",
                "-pointRatio", "6",
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

    @Disabled
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


    private File getInputPath(String path) {
        return new File(INPUT_PATH, path);
    }

    private File getOutputPath(String path) {
        return new File(OUTPUT_PATH, path);
    }
}
