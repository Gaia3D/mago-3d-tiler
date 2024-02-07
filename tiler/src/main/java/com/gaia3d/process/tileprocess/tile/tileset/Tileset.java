package com.gaia3d.process.tileprocess.tile.tileset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.tileset.asset.Asset;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import com.gaia3d.process.tileprocess.tile.tileset.node.Properties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Setter
public class Tileset {
    private Asset asset;
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    private double geometricError = 0.0d;
    private Node root;
    private Properties properties;

    @JsonIgnore
    public List<ContentInfo> findAllContentInfo() {
        List <ContentInfo> contentInfos = new ArrayList<>();
        if (root != null) {
            contentInfos = root.findAllContentInfo(contentInfos);
        } else {
            log.warn("root is null");
        }
        return contentInfos;
    }
}
