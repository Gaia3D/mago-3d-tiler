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
@Deprecated
public class TilerMain {
    private static String PROGRAM_INFO;
    private static String JAVA_INFO;
    private static long START_TIME = System.currentTimeMillis();
    private static long END_TIME = 0;

    public static void main(String[] args) {
        getProgramInfo();
        getJavaInfo();
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
                log.info("Gaia3D Tiler version {}", PROGRAM_INFO);
                return;
            }
            if (!command.hasOption(ProcessOptions.INPUT.getArgName())) {
                log.error("Input file path is not specified.");
                return;
            }
            if (!command.hasOption(ProcessOptions.INPUT_TYPE.getArgName())) {
                log.error("InputType is not specified.");
                return;
            }
            if (!command.hasOption(ProcessOptions.OUTPUT.getArgName())) {
                log.error("output file path is not specified.");
                return;
            }
            log.info("Input file path : {}", command.getOptionValue(ProcessOptions.INPUT.getArgName()));
            log.info("Output file path : {}", command.getOptionValue(ProcessOptions.OUTPUT.getArgName()));

            GeotoolsConfigurator geotoolsConfigurator = new GeotoolsConfigurator();
            geotoolsConfigurator.setEpsg();

            execute(command);
        } catch (ParseException | IOException e) {
            log.error("Failed to parse command line properties", e);
        }
        end();
    }

    private static void execute(CommandLine command) throws IOException {
        String inputExtension = command.getOptionValue(ProcessOptions.INPUT_TYPE.getArgName());
        FormatType formatType = FormatType.fromExtension(inputExtension);

        ProcessFlowModel processFlow = null;
        if (formatType == FormatType.LAS || formatType == FormatType.LAZ) {
            processFlow = new PointCloudProcessModel();
        } /*else if (outputType == FormatType.I3DM) {
            processFlow = new InstanceProcessModel();
        } */else {
            processFlow = new BatchedProcessModel();
        }
        processFlow.run();
    }

    private static void getProgramInfo() {
        String version = TilerMain.class.getPackage().getImplementationVersion();
        String title = TilerMain.class.getPackage().getImplementationTitle();
        String vendor = TilerMain.class.getPackage().getImplementationVendor();
        version = version == null ? "dev-version" : version;
        title = title == null ? "3d-tiler" : title;
        vendor = vendor == null ? "Gaia3D, Inc." : vendor;
        PROGRAM_INFO = title + "(" + version + ") by " + vendor;
    }

    private static void getJavaInfo() {
        String result;
        String jdkVersion = System.getProperty("java.version");
        String vendor = System.getProperty("java.vendor");
        result = "JAVA Version : " + jdkVersion + " (" + vendor + ") ";
        JAVA_INFO = result;
    }

    private static void start() {
        log.info("\n" +
                "┌┬┐┌─┐┌─┐┌─┐  ┌┬┐┬┬  ┌─┐┬─┐\n" +
                "│││├─┤│ ┬│ │───│ ││  ├┤ ├┬┘\n" +
                "┴ ┴┴ ┴└─┘└─┘   ┴ ┴┴─┘└─┘┴└─\n" +
                PROGRAM_INFO + "\n" +
                JAVA_INFO
        );
        underline();
    }

    private static void end() {
        END_TIME = System.currentTimeMillis();
        log.info("Tiling finished in {} seconds.", (END_TIME - START_TIME) / 1000);
        underline();
    }

    private static void underline() {
        log.info("----------------------------------------");
    }
}
