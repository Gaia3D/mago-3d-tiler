package com.gaia3d.process.pipeline.result;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalysisResult {
    private GaiaBoundingBox boundingBox;
    private int fileCount;
    private int tileInfoCount;
    private long nodeCount;
    private long vertexCount;
    private long faceCount;
}
