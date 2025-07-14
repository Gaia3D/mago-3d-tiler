package com.gaia3d.release.small;

import com.gaia3d.command.Configuration;
import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Tag("release")
@Slf4j
class I3dmReleaseTest {
    private static final String INPUT_PATH = "D:/data/mago-3d-tiler/release-sample";
    private static final String OUTPUT_PATH = "E:/data/mago-server/output";

    static {
        Configuration.initConsoleLogger();
    }

    @Test
    void instanced00() {
        String path = "I00-forest-kml";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-ot", "i3dm",
                "-c", "5186",
        };
        execute(args);
    }

    @Test
    void instanced01() {
        String path = "I01-seoul-forest-shp";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "shp",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", getInputPath(path).getAbsolutePath() + "/tree.dae",
                "-terrain", getInputPath(path).getAbsolutePath() + "/seoul-aster.tif",
                "-c", "5186"
        };
        execute(args);
    }

    @Test
    void instanced02() {
        String path = "I02-seoul-forest-geojson";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "geojson",
                "-refineAdd",
                "-instance", getInputPath(path).getAbsolutePath() + "/tree.dae",
                "-terrain", getInputPath(path).getAbsolutePath() + "/seoul-aster.tif",
                "-ot", "i3dm",
                "-c", "5186"
        };
        execute(args);
    }

    @Test
    void instanced03() {
        String path = "I03-seoul-yeouido-gpkg";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "-it", "gpkg",
                "-ot", "i3dm",
                "-terrain", getInputPath(path).getAbsolutePath() + "/seoul-aster.tif",
                "-instance", getInputPath(path).getAbsolutePath() + "/tree.dae",
                //"-debug"
        };
        execute(args);
    }

    @Test
    void instanced04() {
        String path = "I04-forest-shp";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5179",
                "-ot", "i3dm",
                "-instance", getInputPath(path).getAbsolutePath() + "/needle-tree.glb",
                "-terrain", INPUT_PATH + "/korea-compressed.tif",
                //"-debug"
        };
        execute(args);
    }

    @Test
    void instanced04Extend() {
        String path = "I04-forest-shp-extend";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5179",
                "-ot", "i3dm",
                "-instance", getInputPath(path).getAbsolutePath() + "/lowpoly-tree-broad-leaved-big.glb",
                "-terrain", INPUT_PATH + "/korea-compressed.tif",
                //"-debug"
        };
        execute(args);
    }

    @Test
    void instanced06A() {
        String path = "I04-forest-shp";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath("ESD/" + path).getAbsolutePath() + "-A",
                "-c", "5179",
                "-it", "gpkg",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/broad-tree-1m.glb",
                //"-terrain", INPUT_PATH + "/korea-compressed.tif",
                "-terrain", "G:/workspace/dem05-cog.tif",
                "-attributeFilter", "FRTP_NM=활엽수림",
                //"-debug"
        };
        execute(args);
    }

    @Test
    void instanced06B() {
        String path = "I04-forest-shp";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath("ESD/" + path).getAbsolutePath() + "-B",
                "-c", "5179",
                "-it", "gpkg",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/needle-tree-1m.glb",
                "-terrain", "G:/workspace/dem05-cog.tif",
                "-attributeFilter", "FRTP_NM=침엽수림",
                //"-debug"
        };
        execute(args);
    }

    @Test
    void instanced06C() {
        String path = "I04-forest-shp";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath("ESD/" + path).getAbsolutePath() + "-C",
                "-c", "5179",
                "-it", "gpkg",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/mix-tree-1m.glb",
                "-terrain", "G:/workspace/dem05-cog.tif",
                "-attributeFilter", "FRTP_NM=혼효림",
                //"-debug"
        };
        execute(args);
    }

    @Test
    void instanced06D() {
        String path = "I04-forest-shp";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath("ESD/" + path).getAbsolutePath() + "-D",
                "-c", "5179",
                "-it", "gpkg",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/bamboo-tree-1m.glb",
                "-terrain", "G:/workspace/dem05-cog.tif",
                "-attributeFilter", "FRTP_NM=죽림",
                //"-debug"
        };
        execute(args);
    }

    @Test
    void instanced06Merger() {
        String path = "ESD";
        String[] args = new String[] {
                "-i", getOutputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-c", "5179",
                "-merge",
                //"-ot", "i3dm",
        };
        execute(args);
    }

    @Test
    void instanced07Chim() {
        String path = "I07-tree-entities-shp-chim";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5187",
                "-it", "shp",
                "-ot", "i3dm",
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/chim-sample.glb",
                "-terrain", "G:/workspace/dem05.tif",
                //"-debug"
        };
        execute(args);
    }

    @Test
    void instanced07jat() {
        String path = "I07-tree-entities-shp-jat";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5187",
                "-it", "shp",
                "-ot", "i3dm",
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/jat-sample.glb",
                "-terrain", "G:/workspace/dem05.tif",
                //"-debug"
        };
        execute(args);
    }

    @Test
    void instanced07Hwal() {
        String path = "I07-tree-entities-shp-hwal";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5187",
                "-it", "shp",
                "-ot", "i3dm",
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/hwal-sample.glb",
                "-terrain", "G:/workspace/dem05.tif",
                //"-debug"
        };
        execute(args);
    }

    @Test
    void instanced07Nak() {
        String path = "I07-tree-entities-shp-nak";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5187",
                "-it", "shp",
                "-ot", "i3dm",
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/nak-sample.glb",
                "-terrain", "G:/workspace/dem05.tif",
                //"-debug"
        };
        execute(args);
    }

    @Test
    void instanced07Buts() {
        String path = "I07-tree-entities-shp-nak";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath()+"-cherry-blossom",
                "-c", "5187",
                "-it", "shp",
                "-ot", "i3dm",
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/cherry-blossom.glb",
                "-terrain", "G:/workspace/dem05.tif",
        };
        execute(args);
    }

    @Test
    void instanced08() {
        String path = "I08-transmission-geojson";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5179",
                "-it", "geojson",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", getInputPath(path).getAbsolutePath() + "/lite.glb",
                "-terrain", "G:/workspace/dem05.tif",
        };
        execute(args);
    }

    @Test
    void instanced09() {
        String path = "I09-transmission-line-geojson";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5179",
                "-it", "geojson",
                "-terrain", "G:/workspace/dem05.tif",
                //"-debug"
        };
        execute(args);
    }
    @Test
    void instance10() {
        String path = "I10-forest-purdue";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-ot", "i3dm",
                "--refineAdd",
                "--scaleColumn", "rel_height",
                "--instance", "D:\\data\\mago-3d-tiler\\release-sample\\sample-tree\\broad-tree-1m.glb",
                //"-terrain", getInputPath(path).getAbsolutePath() + "/seoul-aster.tif",
        };
        execute(args);
    }

    @Test
    void instance10A() {
        String path = "I10-forest-purdue-original-geojson";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-ot", "i3dm",
                "--refineAdd",
                "--scaleColumn", "rel_height",
                "--instance", "D:\\data\\mago-3d-tiler\\release-sample\\sample-tree\\broad-tree-1m.glb",
                //"-terrain", getInputPath(path).getAbsolutePath() + "/seoul-aster.tif",
        };
        execute(args);
    }

    @Test
    void instance10B() {
        String path = "I10-forest-purdue-original-gpkg";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-ot", "i3dm",
                "--refineAdd",
                "--scaleColumn", "rel_height",
                "--instance", "D:\\data\\mago-3d-tiler\\release-sample\\sample-tree\\broad-tree-1m.glb",
                //"-terrain", getInputPath(path).getAbsolutePath() + "/seoul-aster.tif",
        };
        execute(args);
    }

    @Test
    void instance10C() {
        String path = "I10-forest-purdue-original-gpkg2";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "gpkg",
                "-ot", "i3dm",
                "--refineAdd",
                "--scaleColumn", "rel_height_m",
                "--instance", "D:\\data\\mago-3d-tiler\\release-sample\\sample-tree\\broad-tree-1m.glb",
                //"-terrain", getInputPath(path).getAbsolutePath() + "/seoul-aster.tif",
        };
        execute(args);
    }

    @Test
    void instance10D() {
        String path = "I10-forest-purdue-original-gpkg3";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "gpkg",
                "-ot", "i3dm",
                "--refineAdd",
                "--scaleColumn", "rel_height_m",
                "--instance", "D:\\data\\mago-3d-tiler\\release-sample\\sample-tree\\broad-tree-1m.glb",
                //"-terrain", getInputPath(path).getAbsolutePath() + "/seoul-aster.tif",
        };
        execute(args);
    }

    @Test
    void instance10E() {
        String path = "I10-forest-purdue-original-gpkg4";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-crs", "4326",
                "-it", "gpkg",
                "-ot", "i3dm",
                "--refineAdd",
                "--scaleColumn", "rel_height_m",
                "--instance", "D:\\data\\mago-3d-tiler\\release-sample\\sample-tree\\broad-tree-1m.glb",
                "-terrain", getInputPath(path).getAbsolutePath() + "/hamilton_dem_navd88_meters_4326.tif",
        };
        execute(args);
    }


    @Test
    void runWithSimple() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();

        String dockerImage = "gaia3d/mago-3d-tiler:latest";
        List<String> argList = new ArrayList<>();
        argList.add("docker");
        argList.add("run");
        argList.add("--rm");
        argList.add("-v");
        argList.add(INPUT_PATH + ":/input");
        argList.add("-v");
        argList.add(OUTPUT_PATH + ":/output");
        argList.add(dockerImage);
        argList.add("--input");
        argList.add("/input/I10-forest-purdue-original-gpkg4");
        argList.add("--output");
        argList.add("/output/I10-forest-purdue-original-gpkg4");
        argList.add("-it");
        argList.add("gpkg");
        argList.add("-ot");
        argList.add("i3dm");
        argList.add("--refineAdd");
        argList.add("--scaleColumn");
        argList.add("rel_height_m");
        argList.add("--instance");
        argList.add("/input/I10-forest-purdue-original-gpkg4/broad-tree-1m.glb");
        argList.add("-terrain");
        argList.add("/input/I10-forest-purdue-original-gpkg4/hamilton_dem_navd88_meters_4326.tif");

        runCommand(argList);
    }

    private void runCommand(List<String> argList) throws IOException {
        String[] args = argList.toArray(new String[0]);

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.redirectErrorStream(true);
        StringBuilder stringBuilder = new StringBuilder();
        for (String arg : args) {
            stringBuilder.append(arg).append(" ");
        }
        String command = stringBuilder.toString();
        Process process = processBuilder.start();
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        log.info("Executing command: {}", command);
        log.info("***Starting command execution***");
        for (String str; (str = inputReader.readLine()) != null; ) {
            log.info(str);
        }
        for (String str; (str = errorReader.readLine()) != null; ) {
            log.error(str);
        }
        log.info("***Command executed successfully***");
    }

    private void execute(String[] args) {
        Mago3DTilerMain.main(args);
    }

    private File getInputPath(String path) {
        return new File(INPUT_PATH, path);
    }

    private File getOutputPath(String path) {
        return new File(OUTPUT_PATH, path);
    }
}
