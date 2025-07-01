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

@Tag("default")
@Slf4j
class DockerBuildTest {

    static {
        Configuration.initConsoleLogger();
    }

    @Test
    void pull() throws IOException {
        String dockerImage = "gaia3d/mago-3d-tiler:latest";
        List<String> argList = new ArrayList<>();
        argList.add("docker");
        argList.add("image");
        argList.add("pull");
        argList.add(dockerImage);
        runCommand(argList);
    }

    @Test
    void inspect() throws IOException {
        String dockerImage = "gaia3d/mago-3d-tiler:latest";
        List<String> argList = new ArrayList<>();
        argList.add("docker");
        argList.add("image");
        argList.add("inspect");
        argList.add(dockerImage);
        runCommand(argList);
    }

    @Test
    void runWithSimple() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();

        File input = new File(classLoader.getResource("./sample-3ds").getFile());
        File resource = new File(input.getParent());

        // docker run --rm -v "/workspace:/workspace" gaia3d/mago-3d-tiler -it 3ds -i /workspace/3ds-samples -o /workspace/sample-3d-tiles -crs 5186

        String dockerImage = "gaia3d/mago-3d-tiler:latest";
        List<String> argList = new ArrayList<>();
        argList.add("docker");
        argList.add("run");
        argList.add("--rm");
        argList.add("-v");
        argList.add(resource.getAbsolutePath() + ":/workspace");
        argList.add(dockerImage);
        argList.add("--input");
        argList.add("/workspace/sample-3ds");
        argList.add("--output");
        argList.add("/workspace/sample-output");
        runCommand(argList);
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