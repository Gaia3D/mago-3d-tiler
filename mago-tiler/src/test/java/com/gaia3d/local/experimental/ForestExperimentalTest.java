package com.gaia3d.local.experimental;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.local.MagoTestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("experimental")
@Slf4j
public class ForestExperimentalTest {

    @Test
    void koreaForestService() {
        String path = "korea-forest-service";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5179",
                "-ot", "i3dm",
                "-instance", MagoTestConfig.getTempPath(path).getAbsolutePath() + "/instance.glb",
                "-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void forest5000haReplaceWithAdd() {
        String path = "forest-5000ha-300ha";
        String[] args = new String[]{
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-ReplaceWithAdd",
                "-c", "5186",
                "-ot", "i3dm",
                "-it", "gpkg",
                "--refineAdd",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                "-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void forest5000haReplace() {
        String path = "forest-5000ha-300ha";
        String[] args = new String[]{
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-replace",
                "-c", "5186",
                "-ot", "i3dm",
                "-it", "gpkg",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void forest5000haAdd() {
        String path = "forest-5000ha-300ha";
        String[] args = new String[]{
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-add",
                "-c", "5186",
                "-ot", "i3dm",
                "-it", "gpkg",
                "--refineAdd",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }


    @Test
    void seoraksan300ha() {
        String path = "seoraksan-300ha";
        String[] args = new String[]{
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "-ot", "i3dm",
                "-it", "gpkg",
                //"--refineAdd",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void seoraksan600ha() {
        String path = "seoraksan-600ha";
        String[] args = new String[]{
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "-ot", "i3dm",
                "-it", "gpkg",
                "--refineAdd",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void seoraksan1200ha() {
        String path = "seoraksan-1200ha";
        String[] args = new String[]{
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "-ot", "i3dm",
                "-it", "gpkg",
                "--refineAdd",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }


    /*
        Forest-DT-2500ha test cases
     */
    @Test
    void forestDT2026Add() {
        String path = "Forest-DT-2500ha";
        String[] args = new String[]{
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-Added",
                "-c", "5186",
                "-ot", "i3dm",
                "-it", "gpkg",
                "--refineAdd",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void forestDT2026Replace() {
        String path = "Forest-DT-2500ha";
        String[] args = new String[]{
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-Replaced",
                "-c", "5186",
                "-ot", "i3dm",
                "-it", "gpkg",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void forestDT2026Mixed() {
        String path = "Forest-DT-2500ha";
        String[] args = new String[]{
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-Mixed",
                "-c", "5186",
                "-ot", "forest",
                "-it", "gpkg",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void forestDT625Add() {
        String path = "Forest-DT-625ha";
        String[] args = new String[]{
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-Added",
                "-c", "5186",
                "-ot", "i3dm",
                "-it", "gpkg",
                "--refineAdd",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void forestDT625Replace() {
        String path = "Forest-DT-625ha";
        String[] args = new String[]{
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-Replaced",
                "-c", "5186",
                "-ot", "i3dm",
                "-it", "gpkg",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void forestDT625Mixed() {
        String path = "Forest-DT-625ha";
        String[] args = new String[]{
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-Mixed",
                "-c", "5186",
                "-ot", "forest",
                "-it", "gpkg",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }


    @Test
    void forestDT250Add() {
        String path = "Forest-DT-250ha";
        String[] args = new String[]{
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-Added",
                "-c", "5186",
                "-ot", "i3dm",
                "-it", "gpkg",
                "--refineAdd",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void forestDT250Replace() {
        String path = "Forest-DT-250ha";
        String[] args = new String[]{
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-Replaced",
                "-c", "5186",
                "-ot", "i3dm",
                "-it", "gpkg",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void forestDT250Mixed() {
        String path = "Forest-DT-250ha";
        String[] args = new String[]{
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-Mixed",
                "-c", "5186",
                "-ot", "forest",
                "-it", "gpkg",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath(path).getAbsolutePath() + "/instance.glb",
                //"-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void garisan() {
        GlobalOptions.recreateInstance();
        garisanChim();
        GlobalOptions.recreateInstance();
        garisanHwal();
        GlobalOptions.recreateInstance();
        garisanJat();
        GlobalOptions.recreateInstance();
        garisanNak();

        garisanMerge();
    }

    @Test
    void garisanMerge() {
        String path = "garisan";
        String[] args = new String[]{
                "-i", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-recursive",
                "-merge",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void garisanChim() {
        String path = "garisan/Chim";
        String[] args = new String[]{
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5187",
                "-ot", "i3dm",
                "--refineAdd",
                //"-ot", "forest",
                "--scaleColumn", "TreeHeight",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath("garisan").getAbsolutePath() + "/chim.glb",
                "-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
                "-maxLod", "5",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void garisanHwal() {
        String path = "garisan/Hwal";
        String[] args = new String[]{
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5187",
                "-ot", "i3dm",
                "--refineAdd",
                //"-ot", "forest",
                "--scaleColumn", "TreeHeight",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath("garisan").getAbsolutePath() + "/hwal.glb",
                "-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
                "-maxLod", "5",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void garisanJat() {
        String path = "garisan/Jat";
        String[] args = new String[]{
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5187",
                "-ot", "i3dm",
                "--refineAdd",
                //"-ot", "forest",
                "--scaleColumn", "TreeHeight",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath("garisan").getAbsolutePath() + "/jat.glb",
                "-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
                "-maxLod", "5",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void garisanNak() {
        String path = "garisan/Nak";
        String[] args = new String[]{
                "-i", MagoTestConfig.getSsdInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5187",
                "-ot", "i3dm",
                "--refineAdd",
                //"-ot", "forest",
                "--scaleColumn", "TreeHeight",
                "--tilesVersion", "1.0",
                "-instance", MagoTestConfig.getSsdInputPath("garisan").getAbsolutePath() + "/nak.glb",
                "-terrain", MagoTestConfig.getTerrainPath("korea-05-cog-dem-4326.tif").getAbsolutePath(),
                "-maxLod", "5",
        };
        MagoTestConfig.execute(args);
    }
}
