package com.gaia3d.command.mago;

import com.gaia3d.command.LoggingConfiguration;
import com.gaia3d.util.DecimalUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;

import java.io.IOException;

/**
 * Main class for mago 3DTiler.
 */
@SuppressWarnings("ALL")
@Slf4j
public class Mago3DTilerMain {
    private static final String PROGRAM_NAME = "mago-3d-tiler";

    public static void main(String[] args) {
        try {
            Options options = LoggingConfiguration.createOptions();
            CommandLineParser parser = new DefaultParser();
            CommandLine command = parser.parse(options, args);

            boolean isHelp = command.hasOption(ProcessOptions.HELP.getLongName());
            boolean isQuiet = command.hasOption(ProcessOptions.QUIET.getLongName());
            boolean hasLogPath = command.hasOption(ProcessOptions.LOG_PATH.getLongName());
            boolean isDebug = command.hasOption(ProcessOptions.DEBUG.getLongName());
            boolean isMerge = command.hasOption(ProcessOptions.MERGE.getLongName());

            // Logging configuration
            if (isQuiet) {
                LoggingConfiguration.setLevel(Level.OFF);
            } else if (isDebug) {
                LoggingConfiguration.useAsyncAppended = false;
                LoggingConfiguration.initConsoleLogger("[%p][%d{HH:mm:ss}][%C{2}(%M:%L)]::%message%n");
                if (hasLogPath) {
                    LoggingConfiguration.initFileLogger("[%p][%d{HH:mm:ss}][%C{2}(%M:%L)]::%message%n", command.getOptionValue(ProcessOptions.LOG_PATH.getLongName()));
                }
                LoggingConfiguration.setLevel(Level.DEBUG);
            } else {
                LoggingConfiguration.initConsoleLogger();
                if (hasLogPath) {
                    LoggingConfiguration.initFileLogger(null, command.getOptionValue(ProcessOptions.LOG_PATH.getLongName()));
                }
                LoggingConfiguration.setLevel(Level.INFO);
            }

            printStart();
            if (isHelp || args.length == 0) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.setOptionComparator(null);
                formatter.setWidth(200);
                formatter.setOptPrefix("-");
                formatter.setSyntaxPrefix("Usage: ");
                formatter.setLongOptPrefix(" --");
                formatter.setLongOptSeparator(" ");
                formatter.printHelp("command options", options);
                return;
            }
            GlobalOptions.init(command);
            Mago3DTiler mago3DTiler = new Mago3DTiler();
            if (isMerge) {
                mago3DTiler.merge();
            } else {
                mago3DTiler.execute();
            }

            GlobalOptions globalOptions = GlobalOptions.getInstance();
        } catch (ParseException e) {
            log.error("[ERROR] Failed to parse command line options, Please check the arguments.", e);
            throw new RuntimeException("Failed to parse command line options, Please check the arguments.", e);
        } catch (IOException e) {
            log.error("[ERROR] Failed to run process, Please check the arguments.", e);
            throw new RuntimeException("Failed to run main process, Please check the arguments.", e);
        } catch (Exception e) {
            log.error("[ERROR] Failed to run main process.", e);
            throw new RuntimeException("Failed to run main process.", e);
        }
        printEnd();
        LoggingConfiguration.destroyLogger();
    }

    /**
     * Prints the program information and the java version information.
     */
    private static void printStart() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        String programInfo = globalOptions.getProgramInfo();
        drawLine();
        log.info(programInfo);
        drawLine();
    }

    /**
     * Prints the total file count, total tile count, and the process time.
     */
    private static void printEnd() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        drawLine();
        log.info("[Process Summary]");
        log.info("Total tile contents count : {}", globalOptions.getTileCount());
        log.info("Total tileset.json File Size : {}", DecimalUtils.byteCountToDisplaySize(globalOptions.getTilesetSize()));
        drawLine();
    }

    public static void drawLine() {
        log.info("----------------------------------------");
    }
}
