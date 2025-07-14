package com.gaia3d.release.small;

import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;

@Tag("release")
@Slf4j
class DefaultReleaseTest {
    private static final String INPUT_PATH = "D:/data/mago-3d-tiler/release-sample";
    private static final String OUTPUT_PATH = "E:/data/mago-server/output";

    @Test
    void incheon() {
        String path = "incheon";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5186"
        };
        execute(args);
    }


    @Test
    void batched53V1() {
        String path = "B53-railway-citygml";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath() + "-V1",
                "-tilesVersion", "1.0",
                "-c", "5186"
        };
        execute(args);
    }

    @Test
    void batched53V2() {
        String path = "B53-railway-citygml";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath() + "-V2",
                "-tilesVersion", "1.1",
                "-c", "5186"
        };
        execute(args);
    }

    @Test
    void batched03V1() {
        String path = "B03-wangsuk2-geojson";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath() + "-V1",
                "-tilesVersion", "1.0",
                "-debug",
        };
        execute(args);
    }

    @Test
    void batched03V2() {
        String path = "B03-wangsuk2-geojson";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath() + "-V2",
                "-tilesVersion", "1.1",
                "-debug",
        };
        execute(args);
    }

    @Test
    void batched01V1() {
        String path = "B01-wangsuk2-3ds";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath() + "-V1",
                "-c", "5186",
                "-rotateXAxis", "90",
                "-tilesVersion", "1.0",
                "-debug",
        };
        execute(args);
    }

    @Test
    void batched01V2() {
        String path = "B01-wangsuk2-3ds";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath() + "-V2",
                "-c", "5186",
                "-rotateXAxis", "90",
                "-tilesVersion", "1.1",
                "-debug",
        };
        execute(args);
    }

    @Test
    void instanced06V1() {
        String path = "I04-forest-shp";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath() + "-V1",
                "-c", "5179",
                "-it", "gpkg",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/needle-tree-1m.glb",
                "-terrain", "G:/workspace/dem05-cog.tif",
                "-attributeFilter", "FRTP_NM=침엽수림",
                "-tilesVersion", "1.0",
        };
        execute(args);
    }

    @Test
    void  instanced06V2() {
        String path = "I04-forest-shp";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath() + "-V2",
                "-c", "5179",
                "-it", "gpkg",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/needle-tree-1m.glb",
                "-terrain", "G:/workspace/dem05-cog.tif",
                "-attributeFilter", "FRTP_NM=침엽수림",
                "-tilesVersion", "1.1",
        };
        execute(args);
    }


    @Test
    void pointcloud00V1() {
        String path = "P00-hwangyonggak-las";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath() + "-V1",
                "-tilesVersion", "1.0",
                "-c", "32652",
        };
        execute(args);
    }

    @Test
    void pointcloud00V2() {
        String path = "P00-hwangyonggak-las";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath() + "-V2",
                "-c", "32652",
                "-tilesVersion", "1.1",
        };
        execute(args);
    }

    @Test
    void realistic00V1() {
        String path = "R00-bansong-obj";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath() + "-V1",
                "-it", "obj",
                "-pg",
                "-c", "5187",
                "-rotateX", "90",
                "-tilesVersion", "1.0",
        };
        execute(args);
    }

    @Test
    void realistic00V2() {
        String path = "R00-bansong-obj";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath() + "-V2",
                "-it", "obj",
                "-ot", "photogrammetry",
                //"-pg",
                "-c", "5187",
                "-rotateX", "90",
                "-tilesVersion", "1.1",
        };
        execute(args);
    }

    @Test
    void pointcloud06V1() {
        String path = "P06-classification-las";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath() + "-V1",
                "-tilesVersion", "1.0",
        };
        execute(args);
    }

    @Test
    void pointcloud06V2() {
        String path = "P06-classification-las";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath() + "-V2",
                "-tilesVersion", "1.1",
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
