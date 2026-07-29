package com.gaia3d.process.pipeline.stage;

import com.gaia3d.process.pipeline.TilingPipelineContext;
import com.gaia3d.process.pipeline.component.Ingestor;

import java.util.Objects;

public class IngestionStage implements PipelineStage<TilingPipelineContext> {
    private final Ingestor ingestor;

    public IngestionStage() {
        this(context -> {
        });
    }

    public IngestionStage(Ingestor ingestor) {
        this.ingestor = Objects.requireNonNull(ingestor, "ingestor must not be null");
    }

    @Override
    public void execute(TilingPipelineContext context) {
        ingestor.ingest(context);
    }
}
