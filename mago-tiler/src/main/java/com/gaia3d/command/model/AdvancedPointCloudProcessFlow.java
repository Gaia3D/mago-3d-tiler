package com.gaia3d.command.model;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.loader.PointCloudFileLoader;
import com.gaia3d.converter.pointcloud.LasConverter;
import com.gaia3d.converter.pointcloud.LasConverterOptions;
import com.gaia3d.process.TilingPipeline;
import com.gaia3d.process.postprocess.PostProcess;
import com.gaia3d.process.postprocess.pointcloud.PointCloudModel;
import com.gaia3d.process.postprocess.pointcloud.PointCloudModelV2;
import com.gaia3d.process.postprocess.pointcloud.PointCloudModelV2Old;
import com.gaia3d.process.preprocess.PreProcess;
import com.gaia3d.process.tileprocess.Pipeline;
import com.gaia3d.process.tileprocess.TilingProcess;
import com.gaia3d.process.tileprocess.tile.PointCloudTiler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * PointsCloudProcessModel
 */
@Slf4j
public class AdvancedPointCloudProcessFlow implements ProcessFlow {
    private static final String MODEL_NAME = "PointCloudProcessFlow";
    private final GlobalOptions globalOptions = GlobalOptions.getInstance();

    @Override
    public void run() throws IOException {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        String tempDir = globalOptions.getTempPath();
        Path tempPath = Path.of(tempDir);
        LasConverterOptions options = LasConverterOptions.builder()
                .sourceCrs(globalOptions.getSourceCrs())
                .tempDirectory(tempPath)
                .force4ByteRgb(globalOptions.isForce4ByteRGB())
                .build();
        LasConverter converter = new LasConverter(options);
        PointCloudFileLoader fileLoader = new PointCloudFileLoader(converter);

        /* Pre-process */
        List<PreProcess> preProcessors = new ArrayList<>();

        /* Main-process */
        TilingProcess tilingProcess = new PointCloudTiler();

        /* Post-process */
        List<PostProcess> postProcessors = new ArrayList<>();

        if (globalOptions.getTilesVersion().equals("1.0")) {
            postProcessors.add(new PointCloudModel());
        } else {
            postProcessors.add(new PointCloudModelV2());
        }

        Pipeline processPipeline = new TilingPipeline(preProcessors, tilingProcess, postProcessors);
        processPipeline.process(fileLoader);
    }

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }
}
