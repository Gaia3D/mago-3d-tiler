package com.gaia3d.process.pipeline.factory;

import com.gaia3d.process.pipeline.PipelineExecutor;
import com.gaia3d.process.pipeline.TilingPipelineContext;
import com.gaia3d.process.pipeline.component.*;
import com.gaia3d.process.pipeline.stage.*;

import java.util.List;
import java.util.Objects;

public class TilingPipelineFactory {
    private final Ingestor ingestor;
    private final Normalizer normalizer;
    private final DatasetAnalyzer analyzer;
    private final DatasetRefiner refiner;
    private final TileTreeBuilder tileTreeBuilder;
    private final TileContentGenerator contentGenerator;
    private final CleanupTask cleanupTask;

    public TilingPipelineFactory(Ingestor ingestor, Normalizer normalizer, DatasetAnalyzer analyzer, DatasetRefiner refiner, TileTreeBuilder tileTreeBuilder, TileContentGenerator contentGenerator, CleanupTask cleanupTask) {
        this.ingestor = Objects.requireNonNull(ingestor, "ingestor must not be null");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
        this.analyzer = Objects.requireNonNull(analyzer, "analyzer must not be null");
        this.refiner = Objects.requireNonNull(refiner, "refiner must not be null");
        this.tileTreeBuilder = Objects.requireNonNull(tileTreeBuilder, "tileTreeBuilder must not be null");
        this.contentGenerator = Objects.requireNonNull(contentGenerator, "contentGenerator must not be null");
        this.cleanupTask = Objects.requireNonNull(cleanupTask, "cleanupTask must not be null");
    }

    public PipelineExecutor<TilingPipelineContext> createExecutor() {
        return new PipelineExecutor<>(createStages());
    }

    public List<PipelineStage<TilingPipelineContext>> createStages() {
        return List.of(
                new IngestionStage(ingestor),
                new NormalizationStage(normalizer),
                new AnalysisStage(analyzer),
                new RefinementStage(refiner),
                new TilingStage(tileTreeBuilder),
                new ContentGenerationStage(contentGenerator),
                new CleanupStage(cleanupTask)
        );
    }
}
