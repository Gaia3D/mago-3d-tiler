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
                // "-rotateXAxis", "90",
                //"-leaveTemp"
                //"-debug",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched02() {
        String path = "B02-wangsuk2-dae";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                // "-rotateXAxis", "90",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched03() {
        String path = "B03-wangsuk2-geojson";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched04() {
        String path = "B04-complicated-3ds";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-tilesVersion", "1.1",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched05() {
        String path = "B05-seoul-part-geojson";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                //"-terrain", ReleaseTestConfig.getInputPath(path).getAbsolutePath() + "/seoul.tif",
                "-terrain", "G:\\(archive)\\(archive) 3차원 데이터 모음\\GeoTIFF\\korea_5m\\5m\\37608(서울)",
                "-c", "5186"
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

    /* This test is disabled because it requires a large input file. */
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
    void batched53_1() {
        String path = "B53-railway-citygml";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-SBN",
                "-c", "5186",
                "-splitByNode"
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
                // "-rotateXAxis", "90",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched71() {
        String path = "B71-pole-base-ifc";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                //"-c", "5186",
                //"-rotateXAxis", "-90",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched72() {
        String path = "B72-student-room-ifc";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                // "-rotateXAxis", "90",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched73() {
        String path = "B73-social-room-ifc";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                // "-rotateXAxis", "90",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched80() {
        String path = "B80-kku-tile-dae-kml";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                // "-rotateXAxis", "90",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched81() {
        String path = "B81-glb-problems-kml";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                //"-rotateX", "90",
                "--crs", "3011",
                //"--xOffset", "151400",
                //"--yOffset", "6577000",
                //"-debug"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void batched82() {
        String path = "B82-glb-problems-good-kml";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "--crs", "3011",
                //"--xOffset", "151400",
                //"--yOffset", "6577000",
                //"-debug"
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
                //"-debug"
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

    @Disabled
    @Test
    void batched92() {
        String path = "B92-buildings";
        String[] args = new String[] {
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-it", "geojson",
                "-heightColumn", "rel_height",
                "-terrain", MagoTestConfig.getInputPath("I10-forest-purdue-original-gpkg4").getAbsolutePath() + "/hamilton_dem_navd88_meters_4326.tif",
                "-crs", "4326",
        };
        MagoTestConfig.execute(args);
    }
}
