package com.gaia3d.process.pipeline.stage;

import com.gaia3d.process.pipeline.TilingPipelineContext;
import com.gaia3d.process.pipeline.component.Normalizer;

import java.util.Objects;

public class NormalizationStage implements PipelineStage<TilingPipelineContext> {
    private final Normalizer normalizer;

    public NormalizationStage() {
        this(context -> {
        });
    }

    public NormalizationStage(Normalizer normalizer) {
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
    }

    @Override
    public void execute(TilingPipelineContext context) {
        normalizer.normalize(context);
    }
}
