package com.gaia3d.visual.experimental;

import com.gaia3d.visual.MagoTestConfig;
import org.junit.jupiter.api.Test;

public class GeoDataCubeExperimentalTest {

    void testAutoVoxels(String path, String outputPath, int maxLevel) {
        String prefix = "L";
        for (int level = 0; level <= maxLevel; level++) {
            String levelPath = prefix + level;
            String fullPath = path + "/" + levelPath;
            String outputFullPath = outputPath + "/" + levelPath;
            String[] args = {
                    "-i", MagoTestConfig.getTempPath(fullPath).getAbsolutePath(),
                    "-o", MagoTestConfig.getOutputPath(outputFullPath).getAbsolutePath(),
                    "-refineAdd",
                    "-r"
            };
            MagoTestConfig.execute(args);
        }
    }

    @Test
    void globalVoxels() {
        String path = "global-dem-voxels";
        testAutoVoxels(path, path + "-all-level", 3);
    }

    //voxel-korea-all

    @Test
    void koreaAllVoxels() {
        String path = "voxel-korea-all";
        testAutoVoxels(path, path + "-all-level", 8);
    }

    @Test
    void koreaVoxels() {
        String path = "korea-dem-voxels";
        testAutoVoxels(path, path + "-all-level", 8);
    }

    @Test
    void yeongdeungpoVoxels() {
        String path = "yeongdeungpo-voxels";
        testAutoVoxels(path, path + "-all-level", 5);
    }

    @Test
    void ws2FullsetAllLevel() {
        String path = "ws2-voxel-fullset";
        testAutoVoxels(path, path + "-all-level", 14);
    }

    @Test
    void pointcloudTemple() {
        String path = "pointcloud-temple";
        testAutoVoxels(path, path + "-all-level", 15);
    }

    @Test
    void ws2VoxelFullset10() {
        String path = "ws2-voxel-fullset";
        testAutoVoxels(path, path + "-all-level", 10);
    }

    @Test
    void ws2VoxelTerrain() {
        String path = "ws2-voxel-terrain";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                //"-rotateXAxis", "90",
                "-refineAdd",
                "-r"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void ws2VoxelBuildings() {
        String path = "ws2-voxel-buildings";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                //"-rotateXAxis", "90",
                "-refineAdd",
                "-r"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void ws2VoxelFullset() {
        String path = "ws2-voxel-fullset";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                //"-rotateXAxis", "90",
                "-refineAdd",
                "-r"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void voxelMini() {
        String path = "voxel-mini";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                "-r"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void optimizedVoxel15() {
        String path = "ydp-voxel-optimized";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                "-r"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void optimizedVoxel14() {
        String path = "ydp-voxel-optimized-14";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                "-r"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void optimizedVoxel13() {
        String path = "ydp-voxel-optimized-13";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                "-r"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void optimizedVoxel12() {
        String path = "ydp-voxel-optimized-12";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                "-r"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void optimizedVoxel11() {
        String path = "ydp-voxel-optimized-11";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                "-r"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void optimizedVoxel10() {
        String path = "ydp-voxel-optimized-10";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                "-r"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void optimizedVoxel9() {
        String path = "ydp-voxel-optimized-9";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                "-r"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void optimizedVoxel8() {
        String path = "ydp-voxel-optimized-8";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                "-r"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void voxel15() {
        String path = "ydp-voxel";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                "-r"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void voxelTerrainSample() {
        String path = "voxel-terrain-sample";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                "-r"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void voxelTerrainSampleSingle() {
        String path = "voxel-terrain-sample-single";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                "-r"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void voxelBuildingsSample() {
        String path = "voxel-buildings-sample";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                "-r"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void voxelBuildingsSingle() {
        String path = "voxel-buildings-sample-single";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                "-r"
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void voxelSampleAll() {
        String path = "voxel-buildings-sample-all";
        String[] args = new String[] {
                "-i", MagoTestConfig.getTempPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-rotateXAxis", "90",
                "-refineAdd",
                "-r"
        };
        MagoTestConfig.execute(args);
    }
}
