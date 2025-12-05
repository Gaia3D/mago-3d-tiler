package com.gaia3d.local.release;

import com.gaia3d.command.LoggingConfiguration;
import com.gaia3d.local.MagoTestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Tag("release")
@Slf4j
class DefaultReleaseTest {

    @Test
    void batched01() {
        String path = "B01-wangsuk2-3ds";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-c", "5186",
                "--quantize",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void realistic00() {
        String path = "R00-bansong-obj";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-log", MagoTestConfig.getLogPath(path).getAbsolutePath(),
                "-it", "obj",
                "-pg",
                "-c", "5187",
                "-rotateX", "90",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void pointcloud00V2() {
        String path = "P00-hwangyonggak-las";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-V2",
                "-tilesVersion", "1.1",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void instanced06A() {
        String path = "I04-forest-shp";
        String[] args = new String[]{
                "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath() + "-A",
                "-c", "5179",
                "-it", "gpkg",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", MagoTestConfig.getInputPath("sample-tree").getAbsolutePath() + "/broad-tree-1m.glb",
                "-attributeFilter", "FRTP_NM=활엽수림",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void runWithSimple() throws IOException {
        /*
        "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", MagoTestConfig.getOutputPath(path).getAbsolutePath(),
                "-terrain", MagoTestConfig.getInputPath(path).getAbsolutePath() + "/seoul.tif",
                "-c", "5186"
         */

        String path = "B06-seoul-yeouido-shp";
        List<String> argList = new ArrayList<>();
        argList.add("java");
        argList.add("-jar");
        argList.add(getJarPathFromDist());
        argList.add("--input");
        argList.add(MagoTestConfig.getInputPath(path).getAbsolutePath());
        argList.add("--output");
        argList.add(MagoTestConfig.getOutputPath(path).getAbsolutePath());
        argList.add("--terrain");
        argList.add(MagoTestConfig.getInputPath(path).getAbsolutePath() + "/seoul.tif");
        argList.add("-c");
        argList.add("5186");
        runCommand(argList);
    }

    static {
        LoggingConfiguration.initConsoleLogger();
    }

    private String getJarPathFromDist() {
        String projectDistPath = System.getProperty("user.dir");
        File jarPath = new File(projectDistPath, "dist");
        File[] files = jarPath.listFiles((dir, name) -> name.endsWith(".jar"));
        File jarFile = null;
        if (files != null && files.length > 0) {
            jarFile = files[0];
        }
        if (jarFile == null) {
            log.error("Jar file not found in dist directory : {}", jarFile.getAbsolutePath());
            throw new RuntimeException("Jar file not found");
        }
        return jarFile.getAbsolutePath();
    }

    private void runCommand(List<String> argList) throws IOException {
        String[] args = argList.toArray(new String[0]);

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.redirectErrorStream(true);
        StringBuilder stringBuilder = new StringBuilder();
        for (String arg : args) {
            stringBuilder.append(arg).append(" ");
        }
        String command = stringBuilder.toString();
        Process process = processBuilder.start();
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        log.info("Executing command: {}", command);
        log.info("***Starting command execution***");
        for (String str; (str = inputReader.readLine()) != null; ) {
            log.info(str);
        }
        for (String str; (str = errorReader.readLine()) != null; ) {
            log.error(str);
        }
        log.info("***Command executed successfully***");
    }
}
