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
    void batched03() {
        String path = "B03-wangsuk2-geojson";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-tilesVersion", "1.1",
                "-debug",
        };
        execute(args);
    }

    @Test
    void batched01() {
        String path = "B01-wangsuk2-3ds";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "-tilesVersion", "1.1",
                "-debug",
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
    void pointcloud00() {
        String path = "P00-hwangyonggak-las";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "32652",
        };
        execute(args);
    }

    @Test
    void realistic00() {
        String path = "R00-bansong-obj";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-it", "obj",
                "-pg",
                "-c", "5187",
                "-rotateX", "90",
                "-leaveTemp",
                //"-glb",
                //"-debug",
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
