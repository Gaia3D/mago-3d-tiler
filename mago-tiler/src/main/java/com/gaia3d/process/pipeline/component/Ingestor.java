package com.gaia3d.process.pipeline.component;

import com.gaia3d.process.pipeline.TilingPipelineContext;

@FunctionalInterface
public interface Ingestor {
    void ingest(TilingPipelineContext context);
}
