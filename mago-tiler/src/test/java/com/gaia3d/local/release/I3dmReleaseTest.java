package com.gaia3d.local.release;

import com.gaia3d.command.LoggingConfiguration;
import com.gaia3d.local.MagoTestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("release")
@Slf4j
class I3dmReleaseTest {
    static {
        LoggingConfiguration.initConsoleLogger();
    }

    @Test
    void instanced00() {
        String path = "I00-forest-kml";
        String[] args = new String[]{
                "-tilesVersion", "1.0",
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-ot", "i3dm",
                "-c", "5186",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void instanced01() {
        String path = "I01-seoul-forest-shp";
        String[] args = new String[]{
                "-tilesVersion", "1.0",
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-it", "shp",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", MagoTestConfig.getInputPath("sample-tree").getAbsolutePath() + "/billboard-tree.glb",
                "-c", "5186"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void instanced02() {
        String path = "I02-seoul-forest-geojson";
        String[] args = new String[]{
                "-tilesVersion", "1.0",
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-it", "geojson",
                "-refineAdd",
                "-instance", MagoTestConfig.getInputPath("sample-tree").getAbsolutePath() + "/billboard-tree.glb",
                "-ot", "i3dm",
                "-c", "5186"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void instanced03() {
        String path = "I03-seoul-yeouido-gpkg";
        String[] args = new String[]{
                "-tilesVersion", "1.0",
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "-it", "gpkg",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", MagoTestConfig.getInputPath("sample-tree").getAbsolutePath() + "/billboard-tree.glb",
                //"-debug"
        };
        MagoTestConfig.execute(args);
    }

    @Disabled
    @Test
    void instanced04() {
        String path = "I04-forest-shp";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5179",
                "-ot", "i3dm",
                "-instance", MagoTestConfig.getInputPath("sample-tree").getAbsolutePath() + "/billboard-tree.glb",
                //"-debug"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void instanced06A() {
        String path = "I04-forest-shp";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath("ESD/" + path).getAbsolutePath() + "-A",
                "-c", "5179",
                "-it", "gpkg",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", MagoTestConfig.getInputPath("sample-tree").getAbsolutePath() + "/broad-tree-1m.glb",
                "-terrain", MagoTestConfig.getSsdInputPath("dem05-cog.tif").getAbsolutePath(),
                "-attributeFilter", "FRTP_NM=활엽수림",
                //"-debug"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void instanced06B() {
        String path = "I04-forest-shp";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath("ESD/" + path).getAbsolutePath() + "-B",
                "-c", "5179",
                "-it", "gpkg",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", MagoTestConfig.getInputPath("sample-tree").getAbsolutePath() + "/needle-tree-1m.glb",
                "-terrain", MagoTestConfig.getSsdInputPath("dem05-cog.tif").getAbsolutePath(),
                "-attributeFilter", "FRTP_NM=침엽수림",
                //"-debug"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void instanced06C() {
        String path = "I04-forest-shp";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath("ESD/" + path).getAbsolutePath() + "-C",
                "-c", "5179",
                "-it", "gpkg",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", MagoTestConfig.getInputPath("sample-tree").getAbsolutePath() + "/mix-tree-1m.glb",
                "-terrain", MagoTestConfig.getSsdInputPath("dem05-cog.tif").getAbsolutePath(),
                "-attributeFilter", "FRTP_NM=혼효림",
                //"-debug"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void instanced06D() {
        String path = "I04-forest-shp";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath("ESD/" + path).getAbsolutePath() + "-D",
                "-c", "5179",
                "-it", "gpkg",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", MagoTestConfig.getInputPath("sample-tree").getAbsolutePath() + "/bamboo-tree-1m.glb",
                "-terrain", MagoTestConfig.getSsdInputPath("dem05-cog.tif").getAbsolutePath(),
                "-attributeFilter", "FRTP_NM=죽림",
                //"-debug"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void instanced06Merger() {
        String path = "ESD";
        String[] args = new String[]{
                "-i", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-merge",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void instanced09C() {
        String path = "I09-forest-map-big";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath("ESD/" + path).getAbsolutePath() + "-C",
                "-c", "5179",
                "-ot", "forest",
                //"-refineAdd",
                "-instance", MagoTestConfig.getInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getSsdInputPath("dem05-cog.tif").getAbsolutePath(),
                "-attributeFilter", "FRTP_NM=침엽수림",
        };
        MagoTestConfig.execute(args);
    }
}
