package com.gaia3d.command.model;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.loader.PointCloudFileLoader;
import com.gaia3d.converter.pointcloud.LasConverter;
import com.gaia3d.process.TilingPipeline;
import com.gaia3d.process.postprocess.PostProcess;
import com.gaia3d.process.postprocess.pointcloud.PointCloudModel;
import com.gaia3d.process.preprocess.GaiaMinimizer;
import com.gaia3d.process.preprocess.PreProcess;
import com.gaia3d.process.tileprocess.Pipeline;
import com.gaia3d.process.tileprocess.TilingProcess;
import com.gaia3d.process.tileprocess.tile.PointCloudTiler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PointCloudProcessModel implements ProcessFlowModel {
    private static final String MODEL_NAME = "PointCloudProcessModel";
    private final GlobalOptions globalOptions = GlobalOptions.getInstance();

    public void run() throws IOException {
        LasConverter converter = new LasConverter();

        PointCloudFileLoader fileLoader = new PointCloudFileLoader(converter);
        List<PreProcess> preProcessors = new ArrayList<>();
        preProcessors.add(new GaiaMinimizer());

        TilingProcess tilingProcess = new PointCloudTiler();
        List<PostProcess> postProcessors = new ArrayList<>();
        postProcessors.add(new PointCloudModel());

        Pipeline processPipeline = new TilingPipeline(preProcessors, tilingProcess, postProcessors);
        processPipeline.process(fileLoader);
    }

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }
}
