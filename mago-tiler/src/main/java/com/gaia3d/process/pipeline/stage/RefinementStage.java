package com.gaia3d.process.pipeline.stage;

import com.gaia3d.process.pipeline.TilingPipelineContext;
import com.gaia3d.process.pipeline.component.DatasetRefiner;

import java.util.Objects;

public class RefinementStage implements PipelineStage<TilingPipelineContext> {
    private final DatasetRefiner refiner;

    public RefinementStage() {
        this(context -> {
        });
    }

    public RefinementStage(DatasetRefiner refiner) {
        this.refiner = Objects.requireNonNull(refiner, "refiner must not be null");
    }

    @Override
    public void execute(TilingPipelineContext context) {
        refiner.refine(context);
    }
}
