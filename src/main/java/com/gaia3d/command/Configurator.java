package com.gaia3d.command;

import com.gaia3d.process.ProcessOptions;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.nio.charset.StandardCharsets;

/**
 * Class for setting up logs.
 *
 * @author znkim
 * @see <a href="https://logging.apache.org/log4j/2.x/manual/configuration.html">Log4j2 Configuration</a>
 * @since 1.0.0
 */
public class Configurator {
    public static final Level LEVEL = Level.ALL;
    private static final String DEFAULT_PATTERN = "%message%n";

    public static void initConsoleLogger() {
        initConsoleLogger(null);
    }

    public static void initConsoleLogger(String pattern) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);

        removeAllAppender(loggerConfig);
        if (pattern == null) {
            pattern = DEFAULT_PATTERN;
        }
        PatternLayout layout = createPatternLayout(pattern);
        ConsoleAppender consoleAppender = createConsoleAppender(layout);

        loggerConfig.setLevel(LEVEL);
        loggerConfig.addAppender(consoleAppender, LEVEL, null);
        ctx.updateLoggers();

        consoleAppender.start();
    }

    public static void initFileLogger(String pattern, String path) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);

        if (pattern == null) {
            pattern = DEFAULT_PATTERN;
        }
        PatternLayout layout = createPatternLayout(pattern);
        FileAppender fileAppender = createRollingFileAppender(layout, path);

        loggerConfig.setLevel(LEVEL);
        loggerConfig.addAppender(fileAppender, LEVEL, null);
        ctx.updateLoggers();
        fileAppender.start();
    }

    public static Options createOptions() {
        Options options = new Options();
        for (ProcessOptions processOptions : ProcessOptions.values()) {
            options.addOption(processOptions.getShortName(), processOptions.getLongName(), processOptions.isArgRequired(), processOptions.getDescription());
        }
        return options;
    }

    public static void setLevel(Level level) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(level);
        ctx.updateLoggers();
    }

    private static PatternLayout createPatternLayout(String pattern) {
        return PatternLayout.newBuilder().withPattern(pattern).withCharset(StandardCharsets.UTF_8).build();
    }

    private static FileAppender createRollingFileAppender(PatternLayout layout, String path) {
        if (path == null) {
            path = "logs/gaia3d-tiler.log";
        }
        return FileAppender.newBuilder().setName("FileLogger").withFileName(path).withAppend(true).withImmediateFlush(true).withBufferedIo(true).withBufferSize(8192).setLayout(layout).build();
    }

    private static ConsoleAppender createConsoleAppender(PatternLayout layout) {
        return ConsoleAppender.newBuilder().setName("Console").setTarget(ConsoleAppender.Target.SYSTEM_OUT).setLayout(layout).build();
    }

    private static void removeAllAppender(LoggerConfig loggerConfig) {
        loggerConfig.getAppenders().forEach((key, value) -> loggerConfig.removeAppender(key));
    }

}
