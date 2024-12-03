package com.gaia3d.release;

import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
class ReleaseTest {
    private static final String INPUT_PATH = "D:\\data\\mago-tiler-data\\release-test-input";
    //private static final String OUTPUT_PATH = "D:\\workspaces\\mago-viewer\\data\\release-test-output";
    private static final String OUTPUT_PATH = "D:/data/mago-server/output";

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
                //"-debug"
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
                //"-debug"
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
    void testZUpAxis() {
        String path = "Z-UP-AXIS";
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
        String path = "Y-UP-AXIS";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-swapUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testFlipYUpAxis() {
        String path = "FLIP-Y-UP-AXIS";
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
        String path = "FLIP-Z-UP-AXIS";
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
                //"-debug"
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
    void testInstancedModel() {
        String path = "INSTANCED-MODEL-KML-3DS";
        FileUtils.deleteQuietly(getOutputPath(path));
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-ot", "i3dm",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testInstancedModel2() {
        String path = "INSTANCED-MODEL-KML-DAE";
        FileUtils.deleteQuietly(getOutputPath(path));
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-ot", "i3dm",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testInstancedModel3() {
        String path = "INSTANCED-MODEL-KML-GLB";
        FileUtils.deleteQuietly(getOutputPath(path));
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-ot", "i3dm",
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void testI3dmSeoulTrees() {
        String path = "I3DM-SEOUL-TREES";
        FileUtils.deleteQuietly(getOutputPath(path));
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "shp",
                "-ot", "i3dm",
                "-instance", getInputPath(path).getAbsolutePath() + "/tree.dae",
                "-crs", "5186",
                //"-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void testI3dmSeoulTreesFromGeojson() {
        String path = "I3DM-SEOUL-TREES-GEOJSON";
        FileUtils.deleteQuietly(getOutputPath(path));
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "geojson",
                "-ot", "i3dm",
                "-instance", getInputPath(path).getAbsolutePath() + File.separator + "temp/tree.dae",
                "-terrain", getInputPath(path).getAbsolutePath() + File.separator + "korea-compressed.tif",
                "-crs", "5186",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void testI3dmSeoulVoxelTreesFromGeojson() {
        String path = "I3DM-SEOUL-TREES-GEOJSON-MINI";
        FileUtils.deleteQuietly(getOutputPath(path));
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "geojson",
                "-ot", "i3dm",
                "-instance", getInputPath(path).getAbsolutePath() + File.separator + "temp/tree.dae",
                "-terrain", getInputPath(path).getAbsolutePath() + File.separator + "korea-compressed.tif",
                "-crs", "5186",
                "-voxelLod",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Disabled
    @Test
    void sejongWaterPipe() {
        String path = "SEJEONG-WATER-PIPE-GEOJSON";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "geojson",
                "-crs", "5186",
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
                //"-crs", "5174",
                "-proj", "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43",
                //"-debug"
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
                "-crs", "4326",
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
                "-debug"
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
                //"-glb"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testCityGmlLod1Moran() {
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
                //"-debug",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testNoiseResult() {
        String path = "NOISE-RESULT-GLB-DAY";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-rotateX", "-90",
                "-debug",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testNoiseResultNight() {
        String path = "NOISE-RESULT-GLB-NIGHT";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-rotateX", "-90",
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
                "-crs", "5186",
                "-debug",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testCityGmlLod4WithTerrain() {
        String path = "RAIL-WAY-CITYGML3-TERRAIN";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "citygml",
                "-output", output.getAbsolutePath(),
                "-glb",
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
                //"-hc", "Z_Min",
                //"-debug",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testIndiaPipeShp() {
        String path = "INDIA-PIPE-SHP";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "shp",
                "-crs", "32643",
                "-output", output.getAbsolutePath(),
                "-dc", "length",
                //"-debug",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testGyangGyoInterior3ds() {
        String path = "GYANGGYO-INTERIOR-3DS";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "3ds",
                "-crs", "5186",
                "-output", output.getAbsolutePath(),
                "-debug",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testGyangGyoInteriorOjb() {
        String path = "GYANGGYO-INTERIOR-OBJ";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "obj",
                "-crs", "5186",
                "-output", output.getAbsolutePath(),
                "-debug",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testGyangGyoBridgeOjb() {
        String path = "GYANGGYO-BRIDGE-OBJ";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "citygml",
                "-crs", "5186",
                "-output", output.getAbsolutePath(),
                //"-debug",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testObj() {
        String path = "AMYOK-OBJ";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-inputType", "obj",
                "-crs", "27700",
                "-output", output.getAbsolutePath(),
                "-rotateX", "-90",
                //"-debug",
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
