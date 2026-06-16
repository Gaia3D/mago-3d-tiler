package com.gaia3d.local.env;

import com.gaia3d.command.LoggingConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Tag("manual")
class JarBuildTest {
    private static final String MODULE_NAME = "mago-tiler";

    static {
        LoggingConfiguration.initConsoleLogger();
    }

    @Test
    void runWithSimple() throws IOException {
        buildJarIfNotExists();

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
        buildJarIfNotExists();

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

    private void buildJarIfNotExists() throws IOException {
        if (!checkJarExists()) {
            log.info("Jar file not found in dist directory. Building shadow jar...");
            buildShadowJar();
            if (!checkJarExists()) {
                throw new IllegalStateException("Shadow jar build completed, but no jar was created in dist directory.");
            }
        } else {
            log.info("Jar file already exists in dist directory. Skipping build.");
        }
    }

    private boolean checkJarExists() {
        File jarPath = getDistDirectory();
        File[] files = jarPath.listFiles((dir, name) -> name.endsWith(".jar"));
        return files != null && files.length > 0;
    }

    private void buildShadowJar() throws IOException {
        List<String> argList = new ArrayList<>();
        argList.addAll(getGradleWrapperCommand());
        argList.add(":" + MODULE_NAME + ":shadowJar");
        runCommand(argList);
    }

    private List<String> getGradleWrapperCommand() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        List<String> command = new ArrayList<>();
        if (osName.contains("windows")) {
            command.add("cmd.exe");
            command.add("/c");
            command.add("gradlew.bat");
        } else {
            command.add("./gradlew");
        }
        return command;
    }

    private String getJarPathFromDist() {
        File jarPath = getDistDirectory();
        File[] files = jarPath.listFiles((dir, name) -> name.endsWith(".jar"));
        File jarFile = null;
        if (files != null && files.length > 0) {
            jarFile = files[0];
        }
        if (jarFile == null) {
            log.error("Jar file not found in dist directory: {}", jarPath.getAbsolutePath());
            throw new RuntimeException("Jar file not found");
        }
        return jarFile.getAbsolutePath();
    }

    private void runCommand(List<String> argList) throws IOException {
        String[] args = argList.toArray(new String[0]);

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.directory(getRepositoryRootDirectory());
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
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("Command failed with exit code " + exitCode + ": " + command);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Command execution interrupted: " + command, e);
        }
        log.info("***Command executed successfully***");
    }

    private File getDistDirectory() {
        return new File(getModuleDirectory(), "dist");
    }

    private File getModuleDirectory() {
        return new File(getRepositoryRootDirectory(), MODULE_NAME);
    }

    private File getRepositoryRootDirectory() {
        File current = new File(System.getProperty("user.dir")).getAbsoluteFile();
        while (current != null) {
            File settingsGradle = new File(current, "settings.gradle");
            File rootWrapperJar = new File(current, "gradle/wrapper/gradle-wrapper.jar");
            if (settingsGradle.isFile() && rootWrapperJar.isFile()) {
                return current;
            }
            current = current.getParentFile();
        }
        throw new IllegalStateException("Could not locate repository root containing settings.gradle and gradle wrapper.");
    }
}
