package com.gaia3d.release.small;

import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;

@Tag("release")
@Slf4j
class I3dmReleaseTest {
    private static final String INPUT_PATH = "D:/data/mago-3d-tiler/release-sample";
    private static final String OUTPUT_PATH = "E:/data/mago-server/output";

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
                "-instance", getInputPath(path).getAbsolutePath() + "/tree.dae",
                "-terrain", getInputPath(path).getAbsolutePath() + "/seoul-aster.tif",
                "-debug",
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
                "-debug"
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
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/broad-bluegrass.glb",
                //"-terrain", INPUT_PATH + "/korea-compressed.tif",
                "-terrain", "G:/workspace/dem05.tif",
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
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/needle.glb",
                "-terrain", "G:/workspace/dem05.tif",
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
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/mixed.glb",
                "-terrain", "G:/workspace/dem05.tif",
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
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/bamboo.glb",
                "-terrain", "G:/workspace/dem05.tif",
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
                "-c", "5179",
                "-merge",
                "-ot", "i3dm",
        };
        execute(args);
    }

    /*@Test
    void instanced06E() {
        String path = "I04-forest-shp";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath() + "-textured",
                "-c", "5179",
                "-it", "gpkg",
                "-ot", "i3dm",
                "-instance", getInputPath(path).getAbsolutePath() + "/texture-tree.glb",
                "-terrain", "G:/workspace/dem05.tif",
                //"-debug"
        };
        execute(args);
    }*/

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
                "-debug"
        };
        execute(args);
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
