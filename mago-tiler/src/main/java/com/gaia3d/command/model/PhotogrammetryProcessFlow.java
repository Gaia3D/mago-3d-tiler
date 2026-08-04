package com.gaia3d.command.model;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.assimp.AssimpConverterOptions;
import com.gaia3d.converter.kml.AttributeReader;
import com.gaia3d.converter.kml.FastKmlReader;
import com.gaia3d.converter.loader.BatchedFileLoader;
import com.gaia3d.converter.parametric.ExtrusionTempGenerator;
import com.gaia3d.process.TilingPipeline;
import com.gaia3d.process.postprocess.PostProcess;
import com.gaia3d.process.preprocess.*;
import com.gaia3d.process.tileprocess.Pipeline;
import com.gaia3d.process.tileprocess.TilingProcess;
import com.gaia3d.process.tileprocess.tile.PhotogrammetryTiler;
import com.gaia3d.terrain.GeoTiffTerrainHeightProvider;
import com.gaia3d.terrain.QuantizedMeshTerrainHeightProvider;
import com.gaia3d.terrain.TerrainHeightProvider;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;

import java.io.File;
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

        TerrainHeightProvider terrainHeightProvider = TerrainHeightProvider.empty();
        if (globalOptions.getTerrainPath() != null) {
            File terrainPath = new File(globalOptions.getTerrainPath());
            if (terrainPath.isFile() && terrainPath.getName().equalsIgnoreCase("layer.json")) {
                terrainHeightProvider = new QuantizedMeshTerrainHeightProvider(terrainPath.toPath());
            } else {
                List<GridCoverage2D> geoTiffs = fileLoader.loadGridCoverages(terrainPath, new ArrayList<>());
                terrainHeightProvider = new GeoTiffTerrainHeightProvider(geoTiffs);
            }
        }
        List<GridCoverage2D> geoidTiffs = new ArrayList<>();
        if (globalOptions.getGeoidPath() != null) {
            File geoidPath = new File(globalOptions.getGeoidPath());
            geoidTiffs = fileLoader.loadGridCoverages(geoidPath, geoidTiffs);
        }

        // preProcess
        List<PreProcess> preProcessors = new ArrayList<>();
        preProcessors.add(new TileInfoGenerator());
        preProcessors.add(new GaiaTexCoordCorrection());
        preProcessors.add(new GaiaScaler());

        preProcessors.add(new PhotogrammetryRotation());
        preProcessors.add(new GaiaTranslationForPhotogrammetry(terrainHeightProvider, geoidTiffs));
        PhotogrammetryMinimization gaiaMinimizer = new PhotogrammetryMinimization();
        preProcessors.add(gaiaMinimizer);

        // tileProcess
        TilingProcess tilingProcess = new PhotogrammetryTiler();

        // postProcess
        List<PostProcess> postProcessors = new ArrayList<>();

        // In photogrammetry there are no post-processes.
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
