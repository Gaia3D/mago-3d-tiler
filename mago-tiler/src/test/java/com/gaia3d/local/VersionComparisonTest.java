package com.gaia3d.local;

import com.gaia3d.command.LoggingConfiguration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Tag("version-comparison")
public class VersionComparisonTest {

    public static final String INPUT_PATH = "D:/data/mago-3d-tiler/release-sample";
    public static final String OUTPUT_PATH = "H:/workspace/mago-server/output";
    private static final String OLD_VERSION = "1.14.0-release";
    private static final String NEW_VERSION = "latest";

    static {
        LoggingConfiguration.initConsoleLogger();
    }

    @Test
    void compareVersions() throws IOException {
        pullImages();

        String path = "P00-hwangyonggak-las";
        executeOldVersion(path, path);
        executeNewVersion(path, path);
        // Add comparison logic here
    }

    @Test
    void newOnly() throws IOException {
        pullImages();
        String path = "P00-hwangyonggak-las";
        executeNewVersion(path, path);
    }

    void executeOldVersion(String inputSubDir, String outputSubDir) throws IOException {
        String dockerImage = "gaia3d/mago-3d-tiler:" + OLD_VERSION;
        List<String> argList = new ArrayList<>();
        argList.add("docker");
        argList.add("run");
        argList.add("--rm");
        argList.add("-v");
        argList.add(new File(INPUT_PATH, inputSubDir).getAbsolutePath() + ":/workspace/input");
        argList.add("-v");
        argList.add(new File(OUTPUT_PATH, outputSubDir + "_" + OLD_VERSION).getAbsolutePath() + ":/workspace/output");
        argList.add(dockerImage);
        argList.add("--input");
        argList.add("/workspace/input");
        argList.add("--output");
        argList.add("/workspace/output");
        argList.add("--temp");
        argList.add("/workspace/temp");
        argList.add("--crs");
        argList.add("32652");
        DockerRun.run(argList);
    }

    void executeNewVersion(String inputSubDir, String outputSubDir) throws IOException {
        String dockerImage = "gaia3d/mago-3d-tiler:" + NEW_VERSION;
        List<String> argList = new ArrayList<>();
        argList.add("docker");
        argList.add("run");
        argList.add("--rm");
        argList.add("-v");
        argList.add(new File(INPUT_PATH, inputSubDir).getAbsolutePath() + ":/workspace/input");
        argList.add("-v");
        argList.add(new File(OUTPUT_PATH, outputSubDir + "_" + NEW_VERSION).getAbsolutePath() + ":/workspace/output");
        argList.add(dockerImage);
        argList.add("--input");
        argList.add("/workspace/input");
        argList.add("--output");
        argList.add("/workspace/output");
        argList.add("--temp");
        argList.add("/workspace/temp");
        argList.add("--crs");
        argList.add("32652");
        DockerRun.run(argList);
    }

    void pullImages() throws IOException {
        pull(OLD_VERSION);
        pull(NEW_VERSION);
    }

    void pull(String version) throws IOException {
        String dockerImage = "gaia3d/mago-3d-tiler:" + version;
        List<String> argList = new ArrayList<>();
        argList.add("docker");
        argList.add("image");
        argList.add("pull");
        argList.add(dockerImage);
        DockerRun.run(argList);
    }

    private static File getInputPath(String path, String suDir) {
        if (suDir != null) {
            return new File(new File(INPUT_PATH, suDir), path);
        }
        return new File(INPUT_PATH, path);
    }

    private static File getOutputPath(String path, String suDir) {
        if (suDir != null) {
            return new File(new File(OUTPUT_PATH, suDir), path);
        }
        return new File(OUTPUT_PATH, path);
    }

}
