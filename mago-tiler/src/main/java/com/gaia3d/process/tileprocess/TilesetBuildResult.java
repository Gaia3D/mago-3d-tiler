package com.gaia3d.process.tileprocess;

import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.tileset.Tileset;

import java.util.Collections;
import java.util.List;
import com.gaia3d.process.tileprocess.tile.tileset.implicit.ImplicitSubtreeArtifact;

public class TilesetBuildResult {
    private final Tileset tileset;
    private final List<ContentInfo> contentInfos;
    private final List<ImplicitSubtreeArtifact> implicitSubtreeArtifacts;

    public TilesetBuildResult(Tileset tileset, List<ContentInfo> contentInfos) {
        this(tileset, contentInfos, Collections.emptyList());
    }

    public TilesetBuildResult(Tileset tileset, List<ContentInfo> contentInfos, List<ImplicitSubtreeArtifact> implicitSubtreeArtifacts) {
        this.tileset = tileset;
        this.contentInfos = contentInfos == null ? Collections.emptyList() : List.copyOf(contentInfos);
        this.implicitSubtreeArtifacts = implicitSubtreeArtifacts == null ? Collections.emptyList() : List.copyOf(implicitSubtreeArtifacts);
    }

    public Tileset getTileset() {
        return tileset;
    }

    public List<ContentInfo> getContentInfos() {
        return contentInfos;
    }

    public List<ImplicitSubtreeArtifact> getImplicitSubtreeArtifacts() {
        return implicitSubtreeArtifacts;
    }
}
