package com.gaia3d.command.model;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.kml.AttributeReader;
import com.gaia3d.converter.kml.JacksonKmlReader;
import com.gaia3d.converter.geometry.shape.ShapePointReader;
import com.gaia3d.converter.loader.FileLoader;
import com.gaia3d.converter.loader.InstancedFileLoader;
import com.gaia3d.process.TilingPipeline;
import com.gaia3d.process.postprocess.PostProcess;
import com.gaia3d.process.postprocess.instance.Instanced3DModel;
import com.gaia3d.process.preprocess.*;
import com.gaia3d.process.tileprocess.Pipeline;
import com.gaia3d.process.tileprocess.TilingProcess;
import com.gaia3d.process.tileprocess.tile.Instanced3DModelTiler;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * InstancedProcessModel
 */
@Slf4j
public class InstancedProcessModel implements ProcessFlowModel {
    private static final String MODEL_NAME = "InstancedProcessModel";

    @Override
    public void run() throws IOException {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        FormatType inputFormat = globalOptions.getInputFormat();

        Converter converter = getConverter(inputFormat);
        AttributeReader kmlReader = getAttributeReader(inputFormat);
        FileLoader fileLoader = new InstancedFileLoader(converter, kmlReader);

        List<GridCoverage2D> geoTiffs = new ArrayList<>();
        if (globalOptions.getTerrainPath() != null) {
            geoTiffs = fileLoader.loadGridCoverages(geoTiffs);
        }

        /* Pre-process */
        List<PreProcess> preProcessors = new ArrayList<>();
        preProcessors.add(new GaiaTileInfoInitiator());
        preProcessors.add(new GaiaRotator());
        preProcessors.add(new GaiaTexCoordCorrector());
        preProcessors.add(new GaiaInstanceTranslator(geoTiffs));

        /* Main-process */
        TilingProcess tilingProcess = new Instanced3DModelTiler();

        /* Post-process */
        List<PostProcess> postProcessors = new ArrayList<>();
        postProcessors.add(new Instanced3DModel());

        Pipeline processPipeline = new TilingPipeline(preProcessors, tilingProcess, postProcessors);
        processPipeline.process(fileLoader);
    }

    private AttributeReader getAttributeReader(FormatType formatType) {
        AttributeReader reader = null;
        if (formatType == FormatType.SHP) {
            reader = new ShapePointReader();
        } /*else if (formatType == FormatType.GEOJSON) {
            reader = new GeoJsonConverter();
        }*/ else {
            reader = new JacksonKmlReader();
        }
        return reader;
    }

    private Converter getConverter(FormatType formatType) {
        return new AssimpConverter();
    }

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }

    private boolean getYUpAxis(FormatType formatType, boolean isYUpAxis) {
        if (formatType == FormatType.CITYGML || formatType == FormatType.SHP || formatType == FormatType.GEOJSON) {
            isYUpAxis = true;
        }
        return isYUpAxis;
    }
}
