package com.gaia3d.process.tileprocess.tile.tileset.extension;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Extension3DTilesContentGltf {
    private List<String> extensionsUsed = new ArrayList<>();
    private List<String> extensionsRequired = new ArrayList<>();

    public Extension3DTilesContentGltf() {}
}
