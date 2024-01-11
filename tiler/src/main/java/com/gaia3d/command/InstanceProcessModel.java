package com.gaia3d.command;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.MeshFileLoader;
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
import org.geotools.coverage.grid.GridCoverage2D;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class InstanceProcessModel implements ProcessFlowModel{
    public void run() throws IOException {
        MeshFileLoader fileLoader = new MeshFileLoader(null);
        List<PreProcess> preProcessors = new ArrayList<>();
        TilerOptions tilerOptions = TilerOptions.builder()
                .build();
        TileProcess tileProcess = new Gaia3DTiler();
        List<PostProcess> postProcessors = new ArrayList<>();
        Process processFlow = new ProcessFlow(preProcessors, tileProcess, postProcessors);
        processFlow.process(fileLoader);
    }

    @Override
    public String getModelName() {
        return "InstanceProcessModel";
    }

}
