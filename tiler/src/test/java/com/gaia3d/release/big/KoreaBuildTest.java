package com.gaia3d.release.big;

import com.gaia3d.command.Configurator;
import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import java.io.File;

@Slf4j
class KoreaBuildTest {
    private static final String INPUT_PATH = "D:/data/mago-3d-tiler/build-sample/";
    private static final String OUTPUT_PATH = "E:/data/mago-server/output";

    @Test
    void danang() {
        String path = "danang-buildings";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-log", getLogPath(path).getAbsolutePath(),
                "-mh", "3.3",
                "-terrain", getInputPath(path).getAbsolutePath() + File.separator + "danang_dem.tif",
                "-c", "4326",
        };
        execute(args);
    }

    @Test
    void ogcSample() {
        String path = "ogc-sample";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-log", getLogPath(path).getAbsolutePath(),
                //"-terrain", getInputPath(path).getAbsolutePath() + File.separator + "merged_dem_5m_trimmed.tif",
                "-c", "5186",
        };
        execute(args);
    }

    @Test
    void openDataKoreaSplitBuild() {
        Configurator.initConsoleLogger();

        int totalCount = 192;
        for (int i = 2; i < totalCount; i++) {
            openDataKoreaBuild(i);
        }
    }

    public void openDataKoreaBuild(int number) {
        File geoTiffFile = new File(INPUT_PATH, "split/korea-compressed.tif");

        File parent = new File(INPUT_PATH, "split");
        if (!parent.exists()) {
            parent.mkdirs();
        }

        String fileName = "girdid_" + number;
        String path = "split/" + fileName;
        String splitPath = "open-data-korea-split/" + fileName;

        File inputPath = getInputPath(path);
        /*if (!inputPath.exists()) {
            inputPath.mkdirs();
        }*/

        File splitInputPath = getInputPath(splitPath);
        if (!splitInputPath.exists()) {
            splitInputPath.mkdirs();
        }

       /* File[] files = parent.listFiles();
        for (File file : files) {
            if (file.getName().contains(fileName + ".") && file.isFile()) {
                try {
                    log.info("copy file: {}", file.getName());
                    FileUtils.copyFileToDirectory(file, splitInputPath);
                } catch (Exception e) {
                    log.error("[ERROR] : e")
                }
            }
        }*/

        if (splitInputPath.listFiles().length == 0) {
            //log.info("No files in {}", inputPath);
            FileUtils.deleteQuietly(splitInputPath);
            return;
        }

        try {
            //log.info("Start build: {}", splitInputPath);
            String[] args = new String[] {
                    "-i", splitInputPath.getAbsolutePath(),
                    "-o", getOutputPath(fileName).getAbsolutePath(),
                    "-terrain", geoTiffFile.getAbsolutePath(),
                    "-c", "5186",
            };
            //execute(args);

            String command = "java -jar mago-3d-tiler-1.0.0.jar " + String.join(" ", args);
            log.info("{}", command);
        } catch (Exception e) {
            log.error("[ERROR] : e");
        }
    }


    @Test
    void openDataForest() {
        Configurator.initConsoleLogger();

        int realCount = 0;
        int totalCount = 15000;
        //int totalCount = 100;
        log.info("[START] total count: {}", totalCount);
        for (int i = 0; i <= totalCount; i++) {
            String command = forest(i);
            if (command != null) {
                log.info("{}", command);
                realCount++;
            }
        }
        log.info("[END] real count: {}", realCount);
    }

    public String forest(int number) {
        File inputPath = new File("E:/workspace/TB_FGDI_FS_IM5000.gdb/FOREST_GRID_SPLIT");
        File geoTiffFile = new File("G:/workspace/dem05.tif");


        String outputPath = "E:/data/mago-server/output/TILESET_PATH";
        String fileName = "grid-id_" + number;

        File output = new File(outputPath);
        if (!output.exists()) {
            output.mkdirs();
        }

        File splitInputPath = new File(inputPath, fileName);
        if (!splitInputPath.exists() || !splitInputPath.isDirectory()) {
            return null;
        }
        File makeDir = new File(inputPath, fileName);
        /*if (!makeDir.exists()) {
            makeDir.mkdirs();
        }

        try {
            FileUtils.moveFile(splitInputPath, new File(makeDir, fileName + ".gpkg"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/

        File outputFolder = new File(outputPath, fileName);

        try {
            String[] args = new String[] {
                    "-i", makeDir.getAbsolutePath(),
                    "-o", outputFolder.getAbsolutePath(),
                    "-terrain", geoTiffFile.getAbsolutePath(),
                    "-instance", "E:\\workspace\\TB_FGDI_FS_IM5000.gdb\\FOREST_GRID_SPLIT\\broad-tree.glb",
                    "-it", "gpkg",
                    "-ot", "i3dm",
                    "-c", "5179",
            };
            return "java -jar \"C:\\Workspace\\git-repositories\\mago-3d-tiler\\tiler\\dist\\mago-3d-tiler-1.11.0-beta-natives-windows.jar\" " + String.join(" ", args);
        } catch (Exception e) {
            log.error("[ERROR] : e");
        }
        return null;
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
