package com.gaia3d.release;

import com.gaia3d.command.mago.Mago3DTilerMain;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

public class StressTest {
    private static final String INPUT_PATH = "D:\\data\\mago-tiler-data\\stress-test-input";
    private static final String OUTPUT_PATH = "C:\\Workspaces\\GitSources\\mago-viewer\\data\\stress-test-output";

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

    @Test
    void testDemoLas() {
        String path = "DEMO-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-crs", "32648",
                "-pointSkip", "1",
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
                //"-pointSkip", "8",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

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

    @Test
    void testSangAmLas() {
        String path = "SANGAM-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-crs", "5186",
                //"-pointSkip", "4",
                "-proj", "+proj=utm +zone=52 +datum=WGS84 +units=m +no_defs",
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
                //"-pointSkip", "4",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testSewoonLas() {
        String path = "SEWOON-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                "-proj", "+proj=utm +zone=52 +datum=WGS84 +units=m +no_defs",
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
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testSinChonLas() {
        String path = "SINCHON-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "las",
                //"-crs", "5186",
                "-proj", "+proj=utm +zone=52 +datum=WGS84 +units=m +no_defs",
                //"-pointSkip", "8",
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
