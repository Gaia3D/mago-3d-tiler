package com.gaia3d.process.pipeline.result;

import com.gaia3d.process.tileprocess.TilesetBuildResult;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TilingResult {
    private TilesetBuildResult tilesetBuildResult;
    private List<ContentInfo> contentInfos = new ArrayList<>();
}
