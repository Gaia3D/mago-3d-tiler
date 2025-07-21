package com.gaia3d.visual;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.command.mago.Mago3DTilerMain;

import java.io.File;

public class MagoTestConfig {
    public static final String OUTPUT_PATH = "E:/data/mago-server/output";
    public static final String INPUT_PATH = "D:/data/mago-3d-tiler/release-sample";
    public static final String TEMP_PATH = "D:/data/mago-3d-tiler/temp-sample";
    public static final String TERRAIN_PATh = "D:/data/mago-3d-tiler/terrain-sample";

    public static void execute(String[] args) {
        GlobalOptions.recreateInstance();
        Mago3DTilerMain.main(args);
    }
    public  static File getTempPath(String path) {
        return new File(MagoTestConfig.TEMP_PATH, path);
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
