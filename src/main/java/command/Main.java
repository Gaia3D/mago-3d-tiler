package command;

import assimp.AssimpConverter;
import geometry.structure.GaiaScene;
import geometry.types.FormatType;
import gltf.GltfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.util.Objects;


@Slf4j
public class Main {
    public static AssimpConverter assimpConverter = null;

    public static void main(String[] args) {
        Configurator.initLogger();
        Options options = new Options();

        options.addOption("i", "input", true, "input file path");
        options.addOption("o", "output", true, "output file path");
        options.addOption("it", "inputType", true, "input file type");
        options.addOption("ot", "outputType", true, "output file type");

        options.addOption("h", "help", false, "print help");
        options.addOption("v", "version", false, "print version");
        options.addOption("r", "recursive", false, "recursive");
        options.addOption("q", "quiet", false, "quiet mode");

        options.addOption("s", "scale", true, "scale factor");
        options.addOption("st", "strict", true, "strict mode");
        options.addOption("gn", "genNormals", false, "generate normals");
        options.addOption("gt", "quiet", false, "generate tangents");
        options.addOption("yz", "swapYZ", false, "swap YZ");
        options.addOption("nt", "ignoreTextures", false, "ignore textures");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("quiet")) {
                Configurator.setLevel(Level.OFF);
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

            assimpConverter = new AssimpConverter(cmd);
            File inputFile = new File(cmd.getOptionValue("input"));
            File outputFile = new File(cmd.getOptionValue("output"));
            excute(cmd, inputFile, outputFile, 0);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        end();
    }

    private static void excute(CommandLine command, File inputFile, File outputFile, int depth) {
        String inputExtension = command.getOptionValue("inputType");
        String outputExtension = command.getOptionValue("outputType");
        if (inputFile.isFile() && inputExtension.equals(FilenameUtils.getExtension(inputFile.getName()))) {
            String outputFileName = FilenameUtils.removeExtension(inputFile.getName()) + "." + outputExtension;
            File output = new File(outputFile.getAbsolutePath() + File.separator + outputFileName);
            log.info("convert : " + inputFile.getAbsolutePath() + " -> " + output.getAbsolutePath());
            GaiaScene scene = assimpConverter.load(inputFile.getAbsolutePath(), inputExtension);
            FormatType outputType = FormatType.fromExtension(outputExtension);
            if (outputType == FormatType.GLB) {
                GltfWriter.writeGlb(scene, output.getAbsolutePath());
            } else if (outputType == FormatType.GLTF) {
                GltfWriter.writeGltf(scene, output.getAbsolutePath());
            } else {
                log.error("output type is not supported. :: " + outputExtension);
            }
        } else if (inputFile.isDirectory()) {
            if (!command.hasOption("recursive") && (depth > 0)) {
                return;
            }
            for (File child : Objects.requireNonNull(inputFile.listFiles())) {
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
