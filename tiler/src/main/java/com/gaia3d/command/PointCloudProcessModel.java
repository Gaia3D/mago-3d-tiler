package com.gaia3d.command;

import com.gaia3d.converter.PointCloudFileLoader;
import com.gaia3d.converter.pointcloud.LasConverter;
import com.gaia3d.process.TilingPipeline;
import com.gaia3d.process.postprocess.PostProcess;
import com.gaia3d.process.postprocess.pointcloud.PointCloudModel;
import com.gaia3d.process.preprocess.PreProcess;
import com.gaia3d.process.tileprocess.Pipeline;
import com.gaia3d.process.tileprocess.TilingProcess;
import com.gaia3d.process.tileprocess.tile.PointCloudTiler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileExistsException;

import java.io.File;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PointCloudProcessModel implements ProcessFlowModel{
    public void run() throws IOException {
        LasConverter converter = new LasConverter();

        PointCloudFileLoader fileLoader = new PointCloudFileLoader(converter);
        List<PreProcess> preProcessors = new ArrayList<>();

        TilingProcess tilingProcess = new PointCloudTiler();
        List<PostProcess> postProcessors = new ArrayList<>();
        postProcessors.add(new PointCloudModel());

        Pipeline processPipeline = new TilingPipeline(preProcessors, tilingProcess, postProcessors);
        processPipeline.process(fileLoader);
    }

    @Override
    public String getModelName() {
        return "PointCloudProcessModel";
    }

}
