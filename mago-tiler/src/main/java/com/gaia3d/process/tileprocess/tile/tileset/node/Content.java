package com.gaia3d.process.tileprocess.tile.tileset.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Content {
    BoundingVolume boundingVolume;
    private String uri = null;
    @JsonIgnore
    private ContentInfo contentInfo = null;
}
