package com.gaia3d.command.mago;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.*;
import com.gaia3d.process.ProcessOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;

import java.io.IOException;

/**
 * Main class for mago 3DTiler.
 * @author znkim
 */
@Slf4j
public class Mago3DTilerMain {


    public static void main(String[] args) {
        try {
            Options options = Configurator.createOptions();
            CommandLineParser parser = new DefaultParser();
            CommandLine command = parser.parse(Configurator.createOptions(), args);
            boolean isHelp = command.hasOption(ProcessOptions.HELP.getArgName());
            boolean isQuiet = command.hasOption(ProcessOptions.QUIET.getArgName());
            boolean hasLogPath = command.hasOption(ProcessOptions.LOG.getArgName());
            boolean isDebug = command.hasOption(ProcessOptions.DEBUG.getArgName());
            boolean isVersion = command.hasOption(ProcessOptions.VERSION.getArgName());

            // Logging configuration
            if (isQuiet) {
                Configurator.setLevel(Level.OFF);
            } else if (isDebug) {
                Configurator.initConsoleLogger("[%p][%d{HH:mm:ss}][%C{2}(%M:%L)]::%message%n");
                if (hasLogPath) {
                    Configurator.initFileLogger("[%p][%d{HH:mm:ss}][%C{2}(%M:%L)]::%message%n", command.getOptionValue(ProcessOptions.LOG.getArgName()));
                }
                Configurator.setLevel(Level.DEBUG);
            } else {
                Configurator.initConsoleLogger();
                if (hasLogPath) {
                    Configurator.initFileLogger(null, command.getOptionValue(ProcessOptions.LOG.getArgName()));
                }
            }

            printStart();
            if (isVersion) {
                printVersion();
            }
            if (isHelp) {
                new HelpFormatter().printHelp("Gaia3D Tiler", options);
                return;
            }
            GlobalOptions.init(command);
            Mago3DTiler mago3DTiler = new Mago3DTiler();
            mago3DTiler.execute();
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse command line options, Please check the arguments.", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to run process, Please check the arguments.", e);
        }
        printEnd();
    }

    /**
     * Prints the program information and the java version information.
     */
    private static void printStart() {
        log.info("\n" +
                "┌┬┐┌─┐┌─┐┌─┐  -┐┌┬┐  ┌┬┐┬┬  ┌─┐┬─┐\n" +
                "│││├─┤│ ┬│ │  -┤ ││   │ ││  ├┤ ├┬┘\n" +
                "┴ ┴┴ ┴└─┘└─┘  -┘-┴┘   ┴ ┴┴─┘└─┘┴└─\n" +
                "----------------------------------------"
        );
    }

    /**
     * Prints the program information and the java version information.
     */
    private static void printVersion() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        String programInfo = globalOptions.getProgramInfo();
        String javaVersionInfo = globalOptions.getJavaVersionInfo();
        log.info(
                programInfo + "\n" +
                javaVersionInfo
        );
        log.info("----------------------------------------");
    }

    /**
     * Prints the total file count, total tile count, and the process time.
     */
    private static void printEnd() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        long startTime = globalOptions.getStartTime();
        long endTime = System.currentTimeMillis();
        log.info("----------------------------------------");
        log.info("End Process Time : {}", millisecondToDisplayTime(endTime - startTime));
        log.info("Total tile contents count : {}", globalOptions.getTileCount());
        log.info("Total 'tileset.json' File Size : {}", byteCountToDisplaySize(globalOptions.getTilesetSize()));
        log.info("----------------------------------------");
    }

    /**
     * Converts the byte size to the display size.
     */
    private static String byteCountToDisplaySize(long size) {
        String displaySize;
        if (size / 1073741824L > 0L) {
            displaySize = size / 1073741824L + "GB";
        } else if (size / 1048576L > 0L) {
            displaySize = size / 1048576L + "MB";
        } else if (size / 1024L > 0L) {
            displaySize = size / 1024L + "KB";
        } else {
            displaySize = size + "bytes";
        }
        return displaySize;
    }

    /**
     * Converts the millisecond to the display time.
     */
    private static String millisecondToDisplayTime(long millis) {
        String displayTime = "";
        if (millis / 3600000L > 0L) {
            displayTime += millis / 3600000L + "h ";
        }
        if (millis / 60000L > 0L) {
            displayTime += millis / 60000L + "m ";
        }
        if (millis / 1000L > 0L) {
            displayTime += millis / 1000L + "s ";
        }
        displayTime += millis % 1000L + "ms";
        return displayTime;
    }
}
