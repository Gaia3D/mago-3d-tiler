package com.gaia3d.command.model;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.geometry.ExtrusionTempGenerator;
import com.gaia3d.converter.geometry.citygml.CityGmlConverter;
import com.gaia3d.converter.geometry.geojson.GeoJsonConverter;
import com.gaia3d.converter.geometry.indoorgml.IndoorGmlConverter;
import com.gaia3d.converter.geometry.shape.ShapeConverter;
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

        // preProcess (GaiaTexCoordCorrector, GaiaScaler, GaiaRotator, GaiaTranslatorExact, GaiaMinimizer)
        List<PreProcess> preProcessors = new ArrayList<>();
        preProcessors.add(new TileInfoGenerator());
        preProcessors.add(new GaiaTexCoordCorrection());
        preProcessors.add(new GaiaScaler());

        preProcessors.add(new PhotogrammetryRotation());
        preProcessors.add(new GaiaStrictTranslation(geoTiffs));
        PhotogrammetryMinimization gaiaMinimizer = new PhotogrammetryMinimization();
        preProcessors.add(gaiaMinimizer);

        // tileProcess (PhotogrammetryTiler)
        TilingProcess tilingProcess = new PhotogrammetryTiler();

        // postProcess (GaiaMaximizer, GaiaRelocator, Batched3DModel)
        List<PostProcess> postProcessors = new ArrayList<>();
        postProcessors.add(new GaiaMaximizer());
        postProcessors.add(new GaiaRelocator());
        if (globalOptions.getTilesVersion()
                .equals("1.0")) {
            postProcessors.add(new Batched3DModel());
        } else {
            postProcessors.add(new Batched3DModelV2());
        }

        // Test
        //globalOptions.setDebugLod(true);// Test. delete this.!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // end Test.---

        Pipeline processPipeline = new TilingPipeline(preProcessors, tilingProcess, postProcessors);
        processPipeline.process(fileLoader);
    }

    private boolean getYUpAxis(FormatType formatType, boolean isYUpAxis) {
        if (formatType == FormatType.CITYGML || formatType == FormatType.INDOORGML || formatType == FormatType.SHP || formatType == FormatType.GEOJSON) {
            isYUpAxis = true;
        }
        return isYUpAxis;
    }

    private Converter getConverter(FormatType formatType) {
        Converter converter;
        if (formatType == FormatType.CITYGML) {
            converter = new CityGmlConverter();
        } else if (formatType == FormatType.INDOORGML) {
            converter = new IndoorGmlConverter();
        } else if (formatType == FormatType.SHP) {
            converter = new ShapeConverter();
        } else if (formatType == FormatType.GEOJSON) {
            converter = new GeoJsonConverter();
        } else {
            converter = new AssimpConverter();
        }
        return converter;
    }

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }
}
