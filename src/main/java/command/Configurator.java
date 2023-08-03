package command;

import org.apache.commons.cli.Options;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import process.ProcessOptions;

import java.nio.charset.StandardCharsets;

public class Configurator {
    public static final Level LEVEL = Level.ALL;
    private static final String DEFAULT_PATTERN = "%message%n";

    public static void initLogger() {
        initLogger(null);
    }

    public static void initLogger(String pattern) {
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

    private static ConsoleAppender createConsoleAppender(PatternLayout layout) {
        return ConsoleAppender.newBuilder().setName("Console").setTarget(ConsoleAppender.Target.SYSTEM_OUT).setLayout(layout).build();
    }

    private static void removeAllAppender(LoggerConfig loggerConfig) {
        loggerConfig.getAppenders().forEach((key, value) -> loggerConfig.removeAppender(key));
    }

}
