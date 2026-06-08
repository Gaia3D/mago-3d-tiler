package com.gaia3d.local;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

@Slf4j
public class DockerRun {
    public static void run(List<String> argList) throws IOException {
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
