package com.gaia3d.command;

import com.gaia3d.command.mago.ProcessOptions;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Class for setting up logs.
 */
public class LoggingConfiguration {
    public static final Level LEVEL = Level.ALL;
    private static final String DEFAULT_PATTERN = "%message%n";

    public static boolean useAsyncAppended = true;

    public static void initConsoleLogger() {
        initConsoleLogger(null);
    }

    public static void initConsoleLogger(String pattern) {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(LEVEL);

        removeAllAppender(loggerConfig);
        if (pattern == null) {
            pattern = DEFAULT_PATTERN;
        }

        PatternLayout layout = createPatternLayout(pattern);
        ConsoleAppender consoleAppender = createConsoleAppender(layout, config);
        config.addAppender(consoleAppender);
        consoleAppender.start();

        if (useAsyncAppended) {
            AsyncAppender asyncAppender = createAsyncAppender(consoleAppender, config);
            config.addAppender(asyncAppender);
            asyncAppender.start();
            loggerConfig.addAppender(asyncAppender, LEVEL, null);
        } else {
            loggerConfig.addAppender(consoleAppender, LEVEL, null);
        }
        context.updateLoggers();

        consoleAppender.start();
    }

    public static void initFileLogger(String pattern, String path) throws IOException {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(LEVEL);

        if (pattern == null) {
            pattern = DEFAULT_PATTERN;
        }
        PatternLayout layout = createPatternLayout(pattern);
        FileAppender fileAppender = createRollingFileAppender(layout, path, config);
        config.addAppender(fileAppender);

        if (useAsyncAppended) {
            AsyncAppender asyncAppender = createAsyncAppender(fileAppender, config);
            config.addAppender(asyncAppender);
            asyncAppender.start();
            loggerConfig.addAppender(asyncAppender, LEVEL, null);
        } else {
            loggerConfig.addAppender(fileAppender, LEVEL, null);
        }

        ctx.updateLoggers();
        fileAppender.start();
    }

    public static void destroyLogger() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);

        Appender appender = loggerConfig.getAppenders().get("FileLogger");
        if (appender != null) {
            appender.stop();
        }

        removeAllAppender(loggerConfig);
        ctx.updateLoggers();
    }

    public static Options createOptions() {
        Options options = new Options();
        for (ProcessOptions processOptions : ProcessOptions.values()) {
            Option newOption = new Option(processOptions.getShortName(), processOptions.getLongName(), processOptions.isArgValueRequired(), processOptions.getDescription());
            options.addOption(newOption);
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

    private static FileAppender createRollingFileAppender(PatternLayout layout, String path, Configuration configuration) throws IOException {
        if (path == null) {
            path = "logs/mago-3d-tiler.log";
        }
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            File parentDir = file.getParentFile();

            String fileName = file.getName();
            String baseName = FilenameUtils.getBaseName(fileName);
            String extension = FilenameUtils.getExtension(fileName);

            long lastModified = file.lastModified();
            String newFileName = baseName + "-" + lastModified + (extension.isEmpty() ? "" : "." + extension);

            File backup = new File(parentDir, newFileName);
            Files.move(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }


        return FileAppender.newBuilder()
                .setName("FileLogger")
                .withFileName(path).withAppend(true)
                .setLayout(layout)
                .setConfiguration(configuration)
                .build();
    }

    private static AsyncAppender createAsyncAppender(Appender appender, Configuration config) {
        AppenderRef ref = AppenderRef.createAppenderRef(appender.getName(), null, null);
        AppenderRef[] refs = new AppenderRef[] {ref};
        return AsyncAppender.newBuilder()
                .setName("Async" + appender.getName())
                .setAppenderRefs(refs)
                .setBlocking(false)
                .setBufferSize(1024)
                .setConfiguration(config)
                .build();
    }

    private static ConsoleAppender createConsoleAppender(PatternLayout layout, Configuration config) {
        return ConsoleAppender
                .newBuilder()
                .setName("Console")
                .setTarget(ConsoleAppender.Target.SYSTEM_OUT)
                .setLayout(layout)
                .setConfiguration(config)
                .build();
    }

    private static void removeAllAppender(LoggerConfig loggerConfig) {
        loggerConfig.getAppenders().forEach((key, value) -> loggerConfig.removeAppender(key));
    }
}
