package com.gaia3d.release.big;

import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
class PointCloudBuildTest {
    private static final String INPUT_PATH = "D:/data/mago-3d-tiler/build-sample/";
    private static final String OUTPUT_PATH = "E:/data/mago-server/output";

    @Test
    void buildPointCloud00() {
        String name = "BP00-sangji-university";
        File inputPath = getInputPath(name);
        File outputPath = getOutputPath(name);
        try {
            String[] args = new String[]{
                    "-i", inputPath.getAbsolutePath(),
                    "-o", outputPath.getAbsolutePath(),
                    "-c", "5186",
                    "-pcr", "100",
                    "-force4ByteRGB",
            };
            execute(args);
        } catch (Exception e) {
            log.error("[ERROR] : e");
        }
    }

    @Test
    void buildPointCloud01() {
        String name = "BP01-honam-expressway";
        File inputPath = getInputPath(name);
        File outputPath = getOutputPath(name);
        try {
            String[] args = new String[]{
                    "-i", inputPath.getAbsolutePath(),
                    "-o", outputPath.getAbsolutePath(),
                    "-pcr", "100",
                    "-c", "5186",
                    //"-proj", "+proj=utm +zone=52 +ellps=WGS84 +datum=WGS84 +units=m +no_defs",
            };
            execute(args);
        } catch (Exception e) {
            log.error("[ERROR] : e");
        }

    }

    @Test
    void buildPointCloud02() {
        String name = "BP02-dense-lh";
        File inputPath = getInputPath(name);
        File outputPath = getOutputPath(name);
        try {
            String[] args = new String[]{
                    "-i", inputPath.getAbsolutePath(),
                    "-o", outputPath.getAbsolutePath(),
                    "-pcr", "100",
                    "-c", "4326",
                    //"-proj", "+proj=utm +zone=52 +ellps=WGS84 +datum=WGS84 +units=m +no_defs",
            };
            execute(args);
        } catch (Exception e) {
            log.error("[ERROR] : e");
        }

    }

    @Test
    void buildPointCloud03() {
        String name = "BP03-anyang";
        File inputPath = getInputPath(name);
        File outputPath = getOutputPath(name);
        try {
            String[] args = new String[]{
                    "-i", inputPath.getAbsolutePath(),
                    "-o", outputPath.getAbsolutePath(),
                    "-pcr", "100",
                    "-c", "4326",
            };
            execute(args);
        } catch (Exception e) {
            log.error("[ERROR] : e");
        }

    }

    @Test
    void buildPointCloud04() {
        String name = "BP04-time-gate";
        File inputPath = getInputPath(name);
        File outputPath = getOutputPath(name);
        try {
            String[] args = new String[]{
                    "-i", inputPath.getAbsolutePath(),
                    "-o", outputPath.getAbsolutePath(),
                    "-pcr", "100",
                    "-proj", "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=500000 +datum=WGS84 +units=m +no_defs",
            };
            execute(args);
        } catch (Exception e) {
            log.error("[ERROR] : e");
        }
    }

    @Test
    void buildPointCloud05() {
        String name = "BP05-bogota";
        File inputPath = getInputPath(name);
        File outputPath = getOutputPath(name);
        try {
            String[] args = new String[]{
                    "-i", inputPath.getAbsolutePath(),
                    "-o", outputPath.getAbsolutePath(),
                    //"-pcr", "50",
                    "-crs", "32618",
            };
            execute(args);
        } catch (Exception e) {
            log.error("[ERROR] : e");
        }
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

    private File getLogPath(String path) {
        File logPath = new File(OUTPUT_PATH, path);
        return new File(logPath, "log.txt");
    }
}
