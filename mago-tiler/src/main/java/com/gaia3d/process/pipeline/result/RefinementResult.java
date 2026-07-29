package com.gaia3d.process.pipeline.result;

import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RefinementResult {
    private List<TileInfo> refinedTileInfos = new ArrayList<>();
    private List<File> generatedIntermediateFiles = new ArrayList<>();
}
