package com.gaia3d.process.pipeline.result;

import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class NormalizationResult {
    private List<File> normalizedFiles = new ArrayList<>();
    private List<TileInfo> normalizedTileInfos = new ArrayList<>();
    private List<Path> tempDirectories = new ArrayList<>();
}
