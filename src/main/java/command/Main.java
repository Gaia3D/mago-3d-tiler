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
        //Command command = new Command();
        //command.excute(arg);
        LoggerConfigurator.initLogger();
        Options options = new Options();
        options.addOption("help", false, "print help");
        options.addOption("version", false, "print version");

        options.addOption("input", true, "input file path");
        options.addOption("output", true, "output file path");
        options.addOption("inputType", true, "input file type");
        options.addOption("outputType", "gltf", true, "output file type");

        options.addOption("quiet", false, "quiet mode");

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

            for (Option option : cmd.getOptions()) {
                log.info(option.getOpt());
            }

            String inputPath = cmd.getOptionValue("input");
            String outputPath = cmd.getOptionValue("output");
            File inputFile = new File(inputPath);
            File outputFile = new File(outputPath);
            excute(cmd, inputFile, outputFile);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        end();
    }

    private static void excute(CommandLine command, File inputFile, File outputFile) {
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
            for (File child : inputFile.listFiles()) {
                excute(command, child, outputFile);
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
