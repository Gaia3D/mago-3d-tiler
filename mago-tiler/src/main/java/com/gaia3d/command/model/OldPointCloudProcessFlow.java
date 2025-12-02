package com.gaia3d.command.model;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.loader.OldPointCloudFileLoader;
import com.gaia3d.converter.pointcloud.OldLasConverter;
import com.gaia3d.converter.pointcloud.PointCloudTempGenerator;
import com.gaia3d.process.TilingPipeline;
import com.gaia3d.process.postprocess.PostProcess;
import com.gaia3d.process.postprocess.pointcloud.PointCloudModel;
import com.gaia3d.process.postprocess.pointcloud.PointCloudModelV2Old;
import com.gaia3d.process.preprocess.GaiaMinimization;
import com.gaia3d.process.preprocess.PreProcess;
import com.gaia3d.process.tileprocess.Pipeline;
import com.gaia3d.process.tileprocess.TilingProcess;
import com.gaia3d.process.tileprocess.tile.PointCloudTilerOld;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PointsCloudProcessModel
 */
@Slf4j
public class OldPointCloudProcessFlow implements ProcessFlow {
    private static final String MODEL_NAME = "PointCloudProcessFlow";
    private final GlobalOptions globalOptions = GlobalOptions.getInstance();

    @Override
    public void run() throws IOException {
        OldLasConverter converter = new OldLasConverter();
        PointCloudTempGenerator generator = new PointCloudTempGenerator(converter);
        OldPointCloudFileLoader fileLoader = new OldPointCloudFileLoader(converter, generator);

        /* Pre-process */
        List<PreProcess> preProcessors = new ArrayList<>();
        preProcessors.add(new GaiaMinimization());

        /* Main-process */
        TilingProcess tilingProcess = new PointCloudTilerOld();

        /* Post-process */
        List<PostProcess> postProcessors = new ArrayList<>();

        if (globalOptions.getTilesVersion().equals("1.0")) {
            postProcessors.add(new PointCloudModel());
        } else {
            postProcessors.add(new PointCloudModelV2Old());
        }

        Pipeline processPipeline = new TilingPipeline(preProcessors, tilingProcess, postProcessors);
        processPipeline.process(fileLoader);
    }

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }
}
