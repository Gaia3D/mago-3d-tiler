package com.gaia3d.process.pipeline.result;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ContentGenerationResult {
    private List<File> generatedContentFiles = new ArrayList<>();
    private int tileCount;
}
