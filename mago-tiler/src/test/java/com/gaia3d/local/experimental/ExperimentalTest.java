package com.gaia3d.local.experimental;

import com.gaia3d.local.MagoTestConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("experimental")
public class ExperimentalTest {

    @Test
    void pointcloudIntersection00() {
        String path = "geostory-intersection-las";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "32652",
                "-r",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void multiPolygonZGeoJSONSmall() {
        String path = "multi-polygon-z-geojson-small";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                //"--quantize"
                //"-rotateXAxis", "90",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void multiPolygonZGeoJSON() {
        String path = "multi-polygon-z-geojson";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "--quantize"
                //"-rotateXAxis", "90",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void multiPolygonZShape() {
        String path = "multi-polygon-z-shape";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                //"-rotateXAxis", "90",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void citygmlSNBJ20250908() {
        String path = "20250908-SNBJ-GML";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                //"-c", "5186",
                //"-rotateXAxis", "90",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void ldIfc24Extract20250827() {
        String path = "20250827-ld-IFC24-extract";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "-rotateXAxis", "90",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void ldIfc4Extract20250827() {
        String path = "20250827-ld-IFC4-extract";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "-rotateXAxis", "90",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void isosurface00() {
        String path = "iso-surface-0";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "4978",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void isosurface01() {
        String path = "iso-surface-1";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "4978",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void isosurface02() {
        String path = "iso-surface-2";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "4978",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void isosurface03() {
        String path = "iso-surface-3";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "4978",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void dracoSamples() {
        String path = "draco-samples";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void flughafen() {
        String path = "LoD2-Flughafen";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "25832",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void flughafen4978() {
        String path = "LoD2-Flughafen";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-4978",
                "-c", "4978",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloudLD20250807() {
        String path = "ld-20250807";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5179",
                "-r",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloudLD20250808() {
        String path = "ld-20250808";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "32652",
                "-it", "las",
                //"-pcr", "50",
                "-zOffset", "-20.0",
                "-r",
                "--tilesVersion", "1.0",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void sunEnergyIncheon() {
        String path = "sun-energy-incheon-output";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-splitByNode",
                "-it", "glb",
                "-refineAdd",
                "-c", "5186",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void sunEnergy25cm() {
        String path = "sun-energy-25cm-output";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-splitByNode",
                "-it", "glb",
                "-refineAdd",
                "-c", "5179",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void incheon() {
        String path = "incheon";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-splitByNode",
                "-refineAdd",
                "-c", "5186"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void lottemartV3() {
        String path = "lottemartV3.gltf";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-splitByNode",
                "-refineAdd",
                "-c", "5186"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void incheon_mini() {
        String path = "incheon_mini";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-splitByNode",
                "-refineAdd",
                "-c", "5186"
        };
        MagoTestConfig.execute(args);
    }

    //energy_manual

    @Test
    void energyManual() {
        String path = "energy_manual";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                //"-c", "5186"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void d6_4326() {
        String path = "d6_4326.geojson";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-debug",
                "-c", "4326"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void d6_4326_3D() {
        String path = "d6_4326_3D.geojson";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-debug",
                "-c", "4326",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void ecefRedCube() {
        String path = "ecef_red_cube.glb";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "4978",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void ecefVoxel888() {
        String path = "ecef_voxel_8x8x8.glb";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "4978",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void ecefVoxel888V2() {
        String path = "ecef_voxel_8x8x8_simple.glb";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "4978",
        };
        MagoTestConfig.execute(args);
    }

    // EPSG:4978 is a common 3D coordinate system (cartesian coordinate system)
    // ECEF(Earth-Centered Earth-Fixed)
    @Disabled
    @Test
    void batched100() {
        String path = "B100-cartesian-sample";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-crs", "4978",
                //"-rotateXAxis", "90",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void instance10() {
        String path = "I10-forest-purdue";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-ot", "i3dm",
                "--refineAdd",
                "--scaleColumn", "rel_height",
                "--instance", "D:\\data\\mago-3d-tiler\\release-sample\\sample-tree\\broad-tree-1m.glb",
                "-terrain", MagoTestConfig.getTerrainPath("hamilton_dem_navd88_meters_4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void instance10A() {
        String path = "I10-forest-purdue-original-geojson";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-crs", "4326",
                "-ot", "i3dm",
                "--refineAdd",
                "--scaleColumn", "rel_height",
                "--instance", "D:\\data\\mago-3d-tiler\\release-sample\\sample-tree\\broad-tree-1m.glb",
                "-terrain", MagoTestConfig.getTerrainPath("hamilton_dem_navd88_meters_4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void instance10B() {
        String path = "I10-forest-purdue-original-gpkg";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-crs", "4326",
                "-ot", "i3dm",
                "--refineAdd",
                "--scaleColumn", "rel_height",
                "--instance", "D:\\data\\mago-3d-tiler\\release-sample\\sample-tree\\broad-tree-1m.glb",
                "-terrain", MagoTestConfig.getTerrainPath("hamilton_dem_navd88_meters_4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void instance10C() {
        String path = "I10-forest-purdue-original-gpkg2";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-crs", "4326",
                "-it", "gpkg",
                "-ot", "i3dm",
                "--refineAdd",
                "--scaleColumn", "rel_height_m",
                "--instance", "D:\\data\\mago-3d-tiler\\release-sample\\sample-tree\\broad-tree-1m.glb",
                "-terrain", MagoTestConfig.getTerrainPath("hamilton_dem_navd88_meters_4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void instance10D() {
        String path = "I10-forest-purdue-original-gpkg3";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-crs", "4326",
                "-it", "gpkg",
                "-ot", "i3dm",
                "--refineAdd",
                "--scaleColumn", "rel_height_m",
                "--instance", "D:\\data\\mago-3d-tiler\\release-sample\\sample-tree\\broad-tree-1m.glb",
                "-terrain", MagoTestConfig.getTerrainPath("hamilton_dem_navd88_meters_4326.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void instance10E() {
        String path = "I10-forest-purdue-original-gpkg4";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-crs", "4326",
                "-it", "gpkg",
                "-ot", "i3dm",
                "--refineAdd",
                "--scaleColumn", "rel_height_m",
                "--instance", "D:\\data\\mago-3d-tiler\\release-sample\\sample-tree\\broad-tree-1m.glb",
                "-terrain", MagoTestConfig.getInputPath(path).getAbsolutePath() + "/hamilton_dem_navd88_meters_4326.tif",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void instanced08() {
        String path = "I08-transmission-geojson";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5179",
                "-it", "geojson",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", MagoTestConfig.getInputPath(path).getAbsolutePath() + "/lite.glb",
                "-terrain", MagoTestConfig.getSsdInputPath("dem05-cog.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void instanced09() {
        String path = "I09-transmission-line-geojson";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5179",
                "-it", "geojson",
                "-terrain", MagoTestConfig.getSsdInputPath("dem05-cog.tif").getAbsolutePath(),
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void PNT_CL2_LINZ() {
        String path = "CL2_BA32_2024_1000_3805";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "2193",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void deck4251118() {
        String path = "deck4_251118";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-tilesVersion", "1.0",
                "-crs", "5186"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void deck1251118() {
        String path = "deck1_251118";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-tilesVersion", "1.0",
                "-crs", "5186"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void tuvFnft2019() {
        String path = "TuvFnft2019";
        String[] args = new String[]{
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                //"-c", "32760",
        };
        MagoTestConfig.execute(args);
    }
}
