package com.gaia3d.command;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.PointCloudFileLoader;
import com.gaia3d.converter.pointcloud.LasConverter;
import com.gaia3d.process.PointCloudProcessFlow;
import com.gaia3d.process.ProcessOptions;
import com.gaia3d.process.TilerOptions;
import com.gaia3d.process.postprocess.PostProcess;
import com.gaia3d.process.postprocess.pointcloud.PointCloudModel;
import com.gaia3d.process.preprocess.PreProcess;
import com.gaia3d.process.tileprocess.TileProcess;
import com.gaia3d.process.tileprocess.tile.PointCloudTiler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileExistsException;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PointCloudProcessModel implements ProcessFlowModel{
    public void run() throws IOException {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        LasConverter converter = new LasConverter();

        PointCloudFileLoader fileLoader = new PointCloudFileLoader(converter);
        List<PreProcess> preProcessors = new ArrayList<>();

        TileProcess tileProcess = new PointCloudTiler();
        List<PostProcess> postProcessors = new ArrayList<>();
        postProcessors.add(new PointCloudModel());

        PointCloudProcessFlow processFlow = new PointCloudProcessFlow(preProcessors, tileProcess, postProcessors);
        processFlow.process(fileLoader);
    }

    @Override
    public String getModelName() {
        return "PointCloudProcessModel";
    }

    protected static boolean validate(Path outputPath) throws IOException {
        File output = outputPath.toFile();
        if (!output.exists()) {
            throw new FileExistsException("Output path is not exist.");
        } else if (!output.isDirectory()) {
            throw new NotDirectoryException("Output path is not directory.");
        } else if (!output.canWrite()) {
            throw new IOException("Output path is not writable.");
        }
        return true;
    }

    protected static Path createPath(File file) {
        Path path = file.toPath();
        boolean result = file.mkdir();
        if (result) {
            log.info("Created new directory: {}", path);
        }
        return path;
    }
}
