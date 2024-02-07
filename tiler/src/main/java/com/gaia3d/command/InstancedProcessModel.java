package com.gaia3d.command;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.FileLoader;
import com.gaia3d.converter.InstancedFileLoader;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.process.TilingPipeline;
import com.gaia3d.process.postprocess.PostProcess;
import com.gaia3d.process.postprocess.instance.Instanced3DModel;
import com.gaia3d.process.preprocess.GaiaTileInfoinitiator;
import com.gaia3d.process.preprocess.PreProcess;
import com.gaia3d.process.tileprocess.Pipeline;
import com.gaia3d.process.tileprocess.TilingProcess;
import com.gaia3d.process.tileprocess.tile.Instanced3DModelTiler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class InstancedProcessModel implements ProcessFlowModel{
    public void run() throws IOException {
        Converter converter = new AssimpConverter();
        FileLoader fileLoader = new InstancedFileLoader(converter);

        List<PreProcess> preProcessors = new ArrayList<>();
        preProcessors.add(new GaiaTileInfoinitiator());

        TilingProcess tilingProcess = new Instanced3DModelTiler();

        List<PostProcess> postProcessors = new ArrayList<>();
        postProcessors.add(new Instanced3DModel());

        Pipeline processPipeline = new TilingPipeline(preProcessors, tilingProcess, postProcessors);
        processPipeline.process(fileLoader);
    }

    @Override
    public String getModelName() {
        return "InstancedProcessModel";
    }

    private boolean getYUpAxis(FormatType formatType, boolean isYUpAxis) {
        if (formatType == FormatType.CITY_GML || formatType == FormatType.SHP || formatType == FormatType.GEOJSON || formatType == FormatType.JSON) {
            isYUpAxis = true;
        }
        return isYUpAxis;
    }
}
