package com.gaia3d.process.pipeline;

import com.gaia3d.command.LoggingConfiguration;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.pipeline.component.*;
import com.gaia3d.process.pipeline.factory.TilingPipelineFactory;
import com.gaia3d.process.pipeline.stage.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@Tag("default")
class PipelineExecutorTest {

    static {
        LoggingConfiguration.initConsoleLogger();
        GlobalOptions.getInstance();
    }

    /**
     * Test the execution of the pipeline with all stages.
     */
    @Test
    void executeTest() {
        Ingestor ingestor = context -> {
            // Ingestor logic for testing
        };

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


        TilingPipelineContext context = new TilingPipelineContext();

        TilingPipelineFactory factory = new TilingPipelineFactory(ingestor, normalizer, analyzer, refiner, tileTreeBuilder, contentGenerator, cleanupTask);
        PipelineExecutor<TilingPipelineContext> executor = factory.createExecutor();
        executor.execute(context);
    }

    @Test
    void executeFactoryTest() {
        TilingPipelineFactory factory = new TilingPipelineFactory(context -> context.getIngestion().getSourceFiles().clear(), context -> context.getNormalization().getNormalizedFiles().clear(), context -> context.getAnalysis().setNodeCount(0), context -> context.getRefinement()
                .getRefinedTileInfos()
                .clear(), context -> context.getTiling().getContentInfos().clear(), context -> context.getContentGeneration().setTileCount(0), context -> context.getCleanup().getDeletedTempDirectories().clear());

        PipelineExecutor<TilingPipelineContext> executor = factory.createExecutor();
        executor.execute(new TilingPipelineContext());
    }

    @Test
    void createFactoryWithNullRefinerTest() {
        assertThrows(NullPointerException.class, () -> new TilingPipelineFactory(context -> {}, context -> {}, context -> {}, null, context -> {}, context -> {}, context -> {}));
    }
}
