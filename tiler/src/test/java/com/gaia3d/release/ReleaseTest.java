package com.gaia3d.release;

import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
class ReleaseTest {
    private static final String INPUT_PATH = "D:\\data\\mago-tiler-data\\release-test-input";
    private static final String OUTPUT_PATH = "C:\\Workspaces\\GitSources\\mago-viewer\\data\\release-test-output";

    @Test
    void testKmlWithCollada() {
        String path = "NAMYANGJU-WANGSUK-KML-DAE";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testMaxStudio() {
        String path = "NAMYANGJU-WANGSUK-3DS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "3ds",
                "-crs", "5186"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testDcLibrary() {
        String path = "DC-LIBRARY-3DS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-crs", "5186"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testPointCloud() {
        String path = "HWANGYONGGAK-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-it", "las",
                "-crs", "32652"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testKoreaHouseGlb() {
        String path = "KOREA-HOUSE-GLB";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-it", "glb",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testSejongPoleIfc() {
        String path = "SEJONG-POLE-IFC";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-crs", "5186",
                "-it", "ifc",
                //"-swapUpAxis",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testAraon() {
        String path = "ARAON-IFC";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-crs", "5186",
                //"-it", "ifc",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testMonkey() {
        String path = "MONKEY-GLB";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-it", "glb",
                //"-swapUpAxis",
                //"-flipUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testYUpAxis() {
        String path = "Y-Up-AXIS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-it", "glb",
                //"-swapUpAxis",
                //"-flipUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testZUpAxis() {
        String path = "Z-Up-AXIS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-it", "glb",
                "-swapUpAxis",
                //"-flipUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testFlipYUpAxis() {
        String path = "FLIP-Y-Up-AXIS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-it", "glb",
                //"-swapUpAxis",
                "-flipUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testFlipZUpAxis() {
        String path = "FLIP-Z-Up-AXIS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-it", "glb",
                "-swapUpAxis",
                "-flipUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testKoryoSoftIfc() {
        String path = "KORYO-SOFT-IFC";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-crs", "5186",
                //"-it", "ifc",
                //"-swapUpAxis",
                //"-flipUpAxis",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testDuckGlb() {
        String path = "DUCK-GLB";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-d",
                //"-it", "glb",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testPointCloudImmdi() {
        String path = "HWANGYONGGAK-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath() + File.separator + "Hwangyounggak_group1_densified_point_cloud.laz",
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-it", "las",
                //"-ot", "pnts",
                //"-crs", "32652",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testSangjiUnivPointCloud() {
        String path = "SANGJUUNIV-LAS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-crs", "5186",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testInstancedModel() {
        String path = "INSTANCED-MODEL-KML";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "kml",
                "-ot", "i3dm",
                //"-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testShapeSeoul() {
        String path = "SEOUL-PART-SHP";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-nameColumn", "PNU",
                "-it", "shp",
                "-crs", "5174",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testGeojsonPolygon() {
        String path = "NAMYANGJU-WANGSUK-GEOJSON";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "geojson",
                "-nameColumn", "layer",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testComplicatedKmlWithMaxStudio() {
        String path = "COMPLICATED-KML-3DS";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "kml",
                "-output", output.getAbsolutePath(),
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testSejongBridge() {
        String path = "SEJONG-BRIDGE-IFC";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "ifc",
                "-output", output.getAbsolutePath(),
                "-debug",
                "-glb"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testCityGmlLod1() {
        String path = "JAPAN-MORAN-CITYGML-LOD1";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "citygml",
                "-crs", "4326",
                "-flipCoordinate",
                "-output", output.getAbsolutePath(),
                "-debug",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testCityGmlLod4() {
        String path = "RAIL-WAY-CITYGML3";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "citygml",
                "-output", output.getAbsolutePath(),
                "-debug",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testGyangGyoShpZ() {
        String path = "GYANGGYO-SHP-Z";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "geojson",
                "-crs", "5186",
                "-output", output.getAbsolutePath(),
                "-debug",
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
