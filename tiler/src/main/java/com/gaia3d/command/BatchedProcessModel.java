package com.gaia3d.command;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.BatchedFileLoader;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.geometry.citygml.CityGmlConverter;
import com.gaia3d.converter.geometry.geojson.GeoJsonConverter;
import com.gaia3d.converter.geometry.shape.ShapeConverter;
import com.gaia3d.converter.kml.FastKmlReader;
import com.gaia3d.converter.kml.JacksonKmlReader;
import com.gaia3d.converter.kml.AttributeReader;
import com.gaia3d.process.TilingPipeline;
import com.gaia3d.process.postprocess.GaiaMaximizer;
import com.gaia3d.process.postprocess.GaiaRelocator;
import com.gaia3d.process.postprocess.PostProcess;
import com.gaia3d.process.postprocess.batch.Batched3DModel;
import com.gaia3d.process.preprocess.*;
import com.gaia3d.process.tileprocess.Pipeline;
import com.gaia3d.process.tileprocess.TilingProcess;
import com.gaia3d.process.tileprocess.tile.Batched3DModelTiler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileExistsException;
import org.geotools.coverage.grid.GridCoverage2D;

import java.io.File;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class BatchedProcessModel implements ProcessFlowModel {
    public void run() throws IOException {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        String inputExtension = globalOptions.getInputFormat();
        FormatType inputFormat = FormatType.fromExtension(inputExtension);
        boolean isYUpAxis = getYUpAxis(inputFormat, globalOptions.isYUpAxis());
        Converter converter = getConverter(inputFormat);
        AttributeReader kmlReader = new JacksonKmlReader();
        kmlReader = new FastKmlReader();
        BatchedFileLoader fileLoader = new BatchedFileLoader(converter, kmlReader);

        List<GridCoverage2D> geoTiffs = new ArrayList<>();
        if (globalOptions.getTerrainPath() != null) {
            geoTiffs = fileLoader.loadGridCoverages(geoTiffs);
        }
        List<PreProcess> preProcessors = new ArrayList<>();
        preProcessors.add(new GaiaTileInfoInitiator());
        preProcessors.add(new GaiaTexCoordCorrector());
        preProcessors.add(new GaiaScaler());
        if (!isYUpAxis) {
            preProcessors.add(new GaiaRotator());
        }
        preProcessors.add(new GaiaTranslator(geoTiffs));
        preProcessors.add(new GaiaMinimizer());

        TilingProcess tilingProcess = new Batched3DModelTiler();

        List<PostProcess> postProcessors = new ArrayList<>();
        postProcessors.add(new GaiaMaximizer());
        postProcessors.add(new GaiaRelocator());
        postProcessors.add(new Batched3DModel());

        Pipeline processPipeline = new TilingPipeline(preProcessors, tilingProcess, postProcessors);
        processPipeline.process(fileLoader);
    }

    private boolean getYUpAxis(FormatType formatType, boolean isYUpAxis) {
        if (formatType == FormatType.CITY_GML || formatType == FormatType.SHP || formatType == FormatType.GEOJSON || formatType == FormatType.JSON) {
            isYUpAxis = true;
        }
        return isYUpAxis;
    }
    private Converter getConverter(FormatType formatType) {
        Converter converter;
        if (formatType == FormatType.CITY_GML) {
            converter = new CityGmlConverter();
        } else if (formatType == FormatType.SHP) {
            converter = new ShapeConverter();
        } else if (formatType == FormatType.GEOJSON || formatType == FormatType.JSON) {
            converter = new GeoJsonConverter();
        } else {
            converter = new AssimpConverter();
        }
        return converter;
    }

    @Override
    public String getModelName() {
        return "BatchedProcessModel";
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
