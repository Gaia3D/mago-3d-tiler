package com.gaia3d.command;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.TriangleFileLoader;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.geometry.citygml.CityGmlConverter;
import com.gaia3d.converter.geometry.geojson.GeoJsonConverter;
import com.gaia3d.converter.geometry.shape.ShapeConverter;
import com.gaia3d.process.ProcessFlow;
import com.gaia3d.process.ProcessFlowThread;
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
import com.gaia3d.process.tileprocess.Process;
import com.gaia3d.process.tileprocess.TileProcess;
import com.gaia3d.process.tileprocess.tile.Gaia3DTiler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileExistsException;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class BatchProcessModel implements ProcessFlowModel{
    public void run(CommandLine command) throws IOException {
        File inputFile = new File(command.getOptionValue(ProcessOptions.INPUT.getArgName()));
        File outputFile = new File(command.getOptionValue(ProcessOptions.OUTPUT.getArgName()));
        String crs = command.getOptionValue(ProcessOptions.CRS.getArgName());
        String proj = command.getOptionValue(ProcessOptions.PROJ4.getArgName());
        String inputExtension = command.getOptionValue(ProcessOptions.INPUT_TYPE.getArgName());

        Path inputPath = createPath(inputFile);
        Path outputPath = createPath(outputFile);
        if (!validate(outputPath)) {
            return;
        }
        FormatType formatType = FormatType.fromExtension(inputExtension);

        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem source = null;
        if (proj != null && !proj.isEmpty()) {
            source = factory.createFromParameters("CUSTOM", proj);
        } else {
            source = (crs != null && !crs.isEmpty()) ? factory.createFromName("EPSG:" + crs) : null;
        }

        Converter converter;
        if (formatType == FormatType.CITY_GML) {
            converter = new CityGmlConverter(command);
        } else if (formatType == FormatType.SHP) {
            converter = new ShapeConverter(command, source);
        } else if (formatType == FormatType.GEOJSON || formatType == FormatType.JSON) {
            converter = new GeoJsonConverter(command, source);
        } else {
            converter = new AssimpConverter(command);
        }

        TriangleFileLoader fileLoader = new TriangleFileLoader(command, converter);

        List<PreProcess> preProcessors = new ArrayList<>();
        if (command.hasOption(ProcessOptions.Y_UP_AXIS.getArgName())) {
            preProcessors.add(new GaiaRotator());
        }
        preProcessors.add(new GaiaTranslator(source, command));
        preProcessors.add(new GaiaScaler());

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

        Process processFlow;
        if (command.hasOption(ProcessOptions.MULTI_THREAD.getArgName())) {
            processFlow = new ProcessFlowThread(preProcessors, tileProcess, postProcessors);
        } else {
            processFlow = new ProcessFlow(preProcessors, tileProcess, postProcessors);
        }
        processFlow.process(fileLoader);
    }

    protected static boolean validate(Path outputPath) throws IOException {
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

    protected static Path createPath(File file) {
        Path path = file.toPath();
        boolean result = file.mkdir();
        if (result) {
            log.info("Created new directory: {}", path);
        }
        return path;
    }
}
