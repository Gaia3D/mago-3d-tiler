package com.gaia3d.process.pipeline.stage;

import com.gaia3d.process.pipeline.TilingPipelineContext;
import com.gaia3d.process.pipeline.component.TileContentGenerator;

import java.util.Objects;

public class ContentGenerationStage implements PipelineStage<TilingPipelineContext> {
    private final TileContentGenerator contentGenerator;

    public ContentGenerationStage() {
        this(context -> {
        });
    }

    public ContentGenerationStage(TileContentGenerator contentGenerator) {
        this.contentGenerator = Objects.requireNonNull(contentGenerator, "contentGenerator must not be null");
    }

    @Override
    public void execute(TilingPipelineContext context) {
        contentGenerator.generate(context);
    }
}
