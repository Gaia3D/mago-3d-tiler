package com.gaia3d.local.env;

import com.gaia3d.command.LoggingConfiguration;
import com.gaia3d.local.DockerRun;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
class DockerBuildTest {

    static {
        LoggingConfiguration.initConsoleLogger();
    }

    @Test
    void pull() throws IOException {
        String dockerImage = "gaia3d/mago-3d-tiler:latest";
        List<String> argList = new ArrayList<>();
        argList.add("docker");
        argList.add("image");
        argList.add("pull");
        argList.add(dockerImage);
        DockerRun.run(argList);
    }

    @Test
    void inspect() throws IOException {
        String dockerImage = "gaia3d/mago-3d-tiler:latest";
        List<String> argList = new ArrayList<>();
        argList.add("docker");
        argList.add("image");
        argList.add("inspect");
        argList.add(dockerImage);
        DockerRun.run(argList);
    }

    @Test
    void runWithSimple() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();

        File input = new File(classLoader.getResource("./sample-3ds").getFile());
        File resource = new File(input.getParent());

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
        DockerRun.run(argList);
    }

    @Test
    void runWithSimpleArm64() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();

        File input = new File(classLoader.getResource("./sample-3ds").getFile());
        File resource = new File(input.getParent());

        String dockerImage = "gaia3d/mago-3d-tiler:latest";
        List<String> argList = new ArrayList<>();
        argList.add("docker");
        argList.add("run");
        argList.add("--platform");
        argList.add("linux/arm64");
        argList.add("--rm");
        argList.add("-v");
        argList.add(resource.getAbsolutePath() + ":/workspace");
        argList.add(dockerImage);
        argList.add("--input");
        argList.add("/workspace/sample-3ds");
        argList.add("--output");
        argList.add("/workspace/sample-output");
        DockerRun.run(argList);
    }

    @Test
    void runWithSimpleAmd64() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();

        File input = new File(classLoader.getResource("./sample-3ds").getFile());
        File resource = new File(input.getParent());

        String dockerImage = "gaia3d/mago-3d-tiler:latest";
        List<String> argList = new ArrayList<>();
        argList.add("docker");
        argList.add("run");
        argList.add("--platform");
        argList.add("linux/amd64");
        argList.add("--rm");
        argList.add("-v");
        argList.add(resource.getAbsolutePath() + ":/workspace");
        argList.add(dockerImage);
        argList.add("--input");
        argList.add("/workspace/sample-3ds");
        argList.add("--output");
        argList.add("/workspace/sample-output");
        DockerRun.run(argList);
    }
}