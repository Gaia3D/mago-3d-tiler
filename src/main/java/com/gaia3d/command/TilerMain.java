package com.gaia3d.command;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.process.ProcessOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;

import java.io.IOException;

/**
 * Main class for Gaia3D Tiler.
 *
 * @author znkim
 * @since 1.0.0
 */
@Slf4j
public class TilerMain {
    private static String VERSION;
    private static long START_TIME = System.currentTimeMillis();
    private static long END_TIME = 0;

    public static void main(String[] args) {
        String version = TilerMain.class.getPackage().getImplementationVersion();
        VERSION = version != null ? version : "DEV";

        Configurator.initConsoleLogger();
        Options options = Configurator.createOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine command;
        try {
            command = parser.parse(options, args);
            if (command.hasOption(ProcessOptions.DEBUG.getArgName())) {
                Configurator.initConsoleLogger("[%p][%d{HH:mm:ss}][%C{2}(%M:%L)]::%message%n");
            }
            if (command.hasOption(ProcessOptions.LOG.getArgName())) {
                Configurator.initFileLogger(null, command.getOptionValue(ProcessOptions.LOG.getArgName()));
            }
            if (command.hasOption(ProcessOptions.QUIET.getArgName())) {
                Configurator.setLevel(Level.OFF);
            }
            start();
            if (command.hasOption(ProcessOptions.DEBUG.getArgName())) {
                log.info("Starting Gaia3D Tiler in debug mode.");
            }
            if (command.hasOption(ProcessOptions.LOG.getArgName())) {
                Configurator.initFileLogger(null, command.getOptionValue(ProcessOptions.LOG.getArgName()));
            }

            if (command.hasOption(ProcessOptions.HELP.getArgName())) {
                new HelpFormatter().printHelp("Gaia3D Tiler", options);
                return;
            }
            if (command.hasOption(ProcessOptions.VERSION.getArgName())) {
                log.info("Gaia3D Tiler version {}", version);
                return;
            }
            if (!command.hasOption(ProcessOptions.INPUT.getArgName())) {
                log.error("Input file path is not specified.");
                return;
            }
            if (!command.hasOption(ProcessOptions.OUTPUT.getArgName())) {
                log.error("output file path is not specified.");
                return;
            }
            execute(command);
        } catch (ParseException | IOException e) {
            log.error("Failed to parse command line properties", e);
        }
        end();
    }

    private static void execute(CommandLine command) throws IOException {
        String inputExtension = command.getOptionValue(ProcessOptions.INPUT_TYPE.getArgName());
        FormatType formatType = FormatType.fromExtension(inputExtension);

        if (formatType == FormatType.LAS || formatType == FormatType.LAZ) {
            LasProcessFlow processFlow = new LasProcessFlow();
            processFlow.run(command);
        } else {
            TriangleProcessFlow processFlow = new TriangleProcessFlow();
            processFlow.run(command);
        }
    }

    private static void start() {
        log.info("\n" +
                "┏┓┓ ┏┓┏┓┳┳┓┏┓\n" +
                "┃┃┃ ┣┫┗┓┃┃┃┣┫\n" +
                "┣┛┗┛┛┗┗┛┛ ┗┛┗\n" +
                "3DTiler:" + VERSION
        );
        underline();
    }

    private static void end() {
        END_TIME = System.currentTimeMillis();
        log.info("Tiling finished in {} seconds.", (END_TIME - START_TIME) / 1000);
        underline();
    }

    private static void underline() {
        log.info("======================================================");
    }
}
