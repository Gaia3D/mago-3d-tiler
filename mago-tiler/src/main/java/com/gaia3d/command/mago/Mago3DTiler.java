package com.gaia3d.command.mago;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.model.*;
import com.gaia3d.process.tileprocess.TileMerger;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Mago 3DTiler.
 */
@Slf4j
public class Mago3DTiler {

    public void execute() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        FormatType inputFormat = globalOptions.getInputFormat();
        FormatType outputFormat = globalOptions.getOutputFormat();
        try {
            ProcessFlow processFlow = getProcessModel(inputFormat, outputFormat);
            log.info("[Init] Starting process flow: {}", processFlow.getModelName());
            processFlow.run();
        } catch (IOException e) {
            log.error("[ERROR] Failed to run process.", e);
            throw new RuntimeException("Failed to run process.", e);
        }
    }

    public void merge() {
        TileMerger tileMerger = new TileMerger();
        tileMerger.merge();
    }

    /**
     * get 3dTiles process model (batched, instance, point cloud)
     * @param inputFormat FormatType
     * @param outputFormat FormatType
     * @return ProcessFlowModel
     */
    private ProcessFlow getProcessModel(FormatType inputFormat, FormatType outputFormat) {
        ProcessFlow processFlow;
        if (FormatType.I3DM == outputFormat) {
            processFlow = new InstancedProcessFlow();
        } else if (FormatType.PHOTOGRAMMETRY == outputFormat) {
            processFlow = new PhotogrammetryProcessFlow();
        } else if (FormatType.B3DM == outputFormat) {
            boolean isPhotogrammetry = GlobalOptions.getInstance().isPhotogrammetry();
            if (isPhotogrammetry) {
                processFlow = new PhotogrammetryProcessFlow();
            } else {
                processFlow = new BatchedModelProcessFlow();
            }
        } else if (FormatType.PNTS == outputFormat) {
            processFlow = new AdvancedPointCloudProcessFlow();
        } else {
            throw new IllegalArgumentException("Unsupported output format: " + outputFormat);
        }
        return processFlow;
    }
}
