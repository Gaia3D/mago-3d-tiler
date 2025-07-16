package com.gaia3d.release.small;

import com.gaia3d.command.mago.Mago3DTilerMain;

import java.io.File;

public class ReleaseTestConfig {
    public static final String OUTPUT_PATH = "E:/data/mago-server/output";
    public static final String INPUT_PATH = "D:/data/mago-3d-tiler/release-sample";
    public static final String TEMP_PATH = "D:/data/mago-3d-tiler/temp-sample";

    public static void execute(String[] args) {
        Mago3DTilerMain.main(args);
    }
    public  static File getTempPath(String path) {
        return new File(ReleaseTestConfig.TEMP_PATH, path);
    }
    public static File getInputPath(String path) {
        return new File(ReleaseTestConfig.INPUT_PATH, path);
    }
    public static File getOutputPath(String path) {
        return new File(ReleaseTestConfig.OUTPUT_PATH, path);
    }
    public static File getLogPath(String path) {
        File logPath = new File(ReleaseTestConfig.OUTPUT_PATH, path);
        return new File(logPath, "mago-3d-tiler.log");
    }
}
