package com.gaia3d.release;

import com.gaia3d.command.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

//@Tag("default")
@Slf4j
class JarBuildTest {

    static {
        Configuration.initConsoleLogger();
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

    @Test
    void runWithSimple() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource("./sample-3ds").getFile());
        File output = new File(classLoader.getResource("./sample-output").getFile());

        // java -jar mago-3d-tiler-x.x.x.jar --input ./sample-3ds --output ./sample-output
        List<String> argList = new ArrayList<>();
        argList.add("java");
        argList.add("-jar");
        argList.add(getJarPathFromDist());
        argList.add("--input");
        argList.add(input.getAbsolutePath());
        argList.add("--output");
        argList.add(output.getAbsolutePath());
        runCommand(argList);
    }

    @Test
    void runWithKml() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource("./sample-kml").getFile());
        File output = new File(classLoader.getResource("./sample-output").getFile());

        // java -jar mago-3d-tiler-x.x.x.jar --input ./sample-kml --output ./sample-output
        List<String> argList = new ArrayList<>();
        argList.add("java");
        argList.add("-jar");
        argList.add(getJarPathFromDist());
        argList.add("--input");
        argList.add(input.getAbsolutePath());
        argList.add("--output");
        argList.add(output.getAbsolutePath());
        runCommand(argList);
    }
}