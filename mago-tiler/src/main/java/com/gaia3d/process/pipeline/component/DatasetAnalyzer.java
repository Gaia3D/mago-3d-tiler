package com.gaia3d.process.pipeline.component;

import com.gaia3d.process.pipeline.TilingPipelineContext;

@FunctionalInterface
public interface DatasetAnalyzer {
    void analyze(TilingPipelineContext context);
}
