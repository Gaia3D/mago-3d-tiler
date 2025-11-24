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
import com.gaia3d.converter.Parametric3DOptions;
import com.gaia3d.converter.citygml.CityGmlConverter;
import com.gaia3d.converter.geojson.GeoJsonConverter;
import com.gaia3d.converter.geopackage.GeoPackageConverter;
import com.gaia3d.converter.indoorgml.IndoorGmlConverter;
import com.gaia3d.converter.shape.ShapeConverter;
import com.gaia3d.process.TilingPipeline;
import com.gaia3d.process.postprocess.GaiaMaximizer;
import com.gaia3d.process.postprocess.GaiaRelocator;
import com.gaia3d.process.postprocess.PostProcess;
import com.gaia3d.process.postprocess.batch.Batched3DModel;
import com.gaia3d.process.postprocess.batch.Batched3DModelV2;
import com.gaia3d.process.preprocess.*;
import com.gaia3d.process.tileprocess.Pipeline;
import com.gaia3d.process.tileprocess.TilingProcess;
import com.gaia3d.process.tileprocess.tile.Batched3DModelTiler;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * BatchedProcessModel
 */
@Slf4j
public class BatchedModelProcessFlow implements ProcessFlow {
    private static final String MODEL_NAME = "BatchedModelProcessFlow";
    private final GlobalOptions globalOptions = GlobalOptions.getInstance();

    @Override
    public void run() throws IOException {
        FormatType inputFormat = globalOptions.getInputFormat();

        Converter converter = getConverter(inputFormat);
        AttributeReader kmlReader = new FastKmlReader();
        ExtrusionTempGenerator tempGenerator = new ExtrusionTempGenerator(converter);
        BatchedFileLoader fileLoader = new BatchedFileLoader(converter, kmlReader, tempGenerator);

        List<GridCoverage2D> geoTiffs = new ArrayList<>();
        if (globalOptions.getTerrainPath() != null) {
            geoTiffs = fileLoader.loadGridCoverages(geoTiffs);
        }

        /* Pre-process */
        List<PreProcess> preProcessors = new ArrayList<>();
        preProcessors.add(new TileInfoGenerator());

        preProcessors.add(new GaiaScaler());
        preProcessors.add(new GaiaZUpTransformer());
        preProcessors.add(new GaiaRotator());
        preProcessors.add(new GaiaTransformBaker());

        preProcessors.add(new GaiaCoordinateExtractor());
        preProcessors.add(new GaiaTranslator(geoTiffs));
        preProcessors.add(new GaiaTexCoordCorrection());
        preProcessors.add(new GaiaTransformBaker());

        preProcessors.add(new GaiaMinimization());

        /* Main-process */
        TilingProcess tilingProcess = new Batched3DModelTiler();

        /* Post-process */
        List<PostProcess> postProcessors = new ArrayList<>();
        postProcessors.add(new GaiaMaximizer());
        postProcessors.add(new GaiaRelocator());

        if (globalOptions.getTilesVersion().equals("1.0")) {
            postProcessors.add(new Batched3DModel());
        } else {
            postProcessors.add(new Batched3DModelV2());
        }

        Pipeline processPipeline = new TilingPipeline(preProcessors, tilingProcess, postProcessors);
        processPipeline.process(fileLoader);
    }

    private Converter getConverter(FormatType formatType) {

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

        Converter converter;
        if (formatType == FormatType.CITYGML) {
            converter = new CityGmlConverter(vectorOptions);
        } else if (formatType == FormatType.INDOORGML) {
            converter = new IndoorGmlConverter(vectorOptions);
        } else if (formatType == FormatType.SHP) {
            converter = new ShapeConverter(vectorOptions);
        } else if (formatType == FormatType.GEOJSON) {
            converter = new GeoJsonConverter(vectorOptions);
        } else if (formatType == FormatType.GEO_PACKAGE) {
            converter = new GeoPackageConverter(vectorOptions);
        } else {
            AssimpConverterOptions options = AssimpConverterOptions.builder().build();
            options.setSplitByNode(globalOptions.isSplitByNode());
            converter = new AssimpConverter(options);
        }
        return converter;
    }

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }
}
