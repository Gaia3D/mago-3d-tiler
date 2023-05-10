package command;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.nio.charset.StandardCharsets;

public class LoggerConfigurator {
    public static final Level LEVEL = Level.ALL;
    //private static final String PATTERN = "[%level{lowerCase=true} %date{yyyy/MM/dd HH:mm:ss.SSS z} <%thread> tid=%tid] %message%n%throwable";
    private static final String PATTERN = "%message%n";

    public static void initLogger() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);

        removeAppenders(loggerConfig);
        PatternLayout layout = createPatternLayout(PATTERN);
        ConsoleAppender consoleAppender = createConsoleAppender(layout);

        loggerConfig.setLevel(LEVEL);
        loggerConfig.addAppender(consoleAppender, LEVEL, null);
        ctx.updateLoggers();
    }

    public static void setLevel(Level level) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(level);
        ctx.updateLoggers();
    }

    // create PatternLayout
    private static PatternLayout createPatternLayout(String pattern) {
        return PatternLayout.newBuilder()
                .withPattern(pattern)
                .withCharset(StandardCharsets.UTF_8)
                .build();
    }

    // create ConsoleAppender
    private static ConsoleAppender createConsoleAppender(PatternLayout layout) {
        return ConsoleAppender.newBuilder()
                .setName("Console")
                .setTarget(ConsoleAppender.Target.SYSTEM_OUT)
                .setLayout(layout)
                .build();
    }

    // removeAppenders
    private static void removeAppenders(LoggerConfig loggerConfig) {
        loggerConfig.getAppenders().forEach((key, value) -> {
            loggerConfig.removeAppender(key);
        });
    }
}
