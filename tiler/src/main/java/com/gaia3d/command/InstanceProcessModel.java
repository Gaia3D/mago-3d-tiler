package com.gaia3d.command;

import com.gaia3d.converter.MeshFileLoader;
import com.gaia3d.process.TilingPipeline;
import com.gaia3d.process.postprocess.PostProcess;
import com.gaia3d.process.preprocess.PreProcess;
import com.gaia3d.process.tileprocess.Pipeline;
import com.gaia3d.process.tileprocess.TilingProcess;
import com.gaia3d.process.tileprocess.tile.BatchedModelTiler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class InstanceProcessModel implements ProcessFlowModel{
    public void run() throws IOException {
        MeshFileLoader fileLoader = new MeshFileLoader(null);
        List<PreProcess> preProcessors = new ArrayList<>();
        TilingProcess tilingProcess = new BatchedModelTiler();
        List<PostProcess> postProcessors = new ArrayList<>();
        Pipeline processPipeline = new TilingPipeline(preProcessors, tilingProcess, postProcessors);
        processPipeline.process(fileLoader);
    }

    @Override
    public String getModelName() {
        return "InstanceProcessModel";
    }

}
