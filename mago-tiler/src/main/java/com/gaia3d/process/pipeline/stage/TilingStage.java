package com.gaia3d.process.pipeline.stage;

import com.gaia3d.process.pipeline.TilingPipelineContext;
import com.gaia3d.process.pipeline.component.TileTreeBuilder;

import java.util.Objects;

public class TilingStage implements PipelineStage<TilingPipelineContext> {
    private final TileTreeBuilder tileTreeBuilder;

    public TilingStage() {
        this(context -> {

        });
    }

    public TilingStage(TileTreeBuilder tileTreeBuilder) {
        this.tileTreeBuilder = Objects.requireNonNull(tileTreeBuilder, "tileTreeBuilder must not be null");
    }

    @Override
    public void execute(TilingPipelineContext context) {
        tileTreeBuilder.build(context);
    }
}
