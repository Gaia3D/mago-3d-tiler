package com.gaia3d.visual.release;

import com.gaia3d.visual.MagoTestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("release")
@Slf4j
class B3dmReleaseTest {

    @Test
    void batched00() {
        String path = "B00-up-axis-glb";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched01() {
        String path = "B01-wangsuk2-3ds";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "--quantize",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched02() {
        String path = "B02-wangsuk2-dae";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched03() {
        String path = "B03-wangsuk2-geojson";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "4326",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched04() {
        String path = "B04-complicated-3ds";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched06() {
        String path = "B06-seoul-yeouido-shp";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-terrain", MagoTestConfig.getInputPath(path).getAbsolutePath() + "/seoul.tif",
                "-c", "5186"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched07() {
        String path = "B07-sejong-pipe-geojson";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-debug",
                "-c", "5186"
        };
        MagoTestConfig.execute(args);
    }

    @Disabled
    @Test
    void batched08() {
        String path = "B08-seoul-shape";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "-skirtHeight", "10",
                "-terrain", MagoTestConfig.getInputPath(path).getAbsolutePath() + "/korea-compressed.tif",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched09() {
        String path = "B09-seoul-yeouido-gpkg";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "-terrain", MagoTestConfig.getInputPath(path).getAbsolutePath() + "/seoul.tif",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched50() {
        String path = "B50-wangsuk2-citygml";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched51() {
        String path = "B51-japan-moran-citygml";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "4326",
                "-flipCoordinate",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched52() {
        String path = "B52-house-citygml";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched53() {
        String path = "B53-railway-citygml";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched70() {
        String path = "B70-sejong-bridge-ifc";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched71() {
        String path = "B71-pole-base-ifc";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched72() {
        String path = "B72-student-room-ifc";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched73() {
        String path = "B73-social-room-ifc";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched74() {
        String path = "B74-student-room-citygml";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-it", "citygml",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched75() {
        String path = "B75-social-room-citygml";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-it", "citygml",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched90() {
        String path = "B90-compo";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-tilesVersion", "1.1",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched91() {
        String path = "B91-buildings";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-it", "geojson",
                "-heightColumn", "rel_height",
                "-tilesVersion", "1.1",
                "-c", "3857",
        };
        MagoTestConfig.execute(args);
    }
}
