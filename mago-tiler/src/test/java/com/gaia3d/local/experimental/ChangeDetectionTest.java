package com.gaia3d.local.experimental;

import com.gaia3d.local.MagoTestConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;

@Tag("experimental")
public class ChangeDetectionTest {

    @Disabled
    @Test
    void convertChangeDetection251106posTest() {
        String name = "dunpo-change-detection-251106";
        String path = "D:\\data\\mago-3d-tiler\\temp-sample\\아산시 변화탐지\\2.둔포면(폐기물_변화탐지)\\251106\\3d_mesh";
        String[] args = new String[]{
                "-i", path,
                "-o", MagoTestConfig.getOutputPath(name).getAbsolutePath() + "-pos_test",
                //"-crs", "5186",
                "-lon", Double.toString(127.00853617d - 0.0018446d -0.00001167d),
                "-lat", Double.toString(36.91106941d - 0.00113682d -0.00000126d),
                "-zOffset", "119.757",
                "-rotateXAxis", "90",
                "-pg",
        };
        MagoTestConfig.execute(args);
    }

    @Disabled
    @Test
    void convertChangeDetection251209posTest() {
        String name = "dunpo-change-detection-251209";
        String path = "D:\\data\\mago-3d-tiler\\temp-sample\\아산시 변화탐지\\2.둔포면(폐기물_변화탐지)\\251209\\3d_mesh";
        String[] args = new String[]{
                "-i", path,
                "-o", MagoTestConfig.getOutputPath(name).getAbsolutePath() + "-pos_test",
                //"-crs", "5186",
                "-lon", Double.toString(127.00853617d - 0.0018446d),
                "-lat", Double.toString(36.91106941d - 0.00113682d),
                "-zOffset", Double.toString(119.757d),
                "-rotateXAxis", "90",
                "-pg",
        };
        MagoTestConfig.execute(args);
    }

    @Disabled
    @Test
    void convertChangeDetection260209posTest() {
        String name = "dunpo-change-detection-260209";
        String path = "D:\\data\\mago-3d-tiler\\temp-sample\\아산시 변화탐지\\2.둔포면(폐기물_변화탐지)\\260209\\3d_mesh";
        String[] args = new String[]{
                "-i", path,
                "-o", MagoTestConfig.getOutputPath(name).getAbsolutePath() + "-pos_test",
                //"-crs", "5186",
                "-lon", Double.toString(127.00853617d - 0.0018446d),
                "-lat", Double.toString(36.91106941d - 0.00113682d),
                "-zOffset", Double.toString(119.757d),
                "-rotateXAxis", "90",
                "-pg",
        };
        MagoTestConfig.execute(args);
    }

    @Disabled
    @Test
    void convertChangeDetection250415posTest() {
        String name = "eumbong-change-detection-250415";
        String path = "D:\\data\\mago-3d-tiler\\temp-sample\\아산시 변화탐지\\3.음봉면(폐기물_변화탐지)\\25년4월\\3d_mesh";
        String[] args = new String[]{
                "-i", path,
                "-o", MagoTestConfig.getOutputPath(name).getAbsolutePath() + "-pos_test",
                "-lon", Double.toString(127.086188d - 0.00018912d),
                "-lat", Double.toString(36.883866d - 0.00055463),
                "-zOffset", Double.toString(79.493 + 114.760),
                "-minLod", "0",
                "-maxLod", "0",
        };
        MagoTestConfig.execute(args);
    }

    /*@Disabled
    @Test
    void convertChangeDetection251106() {
        String name = "dunpo-change-detection-251106";
        String path = "D:\\data\\mago-3d-tiler\\temp-sample\\아산시 변화탐지\\2.둔포면(폐기물_변화탐지)\\251106\\3d_mesh";
        String[] args = new String[]{
                "-i", path,
                "-o", MagoTestConfig.getOutputPath(name).getAbsolutePath(),
                "-lon", Double.toString(127.00853617d - 0.0018446d),
                "-lat", Double.toString(36.91106941d - 0.00113682d),
                "-zOffset", "119.757",
                "-rotateXAxis", "90",
                "-pg",
        };
        MagoTestConfig.execute(args);
    }

    @Disabled
    @Test
    void convertChangeDetection251209() {
        String name = "dunpo-change-detection-251209";
        String path = "D:\\data\\mago-3d-tiler\\temp-sample\\아산시 변화탐지\\2.둔포면(폐기물_변화탐지)\\251209\\3d_mesh";
        String[] args = new String[]{
                "-i", path,
                "-o", MagoTestConfig.getOutputPath(name).getAbsolutePath(),
                "-lon", Double.toString(127.00853617d - 0.0018446d),
                "-lat", Double.toString(36.91106941d - 0.00113682d),
                "-zOffset", "119.757",
                "-rotateXAxis", "90",
                "-pg",
        };
        MagoTestConfig.execute(args);
    }

    @Disabled
    @Test
    void convertChangeDetection260209() {
        String name = "dunpo-change-detection-260209";
        String path = "D:\\data\\mago-3d-tiler\\temp-sample\\아산시 변화탐지\\2.둔포면(폐기물_변화탐지)\\260209\\3d_mesh";
        String[] args = new String[]{
                "-i", path,
                "-o", MagoTestConfig.getOutputPath(name).getAbsolutePath(),
                "-lon", Double.toString(127.00853617d - 0.0018446d),
                "-lat", Double.toString(36.91106941d - 0.00113682d),
                "-zOffset", "119.575",
                "-rotateXAxis", "90",
                "-pg",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void convertChangeDetection251106PointCloud() {
        String name = "dunpo-change-detection-251106-point-cloud";
        String path = "D:\\data\\mago-3d-tiler\\temp-sample\\아산시 변화탐지\\2.둔포면(폐기물_변화탐지)\\251106\\point_cloud";
        String[] args = new String[]{
                "-i", path,
                "-o", MagoTestConfig.getOutputPath(name).getAbsolutePath(),
                "-crs", "5186",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void convertChangeDetection251209PointCloud() {
        String name = "dunpo-change-detection-251209-point-cloud";
        String path = "D:\\data\\mago-3d-tiler\\temp-sample\\아산시 변화탐지\\2.둔포면(폐기물_변화탐지)\\251209\\point_cloud";
        String[] args = new String[]{
                "-i", path,
                "-o", MagoTestConfig.getOutputPath(name).getAbsolutePath(),
                "-crs", "5186",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void convertChangeDetection260209PointCloud() {
        String name = "dunpo-change-detection-260209-point-cloud";
        String path = "D:\\data\\mago-3d-tiler\\temp-sample\\아산시 변화탐지\\2.둔포면(폐기물_변화탐지)\\260209\\point_cloud";
        String[] args = new String[]{
                "-i", path,
                "-o", MagoTestConfig.getOutputPath(name).getAbsolutePath(),
                "-crs", "5186",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void convertChangeDetectionEumbongPointCloud() {
        String folder = "D:\\data\\mago-3d-tiler\\temp-sample\\아산시 변화탐지\\3.음봉면(폐기물_변화탐지)";
        File folderFile = new File(folder);
        for (File child : folderFile.listFiles()) {
            File pointcloudFolder = new File(child, "point_cloud");
            if (pointcloudFolder.exists() && pointcloudFolder.isDirectory()) {
                String name = "eumbong-change-detection-" + child.getName() + "-point-cloud";
                String[] args = new String[]{
                        "-i", pointcloudFolder.getAbsolutePath(),
                        "-o", MagoTestConfig.getOutputPath(name).getAbsolutePath(),
                        "-crs", "5186",
                };
                MagoTestConfig.execute(args);
            }
        }
    }*/

    @Test
    void convertChangeDetectionEumbongRealisticMesh() {
        String folder = "D:\\data\\mago-3d-tiler\\temp-sample\\아산시 변화탐지\\3.음봉면(폐기물_변화탐지)";
        File folderFile = new File(folder);
        for (File child : folderFile.listFiles()) {
            File pointcloudFolder = new File(child, "3d_mesh");
            if (pointcloudFolder.exists() && pointcloudFolder.isDirectory()) {
                String name = "eumbong-change-detection-" + child.getName() + "-realistic-mesh";
                String[] args = new String[]{
                        "-i", pointcloudFolder.getAbsolutePath(),
                        "-o", MagoTestConfig.getOutputPath(name).getAbsolutePath(),
                        "-lon", Double.toString(127.086188d - 0.00018912d),
                        "-lat", Double.toString(36.883866d - 0.00055463),
                        "-zOffset", Double.toString(79.493 + 114.760),
                        "-rotateXAxis", "90",
                        "-pg"
                };
                MagoTestConfig.execute(args);
            }
        }
    }

    @Test
    void convertChangeDetectionDunpoPointCloud() {
        String folder = "D:\\data\\mago-3d-tiler\\temp-sample\\아산시 변화탐지\\2.둔포면(폐기물_변화탐지)";
        File folderFile = new File(folder);
        for (File child : folderFile.listFiles()) {
            File pointcloudFolder = new File(child, "point_cloud");
            if (pointcloudFolder.exists() && pointcloudFolder.isDirectory()) {
                String name = "dunpo-change-detection-" + child.getName() + "-point-cloud";
                String[] args = new String[]{
                        "-i", pointcloudFolder.getAbsolutePath(),
                        "-o", MagoTestConfig.getOutputPath(name).getAbsolutePath(),
                        "-crs", "5186",
                };
                MagoTestConfig.execute(args);
            }
        }
    }

    @Test
    void convertChangeDetectionDunpoRealisticMesh() {
        String folder = "D:\\data\\mago-3d-tiler\\temp-sample\\아산시 변화탐지\\3.둔포면(폐기물_변화탐지)";
        File folderFile = new File(folder);
        for (File child : folderFile.listFiles()) {
            File pointcloudFolder = new File(child, "3d_mesh");
            if (pointcloudFolder.exists() && pointcloudFolder.isDirectory()) {
                String name = "dunpo-change-detection-" + child.getName() + "-realistic-mesh";
                String[] args = new String[]{
                        "-i", pointcloudFolder.getAbsolutePath(),
                        "-o", MagoTestConfig.getOutputPath(name).getAbsolutePath(),
                        "-lon", Double.toString(127.00853617d - 0.0018446d),
                        "-lat", Double.toString(36.91106941d - 0.00113682d),
                        "-zOffset", "119.575",
                        "-rotateXAxis", "90",
                        "-pg"
                };
                MagoTestConfig.execute(args);
            }
        }
    }
}
