package command;

import assimp.DataLoader;
import geometry.structure.GaiaScene;
import gltf.GltfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.slf4j.LoggerFactory;
import util.FileUtils;

import java.io.File;
import java.lang.reflect.Field;


@Slf4j
public class Main {
    public static void main(String[] args) {
        LoggerConfigurator.initLogger();
        Options options = new Options();
        options.addOption("h", "help", false, "print help");
        options.addOption("v", "version", false, "print version");
        options.addOption("r", "recursive", false, "recursive");
        options.addOption("i", "input", true, "input file path");
        options.addOption("o", "output", true, "output file path");
        options.addOption("it", "inputType", true, "input file type");
        options.addOption("ot", "outputType", true, "output file type");
        options.addOption("q", "quiet", false, "quiet mode");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("quiet")) {
                LoggerConfigurator.setLevel(Level.OFF);
            }
            start();
            if (cmd.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("GaiaConverter", options);
                return;
            }
            if (cmd.hasOption("version")) {
                log.info("GaiaConverter version 0.1.0");
                return;
            }

            if (!cmd.hasOption("input")) {
                log.error("input file path is not specified.");
                return;
            }
            if (!cmd.hasOption("output")) {
                log.error("output file path is not specified.");
                return;
            }

            File inputFile = new File(cmd.getOptionValue("input"));
            File outputFile = new File(cmd.getOptionValue("output"));
            excute(cmd, inputFile, outputFile, 0);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        end();
    }

    private static void excute(CommandLine command, File inputFile, File outputFile, int depth) {
        //String outputExtension = FilenameUtils.getExtension(outputFile.getName());
        String inputExtension = command.getOptionValue("inputType");
        String outputExtension = command.getOptionValue("outputType");
        if (inputFile.isFile() && inputExtension.equals(FilenameUtils.getExtension(inputFile.getName()))) {
            String outputFileName = FilenameUtils.removeExtension(inputFile.getName()) + "." + outputExtension;
            File output = new File(outputFile.getAbsolutePath() + File.separator + outputFileName);
            log.info("convert : " + inputFile.getAbsolutePath() + " -> " + output.getAbsolutePath());
            GaiaScene scene = DataLoader.load(inputFile.getAbsolutePath(), command);
            CommandOption.OutputType outputType = CommandOption.OutputType.fromExtension(outputExtension);
            if (outputType == CommandOption.OutputType.OUT_GLB) {
                GltfWriter.writeGlb(scene, output.getAbsolutePath());
            } else if (outputType == CommandOption.OutputType.OUT_GLTF) {
                GltfWriter.writeGltf(scene, output.getAbsolutePath());
            } else {
                log.error("output type is not supported. :: " + outputExtension);
            }
        } else if (inputFile.isDirectory()) {
            if (!command.hasOption("recursive") && (depth > 0)) {
                return;
            }
            for (File child : inputFile.listFiles()) {
                excute(command, child, outputFile, depth++);
            }
        }
    }

    private static void start() {
        log.info(
                " _______  ___      _______  _______  __   __  _______ \n" +
                        "|       ||   |    |   _   ||       ||  |_|  ||   _   |\n" +
                        "|    _  ||   |    |  |_|  ||  _____||       ||  |_|  |\n" +
                        "|   |_| ||   |    |       || |_____ |       ||       |\n" +
                        "|    ___||   |___ |       ||_____  ||       ||       |\n" +
                        "|   |    |       ||   _   | _____| || ||_|| ||   _   |\n" +
                        "|___|    |_______||__| |__||_______||_|   |_||__| |__|\n" +
                        "==================[Plasma Converter]==================");
    }

    private static void end() {
        log.info("================[Plasma Converter END]================");
    }
}
