package com.gaia3d.release;

import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
class B3dmReleaseTest {
    private static final String INPUT_PATH = "D:/data/mago-3d-tiler/release-sample";
    private static final String OUTPUT_PATH = "E:/data/mago-server/output";

    @Test
    void batched00() {
        String path = "B00-up-axis-glb";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                //"-debug",
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
                "-leaveTemp"
                //"-debug",
        };
        execute(args);
    }

    @Test
    void batched02() {
        String path = "B02-wangsuk2-dae";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
        };
        execute(args);
    }

    @Test
    void batched03() {
        String path = "B03-wangsuk2-geojson";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
        };
        execute(args);
    }

    @Test
    void batched04() {
        String path = "B04-complicated-3ds";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
        };
        execute(args);
    }

    @Test
    void batched05() {
        String path = "B05-seoul-part-geojson";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-terrain", getInputPath(path).getAbsolutePath() + "/seoul.tif",
                "-c", "5186"
        };
        execute(args);
    }

    @Test
    void batched06() {
        String path = "B06-seoul-yeouido-shp";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-terrain", getInputPath(path).getAbsolutePath() + "/seoul.tif",
                "-c", "5186"
        };
        execute(args);
    }

    @Test
    void batched07() {
        String path = "B07-sejong-pipe-geojson";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5186"
        };
        execute(args);
    }

    /* This test is disabled because it requires a large input file. */
    @Disabled
    @Test
    void batched08() {
        String path = "B08-seoul-shape";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "-skirtHeight", "10",
                "-terrain", getInputPath(path).getAbsolutePath() + "/korea-compressed.tif",
        };
        execute(args);
    }

    @Test
    void batched09() {
        String path = "B09-seoul-yeouido-gpkg";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "-terrain", getInputPath(path).getAbsolutePath() + "/seoul.tif",
        };
        execute(args);
    }

    @Test
    void batched50() {
        String path = "B50-wangsuk2-citygml";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5186"
        };
        execute(args);
    }

    @Test
    void batched51() {
        String path = "B51-japan-moran-citygml";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "4326",
                "-flipCoordinate",
        };
        execute(args);
    }

    @Test
    void batched52() {
        String path = "B52-house-citygml";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
        };
        execute(args);
    }

    @Test
    void batched53() {
        String path = "B53-railway-citygml";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
        };
        execute(args);
    }

    @Test
    void batched70() {
        String path = "B70-sejong-bridge-ifc";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
        };
        execute(args);
    }

    @Test
    void batched71() {
        String path = "B71-pole-base-ifc";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
        };
        execute(args);
    }

    @Test
    void batched72() {
        String path = "B72-student-room-ifc";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
        };
        execute(args);
    }

    @Test
    void batched73() {
        String path = "B73-social-room-ifc";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
        };
        execute(args);
    }

    @Test
    void batched80() {
        String path = "B80-kku-tile-dae-kml";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
        };
        execute(args);
    }

    @Test
    void batched82() {
        String path = "B82-glb-problems-good-kml";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "--crs", "3011",
                "--xOffset", "151400",
                "--yOffset", "6577000",
                "-debug"
        };
        execute(args);
    }

    @Test
    void batched81() {
        String path = "B81-glb-problems-kml";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                //"-rotateX", "90",
                "--crs", "3011",
                "--xOffset", "151400",
                "--yOffset", "6577000",
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
