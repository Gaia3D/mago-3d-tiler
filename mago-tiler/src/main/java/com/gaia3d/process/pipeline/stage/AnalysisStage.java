package com.gaia3d.process.pipeline.stage;

import com.gaia3d.process.pipeline.TilingPipelineContext;
import com.gaia3d.process.pipeline.component.DatasetAnalyzer;

import java.util.Objects;

public class AnalysisStage implements PipelineStage<TilingPipelineContext> {
    private final DatasetAnalyzer analyzer;

    public AnalysisStage() {
        this(context -> {
        });
    }

    public AnalysisStage(DatasetAnalyzer analyzer) {
        this.analyzer = Objects.requireNonNull(analyzer, "analyzer must not be null");
    }

    @Override
    public void execute(TilingPipelineContext context) {
        analyzer.analyze(context);
    }
}
