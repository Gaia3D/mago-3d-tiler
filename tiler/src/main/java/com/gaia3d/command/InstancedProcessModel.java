package com.gaia3d.command;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.FileLoader;
import com.gaia3d.converter.InstancedFileLoader;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.kml.AttributeReader;
import com.gaia3d.converter.kml.JacksonKmlReader;
import com.gaia3d.converter.kml.ShapeReader;
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

@Slf4j
public class InstancedProcessModel implements ProcessFlowModel{
    public void run() throws IOException {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        FormatType inputFormat = globalOptions.getInputFormat();
        boolean isYUpAxis = getYUpAxis(inputFormat, globalOptions.isYUpAxis());
        Converter converter = getConverter(inputFormat);
        AttributeReader kmlReader = getAttributeReader(inputFormat);
        FileLoader fileLoader = new InstancedFileLoader(converter, kmlReader);

        List<GridCoverage2D> geoTiffs = new ArrayList<>();
        if (globalOptions.getTerrainPath() != null) {
            geoTiffs = fileLoader.loadGridCoverages(geoTiffs);
        }

        List<PreProcess> preProcessors = new ArrayList<>();
        preProcessors.add(new GaiaTileInfoInitiator());
        preProcessors.add(new GaiaTexCoordCorrector());
        preProcessors.add(new GaiaInstanceTranslator(geoTiffs));

        TilingProcess tilingProcess = new Instanced3DModelTiler();

        List<PostProcess> postProcessors = new ArrayList<>();
        postProcessors.add(new Instanced3DModel());

        Pipeline processPipeline = new TilingPipeline(preProcessors, tilingProcess, postProcessors);
        processPipeline.process(fileLoader);
    }

    private AttributeReader getAttributeReader(FormatType formatType) {
        AttributeReader reader = null;
        if (formatType == FormatType.CITYGML) {
            //reader = new CityGmlConverter();
        } else if (formatType == FormatType.SHP) {
            reader = new ShapeReader();
        } else if (formatType == FormatType.GEOJSON) {
            //reader = new GeoJsonConverter();
        } else {
            reader = new JacksonKmlReader();
        }
        return reader;
    }

    private Converter getConverter(FormatType formatType) {
        Converter converter = new AssimpConverter();
        return converter;
    }

    @Override
    public String getModelName() {
        return "InstancedProcessModel";
    }

    private boolean getYUpAxis(FormatType formatType, boolean isYUpAxis) {
        if (formatType == FormatType.CITYGML || formatType == FormatType.SHP || formatType == FormatType.GEOJSON) {
            isYUpAxis = true;
        }
        return isYUpAxis;
    }
}
