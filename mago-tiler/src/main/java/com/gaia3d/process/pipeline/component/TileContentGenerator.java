package com.gaia3d.process.pipeline.component;

import com.gaia3d.process.pipeline.TilingPipelineContext;

@FunctionalInterface
public interface TileContentGenerator {
    void generate(TilingPipelineContext context);
}
