package com.gaia3d.command.model;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.assimp.AssimpConverterOptions;
import com.gaia3d.converter.kml.AttributeReader;
import com.gaia3d.converter.kml.JacksonKmlReader;
import com.gaia3d.converter.loader.FileLoader;
import com.gaia3d.converter.loader.InstancedFileLoader;
import com.gaia3d.converter.Parametric3DOptions;
import com.gaia3d.converter.geojson.GeoJsonInstanceConverter;
import com.gaia3d.converter.geopackage.GeoPackageInstanceConverter;
import com.gaia3d.converter.shape.ShapeInstanceConverter;
import com.gaia3d.process.TilingPipeline;
import com.gaia3d.process.postprocess.PostProcess;
import com.gaia3d.process.postprocess.instance.Instanced3DModel;
import com.gaia3d.process.postprocess.instance.Instanced3DModelV2;
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
public class InstancedProcessFlow implements ProcessFlow {
    private static final String MODEL_NAME = "InstancedProcessFlow";
    private final GlobalOptions globalOptions = GlobalOptions.getInstance();

    @Override
    public void run() throws IOException {
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
        preProcessors.add(new TileInfoGenerator());
        preProcessors.add(new GaiaRotator());
        preProcessors.add(new GaiaTexCoordCorrection());
        preProcessors.add(new InstanceTranslation(geoTiffs));

        /* Main-process */
        TilingProcess tilingProcess = new Instanced3DModelTiler();
        //TilingProcess tilingProcess = new Instanced3DModelTiler4Trees();

        /* Post-process */
        List<PostProcess> postProcessors = new ArrayList<>();
        if (globalOptions.getTilesVersion().equals("1.0")) {
            postProcessors.add(new Instanced3DModel());
        } else {
            postProcessors.add(new Instanced3DModelV2());
        }

        Pipeline processPipeline = new TilingPipeline(preProcessors, tilingProcess, postProcessors);
        processPipeline.process(fileLoader);
    }

    private AttributeReader getAttributeReader(FormatType formatType) {
        Parametric3DOptions vectorOptions = Parametric3DOptions.builder()
                .attributeFilters(globalOptions.getAttributeFilters())
                .sourceCrs(globalOptions.getSourceCrs())
                .targetCrs(globalOptions.getTargetCrs())
                .heightColumnName(globalOptions.getHeightColumn())
                .altitudeColumnName(globalOptions.getAltitudeColumn())
                .diameterColumnName(globalOptions.getDiameterColumn())
                .scaleColumnName(globalOptions.getScaleColumn())
                .densityColumnName(globalOptions.getDensityColumn())
                .headingColumnName(globalOptions.getHeadingColumn())
                .absoluteAltitudeValue(globalOptions.getAbsoluteAltitude())
                .minimumHeightValue(globalOptions.getMinimumHeight())
                .skirtHeight(globalOptions.getSkirtHeight())
                .flipCoordinate(globalOptions.isFlipCoordinate())
                .build();

        AttributeReader reader = null;
        if (formatType == FormatType.SHP) {
            reader = new ShapeInstanceConverter(vectorOptions);
        } else if (formatType == FormatType.GEOJSON) {
            reader = new GeoJsonInstanceConverter(vectorOptions);
        } else if (formatType == FormatType.GEO_PACKAGE) {
            reader = new GeoPackageInstanceConverter(vectorOptions);
        } else {
            reader = new JacksonKmlReader();
        }
        return reader;
    }

    private Converter getConverter(FormatType formatType) {
        AssimpConverterOptions options = AssimpConverterOptions.builder()
                .build();
        options.setSplitByNode(globalOptions.isSplitByNode());
        return new AssimpConverter(options);
    }

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }

    private boolean getYUpAxis(FormatType formatType, boolean isYUpAxis) {
        if (formatType == FormatType.CITYGML || formatType == FormatType.SHP || formatType == FormatType.GEOJSON || formatType == FormatType.GEO_PACKAGE) {
            isYUpAxis = true;
        }
        return isYUpAxis;
    }
}
