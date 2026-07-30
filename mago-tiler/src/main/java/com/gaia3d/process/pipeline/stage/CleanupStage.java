package com.gaia3d.process.pipeline.stage;

import com.gaia3d.process.pipeline.TilingPipelineContext;
import com.gaia3d.process.pipeline.component.CleanupTask;

import java.util.Objects;

public class CleanupStage implements PipelineStage<TilingPipelineContext> {
    private final CleanupTask cleanupTask;

    public CleanupStage() {
        this(context -> {
        });
    }

    public CleanupStage(CleanupTask cleanupTask) {
        this.cleanupTask = Objects.requireNonNull(cleanupTask, "cleanupTask must not be null");
    }

    @Override
    public void execute(TilingPipelineContext context) {
        cleanupTask.cleanup(context);
    }
}
