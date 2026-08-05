package com.gaia3d.local;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;

import java.io.File;
import java.util.Arrays;

import static com.gaia3d.util.FileUtils.deleteDirectory;

@Slf4j
@Tag("manual")
public class MagoTestConfig {
    public static final String OUTPUT_PATH = "H:/workspace/mago-server/output";
    public static final String SSD_INPUT_PATH = "H:/workspace/mago-3d-tiler";
    public static final String INPUT_PATH = "D:/data/mago-3d-tiler/release-sample";
    public static final String TEMP_PATH = "D:/data/mago-3d-tiler/temp-sample";
    public static final String TERRAIN_PATh = "D:/data/mago-3d-tiler/terrain-sample";

    public static void execute(String[] args) {
        log.info("[Execute] {}", String.join(" ", args));
        GlobalOptions.recreateInstance();
        Mago3DTilerMain.main(args);
    }

    public static void execute(String[] args, boolean isOverwrite) {
        log.info("[Execute]{} {}", isOverwrite ? " [Overwrite]" : "", String.join(" ", args));

        if (isOverwrite) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-o") || args[i].equals("--output") || args[i].equals("-output")) {
                    String outputPath = args[i + 1];
                    File outputDir = new File(outputPath);
                    if (outputDir.exists()) {
                        log.info("Output path exists, deleting: {}", outputPath);
                        deleteDirectory(outputDir);
                    }
                    break;
                }
            }
        }

        GlobalOptions.recreateInstance();
        Mago3DTilerMain.main(args);
    }

    public static File getTempPath(String path) {
        return new File(MagoTestConfig.TEMP_PATH, path);
    }

    public static File getSsdInputPath(String path) {
        return new File(MagoTestConfig.SSD_INPUT_PATH, path);
    }

    public static File getInputPath(String path) {
        return new File(MagoTestConfig.INPUT_PATH, path);
    }

    public static File getTerrainPath(String path) {
        return new File(MagoTestConfig.TERRAIN_PATh, path);
    }

    public static File getOutputPath(String path) {
        return new File(MagoTestConfig.OUTPUT_PATH, path);
    }

    public static File getLogPath(String path) {
        File logPath = new File(MagoTestConfig.OUTPUT_PATH, path);
        return new File(logPath, "mago-3d-tiler.log");
    }
}
