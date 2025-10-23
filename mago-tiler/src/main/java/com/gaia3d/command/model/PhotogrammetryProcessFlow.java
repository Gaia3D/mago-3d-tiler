package com.gaia3d.command.model;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.assimp.AssimpConverterOptions;
import com.gaia3d.converter.parametric.ExtrusionTempGenerator;
import com.gaia3d.converter.kml.AttributeReader;
import com.gaia3d.converter.kml.FastKmlReader;
import com.gaia3d.converter.loader.BatchedFileLoader;
import com.gaia3d.process.TilingPipeline;
import com.gaia3d.process.postprocess.GaiaMaximizer;
import com.gaia3d.process.postprocess.GaiaRelocator;
import com.gaia3d.process.postprocess.PostProcess;
import com.gaia3d.process.postprocess.batch.Batched3DModel;
import com.gaia3d.process.postprocess.batch.Batched3DModelV2;
import com.gaia3d.process.preprocess.*;
import com.gaia3d.process.tileprocess.Pipeline;
import com.gaia3d.process.tileprocess.TilingProcess;
import com.gaia3d.process.tileprocess.tile.PhotogrammetryTiler;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PhotogrammetryProcessFlow implements ProcessFlow {
    private static final String MODEL_NAME = "PhotogrammetryProcessFlow";
    private final GlobalOptions globalOptions = GlobalOptions.getInstance();

    public void run() throws IOException {
        // Photogrammetry Mesh
        FormatType inputFormat = globalOptions.getInputFormat();

        Converter converter = getConverter(inputFormat);
        AttributeReader kmlReader = new FastKmlReader();
        ExtrusionTempGenerator tempGenerator = new ExtrusionTempGenerator(converter);
        BatchedFileLoader fileLoader = new BatchedFileLoader(converter, kmlReader, tempGenerator);

        List<GridCoverage2D> geoTiffs = new ArrayList<>();
        if (globalOptions.getTerrainPath() != null) {
            geoTiffs = fileLoader.loadGridCoverages(geoTiffs);
        }

        // preProcess
        List<PreProcess> preProcessors = new ArrayList<>();
        preProcessors.add(new TileInfoGenerator());
        preProcessors.add(new GaiaTexCoordCorrection());
        preProcessors.add(new GaiaScaler());

        preProcessors.add(new PhotogrammetryRotation());
        preProcessors.add(new GaiaStrictTranslation(geoTiffs));
        PhotogrammetryMinimization gaiaMinimizer = new PhotogrammetryMinimization();
        preProcessors.add(gaiaMinimizer);

        // tileProcess
        TilingProcess tilingProcess = new PhotogrammetryTiler();

        // postProcess
        List<PostProcess> postProcessors = new ArrayList<>();
        postProcessors.add(new GaiaMaximizer());
        postProcessors.add(new GaiaRelocator());
        if (globalOptions.getTilesVersion()
                .equals("1.0")) {
            postProcessors.add(new Batched3DModel());
        } else {
            postProcessors.add(new Batched3DModelV2());
        }

        Pipeline processPipeline = new TilingPipeline(preProcessors, tilingProcess, postProcessors);
        processPipeline.process(fileLoader);
    }

    private Converter getConverter(FormatType formatType) {
        Converter converter;
        AssimpConverterOptions options = AssimpConverterOptions.builder()
                .build();
        options.setSplitByNode(globalOptions.isSplitByNode());
        converter = new AssimpConverter(options);
        return converter;
    }

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }
}
