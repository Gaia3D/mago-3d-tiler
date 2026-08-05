package com.gaia3d.process.pipeline.result;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class IngestionResult {
    private List<File> sourceFiles = new ArrayList<>();
    private List<File> modelFiles = new ArrayList<>();
    private List<File> metadataFiles = new ArrayList<>();
    private List<File> terrainFiles = new ArrayList<>();
    private List<File> geoidFiles = new ArrayList<>();
}
