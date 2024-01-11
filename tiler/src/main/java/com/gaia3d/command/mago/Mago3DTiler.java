package com.gaia3d.command.mago;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.*;
import com.gaia3d.process.ProcessOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;

/**
 * Mago 3DTiler.
 * @author znkim
 * @since 1.0.0
 */
@Slf4j
public class Mago3DTiler {

    public void execute() {
        String inputType = GlobalOptions.getInstance().getInputFormat();
        String outputType = GlobalOptions.getInstance().getOutputFormat();
        FormatType inputFormat = FormatType.fromExtension(inputType);
        FormatType outputFormat = FormatType.fromExtension(outputType);
        try {
            ProcessFlowModel processFlow = getProcessModel(inputFormat, outputFormat);
            log.info("Starting process flow: {}", processFlow.getModelName());
            processFlow.run();
        } catch (Exception e) {
            throw new RuntimeException("Failed to run process.", e);
        }
    }

    /**
     * get 3dTiles process model (batched, instance, point cloud)
     * @param inputFormat FormatType
     * @param outputFormat FormatType
     * @return ProcessFlowModel
     */
    private ProcessFlowModel getProcessModel(FormatType inputFormat, FormatType outputFormat) {
        ProcessFlowModel processFlow;
        if (FormatType.I3DM == outputFormat) {
            processFlow = new InstanceProcessModel();
        } else if (FormatType.B3DM == outputFormat) {
            processFlow = new BatchProcessModel();
        } else if (FormatType.PNTS == outputFormat) {
            processFlow = new PointCloudProcessModel();
        } else {
            if (FormatType.LAS == inputFormat || FormatType.LAZ == inputFormat) {
                processFlow = new PointCloudProcessModel();
            } else {
                processFlow = new BatchProcessModel();
            }
        }
        return processFlow;
    }
}
