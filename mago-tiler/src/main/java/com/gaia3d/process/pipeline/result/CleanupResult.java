package com.gaia3d.process.pipeline.result;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CleanupResult {
    private List<Path> deletedTempDirectories = new ArrayList<>();
    private List<Path> retainedTempDirectories = new ArrayList<>();
}
