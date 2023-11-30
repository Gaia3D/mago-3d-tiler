package com.gaia3d.process.tileprocess.tile.tileset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.tileset.asset.Asset;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import com.gaia3d.process.tileprocess.tile.tileset.node.Properties;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Tileset {
    private Asset asset;
    private double geometricError = 0.0d;
    private Node root;
    private Properties properties;

    @JsonIgnore
    public List<ContentInfo> findAllContentInfo() {
        return root.findAllContentInfo(new ArrayList<>());
    }
}
