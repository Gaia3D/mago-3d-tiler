package com.gaia3d.process.pipeline.component;

import com.gaia3d.process.pipeline.TilingPipelineContext;

@FunctionalInterface
public interface Normalizer {
    void normalize(TilingPipelineContext context);
}
