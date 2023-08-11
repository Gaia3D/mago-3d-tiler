package com.gaia3d.command;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.converter.FileLoader;
import com.gaia3d.process.ProcessFlow;
import com.gaia3d.process.ProcessOptions;
import com.gaia3d.process.TilerOptions;
import com.gaia3d.process.postprocess.GaiaRelocator;
import com.gaia3d.process.postprocess.PostProcess;
import com.gaia3d.process.postprocess.batch.Batched3DModel;
import com.gaia3d.process.postprocess.batch.GaiaBatcher;
import com.gaia3d.process.preprocess.GaiaRotator;
import com.gaia3d.process.preprocess.GaiaScaler;
import com.gaia3d.process.preprocess.GaiaTranslator;
import com.gaia3d.process.preprocess.PreProcess;
import com.gaia3d.process.tileprocess.TileProcess;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileExistsException;
import org.apache.logging.log4j.Level;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import com.gaia3d.process.tileprocess.tile.Gaia3DTiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Main class for Gaia3D Tiler.
 * @Author znkim
 * @Since 1.0.0
 */
@Slf4j
public class TilerMain {
    public static String version = "1.0.1";
    public static CommandLine command = null;

    public static void main(String[] args) {
        Configurator.initConsoleLogger();
        Options options = Configurator.createOptions();
        CommandLineParser parser = new DefaultParser();
        try {
            command = parser.parse(options, args);
            if (command.hasOption(ProcessOptions.LOG.getArgName())) {
                Configurator.initFileLogger(null, command.getOptionValue(ProcessOptions.LOG.getArgName()));
            }
            if (command.hasOption(ProcessOptions.QUIET.getArgName())) {
                Configurator.setLevel(Level.OFF);
            }
            start();
            if (command.hasOption(ProcessOptions.DEBUG.getArgName())) {
                log.info("Starting Gaia3D Tiler in debug mode.");
            }
            if (command.hasOption(ProcessOptions.HELP.getArgName())) {
                new HelpFormatter().printHelp("Gaia3D Tiler", options);
                return;
            }
            if (command.hasOption(ProcessOptions.VERSION.getArgName())) {
                log.info("Gaia3D Tiler version {}", version);
                return;
            }
            if (!command.hasOption(ProcessOptions.INPUT.getArgName())) {
                log.error("Input file path is not specified.");
                return;
            }
            if (!command.hasOption(ProcessOptions.OUTPUT.getArgName())) {
                log.error("output file path is not specified.");
                return;
            }
            execute();
        } catch (ParseException | IOException e) {
            log.error("Failed to parse command line properties", e);
        }
        underline();
    }

    private static void execute() throws IOException {
        long start = System.currentTimeMillis();

        File inputFile = new File(command.getOptionValue(ProcessOptions.INPUT.getArgName()));
        File outputFile = new File(command.getOptionValue(ProcessOptions.OUTPUT.getArgName()));
        String crs = command.getOptionValue(ProcessOptions.CRS.getArgName());
        String inputExtension = command.getOptionValue(ProcessOptions.INPUT_TYPE.getArgName());

        Path inputPath = createPath(inputFile);
        Path outputPath = createPath(outputFile);
        if (!validate(outputPath)) {
            return;
        }
        FormatType formatType = FormatType.fromExtension(inputExtension);
        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem source = null;

        if (formatType != FormatType.KML) {
            source = (crs != null) ? factory.createFromName("EPSG:" + crs) : null;
        }
        FileLoader fileLoader = new FileLoader(command);

        List<PreProcess> preProcessors = new ArrayList<>();
        if (command.hasOption("swapYZ")) {
            preProcessors.add(new GaiaRotator());
        }
        preProcessors.add(new GaiaTranslator(source));
        preProcessors.add(new GaiaScaler());
        //preProcessors.add(new GeometryOptimizer()); // son.***

        TilerOptions tilerOptions = TilerOptions.builder()
                .inputPath(inputPath)
                .outputPath(outputPath)
                .inputFormatType(formatType)
                .source(source)
                .build();
        TileProcess tileProcess = new Gaia3DTiler(tilerOptions, command);

        List<PostProcess> postProcessors = new ArrayList<>();
        postProcessors.add(new GaiaRelocator());
        postProcessors.add(new GaiaBatcher(command));
        postProcessors.add(new Batched3DModel(command));

        ProcessFlow processFlow = new ProcessFlow(preProcessors, tileProcess, postProcessors);
        processFlow.process(fileLoader);

        long end = System.currentTimeMillis();
        log.info("Tiling finished in {} seconds.", (end - start) / 1000);
    }

    private static boolean validate(Path outputPath) throws IOException {
        File output = outputPath.toFile();
        if (!output.exists()) {
            throw new FileExistsException("Output path is not exist.");
        } else if (!output.isDirectory()) {
            throw new NotDirectoryException("Output path is not directory.");
        } else if (!output.canWrite()) {
            throw new IOException("Output path is not writable.");
        }
        return true;
    }

    private static void start() {
        log.info(
                " _______  ___      _______  _______  __   __  _______ \n" +
                "|       ||   |    |   _   ||       ||  |_|  ||   _   |\n" +
                "|    _  ||   |    |  |_|  ||  _____||       ||  |_|  |\n" +
                "|   |_| ||   |    |       || |_____ |       ||       |\n" +
                "|    ___||   |___ |       ||_____  ||       ||       |\n" +
                "|   |    |       ||   _   | _____| || ||_|| ||   _   |\n" +
                "|___|    |_______||__| |__||_______||_|   |_||__| |__|");
        underline();
    }

    private static void underline() {
        log.info("======================================================");
    }

    private static Path createPath(File file) {
        Path path = file.toPath();
        boolean result = file.mkdir();
        if (result) {
            log.info("Created new directory: {}", path);
        }
        return path;
    }
}
