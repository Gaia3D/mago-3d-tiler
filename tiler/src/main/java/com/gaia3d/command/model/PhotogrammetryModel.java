package com.gaia3d.command.model;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.assimp.LargeMeshConverter;
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
import com.gaia3d.process.preprocess.*;
import com.gaia3d.process.tileprocess.Pipeline;
import com.gaia3d.process.tileprocess.TilingProcess;
import com.gaia3d.process.tileprocess.tile.PhotogrammetryTiler;
import com.gaia3d.process.preprocess.PhotogrammetryMinimizer;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PhotogrammetryModel implements ProcessFlowModel {
    private static final String MODEL_NAME = "PhotogrammetryModel";
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
        preProcessors.add(new GaiaTileInfoInitiator());
        preProcessors.add(new GaiaTexCoordCorrector());
        preProcessors.add(new GaiaScaler());

        preProcessors.add(new GaiaRotatorPR());
        preProcessors.add(new GaiaStrictTranslator(geoTiffs));
        PhotogrammetryMinimizer gaiaMinimizer = new PhotogrammetryMinimizer();
        preProcessors.add(gaiaMinimizer);

        // tileProcess (PhotogrammetryTiler)
        TilingProcess tilingProcess = new PhotogrammetryTiler();

        // postProcess (GaiaMaximizer, GaiaRelocator, Batched3DModel)
        List<PostProcess> postProcessors = new ArrayList<>();
        postProcessors.add(new GaiaMaximizer());
        postProcessors.add(new GaiaRelocator());
        postProcessors.add(new Batched3DModel());

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
            if (globalOptions.isLargeMesh()) {
                converter = new LargeMeshConverter(new AssimpConverter());
            } else {
                converter = new AssimpConverter();
            }
        }
        return converter;
    }

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }
}
