package com.gaia3d.process.pipeline;

import com.gaia3d.converter.loader.FileLoader;
import com.gaia3d.process.pipeline.result.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TilingPipelineContext implements PipelineContext {
    private TilingType tilingType = TilingType.UNKNOWN;
    private FileLoader fileLoader;

    private IngestionResult ingestion = new IngestionResult();
    private NormalizationResult normalization = new NormalizationResult();
    private AnalysisResult analysis = new AnalysisResult();
    private RefinementResult refinement = new RefinementResult();
    private TilingResult tiling = new TilingResult();
    private ContentGenerationResult contentGeneration = new ContentGenerationResult();
    private CleanupResult cleanup = new CleanupResult();
}
