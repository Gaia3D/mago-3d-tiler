package command;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.joran.spi.JoranException;
import org.slf4j.LoggerFactory;

import java.net.URL;

public class LoggerConfiguration {
    private final static String PATTERN = "%date %-5level [%thread] %logger{36} %m%n %rEx";

    public static void test() {
        URL url = Main.class.getResource("/logback.xml");
        JoranConfigurator configurator = new JoranConfigurator();
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        configurator.setContext(loggerContext);
        try {
            configurator.doConfigure(url);
        } catch (JoranException e) {
            e.printStackTrace();
        }

        System.out.println("");
    }




    public static Logger logger() {
        return (Logger) LoggerFactory.getLogger(Main.class);
    }
    public static LoggerContext loggerContext() {
        LoggerContext context = logger().getLoggerContext();
        //return (LoggerContext) LoggerFactory.getILoggerFactory();
        return context;
    }

    public static Logger setLogger() {
        Logger logger = logger();
        LoggerContext context = loggerContext();
        logger.setLevel(Level.INFO);
        logger.addAppender(consoleAppender(context));

        return logger;
    }

    public static ConsoleAppender consoleAppender (LoggerContext context) {
        ConsoleAppender consoleAppender = new ConsoleAppender();
        consoleAppender.setContext(context);
        consoleAppender.setEncoding("UTF-8");

        PatternLayout patternLayout = new PatternLayout();
        patternLayout.setPattern(PATTERN);
        consoleAppender.setLayout(patternLayout);
        return consoleAppender;
    }
}
