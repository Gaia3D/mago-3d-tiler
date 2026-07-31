package com.gaia3d.process.tileprocess;

import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;
import com.gaia3d.process.tileprocess.tile.tileset.implicit.ImplicitSubtreeArtifact;

import java.util.Collections;
import java.util.List;

public record TilesetBuildResult(Tileset tileset, List<ContentInfo> contentInfos, List<ImplicitSubtreeArtifact> implicitSubtreeArtifacts) {
    public TilesetBuildResult(Tileset tileset, List<ContentInfo> contentInfos) {
        this(tileset, contentInfos, Collections.emptyList());
    }

    public TilesetBuildResult(Tileset tileset, List<ContentInfo> contentInfos, List<ImplicitSubtreeArtifact> implicitSubtreeArtifacts) {
        this.tileset = tileset;
        this.contentInfos = contentInfos == null ? Collections.emptyList() : List.copyOf(contentInfos);
        this.implicitSubtreeArtifacts = implicitSubtreeArtifacts == null ? Collections.emptyList() : List.copyOf(implicitSubtreeArtifacts);
    }
}
