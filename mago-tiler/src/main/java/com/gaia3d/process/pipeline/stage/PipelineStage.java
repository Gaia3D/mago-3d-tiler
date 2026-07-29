package com.gaia3d.process.pipeline.stage;

import com.gaia3d.process.pipeline.PipelineContext;

public interface PipelineStage<C extends PipelineContext> {
    void execute(C context);
}
