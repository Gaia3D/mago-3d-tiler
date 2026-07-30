package com.gaia3d.process.pipeline;

import com.gaia3d.process.pipeline.stage.PipelineStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * PipelineExecutor
 */
@Slf4j
@RequiredArgsConstructor
public class PipelineExecutor<C extends PipelineContext> {
    private final List<PipelineStage<C>> stages;

    public void execute(C context) {
        Objects.requireNonNull(context, "context must not be null");
        int stageCount = stages.size();
        String contextClassName = context.getClass().getSimpleName();

        log.info("Pipeline execution started for context: {}", contextClassName);
        for (PipelineStage<C> stage : stages) {
            int stageIndex = stages.indexOf(stage) + 1;
            String stageName = stage.getClass().getSimpleName();
            log.info("Executing stage [{}/{}]: {}", stageIndex, stageCount, stageName);
            stage.execute(context);
        }
        log.info("Pipeline execution finished for context: {}", contextClassName);
    }
}
