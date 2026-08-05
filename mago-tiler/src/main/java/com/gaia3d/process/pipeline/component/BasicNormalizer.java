package com.gaia3d.process.pipeline.component;

import com.gaia3d.process.pipeline.TilingPipelineContext;
import com.gaia3d.process.pipeline.result.IngestionResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BasicNormalizer implements Normalizer {

    @Override
    public void normalize(TilingPipelineContext context) {
        IngestionResult ingestionResult = context.getIngestion();
        if (ingestionResult == null || ingestionResult.getSourceFiles() == null) {
            log.warn("No ingestion result found. Skipping normalization.");
        }


    }
}
