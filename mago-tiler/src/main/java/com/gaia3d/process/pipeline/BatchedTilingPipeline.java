package com.gaia3d.process.pipeline;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.pipeline.component.*;
import com.gaia3d.process.pipeline.factory.TilingPipelineFactory;

import java.io.IOException;

public class BatchedTilingPipeline {
    private final GlobalOptions globalOptions = GlobalOptions.getInstance();

    public void execute() throws IOException {

        TilingPipelineContext context = new TilingPipelineContext();
        PipelineExecutor<TilingPipelineContext> executor = createComponents();
        executor.execute(context);
    }

    private PipelineExecutor<TilingPipelineContext> createComponents() {
        Ingestor ingestor = new BasicIngestor();

        Normalizer normalizer = context -> {
            // Normalizer logic for testing
        };

        DatasetAnalyzer analyzer = context -> {
            // Analyzer logic for testing
        };

        DatasetRefiner refiner = context -> {
            // Refiner logic for testing
        };

        TileTreeBuilder tileTreeBuilder = context -> {
            // TileTreeBuilder logic for testing
        };

        TileContentGenerator contentGenerator = context -> {
            // ContentGenerator logic for testing
        };

        CleanupTask cleanupTask = context -> {
            // CleanupTask logic for testing
        };

        return new TilingPipelineFactory(ingestor, normalizer, analyzer, refiner, tileTreeBuilder, contentGenerator, cleanupTask).createExecutor();
    }
}
